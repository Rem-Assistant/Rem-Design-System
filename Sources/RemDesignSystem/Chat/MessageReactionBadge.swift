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

/// **MessageReactionBadge** — one tapback circle, mirroring the iMessage tapback reference Samuel shared
/// (2026-10-11): a 34pt **solid** `backgroundSecondary` disc holding a 19pt emoji, inside a 2pt
/// `backgroundPrimary` ring that cuts it out of whatever it overlaps. Both fills are opaque, so the
/// bubble underneath never shows through.
///
/// Figma canonical: **Rem/Chat/Reaction badge** (`2654:20860`). `MessageBubble` does not place this view
/// directly: it places a `MessageReactionStack` (the badge, its thought-bubble tail and any stacked
/// badges) at the bubble's upper corner toward the conversation centre. Geometry:
/// `MessageReactionBadgeMetrics`. Compose sibling: `chat/MessageReactionBadge.kt`.
public struct MessageReactionBadge: View {
    private let reaction: MessageReaction

    public init(_ reaction: MessageReaction) {
        self.reaction = reaction
    }

    public var body: some View {
        MessageReactionDisc(diameter: MessageReactionBadgeMetrics.badge) {
            Text(reaction.emoji)
                .font(.system(size: MessageReactionBadgeMetrics.emoji))
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MessageReactionBadgeMetrics.accessibilityLabel(for: [reaction]))
    }
}

/// A solid tapback disc: an opaque `backgroundSecondary` circle of `diameter` inside an opaque
/// `backgroundPrimary` ring. The frame is the ring's outer edge (`diameter + 2 × ring`).
struct MessageReactionDisc<Content: View>: View {
    let diameter: CGFloat
    let content: Content

    init(diameter: CGFloat, @ViewBuilder content: () -> Content) {
        self.diameter = diameter
        self.content = content()
    }

    var body: some View {
        let outer = diameter + 2 * MessageReactionBadgeMetrics.ring
        ZStack {
            Circle().fill(DesignTokens.Color.backgroundPrimary)
            Circle().fill(DesignTokens.Color.backgroundSecondary).frame(width: diameter, height: diameter)
            content
        }
        .frame(width: outer, height: outer)
    }
}

extension MessageReactionDisc where Content == EmptyView {
    /// A bare disc — the thought-bubble tail dots.
    init(diameter: CGFloat) {
        self.init(diameter: diameter) { EmptyView() }
    }
}

/// The reaction set drawn on a message: the front badge at the bubble's corner with its two-dot
/// thought-bubble tail, and up to two more badges stacked behind it toward the bubble's interior
/// (each 20pt further in). One accessibility element for the whole set. Positions come from
/// `MessageReactionBadgeMetrics.layout(count:corner:)`, shared with Compose and the unit tests.
struct MessageReactionStack: View {
    let reactions: [MessageReaction]
    let corner: MessageReactionBadgeMetrics.Corner

    var body: some View {
        let layout = MessageReactionBadgeMetrics.layout(count: reactions.count, corner: corner)
        let visible = Array(reactions.prefix(MessageReactionBadgeMetrics.maxVisible))
        ZStack(alignment: .topLeading) {
            ForEach(Array(layout.dots.enumerated()), id: \.offset) { _, dot in
                MessageReactionDisc(diameter: dot.width - 2 * MessageReactionBadgeMetrics.ring)
                    .offset(x: dot.minX, y: dot.minY)
            }
            // Back to front: the corner badge (index 0) is drawn last, on top.
            ForEach(Array(visible.enumerated()).reversed(), id: \.offset) { index, reaction in
                MessageReactionDisc(diameter: MessageReactionBadgeMetrics.badge) {
                    Text(reaction.emoji).font(.system(size: MessageReactionBadgeMetrics.emoji))
                }
                .offset(x: layout.badges[index].minX, y: layout.badges[index].minY)
            }
        }
        .frame(width: layout.size.width, height: layout.size.height, alignment: .topLeading)
        .offset(x: layout.offsetFromCorner.width, y: layout.offsetFromCorner.height)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MessageReactionBadgeMetrics.accessibilityLabel(for: reactions))
    }
}

/// Tapback geometry (pt), identical to the Compose `MessageReactionBadgeMetrics` and asserted by
/// `ChatGeometryTests` / `ChatGeometryTest`. Frames are in the stack's own coordinates (origin at its
/// top-left); `offsetFromCorner` moves that origin from the bubble's anchoring top corner.
public enum MessageReactionBadgeMetrics {
    /// Which bubble corner the set anchors to: outgoing → top-leading, incoming → top-trailing.
    public enum Corner: Equatable, Sendable {
        case topLeading
        case topTrailing

        public init(role: MessageBubble.Role) {
            self = role == .user ? .topLeading : .topTrailing
        }
    }

