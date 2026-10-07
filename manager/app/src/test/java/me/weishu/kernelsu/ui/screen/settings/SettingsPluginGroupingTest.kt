package me.weishu.kernelsu.ui.screen.settings

import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.util.ManagerPlugin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPluginGroupingTest {
    private val installed = SettingsUiState(
        runtimeModeResolved = true,
        pluginCompatibilityResolved = true,
        installedPluginIds = ManagerPlugin.entries.map { it.id }.toSet(),
        graphicsRendererFeatureEnabled = true,
    )

    private fun allEntries(state: SettingsUiState) =
        SettingsCategory.entries.flatMap { SettingsCatalog.entriesFor(it, state) }

    @Test
    fun everyDownloadablePluginIsRepresentedOnlyInPlugins() {
        val entries = allEntries(installed) + allEntries(installed.copy(isLkmMode = true))
        val plugins = entries.filter { it.plugin != null }
        assertEquals(ManagerPlugin.entries.toSet(), plugins.mapNotNull { it.plugin }.toSet())
        assertTrue(plugins.all { it.category == SettingsCategory.Plugins })
    }

    @Test
    fun builtInCategoriesNeverChangeWhenPluginsAreInstalled() {
        for (lkm in listOf(false, true)) {
            val available = installed.copy(isLkmMode = lkm)
            val empty = available.copy(installedPluginIds = emptySet())
            SettingsCategory.entries.filter { it != SettingsCategory.Plugins }.forEach { category ->
                assertEquals(
                    SettingsCatalog.entriesFor(category, empty).map { it.key },
                    SettingsCatalog.entriesFor(category, available).map { it.key },
                )
            }
        }
    }

    @Test
    fun uninstallAndIncompatibilityRemoveEveryEntryOfThePlugin() {
        val lkm = installed.copy(isLkmMode = true)
        ManagerPlugin.entries.forEach { plugin ->
            val without = lkm.copy(installedPluginIds = lkm.installedPluginIds - plugin.id)
            val incompatible = lkm.copy(incompatiblePluginIds = setOf(plugin.id))
            assertFalse(allEntries(without).any { it.plugin == plugin })
            assertFalse(allEntries(incompatible).any { it.plugin == plugin })
            assertEquals(
                allEntries(lkm).filter { it.plugin != plugin }.map { it.key },
                allEntries(without).map { it.key },
            )
        }
    }

    @Test
    fun everySettingHasOneCategoryAcrossStylesAndRuntimeModes() {
        InterfaceStyle.entries.forEach { style ->
            for (lkm in listOf(false, true)) {
                for (lateLoad in listOf(false, true)) {
                    val state = installed.copy(uiMode = style.value, isLkmMode = lkm, isLateLoadMode = lateLoad)
                    val keys = allEntries(state).map { it.key }
                    assertEquals(keys.size, keys.distinct().size)
                    assertTrue(SettingsCatalog.entriesFor(SettingsCategory.Plugins, state).none { it.key == "kpm" })
                }
            }
        }
    }

    @Test
    fun pathmaskWaitsForLkmDetectionAndNeverAppearsInLateLoadMode() {
        val lkm = installed.copy(isLkmMode = true)
        assertTrue(allEntries(lkm).any { it.key == "pathmask_lkm" })
        assertFalse(allEntries(lkm.copy(runtimeModeResolved = false)).any { it.key == "pathmask_lkm" })
        assertFalse(allEntries(lkm.copy(isLateLoadMode = true)).any { it.key == "pathmask_lkm" })
        assertFalse(allEntries(lkm).any { it.key == "path_config" })
        assertTrue(allEntries(installed).any { it.key == "path_config" && it.plugin == null })
    }

    @Test
    fun emptyPluginsSectionHasNoStoreEntry() {
        val unavailable = installed.copy(incompatiblePluginIds = installed.installedPluginIds)
        assertTrue(SettingsCatalog.entriesFor(SettingsCategory.Plugins, unavailable).isEmpty())
    }
}
