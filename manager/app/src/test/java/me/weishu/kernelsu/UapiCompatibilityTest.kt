package me.weishu.kernelsu

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UapiCompatibilityTest {
    @Test
    fun acceptsTheAdditiveServicesEventUpgrade() {
        assertTrue(isCompatibleUapiVersion(4, 5))
        assertTrue(isCompatibleUapiVersion(5, 5))
    }

    @Test
    fun rejectsOtherVersionMismatches() {
        assertFalse(isCompatibleUapiVersion(3, 5))
        assertFalse(isCompatibleUapiVersion(6, 5))
        assertFalse(isCompatibleUapiVersion(5, 4))
        assertFalse(isCompatibleUapiVersion(4, 6))
    }
}
