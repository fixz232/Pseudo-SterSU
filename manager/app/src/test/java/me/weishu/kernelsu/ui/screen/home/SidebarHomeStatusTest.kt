package me.weishu.kernelsu.ui.screen.home

import org.junit.Assert.assertEquals
import org.junit.Test

class SidebarHomeStatusTest {
    @Test
    fun metricCountsAreHiddenUntilRootPagesAreAvailable() {
        assertEquals("—", sidebarHomeMetricValue(available = false, count = 21))
        assertEquals("—", sidebarHomeMetricValue(available = true, count = -1))
        assertEquals("0", sidebarHomeMetricValue(available = true, count = 0))
        assertEquals("21", sidebarHomeMetricValue(available = true, count = 21))
    }
}
