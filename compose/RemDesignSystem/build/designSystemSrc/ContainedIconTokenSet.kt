package com.rem.designsystem.primitives

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius

/**
 * Size axis of `ContainedIcon` — 1:1 with the Figma **Size** property (ContainedIcon set `614:8`).
 * Public top-level enum (not nested in the internal token set) so it can appear in `ContainedIcon`'s
 * public API — the Kotlin twin of Swift `ContainedIconSize`.
 */
enum class ContainedIconSize { Small, Large }

/** Fill axis — 1:1 with the Figma **Fill** property: `Tinted` (solid + on-color glyph) / `Subtle`. */
sealed interface ContainedIconFill {
    data class Tint(val color: Color) : ContainedIconFill
    data object Subtle : ContainedIconFill
}

/**
 * Resolves every styleable value for a `(fill, size)` into one value type — the **TokenSet** pattern,
 * the Kotlin twin of `ContainedIconTokenSet.swift`. Every value comes from the generated `RemTokens`
 * (one documented exception below), so the `ContainedIcon` composable stays thin and no literals live
 * in the view.
 *
 * There is deliberately **no `ContainedIconStyle`**: unlike `RemButton`, a `ContainedIcon` is a
 * decorative view, not a control — no rest/pressed/disabled. The design-system parallel for a plain
 * view is *TokenSet + View*; for a control it is *TokenSet + Style*.
 */
internal data class ContainedIconTokens(
    val dimension: Dp,
    val cornerRadius: Dp,
    val glyphSize: Dp,
    val background: Color,
    val foreground: Color,
)

@Composable
internal fun containedIconTokens(fill: ContainedIconFill, size: ContainedIconSize): ContainedIconTokens {
    val colors = RemColors.current
    val dimension: Dp = if (size == ContainedIconSize.Large) 64.dp else 38.dp
    // 18dp is the 64dp hero squircle radius the founder approved — intentionally OFF the
    // 8/12/16/24 scale, flagged for token reconciliation, centralized here (mirrors the Swift note).
    val cornerRadius: Dp = if (size == ContainedIconSize.Large) 18.dp else RemRadius.small
    val glyphSize: Dp = dimension * 0.46f
    return when (fill) {
        is ContainedIconFill.Tint -> ContainedIconTokens(
            dimension = dimension,
            cornerRadius = cornerRadius,
            glyphSize = glyphSize,
            background = fill.color,
            // on-color glyph. Swift uses a `labelOnColor` token; it isn't on main's tokens.json yet,
            // so white here — reconcile by adding `color.label.onColor` to tokens.json + the generator.
            foreground = Color.White,
        )
        ContainedIconFill.Subtle -> ContainedIconTokens(
            dimension = dimension,
            cornerRadius = cornerRadius,
            glyphSize = glyphSize,
            background = colors.fillTertiary,
            foreground = colors.labelSecondary,
        )
    }
}
