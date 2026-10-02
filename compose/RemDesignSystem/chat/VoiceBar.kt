package com.rem.designsystem.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rem.designsystem.tokens.Inter
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme

/**
 * **VoiceBar** (a.k.a. *MiniPlayerBar*) — Compose sibling of the SwiftUI [VoiceBar]. The bottom-anchored
 * voice bar shown in Chat while a voice session is live: one rounded bar (backgroundSecondary fill,
 * radius.large) that reads its whole layout from a single [VoiceBarState] — a leading control circle, a
 * centered status stack (eyebrow · title · subtitle/timer), and a trailing control. The same bar
 * transforms in place across states rather than stacking separate widgets.
 *
 * Cross-platform contract (SPEC): intent + tokens are shared, form is native — glyphs are Material
 * [Icons] here (Mic / MicOff / CallEnd / Stop), SF Symbols on iOS. Figma canonical: **VoiceBar**
 * `160:884` (variant set). Source lineage: `MiniPlayerBar.swift`.
 *
 * This is the DS *display* form — it renders a state, not the interactive session controls.
 */
enum class VoiceBarState {
    /** Session is starting but not yet live — collapses to thinking-dots, no controls. */
    Connecting,
    /** Live, capturing the user — neutral mic + red hang-up + running timer. */
    Listening,
    /** The assistant is talking back — same chrome as listening, `Speaking…` title. */
    Speaking,
    /** Mic is muted — red mic-off leading + red hang-up. */
    Muted,
    /** Reading an authored brief aloud — stop trailing instead of hang-up. */
    Reading,
    /** Idle and auto-closing — brand-accent draining top line + "Keep open" CTA. */
    Closing,
}

@Composable
fun VoiceBar(
    state: VoiceBarState,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    title: String? = null,
    subtitle: String? = null,
) {
    val colors = RemColors.current
    val appearance = voiceBarAppearance(state, colors.labelSecondary, colors.systemRed)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(RemRadius.large))
            .background(colors.backgroundSecondary),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 58.dp)
                .padding(horizontal = RemSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
        ) {
            // Leading control (mic / mic-off / dots / none)
            when {
                state == VoiceBarState.Connecting -> TypingDots(colors.labelSecondary)
                appearance.leadingIcon != null -> CircleControl(appearance.leadingIcon, appearance.leadingTint)
                else -> Spacer(Modifier.size(CircleDiameter))
            }

            Spacer(Modifier.width(RemSpacing.sm))

            // Centered status stack (hidden while connecting)
            if (state != VoiceBarState.Connecting) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = if (appearance.leadingAligned) Alignment.Start else Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = (eyebrow ?: appearance.eyebrow).uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Inter,
                        color = colors.labelSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = title ?: appearance.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Inter,
                        color = colors.labelPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle ?: appearance.subtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = Inter,
                        color = colors.labelSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.width(RemSpacing.sm))

            // Trailing control (hang-up / stop / Keep-open / none)
            when (appearance.trailing) {
                Trailing.HangUp -> CircleControl(Icons.Filled.CallEnd, colors.systemRed)
                Trailing.Stop -> CircleControl(Icons.Filled.Stop, colors.systemRed)
                Trailing.KeepOpen -> Text(
                    text = "Keep open",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Inter,
                    color = Color.White,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.brandBlue)
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                        .wrapContentWidth(),
                )
                Trailing.None -> Spacer(Modifier.size(CircleDiameter))
            }
        }

        // The auto-close countdown: brand-accent line draining along the top edge (shown ~55% here).
        if (appearance.showsClosingLine) {
            Row(modifier = Modifier.fillMaxWidth().align(Alignment.TopStart)) {
                Box(Modifier.weight(0.55f).height(3.dp).background(colors.brandBlue))
                Box(Modifier.weight(0.45f).height(3.dp).background(colors.brandBlue.copy(alpha = 0.18f)))
            }
        }
    }
}

private val CircleDiameter = 32.dp

@Composable
private fun CircleControl(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(CircleDiameter)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun TypingDots(color: Color) {
    Row(
        modifier = Modifier.size(CircleDiameter),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        }
    }
}

private enum class Trailing { HangUp, Stop, KeepOpen, None }

private data class VoiceBarAppearance(
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val leadingIcon: androidx.compose.ui.graphics.vector.ImageVector?,
    val leadingTint: Color,
    val trailing: Trailing,
    val leadingAligned: Boolean,
    val showsClosingLine: Boolean,
)

private fun voiceBarAppearance(
    state: VoiceBarState,
    neutralTint: Color,
    redTint: Color,
): VoiceBarAppearance = when (state) {
    VoiceBarState.Connecting -> VoiceBarAppearance(
        "", "", "", null, Color.Unspecified, Trailing.None, leadingAligned = false, showsClosingLine = false,
    )
    VoiceBarState.Listening -> VoiceBarAppearance(
        "Voice chat", "Listening…", "0:03", Icons.Filled.Mic, neutralTint, Trailing.HangUp,
        leadingAligned = false, showsClosingLine = false,
    )
    VoiceBarState.Speaking -> VoiceBarAppearance(
        "Voice chat", "Speaking…", "0:12", Icons.Filled.Mic, neutralTint, Trailing.HangUp,
        leadingAligned = false, showsClosingLine = false,
    )
    VoiceBarState.Muted -> VoiceBarAppearance(
        "Voice chat", "Muted", "0:24", Icons.Filled.MicOff, redTint, Trailing.HangUp,
        leadingAligned = false, showsClosingLine = false,
    )
    VoiceBarState.Reading -> VoiceBarAppearance(
        "Latest brief", "Reading latest brief", "Continue listening, then reply",
        Icons.Filled.Mic, neutralTint, Trailing.Stop, leadingAligned = false, showsClosingLine = false,
    )
    VoiceBarState.Closing -> VoiceBarAppearance(
        "Voice chat", "Closing voice chat", "Paused — no recent activity",
        null, Color.Unspecified, Trailing.KeepOpen, leadingAligned = true, showsClosingLine = true,
    )
}

@Preview(name = "VoiceBar — all states", showBackground = true, widthDp = 418)
@Composable
private fun VoiceBarPreview() {
    RemTheme {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VoiceBarState.values().forEach { state ->
                VoiceBar(state)
            }
        }
    }
}
