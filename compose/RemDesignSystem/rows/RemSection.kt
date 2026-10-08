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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography

/**
 * List-section surface with optional header/footer text and an open rows slot.
 *
 * Figma canonical: Section (`1307:667`). InsetGrouped owns the rounded secondary surface; Plain
 * inherits its parent surface.
 */
enum class RemSectionStyle {
    InsetGrouped,
    Plain,
}

@Composable
fun RemSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    style: RemSectionStyle = RemSectionStyle.InsetGrouped,
    settingsHeader: Boolean = false,
    rows: @Composable ColumnScope.() -> Unit,
) {
    val colors = RemColors.current
    Column(modifier = modifier) {
        if (header != null) {
            Text(
                text = if (settingsHeader) header else header.uppercase(),
                style = if (settingsHeader) RemTypography.body.copy(fontWeight = FontWeight.SemiBold) else RemTypography.footnote,
                color = colors.labelSecondary,
                modifier = Modifier.padding(
                    start = if (settingsHeader) 16.dp else RemSpacing.md,
                    end = if (settingsHeader) 16.dp else RemSpacing.md,
                    bottom = if (settingsHeader) 6.dp else RemSpacing.xs,
                ),
            )
        }
        val rowsModifier = when (style) {
            RemSectionStyle.InsetGrouped -> Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(RemRadius.xlarge))
                .background(colors.backgroundSecondary)
            RemSectionStyle.Plain -> Modifier.fillMaxWidth()
        }
        Column(
            modifier = rowsModifier,
            content = rows,
        )
        if (footer != null) {
            Text(
                text = footer,
                style = RemTypography.footnote,
                color = colors.labelSecondary,
                modifier = Modifier.padding(
                    start = RemSpacing.md,
                    top = RemSpacing.xs,
                    end = RemSpacing.md,
                ),
            )
        }
    }
}

/** Slot-based adapter for configurable headers/footers, without changing the text convenience API. */
@Composable
fun RemSection(
    header: @Composable () -> Unit,
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    style: RemSectionStyle = RemSectionStyle.InsetGrouped,
    rows: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier) {
        Column(Modifier.padding(horizontal = RemSpacing.md)) { header() }
        RemSection(style = style, rows = rows)
        Column(Modifier.padding(horizontal = RemSpacing.md)) { footer() }
    }
}
