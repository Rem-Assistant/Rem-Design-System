package com.rem.designsystem.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography

/**
 * Tap affordance with button semantics + an accessibility label. Used for the onboarding CTAs and the
 * back chevron so every tappable region announces itself (there is no Compose `Button` primitive in
 * this design-system module yet — flagged for extraction; until then onboarding composes tap targets
 * natively with correct semantics rather than forking a bespoke button treatment).
 */
internal fun Modifier.clickableRole(onClick: () -> Unit, label: String): Modifier =
    this
        .semantics {
            contentDescription = label
            role = Role.Button
        }
        .clickable(onClick = onClick)

/**
 * The sequencer progress indicator — one dot per step, the current step filled with the brand color,
 * the rest a subtle fill. This is an **Extend** addition (the reproduce-fidelity reference frames show
 * no progress affordance); kept intentionally quiet and token-bound so it never competes with a
 * step's reproduced content. Flagged for founder confirmation of style/placement.
 */
data class OnboardingProgress(
    /** Zero-based index of the current step. */
    val current: Int,
    /** Total number of steps in the sequence. */
    val total: Int,
)

/**
 * The shared onboarding **error/recovery notice** — `systemRed` at 12% on a `medium`-radius surface,
 * leading warning glyph + `caption1` message. One treatment reused by both sign-in (below the centered
 * actions) and the scaffold (directly above the bottom-pinned CTA), so the paired iOS⟷Android evidence
 * reads as the same card wherever it appears. The leading glyph resolves through the icon registry by
 * meaning + FILL: the Material Symbols `error` glyph at **FILL 1** (pairs with the iOS
 * `exclamationmark.triangle.fill`).
 */
@Composable
internal fun OnboardingNotice(message: String, modifier: Modifier = Modifier) {
    val colors = RemColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RemRadius.medium))
            .background(colors.systemRed.copy(alpha = 0.12f))
            .padding(RemSpacing.md),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        Text(
            text = RemMaterialSymbols.ErrorNotice.glyph,
            fontFamily = RemMaterialSymbols.family(RemMaterialSymbols.ErrorNotice),
            fontSize = 14.sp,
            color = colors.systemRed,
        )
        Text(text = message, style = RemTypography.caption1, color = colors.labelPrimary)
    }
}

@Composable
internal fun OnboardingProgressIndicator(progress: OnboardingProgress, modifier: Modifier = Modifier) {
    val colors = RemColors.current
    if (progress.total <= 1) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs),
    ) {
        for (i in 0 until progress.total) {
            val active = i == progress.current
            Box(
                modifier = Modifier
                    .size(if (active) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (active) colors.brandBlue else colors.fillTertiary),
            )
        }
    }
}
