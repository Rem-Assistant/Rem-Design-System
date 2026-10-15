package com.rem.designsystem.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.icons.RemMaterialSymbol
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.primitives.ContainedIconSize
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.tokens.RemColors
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
 *  - primary:  "Accept and Continue"
 *  - footer:   By tapping "Accept and Continue," you agree to our Terms of Service and Privacy Policy.
 *
 * **Icons resolve through the canonical `ContainedIcon` primitive and the static Material Symbols
 * font selected for their registry FILL** (icon-registry rule 2 — never legacy `Icons.Filled.*`,
 * which is always-filled and can't honour an outline row). SwiftUI uses the paired SF Symbol through
 * its `ContainedIcon`; the platform-native backends intentionally share the semantic registry row,
 * FILL, size, color, and render contract rather than a font implementation.
 * This is the drift fix: the hero is `shield_lock` at **FILL 1** (pairs with iOS `lock.shield.fill` —
 * NOT `Security`, a shield-*check*), and the two legal rows + their chevrons are outline (**FILL 0**),
 * matching the iOS SF Symbols glyph-for-glyph.
 *
 * The two rows open the legal documents ([onOpenTerms]/[onOpenPrivacy] — page sheets on the host);
 * [onAccept] persists consent and the host advances the sequencer. The shipping consent view has no
 * consent-local loading or error state, so this step does not invent either one.
 */
fun consentStep(
    onAccept: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    id: String = "consent",
): OnboardingStep = OnboardingStep(id = id) { scope ->
    OnboardingScaffold(
        primary = OnboardingAction(
            label = "Accept and Continue",
            onClick = onAccept,
            style = OnboardingActionStyle.Primary,
        ),
        // Hero = shield-lock (`shield_lock`, FILL 1) — the registry consent hero, matching the iOS
        // `lock.shield.fill`. NOT `Security` (a shield-with-check, the near-miss the registry flags).
        hero = OnboardingHero(
            symbol = RemMaterialSymbols.PrivacyLockShield,
            contentDescription = "Privacy",
        ),
        title = "Privacy by design",
        subtitle = "Rem uses your data to answer you and act on the things you ask. " +
            "You can review or delete it anytime in Settings.",
        legalFooter = "By tapping \"Accept and Continue,\" you agree to our Terms of Service and Privacy Policy.",
        background = OnboardingBackground.Primary,
        progress = scope.progress,
        onBack = scope.onBack,
    ) {
        // Canonical grouped Section: backgroundSecondary + xlarge radius, no outer stroke.
        RemSection(modifier = Modifier.fillMaxWidth()) {
            ConsentLegalRow(
                symbol = RemMaterialSymbols.TermsDocument, // doc.text on iOS — FILL 0 (outline)
                title = "Terms of Service",
                subtitle = "How Rem accounts, subscriptions, and approved actions work.",
                onClick = onOpenTerms,
                showSeparator = true,
            )
            ConsentLegalRow(
                symbol = RemMaterialSymbols.PrivacyPolicy, // shield on iOS — FILL 0 (outline)
                title = "Privacy Policy",
                subtitle = "What Rem, your gateway, and AI or voice providers process.",
                onClick = onOpenPrivacy,
                showSeparator = false,
            )
        }
    }
}

/** A legal-document configuration of the canonical [ListRow]. */
@Composable
private fun ConsentLegalRow(
    symbol: RemMaterialSymbol,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showSeparator: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = RemColors.current
    ListRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        showSeparator = showSeparator,
        modifier = modifier,
        leading = {
            ContainedIcon(
                symbol = symbol,
                fill = ContainedIconFill.Subtle,
                size = ContainedIconSize.Small,
                contentDescription = null,
            )
        },
        trailing = {
            Text(
                text = RemMaterialSymbols.DisclosureChevron.glyph,
                fontFamily = RemMaterialSymbols.family(RemMaterialSymbols.DisclosureChevron),
                fontSize = 20.sp,
                color = colors.labelTertiary,
            )
        },
    )
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
