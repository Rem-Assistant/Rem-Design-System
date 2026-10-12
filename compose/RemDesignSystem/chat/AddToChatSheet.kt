package com.rem.designsystem.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.buttons.RemButton
import com.rem.designsystem.buttons.RemButtonSize
import com.rem.designsystem.buttons.RemButtonVariant
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/** Reasoning effort for the next message — Compose sibling of the SwiftUI `ThinkingLevel`. */
enum class ThinkingLevel(val title: String) { Off("Off"), Low("Low"), Medium("Medium"), High("High") }

/** The most photos one message may carry; hosts pass it to the system photo picker. */
const val AddToChatMaxPhotoSelection = 4

/**
 * **AddToChatSheet** — Compose sibling of the SwiftUI `AddToChatSheet`: the composer `+` sheet body —
 * Camera / Photos / Files tiles, the conditional Cloud browser row, and the Thinking menu, under an
 * "Add to Chat" title with Done. Hosts wrap it in their bottom sheet and own the real pickers:
 * Photos → system photo picker (images only, at most [AddToChatMaxPhotoSelection]); Files → document
 * picker for image types. The shipped Android app has no camera flow, so Android hosts pass
 * `showsCamera = false`. Cloud browser adds a removable capability chip and dismisses (host does both).
 *
 * Figma: **Rem/Chat/Add to Chat** (`2656:21214`), Thinking menu (`2660:128572`).
 */
@Composable
fun AddToChatSheet(
    showsCamera: Boolean,
    browserAvailable: Boolean,
    thinking: ThinkingLevel,
    onThinkingChange: (ThinkingLevel) -> Unit,
    onPhotos: () -> Unit,
    onFiles: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    accessibilityPrefix: String = "addToChat",
    onCamera: () -> Unit = {},
    onCloudBrowser: () -> Unit = {},
) {
    val colors = RemColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(RemSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.lg),
    ) {
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
            Text(
                "Add to Chat",
                style = RemTypography.bodyBold,
                color = colors.labelPrimary,
                modifier = Modifier.semantics { heading() },
            )
            RemButton(
                text = "Done",
                onClick = onDone,
                variant = RemButtonVariant.TextAccent,
                size = RemButtonSize.Compact,
                modifier = Modifier.align(Alignment.CenterEnd).testTag("$accessibilityPrefix.done"),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(RemSpacing.md), modifier = Modifier.fillMaxWidth()) {
            if (showsCamera) Tile("Camera", Icons.Outlined.PhotoCamera, "$accessibilityPrefix.camera", onCamera)
            Tile("Photos", Icons.Outlined.PhotoLibrary, "$accessibilityPrefix.photos", onPhotos)
            Tile("Files", Icons.Outlined.Folder, "$accessibilityPrefix.files", onFiles)
        }
        if (browserAvailable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.fillTertiary, RoundedCornerShape(RemRadius.medium))
                    .clickable(role = Role.Button, onClick = onCloudBrowser)
                    .padding(RemSpacing.md)
                    .testTag("$accessibilityPrefix.cloudBrowser"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
            ) {
                Icon(Icons.Outlined.Public, contentDescription = null, tint = colors.labelPrimary, modifier = Modifier.size(22.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Cloud browser", style = RemTypography.body, color = colors.labelPrimary)
                    Text("Add Cloud browser to your message.", style = RemTypography.caption1, color = colors.labelSecondary)
                }
            }
        }
        ThinkingRow(thinking, onThinkingChange, accessibilityPrefix)
    }
}

@Composable
private fun RowScope.Tile(title: String, icon: ImageVector, tag: String, onClick: () -> Unit) {
    val colors = RemColors.current
    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 78.dp)
            .background(colors.fillTertiary, RoundedCornerShape(RemRadius.medium))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = title }
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RemSpacing.xs, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = null, tint = colors.labelPrimary, modifier = Modifier.size(22.dp))
        Text(title, style = RemTypography.caption1, color = colors.labelPrimary)
    }
}

@Composable
private fun ThinkingRow(thinking: ThinkingLevel, onChange: (ThinkingLevel) -> Unit, prefix: String) {
    val colors = RemColors.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.fillTertiary, RoundedCornerShape(RemRadius.medium))
                .clickable(role = Role.Button, onClickLabel = "Choose thinking level") { expanded = true }
                .padding(RemSpacing.md)
                .testTag("$prefix.thinking"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
        ) {
            Icon(Icons.Outlined.Psychology, contentDescription = null, tint = colors.labelPrimary, modifier = Modifier.size(22.dp))
            Text("Thinking", style = RemTypography.body, color = colors.labelPrimary, modifier = Modifier.weight(1f))
            Text(thinking.title, style = RemTypography.body, color = colors.labelSecondary)
            Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ThinkingLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = { Text(level.title) },
                    trailingIcon = { if (level == thinking) Icon(Icons.Filled.Check, contentDescription = "Selected") },
                    onClick = { onChange(level); expanded = false },
                    modifier = Modifier.testTag("$prefix.thinking.${level.name.lowercase()}"),
                )
            }
        }
    }
}

@Preview(name = "AddToChatSheet", showBackground = true, widthDp = 402)
@Composable
private fun AddToChatSheetPreview() {
    RemTheme {
        AddToChatSheet(
            showsCamera = false, browserAvailable = true, thinking = ThinkingLevel.Medium, onThinkingChange = {},
            onPhotos = {}, onFiles = {}, onDone = {},
            modifier = Modifier.background(RemColors.current.backgroundPrimary),
        )
    }
}
