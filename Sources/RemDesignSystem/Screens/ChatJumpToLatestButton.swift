import SwiftUI

/// Jump to latest: the round down-arrow control the full Chat composition shows, centred just above the
/// docked composer, while the person is scrolled away from the newest message. Tapping it scrolls
/// (animated) to the latest message. The composition owns it; hosts wire nothing. Pure rules so both
/// platforms agree. Twin of Compose `ChatJumpToLatest`. Contract: `docs/contracts/chat.md`.
///
/// Lives in Screens/ as part of the Chat composition (like `ChatEmptyStateView`): it has no Figma master,
/// so it has no Code Connect mapping. Promote it to Chat/ with a parserless mapping once a node exists.
public enum ChatJumpToLatest {
    /// Shown only when the transcript's bottom edge is more than this far below the visible area (pt).
    /// A fixed distance (about four short rows) rather than a viewport fraction, so the rule reads the same
    /// with the keyboard open or closed. Platform-convention choice marked for review.
    public static let threshold: CGFloat = 200
    /// Circle diameter: the 44pt minimum touch target.
    public static let diameter: CGFloat = 44
    /// Down-arrow glyph size. Fixed, like other icon-only chrome; Large Content Viewer covers large text.
    public static let glyphSize: CGFloat = 17
    /// Gap between the button and the top of the docked composer chrome.
    public static let bottomSpacing: CGFloat = DesignTokens.Spacing.md
    /// Appear / disappear: short fade plus scale from this factor. No animation with Reduce Motion.
    public static let appearScale: CGFloat = 0.8
    public static let appearDuration: Double = 0.2
    public static let accessibilityLabel = "Scroll to latest message"
    public static let accessibilityIdentifier = "chat.jumpToLatest"

    /// Whether the button shows for a transcript whose bottom edge is `distanceFromBottom` below the
    /// visible area. At or near the bottom (including overscroll, where the distance is negative) it hides.
    public static func isVisible(distanceFromBottom: CGFloat) -> Bool {
        distanceFromBottom > threshold
    }
}

/// The jump-to-latest control: a solid 44pt circle (`backgroundTertiary`, hairline `separator`, soft
/// shadow) with `arrow.down` in `labelPrimary`. Used by `ChatScreen`; public for the catalog.
public struct ChatJumpToLatestButton: View {
    private let action: () -> Void

    public init(action: @escaping () -> Void) {
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            Image(systemName: "arrow.down")
                .font(.system(size: ChatJumpToLatest.glyphSize, weight: .semibold))
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .frame(width: ChatJumpToLatest.diameter, height: ChatJumpToLatest.diameter)
                .background(DesignTokens.Color.backgroundTertiary, in: Circle())
                .overlay(Circle().strokeBorder(DesignTokens.Color.separator, lineWidth: 0.5))
                .shadow(color: .black.opacity(0.12), radius: 8, x: 0, y: 2)
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(ChatJumpToLatest.accessibilityLabel)
        .accessibilityIdentifier(ChatJumpToLatest.accessibilityIdentifier)
    #if os(iOS)
        .accessibilityShowsLargeContentViewer()
    #endif
    }
}

#Preview {
    HStack(spacing: 24) {
        ChatJumpToLatestButton {}
        ChatJumpToLatestButton {}.environment(\.colorScheme, .dark)
    }
    .padding(32)
}
