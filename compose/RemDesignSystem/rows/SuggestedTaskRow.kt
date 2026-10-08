package com.rem.designsystem.rows

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * **SuggestedTaskRow** — Compose sibling of the SwiftUI `SuggestedTaskRow`. A proposed task, rendered
 * to **mirror [TaskEventRow]** with three deliberate substitutions:
 *   • the left **time slot becomes an accept CTA** (Add / Move) — a suggestion's whole point is its action;
 *   • the trailing **chevron becomes a dismiss ✕** — this is not a drill-down;
 *   • a **dashed ring** frames the row, signalling "proposed, not yet real".
 *
 * Every row states WHY ([TaskSuggestion.subtitle], e.g. "'File visa paperwork' · overdue 3d") — an
 * unattributed suggestion is indistinguishable from the app inventing work. Token-only values.
 *
 * NOTE: the Android build's "title + trailing [Add] [Dismiss] text buttons, no ring" treatment was
 * engineer drift; this row is the design-system source of truth (left CTA + dashed ring + ✕).
 */
enum class SuggestionAccept(val label: String) { Add("Add"), Move("Move") }

data class TaskSuggestion(
    val title: String,
    val subtitle: String,
    val accept: SuggestionAccept = SuggestionAccept.Add,
)

private val LeftSlotWidth = 64.dp

@Composable
fun SuggestedTaskRow(
    suggestion: TaskSuggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    acceptTag: String? = null,
    dismissTag: String? = null,
) {
    val colors = RemColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = colors.separator,
                    cornerRadius = CornerRadius(RemRadius.medium.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                    ),
                )
            }
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        // LEFT — an accept CTA where a task row shows the time.
        Column(
            modifier = Modifier
                .width(LeftSlotWidth)
                .heightIn(min = 44.dp)
                .then(acceptTag?.let { Modifier.testTag(it) } ?: Modifier)
                .clickable(onClick = onAccept),
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                if (suggestion.accept == SuggestionAccept.Add) Icons.Filled.Add else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = colors.brandBlue,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = suggestion.accept.label,
                style = RemTypography.caption1.copy(fontWeight = FontWeight.Bold),
                color = colors.brandBlue,
            )
        }

        // MIDDLE — title + the WHY line, mirroring TaskEventRow's content column.
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.xs),
        ) {
            Text(
                text = suggestion.title,
                style = RemTypography.bodyBold,
                color = colors.labelPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = suggestion.subtitle,
                style = RemTypography.footnote,
                color = colors.labelSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // RIGHT — dismiss ✕, replacing the drill-down chevron.
        Icon(
            Icons.Filled.Close,
            contentDescription = "Dismiss suggestion",
            tint = colors.labelSecondary,
            modifier = Modifier
                .size(30.dp)
                .then(dismissTag?.let { Modifier.testTag(it) } ?: Modifier)
                .clickable(onClick = onDismiss)
                .padding(8.dp),
        )
    }
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
