import SwiftUI

/// **Pill** — a subtle status / metadata chip. Always the same quiet capsule: `backgroundSecondary`
/// fill, `caption1` label in `labelSecondary`, `h.sm` / `v6` padding, capsule (`xlarge`) radius.
/// It is deliberately *not* a saturated tone fill — the loud status badges (Overdue, Priority, task
/// run status) are separate components. This is the quiet chip used by `TaskEventRow` for the calendar
/// dot (events) and the list badge (tasks).
///
/// Figma canonical: **Pill** (component set). Source lineage: `PillView` + the calendar/list badges in
/// `TaskEventRowView.swift`. Compose sibling: `RemPill` in `primitives/RemPill.kt`.
public struct RemPill: View {
    /// The pill's leading accessory. Neutral is a plain label; `dot` shows an 8pt colored dot (calendar
    /// events); `list` shows a `list.bullet` glyph (tasks).
    public enum Kind: Equatable {
        case neutral
        case dot(Color)
        case list
    }

    private let text: String
    private let kind: Kind

    public init(_ text: String, kind: Kind = .neutral) {
        self.text = text
        self.kind = kind
    }

    public var body: some View {
        HStack(spacing: DesignTokens.Spacing.xs) {
            switch kind {
            case .neutral:
                EmptyView()
            case .dot(let color):
                Circle()
                    .fill(color)
                    .frame(width: 8, height: 8)
            case .list:
                Image(systemName: "list.bullet")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
            }
            Text(text)
                .font(DesignTokens.Typography.caption1)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
        }
        .padding(.horizontal, DesignTokens.Spacing.sm)
        .padding(.vertical, 6)
        .background(DesignTokens.Color.backgroundSecondary, in: Capsule())
    }
}

#Preview {
    VStack(spacing: 12) {
        RemPill("3 tasks", kind: .list)
        RemPill("Standup", kind: .dot(DesignTokens.Color.systemBlue))
        RemPill("Personal")
    }
    .padding(24)
    .background(DesignTokens.Color.backgroundPrimary)
}
