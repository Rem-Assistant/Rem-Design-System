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
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
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

/**
 * How the leading column is drawn. [Compact] is the design-system default and is unchanged. [App]
 * reproduces the shipped Rem app's time column (RemClaw `Rem/Sources/Components/TaskEventRowView.swift`)
 * for products that want parity with it:
 * - **Width** 64dp: `TimeIndicatorWidthCalculator` (L6–14), applied at L67–70.
 * - **Alignment**: the column is start-aligned (L69) and the row centres vertically (L66, `alignment:
 *   .center`); the column-to-content gap is `Spacing.sm` (L66). The Schedule / Clock glyphs centre in
 *   the column (L110–118).
 * - **Format and type**: a [TaskEventLeading.Time] label is drawn as the app's stacked clock — the
 *   two-digit 12-hour hour in title2, then minutes over AM/PM in caption1, 2dp apart (`timeDisplay`,
 *   L125–137; `formatHour` / `formatMinutes` / `formatAMPM`, L358–372) — in the primary label colour (the
 *   app sets no foreground, L128–134). The label is parsed by [TaskEventClockFace]; a label it cannot read
 *   (e.g. "Oct 14") falls back to the compact footnote.
 * - **Glyphs**: Clock at title3 size, 20dp (L110–113); Schedule at 22dp (L115–118).
 */
enum class TaskEventLeadingStyle { Compact, App }

/**
 * The shipped Rem app's stacked time face for a [TaskEventLeading.Time] label, used by
 * [TaskEventLeadingStyle.App]. Mirrors RemClaw `Rem/Sources/Components/TaskEventRowView.swift`:
 * `formatHour` (L358–362, two-digit 12-hour, 0 → 12), `formatMinutes` (L364–367) and `formatAMPM`
 * (L369–372). [parse] reads `H:mm` / `HH:mm` (24-hour, e.g. "14:00") and `h:mm AM` / `h:mm PM` (any case,
 * optional space, e.g. "8:00 AM"); anything else (e.g. "Oct 14") is `null` and the row keeps the compact
 * label. SwiftUI twin: `TaskEventClockFace` in `Rows/TaskEventRow.swift`; both are unit-tested alike.
 */
data class TaskEventClockFace(val hour: String, val minutes: String, val meridiem: String) {
    /** TalkBack reads the time once, naturally ("9:00 AM"), not as three fragments. */
    val spoken: String get() = "${hour.toInt()}:$minutes $meridiem"

    companion object {
        /** RemClaw `TaskEventRowView.swift` `TimeIndicatorWidthCalculator` (L6–14): the fixed 64pt column. */
        val AppColumnWidth = 64.dp

        private val Pattern = Regex("""^(\d{1,2}):(\d{2})[\s\u00A0\u202F]*([AaPp][Mm])?$""")

        /** From a 24-hour clock time ([hour24] 0–23, [minute] 0–59). */
        fun of(hour24: Int, minute: Int): TaskEventClockFace {
            val display = if (hour24 == 0) 12 else if (hour24 > 12) hour24 - 12 else hour24
            return TaskEventClockFace(
                hour = display.toString().padStart(2, '0'),
                minutes = minute.toString().padStart(2, '0'),
                meridiem = if (hour24 < 12) "AM" else "PM",
            )
        }

        fun parse(label: String): TaskEventClockFace? {
            val match = Pattern.matchEntire(label.trim { it.isWhitespace() }) ?: return null
            val (rawHour, rawMinute, suffix) = match.destructured
            val hour = rawHour.toInt()
            val minute = rawMinute.toInt()
            if (minute >= 60) return null
            val hour24 = when (suffix.uppercase()) {
                "" -> if (hour < 24) hour else return null
                "AM", "PM" -> if (hour in 1..12) (hour % 12) + (if (suffix.uppercase() == "PM") 12 else 0) else return null
                else -> return null
            }
            return of(hour24, minute)
        }
    }
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
    // The shipped app's completed status: a filled check circle instead of the ring (RemClaw
    // `TaskStatusIndicator.swift` L24, L64–65 `checkmark.circle.fill`). Wins over [pending]; tasks only.
    completed: Boolean = false,
    // [TaskEventLeadingStyle.Compact] (default, unchanged) or [TaskEventLeadingStyle.App] for parity
    // with the shipped app's 64pt time column.
    leadingStyle: TaskEventLeadingStyle = TaskEventLeadingStyle.Compact,
) {
    val colors = RemColors.current
    val app = leadingStyle == TaskEventLeadingStyle.App
    val pillKind = when (kind) {
        is TaskEventKind.Task -> RemPillKind.List
        is TaskEventKind.Event -> RemPillKind.Dot(kind.color)
    }

    Row(
        modifier = modifier
            .padding(horizontal = RemSpacing.md, vertical = RemSpacing.sm),
        verticalAlignment = if (app) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(if (app) RemSpacing.sm else RemSpacing.md),
    ) {
        if (leading != TaskEventLeading.None) {
            // App: glyphs centre in the column (RemClaw TaskEventRowView.swift L110–118); time starts it (L69).
            val glyphAlignment = if (app) Alignment.Center else Alignment.CenterStart
            Box(
                modifier = Modifier.width(if (app) TaskEventClockFace.AppColumnWidth else LeadingColumnWidth),
                contentAlignment = if (leading is TaskEventLeading.Time) Alignment.CenterStart else glyphAlignment,
            ) {
                when (leading) {
                    is TaskEventLeading.Time -> {
                        val face = if (app) TaskEventClockFace.parse(leading.label) else null
                        if (face != null) {
                            AppClock(face)
                        } else {
                            Text(
                                text = leading.label,
                                style = RemTypography.footnote,
                                color = colors.labelSecondary,
                            )
                        }
                    }
                    is TaskEventLeading.Schedule -> Icon(
                        Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = colors.labelSecondary,
                        modifier = Modifier.size(if (app) 22.dp else 20.dp),
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
                    // A completed task draws the shipped app's filled check circle (RemClaw
                    // TaskStatusIndicator.swift L64–65, primary label colour per L73–75). A pending row (a
                    // just-accepted suggestion) draws a dashed ring; a committed task draws a solid one.
                    if (completed) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Completed",
                            tint = colors.labelPrimary,
                            modifier = Modifier.size(StatusRingSize),
                        )
                    } else if (pending) {
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

/** The shipped app's stacked clock (RemClaw `TaskEventRowView.swift` `timeDisplay`, L125–137). */
@Composable
private fun AppClock(face: TaskEventClockFace) {
    val colors = RemColors.current
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = face.spoken },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // `.title2` (22pt regular): the token set only carries the bold cut, so take its metrics at Normal.
        Text(face.hour, style = RemTypography.title2Bold.copy(fontWeight = FontWeight.Normal), color = colors.labelPrimary)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(face.minutes, style = RemTypography.caption1, color = colors.labelPrimary)
            Text(face.meridiem, style = RemTypography.caption1, color = colors.labelPrimary)
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
            // App parity: the shipped app's 64pt stacked time column and a completed task.
            TaskEventRow(
                kind = TaskEventKind.Task,
                title = "Send the venue deposit",
                leading = TaskEventLeading.Time("9:00"),
                pills = listOf("Follow-ups"),
                completed = true,
                leadingStyle = TaskEventLeadingStyle.App,
            )
        }
    }
}
