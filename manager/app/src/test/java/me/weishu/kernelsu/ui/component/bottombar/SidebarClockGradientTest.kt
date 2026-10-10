package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.ui.graphics.Color
import me.weishu.kernelsu.ui.util.SidebarClockStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SidebarClockGradientTest {
    @Test
    fun digitGradientsRemainReadableInDefaultThemes() {
        for (dark in listOf(false, true)) {
            val colors = sidebarColors(dark)
            for (style in SidebarClockStyle.entries.filter { it.gradient }) {
                val stops = sidebarClockGradientStops(style, colors)
                assertEquals(2, stops.size)
                assertTrue(stops[0] != stops[1])
                assertTrue(stops.all { sidebarContrast(it, colors.background) >= 4.5f })
            }
        }
    }

    @Test
    fun customSidebarColorUsesReadableFallback() {
        val colors = SidebarColors(
            background = Color(0xFF555555),
            selection = Color(0xFF666666),
            content = Color.White,
            divider = Color(0xFF777777),
        )
        for (style in SidebarClockStyle.entries.filter { it.gradient }) {
            assertTrue(sidebarClockGradientStops(style, colors).all {
                sidebarContrast(it, colors.background) >= 4.5f
            })
        }
    }
}
