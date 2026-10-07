package me.weishu.kernelsu.ui.util

import me.weishu.kernelsu.core.tasks.KernelIdentity
import org.junit.Assert.*
import org.junit.Test

class SusfsKernelSlotsTest {
    @Test fun keepsSlotsSeparateAndMarksCurrent() {
        val result = parseKernelSlotProbe(listOf("current=_b", "boota=/dev/block/sda1", "bootb=/dev/block/sda2"))
        assertEquals(KernelBootSlot.B, result.currentSlot)
        assertEquals(listOf(KernelBootSlot.A, KernelBootSlot.B), result.slots.map { it.slot })
        assertEquals(listOf("/dev/block/sda1", "/dev/block/sda2"), result.slots.map { it.partition })
    }

    @Test fun neverUsesUnsuffixedBootForMissingInactiveSlot() {
        val result = parseKernelSlotProbe(listOf("current=_a", "boota=/dev/block/sda1", "boot=/dev/block/sda1"))
        assertEquals("slot_missing", result.slots[1].error)
        assertEquals("", result.slots[1].partition)
        assertEquals("", result.slots[0].error)
    }

    @Test fun abPropertyKeepsBothMissingSlotsVisible() {
        assertEquals(2, parseKernelSlotProbe(listOf("ab=true", "boot=/dev/block/sda1")).slots.size)
    }

    @Test fun partitionNamesEstablishAbWithoutGuessingCurrentSlot() {
        val result = parseKernelSlotProbe(listOf("current=bad", "bootb=/dev/block/sda2"))
        assertEquals(2, result.slots.size)
        assertNull(result.currentSlot)
    }

    @Test fun detectsConflictingAliases() {
        val result = parseKernelSlotProbe(listOf("boota=/dev/block/sda1", "bootb=/dev/block/sda1"))
        assertTrue(result.slots.all { it.error == "slot_alias_conflict" && it.identity == null })
    }

    @Test fun supportsSingleSlotWithoutInventingB() {
        val result = parseKernelSlotProbe(listOf("boot=/dev/block/sda1", "ab=false"))
        assertEquals(KernelBootSlot.Single, result.slots.single().slot)
    }

    @Test fun rejectsInjectedPartitionPaths() {
        val result = parseKernelSlotProbe(listOf("current=_a", "boota=/dev/block/a; touch /tmp/foo", "bootb=/data/boot"))
        assertTrue(result.slots.all { it.error == "slot_missing" })
    }

    @Test fun probeDoesNotChangeSlotOrUseRunningUname() {
        val command = buildKernelSlotProbe()
        assertTrue(command.contains("/dev/block/by-name/boot_a"))
        assertTrue(command.contains("/dev/block/by-name/boot_b"))
        assertFalse(command.contains("init_boot"))
        assertFalse(command.contains("set-active"))
        assertFalse(command.contains("uname"))
        assertFalse(command.contains("/proc/version"))
    }

    @Test fun readCommandOnlyWritesToQuotedCacheAndLimitsIo() {
        val command = buildKernelReadCommand("/dev/block/by-name/boot_a", "/data/user/0/app/cache/a'b.bin", 4096, 1, 100)
        assertTrue(command.contains("if='/dev/block/by-name/boot_a'"))
        assertTrue(command.contains("of='/data/user/0/app/cache/a'\"'\"'b.bin'"))
        assertTrue(command.contains("timeout -s KILL 25"))
        assertTrue(command.contains("bs=4096 skip=1 count=100"))
    }

    @Test fun rejectsBlockDeviceDestinationAndOversizedCopy() {
        assertThrows(IllegalArgumentException::class.java) {
            buildKernelReadCommand("/dev/block/boot_a", "/dev/block/boot_b", 4096, 1, 100)
        }
        assertThrows(IllegalArgumentException::class.java) {
            buildKernelReadCommand("/dev/block/boot_a", "/data/cache/file", 65536, 1, Int.MAX_VALUE)
        }
    }

    @Test fun fillChangesOnlyTwoDraftFields() {
        val original = SusfsPathConfigState(
            enabled = true,
            unameRelease = "old-release",
            unameVersion = "old-version",
            cmdlineOrBootconfig = "/data/adb/custom-cmdline",
            paths = listOf("/data/secret"),
        )
        val filled = original.withKernelIdentity(KernelIdentity("6.1.157-custom", "#1 SMP test date", null))
        assertEquals("6.1.157-custom", filled.unameRelease)
        assertEquals("#1 SMP test date", filled.unameVersion)
        assertEquals(original, filled.copy(unameRelease = original.unameRelease, unameVersion = original.unameVersion))
        assertEquals("old-release", original.unameRelease)
    }
}
