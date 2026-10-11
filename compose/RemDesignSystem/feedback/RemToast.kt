package com.rem.designsystem.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography
import kotlinx.coroutines.delay

/** Semantic tone for [RemToast]. It owns the icon and tint; callers provide only the message. */
enum class RemToastVariant { Info, Success, Warning, Error }

/**
 * A brief, non-actionable status message. It enters the accessibility polite live region and
 * dismisses itself after [durationMillis] (four seconds by default) without taking focus.
 *
 * Use a toast for transient feedback. Use a persistent contextual message when the user must act.
 * Figma: `Toast` (`72:24`), properties `Variant` and `Message`.
 */
@Composable
fun RemToast(
    message: String,
    modifier: Modifier = Modifier,
    variant: RemToastVariant = RemToastVariant.Info,
    durationMillis: Long = 4_000L,
    onDismiss: () -> Unit = {},
) {
    var visible by remember(message, variant) { mutableStateOf(true) }
    LaunchedEffect(message, variant, durationMillis) {
        delay(durationMillis)
        visible = false
        onDismiss()
    }
    if (!visible) return

    val colors = RemColors.current
    val (icon, tint) = when (variant) {
        RemToastVariant.Info -> Icons.Filled.Info to colors.systemBlue
        RemToastVariant.Success -> Icons.Filled.CheckCircle to colors.systemGreen
        RemToastVariant.Warning -> Icons.Filled.Warning to colors.systemOrange
        RemToastVariant.Error -> Icons.Filled.Cancel to colors.systemRed
    }
    ToastCapsule(message = message, icon = icon, tint = tint, modifier = modifier)
}

@Composable
private fun ToastCapsule(message: String, icon: ImageVector, tint: Color, modifier: Modifier) {
    val colors = RemColors.current
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.pillBackground)
            .padding(horizontal = RemSpacing.md, vertical = RemSpacing.sm)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text = message, style = RemTypography.footnote, color = colors.labelPrimary)
    }
}
