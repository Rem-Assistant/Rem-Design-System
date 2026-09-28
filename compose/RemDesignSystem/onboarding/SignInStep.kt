package com.rem.designsystem.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rem.designsystem.brand.RemAppIcon
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * The sign-in state, driven by the host's **real auth** — the component never mocks a credential. The
 * host maps its auth flow to these cases and updates the step; the enumerated states are exactly those
 * the screen must render (returning / new / checking / error / recovery).
 */
sealed interface SignInState {
    /** A credential is already present (Sign-in-with-Apple returning). Primary = "Continue as <name>". */
    data class Returning(val accountName: String) : SignInState

    /** No credential yet. Offers Google + Apple provider buttons. */
    data object New : SignInState

    /** Auth in flight after a provider tap. Primary shows the disabled spinner ("Signing in…"). */
    data object Checking : SignInState

    /** Auth failed. Shows the error notice + "Try again" and the different-account escape. */
    data class Error(val message: String) : SignInState

    /** Account-recovery path (e.g. credential revoked / needs re-auth). */
    data class Recovery(val message: String) : SignInState
}

/**
 * The **onboarding sign-in screen** (Compose sibling of the SwiftUI `OnboardingSignInTemplate`), built
 * to `docs/contracts/onboarding-sign-in.md`. Authority: shipping `OnboardingFlow.signInContent` /
 * `SignInButton` / `OnboardingLogoView` + the founder reference frame `tasks/refs/onboarding/01-sign-in.png`.
 *
 * It is sign-in's **own centered screen** — one block centered in the safe area (equal space above and
 * below, **never** bottom-pinned), contents left-aligned, capped at 560dp. It deliberately does **not**
 * use the multi-step [OnboardingScaffold]'s bottom-pinned CTA bar: that arrangement was the drift this
 * screen fixes. iOS and Android render the same arrangement, emphasis, and icons so the paired render
 * diffs clean.
 *
 * Presentational and state-driven — no auth, no navigation. The host wires real behaviour through the
 * callbacks: [onContinue] starts auth (returning / new-Apple), [onUseGoogle] the Google provider,
 * [onUseDifferentAccount] switches account, [onRetry] re-attempts after a failure.
 */
@Composable
fun OnboardingSignInScreen(
    state: SignInState,
    onContinue: () -> Unit,
    onUseDifferentAccount: () -> Unit,
    modifier: Modifier = Modifier,
    onUseGoogle: () -> Unit = {},
    onRetry: () -> Unit = {},
    title: String = "Rem",
    // Two lines, matching the SwiftUI template + the reference frame.
    tagline: String = "Turn your thoughts\ninto actions",
    legalFootnote: String = "By continuing, you agree to our Terms of Service and Privacy Policy.",
    appleMark: ImageVector = RemBrandGlyphs.AppleLogo,
    googleMark: ImageVector? = RemBrandGlyphs.GoogleG,
) {
    val colors = RemColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundPrimary),
        // Centered in the safe area — equal space above and below, never bottom-pinned.
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(RemSpacing.md),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
        ) {
            // Brand lockup mark: the **app icon** (blue squircle + white bloom), shared with iOS —
            // NOT the face mark — per the founder reference and the shipping `OnboardingLogoView`.
            RemAppIcon(size = 40.dp)

            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = title,
                    style = RemTypography.largeTitle.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.labelPrimary,
                )
                Spacer(Modifier.height(RemSpacing.xs))
                Text(
                    text = tagline,
                    style = RemTypography.title1,
                    color = colors.labelSecondary,
                    maxLines = 2,
                )
            }

            SignInActions(
                state = state,
                onContinue = onContinue,
                onUseDifferentAccount = onUseDifferentAccount,
                onUseGoogle = onUseGoogle,
                onRetry = onRetry,
                legalFootnote = legalFootnote,
                appleMark = appleMark,
                googleMark = googleMark,
            )

            // Notice card (error / recovery only) — directly below the action group, systemRed @ 12%.
            when (state) {
                is SignInState.Error -> SignInNotice(state.message)
                is SignInState.Recovery -> SignInNotice(state.message)
                else -> Unit
            }
        }
    }
}

