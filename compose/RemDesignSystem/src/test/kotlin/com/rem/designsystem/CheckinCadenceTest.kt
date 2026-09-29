package com.rem.designsystem

import com.rem.designsystem.onboarding.CheckinStatus
import com.rem.designsystem.onboarding.checkinDefaultPeriods
import com.rem.designsystem.onboarding.isPrimaryEnabled
import com.rem.designsystem.onboarding.primaryLabel
import com.rem.designsystem.onboarding.rowsInteractive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pure Check-in save-lifecycle model — the CTA label, enablement, and row-lock rules that the
 * SwiftUI `OnboardingCheckinTemplate` and the Compose `OnboardingCheckinScreen` must agree on. Tested
 * without composing the screen so the cross-platform contract is asserted, not eyeballed.
 */
class CheckinCadenceTest {

    @Test
    fun ctaLabelTracksTheSaveLifecycle() {
        assertEquals("Continue", CheckinStatus.Default.primaryLabel())
        assertEquals("Continue", CheckinStatus.Edited.primaryLabel())
        assertEquals("Saving…", CheckinStatus.Saving.primaryLabel())
        assertEquals("Saved", CheckinStatus.Saved.primaryLabel())
        assertEquals("Try again", CheckinStatus.Failure("boom").primaryLabel())
    }

    @Test
    fun actionableStatesRequireAtLeastOneSelectedTime() {
        // "Start with one": Continue is inert when nothing is selected.
        assertFalse(CheckinStatus.Default.isPrimaryEnabled(anyEnabled = false))
        assertTrue(CheckinStatus.Default.isPrimaryEnabled(anyEnabled = true))
        assertTrue(CheckinStatus.Edited.isPrimaryEnabled(anyEnabled = true))
    }

    @Test
    fun savingAndSavedLockTheCtaWhileFailureReenablesTheRetry() {
        assertFalse(CheckinStatus.Saving.isPrimaryEnabled(anyEnabled = true))
        assertFalse(CheckinStatus.Saved.isPrimaryEnabled(anyEnabled = true))
        assertTrue(CheckinStatus.Failure("boom").isPrimaryEnabled(anyEnabled = false))
    }

    @Test
    fun rowsLockOnlyWhileSavingOrJustSaved() {
        assertTrue(CheckinStatus.Default.rowsInteractive())
        assertTrue(CheckinStatus.Edited.rowsInteractive())
        assertFalse(CheckinStatus.Saving.rowsInteractive())
        assertFalse(CheckinStatus.Saved.rowsInteractive())
        assertTrue(CheckinStatus.Failure("boom").rowsInteractive())
    }

    @Test
    fun defaultCadenceStartsWithMorningOnAndTheOthersOff() {
        val periods = checkinDefaultPeriods()
        assertEquals(listOf("morning", "midday", "evening"), periods.map { it.id })
        assertTrue(periods.first { it.id == "morning" }.enabled)
        assertEquals("8:00 AM", periods.first { it.id == "morning" }.time)
        assertFalse(periods.first { it.id == "midday" }.enabled)
        assertFalse(periods.first { it.id == "evening" }.enabled)
    }
}
