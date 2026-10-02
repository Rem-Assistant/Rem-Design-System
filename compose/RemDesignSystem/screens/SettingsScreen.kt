package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.rows.DisclosureChevron
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * **SettingsScreen** — Compose sibling of the SwiftUI [SettingsScreen]. A large "Settings" title over a
 * plan card + grouped, inset [RemSection] cards of navigation [ListRow]s (monochrome [NavIcon] leading
 * + chevron trailing), supplied by the host through [content]. Chrome comes from the OS. A single-row
 * section draws no divider; multi-row sections carry inset separators between rows.
 *
 * Authority: `Screen/Settings` (Figma page "Settings") — the reference IA: a Free-plan usage card, then
 * Connectors / Devices / Wallet / Secure credentials store / Permissions / Messaging channels, then
 * Notifications / Appearance.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    title: String = "Settings",
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = RemColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.backgroundPrimary)) {
        Text(
            text = title,
            style = RemTypography.largeTitle.copy(fontWeight = FontWeight.Bold),
            color = colors.labelPrimary,
            modifier = Modifier
                .padding(horizontal = RemSpacing.lg)
                .padding(top = RemSpacing.md, bottom = RemSpacing.sm),
        )
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RemSpacing.lg)
                .padding(bottom = RemSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.xl),
            content = content,
        )
    }
}

/** A plain monochrome settings-row leading glyph (labelPrimary, 24dp) — the reference Settings rows use
 *  flat icons, not colored ContainedIcon tiles. */
@Composable
private fun NavIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, tint = RemColors.current.labelPrimary, modifier = Modifier.size(24.dp))
}

/** Canonical inset row separator (60dp = xxl + md leading inset), matching the Swift `RemSection`. */
@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .padding(start = RemSpacing.xxl + RemSpacing.md)
            .fillMaxWidth()
            .height(1.dp)
            .background(RemColors.current.separator),
    )
}

/** The **plan / usage card** at the top of Settings: plan name + % used, the reset caption, a usage
 *  meter, a hairline, and the Upgrade action. */
@Composable
private fun PlanCard(
    plan: String,
    usage: String,
    resetText: String,
    fraction: Float,
    onUpgrade: () -> Unit,
) {
    val colors = RemColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RemRadius.xlarge))
            .background(colors.backgroundSecondary)
            .padding(RemSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = plan,
                style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.labelPrimary,
            )
            Box(Modifier.weight(1f))
            Text(text = usage, style = RemTypography.subheadline, color = colors.labelSecondary)
        }
        Text(text = resetText, style = RemTypography.footnote, color = colors.labelSecondary)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(colors.fillTertiary),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(colors.brandBlue),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.separator))
        Text(
            text = "Upgrade",
            style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.brandBlue,
            modifier = Modifier.clickable(onClick = onUpgrade),
        )
    }
}

/** The reference Settings IA — plan card + grouped nav rows. Shared by the preview and the Paparazzi
 *  evidence so CI renders exactly what the app should show. */
@Composable
internal fun ColumnScope.SettingsReferenceContent() {
    PlanCard(
        plan = "Free plan",
        usage = "0% used",
        resetText = "Weekly limit resets on Oct 8",
        fraction = 0f,
        onUpgrade = {},
    )
    RemSection {
        SettingsNavRow("Connectors", Icons.Filled.GridView)
        RowDivider()
        SettingsNavRow("Devices", Icons.Filled.Devices)
        RowDivider()
        SettingsNavRow("Wallet", Icons.Filled.AccountBalanceWallet)
        RowDivider()
        SettingsNavRow("Secure credentials store", Icons.Filled.Shield)
        RowDivider()
        SettingsNavRow("Permissions", Icons.Filled.PanTool)
        RowDivider()
        SettingsNavRow("Messaging channels", Icons.AutoMirrored.Filled.Chat)
    }
    RemSection {
        SettingsNavRow("Notifications", Icons.Filled.Notifications)
        RowDivider()
        SettingsNavRow("Appearance", Icons.Filled.Brush)
    }
}

@Composable
private fun SettingsNavRow(title: String, icon: ImageVector) {
    ListRow(
        title = title,
        onClick = {},
        leading = { NavIcon(icon) },
        trailing = { DisclosureChevron() },
    )
}

@Preview(name = "SettingsScreen", showBackground = true, widthDp = 402, heightDp = 1000)
@Composable
private fun SettingsScreenPreview() {
    RemTheme { SettingsScreen { SettingsReferenceContent() } }
}
