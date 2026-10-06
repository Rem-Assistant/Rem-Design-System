import SwiftUI

/// Canonical **empty / content-unavailable** state: centered symbol · title · message · optional action.
/// The reusable piece of `Screen/Agenda-Empty` (`140:1613`) — "No agenda yet" — and every other empty
/// surface (inbox, search, connectors-none). Named to match SwiftUI's own `ContentUnavailableView`
/// (registry: `ContentUnavailableView`, not "EmptyState"). Pure/presentational; token-driven.
///
/// The Empty Agenda screen composes `DateNavigationHeader` (Figma canonical `43:2`) above this view;
/// a SwiftUI `DateNavigationHeader` DS component is a separate follow-up, so this ships the novel piece.
public struct RemContentUnavailableView: View {
    var symbol: String
    var title: String
    var message: String
    var actionLabel: String?
    var action: (() -> Void)?

    public init(
        symbol: String,
        title: String,
        message: String,
        actionLabel: String? = nil,
        action: (() -> Void)? = nil
    ) {
        self.symbol = symbol
        self.title = title
        self.message = message
        self.actionLabel = actionLabel
        self.action = action
    }

    public var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Image(systemName: symbol)
                .font(.system(size: 52, weight: .regular))
                .foregroundStyle(DesignTokens.Color.labelTertiary)
            VStack(spacing: DesignTokens.Spacing.xs) {
                Text(title)
                    .font(DesignTokens.Typography.title3Bold)
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .multilineTextAlignment(.center)
                Text(message)
                    .font(DesignTokens.Typography.subheadline)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if let actionLabel, let action {
                Button(action: action) {
                    Label(actionLabel, systemImage: "plus")
                        .font(DesignTokens.Typography.body)
                }
                .foregroundStyle(DesignTokens.Color.brandBlue)
                .padding(.top, DesignTokens.Spacing.xs)
            }
        }
        .padding(DesignTokens.Spacing.xl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#if DEBUG
#Preview("RemContentUnavailableView — Agenda empty") {
    RemContentUnavailableView(
        symbol: "calendar.badge.plus",
        title: "No agenda yet",
        message: "Create a new task or schedule existing ones",
        actionLabel: "Add New",
        action: {}
    )
}
#endif
