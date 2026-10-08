package com.rem.designsystem.primitives

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * Canonical **empty / content-unavailable** state — centered icon · title · message · optional action.
 * Compose sibling of the SwiftUI `RemContentUnavailableView`; the reusable piece of `Screen/Agenda-Empty`
 * (`140:1613`, "No agenda yet"). Pure/presentational, token-driven; the icon is a native Material
 * [ImageVector] (shared intent, native form).
 */
@Composable
fun RemContentUnavailableView(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = RemColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(RemSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.labelTertiary,
            modifier = Modifier.size(52.dp),
        )
        Spacer(Modifier.height(RemSpacing.md))
        Text(text = title, style = RemTypography.title3Bold, color = colors.labelPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(RemSpacing.xs))
        Text(text = message, style = RemTypography.subheadline, color = colors.labelSecondary, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(RemSpacing.sm))
            TextButton(onClick = onAction) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = colors.brandBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(RemSpacing.xs))
                Text(text = actionLabel, style = RemTypography.body, color = colors.brandBlue)
            }
        }
    }
}

@Preview(name = "ContentUnavailable · Agenda empty", showBackground = true, widthDp = 402, heightDp = 600)
@Composable
private fun RemContentUnavailablePreview() {
    RemTheme {
        RemContentUnavailableView(
            icon = Icons.Filled.DateRange,
            title = "No agenda yet",
            message = "Create a new task or schedule existing ones",
            actionLabel = "Add New",
            onAction = {},
        )
    }
}
