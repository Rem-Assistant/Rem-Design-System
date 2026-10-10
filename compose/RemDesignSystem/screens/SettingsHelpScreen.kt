package com.rem.designsystem.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.primitives.ContainedIconSize
import com.rem.designsystem.rows.DisclosureChevron
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.rows.ListRowLabel
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors

/**
 * Settings Help & Support `2014:68798`, reached from Settings → Help & Support. Send Feedback and
 * Report a Bug stay separate destinations; Shake to report is a local switch. The source marks the
 * forms, shake detection and submission as unsettled implementation decisions, so both rows state
 * that boundary and the footer says shake detection is not included. Leading icons use the approved
 * Settings Subtle amendment rather than the source's blue tint.
 * SwiftUI sibling: `Screens/SettingsHelpScreen.swift`.
 */
@Composable
fun SettingsHelpScreen(onBack: () -> Unit) {
    val colors = RemColors.current
    var boundary by rememberSaveable { mutableStateOf<String?>(null) }
    var shakeToReport by rememberSaveable { mutableStateOf(SettingsHelpFixture.shakeDefault) }
    SettingsPageScaffold("Help & Support", "settingsHelp", onBack) {
        RemSection {
            val destinations = SettingsHelpDestination.entries
            destinations.forEachIndexed { index, destination ->
                ListRow(
                    modifier = Modifier.testTag("help.${destination.id}"),
                    showsDivider = index < destinations.lastIndex,
                    onClick = { boundary = destination.boundary },
                    leading = {
                        ContainedIcon(
                            if (destination == SettingsHelpDestination.SendFeedback) Icons.Outlined.ChatBubbleOutline else Icons.Outlined.BugReport,
                            modifier = Modifier.clearAndSetSemantics {},
                            fill = ContainedIconFill.Subtle, size = ContainedIconSize.Settings,
                        )
                    },
                    content = { ListRowLabel(destination.title) },
                    trailing = { DisclosureChevron() },
                )
            }
        }
        RemSection(footer = SettingsHelpFixture.shakeFooter) {
            ListRow(
                leading = {},
                content = { ListRowLabel(SettingsHelpFixture.shakeTitle) },
                trailing = {
                    Switch(
                        checked = shakeToReport,
                        onCheckedChange = { shakeToReport = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.systemGreen,
                            checkedBorderColor = colors.systemGreen,
                        ),
                        modifier = Modifier.testTag("help.shakeToReport")
                            .semantics { contentDescription = SettingsHelpFixture.shakeTitle },
                    )
                },
            )
        }
    }
    SettingsPrototypeBoundary(boundary) { boundary = null }
}
