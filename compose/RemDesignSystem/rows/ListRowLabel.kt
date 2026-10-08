package com.rem.designsystem.rows

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemTypography

enum class ListRowTitleLayout { Fill, Hug }

/** Editable label slot matching Figma 1966:60442 and SwiftUI ListRowLabel. */
@Composable
fun ListRowLabel(title: String, subtitle: String? = null,
                 titleLayout: ListRowTitleLayout = ListRowTitleLayout.Fill,
                 titleAccessory: @Composable () -> Unit = {}) {
    val colors = RemColors.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, modifier = if (titleLayout == ListRowTitleLayout.Fill) Modifier.weight(1f) else Modifier,
                style = RemTypography.body, color = colors.labelPrimary)
            titleAccessory()
        }
        if (subtitle != null) Text(subtitle, style = RemTypography.footnote, color = colors.labelSecondary)
    }
}
