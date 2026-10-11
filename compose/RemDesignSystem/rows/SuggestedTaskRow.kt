package com.rem.designsystem.rows

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme

/**
 * **SuggestedTaskRow** — a `TaskSuggestion`-based convenience wrapper over the canonical
 * [AgendaSuggestionRow]. It keeps the existing call sites (`SuggestionSection`,
 * `AgendaSuggestionsPlayground`) unchanged while the canonical row owns the rendering and the
 * cross-platform Code Connect mapping. A proposed task rendered to **mirror `TaskEventRow`** with
 * three deliberate substitutions:
 *   • the left **time slot becomes an accept CTA** (Add / Move) — a suggestion's whole point is its action;
 *   • the trailing **chevron becomes a dismiss ✕** — this is not a drill-down;
 *   • a **dashed ring** frames the row, signalling "proposed, not yet real".
 *
 * Every row states WHY ([TaskSuggestion.subtitle]) — an unattributed suggestion is indistinguishable
 * from the app inventing work. Token-only values.
 */
enum class SuggestionAccept(val label: String) { Add("Add"), Move("Move") }

data class TaskSuggestion(
    val title: String,
    val subtitle: String,
    val accept: SuggestionAccept = SuggestionAccept.Add,
)

@Composable
fun SuggestedTaskRow(
    suggestion: TaskSuggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    acceptTag: String? = null,
    dismissTag: String? = null,
) {
    AgendaSuggestionRow(
        action = suggestion.accept,
        title = suggestion.title,
        metadata = suggestion.subtitle,
        onAccept = onAccept,
        onDismiss = onDismiss,
        modifier = modifier,
        acceptTag = acceptTag,
        dismissTag = dismissTag,
    )
}

@Preview(name = "SuggestedTaskRow", showBackground = true, widthDp = 402)
@Composable
private fun SuggestedTaskRowPreview() {
    RemTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
        ) {
            SuggestedTaskRow(
                suggestion = TaskSuggestion(
                    title = "Set up the TestFlight pipeline using ASC CLI",
                    subtitle = "Samuel · Granola · 8h ago",
                ),
                onAccept = {},
                onDismiss = {},
            )
            SuggestedTaskRow(
                suggestion = TaskSuggestion(
                    title = "Reply to the venue about the deposit",
                    subtitle = "‘Confirm Saturday’ · overdue 3d",
                    accept = SuggestionAccept.Move,
                ),
                onAccept = {},
                onDismiss = {},
            )
        }
    }
}
