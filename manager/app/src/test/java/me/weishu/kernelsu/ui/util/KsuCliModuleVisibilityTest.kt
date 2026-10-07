package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KsuCliModuleVisibilityTest {
    @Test
    fun nativeWebManagerMustBeBothDisabledAndStoppedBeforeRemoval() {
        assertTrue(isNativeWebManagerDisabledStatus("""{"supported":true,"enabled":false,"running":false}"""))
        assertFalse(isNativeWebManagerDisabledStatus("""{"supported":true,"enabled":true,"running":false}"""))
        assertFalse(isNativeWebManagerDisabledStatus("""{"supported":true,"enabled":false,"running":true}"""))
        assertFalse(isNativeWebManagerDisabledStatus("""{"supported":false,"enabled":false,"running":false}"""))
        assertFalse(isNativeWebManagerDisabledStatus("not-json"))
    }
}
