package me.weishu.kernelsu.ui.component.liquid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrostedGlassAppearanceTest {
    @Test
    fun noBlurUsesOpaqueFallback() {
        assertEquals(1f, frostedSurfaceAlpha(0.54f, false), 0f)
        assertEquals(0.86f, frostedSurfaceAlpha(0.86f, true), 0f)
        assertEquals(0.94f, frostedSurfaceAlpha(0.94f, true), 0f)
        assertEquals(1f, frostedSurfaceAlpha(2f, true), 0f)
    }

    @Test
    fun strokeCanBeDisabledInBothThemes() {
        for (dark in listOf(false, true)) {
            assertEquals(0f, frostedStrokeAlpha(0f, dark), 0f)
            assertEquals(0f, frostedStrokeAlpha(-1f, dark), 0f)
            assertTrue(frostedStrokeAlpha(0.14f, dark) < 0.1f)
            assertTrue(frostedStrokeAlpha(1f, dark) <= 0.48f)
        }
    }

    @Test
    fun disabledBlurIsNeverForcedBackOn() {
        assertEquals(0f, frostedBlurDp(18f, 0f), 0f)
        assertEquals(0f, frostedBlurDp(0f, 1f), 0f)
        assertEquals(0f, frostedBlurDp(18f, -1f), 0f)
        assertEquals(7f, frostedBlurDp(14f, 0.5f), 0f)
        assertEquals(48f, frostedBlurDp(48f, 3f), 0f)
    }
}
