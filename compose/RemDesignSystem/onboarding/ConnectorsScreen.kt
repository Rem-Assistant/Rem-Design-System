package com.rem.designsystem.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.primitives.ContainedIconSize
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * The **Connectors** onboarding step — Compose sibling of the SwiftUI `OnboardingConnectorsTemplate`.
 * This is the onboarding treatment (hero lockup → grouped card on a white flip background → Continue /
 * Skip), consistent with the Consent step — NOT the Settings connectors list. Authority:
 * `tasks/refs/onboarding/03-connectors.png`. Rows use a **toggle** (connected) or a **Connect** button
 * (available); the "See more" row expands the list.
 */
data class Connector(
    val icon: ImageVector,
    val tint: Color,
    val name: String,
    val status: String,
    val isConnected: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun OnboardingConnectorsScreen(
    connectors: List<Connector>,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    heroIcon: ImageVector = Icons.Filled.Link,
    title: String = "Connectors",
    message: String = "Connect Rem to the tools you use so it can keep you up to date and surface what needs doing.",
    showSeeMore: Boolean = true,
    onSeeMore: () -> Unit = {},
) {
    val colors = RemColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.backgroundPrimary)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(RemSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RemSpacing.lg),
        ) {
            Spacer(Modifier.height(RemSpacing.xxl))
            ContainedIcon(icon = heroIcon, fill = ContainedIconFill.Tint(colors.brandBlue), size = ContainedIconSize.Large)
            Text(text = title, style = RemTypography.largeTitle, color = colors.labelPrimary, textAlign = TextAlign.Center)
            Text(text = message, style = RemTypography.body, color = colors.labelSecondary, textAlign = TextAlign.Center)
            RemSection(modifier = Modifier.fillMaxWidth()) {
                connectors.forEach { c ->
                    ConnectorRow(c)
                    Divider()
                }
                if (showSeeMore) {
                    TextButton(onClick = onSeeMore, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = colors.brandBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(RemSpacing.sm))
                        Text("See more", style = RemTypography.body, color = colors.brandBlue)
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(RemSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                // Match the iOS primary action button (RemButtonStyle.primary = rounded-rect at the
                // `medium` radius), not Material's default pill, so Continue is identical across platforms.
                shape = RoundedCornerShape(RemRadius.medium),
                colors = ButtonDefaults.buttonColors(containerColor = colors.buttonBackground, contentColor = colors.backgroundPrimary),
            ) { Text("Continue", style = RemTypography.bodyBold) }
            TextButton(onClick = onSkip) { Text("Skip", style = RemTypography.bodyBold, color = colors.brandBlue) }
        }
    }
}

@Composable
private fun ConnectorRow(c: Connector) {
    val colors = RemColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = RemSpacing.md, vertical = RemSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContainedIcon(icon = c.icon, fill = ContainedIconFill.Tint(c.tint), size = ContainedIconSize.Small)
        Spacer(Modifier.width(RemSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = c.name, style = RemTypography.bodyBold, color = colors.labelPrimary)
            Spacer(Modifier.height(3.dp))
            Text(text = c.status, style = RemTypography.caption1, color = colors.labelSecondary)
        }
        Spacer(Modifier.width(RemSpacing.sm))
        if (c.isConnected) {
            // iOS-green track (matches the SwiftUI `Toggle` .tint(systemGreen)) instead of Material's
            // default purple.
            Switch(
                checked = true,
                onCheckedChange = { c.onClick() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = colors.systemGreen,
                    checkedBorderColor = colors.systemGreen,
                ),
            )
        } else {
            // Capsule pill, matching the iOS `.remButton(.pillSecondary)` Connect affordance.
            Button(
                onClick = c.onClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = colors.fillTertiary, contentColor = colors.brandBlue),
            ) { Text("Connect", style = RemTypography.bodyBold) }
        }
    }
}

@Composable
private fun Divider() {
    val colors = RemColors.current
    Box(modifier = Modifier.padding(start = RemSpacing.xxl + RemSpacing.md).fillMaxWidth().height(1.dp).background(colors.separator))
}

@Preview(name = "Connectors · light", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun ConnectorsLightPreview() {
    RemTheme {
        OnboardingConnectorsScreen(
            connectors = listOf(
                Connector(Icons.Filled.Email, Color(0xFFEA4335), "Gmail", "Connected", true) {},
                Connector(Icons.Filled.DateRange, Color(0xFF1A73E8), "Google Calendar", "Not connected", false) {},
                Connector(Icons.Filled.Notifications, Color(0xFF6B4FBB), "Slack", "Not connected", false) {},
            ),
            onContinue = {}, onSkip = {},
        )
    }
}
