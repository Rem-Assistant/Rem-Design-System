package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.rows.DisclosureChevron
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * **SettingsScreen** — Compose sibling of the SwiftUI [SettingsScreen]. A large "Settings" title over
 * grouped, inset [RemSection] cards: a profile row first (avatar + name/email), then grouped [ListRow]s
 * with [ContainedIcon] leadings and chevron / toggle / value trailings, supplied by the host through
 * [content]. Chrome comes from the OS. A single-row section draws no divider; multi-row sections carry
 * inset separators between rows.
 *
 * Authority: `Screen/Settings` (Figma `130:44`, page "Settings") + `SharedSettingsView.swift`.
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

/** Canonical inset row separator (60dp = xxl + md leading inset), matching the house Divider used by
 *  the onboarding rows and the Swift `RemSection` separator. */
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

/** Trailing value label + disclosure chevron — the "value" accessory (e.g. Voice -> "Aria"). */
@Composable
private fun ValueDisclosure(value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs),
    ) {
        Text(text = value, style = RemTypography.body, color = RemColors.current.labelSecondary)
        DisclosureChevron()
    }
}

/** Circular profile avatar — no canonical Avatar component yet, so it lives with the preview. */
@Composable
private fun ProfileAvatar() {
    val colors = RemColors.current
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.fillTertiary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            tint = colors.labelSecondary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Preview(name = "SettingsScreen", showBackground = true, widthDp = 402, heightDp = 900)
@Composable
private fun SettingsScreenPreview() {
    RemTheme {
        val colors = RemColors.current
        var proactiveSuggestions by remember { mutableStateOf(true) }

        SettingsScreen {
            // Profile — single-row section, no divider.
            RemSection {
                ListRow(
                    title = "Avery Diaz",
                    subtitle = "avery@example.com",
                    leading = { ProfileAvatar() },
                )
            }

            // Core surfaces — multi-row section, so it carries inset separators between rows.
            RemSection(header = "General") {
                ListRow(
                    title = "Connectors",
                    subtitle = "3 connected",
                    leading = { ContainedIcon(Icons.Filled.Cable, fill = ContainedIconFill.Tint(colors.systemIndigo)) },
                    trailing = { DisclosureChevron() },
                )
                RowDivider()
                ListRow(
                    title = "Voice",
                    leading = { ContainedIcon(Icons.Filled.Mic, fill = ContainedIconFill.Tint(colors.systemOrange)) },
                    trailing = { ValueDisclosure("Aria") },
                )
                RowDivider()
                ListRow(
                    title = "General",
                    leading = { ContainedIcon(Icons.Filled.Settings, fill = ContainedIconFill.Tint(colors.systemBlue)) },
                    trailing = { DisclosureChevron() },
                )
            }

            // A toggle row — single-row section, no divider. iOS-green Switch per the design system.
            RemSection(
                header = "Notifications",
                footer = "Rem surfaces new suggestions as they're ready.",
            ) {
                ListRow(
                    title = "Proactive suggestions",
                    leading = { ContainedIcon(Icons.Filled.Notifications, fill = ContainedIconFill.Tint(colors.systemRed)) },
                    trailing = {
                        Switch(
                            checked = proactiveSuggestions,
                            onCheckedChange = { proactiveSuggestions = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.systemGreen,
                                checkedBorderColor = colors.systemGreen,
                            ),
                        )
                    },
                )
            }
        }
    }
}
