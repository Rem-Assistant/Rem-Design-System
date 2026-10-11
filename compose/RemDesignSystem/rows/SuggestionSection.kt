package com.rem.designsystem.rows

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.buttons.RemButton
import com.rem.designsystem.buttons.RemButtonVariant
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
 *   3. **One header everywhere** — "Suggestions", sentence case (NOT uppercased), SectionHeader
 *      (`161:68`) metrics: body semibold, labelSecondary, 16dp horizontal inset, 6dp below.
 *
 * The DS component renders the list it is given, bounded; contextual ordering
 * (`SuggestionBriefRelevance`) is app logic the host applies before passing [suggestions].
 * Authority: the "Suggestions region" frame `2336:19714` — a SectionHeader, then **standalone**
 * AgendaSuggestionRows (`2336:19583`, 4dp apart, no Section/rows surface around them), then the
 * "See more · Plain" Button (`377:8`), i.e. the Text · Accent [RemButton], hugging its label
 * at the leading edge. This wrapper uses SuggestedTaskRow to forward TaskSuggestion into
 * AgendaSuggestionRow. It has no standalone Figma master; see
 * `code-connect/SuggestionSection.composition.json`.
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
        verticalArrangement = Arrangement.spacedBy(RemSpacing.xs),
    ) {
        Text(
            text = "Suggestions",
            style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.labelSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = RemSpacing.lg, end = RemSpacing.lg, bottom = 6.dp)
                .semantics { heading() },
        )

        inline.forEach { suggestion ->
            SuggestedTaskRow(
                suggestion = suggestion,
                onAccept = { onAccept(suggestion) },
                onDismiss = { onDismiss(suggestion) },
            )
        }

        if (overflow > 0 && onSeeMore != null) {
            // The canonical text button, hugging its label (intrinsic width caps the regular size's
            // full-width fill) so it sits at the leading edge as in `2336:19751`.
            RemButton(
                text = "See more",
                onClick = onSeeMore,
                variant = RemButtonVariant.TextAccent,
                modifier = Modifier.width(IntrinsicSize.Max),
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
