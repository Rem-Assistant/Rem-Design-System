package com.rem.designsystem.chat

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
 */
enum class ComposerSendState { Idle, Active, Sending }

@Composable
fun RemComposerBar(
    modifier: Modifier = Modifier,
    text: String = "",
    placeholder: String = "Ask anything",
    model: String = "Auto",
    state: ComposerSendState = ComposerSendState.Idle,
    showAttachments: Boolean = false,
) {
    val colors = RemColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.backgroundSecondary, RoundedCornerShape(30.dp))
            .padding(RemSpacing.md),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        if (showAttachments) {
            AttachmentsStrip()
        }

        Text(
            text = text.ifEmpty { placeholder },
            style = RemTypography.chatMessage,
            color = if (text.isEmpty()) colors.labelTertiary else colors.labelPrimary,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add", tint = colors.labelSecondary, modifier = Modifier.size(20.dp))
            // Model selector
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = model, style = RemTypography.subheadline, color = colors.labelSecondary)
                Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = colors.labelTertiary, modifier = Modifier.size(14.dp))
            }
            Box(modifier = Modifier.weight(1f))
            SpeakPill()
            SendButton(state)
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
private fun AttachmentsStrip() {
    val colors = RemColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        // Cloud browser chip
        Row(
            modifier = Modifier
                .background(colors.backgroundPrimary, CircleShape)
                .padding(horizontal = RemSpacing.sm, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs),
        ) {
            Icon(Icons.Filled.Public, contentDescription = null, tint = colors.brandBlue, modifier = Modifier.size(12.dp))
            Text(text = "Cloud browser", style = RemTypography.caption1, color = colors.labelPrimary)
            Icon(Icons.Filled.Close, contentDescription = "Remove", tint = colors.labelTertiary, modifier = Modifier.size(10.dp))
        }
        // Image thumbnail with remove affordance
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(colors.systemBlue.copy(alpha = 0.35f), RoundedCornerShape(RemRadius.medium)),
            )
            Icon(
                Icons.Filled.Cancel,
                contentDescription = "Remove image",
                tint = colors.labelPrimary,
                modifier = Modifier.size(15.dp),
            )
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
