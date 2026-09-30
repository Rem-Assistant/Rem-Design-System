package com.rem.designsystem.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.primitives.ContainedIconSize
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * The **Voice** onboarding step — Compose sibling of the SwiftUI `OnboardingVoiceTemplate`. Onboarding
 * treatment: a hero tile at top → hear/picker rows → Character & speed sliders on a white flip
 * background → bottom **Continue** CTA, consistent with the other onboarding steps. Authority:
 * `tasks/refs/onboarding/05-voice.png` (flipped to white bg + grey content per the flow-consistency rule).
 */
@Composable
fun OnboardingVoiceScreen(
    voiceName: String,
    selectedVoice: String,
    onHearVoice: () -> Unit,
    onSelectVoice: () -> Unit,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    consistency: Float,
    onConsistencyChange: (Float) -> Unit,
    likeness: Float,
    onLikenessChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    heroIcon: ImageVector = Icons.Filled.PlayArrow,
    onContinue: () -> Unit = {},
) {
    val colors = RemColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.backgroundPrimary)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(RemSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
        ) {
            Spacer(Modifier.height(RemSpacing.lg))
            ContainedIcon(icon = heroIcon, fill = ContainedIconFill.Tint(colors.brandBlue), size = ContainedIconSize.Large)

            RemSection(modifier = Modifier.fillMaxWidth()) {
                IconTextRow(Icons.Filled.PlayArrow, colors.brandBlue, "Hear this voice", onHearVoice, subtitle = voiceName)
            }
            RemSection(
                modifier = Modifier.fillMaxWidth(),
                header = "Spoken responses",
                footer = "Choose how Rem sounds when reading a response or talking with you.",
            ) {
                IconTextRow(Icons.Filled.Settings, colors.brandBlue, "Voice", onSelectVoice, trailingValue = selectedVoice)
            }
            RemSection(
                modifier = Modifier.fillMaxWidth(),
                header = "Character & speed",
                footer = "Speed applies to the next thing Rem says. Consistency trades expressive range for " +
                    "a steadier delivery, and likeness controls how closely Rem holds to the chosen voice.",
            ) {
                SliderRow("Speed", "Slower", "Faster", speed, onSpeedChange)
                RowSeparator()
                SliderRow("Consistency", "More expressive", "More consistent", consistency, onConsistencyChange)
                RowSeparator()
                SliderRow("Likeness", "Looser", "Closer", likeness, onLikenessChange)
            }
        }
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().padding(RemSpacing.lg),
            // Match the iOS primary action button radius (RemButtonStyle.primary = `medium`), not
            // Material's default pill.
            shape = RoundedCornerShape(RemRadius.medium),
            colors = ButtonDefaults.buttonColors(containerColor = colors.buttonBackground, contentColor = colors.backgroundPrimary),
        ) { Text("Continue", style = RemTypography.bodyBold) }
    }
}

@Composable
private fun IconTextRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailingValue: String? = null,
) {
    val colors = RemColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableRole(onClick = onClick, label = title)
            .padding(horizontal = RemSpacing.md, vertical = RemSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContainedIcon(icon = icon, fill = ContainedIconFill.Tint(tint), size = ContainedIconSize.Small)
        Spacer(Modifier.width(RemSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = RemTypography.bodyBold, color = colors.labelPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(text = subtitle, style = RemTypography.caption1, color = colors.labelSecondary)
            }
        }
        if (trailingValue != null) {
            Text(text = trailingValue, style = RemTypography.caption1, color = colors.labelSecondary)
            Spacer(Modifier.width(RemSpacing.sm))
        }
        Text(
            text = RemMaterialSymbols.DisclosureChevron.glyph,
            fontFamily = RemMaterialSymbols.family(RemMaterialSymbols.DisclosureChevron),
            fontSize = 20.sp,
            color = colors.labelTertiary,
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    minLabel: String,
    maxLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    val colors = RemColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RemSpacing.md, vertical = RemSpacing.sm),
    ) {
        Text(text = label, style = RemTypography.bodyBold, color = colors.labelPrimary)
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(thumbColor = colors.brandBlue, activeTrackColor = colors.brandBlue),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = minLabel, style = RemTypography.caption1, color = colors.labelSecondary)
            Spacer(Modifier.weight(1f))
            Text(text = maxLabel, style = RemTypography.caption1, color = colors.labelSecondary)
        }
    }
}

@Composable
private fun RowSeparator() {
    val colors = RemColors.current
    Box(modifier = Modifier.padding(horizontal = RemSpacing.md).fillMaxWidth().height(1.dp).background(colors.separator))
}

@Preview(name = "Voice · light", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun VoiceLightPreview() {
    RemTheme {
        var speed by remember { mutableFloatStateOf(0.45f) }
        var consistency by remember { mutableFloatStateOf(0.7f) }
        var likeness by remember { mutableFloatStateOf(0.6f) }
        OnboardingVoiceScreen(
            voiceName = "Aria",
            selectedVoice = "Aria (Warm)",
            onHearVoice = {}, onSelectVoice = {},
            speed = speed, onSpeedChange = { speed = it },
            consistency = consistency, onConsistencyChange = { consistency = it },
            likeness = likeness, onLikenessChange = { likeness = it },
            onContinue = {},
        )
    }
}
