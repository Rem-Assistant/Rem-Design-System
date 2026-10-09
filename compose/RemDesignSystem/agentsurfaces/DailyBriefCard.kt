package com.rem.designsystem.agentsurfaces

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * **DailyBriefCard** — Compose sibling of the SwiftUI `DailyBriefCard` (Rem/Sources/Components). The
 * orchestrator summary inserted at the top of the Agenda for *today*: a title + chevron, the brief
 * prose, and — only when a structured brief has no prose — a fallback row of count capsules
 * (blocked / overdue / done / today). An optional "Read latest brief" action sits **beside** the
 * navigation button, never nested inside it (so the playback control is independently tappable).
 *
 * Deliberately **uncontained** — no boxed card background. The brief reads as an inline summary at the
 * top of the agenda, exactly as the shipping iOS card. (The Android build's grey-card + "DAILY BRIEF"
 * header treatment was engineer drift; this matches the design-system source of truth.)
 *
 * Canonical Figma set `2190:12237`: Headline/Summary and Playback Ready/Reading map to this API.
 * Finished/Retry need a separate state API; see `docs/contracts/playground-mappings.md`.
 * Authority: `DailyBriefCard.swift` + `DailyBriefAgendaPresentation`. Token-only values.
 */
data class BriefCounts(
    val blocked: Int = 0,
    val overdue: Int = 0,
    val scheduledToday: Int = 0,
    val done: Int = 0,
    val total: Int = 0,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyBriefCard(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    counts: BriefCounts = BriefCounts(),
    onTap: (() -> Unit)? = null,
    onRead: (() -> Unit)? = null,
    isReading: Boolean = false,
) {
    val colors = RemColors.current
    // Counts are a prose-less fallback: only when there is no summary AND something is non-zero.
    val showCounts = summary == null &&
        (counts.total > 0 || counts.blocked > 0 || counts.overdue > 0 || counts.scheduledToday > 0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = RemSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
        ) {
            // Header — title + drill-down chevron.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = RemTypography.title3Bold,
                    color = colors.labelPrimary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = colors.labelTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }

            // Prose — the canonical brief summary.
            if (summary != null) {
                Text(
                    text = summary,
                    style = RemTypography.subheadline,
                    color = colors.labelSecondary,
                )
            }

            // Counts fallback — only for a prose-less structured brief.
            if (showCounts) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (counts.blocked > 0) {
                        BriefCapsule(Icons.Filled.Warning, "${counts.blocked} blocked", colors.systemRed)
                    }
                    if (counts.overdue > 0) {
                        BriefCapsule(Icons.Filled.Schedule, "${counts.overdue} overdue", colors.systemOrange)
                    }
                    if (counts.total > 0) {
                        BriefCapsule(null, "${counts.done} of ${counts.total} done", colors.systemGreen)
                    }
                    if (counts.scheduledToday > 0) {
                        BriefCapsule(Icons.Filled.CalendarMonth, "${counts.scheduledToday} today", colors.brandBlue)
                    }
                }
            }
        }

        // Read action — a sibling of the navigation button, never nested inside it.
        if (onRead != null) {
            Row(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clickable(onClick = onRead),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
            ) {
                Icon(
                    if (isReading) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = colors.brandBlue,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = if (isReading) "Stop reading" else "Read latest brief",
                    style = RemTypography.subheadline.copy(fontWeight = FontWeight.Bold),
                    color = colors.brandBlue,
                )
            }
        }
    }
}

/** A calm, desaturated count chip — the brief's blocked / overdue / done / today glyphs. */
@Composable
private fun BriefCapsule(icon: ImageVector?, text: String, color: Color) {
    val colors = RemColors.current
    Row(
        modifier = Modifier
            .background(colors.backgroundSecondary, CircleShape)
            .padding(horizontal = RemSpacing.sm, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        } else {
            Box(Modifier.size(8.dp).background(color, CircleShape))
        }
        Text(text = text, style = RemTypography.caption1, color = colors.labelSecondary)
    }
}

@Preview(name = "DailyBriefCard — prose", showBackground = true, widthDp = 402)
@Composable
private fun DailyBriefCardProsePreview() {
    RemTheme {
        DailyBriefCard(
            title = "Daily brief",
            summary = "Sent to damilola.ogunnaike@gmail.com at 9:37 am — “Hi Damilola, I’ll send " +
                "you the notes from yesterday’s call before 2pm. Best, Larissa.” 1 task overdue needs attention.",
            onTap = {},
            onRead = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "DailyBriefCard — counts", showBackground = true, widthDp = 402)
@Composable
private fun DailyBriefCardCountsPreview() {
    RemTheme {
        DailyBriefCard(
            title = "Good morning",
            counts = BriefCounts(blocked = 2, overdue = 3, scheduledToday = 5, done = 2, total = 7),
            onTap = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
