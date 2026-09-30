package com.rem.designsystem.rows

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography

/**
 * Compose sibling of the SwiftUI `ListRow` (`Rows/ListRow.swift`): the canonical list-row **content** —
 * **[leading accessory] · Title/Subtitle · [trailing accessory]**. Meant to sit inside a [RemSection]
 * (which owns the grouped surface, radius, and inset separators), so this composable draws only the
 * row itself. The trailing slot is for non-disclosure accessories (a `Switch`, a value label, a
 * badge); for a navigation row use [DisclosureChevron]. When [onClick] is set the whole row is the
 * tap target.
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
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = RemColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = RemSpacing.md, vertical = RemSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        leading?.invoke()
        Column(
            modifier = Modifier.weight(1f),
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
        }
        trailing?.invoke()
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
