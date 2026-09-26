package com.rem.designsystem.onboarding

import androidx.compose.runtime.Composable
import com.figma.code.connect.FigmaConnect
import com.figma.code.connect.FigmaProperty
import com.figma.code.connect.FigmaType

/**
 * Code Connect for the onboarding steps — the Compose twins of the SwiftUI onboarding Code Connect,
 * binding the Figma onboarding masters to the shipped Kotlin builders. Co-located so a rename surfaces
 * as drift (`figma connect check`).
 *
 * Like every `*.figma.kt` in this module, these are **excluded from the library's Gradle build** (the
 * app never links `com.figma.code.connect`); the `figma connect` CLI reads them. Dormant-but-ready —
 * publishing is plan-gated (same as ContainedIcon / RemButton / the SwiftUI Code Connect).
 *
 * The sequencer *shell* itself is a flow, not a variant component, so it is represented on the Figma
 * **Flows** page (Sign-in → Consent → middle steps, no deploy phase) rather than as a single variant
 * master; only the reproduced step masters get a Code Connect binding here.
 *
 * ⚠️ VERIFY ON THE ANDROID RUNNER: Compose Code Connect's DSL is newer than SwiftUI's — confirm the
 * `@FigmaProperty` enum-mapping signature against the pinned `com.figma.code.connect` version, exactly
 * as flagged in `ContainedIcon.figma.kt`.
 */

/** Sign-in — Figma Login `411:15` (`OnboardingFlow.signInContent`). The Figma `State` variant maps to [SignInState]. */
@FigmaConnect("https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=411-15")
class SignInStepDoc {
    @FigmaProperty(FigmaType.Enum, "State")
    val state: String = "Returning"

    @Composable
    fun example() {
        // The host maps its real-auth state to SignInState; the sequencer renders the returned step.
        val signInState = when (state) {
            "New" -> SignInState.New
            "Checking" -> SignInState.Checking
            "Error" -> SignInState.Error("We couldn't sign you in.")
            "Recovery" -> SignInState.Recovery("Please sign in again.")
            else -> SignInState.Returning(accountName = "Sam")
        }
        OnboardingSequencer(
            steps = listOf(
                signInStep(state = signInState, onContinue = {}, onUseDifferentAccount = {}),
            ),
        )
    }
}

/** Consent — Figma Privacy `410:16` (`AIDataSharingConsentView`). */
@FigmaConnect("https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=410-16")
class ConsentStepDoc {
    @Composable
    fun example() {
        OnboardingSequencer(
            steps = listOf(
                consentStep(onAccept = {}, onOpenTerms = {}, onOpenPrivacy = {}),
            ),
        )
    }
}
