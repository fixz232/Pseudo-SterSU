package me.weishu.kernelsu.ui.screen.settings

import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.util.InstalledInterfaceStyle
import me.weishu.kernelsu.ui.util.InterfaceStylePackage
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsInterfaceStyleSelectionTest {
    @Test
    fun legacyDownloadedSidebarUsesBuiltInPickerEntry() {
        val downloaded = listOf(
            installedStyle("spring", InterfaceStyle.Snow, "spring"),
            installedStyle("sidebar", InterfaceStyle.SidebarWidget),
        )
        val state = SettingsUiState(
            uiMode = InterfaceStyle.SidebarWidget.value,
            installedInterfaceStyles = downloaded,
        )
        assertEquals(InterfaceStyle.selectableEntries.indexOf(InterfaceStyle.SidebarWidget), state.interfaceStyleSelectedIndex())
    }

    @Test
    fun downloadedVariantsKeepTheirOwnPickerSelection() {
        val downloaded = listOf(
            installedStyle("spring", InterfaceStyle.Snow, "spring"),
            installedStyle("winter", InterfaceStyle.Snow, "winter"),
            installedStyle("pixel-one", InterfaceStyle.Pixel, "one"),
            installedStyle("pixel-two", InterfaceStyle.Pixel, "two"),
        )
        downloaded.forEachIndexed { index, installed ->
            val state = SettingsUiState(
                uiMode = installed.style.engine,
                seasonStyle = installed.style.variant.orEmpty(),
                pixelStyle = installed.style.variant.orEmpty(),
                installedInterfaceStyles = downloaded,
            )
            assertEquals(InterfaceStyle.selectableEntries.size + index, state.interfaceStyleSelectedIndex())
        }
    }

    @Test
    fun downloadsDoNotAddSeparateAppearanceCatalogEntries() {
        val base = SettingsUiState(uiMode = InterfaceStyle.SidebarWidget.value)
        val downloaded = base.copy(installedInterfaceStyles = listOf(
            installedStyle("sidebar", InterfaceStyle.SidebarWidget),
            installedStyle("spring", InterfaceStyle.Snow, "spring"),
        ))
        assertEquals(
            SettingsCatalog.entriesFor(SettingsCategory.Appearance, base).map { it.key },
            SettingsCatalog.entriesFor(SettingsCategory.Appearance, downloaded).map { it.key },
        )
        assertEquals(1, SettingsCatalog.entriesFor(SettingsCategory.Appearance, downloaded).count { it.key == "ui_style" })
    }

    @Test
    fun builtInSelectionIsUnchangedWhenDownloadsExist() {
        val downloaded = listOf(installedStyle("sidebar", InterfaceStyle.SidebarWidget))
        assertEquals(
            listOf(InterfaceStyle.Miuix, InterfaceStyle.Material, InterfaceStyle.SidebarWidget),
            InterfaceStyle.selectableEntries,
        )
        InterfaceStyle.selectableEntries.forEachIndexed { index, style ->
            assertEquals(index, SettingsUiState(
                uiMode = style.value,
                installedInterfaceStyles = downloaded,
            ).interfaceStyleSelectedIndex())
        }
    }

    @Test
    fun selectedPackageWinsWhenAlphaAndWindowsShareRenderer() {
        val downloaded = listOf(
            installedStyle("windows-fluent", InterfaceStyle.Alpha),
            installedStyle("alpha-delta", InterfaceStyle.Alpha),
        )
        val alpha = SettingsUiState(
            uiMode = InterfaceStyle.Alpha.value,
            activeInterfaceStyleId = "alpha-delta",
            installedInterfaceStyles = downloaded,
        )
        val delta = alpha.copy(uiMode = InterfaceStyle.Delta.value)
        assertEquals(InterfaceStyle.selectableEntries.size + 1, alpha.interfaceStyleSelectedIndex())
        assertEquals(InterfaceStyle.selectableEntries.size + 1, delta.interfaceStyleSelectedIndex())
    }

    private fun installedStyle(id: String, engine: InterfaceStyle, variant: String? = null) =
        InstalledInterfaceStyle(
            style = InterfaceStylePackage(
                id = id,
                name = id,
                summary = "Test style",
                engine = engine.value,
                variant = variant,
                version = 1,
                downloadUrl = "https://example.invalid/$id.ksstyle",
                sha256 = "0".repeat(64),
                sizeBytes = 1,
                accent = 0xFF222222,
            ),
            installedAt = 0,
        )
}
