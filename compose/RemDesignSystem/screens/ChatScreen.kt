package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rem.designsystem.chat.ComposerSendState
import com.rem.designsystem.chat.MessageBubble
import com.rem.designsystem.chat.MessageRole
import com.rem.designsystem.chat.RemComposerBar
import com.rem.designsystem.chat.VoiceBar
import com.rem.designsystem.chat.VoiceBarState
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme

/**
 * **ChatScreen** — Compose sibling of the SwiftUI [ChatScreen]. A scrolling transcript of
 * [MessageBubble]s, an optional [VoiceBar] above the input, and a [RemComposerBar] pinned at the
 * bottom, supplied by the host through [transcript]. Platform chrome comes from the OS. Authority:
 * Chat screen (Figma `71:533`, composer `527:2`) + `SharedRemChatView.swift`.
 */
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    composerText: String = "",
    composerPlaceholder: String = "Ask anything",
    composerState: ComposerSendState = ComposerSendState.Idle,
    voiceBar: VoiceBarState? = null,
    transcript: @Composable ColumnScope.() -> Unit,
) {
    val colors = RemColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.backgroundPrimary)) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.lg),
        ) {
            transcript()
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = RemSpacing.lg).padding(bottom = RemSpacing.md),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
        ) {
            if (voiceBar != null) {
                VoiceBar(state = voiceBar)
            }
            RemComposerBar(text = composerText, placeholder = composerPlaceholder, state = composerState)
        }
    }
}

@Preview(name = "ChatScreen", showBackground = true, widthDp = 402, heightDp = 760)
@Composable
private fun ChatScreenPreview() {
    RemTheme {
        ChatScreen(composerState = ComposerSendState.Idle) {
            MessageBubble("Can you tidy up my inbox before I start my day?", role = MessageRole.User)
            MessageBubble(
                "Done — I archived 38 newsletters and snoozed 5 low-priority threads. " +
                    "Want me to draft replies to the two that still need you?",
                role = MessageRole.Assistant,
            )
            MessageBubble("Yes, go ahead.", role = MessageRole.User)
        }
    }
}
