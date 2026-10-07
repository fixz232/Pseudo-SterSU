package me.weishu.kernelsu.ui.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import me.weishu.kernelsu.Natives
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class SusfsAppPolicyTest {
    private val name = "com.example.reader"
    private val other = "com.example.shared"
    private val uid = 10123

    private class FakeAccess : SusfsPolicyAccess {
        var stored: Natives.Profile? = null
        var globalDefault = false
        var installed = listOf("com.example.reader")
        var readFailure: Exception? = null
        var effectiveFailure: Exception? = null
        var effectiveOverride: Boolean? = null
        var acceptWrites = true
        var ignoreWrites = false
        var writes = 0
        override fun profile(packageName: String, uid: Int): Natives.Profile? {
            readFailure?.let { throw it }
            return stored
        }
        override fun effectiveHidden(uid: Int): Boolean {
            effectiveFailure?.let { throw it }
            effectiveOverride?.let { return it }
            return when {
                stored?.allowSu == true -> false
                stored == null || stored!!.nonRootUseDefault -> globalDefault
                else -> stored!!.umountModules
            }
        }
        override fun packages(uid: Int) = installed
        override fun write(profile: Natives.Profile): Boolean {
            writes++
            if (acceptWrites && !ignoreWrites) stored = profile
            return acceptWrites
        }
    }

    private fun controller(access: FakeAccess) = SusfsPolicyController(access, "com.example.manager")
    private fun target(access: FakeAccess, policy: SusfsPolicyController = controller(access)) =
        SusfsPolicyTarget(uid, access.installed.sorted(), false, policy.read(uid, name))
    private fun plan(mode: SusfsHideMode = SusfsHideMode.Enabled, restoring: Boolean = false) = planSusfsImport(
        listOf(SusfsApplicationHidingConfigEntry(name, mode)),
        listOf(SusfsPolicyTarget(uid, listOf(name), false, SusfsPolicyState(SusfsHideMode.Default, false))),
        restoring,
    )

    @Test fun inheritedOffIgnoresStoredOnBit() {
        val access = FakeAccess().apply { stored = Natives.Profile(name, uid, nonRootUseDefault = true, umountModules = true) }
        val state = controller(access).read(uid, name)
        assertEquals(SusfsHideMode.Default, state.mode)
        assertEquals(false, state.effectiveHidden)
        assertTrue(state.known)
    }

    @Test fun inheritedOnIgnoresStoredOffBit() {
        val access = FakeAccess().apply {
            globalDefault = true
            stored = Natives.Profile(name, uid, nonRootUseDefault = true, umountModules = false)
        }
        assertEquals(true, controller(access).read(uid, name).effectiveHidden)
    }

    @Test fun missingProfileUsesCheckedDefault() {
        val access = FakeAccess().apply { globalDefault = true }
        assertEquals(SusfsPolicyState(SusfsHideMode.Default, true), controller(access).read(uid, name))
    }

    @Test fun explicitRuleDoesNotInheritDefault() {
        val access = FakeAccess().apply { stored = Natives.Profile(name, uid, nonRootUseDefault = false, umountModules = true) }
        assertEquals(SusfsPolicyState(SusfsHideMode.Enabled, true), controller(access).read(uid, name))
    }

    @Test fun profileReadFailureIsUnknownNotDefault() {
        val access = FakeAccess().apply { readFailure = IOException("GET_APP_PROFILE: errno=19") }
        val state = controller(access).read(uid, name)
        assertFalse(state.known)
        assertNull(state.effectiveHidden)
        assertTrue(state.error!!.contains("errno=19"))
    }

    @Test fun effectiveReadFailureIsUnknownNotFalse() {
        val access = FakeAccess().apply { effectiveFailure = IOException("UID_SHOULD_UMOUNT: errno=95") }
        assertFalse(controller(access).read(uid, name).known)
        assertNull(controller(access).read(uid, name).effectiveHidden)
    }

    @Test fun cancellationIsNotConvertedToUnknown() {
        val access = FakeAccess().apply { readFailure = CancellationException("cancel") }
        assertThrows(CancellationException::class.java) { controller(access).read(uid, name) }
    }

    @Test fun conflictingExplicitAndEffectiveReadsAreUnknown() {
        val access = FakeAccess().apply {
            stored = Natives.Profile(name, uid, nonRootUseDefault = false, umountModules = true)
            effectiveOverride = false
        }
        val result = controller(access).read(uid, name)
        assertFalse(result.known)
        assertEquals("inconsistent_policy_read", result.error)
    }

    @Test fun rootGrantAndEffectiveHidingCannotBothBeDisplayed() {
        val access = FakeAccess().apply {
            stored = Natives.Profile(name, uid, allowSu = true)
            effectiveOverride = true
        }
        assertFalse(controller(access).read(uid, name).known)
    }

    @Test fun changedRootGrantIsNeverOverwritten() {
        val access = FakeAccess()
        val original = target(access)
        access.stored = Natives.Profile(name, uid, allowSu = true)
        val error = assertThrows(IllegalStateException::class.java) { controller(access).change(original, SusfsHideMode.Enabled) }
        assertEquals("root_allowed_app", error.message)
        assertEquals(0, access.writes)
        assertTrue(access.stored!!.allowSu)
    }

    @Test fun rejectsChangedPackageUidMembership() {
        val access = FakeAccess()
        val original = target(access)
        access.installed = listOf(name, other)
        assertThrows(IllegalStateException::class.java) { controller(access).change(original, SusfsHideMode.Enabled) }
        assertEquals(0, access.writes)
    }

    @Test fun protectsEntireSharedUidEvenWhenSnapshotForgotProtection() {
        val access = FakeAccess().apply { installed = listOf(name, "com.example.manager") }
        assertThrows(IllegalStateException::class.java) { controller(access).change(target(access), SusfsHideMode.Enabled) }
        assertEquals(0, access.writes)
    }

    @Test fun protectsSystemUidInSecondaryUser() {
        assertTrue(isProtectedSusfsPackage(name, 101000, "com.example.manager"))
        assertFalse(isProtectedSusfsPackage(name, 110123, "com.example.manager"))
    }

    @Test fun rejectsUnknownAndChangedPolicy() {
        val access = FakeAccess()
        val original = target(access)
        assertThrows(IllegalStateException::class.java) {
            controller(access).change(original.copy(state = SusfsPolicyState(error = "driver_missing")), SusfsHideMode.Enabled)
        }
        access.globalDefault = true
        assertThrows(IllegalStateException::class.java) { controller(access).change(original, SusfsHideMode.Enabled) }
        assertEquals(0, access.writes)
    }

    @Test fun preservesUnrelatedFieldsAndVerifiesWrite() {
        val access = FakeAccess().apply {
            stored = Natives.Profile(name, uid, rootTemplate = "keep", groups = listOf(1000), flags = 42, rules = "keep rules")
        }
        val before = access.stored!!
        val state = controller(access).change(target(access), SusfsHideMode.Enabled)
        assertEquals(SusfsHideMode.Enabled, state.mode)
        assertEquals(before.copy(nonRootUseDefault = false, umountModules = true), access.stored)
    }

    @Test fun returningToDefaultTracksGlobalInsteadOfOldBit() {
        val access = FakeAccess().apply { stored = Natives.Profile(name, uid, nonRootUseDefault = false, umountModules = true) }
        val state = controller(access).change(target(access), SusfsHideMode.Default)
        assertEquals(SusfsHideMode.Default, state.mode)
        assertEquals(false, state.effectiveHidden)
        assertTrue(access.stored!!.nonRootUseDefault)
    }

    @Test fun unsuccessfulWriteAndReadbackAreNotSuccesses() {
        val access = FakeAccess().apply { acceptWrites = false }
        assertThrows(IllegalStateException::class.java) { controller(access).change(target(access), SusfsHideMode.Enabled) }
        access.acceptWrites = true
        access.ignoreWrites = true
        val error = assertThrows(IllegalStateException::class.java) { controller(access).change(target(access), SusfsHideMode.Enabled) }
        assertEquals("readback_mismatch", error.message)
    }

    @Test fun v2RoundTripKeepsDefaultAndRecoveryUid() {
        val entries = listOf(SusfsApplicationHidingConfigEntry(name, SusfsHideMode.Default, uid))
        assertEquals(entries, parseSusfsApplicationHidingConfig(encodeSusfsApplicationHidingConfig(entries)))
    }

    @Test fun legacyBooleansBecomeExplicitNotInherited() {
        val json = """{"schema":"io.github.fixz.stersu.susfs-app-hiding","version":1,"applications":[{"packageName":"$name","hidden":false}]}"""
        assertEquals(SusfsHideMode.Disabled, parseSusfsApplicationHidingConfig(json).single().mode)
    }

    @Test fun rejectsCoercedBooleansAndUnknownModes() {
        val json = """{"schema":"io.github.fixz.stersu.susfs-app-hiding","version":1,"applications":[{"packageName":"$name","hidden":"false"}]}"""
        assertThrows(IllegalStateException::class.java) { parseSusfsApplicationHidingConfig(json) }
        assertThrows(IllegalStateException::class.java) {
            parseSusfsApplicationHidingConfig(encodeSusfsApplicationHidingConfig(listOf(SusfsApplicationHidingConfigEntry(name, true))).replace("enabled", "invalid"))
        }
    }

    @Test fun rejectsOversizedAndDuplicateExports() {
        assertThrows(IllegalArgumentException::class.java) { parseSusfsApplicationHidingConfig(" ".repeat(SUSFS_APP_CONFIG_MAX_BYTES + 1)) }
        assertThrows(IllegalArgumentException::class.java) {
            encodeSusfsApplicationHidingConfig(listOf(SusfsApplicationHidingConfigEntry(name, true), SusfsApplicationHidingConfigEntry(name, false)))
        }
    }

    @Test fun previewGroupsAliasesAndIncludesAllAffectedPackages() {
        val access = FakeAccess().apply { installed = listOf(name, other) }
        val plan = planSusfsImport(listOf(SusfsApplicationHidingConfigEntry(name, true)), listOf(target(access)))
        assertEquals(1, plan.changes.size)
        assertEquals(listOf(name, other), plan.changes.single().packages)
        assertEquals(0, access.writes)
    }

    @Test fun sharedUidConflictsBlockWholeImportBeforeWrites() {
        val access = FakeAccess().apply { installed = listOf(name, other) }
        val plan = planSusfsImport(listOf(SusfsApplicationHidingConfigEntry(name, true), SusfsApplicationHidingConfigEntry(other, false)), listOf(target(access)))
        assertFalse(plan.canApply)
        assertEquals(SusfsPlanAction.Conflict, plan.items.single().action)
        assertEquals(0, access.writes)
    }

    @Test fun recoverySkipsReinstalledUidAndRootGrantedApps() {
        val original = target(FakeAccess())
        val changed = original.copy(uid = uid + 1)
        val entries = listOf(SusfsApplicationHidingConfigEntry(name, SusfsHideMode.Default, uid))
        assertEquals("package_uid_changed", planSusfsImport(entries, listOf(changed), true).items.single().reason)
        assertEquals("root_allowed_app", planSusfsImport(entries, listOf(original.copy(state = original.state.copy(allowSu = true))), true).items.single().reason)
    }

    @Test fun explicitOffIsStillAChangeWhenInheritedDefaultIsOff() {
        val plan = plan(SusfsHideMode.Disabled)
        assertTrue(plan.canApply)
        assertEquals(1, plan.changes.size)
    }

    @Test fun absentProtectedAndUnknownEntriesAreExplained() {
        val base = target(FakeAccess())
        val entries = listOf(SusfsApplicationHidingConfigEntry(name, true))
        assertEquals("not_installed", planSusfsImport(entries, emptyList()).items.single().reason)
        assertEquals("protected_app", planSusfsImport(entries, listOf(base.copy(protected = true))).items.single().reason)
        assertTrue(planSusfsImport(entries, listOf(base.copy(state = SusfsPolicyState(error = "errno=19")))).items.single().reason.contains("errno=19"))
    }

    @Test fun backupAndPendingJournalPrecedeAnyWrite() = runBlocking {
        val events = mutableListOf<String>()
        var backup = ""
        val result = applySusfsPlan(plan(), { json, _ -> backup = json; events += "backup" }, { events += it.items.first().status }) { _, _ -> events += "write" }
        assertEquals(listOf("backup", "pending", "write", "applied"), events)
        val saved = parseSusfsApplicationHidingConfig(backup).single()
        assertEquals(SusfsHideMode.Default, saved.mode)
        assertEquals(uid, saved.expectedUid)
        assertEquals("applied", result.items.single().status)
    }

    @Test fun failedBackupPreventsAllWrites() {
        var writes = 0
        assertThrows(IOException::class.java) {
            runBlocking { applySusfsPlan(plan(), { _, _ -> throw IOException("disk_full") }, {}) { _, _ -> writes++ } }
        }
        assertEquals(0, writes)
    }

    @Test fun failedInitialJournalPreventsAllWrites() {
        var writes = 0
        assertThrows(IOException::class.java) {
            runBlocking { applySusfsPlan(plan(), { _, _ -> }, { throw IOException("journal_failed") }) { _, _ -> writes++ } }
        }
        assertEquals(0, writes)
    }

    @Test fun partialFailureKeepsConcreteReasonAndContinues() = runBlocking {
        val target2 = SusfsPolicyTarget(uid + 1, listOf(other), false, SusfsPolicyState(SusfsHideMode.Default, false))
        val plan = planSusfsImport(listOf(SusfsApplicationHidingConfigEntry(name, true), SusfsApplicationHidingConfigEntry(other, true)), listOf(target(FakeAccess()), target2))
        val report = applySusfsPlan(plan, { _, _ -> }, {}) { target, _ -> if (target.uid == uid) throw IOException("write errno=1") }
        assertEquals(listOf("failed", "applied"), report.items.map { it.status })
        assertEquals("write errno=1", report.items.first().detail)
        assertEquals(report, SusfsApplyReport.parse(report.encode()))
    }

    @Test fun cancellationLeavesPersistentPendingEvidence() {
        var saved: SusfsApplyReport? = null
        assertThrows(CancellationException::class.java) {
            runBlocking { applySusfsPlan(plan(), { _, _ -> }, { saved = it }) { _, _ -> throw CancellationException("stop") } }
        }
        assertEquals("pending", saved!!.items.single().status)
    }

    @Test fun recoveryUsesSeparateBackupSlot() = runBlocking {
        var recoverySlot = false
        applySusfsPlan(plan(restoring = true), { _, restoring -> recoverySlot = restoring }, {}) { _, _ -> }
        assertTrue(recoverySlot)
    }

    @Test fun checkpointFailureStopsRemainingUids() {
        val target2 = SusfsPolicyTarget(uid + 1, listOf(other), false, SusfsPolicyState(SusfsHideMode.Default, false))
        val plan = planSusfsImport(listOf(SusfsApplicationHidingConfigEntry(name, true), SusfsApplicationHidingConfigEntry(other, true)), listOf(target(FakeAccess()), target2))
        var writes = 0
        assertThrows(IOException::class.java) {
            runBlocking { applySusfsPlan(plan, { _, _ -> }, { if (it.items.any { entry -> entry.status == "applied" }) throw IOException("disk_full") }) { _, _ -> writes++ } }
        }
        assertEquals(1, writes)
    }
}
