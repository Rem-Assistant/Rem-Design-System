package com.rem.designsystem.demo

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing

/**
 * True when the system has animations turned off (Settings ▸ Accessibility ▸ Remove animations, or
 * an animator duration scale of 0). Skeletons then stay static instead of sweeping.
 */
@Composable
internal fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Sibling of the iOS playground `ShimmerModifier` (ported from the shipping app): a moving highlight across skeleton placeholders,
 * static when [rememberReduceMotion] is true.
 */
internal fun Modifier.skeletonShimmer(): Modifier = composed {
    if (rememberReduceMotion()) return@composed this
    val transition = rememberInfiniteTransition(label = "remShimmer")
    val phase by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "remShimmerPhase",
    )
    drawWithContent {
        drawContent()
        val x = phase * size.width
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.3f), Color.Transparent),
                startX = x,
                endX = x + size.width,
            ),
            topLeft = Offset.Zero,
            size = size,
        )
    }
}

/** One rounded placeholder bar in the shared skeleton fill. */
@Composable
internal fun SkeletonBlock(modifier: Modifier = Modifier, width: Dp? = null, height: Dp = 14.dp, cornerRadius: Dp = 4.dp) {
    val sized = if (width != null) modifier.width(width) else modifier.fillMaxWidth()
    Box(
        sized
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(RemColors.current.labelSecondary.copy(alpha = 0.2f)),
    )
}

/**
 * Sibling of the iOS playground `SkeletonList`: grouped-list placeholder rows. Collapsed into one polite
 * live region carrying [label], so TalkBack announces the loading state once.
 */
@Composable
internal fun SkeletonList(label: String, modifier: Modifier = Modifier, rows: Int = 4) {
    val colors = RemColors.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RemRadius.medium))
            .background(colors.backgroundSecondary)
            .skeletonShimmer()
            .clearAndSetSemantics {
                contentDescription = label
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        repeat(rows) { index ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = RemSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
            ) {
                SkeletonBlock(width = 30.dp, height = 30.dp, cornerRadius = 7.dp)
                SkeletonBlock(width = if (index % 2 == 0) 160.dp else 120.dp)
                Spacer(Modifier.size(0.dp))
            }
            if (index < rows - 1) {
                HorizontalDivider(Modifier.padding(start = RemSpacing.lg + 30.dp + RemSpacing.md), color = colors.separator)
            }
        }
    }
}
