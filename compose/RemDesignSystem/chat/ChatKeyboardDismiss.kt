package com.rem.designsystem.chat

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs

/**
 * Swipe down to dismiss the keyboard — twin of SwiftUI `ChatKeyboardDismiss`. A clear downward swipe that
 * starts on the composer clears focus and hides the IME; an upward or mostly sideways drag, a tap, or a
 * press-and-hold drag (text selection) does nothing. The full composition also dismisses the IME when the
 * person drags the transcript toward older messages (iOS `.scrollDismissesKeyboard(.interactively)`).
 * The design system owns both; hosts wire nothing. Contract: `docs/contracts/chat.md` rule 11.
 */
object ChatKeyboardDismiss {
    /** Minimum downward travel (dp) for a swipe on the composer, or a transcript drag, to dismiss the IME. */
    const val THRESHOLD = 24f
    /**
     * Vertical travel must exceed horizontal travel by this factor, so sideways drags (attachment chips, a
     * diagonal slip while tapping) never dismiss. Platform-convention choice marked for review.
     */
    const val VERTICAL_DOMINANCE = 1.5f
    /**
     * A drag that only starts moving after the finger has been held still this long (ms) is a text
     * selection or caret gesture, never a dismiss swipe.
     */
    const val HOLD_LIMIT_MILLIS = 350L
    /** The TalkBack custom action offered on the focused composer field. */
    const val ACCESSIBILITY_ACTION_NAME = "Dismiss keyboard"

    /**
     * Whether a drag that travelled [dx], [dy] (dp, positive [dy] is downward) is a dismiss swipe: at least
     * [THRESHOLD] down, with vertical travel dominating horizontal.
     */
    fun isDismissSwipe(dx: Float, dy: Float): Boolean = dy >= THRESHOLD && dy > abs(dx) * VERTICAL_DOMINANCE

    /**
     * Whether a drag that began moving [holdBeforeMovingMillis] after the finger went down is a swipe (true)
     * rather than a press-and-hold selection drag (false).
     */
    fun isSwipeStart(holdBeforeMovingMillis: Long): Boolean = holdBeforeMovingMillis < HOLD_LIMIT_MILLIS
}

/**
 * Composer swipe-down detector. Observes on the Final pass and consumes nothing, so taps, the controls and
 * the text field's own gestures (cursor, selection, an overflowed field's scroll) keep working: any change
 * a child consumed (a click's up, a selection drag, a scroll) cancels the swipe. Decided on release.
 */
internal fun Modifier.chatKeyboardDismissSwipe(enabled: Boolean, onDismiss: () -> Unit): Modifier =
    if (!enabled) this else pointerInput(onDismiss) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            var movedAt: Long? = null
            var cancelled = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                if (event.changes.size > 1) cancelled = true
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.isConsumed) cancelled = true
                val travel = change.position - down.position
                if (movedAt == null && travel.getDistance() > viewConfiguration.touchSlop) movedAt = change.uptimeMillis
                if (!change.pressed) {
                    val start = movedAt
                    if (!cancelled && start != null &&
                        ChatKeyboardDismiss.isSwipeStart(start - down.uptimeMillis) &&
                        ChatKeyboardDismiss.isDismissSwipe(travel.x / density, travel.y / density)
                    ) onDismiss()
                    break
                }
            }
        }
    }

/**
 * Transcript drag detector: while [imeVisible] reports the IME open, a user drag toward older messages
 * (finger moving down) of at least [ChatKeyboardDismiss.THRESHOLD] calls [onDismiss]. It consumes nothing,
 * so scrolling, jump to latest and following new content are unchanged. An upward drag or a fling end
 * resets the travel.
 */
internal class ChatTranscriptKeyboardDismissConnection(
    private val density: () -> Float,
    private val imeVisible: () -> Boolean,
    private val onDismiss: () -> Unit,
) : NestedScrollConnection {
    private var travel = 0f

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        travel = if (available.y > 0f) travel + available.y / density() else 0f
        if (imeVisible() && ChatKeyboardDismiss.isDismissSwipe(0f, travel)) {
            travel = 0f
            onDismiss()
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        travel = 0f
        return Velocity.Zero
    }
}
