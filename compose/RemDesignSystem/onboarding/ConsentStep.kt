package com.rem.designsystem.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.primitives.ContainedIconSize
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * The **reproduced consent step** — "Privacy by design" (Reproduce mode; authority = the founder
 * onboarding reference frame `02-consent.png` + `AIDataSharingConsentView` / Figma Privacy `410:16`),
 * built to `docs/contracts/onboarding-consent.md`. The Compose sibling of the SwiftUI
 * `OnboardingConsentTemplate`; the two render the same screen so the paired iOS⟷Android evidence diffs
 * clean.
 *
 * All copy is verbatim from the reference and MUST NOT be edited without an authority change:
 *  - title:    "Privacy by design"
 *  - subtitle: "Rem uses your data to answer you and act on the things you ask. You can review or
 *               delete it anytime in Settings."
 *  - Terms of Service — "How Rem accounts, subscriptions, and approved actions work."
 *  - Privacy Policy   — "What Rem, your gateway, and AI or voice providers process."
 *  - primary:  "Accept and Continue" (or "Try again" in the error state)
 *  - footer:   By tapping "Accept and Continue," you agree to our Terms of Service and Privacy Policy.
 *
 * **Icons resolve through the Material Symbols variable font at their registry FILL** (icon-registry
 * rule 2 — never legacy `Icons.Filled.*`, which is always-filled and can't honour an outline row).
 * This is the drift fix: the hero is `shield_lock` at **FILL 1** (pairs with iOS `lock.shield.fill` —
 * NOT `Security`, a shield-*check*), and the two legal rows + their chevrons are outline (**FILL 0**),
 * matching the iOS SF Symbols glyph-for-glyph.
 *
 * The two rows open the legal documents ([onOpenTerms]/[onOpenPrivacy] — page sheets on the host);
 * [onAccept] persists consent and the host advances the sequencer. [state] is the single source for
 * the primary action and notice: Loading shows the spinner, while Error carries the notice and flips
 * the CTA to "Try again". Those values cannot drift apart at a call site.
 */
sealed interface ConsentState {
    data object Idle : ConsentState
    data object Loading : ConsentState
    data class Error(val message: String) : ConsentState
}

fun consentStep(
    onAccept: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    state: ConsentState = ConsentState.Idle,
    id: String = "consent",
): OnboardingStep = OnboardingStep(id = id) { scope ->
    OnboardingScaffold(
        primary = OnboardingAction(
            label = if (state is ConsentState.Error) "Try again" else "Accept and Continue",
            onClick = onAccept,
            style = OnboardingActionStyle.Primary,
            loading = state is ConsentState.Loading,
            enabled = state !is ConsentState.Loading,
        ),
        // Hero = shield-lock (`shield_lock`, FILL 1) — the registry consent hero, matching the iOS
        // `lock.shield.fill`. NOT `Security` (a shield-with-check, the near-miss the registry flags).
        hero = OnboardingHero(
            glyph = RemMaterialSymbols.ShieldLock,
            glyphFill = 1f,
            contentDescription = "Privacy",
        ),
        title = "Privacy by design",
        subtitle = "Rem uses your data to answer you and act on the things you ask. " +
            "You can review or delete it anytime in Settings.",
        legalFooter = "By tapping \"Accept and Continue,\" you agree to our Terms of Service and Privacy Policy.",
        bottomBarState = when (state) {
            ConsentState.Idle, ConsentState.Loading -> OnboardingBottomBarState.Standard
            is ConsentState.Error -> OnboardingBottomBarState.Error(state.message)
        },
        background = OnboardingBackground.Primary,
        progress = scope.progress,
        onBack = scope.onBack,
    ) {
        // Grouped card holding the two legal rows — the iOS inset-grouped section, on backgroundSecondary
        // at `medium` radius (the contract's grouped-card radius, matched to iOS).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(RemRadius.medium))
                .background(RemColors.current.backgroundSecondary),
        ) {
            ConsentLegalRow(
                glyph = RemMaterialSymbols.Description, // doc.text on iOS — FILL 0 (outline)
                title = "Terms of Service",
                subtitle = "How Rem accounts, subscriptions, and approved actions work.",
                onClick = onOpenTerms,
                showSeparator = true,
            )
            ConsentLegalRow(
                glyph = RemMaterialSymbols.Shield, // shield on iOS — FILL 0 (outline)
                title = "Privacy Policy",
                subtitle = "What Rem, your gateway, and AI or voice providers process.",
                onClick = onOpenPrivacy,
                showSeparator = false,
            )
        }
    }
}

