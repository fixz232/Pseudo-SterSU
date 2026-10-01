package me.weishu.kernelsu.ui.screen.install

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DirectInstallPartitionTest {
    @Test
    fun `direct install leaves automatic partition selection to ksud by default`() {
        assertNull(
            resolveDirectInstallPartition(
                method = InstallMethod.DirectInstall,
                hasCustomSelected = false,
                selectedPartitionName = "boot",
                availablePartitions = listOf("boot", "init_boot"),
            )
        )
    }

    @Test
    fun `direct install preserves an explicit available partition`() {
        assertEquals(
            "init_boot",
            resolveDirectInstallPartition(
                method = InstallMethod.DirectInstall,
                hasCustomSelected = true,
                selectedPartitionName = "init_boot",
                availablePartitions = listOf("boot", "init_boot"),
            )
        )
    }

    @Test
    fun `direct install ignores a stale explicit partition`() {
        assertNull(
            resolveDirectInstallPartition(
                method = InstallMethod.DirectInstallToInactiveSlot,
                hasCustomSelected = true,
                selectedPartitionName = "vendor_boot",
                availablePartitions = listOf("boot", "init_boot"),
            )
        )
    }

    @Test
    fun `file patch never receives a direct install partition`() {
        assertNull(
            resolveDirectInstallPartition(
                method = InstallMethod.SelectFile(summary = "boot.img"),
                hasCustomSelected = true,
                selectedPartitionName = "boot",
                availablePartitions = listOf("boot"),
            )
        )
    }
}
