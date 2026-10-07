package me.weishu.kernelsu.ui.screen.settings

import me.weishu.kernelsu.ui.util.ManagerPlugin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsFeatureAvailabilityTest {
    @Test
    fun lateLoadModeDisablesPathConfig() {
        val state = SettingsUiState(
            isLkmMode = false,
            isLateLoadMode = true,
            runtimeModeResolved = true,
        )

        assertEquals(PathConfigBackend.Disabled, state.pathConfigBackend)
        assertFalse(state.canOpenPathConfig)
    }

    @Test
    fun lkmModeNeedsPathmaskPlugin() {
        val state = SettingsUiState(isLkmMode = true, runtimeModeResolved = true)

        assertEquals(PathConfigBackend.PathmaskLkm, state.pathConfigBackend)
        assertFalse(state.canOpenPathConfig)
        assertFalse(state.isPathConfigEntryVisible)
        val installed = state.copy(installedPluginIds = setOf(ManagerPlugin.PathmaskLkm.id))
        assertTrue(installed.canOpenPathConfig)
        assertTrue(installed.isPathConfigEntryVisible)
        assertFalse(state.isGkiMode)
    }

    @Test
    fun lateLoadModeWinsWhenBothModeFlagsAreSet() {
        val state = SettingsUiState(
            isLkmMode = true,
            isLateLoadMode = true,
            runtimeModeResolved = true,
        )

        assertEquals(PathConfigBackend.Disabled, state.pathConfigBackend)
        assertFalse(state.canOpenPathConfig)
    }

    @Test
    fun builtInGkiModeUsesSusfsConfig() {
        val state = SettingsUiState(
            isLkmMode = false,
            isLateLoadMode = false,
            runtimeModeResolved = true,
        )

        assertEquals(PathConfigBackend.SusfsGki, state.pathConfigBackend)
        assertTrue(state.canOpenPathConfig)
        assertTrue(state.isPathConfigEntryVisible)
        assertTrue(state.isGkiMode)
    }

    @Test
    fun nativeGkiCapabilityShowsItsOwnKpmEntry() {
        val state = SettingsUiState(
            isLkmMode = false,
            isLateLoadMode = false,
            runtimeModeResolved = true,
            kpmBackend = "native-gki",
            isKpmManagementAvailable = true,
            isKpmCapabilityResolved = true,
        )

        assertTrue(state.isKpmSettingsEntryVisible)
    }

    @Test
    fun lkmKpmBackendCannotExposeEntryInGkiMode() {
        val state = SettingsUiState(
            isLkmMode = false,
            isLateLoadMode = false,
            runtimeModeResolved = true,
            kpmBackend = "kpatch-next",
            isKpmManagementAvailable = true,
            isKpmCapabilityResolved = true,
        )

        assertFalse(state.isKpmSettingsEntryVisible)
    }

    @Test
    fun independentlyInstalledLkmBackendExposesKpmEntry() {
        val state = SettingsUiState(
            isLkmMode = true,
            runtimeModeResolved = true,
            kpmBackend = "kpatch-next",
            isKpmManagementAvailable = true,
            isKpmCapabilityResolved = true,
        )

        assertTrue(state.isKpmSettingsEntryVisible)
    }

    @Test
    fun unresolvedKpmCapabilityDoesNotExposeSettingsEntry() {
        val state = SettingsUiState(
            isLkmMode = false,
            isLateLoadMode = false,
            runtimeModeResolved = true,
            kpmBackend = "native-gki",
            isKpmManagementAvailable = true,
        )

        assertFalse(state.isKpmSettingsEntryVisible)
    }

    @Test
    fun unresolvedModeKeepsRuntimeSpecificEntriesDisabled() {
        val state = SettingsUiState()

        assertEquals(PathConfigBackend.Unknown, state.pathConfigBackend)
        assertFalse(state.canOpenPathConfig)
    }
}
