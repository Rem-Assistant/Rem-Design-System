package com.rem.designsystem.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.brand.RemFaceMark
import com.rem.designsystem.brand.RemFaceMarkMode
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * The sign-in state, driven by the host's **real auth** — the component never mocks a credential. The
 * host maps its auth flow to these cases and updates the step; the enumerated states are exactly those
 * the sequencer must render (returning / new / checking / error / recovery).
 */
sealed interface SignInState {
    /** A credential is already present (Sign-in-with-Apple returning). Primary = "Continue as <name>". */
    data class Returning(val accountName: String) : SignInState

    /** No credential yet. Offers Apple + Google provider buttons. */
    data object New : SignInState

    /** Auth in flight after a provider tap. Primary shows the disabled spinner ("Signing in…"). */
    data object Checking : SignInState

    /** Auth failed. Shows the error message + "Try again" and the different-account escape. */
    data class Error(val message: String) : SignInState

    /** Account-recovery path (e.g. credential revoked / needs re-auth). Different-account is primary. */
    data class Recovery(val message: String) : SignInState
}

/**
 * The **reproduced sign-in step** (Reproduce mode; authority = the founder onboarding reference frame
 * `01-sign-in.png` + `OnboardingFlow.signInContent` / Figma Login `411:15`). The brand lockup (logo +
 * "Rem" + "Turn your thoughts into actions") is left-aligned exactly as the reference; the
 * Sign-in-with-Apple treatment is the filled black button with the Apple mark and a bold inverted
 * label. It hosts into the shared [OnboardingScaffold] so back/progress/CTA chrome matches the flow.
 *
 * All behaviour is host-wired: [onContinue] starts real auth (returning/new-Apple), [onUseGoogle] the
 * Google provider, [onUseDifferentAccount] switches account, [onRetry] re-attempts after an error. The
 * host advances the sequencer (`scope.advance()`) only when auth actually succeeds — the button does
 * not fake-advance.
 *
 * error/recovery visuals are an **Extend** around the reproduced default (the reference frame shows the
 * returning state only); they reuse a token-bound inline message treatment and are flagged for founder
 * confirmation.
 */
