package com.rem.designsystem.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.primitives.ContainedIconSize
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * The **Connectors** onboarding screen — "Connect your apps". The Compose sibling of the SwiftUI
 * `OnboardingConnectorsTemplate`; the two render the same screen so the paired iOS⟷Android evidence
 * diffs clean. Authority = Figma `Screen/Connectors` (`133:192`) + the shipping
 * `SharedComposioConnectionsView`.
 *
 * Pure/presentational: the host supplies the connector data + row actions and owns navigation
 * (TopAppBar + Large Title "Connectors") and the first-run coach-mark. Composes canonical
 * `RemSection` (grouped CONNECTED / AVAILABLE surfaces) + a token-bound row.
 *
 * Brand tiles use `ContainedIcon(ContainedIconFill.Tint(...))` with a representative core Material
 * icon per provider — brand *logo* assets are a follow-up (icon registry); the tile color + glyph
 * read as the provider today, matching the SwiftUI stand-ins glyph-for-intent.
 */
data class Connector(
    val icon: ImageVector,
    val tint: Color,
    val name: String,
    val status: String,
    val onClick: () -> Unit,
)

@Composable
fun OnboardingConnectorsScreen(
    connected: List<Connector>,
    available: List<Connector>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(RemSpacing.md),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        if (connected.isNotEmpty()) {
            RemSection(modifier = Modifier.fillMaxWidth(), header = "Connected") {
                connected.forEachIndexed { i, c ->
                    ConnectorRow(c, showSeparator = i < connected.lastIndex)
                }
            }
        }
        if (available.isNotEmpty()) {
            RemSection(modifier = Modifier.fillMaxWidth(), header = "Available") {
                available.forEachIndexed { i, c ->
                    ConnectorRow(c, showSeparator = i < available.lastIndex)
                }
            }
        }
    }
}

/**
 * A single connector row (brand tile · name + status · trailing chevron). Hand-rolled with token-bound
 * metrics, mirroring the iOS `ListRow` and the sibling `ConsentLegalRow` — flagged for extraction once
 * a Compose `ListRow` primitive lands.
 */
@Composable
private fun ConnectorRow(
    c: Connector,
    showSeparator: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = RemColors.current
    Column(modifier = modifier.clickableRole(onClick = c.onClick, label = c.name)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RemSpacing.md, vertical = RemSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContainedIcon(
                icon = c.icon,
                fill = ContainedIconFill.Tint(c.tint),
                size = ContainedIconSize.Small,
                contentDescription = null,
            )
            Spacer(Modifier.width(RemSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = c.name, style = RemTypography.bodyBold, color = colors.labelPrimary)
                Spacer(Modifier.height(3.dp)) // matches the iOS ListRow title↔subtitle gap
                Text(text = c.status, style = RemTypography.caption1, color = colors.labelSecondary)
            }
            Spacer(Modifier.width(RemSpacing.sm))
            Text(
                text = RemMaterialSymbols.DisclosureChevron.glyph,
                fontFamily = RemMaterialSymbols.family(RemMaterialSymbols.DisclosureChevron),
                fontSize = 20.sp,
                color = colors.labelTertiary,
            )
        }
        if (showSeparator) {
            Box(
                modifier = Modifier
                    .padding(start = RemSpacing.xxl + RemSpacing.md)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.separator),
            )
        }
    }
}

@Preview(name = "Connectors · light", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun ConnectorsLightPreview() {
    RemTheme {
        OnboardingConnectorsScreen(
            connected = listOf(
                Connector(Icons.Filled.Email, Color(0xFFEA4335), "Gmail", "Connected · Active") {},
                Connector(Icons.Filled.DateRange, Color(0xFF1A73E8), "Google Calendar", "Connected · Active") {},
                Connector(Icons.Filled.List, Color(0xFF111827), "Notion", "Connected · Active") {},
                Connector(Icons.Filled.Notifications, Color(0xFF6B4FBB), "Slack", "Connected · Paused") {},
            ),
            available = listOf(
                Connector(Icons.Filled.Star, Color(0xFF1FA463), "Google Drive", "Not connected") {},
                Connector(Icons.Filled.Menu, Color(0xFF5E6AD2), "Linear", "Not connected") {},
                Connector(Icons.Filled.CheckCircle, Color(0xFFE44332), "Todoist", "Not connected") {},
            ),
        )
    }
}
