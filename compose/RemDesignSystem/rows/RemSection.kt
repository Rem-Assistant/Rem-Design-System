package com.rem.designsystem.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography

/**
 * Grouped list surface with optional header/footer text and an open rows slot.
 *
 * Figma canonical: Section (`741:311`). The surface intentionally has no outline; hierarchy comes
 * from backgroundSecondary, the xlarge radius, and row-owned separators.
 */
@Composable
fun RemSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    rows: @Composable ColumnScope.() -> Unit,
) {
    val colors = RemColors.current
    Column(modifier = modifier) {
        if (header != null) {
            Text(
                text = header.uppercase(),
                style = RemTypography.footnote,
                color = colors.labelSecondary,
                modifier = Modifier.padding(horizontal = RemSpacing.md, bottom = RemSpacing.xs),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(RemRadius.xlarge))
                .background(colors.backgroundSecondary),
            content = rows,
        )
        if (footer != null) {
            Text(
                text = footer,
                style = RemTypography.footnote,
                color = colors.labelSecondary,
                modifier = Modifier.padding(horizontal = RemSpacing.md, top = RemSpacing.xs),
            )
        }
    }
}
