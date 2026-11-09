package me.weishu.kernelsu.ui.viewmodel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.model.AppInfo
import me.weishu.kernelsu.data.repository.SuperUserRepository
import me.weishu.kernelsu.data.repository.SuperUserRepositoryImpl
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.component.SearchStatus
import me.weishu.kernelsu.ui.screen.superuser.GroupedApps
import me.weishu.kernelsu.ui.screen.superuser.SuperUserUiState
import me.weishu.kernelsu.ui.util.PinyinUtil
import me.weishu.kernelsu.ui.util.ensureManagerRegistered
import me.weishu.kernelsu.ui.util.getSepolicy
import me.weishu.kernelsu.ui.util.ownerNameForUid
import me.weishu.kernelsu.ui.util.pickPrimary
import me.weishu.kernelsu.ui.util.setSepolicy
import java.text.Collator
import java.util.Locale

internal const val RECENTLY_INSTALLED_WINDOW_MILLIS = 60 * 60 * 1000L

private const val PREFS_SORT_OPTION = "superuser_sort_option"
private const val PREFS_RECENT_SORT_DEFAULT = "superuser_recent_sort_default_applied"

internal fun buildRecentlyInstalledGroups(
    groups: List<GroupedApps>,
    nowMillis: Long = System.currentTimeMillis(),
): List<GroupedApps> {
    val cutoffMillis = nowMillis - RECENTLY_INSTALLED_WINDOW_MILLIS

    return groups.mapNotNull { group ->
        val latestInstallTime = group.apps.maxOfOrNull { it.packageInfo.firstInstallTime } ?: return@mapNotNull null
        if (latestInstallTime < cutoffMillis) {
            null
        } else {
            group to latestInstallTime
        }
    }.sortedWith(
        compareByDescending<Pair<GroupedApps, Long>> { it.second }
            .thenBy { it.first.primary.label.lowercase() }
    ).map { it.first }
}

