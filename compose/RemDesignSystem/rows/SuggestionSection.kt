package com.rem.designsystem.rows

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * **SuggestionSection** — Compose sibling of the SwiftUI `SharedSuggestionSection`. The one suggestion
 * surface used everywhere suggestions appear. Owns three behaviours so no call site can drift:
 *   1. **Bounded inline set** — only [inlineLimit] rows render in place (an unbounded list stops reading
 *      as "next steps" and starts reading as a backlog).
 *   2. **Overflow behind "See more"** — the remainder is one tap away via [onSeeMore]; nothing dropped.
 *   3. **One header everywhere** — "Suggestions", sentence case (NOT uppercased — footnote semibold,
 *      labelSecondary).
 *
 * The DS component renders the list it is given, bounded; contextual ordering
 * (`SuggestionBriefRelevance`) is app logic the host applies before passing [suggestions].
 * Token-only values. Authority: `SharedSuggestionSection.swift`.
 */
@Composable
fun SuggestionSection(
    suggestions: List<TaskSuggestion>,
    onAccept: (TaskSuggestion) -> Unit,
    onDismiss: (TaskSuggestion) -> Unit,
    modifier: Modifier = Modifier,
    inlineLimit: Int = 3,
    onSeeMore: (() -> Unit)? = null,
) {
    if (suggestions.isEmpty()) return
    val colors = RemColors.current
    val inline = suggestions.take(inlineLimit.coerceAtLeast(0))
    val overflow = suggestions.size - inline.size

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        Text(
            text = "Suggestions",
            style = RemTypography.footnote.copy(fontWeight = FontWeight.Bold),
            color = colors.labelSecondary,
            modifier = Modifier.fillMaxWidth(),
        )

        inline.forEach { suggestion ->
            SuggestedTaskRow(
                suggestion = suggestion,
                onAccept = { onAccept(suggestion) },
                onDismiss = { onDismiss(suggestion) },
            )
        }

        if (overflow > 0 && onSeeMore != null) {
            Text(
                text = "See more",
                style = RemTypography.footnote.copy(fontWeight = FontWeight.Bold),
                color = colors.brandBlueOnFill,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 32.dp)
                    .clickable(onClick = onSeeMore)
                    .padding(vertical = RemSpacing.xs),
            )
        }
    }
}

@Preview(name = "SuggestionSection", showBackground = true, widthDp = 402)
@Composable
private fun SuggestionSectionPreview() {
    RemTheme {
        SuggestionSection(
            suggestions = listOf(
                TaskSuggestion("Set up the TestFlight pipeline using ASC CLI", "Samuel · Granola · 8h ago"),
                TaskSuggestion("Pull the Claude/DSFlows branch and confirm the Compose UI renders", "Damilola · Granola · 8h ago"),
                TaskSuggestion("Continue applying to other programs", "Larissa · Gmail · 1d ago"),
                TaskSuggestion("Book the standup room", "Overflow item", accept = SuggestionAccept.Move),
            ),
            onAccept = {},
            onDismiss = {},
            onSeeMore = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