    /// Diameter of the solid badge disc (not counting its ring).
    public static let badge: CGFloat = 34
    /// Width of the opaque page-background ring around every disc and tail dot.
    public static let ring: CGFloat = 2
    /// The badge including its ring.
    public static let badgeOuter: CGFloat = badge + 2 * ring
    public static let emoji: CGFloat = 19
    /// How far the badge disc extends outward past the bubble's side edge (20 of its 34 overlap the bubble).
    public static let outward: CGFloat = MessageBubbleGeometry.reactionOverlap
    /// How far the badge disc rises above the bubble's top edge (14 of its 34 overlap the bubble).
    public static let lift: CGFloat = 20
    /// Top clearance a reacted message reserves so the set never reaches the row above.
    public static let topReserve: CGFloat = lift + ring
    /// Horizontal step between stacked badges: 60% of a badge, so each overlaps the previous one.
    public static let stackStep: CGFloat = 20
    /// Badges drawn at most; the accessibility label still names every reaction.
    public static let maxVisible = 3
    /// The thought-bubble tail, two dots below-outward of the front badge.
    public static let tailLarge: CGFloat = 8
    public static let tailSmall: CGFloat = 4
    /// Dot centres in the front badge disc's coordinates (outgoing orientation; mirrored for incoming).
    public static let tailLargeCenter = CGPoint(x: 8, y: 34)
    public static let tailSmallCenter = CGPoint(x: 3, y: 41)

    public struct Placement: Equatable, Sendable {
        public var size: CGSize
        /// Outer (ring) frames, front badge first.
        public var badges: [CGRect]
        /// Outer (ring) frames of the large then small tail dot; empty when there is no reaction.
        public var dots: [CGRect]
        /// Translation from the bubble's anchoring top corner to the stack's top-left.
        public var offsetFromCorner: CGSize
    }

    public static func layout(count: Int, corner: Corner) -> Placement {
        let visible = max(0, min(count, maxVisible))
        guard visible > 0 else { return Placement(size: .zero, badges: [], dots: [], offsetFromCorner: .zero) }
        func dot(_ centre: CGPoint, _ diameter: CGFloat) -> CGRect {
            let outer = diameter + 2 * ring
            return CGRect(x: centre.x + ring - outer / 2, y: centre.y + ring - outer / 2, width: outer, height: outer)
        }
        var badges = (0..<visible).map { CGRect(x: CGFloat($0) * stackStep, y: 0, width: badgeOuter, height: badgeOuter) }
        var dots = [dot(tailLargeCenter, tailLarge), dot(tailSmallCenter, tailSmall)]
        let width = badgeOuter + CGFloat(visible - 1) * stackStep
        let height = (badges + dots).map(\.maxY).max() ?? badgeOuter
        let reach = outward + ring
        switch corner {
        case .topLeading:
            return Placement(size: CGSize(width: width, height: height), badges: badges, dots: dots,
                          offsetFromCorner: CGSize(width: -reach, height: -topReserve))
        case .topTrailing:
            func mirror(_ r: CGRect) -> CGRect { CGRect(x: width - r.maxX, y: r.minY, width: r.width, height: r.height) }
            badges = badges.map(mirror)
            dots = dots.map(mirror)
            return Placement(size: CGSize(width: width, height: height), badges: badges, dots: dots,
                          offsetFromCorner: CGSize(width: reach, height: -topReserve))
        }
    }

    /// "Reaction: Heart" for one reaction; "Reactions: Thumbs up, Heart" for several.
    public static func accessibilityLabel(for reactions: [MessageReaction]) -> String {
        reactions.count == 1
            ? "Reaction: \(reactions[0].name)"
            : "Reactions: \(reactions.map(\.name).joined(separator: ", "))"
    }

    /// The set a message shows: its own reaction (the picker selection) in front, then any others.
    public static func stack(primary: MessageReaction?, additional: [MessageReaction]) -> [MessageReaction] {
        (primary.map { [$0] } ?? []) + additional
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
    static let pickerTarget: CGFloat = 44
    static let pickerEmoji: CGFloat = 27
    /// Vertical gap between grid rows in the long-press sheet.
    static let gridRowGap: CGFloat = DesignTokens.Spacing.md
}

#Preview {
    VStack(spacing: DesignTokens.Spacing.xl) {
        MessageReactionBadge(.heart)
        MessageBubble("Sounds good", role: .user, reaction: .thumbsUp, additionalReactions: [.thumbsUp])
        MessageReactionPicker(selection: .heart) { _ in }
        MessageReactionPicker(choices: MessageReaction.sheetChoices, selection: .heart, columns: 6, onMore: {}) { _ in }
    }
    .padding(DesignTokens.Spacing.lg)
    .background(DesignTokens.Color.backgroundPrimary)
}