class SuperUserViewModel(
    private val repo: SuperUserRepository = SuperUserRepositoryImpl()
) : ViewModel() {

    companion object {
        private const val TAG = "SuperUserViewModel"

        private val appsLock = Any()
        private var cachedApps: List<AppInfo> = emptyList()
        private var cachedUserIds: List<Int> = emptyList()
        private var appListScan: Deferred<Result<Pair<List<AppInfo>, List<Int>>>>? = null
        private val scanScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val groupedAppsLock = Any()
        private var cachedGroupedApps: List<GroupedApps> = emptyList()

        val apps: List<AppInfo>
            get() = synchronized(appsLock) { cachedApps }

        // Callers join a scan in flight instead of starting a parallel one. useCache = false
        // still rescans when none is running, so a fresh screen sees newly installed apps.
        suspend fun loadApps(
            useCache: Boolean,
            repo: SuperUserRepository = SuperUserRepositoryImpl(),
        ): Result<Pair<List<AppInfo>, List<Int>>> {
            val scan = synchronized(appsLock) {
                if (useCache && cachedApps.isNotEmpty()) return Result.success(cachedApps to cachedUserIds)
                appListScan?.takeIf { it.isActive } ?: scanScope.async {
                    repo.getAppList()
                        .onSuccess { (apps, ids) ->
                            synchronized(appsLock) {
                                cachedApps = apps
                                cachedUserIds = ids
                            }
                        }
                        .onFailure { Log.e(TAG, "loadApps failed", it) }
                }.also { appListScan = it }
            }
            return scan.await()
        }

        @JvmStatic
        fun getGroupedApp(uid: Int): GroupedApps? {
            return synchronized(groupedAppsLock) { cachedGroupedApps.find { it.uid == uid } }
        }
    }

    private val _uiState = MutableStateFlow(SuperUserUiState())
    val uiState: StateFlow<SuperUserUiState> = _uiState.asStateFlow()
    private val prefs = ksuApp.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val refreshMutex = Mutex()
    private val searchQuery = MutableStateFlow("")
    private var sortJob: Job? = null
    var isNeedRefresh = false
        private set

    init {
        viewModelScope.launchSearchQueryCollector(searchQuery, ::applySearchText)
    }

    fun markNeedRefresh() {
        isNeedRefresh = true
    }

    fun setRootAccess(group: GroupedApps, enabled: Boolean) {
        val uid = group.uid
        if (uid in _uiState.value.updatingRootUids || group.anyAllowSu == enabled) return
        if (group.primary.special || (enabled && uid < 2000 && uid != 1000)) {
            Toast.makeText(ksuApp, ksuApp.getString(R.string.su_not_allowed, group.primary.label), Toast.LENGTH_SHORT).show()
            return
        }

        _uiState.update { it.copy(updatingRootUids = it.updatingRootUids + uid) }
        viewModelScope.launch {
            val updated = withContext(Dispatchers.IO) {
                runCatching {
                    val key = group.primary.profileKey
                    val current = Natives.getAppProfileStrict(key, uid)
                        ?: if (!group.anyAllowSu && !group.anyCustom) Natives.Profile(key, uid) else return@runCatching false
                    val policyKey = current.name
                    val previousRules = if (current.rootUseDefault) "" else getSepolicy(policyKey)
                    val desiredRules = if (enabled) previousRules else ""
                    val updateRules = !current.rootUseDefault && (!enabled || previousRules.isNotEmpty())
                    if (updateRules && !setSepolicy(policyKey, desiredRules)) return@runCatching false

                    val profile = current.copy(allowSu = enabled)
                    val saved = Natives.setAppProfile(profile) ||
                        (ensureManagerRegistered() && Natives.setAppProfile(profile))
                    if (!saved && updateRules) setSepolicy(policyKey, previousRules)
                    saved
                }.onFailure { Log.e(TAG, "Failed to change root access for uid $uid", it) }
                    .getOrDefault(false)
            }
            if (updated) {
                refreshAppList(resort = false)
            } else {
                Toast.makeText(
                    ksuApp,
                    ksuApp.getString(R.string.failed_to_update_app_profile, group.primary.label),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }.invokeOnCompletion {
            _uiState.update { it.copy(updatingRootUids = it.updatingRootUids - uid) }
        }
    }

    fun initializePreferences() {
        val showSystemApps = prefs.getBoolean("show_system_apps", false)
        val showOnlyPrimaryUserApps = prefs.getBoolean("show_only_primary_user_apps", false)
        val recentDefaultApplied = prefs.getBoolean(PREFS_RECENT_SORT_DEFAULT, false)
        val sortOption = resolveSuperUserSortOption(
            savedOption = prefs.getInt(PREFS_SORT_OPTION, DEFAULT_SUPERUSER_SORT_OPTION),
            recentDefaultApplied = recentDefaultApplied,
        )
        if (!recentDefaultApplied) {
            // Apply the new default once; later manual sorting choices stay intact.
            prefs.edit {
                putInt(PREFS_SORT_OPTION, sortOption)
                putBoolean(PREFS_RECENT_SORT_DEFAULT, true)
            }
        }
        _uiState.update {
            it.copy(
                showSystemApps = showSystemApps,
                showOnlyPrimaryUserApps = showOnlyPrimaryUserApps,
                sortOption = sortOption,
            )
        }
    }

    fun updateSortOption(option: Int): Job {
        prefs.edit {
            putInt(PREFS_SORT_OPTION, option)
            putBoolean(PREFS_RECENT_SORT_DEFAULT, true)
        }
        _uiState.update { it.copy(sortOption = option) }
        sortJob?.cancel()
        return viewModelScope.launch {
            val current = _uiState.value.groupedApps
            if (current.isEmpty()) return@launch
            updateVisibleApps(current)
        }.also { sortJob = it }
    }

    private fun refilterVisibleApps(): Job = viewModelScope.launch {
        // Re-filter when a filter setting changes
        val grouped = withContext(Dispatchers.IO) {
            buildGroups(filterAndSort(apps))
        }
        updateVisibleApps(grouped)
    }

    fun toggleShowSystemApps(): Job {
        val newValue = !_uiState.value.showSystemApps
        prefs.edit { putBoolean("show_system_apps", newValue) }
        _uiState.update { it.copy(showSystemApps = newValue) }
        return refilterVisibleApps()
    }

    fun toggleShowOnlyPrimaryUserApps(): Job {
        val newValue = !_uiState.value.showOnlyPrimaryUserApps
        prefs.edit { putBoolean("show_only_primary_user_apps", newValue) }
        _uiState.update { it.copy(showOnlyPrimaryUserApps = newValue) }
        return refilterVisibleApps()
    }

    fun updateSearchStatus(status: SearchStatus) {
        val previous = _uiState.value.searchStatus
        _uiState.update { it.copy(searchStatus = status) }
        if (previous.searchText != status.searchText) {
            searchQuery.value = status.searchText
        }
    }

    fun updateSearchText(text: String) {
        updateSearchStatus(_uiState.value.searchStatus.copy(searchText = text))
    }

    private fun filterSearchResults(groups: List<GroupedApps>, text: String): List<GroupedApps> {
        if (text.isEmpty()) return emptyList()

        return groups.mapNotNull { group ->
            val uidMatched = group.uid.toString().contains(text, true)
            val matchedIdentifiers = group.apps.filter {
                uidMatched ||
                it.label.contains(text, true) ||
                        it.displayIdentifier.contains(text, true) ||
                        PinyinUtil.toPinyin(it.label).contains(text, true)
            }.mapTo(linkedSetOf()) { it.displayIdentifier }

            if (matchedIdentifiers.isEmpty()) {
                null
            } else {
                val sortedApps = group.apps.sortedWith(
                    compareByDescending { it.displayIdentifier in matchedIdentifiers }
                )
                group.copy(
                    apps = sortedApps,
                    matchedIdentifiers = matchedIdentifiers,
                )
            }
        }
    }

    private suspend fun applySearchText(text: String) {
        _uiState.update {
            it.copy(
                searchStatus = it.searchStatus.copy(
                    resultStatus = searchLoadingStatusFor(text)
                )
            )
        }

        if (text.isEmpty()) {
            _uiState.update { state ->
                state.copy(
                    searchResults = emptyList(),
                    searchStatus = state.searchStatus.copy(resultStatus = SearchStatus.ResultStatus.DEFAULT)
                )
            }
            return
        }

        val result = withContext(Dispatchers.IO) {
            filterSearchResults(_uiState.value.groupedApps, text)
        }

        _uiState.update {
            it.copy(
                searchResults = result,
                searchStatus = it.searchStatus.copy(resultStatus = searchResultStatusFor(text, result.isEmpty()))
            )
        }
    }

    private fun updateCachedGroupedApps(grouped: List<GroupedApps>) {
        synchronized(groupedAppsLock) {
            cachedGroupedApps = grouped
        }
    }

    private suspend fun updateVisibleApps(grouped: List<GroupedApps>, resort: Boolean = true) {
        val sortOption = _uiState.value.sortOption
        val searchText = _uiState.value.searchStatus.searchText
        val (sorted, searchResults, recentlyInstalledResults) = withContext(Dispatchers.IO) {
            val ordered = if (resort) sortGroups(grouped, sortOption) else grouped
            val s = moveMostRecentlyInstalledFirst(ordered) { group ->
                group.apps.maxOfOrNull { it.packageInfo.firstInstallTime } ?: group.primary.packageInfo.firstInstallTime
            }
            Triple(s, filterSearchResults(s, searchText), buildRecentlyInstalledGroups(s))
        }
        _uiState.update {
            it.copy(
                groupedApps = sorted,
                recentlyInstalledResults = recentlyInstalledResults,
                searchResults = searchResults,
                searchStatus = it.searchStatus.copy(
                    resultStatus = searchResultStatusFor(searchText, searchResults.isEmpty())
                )
            )
        }
    }

    private fun filterAndSort(list: List<AppInfo>): List<AppInfo> {
        val comparator = compareBy<AppInfo> {
            when {
                it.allowSu -> 0
                it.hasCustomProfile -> 1
                else -> 2
            }
        }.then(compareBy(Collator.getInstance(Locale.getDefault()), AppInfo::label))

        val currentState = _uiState.value

        return list.filter {
            if (it.packageName == ksuApp.packageName) return@filter false
            // UID 1053 is the single system-wide WebView zygote (primary user only).
            if (it.isWebViewZygote) return@filter true
            if (it.allowSu || it.hasCustomProfile) {
                return@filter true
            }
            val userFilter = !currentState.showOnlyPrimaryUserApps || it.uid / 100000 == 0
            val appInfo = it.packageInfo.applicationInfo ?: return@filter false
            val isSystemApp = appInfo.flags.and(ApplicationInfo.FLAG_SYSTEM) != 0
            val typeFilter = it.uid == 2000
                    || currentState.showSystemApps
                    || !isSystemApp
            userFilter && typeFilter
        }.sortedWith(comparator)
    }

    private fun buildCachedGroups(apps: List<AppInfo>): List<GroupedApps> {
        return buildGroups(apps.filter { it.packageName != ksuApp.packageName })
    }

    private fun buildGroups(
        apps: List<AppInfo>,
        umount: (Int) -> Boolean = {
            runCatching { Natives.uidShouldUmount(it) }.getOrDefault(false)
        },
    ): List<GroupedApps> {
        val collator = Collator.getInstance(Locale.getDefault())
        val comparator = compareBy<AppInfo> {
            when {
                it.allowSu -> 0
                it.hasCustomProfile -> 1
                else -> 2
            }
        }.then(Comparator { a, b -> collator.compare(a.label, b.label) })
        return apps.groupBy { it.uid }.map { (uid, list) ->
            val sorted = list.sortedWith(comparator)
            val primary = pickPrimary(sorted)
            val shouldUmount = umount(uid)
            val ownerName = if (sorted.size > 1) ownerNameForUid(uid, sorted) else null

            GroupedApps(
                uid = uid,
                apps = sorted,
                primary = primary,
                anyAllowSu = sorted.any { it.allowSu },
                anyCustom = sorted.any { it.hasCustomProfile },
                shouldUmount = shouldUmount,
                ownerName = ownerName
            )
        }
    }

    private fun groupRank(group: GroupedApps): Int = when {
        group.anyAllowSu -> 0
        group.anyCustom -> 1
        group.apps.size > 1 -> 2
        group.shouldUmount -> 4
        else -> 3
    }

    private fun sortGroups(groups: List<GroupedApps>, sortOption: Int): List<GroupedApps> {
        val sortType = sortOption / 2
        val collator = Collator.getInstance(Locale.getDefault())
        val base: Comparator<GroupedApps> = when (sortType) {
            SORT_BY_PACKAGE_NAME -> compareBy { it.primary.displayIdentifier }
            SORT_BY_INSTALL_TIME -> compareBy { group ->
                group.apps.maxOfOrNull { it.packageInfo.firstInstallTime } ?: group.primary.packageInfo.firstInstallTime
            }
            SORT_BY_UPDATE_TIME -> compareBy { group ->
                group.apps.maxOfOrNull { it.packageInfo.lastUpdateTime } ?: group.primary.packageInfo.lastUpdateTime
            }
            else -> Comparator { a, b -> collator.compare(a.primary.label, b.primary.label) }
        }
        return groups.sortedWith(superUserSortComparator(sortOption, base, ::groupRank))
    }

    suspend fun fetchAppList() {
        refreshMutex.withLock {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            loadApps(useCache = false, repo = repo).onSuccess { (newApps, ids) ->
                val (cachedGroups, grouped) = withContext(Dispatchers.IO) {
                    val cached = buildCachedGroups(newApps)
                    val umountByUid = cached.associate { it.uid to it.shouldUmount }
                    cached to buildGroups(filterAndSort(newApps)) {
                        umountByUid[it] ?: runCatching { Natives.uidShouldUmount(it) }.getOrDefault(false)
                    }
                }

                updateCachedGroupedApps(cachedGroups)
                updateVisibleApps(grouped)
                _uiState.update { it.copy(userIds = ids, isRefreshing = false, hasLoaded = true) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        hasLoaded = true,
                        error = e
                    )
                }
            }

            isNeedRefresh = false
        }
    }

    private suspend fun refreshAppList(resort: Boolean = true) {
        refreshMutex.withLock {
            val currentApps = synchronized(appsLock) { cachedApps }
            if (currentApps.isEmpty()) return

            repo.refreshProfiles(currentApps).onSuccess { updatedApps ->
                // Update cache for static method
                synchronized(appsLock) { cachedApps = updatedApps }

                val (cachedGroups, grouped) = withContext(Dispatchers.IO) {
                    val cached = buildCachedGroups(updatedApps)
                    val umountByUid = cached.associate { it.uid to it.shouldUmount }
                    val visible = buildGroups(filterAndSort(updatedApps)) {
                        umountByUid[it] ?: runCatching { Natives.uidShouldUmount(it) }.getOrDefault(false)
                    }
                    val result = if (resort) {
                        visible
                    } else {
                        val byUid = visible.associateBy { it.uid }
                        _uiState.value.groupedApps.mapNotNull { group -> byUid[group.uid] }
                    }
                    cached to result
                }
                updateCachedGroupedApps(cachedGroups)

                updateVisibleApps(grouped, resort = resort)
                _uiState.update { it.copy(isRefreshing = false) }
                isNeedRefresh = false
            }
        }
    }

    fun loadAppList(force: Boolean = false, resort: Boolean = true): Job {
        return viewModelScope.launch {
            if (force || _uiState.value.groupedApps.isEmpty()) {
                fetchAppList()
            } else {
                refreshAppList(resort)
            }
        }
    }

}
