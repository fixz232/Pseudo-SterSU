package me.weishu.kernelsu.ui.screen.settings

import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.util.ManagerPlugin
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsCatalogTest {
    @Test
    fun toolboxCountTracksDynamicEntries() {
        val base = SettingsUiState(uiMode = InterfaceStyle.Material.value)
        assertEquals(1, SettingsCatalog.visibleEntryCount(SettingsCategory.Toolbox, base))
        val installed = base.copy(installedPluginIds = ManagerPlugin.entries.map { it.id }.toSet())
        assertEquals(7, SettingsCatalog.visibleEntryCount(SettingsCategory.Toolbox, installed))
        assertEquals(
            8,
            SettingsCatalog.visibleEntryCount(
                SettingsCategory.Toolbox,
                installed.copy(graphicsRendererFeatureEnabled = true),
            ),
        )
        assertEquals(
            2,
            SettingsCatalog.visibleEntryCount(
                SettingsCategory.Toolbox,
                base.copy(
                    runtimeModeResolved = true,
                    isLkmMode = true,
                    isKPatchNextEnabled = true,
                    kpmBackend = "kpatch-next",
                    isKpmManagementAvailable = true,
                    isKpmCapabilityResolved = true,
                ),
            ),
        )
    }

    @Test
    fun appearanceCountTracksInterfaceSpecificEntry() {
        val base = SettingsUiState(uiMode = InterfaceStyle.Material.value)
        assertEquals(5, SettingsCatalog.visibleEntryCount(SettingsCategory.Appearance, base))
        assertEquals(
            6,
            SettingsCatalog.visibleEntryCount(
                SettingsCategory.Appearance,
                base.copy(uiMode = InterfaceStyle.Miuix.value),
            ),
        )
        assertEquals(
            6,
            SettingsCatalog.visibleEntryCount(
                SettingsCategory.Appearance,
                base.copy(uiMode = InterfaceStyle.Pixel.value),
            ),
        )
    }

    @Test
    fun sidebarDesignIsDiscoverableFromAppearanceForEveryStyle() {
        for (style in InterfaceStyle.entries) {
            val entries = SettingsCatalog.entriesFor(SettingsCategory.Appearance, SettingsUiState(uiMode = style.value))
            assertEquals(1, entries.count { it.key == "sidebar_design" })
        }
    }

    @Test
    fun rootCountIncludesSoftReboot() {
        assertEquals(
            9,
            SettingsCatalog.visibleEntryCount(SettingsCategory.RootAndPermissions, SettingsUiState()),
        )
    }

    @Test
    fun homeAndManagerCountIncludesDynamicManager() {
        assertEquals(
            6,
            SettingsCatalog.visibleEntryCount(SettingsCategory.HomeAndManager, SettingsUiState()),
        )
    }
}
