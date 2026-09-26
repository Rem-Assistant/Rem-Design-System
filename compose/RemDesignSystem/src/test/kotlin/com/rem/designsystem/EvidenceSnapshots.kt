package com.rem.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.rem.designsystem.brand.RemFaceMark
import com.rem.designsystem.brand.RemFaceMarkMode
import com.rem.designsystem.onboarding.OnboardingSequencer
import com.rem.designsystem.onboarding.SignInState
import com.rem.designsystem.onboarding.consentStep
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

    @Composable
    private fun iconRow() {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            ContainedIcon(
                Icons.Filled.Lock,
                fill = ContainedIconFill.Tint(RemColors.current.brandBlue),
                size = ContainedIconSize.Large,
            )
            ContainedIcon(Icons.Filled.Settings, fill = ContainedIconFill.Subtle)
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

    @Test
    fun signInReturning() = shot("SignIn-returning-light") {
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

    @Test
    fun signInNew() = shot("SignIn-new-light") {
        RemTheme {
            OnboardingSequencer(
                steps = listOf(
                    signInStep(state = SignInState.New, onContinue = {}, onUseDifferentAccount = {}),
                ),
            )
        }
    }

    @Test
    fun signInError() = shot("SignIn-error-dark") {
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

    @Test
    fun consent() {
        shot("Consent-light") { RemTheme { consentScreen() } }
        shot("Consent-dark") { RemTheme(darkTheme = true) { consentScreen() } }
    }

    @Composable
    private fun consentScreen() {
        OnboardingSequencer(
            steps = listOf(
                signInStep(state = SignInState.Returning("Sam"), onContinue = {}, onUseDifferentAccount = {}),
                consentStep(onAccept = {}, onOpenTerms = {}, onOpenPrivacy = {}),
            ),
            state = rememberOnboardingSequencerState(stepCount = 2, initialIndex = 1),
        )
    }
}