/**
 * A single consent legal row (leading icon · title + subtitle · trailing chevron). This is a **bespoke,
 * hand-rolled row** — there is no Compose `ListRow` primitive in this design-system module yet, so
 * (like the CTA button flagged in `OnboardingSupport.kt`) it composes the row natively with token-bound
 * metrics rather than forking a canonical component. Flagged for extraction; until a `ListRow` primitive
 * lands, the manifest records this as `pendingNative`, not a canonical reuse.
 *
 * Leading + trailing glyphs are Material Symbols at **FILL 0** (outline) — matching the iOS `doc.text` /
 * `shield` / `chevron.right` per the icon registry.
 */
@Composable
private fun ConsentLegalRow(
    glyph: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showSeparator: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = RemColors.current
    Column(modifier = modifier.clickableRole(onClick = onClick, label = title)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // horizontal md, vertical sm — matches the iOS `ListRow` row metrics.
                .padding(horizontal = RemSpacing.md, vertical = RemSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContainedIcon(
                glyph = glyph,
                glyphFill = 0f,
                fill = ContainedIconFill.Subtle,
                size = ContainedIconSize.Small,
                contentDescription = null,
            )
            Spacer(Modifier.width(RemSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = RemTypography.bodyBold, color = colors.labelPrimary)
                Spacer(Modifier.height(3.dp)) // matches the iOS ListRow title↔subtitle gap
                Text(text = subtitle, style = RemTypography.caption1, color = colors.labelSecondary)
            }
            Spacer(Modifier.width(RemSpacing.sm))
            Text(
                text = RemMaterialSymbols.ChevronRight,
                fontFamily = RemMaterialSymbols.family(fill = 0f),
                fontSize = 20.sp,
                color = colors.labelTertiary,
            )
        }
        if (showSeparator) {
            Box(
                modifier = Modifier
                    .padding(start = RemSpacing.xxl + RemSpacing.md)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.separator),
            )
        }
    }
}

@Preview(name = "Consent · light", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun ConsentLightPreview() {
    RemTheme {
        OnboardingSequencer(
            steps = listOf(
                signInStep(state = SignInState.Returning("Sam"), onContinue = {}, onUseDifferentAccount = {}),
                consentStep(onAccept = {}, onOpenTerms = {}, onOpenPrivacy = {}),
            ),
            state = rememberOnboardingSequencerState(stepCount = 2, initialIndex = 1),
        )
    }
}

@Preview(name = "Consent · dark", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun ConsentDarkPreview() {
    RemTheme(darkTheme = true) {
        OnboardingSequencer(
            steps = listOf(
                signInStep(state = SignInState.Returning("Sam"), onContinue = {}, onUseDifferentAccount = {}),
                consentStep(onAccept = {}, onOpenTerms = {}, onOpenPrivacy = {}),
            ),
            state = rememberOnboardingSequencerState(stepCount = 2, initialIndex = 1),
        )
    }
}

@Preview(name = "Consent · error (dark)", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun ConsentErrorPreview() {
    RemTheme(darkTheme = true) {
        OnboardingSequencer(
            steps = listOf(
                signInStep(state = SignInState.Returning("Sam"), onContinue = {}, onUseDifferentAccount = {}),
                consentStep(
                    onAccept = {}, onOpenTerms = {}, onOpenPrivacy = {},
                    state = ConsentState.Error(
                        "We couldn't save your choice. Check your connection and try again.",
                    ),
                ),
            ),
            state = rememberOnboardingSequencerState(stepCount = 2, initialIndex = 1),
        )
    }
}
