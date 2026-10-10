package com.rem.designsystem.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * **RemComposerBar** — Compose sibling of the SwiftUI [RemComposerBar]. The chat / comment composer: a
 * text field in a rounded, borderless pill with a bottom control row `[+] · model · field · [Speak] ·
 * send`, and an optional attachments strip. No border — the material pill is the whole shape. Authority:
 * Figma **RemComposerBar** (`53:2`) + the 3-state doc (`527:2`) and `RemComposerBar.swift`.
 *
 * (The shipping SwiftUI pill uses `.ultraThinMaterial`; Compose has no material blur, so both DS
 * renders use `backgroundSecondary` — the flat grey the Figma pill shows.)
 *
 * Chat slice (Figma **Composer** `2071:11555`): the model control is the secondary-pill **Auto**
 * trigger. Pass [modelMenu] (typically a [ChatModelMenu]) to make it open the runtime-supplied model
 * menu; its `enabled` argument is false while sending (45%), when Speak is also hidden. [attachments]
 * are removable chips owned by the host — a Cloud browser chip is a capability for the next message.
 */
enum class ComposerSendState { Idle, Active, Sending }

/** One item attached to the next message, rendered as a removable chip in [RemComposerBar]. */
data class ComposerAttachment(val id: String, val title: String, val kind: Kind) {
    enum class Kind {
        /** A capability for the next turn (e.g. Cloud browser) — not content, not an immediate launch. */
        Capability,
        Image,
        File,
    }

    companion object {
        val CloudBrowser = ComposerAttachment("cloud-browser", "Cloud browser", Kind.Capability)
    }
}

@Composable
fun RemComposerBar(
    modifier: Modifier = Modifier,
    text: String = "",
    placeholder: String = "Ask anything",
    model: String = "Auto",
    state: ComposerSendState = ComposerSendState.Idle,
    showAttachments: Boolean = false,
    showModel: Boolean = true,
    showSpeak: Boolean = true,
    onTextChange: ((String) -> Unit)? = null,
    onSend: (() -> Unit)? = null,
    onAdd: (() -> Unit)? = null,
    accessibilityPrefix: String = "composer",
    attachments: List<ComposerAttachment> = emptyList(),
    onRemoveAttachment: ((ComposerAttachment) -> Unit)? = null,
    modelMenu: (@Composable (enabled: Boolean) -> Unit)? = null,
) {
    val colors = RemColors.current
    val effectiveState = if (onTextChange != null && state != ComposerSendState.Sending) {
        if (text.isBlank()) ComposerSendState.Idle else ComposerSendState.Active
    } else state
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.backgroundSecondary, RoundedCornerShape(30.dp))
            .padding(RemSpacing.md),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        // `showAttachments` is the legacy display flag: it shows the canonical Cloud browser chip.
        val chips = if (attachments.isEmpty() && showAttachments) listOf(ComposerAttachment.CloudBrowser) else attachments
        if (chips.isNotEmpty()) {
            AttachmentsStrip(chips, accessibilityPrefix, onRemoveAttachment)
        }

        if (onTextChange != null) {
            Box {
                if (text.isEmpty()) Text(placeholder, style = RemTypography.chatMessage, color = colors.labelTertiary)
                BasicTextField(
                    value = text, onValueChange = onTextChange,
                    textStyle = RemTypography.chatMessage.copy(color = colors.labelPrimary),
                    cursorBrush = SolidColor(colors.brandBlue),
                    modifier = Modifier.fillMaxWidth().testTag("$accessibilityPrefix.composerField"),
                )
            }
        } else {
            Text(
                text = text.ifEmpty { placeholder },
                style = RemTypography.chatMessage,
                color = if (text.isEmpty()) colors.labelTertiary else colors.labelPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
        ) {
            if (onAdd != null) {
                IconButton(onClick = onAdd, modifier = Modifier.testTag("$accessibilityPrefix.composerAdd")) {
                    Icon(Icons.Filled.Add, contentDescription = "Add", tint = colors.labelSecondary, modifier = Modifier.size(20.dp))
                }
            } else {
                Icon(Icons.Filled.Add, contentDescription = "Add", tint = colors.labelSecondary, modifier = Modifier.size(20.dp))
            }
            val modelEnabled = effectiveState != ComposerSendState.Sending
            if (showModel) {
                if (modelMenu != null) modelMenu(modelEnabled) else ChatModelTriggerPill(label = model, enabled = modelEnabled)
            }
            Box(modifier = Modifier.weight(1f))
            if (showSpeak && effectiveState != ComposerSendState.Sending) SpeakPill()
            if (onSend != null) {
                IconButton(onClick = onSend, enabled = effectiveState != ComposerSendState.Idle,
                    modifier = Modifier.testTag("$accessibilityPrefix.composerSend")) {
                    SendButton(effectiveState)
                }
            } else { SendButton(effectiveState) }
        }
    }
}

@Composable
private fun SpeakPill() {
    val colors = RemColors.current
    Row(
        modifier = Modifier
            .background(colors.brandBlue, CircleShape)
            .padding(horizontal = RemSpacing.md, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs),
    ) {
        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = colors.labelOnColor, modifier = Modifier.size(15.dp))
        Text(text = "Speak", style = RemTypography.subheadline.copy(fontWeight = FontWeight.Bold), color = colors.labelOnColor)
    }
}

@Composable
private fun SendButton(state: ComposerSendState) {
    val colors = RemColors.current
    val fill = when (state) {
        ComposerSendState.Idle -> colors.fillTertiary
        ComposerSendState.Active -> colors.brandBlue
        ComposerSendState.Sending -> colors.systemRed
    }
    val fg = if (state == ComposerSendState.Idle) colors.labelSecondary else colors.labelOnColor
    Box(
        modifier = Modifier.size(32.dp).background(fill, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (state == ComposerSendState.Sending) Icons.Filled.Stop else Icons.Filled.ArrowUpward,
            contentDescription = if (state == ComposerSendState.Sending) "Stop" else "Send",
            tint = fg,
            modifier = Modifier.size(if (state == ComposerSendState.Sending) 15.dp else 18.dp),
        )
    }
}

@Composable
private fun AttachmentsStrip(
    attachments: List<ComposerAttachment>,
    prefix: String,
    onRemove: ((ComposerAttachment) -> Unit)?,
) {
    val colors = RemColors.current
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        attachments.forEach { attachment ->
            Row(
                modifier = Modifier
                    .background(colors.backgroundPrimary, RoundedCornerShape(RemRadius.small))
                    .padding(horizontal = RemSpacing.sm, vertical = RemSpacing.xs)
                    .testTag("$prefix.attachment.${attachment.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs),
            ) {
                Text(text = attachment.title, style = RemTypography.footnote, color = colors.labelPrimary, maxLines = 1)
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .then(
                            if (onRemove != null) Modifier
                                .clickable(role = Role.Button) { onRemove(attachment) }
                                .testTag("$prefix.removeAttachment.${attachment.id}")
                            else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = if (onRemove != null) "Remove ${attachment.title}" else null,
                        tint = colors.brandBlue,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

@Preview(name = "RemComposerBar", showBackground = true, widthDp = 420)
@Composable
private fun RemComposerBarPreview() {
    RemTheme {
        Column(
            modifier = Modifier.background(RemColors.current.backgroundPrimary).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            RemComposerBar()
            RemComposerBar(text = "Remind me to send the investor update tomorrow", state = ComposerSendState.Active, showAttachments = true)
            RemComposerBar(text = "Plan the rest of my day", state = ComposerSendState.Sending)
        }
    }
}
