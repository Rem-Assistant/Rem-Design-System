package com.rem.designsystem.agentsurfaces

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rem.designsystem.tokens.Inter
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme

/**
 * **AgentStatusPill** — Compose sibling of the SwiftUI [AgentStatusPill]. The small glass status pill
 * shown under the agent avatar while the agent is working: a quiet capsule carrying a single [status]
 * string whose color is driven by one [AgentStatusTone] — [AgentStatusTone.Neutral] reads in
 * labelPrimary; [AgentStatusTone.Attention] reads in the adaptive brand accent to flag a state that
 * needs the user ("Needs you", "Needs approval"). Like `VoiceBar` it is the DS *display* form.
 *
 * Cross-platform contract (SPEC): intent + tokens are shared, form is native. Figma canonical:
 * **AgentStatusPill** `427:21` (variant set, `Tone=` Neutral / Attention).
 *
 * GLASS: the SwiftUI pill uses `.ultraThinMaterial` (true glass). Compose has no material blur, so this
 * render uses `backgroundSecondary` — the flat-grey substitution the other DS components use.
 */
enum class AgentStatusTone { Neutral, Attention }

@Composable
fun AgentStatusPill(
    status: String,
    modifier: Modifier = Modifier,
    tone: AgentStatusTone = AgentStatusTone.Neutral,
) {
    val colors = RemColors.current
    val labelColor = when (tone) {
        AgentStatusTone.Neutral -> colors.labelPrimary
        AgentStatusTone.Attention -> colors.brandBlueOnFill
    }
    Text(
        text = status,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = Inter,
        color = labelColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(colors.backgroundSecondary, CircleShape)
            .padding(horizontal = RemSpacing.md, vertical = 6.dp),
    )
}

@Preview(name = "AgentStatusPill — tones", showBackground = true)
@Composable
private fun AgentStatusPillPreview() {
    RemTheme {
        Column(
            modifier = Modifier.padding(40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AgentStatusPill("Working")
            AgentStatusPill("Generating PDF")
            AgentStatusPill("Reviewing guidance")
            AgentStatusPill("Needs approval", tone = AgentStatusTone.Attention)
            AgentStatusPill("Needs you", tone = AgentStatusTone.Attention)
        }
    }
}
