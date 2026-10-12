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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.icons.RemMaterialSymbol
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemTheme

/**
 * Compose `ContainedIcon` — the Android sibling of `Primitives/ContainedIcon.swift`. **Thin**: it
 * reads every value from [containedIconTokens] and renders. Cross-platform contract (SPEC): intent +
 * tokens are shared, form is native — the glyph is a Material [ImageVector], not an SF Symbol.
 * Figma canonical: ContainedIcon `110:54` / variant set `614:8` (Style × Size). Code Connect binding:
 * `code-connect/compose/ContainedIcon.figma.ts`. Subtle pairs `backgroundSecondary` with `labelPrimary`;
 * explicit tinted/brand/status fills retain their on-color foreground.
 */
@Composable
fun ContainedIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    fill: ContainedIconFill = ContainedIconFill.Subtle,
    size: ContainedIconSize = ContainedIconSize.Small,
    contentDescription: String? = null,
    glyphSize: Dp? = null,
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
            modifier = Modifier.size(glyphSize ?: tokens.glyphSize),
        )
    }
}

/**
 * Registry-safe glyph overload. The semantic symbol owns its verified codepoint and pinned FILL, so
 * a caller cannot pair a filled glyph with the outline font (or vice versa).
 */
@Composable
fun ContainedIcon(
    symbol: RemMaterialSymbol,
    modifier: Modifier = Modifier,
    fill: ContainedIconFill = ContainedIconFill.Subtle,
    size: ContainedIconSize = ContainedIconSize.Small,
    contentDescription: String? = null,
    glyphSize: Dp? = null,
) = ContainedIcon(
    glyph = symbol.glyph,
    glyphFill = symbol.fill,
    modifier = modifier,
    fill = fill,
    size = size,
    contentDescription = contentDescription,
    glyphSize = glyphSize,
)

/**
 * Glyph overload — the same token-driven tile, but the glyph is a **Material Symbols** font glyph
 * (from [RemMaterialSymbols]) rendered as a `Text` node at the given [glyphFill] (0 outline … 1 filled)
 * instead of a Material [ImageVector]. This is how the onboarding hero + legal-row leadings honour
 * `docs/contracts/icon-registry.md` (rule 2: no legacy `Icons.Filled.*`), so the FILL matches the iOS
 * SF Symbol per row and the visual-parity gate diffs clean. The tile chrome (dimension, radius,
 * background, foreground) is unchanged — it reads the same [containedIconTokens] as the vector overload.
 */
@Composable
fun ContainedIcon(
    glyph: String,
    glyphFill: Float,
    modifier: Modifier = Modifier,
    fill: ContainedIconFill = ContainedIconFill.Subtle,
    size: ContainedIconSize = ContainedIconSize.Small,
    contentDescription: String? = null,
    glyphSize: Dp? = null,
) {
    val tokens = containedIconTokens(fill, size)
    val glyphSizeSp = with(LocalDensity.current) { (glyphSize ?: tokens.glyphSize).toSp() }
    val desc = contentDescription
    Box(
        modifier = modifier
            .size(tokens.dimension)
            .clip(RoundedCornerShape(tokens.cornerRadius))
            .background(tokens.background)
            .then(
                if (desc != null) {
                    Modifier.semantics { this.contentDescription = desc }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            fontFamily = RemMaterialSymbols.family(fill = glyphFill),
            fontSize = glyphSizeSp,
            color = tokens.foreground,
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
