package com.rem.designsystem.rows

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemOpacity
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography

/** Semantic row emphasis backed by the shared foundation opacity token. */
enum class ListRowEmphasis { Standard, Deemphasized }

/**
 * Canonical Compose list-row content: optional leading accessory, title/subtitle, and trailing slot.
 * It is the Android sibling of SwiftUI `ListRow` and Figma `ListRow` (`101:18`). The root stays
 * transparent so a containing [RemSection] owns grouped-list surface color.
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
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = RemColors.current
    val emphasisAlpha = when (emphasis) {
        ListRowEmphasis.Standard -> 1f
        ListRowEmphasis.Deemphasized -> RemOpacity.deemphasized
    }
    val interaction = if (onClick != null) {
        Modifier
            .semantics { role = Role.Button }
            .clickable(enabled = enabled, onClick = onClick)
    } else {
        Modifier
    }
    Column(modifier = modifier.alpha(emphasisAlpha).then(interaction)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RemSpacing.md, vertical = RemSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(RemSpacing.md))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = RemTypography.bodyBold, color = colors.labelPrimary)
                if (subtitle != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(subtitle, style = RemTypography.caption1, color = colors.labelSecondary)
                }
            }
            Spacer(Modifier.width(RemSpacing.sm))
            trailing()
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
