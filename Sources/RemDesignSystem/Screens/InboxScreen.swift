import SwiftUI

/// **InboxScreen** — the Inbox surface: a large "Inbox" title over the unfiled `TaskEventRow`s (no time
/// indicator, pills hidden — they haven't been scheduled or filed yet), or an empty state. Composed
/// from Wave-2 components; the host supplies the rows through `content`. Chrome comes from the platform,
/// as in the shipping `SharedInboxView` (large nav title + plain list).
///
/// Figma canonical: Inbox screen (page "Inbox"). Source: `SharedInboxView.swift`. Compose sibling:
/// `screens/InboxScreen.kt`.
///
/// Host-driven: `InboxScreen(items:onAction:empty:)` renders host-supplied `InboxItemDisplay`s as the
/// canonical unfiled rows (no time indicator, pills hidden) with the host-reported run state as an
/// `AgentStatusPill` (`InboxItemState`; no state is inferred), and reports `InboxAction.open`. Which task
/// opens and where it routes stays in the host. The empty state is a host slot, and row separators are
/// not drawn (the SwiftUI and Compose previews disagree), until the canonical Inbox composition is reviewed.
public struct InboxScreen<Content: View>: View {
    private let title: String
    private let content: () -> Content

    public init(title: String = "Inbox", @ViewBuilder content: @escaping () -> Content) {
        self.title = title
        self.content = content
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(title)
                .font(DesignTokens.Typography.largeTitle.weight(.bold))
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.top, DesignTokens.Spacing.md)
                .padding(.bottom, DesignTokens.Spacing.sm)
            ScrollView {
                VStack(spacing: 0) {
                    content()
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .background(DesignTokens.Color.backgroundPrimary)
    }
}

extension InboxScreen {
    /// The host-driven Inbox: rows from `items`, `onAction(.open)` on tap, `empty` when there are none.
    public init<Empty: View>(
        title: String = "Inbox",
        items: [InboxItemDisplay],
        onAction: @escaping (InboxAction) -> Void,
        @ViewBuilder empty: @escaping () -> Empty
    ) where Content == InboxItemsContent<Empty> {
        self.init(title: title) {
            InboxItemsContent(items: items, onAction: onAction, empty: empty)
        }
    }
}

/// The canonical row list behind `InboxScreen(items:onAction:empty:)`.
public struct InboxItemsContent<Empty: View>: View {
    let items: [InboxItemDisplay]
    let onAction: (InboxAction) -> Void
    let empty: () -> Empty

    public var body: some View {
        if items.isEmpty {
            empty()
        } else {
            ForEach(items) { item in
                Button { onAction(.open(itemID: item.id)) } label: {
                    HStack(spacing: DesignTokens.Spacing.sm) {
                        TaskEventRow(kind: .task, title: item.title, leading: .none, showPills: false)
                        if let status = item.state.statusLabel {
                            AgentStatusPill(status, tone: item.state.needsPerson ? .attention : .neutral)
                                .padding(.trailing, DesignTokens.Spacing.lg)
                                .accessibilityIdentifier("inbox.item.\(item.id).status")
                        }
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("inbox.item.\(item.id)")
            }
        }
    }
}

#Preview {
    InboxScreen {
        TaskEventRow(kind: .task, title: "Follow up with the Freestyle team", leading: .none, showPills: false)
        Divider().padding(.leading, 60)
        TaskEventRow(kind: .task, title: "Review the Q4 roadmap draft", leading: .none, showPills: false)
        Divider().padding(.leading, 60)
        TaskEventRow(kind: .task, title: "Book the venue for the offsite", leading: .none, showPills: false)
    }
}
