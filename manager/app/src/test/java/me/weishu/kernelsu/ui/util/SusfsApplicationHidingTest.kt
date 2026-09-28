package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SusfsApplicationHidingTest {
    @Test
    fun classifiesRootAndIntegrityPackagesAsRisk() {
        val result = assessSusfsAppRisk("com.example.integritycheck")

        assertTrue(SusfsRiskSignal.RootOrIntegrityName in result.signals)
    }

    @Test
    fun classifiesSensitivePermissionsAsRisk() {
        val result = assessSusfsAppRisk(
            packageName = "com.example.reader",
            requestedPermissions = listOf("android.permission.QUERY_ALL_PACKAGES"),
        )

        assertEquals(setOf(SusfsRiskSignal.SensitivePermission), result.signals)
    }

    @Test
    fun ordinaryPackageWithoutSignalsIsNormal() {
        val result = assessSusfsAppRisk("com.example.notes")

        assertTrue(result.signals.isEmpty())
    }

    @Test
    fun protectedPackagesIncludeManagerAndSystemUids() {
        assertTrue(isProtectedSusfsPackage("com.example.manager", 10001, "com.example.manager"))
        assertTrue(isProtectedSusfsPackage("android", 1000, "com.example.manager"))
    }

    @Test
    fun userUidIsNotProtectedByUidRule() {
        assertTrue(!isProtectedSusfsPackage("com.example.notes", 10123, "com.example.manager"))
    }
}
