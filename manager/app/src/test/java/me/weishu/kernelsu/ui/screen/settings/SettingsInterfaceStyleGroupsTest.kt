package me.weishu.kernelsu.ui.screen.settings

import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.util.InstalledInterfaceStyle
import me.weishu.kernelsu.ui.util.InterfaceStylePackage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsInterfaceStyleGroupsTest {
    @Test
    fun seasonAndPixelPackagesHaveOneEntryPerFamily() {
        val state = SettingsUiState(installedInterfaceStyles = listOf(
            installed("spring", InterfaceStyle.Snow, "spring"),
            installed("three-kingdoms", InterfaceStyle.Pixel, "three_kingdoms"),
            installed("winter", InterfaceStyle.Snow, "winter"),
            installed("ink-jade", InterfaceStyle.Pixel, "ink_jade"),
        ))
        val groups = state.interfaceStylePickerGroups()
        assertEquals(InterfaceStyle.selectableEntries.size + 2, groups.size)
        assertEquals(1, groups.count { it.family == InterfaceStyle.Snow })
        assertEquals(1, groups.count { it.family == InterfaceStyle.Pixel })
        assertEquals(listOf(3, 5), groups.single { it.family == InterfaceStyle.Snow }.selectionIndices)
        assertEquals(listOf(4, 6), groups.single { it.family == InterfaceStyle.Pixel }.selectionIndices)
    }

    @Test
    fun groupingNeverAddsUninstalledSubstylesOrFamilies() {
        val state = SettingsUiState(installedInterfaceStyles = listOf(
            installed("winter", InterfaceStyle.Snow, "winter"),
        ))
        val groups = state.interfaceStylePickerGroups()
        assertFalse(groups.any { it.family == InterfaceStyle.Pixel })
        assertEquals(listOf(3), groups.single { it.family == InterfaceStyle.Snow }.selectionIndices)
        assertEquals((0..3).toList(), groups.flatMap { it.selectionIndices })
    }

    @Test
    fun allExistingVariantSelectionsHighlightTheirFamilyAndKeepTheSamePackage() {
        val packages = listOf(
            installed("winter", InterfaceStyle.Snow, "winter"),
            installed("neon", InterfaceStyle.Pixel, "neon_arcade"),
            installed("spring", InterfaceStyle.Snow, "spring"),
        )
        packages.forEachIndexed { index, installed ->
            val state = SettingsUiState(
                uiMode = installed.style.engine,
                seasonStyle = installed.style.variant.orEmpty(),
                pixelStyle = installed.style.variant.orEmpty(),
                activeInterfaceStyleId = installed.style.id,
                installedInterfaceStyles = packages,
            )
            val groups = state.interfaceStylePickerGroups()
            val group = groups[state.interfaceStylePickerSelectedIndex(groups)]
            assertEquals(installed.style.engine, group.family?.value)
            assertTrue(state.interfaceStyleSelectedIndex() in group.selectionIndices)
            var applied: InterfaceStylePackage? = null
            state.applyInterfaceStyleSelection(
                InterfaceStyle.selectableEntries.size + index,
                onBuiltInSelected = { error("Unexpected built-in selection") },
                onPackageSelected = { applied = it },
            )
            assertEquals(installed.style, applied)
        }
    }

    @Test
    fun distinctPackagesWithTheSameVariantStillKeepTheirOwnSelection() {
        val packages = listOf(
            installed("spring-one", InterfaceStyle.Snow, "spring"),
            installed("spring-two", InterfaceStyle.Snow, "spring"),
        )
        val state = SettingsUiState(
            uiMode = InterfaceStyle.Snow.value,
            seasonStyle = "spring",
            activeInterfaceStyleId = "spring-two",
            installedInterfaceStyles = packages,
        )
        assertEquals(4, state.interfaceStyleSelectedIndex())
        assertEquals(listOf(3, 4), state.interfaceStylePickerGroups().last().selectionIndices)
    }

    @Test
    fun unrelatedPackagesSharingARendererAreNotCollapsed() {
        val state = SettingsUiState(installedInterfaceStyles = listOf(
            installed("alpha-delta", InterfaceStyle.Alpha),
            installed("another-alpha", InterfaceStyle.Alpha),
            installed("material-theme", InterfaceStyle.Material),
        ))
        val downloaded = state.interfaceStylePickerGroups().drop(InterfaceStyle.selectableEntries.size)
        assertEquals(3, downloaded.size)
        assertTrue(downloaded.all { it.family == null && it.selectionIndices.size == 1 })
    }

    @Test
    fun unavailableActiveVariantKeepsFamilyHighlightedWithoutSelectingAnotherPackage() {
        val state = SettingsUiState(
            uiMode = InterfaceStyle.Snow.value,
            seasonStyle = "winter",
            installedInterfaceStyles = listOf(installed("spring", InterfaceStyle.Snow, "spring")),
        )
        val groups = state.interfaceStylePickerGroups()
        val group = groups[state.interfaceStylePickerSelectedIndex(groups)]
        assertEquals(InterfaceStyle.Snow, group.family)
        assertFalse(state.interfaceStyleSelectedIndex() in group.selectionIndices)
    }

    @Test
    fun builtInSelectionsAndInvalidIndicesAreHandledWithoutApplyingADownload() {
        val state = SettingsUiState()
        var selected: Int? = null
        var downloads = 0
        val apply: (Int) -> Unit = { index ->
            state.applyInterfaceStyleSelection(index, { selected = it }, { downloads++ })
        }
        apply(-1)
        apply(100)
        assertEquals(null, selected)
        assertEquals(0, downloads)
        InterfaceStyle.selectableEntries.forEachIndexed { index, style ->
            apply(index)
            assertEquals(index, selected)
            assertEquals(index, state.copy(uiMode = style.value).interfaceStylePickerSelectedIndex())
        }
    }

    private fun installed(id: String, engine: InterfaceStyle, variant: String? = null) =
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
