package com.rem.designsystem.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
 * onboarding reference frame `02-consent.png` + `AIDataSharingConsentView` / Figma Privacy `410:16`).
 * All copy is verbatim from the reference and MUST NOT be edited without an authority change:
 *  - title:    "Privacy by design"
 *  - subtitle: "Rem uses your data to answer you and act on the things you ask. You can review or
 *               delete it anytime in Settings."
 *  - Terms of Service — "How Rem accounts, subscriptions, and approved actions work."
 *  - Privacy Policy   — "What Rem, your gateway, and AI or voice providers process."
 *  - primary:  "Accept and Continue"
 *  - footer:   By tapping "Accept and Continue," you agree to our Terms of Service and Privacy Policy.
 *
 * The two rows open the legal documents ([onOpenTerms]/[onOpenPrivacy] — page sheets on the host);
 * [onAccept] persists consent and the host advances the sequencer. Leading glyphs reuse the canonical
 * [ContainedIcon]; the iOS reference uses `doc.text` / `shield` SF Symbols — the Material equivalents
 * default here (overridable), form diverging per the SPEC contract.
 */
fun consentStep(
    onAccept: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    accepting: Boolean = false,
    termsIcon: ImageVector = Icons.Filled.Info,
    privacyIcon: ImageVector = Icons.Filled.Lock,
    id: String = "consent",
): OnboardingStep = OnboardingStep(id = id) { scope ->
    OnboardingScaffold(
        primary = OnboardingAction(
            label = "Accept and Continue",
            onClick = onAccept,
            style = OnboardingActionStyle.Primary,
            loading = accepting,
            enabled = !accepting,
        ),
        hero = OnboardingHero(icon = Icons.Filled.Lock, contentDescription = "Privacy"),
        title = "Privacy by design",
        subtitle = "Rem uses your data to answer you and act on the things you ask. " +
            "You can review or delete it anytime in Settings.",
        legalFooter = "By tapping \"Accept and Continue,\" you agree to our Terms of Service and Privacy Policy.",
        background = OnboardingBackground.Primary,
        progress = scope.progress,
        onBack = scope.onBack,
    ) {
        // Grouped card holding the two legal rows — the iOS inset-grouped section, on backgroundSecondary.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(RemRadius.large))
                .background(RemColors.current.backgroundSecondary),
        ) {
            ConsentLegalRow(
                icon = termsIcon,
                title = "Terms of Service",
                subtitle = "How Rem accounts, subscriptions, and approved actions work.",
                onClick = onOpenTerms,
                showSeparator = true,
            )
            ConsentLegalRow(
                icon = privacyIcon,
                title = "Privacy Policy",
                subtitle = "What Rem, your gateway, and AI or voice providers process.",
                onClick = onOpenPrivacy,
                showSeparator = false,
            )
        }
    }
}

@Composable
private fun ConsentLegalRow(
    icon: ImageVector,
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
                .padding(horizontal = RemSpacing.md, vertical = RemSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContainedIcon(
                icon = icon,
                fill = ContainedIconFill.Subtle,
                size = ContainedIconSize.Small,
                contentDescription = null,
            )
            Spacer(Modifier.width(RemSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = RemTypography.bodyBold, color = colors.labelPrimary)
                Spacer(Modifier.height(2.dp))
                Text(text = subtitle, style = RemTypography.subheadline, color = colors.labelSecondary)
            }
            Spacer(Modifier.width(RemSpacing.sm))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.labelTertiary,
                modifier = Modifier.size(20.dp),
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
