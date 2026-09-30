package com.rem.designsystem.primitives

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * **Pill** — Compose sibling of the SwiftUI [RemPill]. A subtle status / metadata chip: the same quiet
 * capsule (backgroundSecondary fill, caption1 label in labelSecondary, h.sm / v6 padding, capsule
 * radius), deliberately NOT a saturated tone fill. Kinds: [RemPillKind.Neutral] (plain), [RemPillKind.Dot]
 * (8dp color dot — calendar events), [RemPillKind.List] (list glyph — tasks). Authority: the Figma
 * **Pill** component set + `PillView` / the calendar+list badges in `TaskEventRowView.swift`.
 */
sealed interface RemPillKind {
    data object Neutral : RemPillKind
    data class Dot(val color: Color) : RemPillKind
    data object List : RemPillKind
}

@Composable
fun RemPill(
    text: String,
    modifier: Modifier = Modifier,
    kind: RemPillKind = RemPillKind.Neutral,
) {
    val colors = RemColors.current
    Row(
        modifier = modifier
            .background(colors.backgroundSecondary, CircleShape)
            .padding(horizontal = RemSpacing.sm, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs),
    ) {
        when (kind) {
            is RemPillKind.Neutral -> {}
            is RemPillKind.Dot -> Box(Modifier.size(8.dp).background(kind.color, CircleShape))
            is RemPillKind.List -> Icon(
                Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = colors.labelSecondary,
                modifier = Modifier.size(12.dp),
            )
        }
        Text(text = text, style = RemTypography.caption1, color = colors.labelSecondary)
    }
}

@Preview(name = "Pill", showBackground = true)
@Composable
private fun RemPillPreview() {
    RemTheme {
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RemPill("3 tasks", kind = RemPillKind.List)
            RemPill("Standup", kind = RemPillKind.Dot(RemColors.current.systemBlue))
            RemPill("Personal")
        }
    }
}
