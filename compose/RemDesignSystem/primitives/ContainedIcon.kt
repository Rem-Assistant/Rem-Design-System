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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemTheme

/**
 * Compose `ContainedIcon` — the Android sibling of `Primitives/ContainedIcon.swift`. **Thin**: it
 * reads every value from [containedIconTokens] and renders. Cross-platform contract (SPEC): intent +
 * tokens are shared, form is native — the glyph is a Material [ImageVector], not an SF Symbol.
 * Figma canonical: ContainedIcon `110:54` / variant set `614:8` (Fill × Size). Code Connect binding:
 * `ContainedIcon.figma.kt`.
 */
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
