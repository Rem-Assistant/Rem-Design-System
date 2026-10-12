package com.rem.designsystem

import androidx.compose.runtime.Composable
import app.cash.paparazzi.Paparazzi
import com.rem.designsystem.navigation.RemSidebar
import com.rem.designsystem.navigation.RemSidebarContainer
import com.rem.designsystem.screens.ChatPlaygroundFixture
import com.rem.designsystem.screens.ChatScreen
import com.rem.designsystem.screens.ChatTranscriptList
import com.rem.designsystem.screens.PlaygroundShellFixture
import com.rem.designsystem.tokens.RemTheme

/**
 * Paparazzi evidence for the Playground app shell (Build 9, `docs/playground/route-map.md`): the root
 * conversation with the header sidebar button (`2702:2351`), and the sidebar pushed open over that same
 * conversation (`2702:2479`), light and dark. Pairs with `ShellRenderSnapshots.swift`. Run from
 * [EvidenceSnapshots] so the files carry its prefix.
 */
internal class ShellEvidence(private val paparazzi: Paparazzi) {

    private fun shot(name: String, content: @Composable () -> Unit) =
        paparazzi.snapshot(name = name, composable = content)

    fun chat() {
        shot("Shell-chat-light") { RemTheme { rootChat() } }
        shot("Shell-chat-dark") { RemTheme(darkTheme = true) { rootChat() } }
    }

    fun sidebar() {
        shot("Shell-sidebar-light") { RemTheme { openSidebar() } }
        shot("Shell-sidebar-dark") { RemTheme(darkTheme = true) { openSidebar() } }
    }

    @Composable
    private fun rootChat() {
        val fixture = ChatPlaygroundFixture.root()
        ChatScreen(header = fixture.header, composer = fixture.composer.state, onAction = {}) {
            ChatTranscriptList(fixture.entries, onAction = {})
        }
    }

    @Composable
    private fun openSidebar() {
        val shell = PlaygroundShellFixture().openSidebar()
        RemSidebarContainer(
            isOpen = shell.isSidebarOpen,
            onOpenChange = {},
            sidebar = { RemSidebar(shell.sidebar, onAction = {}) },
        ) { rootChat() }
    }
}
