package com.rem.designsystem.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.brand.RemFaceMark
import com.rem.designsystem.brand.RemFaceMarkMode
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/** Drives the [ChatHeader] status dot: green when connected, orange when Rem needs the person. */
enum class ChatHeaderStatus { Connected, NeedsYou }

/**
 * **ChatHeader** — Compose sibling of the SwiftUI `ChatHeader`: the Rem face in a 64dp avatar and,
 * beneath it, the identity pill — name, status dot and the agent's **current activity**, with a
 * chevron. The header **owns** back and overflow (Figma corrections, 2026-10-10): pass [onBack] /
 * [onOverflow] and it draws 44dp circular controls level with the avatar, stretched to the screen width
 * (verified at 320 and 402). Hosts must not add another title row. Without the callbacks the controls
 * are omitted (catalog use). Activity text is host data for the agent's lifecycle, not transport.
 *
 * Figma: **Rem/Chat v2/Header** (`2054:19725`). Compose has no material blur, so the glass pill uses
 * `backgroundPrimary` with the Figma shadow.
 */
@Composable
fun ChatHeader(
    activity: String,
    modifier: Modifier = Modifier,
    name: String = "Rem",
    status: ChatHeaderStatus = ChatHeaderStatus.Connected,
    faceMode: RemFaceMarkMode = RemFaceMarkMode.Idle,
    accessibilityPrefix: String = "chatHeader",
    onTap: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onOverflow: (() -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        HeaderIdentity(name, activity, status, faceMode, accessibilityPrefix, onTap)
        if (onBack != null || onOverflow != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RemSpacing.lg)
                    .padding(top = (ChatHeaderAvatarSize - ChatHeaderControlSize) / 2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    HeaderControl(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back", "$accessibilityPrefix.back", onBack)
                }
                Spacer(modifier = Modifier.weight(1f))
                if (onOverflow != null) {
                    HeaderControl(Icons.Filled.MoreHoriz, "More", "$accessibilityPrefix.overflow", onOverflow)
                }
            }
        }
    }
}

/** Header-owned control and avatar sizes; the control's centre sits on the avatar's centre. */
val ChatHeaderControlSize = 44.dp
val ChatHeaderAvatarSize = 64.dp

@Composable
private fun HeaderControl(icon: ImageVector, label: String, tag: String, onClick: () -> Unit) {
    val colors = RemColors.current
    Box(
        modifier = Modifier
            .size(ChatHeaderControlSize)
            .shadow(12.dp, CircleShape, clip = false)
            .background(colors.backgroundPrimary, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.labelPrimary, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun HeaderIdentity(
    name: String,
    activity: String,
    status: ChatHeaderStatus,
    faceMode: RemFaceMarkMode,
    accessibilityPrefix: String,
    onTap: (() -> Unit)?,
) {
    val colors = RemColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(ChatHeaderAvatarSize)
                .background(colors.backgroundSecondary, CircleShape)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            RemFaceMark(mode = faceMode, tint = colors.brandBlue, size = 48.dp)
        }
        Column(
            modifier = Modifier
                .widthIn(min = 138.dp)
                .shadow(16.dp, RoundedCornerShape(30.dp), clip = false)
                .background(colors.backgroundPrimary, RoundedCornerShape(30.dp))
                .then(if (onTap != null) Modifier.clickable(role = Role.Button, onClick = onTap) else Modifier)
                .semantics(mergeDescendants = true) { contentDescription = "$name, $activity" }
                .testTag("$accessibilityPrefix.identity")
                .padding(horizontal = 22.dp, vertical = RemSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(name, style = RemTypography.title3.copy(fontWeight = FontWeight.SemiBold), color = colors.labelPrimary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (status == ChatHeaderStatus.Connected) colors.systemGreen else colors.systemOrange, CircleShape),
                )
                Text(activity, style = RemTypography.footnote, color = colors.labelSecondary, maxLines = 1)
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.labelSecondary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Preview(name = "ChatHeader", showBackground = true, widthDp = 402)
@Composable
private fun ChatHeaderPreview() {
    RemTheme {
        Column(
            modifier = Modifier.background(RemColors.current.backgroundPrimary).padding(RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.xl),
        ) {
            ChatHeader(activity = "Connected")
            ChatHeader(activity = "Reading the shared notes", faceMode = RemFaceMarkMode.Thinking)
            ChatHeader(activity = "Needs you", status = ChatHeaderStatus.NeedsYou)
        }
    }
}
