import SwiftUI

/// **MessageActionSheet** — the long-press message sheet, Figma composition `2603:19498` (an assistant
/// message): the 2 × 6 reaction grid, then the grouped actions [Reply, Mark as unread], [Copy, Select
/// Text], [Report]. What is shown comes from `ChatMessageActionsDisplay`, so the person's own messages
/// show only their applicable rows (never Report).
///
/// Content only, composed from `MessageReactionPicker`, `RemSection` and `ListRow`: the host presents it
/// in a system sheet with the grabber (`.presentationDragIndicator(.visible)`) over the dimmed chat,
/// handles every `ChatTranscriptAction` it emits, and dismisses the sheet afterwards.
/// Compose sibling: `screens/MessageActionSheet.kt`.
public struct MessageActionSheet: View {
    private let display: ChatMessageActionsDisplay
    private let accessibilityPrefix: String
    private let onAction: (ChatTranscriptAction) -> Void

    /// - Parameter accessibilityPrefix: Defaults to `message.<id>.actions`.
    public init(
        _ display: ChatMessageActionsDisplay,
        accessibilityPrefix: String? = nil,
        onAction: @escaping (ChatTranscriptAction) -> Void
    ) {
        self.display = display
        self.accessibilityPrefix = accessibilityPrefix ?? "message.\(display.messageID).actions"
        self.onAction = onAction
    }

    public var body: some View {
        let id = display.messageID
        VStack(spacing: DesignTokens.Spacing.md) {
            MessageReactionPicker(
                choices: display.reactions,
                selection: display.selection,
                columns: MessageActionSheetMetrics.reactionColumns,
                accessibilityPrefix: "\(accessibilityPrefix).reactions",
                onMore: display.showsMoreReactions ? { onAction(.requestMoreReactions(messageID: id)) } : nil
            ) { onAction(.react(messageID: id, reaction: $0)) }

            ForEach(Array(display.groups.enumerated()), id: \.offset) { _, group in
                RemSection {
                    ForEach(Array(group.enumerated()), id: \.element) { index, action in
                        if index > 0 { divider }
                        row(action)
                    }
                }
            }
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        // The system grabber sits above; the reference keeps the grid clear of it.
        .padding(.top, DesignTokens.Spacing.xl)
        .padding(.bottom, DesignTokens.Spacing.lg)
        .frame(maxWidth: .infinity, alignment: .top)
    }

    private func row(_ action: ChatMessageAction) -> some View {
        Button {
            onAction(.messageAction(messageID: display.messageID, action: action))
        } label: {
            ListRow(leading: {
                Image(systemName: action.systemImage)
                    .font(DesignTokens.Typography.body)
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .frame(width: MessageActionSheetMetrics.leading, height: MessageActionSheetMetrics.leading)
                    .accessibilityHidden(true)
            }, content: {
                ListRowLabel(action.title)
            }, trailing: { EmptyView() })
        }
        .buttonStyle(.plain)
        .accessibilityLabel(action.title)
        .accessibilityIdentifier("\(accessibilityPrefix).\(action.rawValue)")
    }

    /// "Divider · Content inset only": aligned with the row titles, clear of the trailing edge.
    private var divider: some View {
        Divider()
            .overlay(DesignTokens.Color.separator)
            .padding(.leading, MessageActionSheetMetrics.dividerInset)
            .padding(.trailing, DesignTokens.Spacing.lg)
    }
}

enum MessageActionSheetMetrics {
    /// The reference grid is 2 × 6: eleven reactions and the `+` cell.
    static let reactionColumns = 6
    /// Leading symbol frame from the reference rows.
    static let leading: CGFloat = 29
    static let dividerInset: CGFloat = 56
}

#Preview("Assistant message") {
    MessageActionSheet(
        ChatMessageActionsDisplay(message: ChatMessageDisplay(id: "a1", role: .assistant, text: "Here's a clearer introduction."))
    ) { _ in }
    .background(DesignTokens.Color.backgroundPrimary)
}

#Preview("Own message") {
    MessageActionSheet(
        ChatMessageActionsDisplay(message: ChatMessageDisplay(id: "u1", role: .user, text: "Can you review the introduction?",
                                                              reaction: .heart))
    ) { _ in }
    .background(DesignTokens.Color.backgroundPrimary)
}
