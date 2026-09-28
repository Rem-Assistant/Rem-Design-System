package com.rem.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.rem.designsystem.brand.RemFaceMark
import com.rem.designsystem.brand.RemFaceMarkMode
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.onboarding.ConsentState
import com.rem.designsystem.onboarding.LegalDocumentScreen
import com.rem.designsystem.onboarding.OnboardingSequencer
import com.rem.designsystem.onboarding.OnboardingSignInScreen
import com.rem.designsystem.onboarding.SignInState
import com.rem.designsystem.onboarding.consentStep
import com.rem.designsystem.onboarding.previewPrivacySections
import com.rem.designsystem.onboarding.previewTermsSections
import com.rem.designsystem.onboarding.rememberOnboardingSequencerState
import com.rem.designsystem.onboarding.signInStep
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.primitives.ContainedIconSize
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemTheme
import org.junit.Rule
import org.junit.Test

/**
 * Screenshot EVIDENCE (not a golden-diff gate): renders the Compose design-system components to PNGs
 * via Paparazzi — pure JVM/LayoutLib, no emulator — so the Android output is visible next to the
 * SwiftUI renders. Mirrors each component's own `@Preview`. Run with `recordPaparazziDebug`.
 */
class EvidenceSnapshots {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    private fun shot(name: String, content: @Composable () -> Unit) =
        paparazzi.snapshot(name = name, composable = content)

    @Test
    fun containedIcon() {
        shot("ContainedIcon-light") { RemTheme { iconRow() } }
        shot("ContainedIcon-dark") { RemTheme(darkTheme = true) { iconRow() } }
    }

    // Mirrors the iOS ContainedIcon gallery glyph-for-glyph — now via the Material Symbols glyph
    // overload at each registry FILL, so the side-by-side table is a true comparison: shield_lock
    // (≈ lock.shield.fill, FILL 1, tinted+large), description (≈ doc.text, FILL 0, subtle), shield
    // (≈ shield, FILL 0, subtle) — on the primary background so the "light" shot is white, not the
    // Paparazzi default (which otherwise made the subtle tile vanish on dark).
    @Composable
    private fun iconRow() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RemColors.current.backgroundPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp),
            ) {
                ContainedIcon(
                    glyph = RemMaterialSymbols.ShieldLock,
                    glyphFill = 1f,
                    fill = ContainedIconFill.Tint(RemColors.current.brandBlue),
                    size = ContainedIconSize.Large,
                )
                ContainedIcon(glyph = RemMaterialSymbols.Description, glyphFill = 0f, fill = ContainedIconFill.Subtle)
                ContainedIcon(glyph = RemMaterialSymbols.Shield, glyphFill = 0f, fill = ContainedIconFill.Subtle)
            }
        }
    }

    @Test
    fun remFaceMark() {
        shot("RemFaceMark-light") { RemTheme { faceMark() } }
        shot("RemFaceMark-dark") { RemTheme(darkTheme = true) { faceMark() } }
    }

    // The canonical Rem face mark — the same scalloped blob + eyes + smile as the SwiftUI
    // `RemFaceMark` (idle resting frame), brandBlue on the primary background.
    @Composable
    private fun faceMark() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RemColors.current.backgroundPrimary),
            contentAlignment = Alignment.Center,
        ) {
            RemFaceMark(mode = RemFaceMarkMode.Idle, tint = RemColors.current.brandBlue, size = 96.dp)
        }
    }

    // Sign-in renders its own centered screen (OnboardingSignInScreen) — NOT the sequencer scaffold's
    // bottom-pinned CTA bar — so the paired iOS⟷Android render diffs the same arrangement.
    @Test
    fun signInReturning() = shot("SignIn-returning-light") {
        RemTheme {
            OnboardingSignInScreen(
                state = SignInState.Returning(accountName = "Sam"),
                onContinue = {},
                onUseDifferentAccount = {},
            )
        }
    }

    @Test
    fun signInNew() = shot("SignIn-new-light") {
        RemTheme {
            OnboardingSignInScreen(state = SignInState.New, onContinue = {}, onUseDifferentAccount = {})
        }
    }

    @Test
    fun signInChecking() = shot("SignIn-checking-light") {
        RemTheme {
            OnboardingSignInScreen(state = SignInState.Checking, onContinue = {}, onUseDifferentAccount = {})
        }
    }

    @Test
    fun signInError() = shot("SignIn-error-dark") {
        RemTheme(darkTheme = true) {
            OnboardingSignInScreen(
                state = SignInState.Error("We couldn't sign you in. Check your connection and try again."),
                onContinue = {},
                onUseDifferentAccount = {},
            )
        }
    }

    @Test
    fun signInRecovery() = shot("SignIn-recovery-light") {
        RemTheme {
            OnboardingSignInScreen(
                state = SignInState.Recovery("Your session expired. Sign in again to pick up where you left off."),
                onContinue = {},
                onUseDifferentAccount = {},
            )
        }
    }

    @Test
    fun consent() {
        shot("Consent-default-light") { RemTheme { consentScreen() } }
        shot("Consent-default-dark") { RemTheme(darkTheme = true) { consentScreen() } }
        shot("Consent-loading-light") { RemTheme { consentScreen(state = ConsentState.Loading) } }
    }

    // Consent · error — notice above the bottom-pinned CTA, CTA flips to "Try again".
    @Test
    fun consentError() = shot("Consent-error-dark") {
        RemTheme(darkTheme = true) {
            consentScreen(
                state = ConsentState.Error(
                    "We couldn't save your choice. Check your connection and try again.",
                ),
            )
        }
    }

    // The two legal sheets the consent rows open (1:1 with LegalDocumentView), light.
    @Test
    fun consentTerms() = shot("Consent-terms-light") {
        RemTheme { LegalDocumentScreen(title = "Terms of Service", sections = previewTermsSections, onClose = {}) }
    }

    @Test
    fun consentPrivacy() = shot("Consent-privacy-light") {
        RemTheme { LegalDocumentScreen(title = "Privacy Policy", sections = previewPrivacySections, onClose = {}) }
    }

    @Composable
    private fun consentScreen(state: ConsentState = ConsentState.Idle) {
        OnboardingSequencer(
            steps = listOf(
                signInStep(state = SignInState.Returning("Sam"), onContinue = {}, onUseDifferentAccount = {}),
                consentStep(onAccept = {}, onOpenTerms = {}, onOpenPrivacy = {}, state = state),
            ),
            state = rememberOnboardingSequencerState(stepCount = 2, initialIndex = 1),
        )
    }
}
