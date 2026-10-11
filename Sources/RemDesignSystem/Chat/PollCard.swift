import SwiftUI

/// One option a `PollCard` offers. `id` is host data reported back through `onSelect`.
public struct PollOption: Identifiable, Equatable, Sendable {
    public let id: String
    public let label: String

    public init(id: String, label: String) {
        self.id = id
        self.label = label
    }
}

/// Figma property `Purpose` on **PollCard** (`2559:1524`). Purpose does not change the visuals; it keeps
/// the meaning explicit: a Choice records a decision, a Suggestion starts only that bounded step.
public enum PollPurpose: String, CaseIterable, Sendable {
    case choice
    case suggestion

    public var figmaName: String { self == .choice ? "Choice" : "Suggestion" }
}

/// An option paired with its stable list-order marker (A, B, C…).
public struct PollLetteredOption: Equatable, Sendable {
    public let marker: String
    public let option: PollOption
}

/// Figma property `State` on **PollCard**, resolved from the host's selection.
public enum PollCardState: Equatable, Sendable {
    /// No recorded selection: every option is offered.
    case awaiting
    /// The selected option, with its original marker preserved.
    case answered(PollLetteredOption)

    public var figmaName: String {
        if case .answered = self { return "Answered" }
        return "Awaiting"
    }
}

/// Pure display rules for `PollCard`, shared with the Compose `PollCardModel`.
public enum PollCardModel {
    /// The marker for the option at `index` in list order: A…Z, then AA, AB… (bijective base 26).
    /// Markers are identifiers, not outcome states.
    public static func marker(at index: Int) -> String {
        precondition(index >= 0, "Poll option index must not be negative")
        var n = index + 1
        var letters: [Character] = []
        while n > 0 {
            let remainder = (n - 1) % 26
            letters.append(Character(UnicodeScalar(UInt8(65 + remainder))))
            n = (n - 1) / 26
        }
        return String(letters.reversed())
    }

    /// Every option with its list-order marker.
    public static func lettered(_ options: [PollOption]) -> [PollLetteredOption] {
        options.enumerated().map { PollLetteredOption(marker: marker(at: $0.offset), option: $0.element) }
    }

    /// `answered` only when `selection` names one of `options`; an unknown id stays `awaiting` rather
    /// than inventing a selected row.
    public static func state(options: [PollOption], selection: String?) -> PollCardState {
        guard let selection, let match = lettered(options).first(where: { $0.option.id == selection }) else {
            return .awaiting
        }
        return .answered(match)
    }
}

/// **PollCard** — a chat card that asks a question with a small set of options and keeps the user's
/// answer visible. `Awaiting` lists every option as a tappable row; `Answered` shows only the selected
/// row with its original marker and an independent green check. Selecting records a choice; it is not
/// success of any external action, and it grants no standing permission.
///
/// Options carry stable alphabetical markers in list order. The Figma master and its specimens only
/// have two options (A/B); **there is no verified three-option (C) master**. When a host passes three
/// or more options, the same A/B row pattern is extended to C, D… as requested by the product owner.
///
/// Presentation only: `selection` comes from the host and `onSelect(optionID)` reports intent.
/// The master has no leading icon or Button; the question is the card title.
///
/// Figma canonical: **PollCard** (`2559:1524`; Purpose Choice/Suggestion × State Awaiting/Answered:
/// `458:56`, `2559:1504`, `2559:1510`, `2559:1516`). Compose sibling: `chat/PollCard.kt`.
public struct PollCard: View {
    private let question: String
    private let options: [PollOption]
    private let purpose: PollPurpose
    private let selection: String?
    private let accessibilityPrefix: String
    private let onSelect: (String) -> Void

    /// Figma master width; the card fills narrower rows.
    public static let maxWidth: CGFloat = 330
    /// Fixed marker column (Figma 20pt).
    public static let markerWidth: CGFloat = 20

    public init(
        question: String,
        options: [PollOption],
        purpose: PollPurpose = .choice,
        selection: String? = nil,
        accessibilityPrefix: String = "pollCard",
        onSelect: @escaping (String) -> Void = { _ in }
    ) {
        self.question = question
        self.options = options
        self.purpose = purpose
        self.selection = selection
        self.accessibilityPrefix = accessibilityPrefix
        self.onSelect = onSelect
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            Text(question)
                .font(DesignTokens.Typography.body.weight(.semibold))
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
                .accessibilityIdentifier("\(accessibilityPrefix).question")
            switch PollCardModel.state(options: options, selection: selection) {
            case .awaiting:
                ForEach(PollCardModel.lettered(options), id: \.option.id) { item in
                    Button { onSelect(item.option.id) } label: { row(item, selected: false) }
                        .buttonStyle(.plain)
                        .accessibilityLabel("\(item.marker), \(item.option.label)")
                        .accessibilityHint(purpose == .choice ? "Records this choice" : "Starts this step")
                        .accessibilityIdentifier("\(accessibilityPrefix).option.\(item.marker)")
                }
            case .answered(let item):
                row(item, selected: true)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel("\(item.marker), \(item.option.label)")
                    .accessibilityAddTraits(.isSelected)
                    .accessibilityIdentifier("\(accessibilityPrefix).selected.\(item.marker)")
            }
        }
        .padding(DesignTokens.Spacing.md)
        .frame(maxWidth: Self.maxWidth, alignment: .leading)
        .background(
            DesignTokens.Color.backgroundSecondary,
            in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.large, style: .continuous)
        )
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier(accessibilityPrefix)
    }

    private func row(_ item: PollLetteredOption, selected: Bool) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: DesignTokens.Spacing.sm) {
            Text(item.marker)
                .frame(width: Self.markerWidth)
            Text(item.option.label)
                .frame(maxWidth: .infinity, alignment: .leading)
            if selected {
                Text("\u{2713}")
                    .foregroundStyle(DesignTokens.Color.systemGreen)
            }
        }
        .font(DesignTokens.Typography.subheadline)
        .foregroundStyle(DesignTokens.Color.labelPrimary)
        .chatChoiceSurface()
    }
}

/// Emphasis of a stacked, full-width chat choice. Poll options are all `.standard`; `PermissionCard`
/// reuses the same choice for its decisions and fills its single `.primary` one.
enum ChatChoiceEmphasis {
    case standard
    case primary
}

extension View {
    /// The shared stacked-choice surface (PollCard option rows, PermissionCard decisions): 12pt inset,
    /// fills the card width, small radius on the primary background — brand blue for the one primary
    /// choice — and the whole rounded rect is the hit target.
    func chatChoiceSurface(_ emphasis: ChatChoiceEmphasis = .standard) -> some View {
        padding(DesignTokens.Spacing.md)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                emphasis == .primary ? DesignTokens.Color.brandBlue : DesignTokens.Color.backgroundPrimary,
                in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.small, style: .continuous)
            )
            .contentShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.small, style: .continuous))
    }
}

#Preview {
    let options = [
        PollOption(id: "review", label: "Review the draft"),
        PollOption(id: "calendar", label: "Check my calendar"),
        PollOption(id: "later", label: "Remind me later"),
    ]
    // An explicit return: with a local `let`, the builder body is ambiguous between the View and
    // UIViewController #Preview overloads.
    return VStack(spacing: DesignTokens.Spacing.lg) {
        PollCard(question: "What would you like to do next?", options: options, purpose: .suggestion)
        PollCard(question: "What would you like to do next?", options: options, purpose: .suggestion, selection: "calendar")
    }
    .padding(DesignTokens.Spacing.lg)
    .background(DesignTokens.Color.backgroundPrimary)
}
