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
 * **AgendaSuggestionRow** — canonical Compose twin of the SwiftUI `AgendaSuggestionRow`
 * (Figma `2336:19583`, variants `action=add` `2336:19561` / `action=move` `2336:19572`). A proposed
 * task rendered as a dashed-border rounded tile with three regions:
 *   • a leading **accept CTA** ([SuggestionAccept] glyph + "Add" / "Move", brandBlue) — where a
 *     `TaskEventRow` shows the time; a suggestion's whole point is its action;
 *   • the **title + reason** (the field-owned "why");
 *   • a trailing **dismiss ✕** — this is not a drill-down.
 *
 * Per the component's authored description there is **no spinner, success badge, error card or
 * Retry** — Add / Move accept optimistically and ✕ dismisses. Token-only; the host owns the data and
 * the optimistic model. SwiftUI sibling: `AgendaSuggestionRow`. The [SuggestedTaskRow] convenience
 * wrapper preserves the existing `TaskSuggestion`-based call sites against this same rendering.
 */
private val LeftSlotWidth = 64.dp

@Composable
fun AgendaSuggestionRow(
    action: SuggestionAccept,
    title: String,
    metadata: String,
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
                if (action == SuggestionAccept.Add) Icons.Filled.Add else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = colors.brandBlue,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = action.label,
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
                text = title,
                style = RemTypography.bodyBold,
                color = colors.labelPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = metadata,
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

@Preview(name = "AgendaSuggestionRow", showBackground = true, widthDp = 402)
@Composable
private fun AgendaSuggestionRowPreview() {
    RemTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
        ) {
            AgendaSuggestionRow(
                action = SuggestionAccept.Add,
                title = "Prep for tonight’s rehearsal",
                metadata = "Your calendar has rehearsal at 6:00 PM.",
                onAccept = {},
                onDismiss = {},
            )
            AgendaSuggestionRow(
                action = SuggestionAccept.Move,
                title = "Move rehearsal check-in to 3:00 PM",
                metadata = "Your calendar has a conflict at 8:00 AM.",
                onAccept = {},
                onDismiss = {},
            )
        }
    }
}
