package me.weishu.kernelsu.ui.screen.superuser

import me.weishu.kernelsu.ui.component.overflow.overflowMenuWidth as superUserOverflowMenuWidth
import org.junit.Assert.assertEquals
import org.junit.Test

class SuperUserOverflowMenuTest {
    @Test
    fun phoneAndTabletMenusDoNotExpandToFillThePage() {
        for (width in listOf(360f, 412f, 840f, 1280f)) {
            assertEquals(232f, superUserOverflowMenuWidth(width, 1f), 0.01f)
        }
    }

    @Test
    fun largeTextGetsMoreSpaceWithoutAnUnboundedPopup() {
        assertEquals(278.4f, superUserOverflowMenuWidth(412f, 1.2f), 0.01f)
        assertEquals(290f, superUserOverflowMenuWidth(412f, 2f), 0.01f)
        assertEquals(232f, superUserOverflowMenuWidth(412f, 0.85f), 0.01f)
    }

    @Test
    fun tinyWindowsKeepOuterMarginsEvenWithLargeText() {
        assertEquals(176f, superUserOverflowMenuWidth(200f, 1f), 0.01f)
        assertEquals(176f, superUserOverflowMenuWidth(200f, 2f), 0.01f)
        assertEquals(96f, superUserOverflowMenuWidth(120f, 1f), 0.01f)
    }

    @Test
    fun zeroSizeAndVeryNarrowWindowsNeverProduceNegativeWidths() {
        for (width in listOf(0f, 12f, 24f)) {
            assertEquals(0f, superUserOverflowMenuWidth(width, 1f), 0.01f)
        }
    }
}
