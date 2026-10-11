package com.rem.designsystem.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rem.designsystem.tokens.RemTheme

/**
 * Assembles the canonical Rem onboarding order: **Sign-in → Consent → [middle steps]**. There is no
 * deploy/provisioning slot — the sequence is exactly these ordered slots. The #12 middle steps
 * (Connectors → Check-in → Voice) are passed in via [middleSteps]; this slice owns the shell + the two
 * reproduced steps and hands the rest off unchanged.
 *
 * The host wires real behaviour into [signIn] and [consent] (auth + consent persistence) and advances
 * the sequencer only on success. [middleSteps] is empty here until #12 lands.
 */
fun remOnboardingSteps(
    signIn: OnboardingStep,
    consent: OnboardingStep,
    middleSteps: List<OnboardingStep> = emptyList(),
): List<OnboardingStep> = buildList {
    add(signIn)
    add(consent)
    addAll(middleSteps)
}

/**
 * A placeholder for a #12 middle step, used only to demonstrate the handoff (progress advances,
 * Continue/Skip work) in previews. #12 replaces these with the real Connectors/Check-in/Voice steps —
 * which, being ordinary [OnboardingStep]s built on the same [OnboardingScaffold], need no shell change.
 */
internal fun placeholderMiddleStep(
    id: String,
    title: String,
    subtitle: String,
): OnboardingStep = OnboardingStep(id = id) { scope ->
    OnboardingScaffold(
        primary = OnboardingAction(label = "Continue", onClick = scope::advance),
        secondary = OnboardingAction(
            label = "Skip",
            onClick = scope::skip,
            style = OnboardingActionStyle.TextAccent,
        ),
        hero = OnboardingHero(icon = Icons.Filled.Settings),
        title = title,
        subtitle = subtitle,
        background = OnboardingBackground.Secondary,
        progress = scope.progress,
        onBack = scope.onBack,
    )
}

@Preview(name = "Full flow · sign-in → consent → middle slots", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun OnboardingFullFlowPreview() {
    RemTheme {
        OnboardingSequencer(
            steps = remOnboardingSteps(
                signIn = signInStep(
                    state = SignInState.Returning("Sam"),
                    onContinue = {},
                    onUseDifferentAccount = {},
                ),
                consent = consentStep(onAccept = {}, onOpenTerms = {}, onOpenPrivacy = {}),
                middleSteps = listOf(
                    // Stand-ins for #12 — real steps arrive from that slice.
                    placeholderMiddleStep(
                        id = "connectors",
                        title = "Connectors",
                        subtitle = "Connect Rem to the tools you use so it can keep you up to date and surface what needs doing.",
                    ),
                    placeholderMiddleStep(
                        id = "checkin",
                        title = "When should Rem check in?",
                        subtitle = "At each time you pick, Rem writes you a brief on what came in.",
                    ),
                ),
            ),
            // Start mid-flow so the preview shows progress + the back affordance active.
            state = rememberOnboardingSequencerState(stepCount = 4, initialIndex = 1),
        )
    }
}
