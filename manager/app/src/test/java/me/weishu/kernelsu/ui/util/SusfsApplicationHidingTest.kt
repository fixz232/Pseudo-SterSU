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

    @Test
    fun applicationHidingConfigRoundTrips() {
        val encoded = encodeSusfsApplicationHidingConfig(
            entries = listOf(
                SusfsApplicationHidingConfigEntry("com.example.wallet", true),
                SusfsApplicationHidingConfigEntry("com.example.notes", false),
            ),
            exportedAt = 1L,
        )

        assertEquals(
            listOf(
                SusfsApplicationHidingConfigEntry("com.example.notes", false),
                SusfsApplicationHidingConfigEntry("com.example.wallet", true),
            ),
            parseSusfsApplicationHidingConfig(encoded),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun applicationHidingConfigRejectsDuplicatePackages() {
        parseSusfsApplicationHidingConfig(
            """
                {
                  "schema": "io.github.fixz.stersu.susfs-app-hiding",
                  "version": 1,
                  "applications": [
                    {"packageName": "com.example.notes", "hidden": true},
                    {"packageName": "com.example.notes", "hidden": false}
                  ]
                }
            """.trimIndent(),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun applicationHidingConfigRejectsUnknownSchema() {
        parseSusfsApplicationHidingConfig(
            """
                {
                  "schema": "example.invalid",
                  "version": 1,
                  "applications": []
                }
            """.trimIndent(),
        )
    }
}
