package com.rem.designsystem.agentsurfaces

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * **RunningTaskBanner** — Compose sibling of the SwiftUI [RunningTaskBanner]. The glass
 * "Live-Activity"-style banner shown while an agent task is running (e.g. *Browser · Signing in to
 * my.dnb.com*): one capsule laid out as a leading thumbnail, a title + status stack, and a trailing
 * **Stop** control. A single [RunningTaskTone] drives the status color — [RunningTaskTone.Working]
 * reads the status in labelSecondary; [RunningTaskTone.Attention] reads it in the brand accent to flag
 * a task that needs the user ("Needs you · Password rejected"). Like `VoiceBar` it is the DS *display*
 * form — affordances are shown, not wired.
 *
 * Cross-platform contract (SPEC): intent + tokens are shared, form is native. Figma canonical:
 * **RunningTaskBanner** `432:39` (variant set, `Tone=` Working / Attention).
 *
 * GLASS: the SwiftUI banner uses `.ultraThinMaterial` (true glass). Compose has no material blur, so
 * this render uses `backgroundSecondary` — the flat-grey substitution the other DS components use.
 */
enum class RunningTaskTone { Working, Attention }

@Composable
fun RunningTaskBanner(
    task: String,
    status: String,
    modifier: Modifier = Modifier,
    tone: RunningTaskTone = RunningTaskTone.Working,
) {
    val colors = RemColors.current
    val statusColor = when (tone) {
        RunningTaskTone.Working -> colors.labelSecondary
        RunningTaskTone.Attention -> colors.brandBlueOnFill
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.backgroundSecondary)
            .defaultMinSize(minHeight = 56.dp)
            .padding(RemSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        // Leading thumbnail placeholder (the live banner slots the task's preview here).
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(RemRadius.medium))
                .background(colors.fillTertiary),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = task,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = Inter,
                color = colors.labelPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = status,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = Inter,
                color = statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(RemSpacing.sm))

        // Trailing Stop control (display form — shown, not wired).
        Text(
            text = "Stop",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = Inter,
            color = colors.labelPrimary,
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.fillTertiary)
                .padding(horizontal = RemSpacing.lg, vertical = RemSpacing.sm)
                .wrapContentWidth(),
        )
    }
}

@Preview(name = "RunningTaskBanner — tones", showBackground = true, widthDp = 418)
@Composable
private fun RunningTaskBannerPreview() {
    RemTheme {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RunningTaskBanner(task = "Browser", status = "Signing in to my.dnb.com")
            RunningTaskBanner(
                task = "Browser",
                status = "Needs you · Password rejected",
                tone = RunningTaskTone.Attention,
            )
        }
    }
}
