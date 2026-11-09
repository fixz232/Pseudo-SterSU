package me.weishu.kernelsu.ui.component.overflow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.FULL_CUSTOM_WALLPAPER_CROP

class OverflowMenuAppearanceTest {
    private fun source(path: String) = File("src/main/java/me/weishu/kernelsu/ui/$path").readText()

    @Test
    fun largeFontsAndShortWindowsStayWithinTheWindow() {
        assertEquals(290f, overflowMenuWidth(840f, 2f), 0.001f)
        assertEquals(176f, overflowMenuWidth(200f, 2f), 0.001f)
        assertEquals(216f, overflowMenuMaxHeight(240f), 0.001f)
        assertEquals(0f, overflowMenuMaxHeight(20f), 0.001f)
        assertEquals(0f, overflowMenuMaxHeight(Float.NaN), 0.001f)
        assertEquals(0f, overflowMenuWidth(Float.NaN, 1f), 0.001f)
        assertEquals(232f, overflowMenuWidth(412f, Float.NaN), 0.001f)
    }

    @Test
    fun glassTextHasReadableContrastOverBothLightAndDarkWallpapers() {
        for (dark in listOf(false, true)) {
            val text = overflowMenuContentColor(dark)
            assertEquals(1f, text.alpha, 0.001f)
            for (wallpaper in listOf(Color.Black, Color.White, Color.Red, Color.Blue, Color.Green)) {
                for (highlight in listOf(0f, overflowMenuHighlightAlpha(dark))) {
                    val background = Color.White.copy(alpha = highlight).compositeOver(
                        overflowMenuTint(dark, true).compositeOver(wallpaper),
                    )
                    val bright = maxOf(text.luminance(), background.luminance())
                    val dim = minOf(text.luminance(), background.luminance())
                    assertTrue("dark=$dark wallpaper=$wallpaper highlight=$highlight", (bright + 0.05f) / (dim + 0.05f) >= 4.5f)
                }
            }
        }
    }

    @Test
    fun unsupportedBlurFallsBackToAnOpaqueThemedSurface() {
        assertEquals(1f, overflowMenuTint(false, false).alpha, 0.001f)
        assertEquals(1f, overflowMenuTint(true, false).alpha, 0.001f)
        assertTrue(overflowMenuTint(true, false).luminance() < 0.05f)
        assertTrue(overflowMenuTint(false, false).luminance() > 0.85f)
        val menu = source("component/overflow/FrostedOverflowMenu.kt")
        assertTrue(menu.contains("val canBlur = isRenderEffectSupported()"))
        assertTrue(menu.contains("canBlur && (backdrop != null || wallpaper.bitmap != null)"))
    }

    @Test
    fun glassTintLeavesEnoughBackgroundVisibleWithoutFadingForegrounds() {
        for (dark in listOf(false, true)) {
            assertTrue(overflowMenuTint(dark, true).alpha in 0.6f..0.7f)
            assertEquals(1f, overflowMenuContentColor(dark).alpha, 0.001f)
        }
    }

    @Test
    fun menuWallpapersHaveIndependentPersistentKeys() {
        assertEquals(2, OverflowMenuTarget.entries.map { it.wallpaperKey }.toSet().size)
        assertEquals(4, OverflowMenuTarget.entries.flatMap { listOf(it.wallpaperKey, it.cropKey) }.toSet().size)
        val wallpaper = source("component/overflow/OverflowMenuWallpaper.kt")
        assertTrue(wallpaper.contains("ActivityResultContracts.OpenDocument()"))
        assertTrue(wallpaper.contains("persistCustomImageReference(context, uri, target.wallpaperKey"))
        assertTrue(wallpaper.contains("loadCustomImageBitmap(context, newReference"))
        assertTrue(wallpaper.contains("Dispatchers.IO"))
        assertTrue(wallpaper.contains("preferences.registerOnSharedPreferenceChangeListener(listener)"))
        assertTrue(wallpaper.contains("preferences.unregisterOnSharedPreferenceChangeListener(listener)"))
        assertTrue(wallpaper.contains("if (saved) releaseCustomImageReference(context, previous)"))
        assertTrue(wallpaper.contains("if (uri != null && !busy)"))
    }

