package com.rem.designsystem

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.rem.designsystem.screens.AgentActivityScreen
import com.rem.designsystem.screens.AgentActivityTab
import com.rem.designsystem.screens.ChatPlaygroundFixture
import com.rem.designsystem.screens.ChatScreen
import com.rem.designsystem.screens.ChatTranscriptList
import com.rem.designsystem.screens.InboxAction
import com.rem.designsystem.screens.InboxPlaygroundFixture
import com.rem.designsystem.screens.InboxScreen
import com.rem.designsystem.tokens.RemTheme
import org.junit.Rule
import org.junit.Test

/**
 * Full-screen Chat composition evidence (board `2681:21977`) — twin of the SwiftUI `ChatComposition-*`
 * renders: default, empty, task reply and Inbox states at a 402dp-wide device, plus the 320dp width
 * stress fixture. Screenshot EVIDENCE, not a golden gate. Run with `recordPaparazziDebug`.
 */
class ChatCompositionSnapshots {
    /** 402 × 874dp at the Pixel 6 density (2.625): the canonical shell size. */
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6.copy(screenWidth = 1055, screenHeight = 2294))

    private fun shot(name: String, content: @Composable () -> Unit) = paparazzi.snapshot(name = name, composable = content)

    @Composable
    private fun Composition(fixture: ChatPlaygroundFixture) = RemTheme {
        ChatScreen(
            header = fixture.header,
            composer = fixture.composer.state,
            onAction = {},
            replyContext = fixture.replyContext,
            emptyState = fixture.emptyState,
        ) {
            ChatTranscriptList(fixture.entries, onAction = {})
        }
    }

    @Test fun chatCompositionDefault() = shot("ChatComposition-default-light") {
        Composition(ChatPlaygroundFixture.conversation(ChatPlaygroundFixture.Conversation.Populated))
    }

    @Test fun chatCompositionEmpty() = shot("ChatComposition-empty-light") {
        Composition(ChatPlaygroundFixture.conversation(ChatPlaygroundFixture.Conversation.Empty))
    }

    @Test fun chatCompositionTaskReply() = shot("ChatComposition-taskReply-light") {
        Composition(InboxPlaygroundFixture().route(InboxAction.Open("plan-next-step"))!!)
    }

    @Test fun inboxStates() = shot("InboxStates-light") {
        RemTheme { InboxScreen(items = InboxPlaygroundFixture().items, onAction = {}) {} }
    }

    /** Agent activity (`2002:76914`), opened from the Chat header; twin of the SwiftUI `AgentActivity-*`. */
    @Composable
    private fun Activity(tab: AgentActivityTab) = RemTheme {
        AgentActivityScreen(display = ChatPlaygroundFixture().activity.display, selectedTab = tab, onSelectTab = {}, onBack = {})
    }

    @Test fun agentActivity() = shot("AgentActivity-activity-light") { Activity(AgentActivityTab.Activity) }

    @Test fun agentActivityApprovals() = shot("AgentActivity-approvals-light") { Activity(AgentActivityTab.Approvals) }
}

/** The 320dp-wide stress fixture (not a device safe-area model). */
class ChatCompositionNarrowSnapshots {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6.copy(screenWidth = 840, screenHeight = 2294))

    @Test fun chatCompositionNarrow() = paparazzi.snapshot(name = "ChatComposition-narrow-light") {
        val fixture = ChatPlaygroundFixture.conversation(ChatPlaygroundFixture.Conversation.Populated)
        RemTheme {
            ChatScreen(header = fixture.header, composer = fixture.composer.state, onAction = {}) {
                ChatTranscriptList(fixture.entries, onAction = {})
            }
        }
    }

    @Test fun chatCompositionTaskReplyNarrow() = paparazzi.snapshot(name = "ChatComposition-taskReply-narrow-light") {
        val fixture = InboxPlaygroundFixture().route(InboxAction.Open("plan-next-step"))!!
        RemTheme {
            ChatScreen(header = fixture.header, composer = fixture.composer.state, onAction = {}, replyContext = fixture.replyContext) {
                ChatTranscriptList(fixture.entries, onAction = {})
            }
        }
    }
}
