import SwiftUI

/// **AgendaSuggestionRow** — the current Agenda *Suggestions* row (Figma `2336:19583`, variants
/// `action=add` `2336:19561` / `action=move` `2336:19572`). A proposed task rendered as a dashed-border
/// rounded tile with three regions:
///   • a leading **accept CTA** (a `plus` / `arrow.turn.up.right` glyph + "Add" / "Move", brandBlue) —
///     where a `TaskEventRow` shows the time; a suggestion's whole point is its action;
///   • the **title + reason** (the field-owned "why", e.g. "Your calendar has rehearsal at 6:00 PM.");
///   • a trailing **dismiss ✕** — this is not a drill-down.
///
/// Per the component's authored description there is **no spinner, success badge, error card or Retry**
/// control — Add / Move accept optimistically and ✕ dismisses. Token-only; the host owns the data and
/// the optimistic model. Compose sibling: `SuggestedTaskRow`.
public struct AgendaSuggestionRow: View {
    /// The accept action a row offers. `add` inserts a new task; `move` reschedules an existing one.
    public enum Action: String, Equatable, Sendable {
        case add
        case move

        var label: String { self == .add ? "Add" : "Move" }
        var symbol: String { self == .add ? "plus" : "arrow.turn.up.right" }
    }

    private let action: Action
    private let title: String
    private let metadata: String
    private let onAccept: () -> Void
    private let onDismiss: () -> Void
    private let acceptIdentifier: String?
    private let dismissIdentifier: String?

    public init(
        action: Action,
        title: String,
        metadata: String,
        acceptIdentifier: String? = nil,
        dismissIdentifier: String? = nil,
        onAccept: @escaping () -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.action = action
        self.title = title
        self.metadata = metadata
        self.acceptIdentifier = acceptIdentifier
        self.dismissIdentifier = dismissIdentifier
        self.onAccept = onAccept
        self.onDismiss = onDismiss
    }

    public var body: some View {
        HStack(spacing: DesignTokens.Spacing.sm) {
            // LEADING — the accept CTA (replaces the time column of a task row).
            Button(action: onAccept) {
                VStack(spacing: 3) {
                    Image(systemName: action.symbol)
                        .font(.system(size: 18, weight: .regular))
                    Text(action.label)
                        .font(DesignTokens.Typography.caption1Bold)
                }
                .foregroundStyle(DesignTokens.Color.brandBlue)
                .frame(width: 64, height: 44)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .modifier(OptionalIdentifier(acceptIdentifier))
            .accessibilityLabel("\(action.label) \(title)")

            // CONTENT — title + the field-owned reason.
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
                Text(title)
                    .font(DesignTokens.Typography.body.weight(.semibold))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                Text(metadata)
                    .font(DesignTokens.Typography.footnote)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            // TRAILING — dismiss ✕ (replaces the drill-down chevron).
            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.system(size: 14, weight: .regular))
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .frame(width: 30, height: 30)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .modifier(OptionalIdentifier(dismissIdentifier))
            .accessibilityLabel("Dismiss \(title)")
        }
        .padding(6)
        .overlay(
            RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium, style: .continuous)
                .strokeBorder(
                    DesignTokens.Color.separator,
                    style: StrokeStyle(lineWidth: 1, dash: [4, 3])
                )
        )
    }
}

/// Applies an accessibility identifier only when one is supplied, so the same row reads cleanly whether
/// or not the host disambiguates inline vs overflow instances.
private struct OptionalIdentifier: ViewModifier {
    let identifier: String?
    init(_ identifier: String?) { self.identifier = identifier }
    func body(content: Content) -> some View {
        if let identifier {
            content.accessibilityIdentifier(identifier)
        } else {
            content
        }
    }
}

#if DEBUG
#Preview("AgendaSuggestionRow") {
    VStack(spacing: DesignTokens.Spacing.xs) {
        AgendaSuggestionRow(
            action: .add,
            title: "Prep for tonight’s rehearsal",
            metadata: "Your calendar has rehearsal at 6:00 PM.",
            onAccept: {}, onDismiss: {}
        )
        AgendaSuggestionRow(
            action: .move,
            title: "Move rehearsal check-in to 3:00 PM",
            metadata: "Your calendar has a conflict at 8:00 AM.",
            onAccept: {}, onDismiss: {}
        )
    }
    .padding()
    .background(DesignTokens.Color.backgroundPrimary)
}
#endif