    @Test
    fun opacityHandlesEndpointsAndCorruptSavedValues() {
        assertEquals(0f, sanitizeOverflowMenuOpacity(0f), 0f)
        assertEquals(0.5f, sanitizeOverflowMenuOpacity(0.5f), 0f)
        assertEquals(1f, sanitizeOverflowMenuOpacity(1f), 0f)
        assertEquals(0f, sanitizeOverflowMenuOpacity(-0.5f), 0f)
        assertEquals(1f, sanitizeOverflowMenuOpacity(1.5f), 0f)
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertEquals(1f, sanitizeOverflowMenuOpacity(value), 0f)
        }
    }

    @Test
    fun menuOpacityIsIndependentAndSurvivesWallpaperChanges() {
        val keys = OverflowMenuTarget.entries.flatMap { listOf(it.wallpaperKey, it.cropKey, it.opacityKey) }
        assertEquals(6, keys.toSet().size)
        val wallpaper = source("component/overflow/OverflowMenuWallpaper.kt")
        assertTrue(wallpaper.contains("preferences.getFloat(target.opacityKey, 1f)"))
        assertTrue(wallpaper.contains("key == target.opacityKey"))
        assertTrue(wallpaper.contains("putFloat(target.opacityKey, sanitizeOverflowMenuOpacity(value)).commit()"))
        assertFalse(wallpaper.contains("remove(target.opacityKey)"))
    }

    @Test
    fun opacityAffectsOnlyBackgroundsAndIsAccessibleWithoutAWallpaper() {
        val menu = source("component/overflow/FrostedOverflowMenu.kt")
        assertTrue(menu.contains("Modifier.matchParentSize().graphicsLayer { alpha = opacity }"))
        assertTrue(menu.contains("colors.containerColor.alpha * opacity"))
        assertTrue(menu.contains("colors.selectedContainerColor.alpha * opacity"))
        assertTrue(menu.contains("colors.disabledContainerColor.alpha * opacity"))
        assertFalse(menu.contains("textColor ="))
        assertFalse(menu.contains("leadingIconColor ="))
        assertFalse(menu.contains("trailingContentColor ="))
        val dialog = source("component/overflow/OverflowMenuWallpaper.kt")
        val slider = dialog.substringAfter("Slider(").substringBefore("state.bitmap?.let")
        assertTrue(dialog.contains("SliderState(opacity, trackRange = 0f..1f)"))
        assertTrue(slider.contains("state = opacitySlider"))
        assertTrue(slider.contains("onValueChangeFinished = { state.onOpacityChange(opacity) }"))
        assertTrue(slider.contains("stateDescription = opacityValue"))
        assertTrue(slider.contains("state.onOpacityChange(1f)"))
    }

    @Test
    fun cropRoundTripsAndInvalidSavedDataRecoversToTheFullImage() {
        val crop = CustomWallpaperCrop(0.15f, 0.2f, 0.85f, 0.7f)
        assertEquals(crop, decodeOverflowMenuCrop(encodeOverflowMenuCrop(crop)))
        for (invalid in listOf(null, "", "broken", "0,0,1", "0,0,1,1,1", "NaN,0,1,1", "0,Infinity,1,1")) {
            assertEquals(FULL_CUSTOM_WALLPAPER_CROP, decodeOverflowMenuCrop(invalid))
        }
        val sanitized = decodeOverflowMenuCrop("-1,2,-3,5")
        assertTrue(sanitized.left in 0f..0.88f)
        assertTrue(sanitized.top in 0f..0.88f)
        assertTrue(sanitized.width >= 0.1199f)
        assertTrue(sanitized.height >= 0.1199f)
        assertTrue(sanitized.right <= 1f && sanitized.bottom <= 1f)
    }

    @Test
    fun croppedBitmapIsUsedForBothTheMenuAndPreviewAndResetWithNewImages() {
        val wallpaper = source("component/overflow/OverflowMenuWallpaper.kt")
        assertTrue(wallpaper.contains("rememberCustomImageBitmap(uriString, maxSide = 800, crop = crop)"))
        assertTrue(wallpaper.contains("key == target.cropKey"))
        assertTrue(wallpaper.contains(".putString(target.cropKey, encodeOverflowMenuCrop(FULL_CUSTOM_WALLPAPER_CROP))"))
        assertTrue(wallpaper.contains(".remove(target.wallpaperKey).remove(target.cropKey).commit()"))
        assertTrue(wallpaper.contains(".putString(target.cropKey, previousCrop).commit()"))
        assertTrue(wallpaper.contains("if (busy) return"))
        assertTrue(wallpaper.contains("bitmap = it,"))
        val menu = source("component/overflow/FrostedOverflowMenu.kt")
        assertTrue(menu.contains("wallpaper.bitmap?.let { bitmap ->"))
    }

    @Test
    fun cropEditorUsesAnExplicitConfirmationAndDoesNotStackTwoDialogs() {
        val wallpaper = source("component/overflow/OverflowMenuWallpaper.kt")
        val editor = wallpaper.substringAfter("if (editingCrop && state.uriString != null) {").substringBefore("AlertDialog(")
        assertTrue(editor.contains("SettingsWallpaperCropDialog("))
        assertTrue(editor.contains("crop = state.crop"))
        assertTrue(editor.contains("onCropChange = state.onCropChange"))
        assertTrue(editor.contains("onDismissRequest = { editingCrop = false }"))
        assertTrue(editor.contains("allowTransforms = false"))
        assertTrue(editor.contains("return"))
        val cropDialog = source("screen/settings/SettingsWallpaperCropDialog.kt")
        assertTrue(cropDialog.contains("onCropChange(editCrop)"))
        assertTrue(cropDialog.contains("text = stringResource(android.R.string.cancel)"))
    }

    @Test
    fun bothHeaderStylesUseOneAccessibleTapAndLongPressTarget() {
        for (page in listOf("superuser/SuperUser", "module/Module")) {
            for (style in listOf("Material", "Miuix")) {
                val header = source("screen/$page$style.kt")
                assertTrue(header.contains("OverflowMenuAnchor("))
                assertTrue(header.contains("wallpaper = wallpaper"))
            }
        }
        val anchor = source("component/overflow/OverflowMenuAnchor.kt")
        assertEquals(1, Regex("\\.combinedClickable\\(").findAll(anchor).count())
        assertTrue(anchor.contains("role = Role.Button"))
        assertTrue(anchor.contains("onLongClickLabel = wallpaperLabel"))
        assertTrue(anchor.contains(".size(48.dp).clip(CircleShape)"))
        assertTrue(anchor.contains("onClick = { expanded = true }"))
        val longPress = anchor.substringAfter("onLongClick = {").substringBefore("},")
        assertTrue(longPress.contains("expanded = false"))
        assertTrue(longPress.contains("showWallpaperEditor = true"))
        assertFalse(longPress.contains("expanded = true"))
        assertTrue(anchor.contains("OverflowMenuWallpaperDialog("))
        assertFalse(source("component/overflow/FrostedOverflowMenu.kt").contains("overflow_menu_wallpaper"))
    }

    @Test
    fun blurAndWallpaperStayBehindOpaqueAccessibleMenuRows() {
        val menu = source("component/overflow/FrostedOverflowMenu.kt")
        assertTrue(menu.contains("UiPassthroughTheme(UiPassthroughSettings())"))
        assertTrue(menu.contains("Modifier.matchParentSize().drawBackdrop("))
        assertTrue(menu.contains(".blur(12.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)"))
        assertFalse(menu.contains(".alpha("))
    }

    @Test
    fun materialAndMiuixModuleHeadersKeepSortingInsideOneSharedMenu() {
        for (file in listOf("ModuleMaterial.kt", "ModuleMiuix.kt")) {
            val header = source("screen/module/$file").substringBefore("floatingActionButton =")
            assertTrue(header.contains("ModuleOverflowMenu("))
            assertFalse(header.contains("CheckableDropdownMenuItem("))
        }
        val menu = source("screen/module/ModuleOverflowMenu.kt")
        assertTrue(menu.contains("FrostedOverflowMenu(expanded, onDismissRequest, wallpaper)"))
        assertTrue(menu.contains("actions.onToggleSortActionFirst"))
        assertTrue(menu.contains("actions.onToggleSortEnabledFirst"))
        assertTrue(menu.contains("toggleableState = ToggleableState(checked)"))
    }

    @Test
    fun menusHaveAnOnDemandBackdropEvenWithGlobalBlurDisabled() {
        val backdrop = source("component/overflow/OverflowMenuBackdrop.kt")
        assertTrue(backdrop.contains("LocalSidebarGlassBackdrop.current ?: LocalLiquidGlassBackdrop.current"))
        assertTrue(backdrop.contains("LocalView.current.isHardwareAccelerated"))
        assertTrue(backdrop.contains("rememberBlurBackdrop(supported && inherited == null)"))
        assertTrue(backdrop.contains("inherited == null && state.captureActive && pageBackdrop != null"))
        assertTrue(backdrop.contains("Modifier.layerBackdrop(pageBackdrop)"))
        assertFalse(backdrop.contains("LocalEnableBlur"))
        for (page in listOf("module/ModuleScreen.kt", "superuser/SuperUserScreen.kt")) {
            assertTrue(source("screen/$page").contains("OverflowMenuBackdrop {"))
        }
        val menu = source("component/overflow/FrostedOverflowMenu.kt")
        val popup = menu.substringAfter("DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismissRequest) {")
        assertTrue(popup.contains("pageBackdrop?.acquireCapture()"))
        assertTrue(popup.contains("onDispose { release?.invoke() }"))
    }

    @Test
    fun dismissingOneMenuDoesNotReleaseAnotherMenusBackdrop() {
        val state = OverflowMenuBackdropState(null)
        assertFalse(state.captureActive)
        val releaseSuperuser = state.acquireCapture()
        val releaseModule = state.acquireCapture()
        assertTrue(state.captureActive)
        releaseSuperuser()
        assertTrue(state.captureActive)
        releaseModule()
        assertFalse(state.captureActive)
    }

    @Test
    fun rootCaptureIncludesWallpaperAndNestedPagesReuseIt() {
        val activity = source("MainActivity.kt")
        val root = activity.substringAfter("me.weishu.kernelsu.ui.component.overflow.OverflowMenuBackdrop(")
        assertTrue(root.contains("CustomWallpaperRoot("))
        assertTrue(root.indexOf("CustomWallpaperRoot(") < root.indexOf("SeasonChromeOverlay("))
        val backdrop = source("component/overflow/OverflowMenuBackdrop.kt")
        val nested = backdrop.substringAfter("if (LocalOverflowMenuBackdrop.current != null) {")
            .substringBefore("}")
        assertTrue(nested.contains("content()"))
        assertTrue(nested.contains("return"))
    }
}
