package me.weishu.kernelsu.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.util.KernelSlotsResult
import me.weishu.kernelsu.ui.util.readSusfsKernelSlots

internal data class SusfsKernelSlotsState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val result: KernelSlotsResult = KernelSlotsResult(),
)

internal class SusfsKernelSlotsViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(SusfsKernelSlotsState())
    val state = mutableState.asStateFlow()

    fun load(refresh: Boolean = false) {
        if (state.value.loading || (!refresh && state.value.loaded)) return
        mutableState.value = SusfsKernelSlotsState(loading = true)
        viewModelScope.launch {
            val result = readSusfsKernelSlots(ksuApp.cacheDir)
            mutableState.value = SusfsKernelSlotsState(loaded = true, result = result)
        }
    }
}
