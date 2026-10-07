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
        assertEquals(3, SettingsCatalog.visibleEntryCount(SettingsCategory.Appearance, base))
        assertEquals(
            4,
            SettingsCatalog.visibleEntryCount(
                SettingsCategory.Appearance,
                base.copy(uiMode = InterfaceStyle.Miuix.value),
            ),
        )
        assertEquals(
            4,
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
    fun rootAndMountControlsUseTheirOwnCategories() {
        val base = SettingsUiState()
        assertEquals(
            setOf("profile_template", "su_compat", "su_log", "adb_root", "soft_reboot", "auto_jailbreak"),
            SettingsCatalog.entriesFor(SettingsCategory.RootAndPermissions, base).map { it.key }.toSet(),
        )
        assertEquals(
            setOf("kernel_umount", "webview_umount", "default_umount", "selinux_hide", "avc_spoof", "path_config", "apkesu_hide"),
            SettingsCatalog.entriesFor(SettingsCategory.MountAndHide, base).map { it.key }.toSet(),
        )
    }

    @Test
    fun webAndStealthEntriesFollowPluginAvailability() {
        val base = SettingsUiState()
        assertEquals(
            listOf("web_debugging"),
            SettingsCatalog.entriesFor(SettingsCategory.WebAndPrivacy, base).map { it.key },
        )
        val installed = base.copy(installedPluginIds = setOf(ManagerPlugin.RemoteManagementSuite.id))
        assertEquals(5, SettingsCatalog.visibleEntryCount(SettingsCategory.WebAndPrivacy, installed))
        assertEquals(
            false,
            SettingsCatalog.entriesFor(SettingsCategory.AppAndMaintenance, installed)
                .any { it.key.startsWith("web_") || it.key.startsWith("stealth_") },
        )
    }

    @Test
    fun unifiedStoreHasOneCategoryEntry() {
        val base = SettingsUiState()
        assertEquals(
            listOf("store"),
            SettingsCategory.entries.flatMap { SettingsCatalog.entriesFor(it, base) }
                .filter { it.key == "store" }
                .map { it.key },
        )
        assertEquals(
            SettingsCategory.Toolbox,
            SettingsCatalog.entriesFor(SettingsCategory.Toolbox, base)
                .single { it.key == "store" }.category,
        )
    }

    @Test
    fun homeAndManagerCountIncludesDynamicManager() {
        assertEquals(
            5,
            SettingsCatalog.visibleEntryCount(SettingsCategory.HomeAndManager, SettingsUiState()),
        )
        assertEquals(
            false,
            SettingsCatalog.entriesFor(SettingsCategory.HomeAndManager, SettingsUiState())
                .any { it.key == "home_layout" },
        )
    }

    @Test
    fun mountAndHideDoesNotOfferBundledKpatchNext() {
        val entries = SettingsCatalog.entriesFor(SettingsCategory.MountAndHide, SettingsUiState())
        assertEquals(false, entries.any { it.key == "kpatch_next" || it.key == "kpatch_webui" })
    }

    @Test
    fun lkmPathConfigAppearsOnlyAfterInstallingItsPlugin() {
        val lkm = SettingsUiState(isLkmMode = true, runtimeModeResolved = true)
        assertEquals(false, SettingsCatalog.entriesFor(SettingsCategory.MountAndHide, lkm).any { it.key == "path_config" })
        assertEquals(
            true,
            SettingsCatalog.entriesFor(
                SettingsCategory.MountAndHide,
                lkm.copy(installedPluginIds = setOf(ManagerPlugin.PathmaskLkm.id)),
            ).any { it.key == "path_config" },
        )
        assertEquals(
            true,
            SettingsCatalog.entriesFor(
                SettingsCategory.MountAndHide,
                lkm.copy(isLkmMode = false),
            ).any { it.key == "path_config" },
        )
    }
}
