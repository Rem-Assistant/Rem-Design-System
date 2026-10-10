package com.rem.designsystem.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * The flat set of button variants — 1:1 with the Figma `Style` property on **Button** (`377:8`) and
 * with the SwiftUI `RemButtonVariant` (`Sources/RemDesignSystem/Buttons/RemButtonTokenSet.swift`).
 */
enum class RemButtonVariant(val figmaStyleName: String) {
    /** Filled, adaptive ink — login / consent CTA. */
    RectBlack("Rect · Black"),
    /** Filled, brand blue. */
    RectBlue("Rect · Blue"),
    /** Subtle filled. */
    RectSecondary("Rect · Secondary"),
    /** Filled red. */
    RectDestructive("Rect · Destructive"),
    /** Tinted label only, accent. */
    TextAccent("Text · Accent"),
    /** Tinted label only, red. */
    TextDestructive("Text · Destructive"),
    /** Translucent capsule — Connect / Install. */
    PillSecondary("Pill · Secondary"),
}

enum class RemButtonSize { Regular, Compact }

/**
 * **RemButton** — Compose sibling of the SwiftUI `RemButtonStyle`. Every value resolves from
 * [remButtonTokens], which mirrors the SwiftUI `RemButtonTokenSet` value for value: rect variants
 * fill the width on a medium-radius surface, text variants are label-only, and the pill is a
 * fill-tertiary capsule. Disabled uses the semantic disabled colors; press dims the whole button.
 * [trailing] fills the Figma Trailing slot (e.g. a chevron) and inherits the label colour.
 */
@Composable
fun RemButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: RemButtonVariant = RemButtonVariant.RectBlack,
    size: RemButtonSize = RemButtonSize.Regular,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val tokens = remButtonTokens(variant, size)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .then(if (tokens.fillsWidth) Modifier.fillMaxWidth() else Modifier)
            .alpha(if (pressed) tokens.pressedOpacity else 1f)
            .clip(tokens.shape)
            .background(if (enabled) tokens.background else tokens.backgroundDisabled)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = tokens.horizontalPadding, vertical = tokens.verticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        val foreground = if (enabled) tokens.foreground else tokens.foregroundDisabled
        if (trailing == null) {
            // Centered so a label that wraps (e.g. a narrow horizontal ButtonGroup) stays centered.
            Text(text = text, style = tokens.font, color = foreground, textAlign = TextAlign.Center)
        } else {
            // Figma Button `377:8` Trailing slot (e.g. the canonical Chevron), tinted like the label.
            Row(horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Text(text = text, style = tokens.font, color = foreground)
                CompositionLocalProvider(LocalContentColor provides foreground) { trailing() }
            }
        }
    }
}

/** Resolved values for one `(variant, size)` — the Compose twin of SwiftUI `RemButtonTokenSet`. */
internal data class RemButtonTokens(
    val shape: Shape,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val fillsWidth: Boolean,
    val font: TextStyle,
    val background: Color,
    val backgroundDisabled: Color,
    val foreground: Color,
    val foregroundDisabled: Color,
    val pressedOpacity: Float,
)

@Composable
internal fun remButtonTokens(variant: RemButtonVariant, size: RemButtonSize): RemButtonTokens {
    val colors = RemColors.current
    val semibold = { style: TextStyle -> style.copy(fontWeight = FontWeight.SemiBold) }
    return when (variant) {
        RemButtonVariant.RectBlack, RemButtonVariant.RectBlue, RemButtonVariant.RectSecondary, RemButtonVariant.RectDestructive -> {
            val (background, foreground) = when (variant) {
                RemButtonVariant.RectBlue -> colors.brandBlue to colors.labelOnColor
                RemButtonVariant.RectSecondary -> colors.fillTertiary to colors.labelPrimary
                RemButtonVariant.RectDestructive -> colors.systemRed to colors.labelOnColor
                else -> colors.buttonBackground to colors.backgroundPrimary
            }
            RemButtonTokens(
                shape = RoundedCornerShape(RemRadius.medium),
                horizontalPadding = RemSpacing.md,
                verticalPadding = RemSpacing.md,
                fillsWidth = true,
                font = RemTypography.bodyBold,
                background = background,
                backgroundDisabled = colors.fillTertiary,
                foreground = foreground,
                foregroundDisabled = colors.labelTertiary,
                pressedOpacity = 0.72f,
            )
        }
        RemButtonVariant.TextAccent, RemButtonVariant.TextDestructive -> RemButtonTokens(
            shape = RectangleShape,
            horizontalPadding = 2.dp,
            verticalPadding = if (size == RemButtonSize.Regular) 6.dp else 4.dp,
            fillsWidth = size == RemButtonSize.Regular,
            font = semibold(if (size == RemButtonSize.Regular) RemTypography.body else RemTypography.subheadline),
            background = Color.Transparent,
            backgroundDisabled = Color.Transparent,
            foreground = if (variant == RemButtonVariant.TextDestructive) colors.systemRed else colors.brandBlue,
            foregroundDisabled = colors.labelSecondary,
            pressedOpacity = 0.55f,
        )
        RemButtonVariant.PillSecondary -> RemButtonTokens(
            shape = CircleShape,
            horizontalPadding = 16.dp,
            verticalPadding = 5.dp,
            fillsWidth = false,
            font = semibold(RemTypography.subheadline),
            background = colors.fillTertiary,
            backgroundDisabled = colors.fillTertiary,
            foreground = colors.brandBlueOnFill,
            foregroundDisabled = colors.labelSecondary,
            pressedOpacity = 0.7f,
        )
    }
}

@Preview(name = "RemButton — all variants", showBackground = true, widthDp = 300)
@Composable
private fun RemButtonPreview() {
    RemTheme {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RemButtonVariant.entries.forEach { RemButton(it.figmaStyleName, onClick = {}, variant = it) }
            RemButton("Disabled", onClick = {}, enabled = false)
        }
    }
}
