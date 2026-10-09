package com.rem.designsystem.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.primitives.ContainedIconSize
import com.rem.designsystem.screens.VoiceChooserContent
import com.rem.designsystem.screens.VoiceControlsContent
import com.rem.designsystem.screens.VoiceSettingsFixture
import com.rem.designsystem.screens.VoiceSettingsFixtureSaver
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * The **Voice** onboarding step — Compose sibling of the SwiftUI `OnboardingVoiceTemplate`. Source:
 * Figma `Screen/Voice · Onboarding` (`2219:22520`) and `Screen/Voice · Choose a voice`
 * (`2221:83686`) on the Onboarding New page `2213:9168`.
 *
 * Reuses the shared Settings cores `VoiceControlsContent` (`2217:1571`, `showConversationEntry =
 * false`) and `VoiceChooserContent` (`2217:1901`); onboarding owns only the centered lockup
 * (`773:22`) and the fixed Continue + Skip action area (`773:28`). Preview is the local no-audio
 * fixture state; Continue / Skip are host callbacks and `onBack` exits to the playground host.
 * Tapping Voice opens the chooser; native Back returns to the shell with the selection and all
 * slider values retained.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingVoiceScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RemColors.current
    var fixture by rememberSaveable(stateSaver = VoiceSettingsFixtureSaver) { mutableStateOf(VoiceSettingsFixture()) }
    var chooser by rememberSaveable { mutableStateOf(false) }
    val controlsScroll = rememberScrollState()
    val chooserScroll = rememberScrollState()
    val leave = {
        fixture = fixture.stopPreview()
        if (chooser) chooser = false else onBack()
    }
    BackHandler(onBack = leave)
    Scaffold(
        modifier = modifier,
        containerColor = colors.backgroundPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = { if (chooser) Text("Choose a voice", style = RemTypography.bodyBold) },
                navigationIcon = {
                    IconButton(onClick = leave, modifier = Modifier.testTag("back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.backgroundPrimary),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(if (chooser) chooserScroll else controlsScroll)
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 24.dp)
                    .testTag(if (chooser) "onboardingVoiceChooser" else "onboardingVoice"),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                if (chooser) {
                    VoiceChooserContent(
                        fixture,
                        onSelect = { fixture = fixture.select(it) },
                        onPreview = { fixture = fixture.togglePreview(it) },
                    )
                } else {
                    Lockup(colors)
                    VoiceControlsContent(
                        fixture,
                        onFixtureChange = { fixture = it },
                        showConversationEntry = false,
                        onPreview = { fixture = fixture.togglePreview(it) },
                        onChooseVoice = { fixture = fixture.stopPreview(); chooser = true },
                    )
                }
            }
            if (!chooser) {
                ActionArea(colors, onContinue = onContinue, onSkip = onSkip)
            }
        }
    }
}

/** Authored lockup copy (`2219:22520` · `773:20` / `773:21`). */
private const val LockupTitle = "Choose how Rem sounds"
private const val LockupSubtitle = "Preview a voice, choose the one Rem uses, then fine-tune its delivery."

@Composable
private fun Lockup(colors: com.rem.designsystem.tokens.RemColorScheme) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        ContainedIcon(
            RemMaterialSymbols.Voice,
            fill = ContainedIconFill.Tint(colors.brandBlue),
            size = ContainedIconSize.Large,
        )
        Text(
            text = LockupTitle,
            style = RemTypography.largeTitle.copy(fontWeight = FontWeight.SemiBold),
            color = colors.labelPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = LockupSubtitle,
            style = RemTypography.body,
            color = colors.labelSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ActionArea(
    colors: com.rem.designsystem.tokens.RemColorScheme,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(RemSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RemSpacing.xs),
    ) {
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().testTag("onboardingVoice.continue"),
            // Match the iOS primary action button (Rect · Black), not Material's default pill.
            shape = RoundedCornerShape(RemRadius.medium),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.buttonBackground,
                contentColor = colors.backgroundPrimary,
            ),
        ) { Text("Continue", style = RemTypography.bodyBold) }
        TextButton(onClick = onSkip, modifier = Modifier.testTag("onboardingVoice.skip")) {
            Text("Skip", style = RemTypography.bodyBold, color = colors.brandBlue)
        }
    }
}

@Preview(name = "OnboardingVoice · light", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun OnboardingVoiceLightPreview() {
    RemTheme {
        OnboardingVoiceScreen(onBack = {}, onContinue = {}, onSkip = {})
    }
}
