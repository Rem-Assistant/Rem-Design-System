import SwiftUI

/// **SuggestionSection** — the one suggestion surface, design-system sibling of the Compose
/// `SuggestionSection` (`rows/SuggestionSection.kt`). Authority: the shipping app's
/// `Shared/Views/Tasks/SharedSuggestionSection.swift`. It owns three behaviours so no call site can
/// drift:
///
/// 1. **Bounded inline set** — only `inlineLimit` rows render in place (an unbounded list stops
///    reading as "next steps" and starts reading as a backlog).
/// 2. **Overflow behind "See more"** — the remainder is one tap away via `onSeeMore`; nothing is
///    dropped. The host owns where the overflow goes (the app presents a sheet from a stable ancestor).
/// 3. **One header everywhere** — "Suggestions", sentence case, footnote semibold, `labelSecondary`.
///
/// It renders the list it is given; contextual ordering (`SuggestionBriefRelevance`) is app logic the
/// host applies first. Rows are the canonical `AgendaSuggestionRow`. No Figma master exists yet.
public struct SuggestionSection: View {
    private let suggestions: [AgendaSuggestionItem]
    private let inlineLimit: Int
    private let onAccept: (AgendaSuggestionItem) -> Void
    private let onDismiss: (AgendaSuggestionItem) -> Void
    private let onSeeMore: (() -> Void)?

    /// Three is the most that reads as "a few next steps" rather than "a list".
    public static let defaultInlineLimit = 3

    public init(
        suggestions: [AgendaSuggestionItem],
        inlineLimit: Int = SuggestionSection.defaultInlineLimit,
        onAccept: @escaping (AgendaSuggestionItem) -> Void,
        onDismiss: @escaping (AgendaSuggestionItem) -> Void,
        onSeeMore: (() -> Void)? = nil
    ) {
        self.suggestions = suggestions
        self.inlineLimit = inlineLimit
        self.onAccept = onAccept
        self.onDismiss = onDismiss
        self.onSeeMore = onSeeMore
    }

    public var body: some View {
        let inline = Array(suggestions.prefix(max(0, inlineLimit)))
        let overflow = suggestions.count - inline.count
        if !suggestions.isEmpty {
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
                Text("Suggestions")
                    .font(DesignTokens.Typography.footnote.weight(.semibold))
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityAddTraits(.isHeader)
                ForEach(inline) { suggestion in
                    AgendaSuggestionRow(
                        action: suggestion.action,
                        title: suggestion.title,
                        metadata: suggestion.metadata,
                        onAccept: { onAccept(suggestion) },
                        onDismiss: { onDismiss(suggestion) }
                    )
                }
                // Founder-specified copy, deliberately without a count.
                if overflow > 0, let onSeeMore {
                    // The whole row is the tap target: frame and shape sit inside the label, because a
                    // plain-style button only hit-tests its label.
                    Button(action: onSeeMore) {
                        Text("See more")
                            .font(DesignTokens.Typography.footnote.weight(.semibold))
                            .foregroundStyle(DesignTokens.Color.brandBlueOnFill)
                            .frame(maxWidth: .infinity, minHeight: 32, alignment: .leading)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

#if DEBUG
#Preview("SuggestionSection") {
    SuggestionSection(
        suggestions: AgendaSuggestionItem.referenceSuggestions,
        onAccept: { _ in },
        onDismiss: { _ in },
        onSeeMore: {}
    )
    .padding()
}
#endif
