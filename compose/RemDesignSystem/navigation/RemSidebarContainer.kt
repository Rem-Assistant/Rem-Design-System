package com.rem.designsystem.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors

/**
 * **RemSidebarContainer** — the Claude-style push (Compose twin of `Navigation/RemSidebarContainer.swift`).
 * Closed, the chat fills the screen (`2702:2351`); open, the chat slides right as a rounded, shadowed
 * card leaving [RemSidebarLayout.VisibleContentPeek] visible, and tapping that card closes the sidebar
 * (`2702:2479`). [isOpen] is host-owned; [onOpenChange] reports the tap-to-close.
 *
 * Accessibility (twin of iOS `.accessibilityHidden`): only the visible surface is in the semantics tree.
 * Open, the covered chat is cleared (its header, transcript and composer cannot be reached or activated by
 * TalkBack or UI automation) and only the card's "Close sidebar" button represents it; closed, the
 * off-screen sidebar's whole subtree is cleared. Contract: `docs/contracts/sidebar-new-agent.md`.
 */
@Composable
fun RemSidebarContainer(
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    sidebar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = RemColors.current
    BoxWithConstraints(modifier.fillMaxSize().background(colors.backgroundSecondary)) {
        val containerWidth = maxWidth.value
        val sidebarWidth = RemSidebarLayout.sidebarWidth(containerWidth).dp
        val offset by animateDpAsState(RemSidebarLayout.contentOffset(isOpen, containerWidth).dp, label = "sidebarOffset")
        val radius = if (isOpen) RemSidebarLayout.ContentCornerRadius.dp else 0.dp
        val shape = RoundedCornerShape(radius)

        Box(
            Modifier
                .width(sidebarWidth)
                .fillMaxHeight()
                .then(if (isOpen) Modifier else Modifier.clearAndSetSemantics {}),
        ) { sidebar() }

        Box(
            Modifier
                .offset(x = offset)
                .width(maxWidth)
                .fillMaxHeight()
                .shadow(if (isOpen) 16.dp else 0.dp, shape, ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.12f))
                .clip(shape)
                .background(colors.backgroundPrimary),
        ) {
            // The same wrapper open and closed, so toggling only swaps its semantics and content keeps its state.
            Box(Modifier.fillMaxSize().then(if (isOpen) Modifier.clearAndSetSemantics {} else Modifier)) { content() }
            if (isOpen) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onOpenChange(false) }
                        .semantics {
                            role = Role.Button
                            contentDescription = "Close sidebar"
                        }
                        .testTag("sidebar.close"),
                )
            }
        }
    }
}
