package me.weishu.kernelsu.ui.screen.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsCategoryTest {
    @Test
    fun routeValuesRestoreEveryCategory() {
        SettingsCategory.entries.forEach { category ->
            assertEquals(category, SettingsCategory.fromRouteValue(category.routeValue))
        }
    }

    @Test
    fun missingOrUnknownRouteFallsBackToAppearance() {
        assertEquals(SettingsCategory.Appearance, SettingsCategory.fromRouteValue(null))
        assertEquals(SettingsCategory.Appearance, SettingsCategory.fromRouteValue("unknown"))
    }

    @Test
    fun settingsPageModeRestoresSavedValueAndDefaultsToCategories() {
        SettingsPageMode.entries.forEach { mode ->
            assertEquals(mode, SettingsPageMode.fromValue(mode.value))
        }
        assertEquals(SettingsPageMode.entries.size, SettingsPageMode.entries.map { it.value }.distinct().size)
        assertEquals(SettingsPageMode.Categories, SettingsPageMode.fromValue(null))
        assertEquals(SettingsPageMode.Categories, SettingsPageMode.fromValue("unknown"))
    }

    @Test
    fun collapsedModeIsRemovedAndSavedValuesMigrateToCategories() {
        assertEquals(listOf(SettingsPageMode.Categories, SettingsPageMode.Overview), SettingsPageMode.entries)
        assertEquals(SettingsPageMode.Categories, SettingsPageMode.fromValue("collapsed"))
    }

    @Test
    fun formerPluginCategoriesRestoreToTheSinglePluginsSection() {
        assertEquals(SettingsCategory.Plugins, SettingsCategory.fromRouteValue("toolbox"))
        assertEquals(SettingsCategory.Plugins, SettingsCategory.fromRouteValue("web_privacy"))
        assertEquals(SettingsCategory.Plugins, SettingsCategory.fromRouteValue("plugins"))
        assertEquals(6, SettingsCategory.entries.size)
    }

    @Test
    fun overviewProvidesExactlyOneEntryForEveryCategory() {
        val overviewCategories = overviewPrimaryCategories + overviewQuickCategories
        assertEquals(SettingsCategory.entries.size, overviewCategories.size)
        assertEquals(SettingsCategory.entries.toSet(), overviewCategories.toSet())
    }
}
