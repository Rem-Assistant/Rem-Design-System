import SwiftUI

/// One reaction a person can attach to a chat message: the emoji glyph plus its spoken name.
///
/// Reactions are **data**, not design: the approved long-press picker shows `standardChoices`, but a
/// host may supply its own list. This type carries no persistence — where a chosen reaction is stored
/// (if anywhere) is the host's decision. See `docs/contracts/chat.md`.
public struct MessageReaction: Hashable, Identifiable, Sendable {
    public let emoji: String
    public let name: String
    public var id: String { emoji }

    public init(emoji: String, name: String) {
        self.emoji = emoji
        self.name = name
    }

    public static let thumbsUp = MessageReaction(emoji: "👍", name: "Thumbs up")
    public static let thumbsDown = MessageReaction(emoji: "👎", name: "Thumbs down")
    public static let heart = MessageReaction(emoji: "❤️", name: "Heart")
    public static let laugh = MessageReaction(emoji: "😂", name: "Laugh")
    public static let party = MessageReaction(emoji: "🎉", name: "Party")
    public static let surprised = MessageReaction(emoji: "😮", name: "Surprised")
    public static let fire = MessageReaction(emoji: "🔥", name: "Fire")
    public static let eyes = MessageReaction(emoji: "👀", name: "Eyes")
    public static let thanks = MessageReaction(emoji: "🙏", name: "Thanks")
    public static let crying = MessageReaction(emoji: "😢", name: "Crying")
    public static let hundred = MessageReaction(emoji: "💯", name: "Hundred")

    /// The approved six-choice set — the first row of the Figma long-press sheet (`2603:19498`).
    public static let standardChoices: [MessageReaction] = [thumbsUp, thumbsDown, heart, laugh, party, surprised]

    /// The long-press sheet's 2 × 6 grid (`2603:19498`): the standard row, then five more before the
    /// grid's trailing `+` cell.
    public static let sheetChoices: [MessageReaction] = standardChoices + [fire, eyes, thanks, crying, hundred]
}

/// **MessageReactionBadge** — the reaction shown on a message: a 28pt circle in the Rem secondary
/// pill fill (`fillTertiary`) holding a 20pt emoji.
///
/// Figma canonical: **Rem/Chat/Reaction badge** (`2654:20860`). The badge is independent of delivery
/// state; `MessageBubble` anchors it at the upper corner toward the conversation centre (outgoing
/// upper-left, incoming upper-right) with a 14pt overlap. Compose sibling: `chat/MessageReactionBadge.kt`.
public struct MessageReactionBadge: View {
    private let reaction: MessageReaction

    public init(_ reaction: MessageReaction) {
        self.reaction = reaction
    }

    public var body: some View {
        Text(reaction.emoji)
            .font(.system(size: MessageReactionMetrics.badgeEmoji))
            .frame(width: MessageReactionMetrics.badge, height: MessageReactionMetrics.badge)
            .background(DesignTokens.Color.fillTertiary, in: Circle())
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Reaction: \(reaction.name)")
    }
}

/// The approved long-press reaction row: 44pt secondary-pill circles with a 27pt emoji, spread
/// edge to edge. Selecting the current reaction again clears it (`onSelect(nil)`). The host owns the
/// selection and decides how it is stored; the Playground keeps it in local fixture state only.
///
/// With `columns` the choices wrap into rows of that many cells, 12pt apart — the sheet's 2 × 6 grid.
/// `onMore` appends the trailing `+` cell (brand-blue glyph) that asks the host for its full picker;
/// without it no `+` is drawn.
public struct MessageReactionPicker: View {
    private let choices: [MessageReaction]
    private let selection: MessageReaction?
    private let columns: Int?
    private let accessibilityPrefix: String
    private let onSelect: (MessageReaction?) -> Void
    private let onMore: (() -> Void)?

    public init(
        choices: [MessageReaction] = MessageReaction.standardChoices,
        selection: MessageReaction?,
        columns: Int? = nil,
        accessibilityPrefix: String = "reactions",
        onMore: (() -> Void)? = nil,
        onSelect: @escaping (MessageReaction?) -> Void
    ) {
        self.choices = choices
        self.selection = selection
        self.columns = columns
        self.accessibilityPrefix = accessibilityPrefix
        self.onMore = onMore
        self.onSelect = onSelect
    }

    /// One grid cell: a reaction at its index in `choices`, or the `+` cell.
    private enum Cell: Hashable {
        case choice(Int)
        case more
    }

    private var rows: [[Cell]] {
        let cells = choices.indices.map(Cell.choice) + (onMore == nil ? [] : [.more])
        let width = max(1, columns ?? cells.count)
        return stride(from: 0, to: cells.count, by: width).map { Array(cells[$0..<min($0 + width, cells.count)]) }
    }

    public var body: some View {
        VStack(spacing: MessageReactionMetrics.gridRowGap) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: 0) {
                    ForEach(Array(row.enumerated()), id: \.element) { position, cell in
                        if position > 0 { Spacer(minLength: DesignTokens.Spacing.xs) }
                        self.cell(cell)
                    }
                }
                .frame(maxWidth: .infinity)
            }
        }
    }

    @ViewBuilder
    private func cell(_ cell: Cell) -> some View {
        switch cell {
        case .choice(let index):
            let choice = choices[index]
            Button {
                onSelect(choice == selection ? nil : choice)
            } label: {
                Text(choice.emoji)
                    .font(.system(size: MessageReactionMetrics.pickerEmoji))
                    .frame(width: MessageReactionMetrics.pickerTarget, height: MessageReactionMetrics.pickerTarget)
                    .background(DesignTokens.Color.fillTertiary, in: Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(choice.name)
            .accessibilityAddTraits(choice == selection ? .isSelected : [])
            .accessibilityIdentifier("\(accessibilityPrefix).\(index)")
        case .more:
            Button {
                onMore?()
            } label: {
                Image(systemName: "plus")
                    .font(DesignTokens.Typography.subheadline.weight(.semibold))
                    .foregroundStyle(DesignTokens.Color.brandBlue)
                    .frame(width: MessageReactionMetrics.pickerTarget, height: MessageReactionMetrics.pickerTarget)
                    .background(DesignTokens.Color.fillTertiary, in: Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("More reactions")
            .accessibilityIdentifier("\(accessibilityPrefix).more")
        }
    }
}

enum MessageReactionMetrics {
    static let badge: CGFloat = 28
    static let badgeEmoji: CGFloat = 20
    static let pickerTarget: CGFloat = 44
    static let pickerEmoji: CGFloat = 27
    /// Vertical gap between grid rows in the long-press sheet.
    static let gridRowGap: CGFloat = DesignTokens.Spacing.md
}

#Preview {
    VStack(spacing: DesignTokens.Spacing.xl) {
        MessageReactionBadge(.heart)
        MessageReactionPicker(selection: .heart) { _ in }
        MessageReactionPicker(choices: MessageReaction.sheetChoices, selection: .heart, columns: 6, onMore: {}) { _ in }
    }
    .padding(DesignTokens.Spacing.lg)
    .background(DesignTokens.Color.backgroundPrimary)
}
