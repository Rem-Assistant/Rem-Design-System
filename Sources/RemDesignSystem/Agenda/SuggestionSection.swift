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
/// 3. **One header everywhere** — "Suggestions", sentence case, SectionHeader (`161:68`) metrics:
///    Headline (body semibold), `labelSecondary`, 16pt horizontal inset, 6pt below.
///
/// It renders the list it is given; contextual ordering (`SuggestionBriefRelevance`) is app logic the
/// host applies first. Authority: the "Suggestions region" frame `2336:19714` — a SectionHeader, then
/// **standalone** `AgendaSuggestionRow`s (`2336:19583`, 4pt apart, no Section/rows surface around them),
/// then the "See more · Plain" Button (`377:8`), i.e. the `.textAccent` button style, hugging its label
/// at the leading edge. This behavioral wrapper has no standalone master. See
/// `code-connect/SuggestionSection.composition.json` and `docs/contracts/playground-mappings.md`.
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
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
                Text("Suggestions")
                    .font(DesignTokens.Typography.body.weight(.semibold))
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .padding(.horizontal, DesignTokens.Spacing.lg)
                    .padding(.bottom, 6)
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
                    // The canonical text button, hugging its label (`fixedSize` collapses the regular
                    // size's full-width frame) so it sits at the leading edge as in `2336:19751`.
                    Button("See more", action: onSeeMore)
                        .buttonStyle(RemButtonStyle(.textAccent))
                        .fixedSize(horizontal: true, vertical: false)
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
