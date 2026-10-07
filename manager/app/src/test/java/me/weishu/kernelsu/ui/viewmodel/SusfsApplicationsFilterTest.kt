package me.weishu.kernelsu.ui.viewmodel

import me.weishu.kernelsu.ui.util.SusfsHideMode
import me.weishu.kernelsu.ui.util.SusfsPolicyState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SusfsApplicationsFilterTest {
    @Test fun unknownIsNotPresentedAsDisabledOrInherited() {
        val state = SusfsPolicyState(error = "errno=19")
        assertTrue(SusfsAppFilter.All.matches(state))
        assertTrue(SusfsAppFilter.Unknown.matches(state))
        assertFalse(SusfsAppFilter.Enabled.matches(state))
        assertFalse(SusfsAppFilter.Disabled.matches(state))
        assertFalse(SusfsAppFilter.Default.matches(state))
    }

    @Test fun inheritedOffAppearsInDefaultAndDisabled() {
        val state = SusfsPolicyState(SusfsHideMode.Default, false)
        assertTrue(SusfsAppFilter.Default.matches(state))
        assertTrue(SusfsAppFilter.Disabled.matches(state))
        assertFalse(SusfsAppFilter.Enabled.matches(state))
        assertFalse(SusfsAppFilter.Unknown.matches(state))
    }

    @Test fun inheritedOnAppearsInDefaultAndEnabled() {
        val state = SusfsPolicyState(SusfsHideMode.Default, true)
        assertTrue(SusfsAppFilter.Default.matches(state))
        assertTrue(SusfsAppFilter.Enabled.matches(state))
        assertFalse(SusfsAppFilter.Disabled.matches(state))
    }

    @Test fun explicitRulesAreNotInherited() {
        assertFalse(SusfsAppFilter.Default.matches(SusfsPolicyState(SusfsHideMode.Enabled, true)))
        assertFalse(SusfsAppFilter.Default.matches(SusfsPolicyState(SusfsHideMode.Disabled, false)))
    }

    @Test fun rootGrantedIsNotMisclassifiedAsPolicyOff() {
        val state = SusfsPolicyState(SusfsHideMode.Default, false, allowSu = true)
        assertTrue(SusfsAppFilter.All.matches(state))
        assertFalse(SusfsAppFilter.Disabled.matches(state))
        assertFalse(SusfsAppFilter.Default.matches(state))
    }
}
