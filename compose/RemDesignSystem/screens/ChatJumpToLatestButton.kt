package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemDarkColors
import com.rem.designsystem.tokens.RemTheme
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

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
    /** Down-arrow glyph size (dp). */
    const val GLYPH_SIZE = 20f
    /** Gap (dp) between the button and the top of the docked composer chrome (`RemSpacing.md`). */
    const val BOTTOM_SPACING = 12f
    /** Appear / disappear: short fade plus scale from this factor. No animation at animator scale 0. */
    const val APPEAR_SCALE = 0.8f
    const val APPEAR_MILLIS = 200
    /**
     * Glass (API 32+, inside the Chat composition): `backgroundTertiary` tint over the blurred transcript, high
     * enough that the `labelPrimary` arrow stays legible over bubbles in light and dark. Blur radius as the edges.
     */
    const val GLASS_TINT_ALPHA = 0.6f
    const val ACCESSIBILITY_LABEL = "Scroll to latest message"
    const val TEST_TAG = "chat.jumpToLatest"

    /**
     * Whether the button shows for a transcript whose bottom edge is [distanceFromBottom] dp below the
     * visible area. At or near the bottom it hides.
     */
    fun isVisible(distanceFromBottom: Float): Boolean = distanceFromBottom > THRESHOLD
}

/**
 * The jump-to-latest control: a 44dp glass circle (hairline `separator`, soft shadow) with `ArrowDownward` in
 * `labelPrimary` (registry pair `arrow.down` ↔ `ArrowDownward`). Inside the Chat composition on API 32+ the
 * circle blurs the transcript behind it (Haze, tinted `backgroundTertiary` at [ChatJumpToLatest.GLASS_TINT_ALPHA]);
 * elsewhere — below API 32, outside the composition (catalog), or with high contrast — it is solid
 * `backgroundTertiary`.
 */
@Composable
fun ChatJumpToLatestButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RemColors.current
    val haze = LocalChatEdgeHaze.current
    Box(
        modifier = modifier
            .size(ChatJumpToLatest.DIAMETER.dp)
            .shadow(4.dp, CircleShape, clip = false)
            .then(
                if (haze == null) Modifier.background(colors.backgroundTertiary, CircleShape)
                else Modifier.clip(CircleShape).hazeChild(
                    haze,
                    HazeStyle(
                        backgroundColor = colors.backgroundPrimary,
                        tint = HazeTint(colors.backgroundTertiary.copy(alpha = ChatJumpToLatest.GLASS_TINT_ALPHA)),
                        blurRadius = ChatEdgeBackdrop.BLUR_RADIUS.dp,
                        noiseFactor = 0f,
                        // Below API 32: the solid control.
                        fallbackTint = HazeTint(colors.backgroundTertiary),
                    ),
                ),
            )
            .border(0.5.dp, colors.separator, CircleShape)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = ChatJumpToLatest.ACCESSIBILITY_LABEL }
            .testTag(ChatJumpToLatest.TEST_TAG),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowDownward,
            contentDescription = null,
            tint = colors.labelPrimary,
            modifier = Modifier.size(ChatJumpToLatest.GLYPH_SIZE.dp),
        )
    }
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
