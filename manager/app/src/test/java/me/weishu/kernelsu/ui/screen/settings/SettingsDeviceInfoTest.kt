package me.weishu.kernelsu.ui.screen.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsDeviceInfoTest {
    @Test fun missingPropertiesNeverBecomeExampleDeviceInformation() {
        assertNull(firstDeviceValue(null, "", " ", "unknown", "NULL", "N/A", "none"))
        assertEquals("SM8650", firstDeviceValue("unknown", " SM8650 "))
    }

    @Test fun processorNameDoesNotRepeatManufacturer() {
        assertEquals("Qualcomm SM8650", processorDisplayName("Qualcomm", "SM8650"))
        assertEquals("Qualcomm SM8650", processorDisplayName("Qualcomm", "Qualcomm SM8650"))
        assertEquals("SM8650", processorDisplayName("unknown", "SM8650"))
        assertNull(processorDisplayName("Qualcomm", "unknown"))
    }

    @Test fun cpuInfoFallbackUsesHardwareRatherThanCoreNumber() {
        assertEquals("MT6989", cpuHardwareValue("processor : 0\nHardware\t: MT6989\n"))
        assertNull(cpuHardwareValue("processor : 0\nmodel name : ARMv8 Processor"))
        assertNull(cpuHardwareValue("Hardware: unknown"))
    }

    @Test fun shortcutGridAccountsForNarrowPanesAndLargeFonts() {
        assertEquals(4, settingsShortcutColumns(344f, 1f))
        assertEquals(4, settingsShortcutColumns(270f, 1f))
        assertEquals(3, settingsShortcutColumns(210f, 1f))
        assertEquals(2, settingsShortcutColumns(180f, 1f))
        assertEquals(2, settingsShortcutColumns(344f, 2f))
        assertEquals(4, settingsShortcutColumns(800f, 2f))
    }
}
