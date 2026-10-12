package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.buttons.RemGlassIconButton
import com.rem.designsystem.buttons.RemGlassIconButtonDefaults
import com.rem.designsystem.primitives.RemGlass
import com.rem.designsystem.primitives.RemGlassState
import com.rem.designsystem.tokens.RemDarkColors
import com.rem.designsystem.tokens.RemTheme
import kotlin.math.roundToInt

/**
 * Jump to latest — twin of SwiftUI `ChatJumpToLatest`: the round down-arrow control the full Chat
 * composition shows, centred just above the docked composer, while the person is scrolled away from the
 * newest message. Tapping it scrolls (animated) to the latest message. The composition owns it; hosts wire
 * nothing. Contract: `docs/contracts/chat.md`.
 */
object ChatJumpToLatest {
    /**
     * Shown only when the transcript's bottom edge is more than this far below the visible area (dp). A
     * fixed distance (about four short rows) rather than a viewport fraction, so the rule reads the same
     * with the IME open or closed. Platform-convention choice marked for review.
     */
    const val THRESHOLD = 200f
    /** Circle diameter (dp): the 44 minimum touch target. */
    const val DIAMETER = 44f
    /** Down-arrow glyph size (dp): [RemGlassIconButtonDefaults.iconSize] of [DIAMETER]. */
    const val GLYPH_SIZE = 20f
    /** Gap (dp) between the button and the top of the docked composer chrome (`RemSpacing.md`). */
    const val BOTTOM_SPACING = 12f
    /** Appear / disappear: short fade plus scale from this factor. No animation at animator scale 0. */
    const val APPEAR_SCALE = 0.8f
    const val APPEAR_MILLIS = 200
    /**
     * Glass (API 32+, inside the Chat composition): `backgroundTertiary` tint over the blurred transcript, high
     * enough that the `labelPrimary` arrow stays legible over bubbles in light and dark. Blur radius as the edges.
     * The shared glass tint (token `opacity.glassTint`, [RemGlass.TINT_ALPHA]).
     */
    const val GLASS_TINT_ALPHA = RemGlass.TINT_ALPHA
    const val ACCESSIBILITY_LABEL = "Scroll to latest message"
    const val TEST_TAG = "chat.jumpToLatest"

    /**
     * Whether the button shows for a transcript whose bottom edge is [distanceFromBottom] dp below the
     * visible area. At or near the bottom it hides.
     */
    fun isVisible(distanceFromBottom: Float): Boolean = distanceFromBottom > THRESHOLD

    /**
     * Scroll offset (px) for a transcript's first measure in a composition. [savedDistanceFromLatest] is how far
     * (dp) the person was from the latest message when the host saved state (configuration change, or a
     * `SaveableStateProvider` while the screen was away), or null for a fresh transcript. Fresh, or saved at or
     * near the latest message, re-pins to [maxScroll]; saved further up keeps the same distance from the bottom
     * of the new layout, so a rotation that grows `maxValue` does not strand someone above the latest message.
     */
    fun restoredScroll(maxScroll: Int, savedDistanceFromLatest: Float?, density: Float): Int =
        if (savedDistanceFromLatest == null || !isVisible(savedDistanceFromLatest)) maxScroll
        else (maxScroll - (savedDistanceFromLatest * density).roundToInt()).coerceIn(0, maxScroll)
}

/**
 * The jump-to-latest control: a 44dp [RemGlassIconButton] with `ArrowDownward` in `labelPrimary` (registry pair
 * `arrow.down` ↔ `ArrowDownward`). Inside the Chat composition on API 32+ the circle blurs the transcript behind it
 * (the composition's [RemGlassState] via [LocalChatEdgeHaze], tinted `backgroundTertiary` at
 * [ChatJumpToLatest.GLASS_TINT_ALPHA]); elsewhere — below API 32, outside the composition (catalog), or with high
 * contrast — it is solid `backgroundTertiary`. Hairline `separator`, soft shadow: the shared glass surface.
 */
@Composable
fun ChatJumpToLatestButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    RemGlassIconButton(
        icon = Icons.Filled.ArrowDownward,
        contentDescription = ChatJumpToLatest.ACCESSIBILITY_LABEL,
        onClick = onClick,
        modifier = modifier.testTag(ChatJumpToLatest.TEST_TAG),
        size = ChatJumpToLatest.DIAMETER.dp,
        glassState = LocalChatEdgeHaze.current,
    )
}

@Preview(showBackground = true)
@Composable
private fun ChatJumpToLatestButtonPreview() {
    Row {
        RemTheme { Box(Modifier.padding(24.dp)) { ChatJumpToLatestButton(onClick = {}) } }
        RemTheme(darkTheme = true) {
            Box(Modifier.background(RemDarkColors.backgroundPrimary).padding(24.dp)) { ChatJumpToLatestButton(onClick = {}) }
        }
    }
}
