package me.weishu.kernelsu.ui.screen.settings

import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.util.ManagerPlugin
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsCatalogTest {
    @Test
    fun pluginCountTracksInstalledAndEnabledEntries() {
        val base = SettingsUiState(uiMode = InterfaceStyle.Material.value)
        assertEquals(0, SettingsCatalog.visibleEntryCount(SettingsCategory.Plugins, base))
        val installed = base.copy(installedPluginIds = ManagerPlugin.entries.map { it.id }.toSet())
        assertEquals(12, SettingsCatalog.visibleEntryCount(SettingsCategory.Plugins, installed))
        assertEquals(
            13,
            SettingsCatalog.visibleEntryCount(
                SettingsCategory.Plugins,
                installed.copy(graphicsRendererFeatureEnabled = true),
            ),
        )
        val kpm = base.copy(
            runtimeModeResolved = true,
            isLkmMode = true,
            kpmBackend = "kpatch-next",
            isKpmManagementAvailable = true,
            isKpmCapabilityResolved = true,
        )
        assertEquals(0, SettingsCatalog.visibleEntryCount(SettingsCategory.Plugins, kpm))
        assertEquals(true, SettingsCatalog.entriesFor(SettingsCategory.MountAndHide, kpm).any { it.key == "kpm" })
    }

    @Test
    fun appearanceCountTracksInterfaceSpecificEntry() {
        val base = SettingsUiState(uiMode = InterfaceStyle.Material.value)
        assertEquals(4, SettingsCatalog.visibleEntryCount(SettingsCategory.Appearance, base))
        assertEquals(
            5,
            SettingsCatalog.visibleEntryCount(
                SettingsCategory.Appearance,
                base.copy(uiMode = InterfaceStyle.Miuix.value),
            ),
        )
        assertEquals(
            5,
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
        assertEquals(emptyList<String>(), SettingsCatalog.entriesFor(SettingsCategory.Plugins, base).map { it.key })
        val installed = base.copy(installedPluginIds = setOf(ManagerPlugin.RemoteManagementSuite.id))
        assertEquals(4, SettingsCatalog.visibleEntryCount(SettingsCategory.Plugins, installed))
        assertEquals(
            listOf("web_debugging"),
            SettingsCatalog.entriesFor(SettingsCategory.AppAndMaintenance, installed)
                .filter { it.key.startsWith("web_") || it.key.startsWith("stealth_") }
                .map { it.key },
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
            SettingsCategory.Appearance,
            SettingsCatalog.entriesFor(SettingsCategory.Appearance, base)
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
    fun lkmPathConfigAppearsOnlyInPluginsAfterInstallation() {
        val lkm = SettingsUiState(isLkmMode = true, runtimeModeResolved = true)
        val installed = lkm.copy(installedPluginIds = setOf(ManagerPlugin.PathmaskLkm.id))
        assertEquals(false, SettingsCatalog.entriesFor(SettingsCategory.MountAndHide, installed).any { it.key == "path_config" })
        assertEquals(false, SettingsCatalog.entriesFor(SettingsCategory.Plugins, lkm).any { it.key == "pathmask_lkm" })
        assertEquals(true, SettingsCatalog.entriesFor(SettingsCategory.Plugins, installed).any { it.key == "pathmask_lkm" })
        assertEquals(true, SettingsCatalog.entriesFor(SettingsCategory.MountAndHide, lkm.copy(isLkmMode = false)).any { it.key == "path_config" })
        assertEquals(false, SettingsCatalog.entriesFor(SettingsCategory.Plugins, installed.copy(isLkmMode = false)).any { it.key == "pathmask_lkm" })
    }

}
