package com.rem.designsystem.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.primitives.RemPill
import com.rem.designsystem.primitives.RemPillKind
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * **TaskEventRow** — Compose sibling of the SwiftUI [com.rem.designsystem.rows] `TaskEventRow`. The
 * Agenda / Inbox row for a single task or event: a specialization of the base 3-slot row — **leading**
 * (time / schedule / clock) · **content** (task status ring OR event bar, the title, and a Pills row) ·
 * **trailing** (the list-level chevron is NOT baked; the host supplies navigation).
 *
 * The Pills row reuses the canonical [RemPill] primitive — never a bespoke badge. The pill kind is
 * derived from [TaskEventKind]: **Task → [RemPillKind.List]**, **Event → [RemPillKind.Dot]** with the
 * calendar color. Hide the row (`showPills = false`) for unfiled rows such as Inbox.
 *
 * Figma canonical: **TaskEventRow** component set (`46:21`), variants `Kind = task | event` ×
 * `Leading = Time | None`; per the description `Leading` also carries `schedule` / `clock` glyphs.
 * Verified against the set: the event accent bar (`46:19`) is `labelSecondary`. SF Symbols map to the
 * closest Material glyphs: `calendar.badge.plus` → [Icons.Filled.DateRange], `clock` → [Icons.Filled.Schedule].
 */
sealed interface TaskEventKind {
    data object Task : TaskEventKind
    /** [color] is the calendar color that tints the Pills-row dots (the bar itself stays neutral). */
    data class Event(val color: Color) : TaskEventKind
}

/** Leading accessory: a small time label, a schedule/clock glyph, or none (content shifts leading). */
sealed interface TaskEventLeading {
    data class Time(val label: String) : TaskEventLeading
    data object Schedule : TaskEventLeading
    data object Clock : TaskEventLeading
    data object None : TaskEventLeading
}

private val LeadingColumnWidth = 52.dp
private val StatusRingSize = 24.dp
private val EventBarWidth = 4.dp

@Composable
fun TaskEventRow(
    kind: TaskEventKind,
    title: String,
    modifier: Modifier = Modifier,
    leading: TaskEventLeading = TaskEventLeading.None,
    pills: List<String> = emptyList(),
    showPills: Boolean = true,
    pending: Boolean = false,
) {
    val colors = RemColors.current
    val pillKind = when (kind) {
        is TaskEventKind.Task -> RemPillKind.List
        is TaskEventKind.Event -> RemPillKind.Dot(kind.color)
    }

    Row(
        modifier = modifier
            .padding(horizontal = RemSpacing.md, vertical = RemSpacing.sm),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        if (leading != TaskEventLeading.None) {
            Box(modifier = Modifier.width(LeadingColumnWidth), contentAlignment = Alignment.CenterStart) {
                when (leading) {
                    is TaskEventLeading.Time -> Text(
                        text = leading.label,
                        style = RemTypography.footnote,
                        color = colors.labelSecondary,
                    )
                    is TaskEventLeading.Schedule -> Icon(
                        Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = colors.labelSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                    is TaskEventLeading.Clock -> Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = colors.labelSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                    is TaskEventLeading.None -> {}
                }
            }
        }

        when (kind) {
            is TaskEventKind.Task -> Column(
                verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
                ) {
                    // A pending row (a just-accepted suggestion) draws a dashed ring; a committed
                    // task draws a solid one.
                    if (pending) {
                        Box(
                            modifier = Modifier
                                .size(StatusRingSize)
                                .drawBehind {
                                    drawCircle(
                                        color = colors.labelSecondary,
                                        radius = size.minDimension / 2 - 0.75.dp.toPx(),
                                        style = Stroke(
                                            width = 1.5.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(
                                                floatArrayOf(3.dp.toPx(), 2.dp.toPx()),
                                            ),
                                        ),
                                    )
                                },
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(StatusRingSize)
                                .border(1.5.dp, colors.labelSecondary, CircleShape),
                        )
                    }
                    TitleText(title)
                }
                PillsRow(pills, showPills, pillKind)
            }

            is TaskEventKind.Event -> Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .width(EventBarWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(EventBarWidth / 2))
                        .background(colors.labelSecondary),
                )
                Column(verticalArrangement = Arrangement.spacedBy(RemSpacing.sm)) {
                    TitleText(title)
                    PillsRow(pills, showPills, pillKind)
                }
            }
        }
    }
}

@Composable
private fun TitleText(title: String) {
    Text(
        text = title,
        style = RemTypography.bodyBold,
        color = RemColors.current.labelPrimary,
    )
}

@Composable
private fun PillsRow(pills: List<String>, showPills: Boolean, kind: RemPillKind) {
    if (showPills && pills.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm)) {
            pills.forEach { RemPill(text = it, kind = kind) }
        }
    }
}

@Preview(name = "TaskEventRow", showBackground = true)
@Composable
private fun TaskEventRowPreview() {
    RemTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            // Task with a list pill + a leading time.
            TaskEventRow(
                kind = TaskEventKind.Task,
                title = "Reply to the venue",
                leading = TaskEventLeading.Time("8:00 AM"),
                pills = listOf("Follow-ups"),
            )
            // Event with a calendar-color dot pill + a leading time.
            TaskEventRow(
                kind = TaskEventKind.Event(RemColors.current.systemBlue),
                title = "Coffee chat with a mentor",
                leading = TaskEventLeading.Time("7:00 PM"),
                pills = listOf("Personal"),
            )
            // Inbox row: schedule leading, Pills row hidden (unfiled).
            TaskEventRow(
                kind = TaskEventKind.Task,
                title = "Draft the offsite agenda",
                leading = TaskEventLeading.Schedule,
                pills = listOf("Work"),
                showPills = false,
            )
        }
    }
}
