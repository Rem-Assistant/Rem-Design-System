package com.rem.designsystem.primitives

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemTheme

/**
 * Compose form of `ContainedIcon` — the Android sibling of `Primitives/ContainedIcon.swift`.
 *
 * Cross-platform contract (SPEC): the **intent + tokens** are shared, the **form** is native.
 * Shared: a rounded, filled square holding a glyph; two fills (`Tint` solid + on-color glyph,
 * `Subtle` translucent + secondary glyph); two sizes (28 / 44). Divergent: the glyph is a Material
 * [ImageVector], not an SF Symbol string. Values come only from the generated `RemTokens.kt`
 * (same token contract as `DesignTokens.swift`) — this composable is thin, like `RemButtonStyle`.
 *
 * Figma canonical: ContainedIcon `110:54` / variant set `614:8` (Fill × Size).
 */
enum class ContainedIconSize { Small, Large }

sealed interface ContainedIconFill {
    /** Solid color square + on-color (white) glyph — hero / colored settings icons. */
    data class Tint(val color: Color) : ContainedIconFill

    /** Translucent `fillTertiary` square + `labelSecondary` glyph — inline row leading. */
    data object Subtle : ContainedIconFill
}

/** The flat set of resolved values for a `(fill, size)` — the Fluent `TokenSet` pattern. */
private data class ContainedIconTokens(
    val dimension: Dp,
    val cornerRadius: Dp,
    val glyphSize: Dp,
    val background: Color,
    val foreground: Color,
)

@Composable
private fun containedIconTokens(fill: ContainedIconFill, size: ContainedIconSize): ContainedIconTokens {
    val colors = RemColors.current
    val dimension = if (size == ContainedIconSize.Large) 44.dp else 28.dp
    val glyphSize = if (size == ContainedIconSize.Large) 22.dp else 14.dp
    return when (fill) {
        is ContainedIconFill.Tint -> ContainedIconTokens(
            dimension = dimension,
            cornerRadius = RemRadius.small,
            glyphSize = glyphSize,
            background = fill.color,
            foreground = Color.White, // on-color glyph, matches ContainedIcon.swift `.tint`
        )
        ContainedIconFill.Subtle -> ContainedIconTokens(
            dimension = dimension,
            cornerRadius = RemRadius.small,
            glyphSize = glyphSize,
            background = colors.fillTertiary,
            foreground = colors.labelSecondary,
        )
    }
}

@Composable
fun ContainedIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    fill: ContainedIconFill = ContainedIconFill.Subtle,
    size: ContainedIconSize = ContainedIconSize.Small,
    contentDescription: String? = null,
) {
    val tokens = containedIconTokens(fill, size)
    Box(
        modifier = modifier
            .size(tokens.dimension)
            .clip(RoundedCornerShape(tokens.cornerRadius))
            .background(tokens.background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tokens.foreground,
            modifier = Modifier.size(tokens.glyphSize),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContainedIconPreview() {
    RemTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            ContainedIcon(Icons.Filled.Lock, fill = ContainedIconFill.Tint(RemColors.current.brandBlue), size = ContainedIconSize.Large)
            ContainedIcon(Icons.Filled.Settings, fill = ContainedIconFill.Subtle)
        }
    }
}
