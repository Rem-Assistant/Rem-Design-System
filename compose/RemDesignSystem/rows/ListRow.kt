package com.rem.designsystem.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemOpacity
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography

/** Semantic row emphasis backed by the shared foundation opacity token. */
enum class ListRowEmphasis { Standard, Deemphasized }

/**
 * Compose sibling of the SwiftUI `ListRow` (`Rows/ListRow.swift`): the canonical list-row **content** —
 * **[leading accessory] · Title/Subtitle · [trailing accessory]**. Meant to sit inside a [RemSection]
 * (which owns the grouped surface, radius, and inset separators), so this composable draws only the
 * row itself and its root stays transparent. The trailing slot is for non-disclosure accessories (a
 * `Switch`, a value label, a badge); for a navigation row use [DisclosureChevron]. When [onClick] is
 * set the whole row is the tap target; [emphasis] de-emphasizes a locked row with the foundation
 * opacity token, and [showSeparator] draws an inset separator for rows outside a [RemSection].
 * [supporting] is an optional accessory under the title (Material `ListItem.supportingContent`), used
 * when a trailing value cannot share one line with the title, so the title never wraps.
 *
 * [titleLayout] normally fills the available label space; `Hug` permits natural-width measurement
 * for adaptive row composition without querying intrinsic sizes of interactive accessories.
 *
 * Figma canonical: ListRow (`101:18`) + ListRowLabel (`188:2`). Compose has no native `List`/`Section`
 * disclosure chrome, so — unlike iOS, which leans on `NavigationLink` — the chevron is explicit here.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    emphasis: ListRowEmphasis = ListRowEmphasis.Standard,
    showSeparator: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    supporting: (@Composable () -> Unit)? = null,
    titleLayout: ListRowTitleLayout = ListRowTitleLayout.Fill,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = RemColors.current
    val emphasisAlpha = when (emphasis) {
        ListRowEmphasis.Standard -> 1f
        ListRowEmphasis.Deemphasized -> RemOpacity.deemphasized
    }
    val interaction = if (onClick != null) {
        Modifier.semantics { role = Role.Button }.clickable(enabled = enabled, onClick = onClick)
    } else {
        Modifier
    }
    Column(modifier = modifier.alpha(emphasisAlpha).then(interaction)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RemSpacing.md, vertical = RemSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
        ) {
            leading?.invoke()
            Column(
                modifier = if (titleLayout == ListRowTitleLayout.Fill) Modifier.weight(1f) else Modifier,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.labelPrimary,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = RemTypography.caption1,
                        color = colors.labelSecondary,
                    )
                }
                if (supporting != null) {
                    Box(Modifier.padding(top = RemSpacing.xs)) { supporting() }
                }
            }
            trailing?.invoke(this)
        }
        if (showSeparator) {
            Box(
                modifier = Modifier
                    .padding(start = if (leading == null) RemSpacing.md else RemSpacing.xxl + RemSpacing.md)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.separator),
            )
        }
    }
}

/**
 * Explicit disclosure chevron — the twin of the SwiftUI `DisclosureChevron`. Auto-mirrored so it flips
 * for RTL. Tinted `labelTertiary`, the tertiary accessory colour.
 */
@Composable
fun DisclosureChevron(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = RemColors.current.labelTertiary,
        modifier = modifier.size(20.dp),
    )
}

/** Content-slot settings density, matching the SwiftUI ListRow overload. */
@Composable
fun ListRow(
    modifier: Modifier = Modifier,
    showsDivider: Boolean = false,
    onClick: (() -> Unit)? = null,
    leading: @Composable () -> Unit,
    content: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Column {
        Row(
            modifier.fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            leading()
            Column(Modifier.weight(1f)) { content() }
            trailing()
        }
        if (showsDivider) HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = RemColors.current.separator)
    }
}
