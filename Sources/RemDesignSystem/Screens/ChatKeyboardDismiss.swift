import CoreGraphics

/// Swipe down to dismiss the keyboard: the rule the composer applies to a drag that starts on it. A clear
/// downward swipe ends text editing (the composer's focus becomes false, so the keyboard hides); an upward
/// or mostly sideways drag, a tap, or a press-and-hold drag (text selection) does nothing. The transcript
/// keeps its own interactive dismissal (`.scrollDismissesKeyboard(.interactively)`). The composer owns it;
/// hosts wire nothing. Pure rules so both platforms agree. Twin of Compose `ChatKeyboardDismiss`.
/// Contract: `docs/contracts/chat.md` rule 11.
///
/// Lives in Screens/ beside `ChatJumpToLatest`: a rules/constants type of the Chat composition, not a visual
/// component, so it has no Figma master and no Code Connect mapping.
public enum ChatKeyboardDismiss {
    /// Minimum downward travel (pt) for a swipe on the composer to dismiss the keyboard.
    public static let threshold: CGFloat = 24
    /// Vertical travel must exceed horizontal travel by this factor, so sideways drags (attachment chips,
    /// a diagonal slip while tapping) never dismiss. Platform-convention choice marked for review.
    public static let verticalDominance: CGFloat = 1.5
    /// A drag that only starts moving after the finger has been held still this long (seconds) is a text
    /// selection or caret gesture, never a dismiss swipe.
    public static let holdLimit: Double = 0.35
    /// The accessibility action offered on the focused composer field (iOS; TalkBack custom action on Android).
    public static let accessibilityActionName = "Dismiss keyboard"

    /// Whether a drag that travelled `dx`, `dy` (pt, positive `dy` is downward) is a dismiss swipe:
    /// at least `threshold` down, with vertical travel dominating horizontal.
    public static func isDismissSwipe(dx: CGFloat, dy: CGFloat) -> Bool {
        dy >= threshold && dy > abs(dx) * verticalDominance
    }

    /// Whether a drag that began moving `holdBeforeMoving` seconds after the finger went down is a swipe
    /// (true) rather than a press-and-hold selection drag (false).
    public static func isSwipeStart(holdBeforeMoving: Double) -> Bool {
        holdBeforeMoving < holdLimit
    }
}
