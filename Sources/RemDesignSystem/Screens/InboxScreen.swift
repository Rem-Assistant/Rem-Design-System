import SwiftUI

/// **InboxScreen** — the Inbox surface: a large "Inbox" title over the unfiled `TaskEventRow`s (no time
/// indicator, pills hidden — they haven't been scheduled or filed yet), or an empty state. Composed
/// from Wave-2 components; the host supplies the rows through `content`. Chrome comes from the platform,
/// as in the shipping `SharedInboxView` (large nav title + plain list).
///
/// Figma canonical: Inbox screen (page "Inbox"). Source: `SharedInboxView.swift`. Compose sibling:
/// `screens/InboxScreen.kt`.
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

#Preview {
    InboxScreen {
        TaskEventRow(kind: .task, title: "Follow up with the Freestyle team", leading: .none, showPills: false)
        Divider().padding(.leading, 60)
        TaskEventRow(kind: .task, title: "Review the Q4 roadmap draft", leading: .none, showPills: false)
        Divider().padding(.leading, 60)
        TaskEventRow(kind: .task, title: "Book the venue for the offsite", leading: .none, showPills: false)
    }
}
