package com.rem.designsystem.demo

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick

/**
 * Build 9: the Playground opens on the app shell (a full-screen conversation). The catalog, fixture
 * pickers and screen journeys live in the Developer hub, reached through the shell's ordinary route —
 * header sidebar button → account pill → Settings → Developer (`docs/playground/route-map.md`).
 */
internal fun ComposeTestRule.openDeveloperHub() {
    waitForTag("chat.header.menu")
    onNodeWithTag("chat.header.menu").performClick()
    onNodeWithTag("sidebar.account").performClick()
    waitForTag("settings.developer")
    onNodeWithTag("settings.developer").performClick()
    waitForTag("openComponents")
}

internal fun ComposeTestRule.waitForTag(tag: String, timeoutMillis: Long = 5000) {
    waitUntil(timeoutMillis) { onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() }
}