fun signInStep(
    state: SignInState,
    onContinue: () -> Unit,
    onUseDifferentAccount: () -> Unit,
    onUseGoogle: () -> Unit = {},
    onRetry: () -> Unit = {},
    appleMark: ImageVector = RemBrandGlyphs.AppleLogo,
    googleMark: ImageVector? = null,
    id: String = "signIn",
): OnboardingStep = OnboardingStep(id = id) { scope ->
    val primary: OnboardingAction
    val secondary: OnboardingAction?
    when (state) {
        is SignInState.Returning -> {
            primary = OnboardingAction(
                label = "Continue as ${state.accountName}",
                onClick = onContinue,
                style = OnboardingActionStyle.Primary,
                leadingIcon = appleMark,
            )
            secondary = OnboardingAction(
                label = "Sign in with a different account",
                onClick = onUseDifferentAccount,
                style = OnboardingActionStyle.TextSubtle,
            )
        }
        SignInState.New -> {
            primary = OnboardingAction(
                label = "Continue with Apple",
                onClick = onContinue,
                style = OnboardingActionStyle.Primary,
                leadingIcon = appleMark,
            )
            // Second provider button — brand asset supplied by the host (googleMark); placeholder-less
            // when absent, per the repo's brand-asset-debt convention.
            secondary = OnboardingAction(
                label = "Continue with Google",
                onClick = onUseGoogle,
                style = OnboardingActionStyle.Primary,
                leadingIcon = googleMark,
            )
        }
        SignInState.Checking -> {
            primary = OnboardingAction(
                label = "Signing in…",
                onClick = {},
                style = OnboardingActionStyle.Primary,
                loading = true,
                enabled = false,
            )
            secondary = null
        }
        is SignInState.Error -> {
            primary = OnboardingAction(
                label = "Try again",
                onClick = onRetry,
                style = OnboardingActionStyle.Primary,
            )
            secondary = OnboardingAction(
                label = "Sign in with a different account",
                onClick = onUseDifferentAccount,
                style = OnboardingActionStyle.TextSubtle,
            )
        }
        is SignInState.Recovery -> {
            primary = OnboardingAction(
                label = "Sign in with a different account",
                onClick = onUseDifferentAccount,
                style = OnboardingActionStyle.Primary,
            )
            secondary = OnboardingAction(
                label = "Try again",
                onClick = onRetry,
                style = OnboardingActionStyle.TextAccent,
            )
        }
    }

    OnboardingScaffold(
        primary = primary,
        secondary = secondary,
        legalFooter = "By continuing, you agree to our Terms of Service and Privacy Policy.",
        background = OnboardingBackground.Primary,
        progress = scope.progress,
        onBack = scope.onBack,
    ) {
        // Left-aligned brand lockup, matching the reference frame's composition. Pushed down so it sits
        // around the vertical mid-point as in the reference (approximate; flagged for pixel-verify on
        // the render runner).
        Spacer(Modifier.height(RemSpacing.xxxl))
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            // Brand lockup mark: the real, canonical Rem face (shared with iOS) — not a generic
            // Material "face" glyph. Tinted brand-blue, matching the SwiftUI sign-in lockup.
            RemFaceMark(
                mode = RemFaceMarkMode.Idle,
                tint = RemColors.current.brandBlue,
                size = 56.dp,
            )
            Spacer(Modifier.height(RemSpacing.md))
            Text(
                text = "Rem",
                style = RemTypography.largeTitle.copy(fontWeight = RemTypography.title1Bold.fontWeight),
                color = RemColors.current.labelPrimary,
            )
            Spacer(Modifier.height(RemSpacing.xs))
            Text(
                // Tagline reads large + secondary in the reference; title1 is the closest role.
                // Exact size pending confirmation against `OnboardingFlow.signInContent` source.
                text = "Turn your thoughts into actions",
                style = RemTypography.title1,
                color = RemColors.current.labelSecondary,
            )
            if (state is SignInState.Error || state is SignInState.Recovery) {
                Spacer(Modifier.height(RemSpacing.lg))
                val message = when (state) {
                    is SignInState.Error -> state.message
                    is SignInState.Recovery -> state.message
                    else -> ""
                }
                SignInNotice(message = message)
            }
        }
    }
}

/**
 * Inline error/recovery notice — the Extend treatment reused across the sign-in error + recovery
 * states. Token-bound (systemRed tint at 12% on a rounded surface); mirrors the app's contextual
 * message tone rather than inventing a new one.
 */
@Composable
private fun SignInNotice(message: String, modifier: Modifier = Modifier) {
    val colors = RemColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RemRadius.medium))
            .background(colors.systemRed.copy(alpha = 0.12f))
            .padding(horizontal = RemSpacing.md, vertical = RemSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = message, style = RemTypography.subheadline, color = colors.labelPrimary)
    }
}

@Preview(name = "Sign-in · returning", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun SignInReturningPreview() {
    RemTheme {
        OnboardingSequencer(
            steps = listOf(
                signInStep(
                    state = SignInState.Returning(accountName = "Sam"),
                    onContinue = {},
                    onUseDifferentAccount = {},
                ),
            ),
        )
    }
}

@Preview(name = "Sign-in · new", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun SignInNewPreview() {
    RemTheme {
        OnboardingSequencer(
            steps = listOf(
                signInStep(state = SignInState.New, onContinue = {}, onUseDifferentAccount = {}),
            ),
        )
    }
}

@Preview(name = "Sign-in · error (dark)", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun SignInErrorPreview() {
    RemTheme(darkTheme = true) {
        OnboardingSequencer(
            steps = listOf(
                signInStep(
                    state = SignInState.Error("We couldn't sign you in. Check your connection and try again."),
                    onContinue = {},
                    onUseDifferentAccount = {},
                ),
            ),
        )
    }
}
