package me.weishu.kernelsu.ui.screen.home

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import me.weishu.kernelsu.ui.util.SidebarCardGradient
import me.weishu.kernelsu.ui.util.SidebarCardMaterial
import me.weishu.kernelsu.ui.util.SidebarHomeCardStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SidebarHomeCardSurfaceTest {
    private fun contrast(a: Color, b: Color): Float = (maxOf(a.luminance(), b.luminance()) + .05f) /
        (minOf(a.luminance(), b.luminance()) + .05f)

    @Test
    fun defaultThemeWithoutPhotoDoesNotNeedAScrim() {
        for ((ink, base) in listOf(Color.Black to Color(0xFFF4F4F4), Color.White to Color(0xFF222222))) {
            val result = sidebarCardReadability(ink, sidebarCardBackgroundSamples(SidebarHomeCardStyle(), base, false))
            assertEquals(0f, result.alpha)
            assertEquals(ink, result.ink)
        }
    }

    @Test
    fun mediumGreyInkIsProtectedAgainstIntermediatePhotoPixels() {
        val ink = Color(0xFF777777)
        val result = sidebarCardReadability(ink, listOf(Color.Black, Color.White))
        assertTrue(result.alpha > 0f)
        for (channel in 0..255) {
            val photo = Color(channel / 255f, channel / 255f, channel / 255f)
            assertTrue(contrast(ink, result.scrim.copy(alpha = result.alpha).compositeOver(photo)) >= 4.5f)
        }
    }

    @Test
    fun photoProtectionKeepsTheChosenInkForDarkLightAndColoredText() {
        for (ink in listOf(Color.Black, Color.White, Color(0xFF777777), Color(0xFF6688AA), Color.Red, Color.Green, Color.Blue)) {
            val result = sidebarCardReadability(ink, listOf(Color.Black, Color.White))
            assertEquals(ink, result.ink)
            for (r in 0..8) for (g in 0..8) for (b in 0..8) {
                val background = Color(r / 8f, g / 8f, b / 8f)
                assertTrue("$ink on $background", contrast(ink, result.scrim.copy(alpha = result.alpha).compositeOver(background)) >= 4.5f)
            }
        }
    }

    @Test
    fun everyGradientAndOpacityProtectsTextAcrossTheEntireGradient() {
        for (gradient in SidebarCardGradient.entries) for (tint in listOf(.15f, .5f, .72f, 1f)) {
            val style = SidebarHomeCardStyle(material = SidebarCardMaterial.LiquidGlass, gradient = gradient,
                start = 0xFFFF0000, middle = 0xFF00FF00, end = 0xFF0000FF, tint = tint)
            val stops = listOf(Color.Red, Color.Green, Color.Blue, Color.Red)
            for (ink in listOf(Color.Black, Color.White, Color(0xFF777777), Color(0xFF6688AA))) {
                val result = sidebarCardReadability(ink, sidebarCardBackgroundSamples(style, Color.Gray, true))
                for (segment in 0..2) for (step in 0..64) for (photo in listOf(Color.Black, Color.Gray, Color.White)) {
                    val p = step / 64f
                    val first = stops[segment]
                    val last = stops[segment + 1]
                    val color = if (gradient == SidebarCardGradient.Solid) Color.Red else Color(
                        first.red * (1 - p) + last.red * p, first.green * (1 - p) + last.green * p, first.blue * (1 - p) + last.blue * p)
                    val rendered = result.scrim.copy(alpha = result.alpha).compositeOver(color.copy(alpha = tint).compositeOver(photo))
                    assertTrue("$gradient tint=$tint ink=$ink step=$step", contrast(ink, rendered) >= 4.5f)
                }
            }
        }
    }

    @Test
    fun brushesSupportNarrowWideAndNotYetMeasuredCards() {
        for (size in listOf(Size.Zero, Size(1f, 1f), Size(180f, 800f), Size(1000f, 100f))) {
            for (gradient in SidebarCardGradient.entries) assertNotNull(sidebarCardBrush(SidebarHomeCardStyle(gradient = gradient), size))
        }
    }
}
