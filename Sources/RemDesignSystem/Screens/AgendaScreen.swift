import SwiftUI

/// **AgendaScreen** — the Agenda surface: a `DateNavigationHeader`, a divider, then the day's
/// `TaskEventRow`s in a scroll (or an empty state). Composed from Wave-2 components; the host supplies
/// the rows through the `content` slot (a column of `TaskEventRow`s, or a `RemContentUnavailableView`
/// for the empty day). Status bar / home indicator come from the platform, exactly as the shipping
/// `SharedAgendaView` (header + divider + list) relies on the OS chrome.
///
/// Figma canonical: Agenda screen (page "Agenda") + scenarios (`530:22`). Source: `SharedAgendaView.swift`.
/// Compose sibling: `screens/AgendaScreen.kt`.
public struct AgendaScreen<Content: View>: View {
    private let title: String
    private let dateText: String
    private let onPrevious: () -> Void
    private let onNext: () -> Void
    private let onCalendarTap: (() -> Void)?
    private let content: () -> Content

    public init(
        title: String = "Today",
        dateText: String,
        onPrevious: @escaping () -> Void,
        onNext: @escaping () -> Void,
        onCalendarTap: (() -> Void)? = nil,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.title = title
        self.dateText = dateText
        self.onPrevious = onPrevious
        self.onNext = onNext
        self.onCalendarTap = onCalendarTap
        self.content = content
    }

    public var body: some View {
        VStack(spacing: 0) {
            DateNavigationHeader(
                title: title,
                dateText: dateText,
                onPrevious: onPrevious,
                onNext: onNext,
                onCalendarTap: onCalendarTap
            )
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.vertical, DesignTokens.Spacing.sm)
            Divider()
            ScrollView {
                VStack(spacing: 0) {
                    content()
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DesignTokens.Color.backgroundPrimary)
    }
}

#Preview {
    AgendaScreen(dateText: "Oct 1 2026", onPrevious: {}, onNext: {}) {
        TaskEventRow(kind: .task, title: "Reply to Alex about the audition", leading: .time("9:00"), pills: ["3 tasks"])
        Divider().padding(.leading, 60)
        TaskEventRow(kind: .event(DesignTokens.Color.systemBlue), title: "Team standup", leading: .time("10:30"), pills: ["Work"])
        Divider().padding(.leading, 60)
        TaskEventRow(kind: .task, title: "Draft the investor update", leading: .time("14:00"), pills: ["Fundraise"])
    }
}
