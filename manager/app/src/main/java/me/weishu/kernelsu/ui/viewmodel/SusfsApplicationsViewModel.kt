package me.weishu.kernelsu.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.util.SusfsApplication
import me.weishu.kernelsu.ui.util.SusfsApplicationHidingConfigEntry
import me.weishu.kernelsu.ui.util.SusfsApplicationRepository
import me.weishu.kernelsu.ui.util.SusfsApplyReport
import me.weishu.kernelsu.ui.util.SusfsHideMode
import me.weishu.kernelsu.ui.util.SusfsImportPlan
import me.weishu.kernelsu.ui.util.SusfsPolicyState
import me.weishu.kernelsu.ui.util.planSusfsImport
import me.weishu.kernelsu.ui.util.updateSusfsUid

internal enum class SusfsAppCategory { All, Risk, Normal }
internal enum class SusfsAppFilter { All, Enabled, Disabled, Default, Unknown }

internal fun SusfsAppFilter.matches(policy: SusfsPolicyState): Boolean = when (this) {
    SusfsAppFilter.All -> true
    SusfsAppFilter.Enabled -> policy.known && !policy.allowSu && policy.effectiveHidden == true
    SusfsAppFilter.Disabled -> policy.known && !policy.allowSu && policy.effectiveHidden == false
    SusfsAppFilter.Default -> policy.known && !policy.allowSu && policy.mode == SusfsHideMode.Default
    SusfsAppFilter.Unknown -> !policy.known
}

internal fun filterSusfsApplications(
    apps: List<SusfsApplication>,
    query: String,
    category: SusfsAppCategory,
    filter: SusfsAppFilter,
    showSystem: Boolean,
): List<SusfsApplication> {
    val search = query.trim()
    return apps.filter { app ->
        (showSystem || !app.isSystem) &&
            (category == SusfsAppCategory.All || app.isRisk == (category == SusfsAppCategory.Risk)) &&
            filter.matches(app.policy) &&
            (search.isEmpty() || app.label.contains(search, true) ||
                app.packageName.contains(search, true) || app.uid.toString().contains(search))
    }
}

internal data class SusfsApplicationsState(
    val apps: List<SusfsApplication> = emptyList(),
    val loaded: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val preview: SusfsImportPlan? = null,
    val report: SusfsApplyReport? = null,
    val hasBackup: Boolean = false,
)

internal class SusfsApplicationsViewModel : ViewModel() {
    private val repository = SusfsApplicationRepository(ksuApp)
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(SusfsApplicationsState())
    val state = mutableState.asStateFlow()
    // Prevent a lifecycle refresh from racing the SAF result callback.
    var choosingDocument = false

    private fun operate(queue: Boolean = false, block: suspend () -> Unit) {
        if (!queue && !mutex.tryLock()) return
        if (!queue) mutableState.update { it.copy(busy = true, error = null, notice = null) }
        viewModelScope.launch {
            // SAF results may arrive during the initial lifecycle refresh after process recreation.
            // Keep the explicitly selected document instead of silently discarding the callback.
            if (queue) {
                mutex.lock()
                mutableState.update { it.copy(busy = true, error = null, notice = null) }
            }
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableState.update { it.copy(error = failure.message ?: failure.javaClass.simpleName) }
            } finally {
                mutableState.update { it.copy(busy = false) }
                mutex.unlock()
            }
        }
    }

    private suspend fun load() {
        try {
            val apps = repository.load()
            mutableState.update { it.copy(apps = apps, loaded = true) }
        } catch (failure: Exception) {
            if (failure is CancellationException) throw failure
            mutableState.update { state ->
                state.copy(apps = state.apps.map { it.copy(policy = SusfsPolicyState(error = "refresh_failed")) })
            }
            throw failure
        }
    }

    fun refresh() = operate {
        load()
        mutableState.update { it.copy(preview = null) }
        updateRecovery()
    }

    fun onResume() {
        if (choosingDocument || state.value.preview != null) return
        refresh()
    }

    private suspend fun updateRecovery() {
        val backup = repository.hasBackup()
        // A damaged diagnostics file must not hide an otherwise valid recovery snapshot.
        mutableState.update { it.copy(hasBackup = backup) }
        val report = repository.lastReport()
        mutableState.update { it.copy(report = report) }
    }

    fun prepareImport(uri: Uri) = operate(queue = true) {
        val entries = repository.prepare(uri)
        load()
        mutableState.update { it.copy(preview = planSusfsImport(entries, it.apps.distinctBy { app -> app.uid }.map { app -> app.target })) }
    }

    fun prepareRestore() = operate {
        val entries = repository.recoveryEntries()
        load()
        mutableState.update { it.copy(preview = planSusfsImport(entries, it.apps.distinctBy { app -> app.uid }.map { app -> app.target }, restoring = true)) }
    }

    fun dismissPreview() {
        if (!state.value.busy) mutableState.update { it.copy(preview = null) }
    }

    fun applyPreview() {
        val plan = state.value.preview ?: return
        operate {
            try {
                val report = repository.apply(plan)
                mutableState.update { it.copy(report = report, preview = null, notice = "applied") }
            } finally {
                // Re-read only the policies, not PackageManager metadata, including partial failures.
                val apps = repository.refreshPolicies(state.value.apps)
                mutableState.update { it.copy(apps = apps, preview = null) }
                updateRecovery()
            }
        }
    }

    fun change(app: SusfsApplication, mode: SusfsHideMode) = operate {
        val entries = listOf(SusfsApplicationHidingConfigEntry(app.packageName, mode))
        val plan = planSusfsImport(entries, listOf(app.target))
        if (!plan.canApply) return@operate
        try {
            val report = repository.apply(plan)
            mutableState.update { it.copy(report = report, notice = "applied") }
        } finally {
            val policy = repository.readUid(app.target)
            mutableState.update { it.copy(apps = updateSusfsUid(it.apps, app.uid, policy)) }
            updateRecovery()
        }
    }

    fun export(uri: Uri) = operate(queue = true) {
        load()
        repository.export(uri, state.value.apps)
        mutableState.update { it.copy(notice = "exported") }
    }
}
