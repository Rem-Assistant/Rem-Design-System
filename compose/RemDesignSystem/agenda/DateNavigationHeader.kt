package com.rem.designsystem.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rem.designsystem.tokens.Inter
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme

/**
 * **DateNavigationHeader** — Compose sibling of the SwiftUI [DateNavigationHeader]. The Agenda day
 * header: a *previous* affordance, the current day (a brandBlue calendar glyph + relative [title]
 * stacked over the full [dateText]), and a *next* affordance. Each affordance is a **unit** — an
 * outer 22dp chevron paired with three 10×4 dashes that point inward toward the label, mirroring the
 * `unit` frames in Figma `43:2`.
 *
 * The chevrons and dashes share labelSecondary (the Figma binds both — and the date line — to
 * `label/secondary`, #3C3C43 @ 60%); only the calendar glyph is brandBlue and only [title] is
 * labelPrimary. The 22-bold / 13-bold type sizes and the 10×4 dash are the spec's explicit values;
 * everything else routes through the Rem tokens. Icons are Material equivalents of the iOS SF Symbols
 * (`calendar` / `chevron.left` / `chevron.right`), matching the [com.rem.designsystem.primitives.RemPill]
 * convention of using Material glyphs on Android.
 */
@Composable
fun DateNavigationHeader(
    dateText: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Today",
    onCalendarTap: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = RemSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavigationUnit(previous = true, onClick = onPrevious)
        Spacer(Modifier.weight(1f))
        DayLabel(title = title, dateText = dateText, onCalendarTap = onCalendarTap)
        Spacer(Modifier.weight(1f))
        NavigationUnit(previous = false, onClick = onNext)
    }
}

/** Center column: the calendar glyph + relative title, with the full date centered beneath. */
@Composable
private fun DayLabel(title: String, dateText: String, onCalendarTap: (() -> Unit)?) {
    val colors = RemColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RemSpacing.xs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs),
        ) {
            val calendarModifier = Modifier
                .size(22.dp)
                .let { if (onCalendarTap != null) it.clickable(onClick = onCalendarTap) else it }
            Icon(
                Icons.Filled.CalendarMonth,
                contentDescription = if (onCalendarTap != null) "Open calendar" else null,
                tint = colors.brandBlue,
                modifier = calendarModifier,
            )
            Text(
                text = title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp),
                color = colors.labelPrimary,
            )
        }
        Text(
            text = dateText,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 13.sp),
            color = colors.labelSecondary,
        )
    }
}

/**
 * A prev/next affordance: the outer chevron and the inward-pointing dashes form one tappable `unit`
 * (Figma names the frame `unit`), giving a comfortably large target.
 */
@Composable
private fun NavigationUnit(previous: Boolean, onClick: () -> Unit) {
    val colors = RemColors.current
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs),
    ) {
        if (previous) {
            Icon(
                Icons.Filled.ChevronLeft,
                contentDescription = "Previous day",
                tint = colors.labelSecondary,
                modifier = Modifier.size(22.dp),
            )
            DashGroup()
        } else {
            DashGroup()
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Next day",
                tint = colors.labelSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Three 10×4 pill dashes in labelSecondary — the decorative prev/next hint beside each chevron. */
@Composable
private fun DashGroup() {
    val colors = RemColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs)) {
        repeat(3) {
            Box(
                Modifier
                    .size(width = 10.dp, height = 4.dp)
                    .background(colors.labelSecondary, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Preview(name = "DateNavigationHeader", showBackground = true)
@Composable
private fun DateNavigationHeaderPreview() {
    RemTheme {
        Column(
            modifier = Modifier.padding(RemSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.xl),
        ) {
            DateNavigationHeader(dateText = "Aug 13 2026", onPrevious = {}, onNext = {})
            DateNavigationHeader(
                dateText = "Aug 14 2026",
                onPrevious = {},
                onNext = {},
                title = "Tomorrow",
                onCalendarTap = {},
            )
        }
    }
}