/** The per-state provider / continue actions, `Spacing.md` below the lockup. */
@Composable
private fun SignInActions(
    state: SignInState,
    onContinue: () -> Unit,
    onUseDifferentAccount: () -> Unit,
    onUseGoogle: () -> Unit,
    onRetry: () -> Unit,
    legalFootnote: String,
    appleMark: ImageVector,
    googleMark: ImageVector?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        when (state) {
            is SignInState.Returning -> {
                SignInButton("Continue as ${state.accountName}", onClick = onContinue, leadingIcon = appleMark)
                DifferentAccountLink(onUseDifferentAccount)
            }

            SignInState.New -> {
                // Provider order: Google, then Apple (matches the shipping app). The Google "G" stays
                // untinted so it keeps its four brand colors on the filled button.
                SignInButton("Continue with Google", onClick = onUseGoogle, leadingIcon = googleMark, tintLeadingIcon = false)
                SignInButton("Continue with Apple", onClick = onContinue, leadingIcon = appleMark)
                Text(
                    text = legalFootnote,
                    style = RemTypography.caption1,
                    color = RemColors.current.labelSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SignInState.Checking -> {
                SignInButton("Signing in…", onClick = {}, enabled = false, loading = true)
            }

            is SignInState.Error -> {
                // "Try again": no leading icon, centered label.
                SignInButton("Try again", onClick = onRetry)
                DifferentAccountLink(onUseDifferentAccount)
            }

            is SignInState.Recovery -> {
                // Emphasis rule: "Sign in with a different account" is *always* a quiet labelSecondary
                // text link — never filled — so recovery offers two text links (no filled provider
                // action to emphasize), with "Try again" the accent link.
                DifferentAccountLink(onUseDifferentAccount)
                SignInTextLink("Try again", onClick = onRetry, accent = true)
            }
        }
    }
}

/** "Sign in with a different account" — the quiet labelSecondary link, identical in every state. */
@Composable
private fun DifferentAccountLink(onClick: () -> Unit) =
    SignInTextLink("Sign in with a different account", onClick = onClick, accent = false)

/**
 * The filled provider button — reproduces `SignInButton`: `buttonBackground` fill, `medium` corner
 * radius, an inverted `bodyBold` label, and an optional 18dp leading glyph. A monochrome mark (Apple)
 * tints to the inverted label; a multicolor mark (the Google "G") passes `tintLeadingIcon = false` so
 * it keeps its own colors. No glyph → a centered label.
 */
@Composable
private fun SignInButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    tintLeadingIcon: Boolean = true,
) {
    val colors = RemColors.current
    val interactive = enabled && !loading
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RemRadius.medium))
            .background(colors.buttonBackground)
            .alpha(if (interactive) 1f else RemOnboardingMetrics.disabledAlpha)
            .then(if (interactive) Modifier.clickableRole(onClick, label) else Modifier)
            .padding(RemSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = colors.backgroundPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(RemSpacing.sm))
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                // Color.Unspecified keeps a multicolor mark (Google "G") at its own colors.
                tint = if (tintLeadingIcon) colors.backgroundPrimary else Color.Unspecified,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(RemSpacing.sm))
        }
        Text(text = label, style = RemTypography.bodyBold, color = colors.backgroundPrimary)
    }
}

/** A full-width centered text link — quiet ([accent] = false, labelSecondary) or accent (systemBlue). */
@Composable
private fun SignInTextLink(label: String, onClick: () -> Unit, accent: Boolean) {
    val colors = RemColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickableRole(onClick, label)
            .padding(vertical = RemSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = RemTypography.body,
            color = if (accent) colors.systemBlue else colors.labelSecondary,
        )
    }
}

/**
 * Inline error/recovery notice — the Extend treatment reused across the sign-in error + recovery
 * states, directly below the action group. Token-bound (systemRed at 12% on a `medium`-radius
 * surface); the leading glyph resolves through the icon registry by meaning + FILL: the Material
 * Symbols `error` glyph at **FILL 1** (pairs with the iOS `exclamationmark.triangle.fill`).
 */
@Composable
private fun SignInNotice(message: String, modifier: Modifier = Modifier) {
    val colors = RemColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RemRadius.medium))
            .background(colors.systemRed.copy(alpha = 0.12f))
            .padding(RemSpacing.md),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        Text(
            text = RemMaterialSymbols.Error,
            fontFamily = RemMaterialSymbols.family(fill = 1f),
            fontSize = 14.sp,
            color = colors.systemRed,
        )
        Text(text = message, style = RemTypography.caption1, color = colors.labelPrimary)
    }
}

/**
 * The sign-in **step** for the [OnboardingSequencer] — a thin wrapper that renders the standalone
 * [OnboardingSignInScreen]. Sign-in is the flow's entry (step 0): centered on its own, with no
 * back/progress/CTA chrome, exactly as the contract requires.
 */
fun signInStep(
    state: SignInState,
    onContinue: () -> Unit,
    onUseDifferentAccount: () -> Unit,
    onUseGoogle: () -> Unit = {},
    onRetry: () -> Unit = {},
    appleMark: ImageVector = RemBrandGlyphs.AppleLogo,
    googleMark: ImageVector? = RemBrandGlyphs.GoogleG,
    id: String = "signIn",
): OnboardingStep = OnboardingStep(id = id) {
    OnboardingSignInScreen(
        state = state,
        onContinue = onContinue,
        onUseDifferentAccount = onUseDifferentAccount,
        onUseGoogle = onUseGoogle,
        onRetry = onRetry,
        appleMark = appleMark,
        googleMark = googleMark,
    )
}

@Preview(name = "Sign-in · returning", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun SignInReturningPreview() {
    RemTheme {
        OnboardingSignInScreen(
            state = SignInState.Returning(accountName = "Sam"),
            onContinue = {},
            onUseDifferentAccount = {},
        )
    }
}

@Preview(name = "Sign-in · new", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun SignInNewPreview() {
    RemTheme {
        OnboardingSignInScreen(state = SignInState.New, onContinue = {}, onUseDifferentAccount = {})
    }
}

@Preview(name = "Sign-in · checking", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun SignInCheckingPreview() {
    RemTheme {
        OnboardingSignInScreen(state = SignInState.Checking, onContinue = {}, onUseDifferentAccount = {})
    }
}

@Preview(name = "Sign-in · error (dark)", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun SignInErrorPreview() {
    RemTheme(darkTheme = true) {
        OnboardingSignInScreen(
            state = SignInState.Error("We couldn't sign you in. Check your connection and try again."),
            onContinue = {},
            onUseDifferentAccount = {},
        )
    }
}

@Preview(name = "Sign-in · recovery", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun SignInRecoveryPreview() {
    RemTheme {
        OnboardingSignInScreen(
            state = SignInState.Recovery("Your session expired. Sign in again to pick up where you left off."),
            onContinue = {},
            onUseDifferentAccount = {},
        )
    }
}
