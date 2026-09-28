package com.rem.designsystem

import com.rem.designsystem.onboarding.SignInInterruptionPresentation
import com.rem.designsystem.onboarding.SignInState
import com.rem.designsystem.onboarding.interruptionPresentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SignInInterruptionPresentationTest {

    @Test
    fun errorAndRecoveryShareNoticeContractAndActionOrder() {
        val error = requireNotNull(SignInState.Error("Auth failed").interruptionPresentation())
        val recovery = requireNotNull(SignInState.Recovery("Session expired").interruptionPresentation())
        val expectedActions = listOf(
            SignInInterruptionPresentation.Action.Retry,
            SignInInterruptionPresentation.Action.DifferentAccount,
        )

        assertEquals("Auth failed", error.message)
        assertEquals("Session expired", recovery.message)
        assertEquals(expectedActions, error.actions)
        assertEquals(expectedActions, recovery.actions)
    }

    @Test
    fun uninterruptedStatesDoNotRenderTheSharedNotice() {
        assertNull(SignInState.Returning("Sam").interruptionPresentation())
        assertNull(SignInState.New.interruptionPresentation())
        assertNull(SignInState.Checking.interruptionPresentation())
    }
}
