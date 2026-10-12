package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.primitives.ContainedIconSize
import com.rem.designsystem.rows.DisclosureChevron
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.rows.ListRowLabel
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemTypography

/**
 * Settings Permissions `1827:50821`, reached from Settings → Permissions. Three grouped sections of
 * Subtle-icon rows with a status badge and disclosure, plus the source footers. Device permissions
 * belong to the operating system and the design system has no permission bridge or open-Settings
 * pattern, so statuses are illustrative and each row explains that boundary rather than prompting or
 * leaving the app. Leading glyphs are native Material translations of the source SF Symbols.
 * SwiftUI sibling: `Screens/SettingsPermissionsScreen.swift`.
 */
@Composable
fun SettingsPermissionsScreen(onBack: () -> Unit) {
    var boundary by rememberSaveable { mutableStateOf<String?>(null) }
    SettingsPageScaffold("Permissions", "settingsPermissions", onBack,
        contentModifier = Modifier.semantics { contentDescription = PlaygroundMockData.hint }) {
        SettingsPermissionsFixture.sections.forEach { section ->
            RemSection(header = section.header, footer = section.footer, settingsHeader = true) {
                section.permissions.forEachIndexed { index, permission ->
                    ListRow(
                        modifier = Modifier.testTag("permissions.${permission.id}")
                            .semantics { stateDescription = permission.status.title },
                        showsDivider = index < section.permissions.lastIndex,
                        onClick = { boundary = SettingsPermissionsFixture.boundary(permission) },
                        leading = {
                            ContainedIcon(permission.glyph.icon, modifier = Modifier.clearAndSetSemantics {},
                                fill = ContainedIconFill.Subtle, size = ContainedIconSize.Settings)
                        },
                        content = { ListRowLabel(permission.title) },
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                PermissionStatusBadge(permission.status)
                                DisclosureChevron()
                            }
                        },
                    )
                }
            }
        }
    }
    SettingsPrototypeBoundary(boundary) { boundary = null }
}

private val SettingsPermissionGlyph.icon: ImageVector
    get() = when (this) {
        SettingsPermissionGlyph.Notifications -> Icons.Filled.Notifications
        SettingsPermissionGlyph.Calendar -> Icons.Filled.CalendarMonth
        SettingsPermissionGlyph.Reminders -> Icons.AutoMirrored.Filled.FormatListBulleted
        SettingsPermissionGlyph.Microphone -> Icons.Filled.Mic
        SettingsPermissionGlyph.SpeechRecognition -> Icons.Filled.GraphicEq
        SettingsPermissionGlyph.Camera -> Icons.Filled.PhotoCamera
    }

/** PermissionStatusBadge `383:2`: an 8dp status dot and the secondary status label. */
@Composable
private fun PermissionStatusBadge(status: SettingsPermissionStatus) {
    val colors = RemColors.current
    val dot = when (status) {
        SettingsPermissionStatus.Enabled -> colors.systemGreen
        SettingsPermissionStatus.NotSet -> colors.labelTertiary
        SettingsPermissionStatus.Denied -> colors.systemRed
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(dot, CircleShape))
        Text(status.title, style = RemTypography.body, color = colors.labelSecondary)
    }
}
