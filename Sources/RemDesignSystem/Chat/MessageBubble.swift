import SwiftUI

/// **MessageBubble** — a single chat message in the conversation transcript, in one of two roles.
///
/// The two roles read deliberately differently, mirroring the Figma **MessageBubble** component set
/// (`50:7`, page "Chat components"), the outgoing master **Rem/Chat/Outgoing** (`2000:3605`) and the
/// shipped `Shared/Views/Chat/ChatMessageViews.swift`:
///
/// - `.user` (outgoing) — a `brandBlue` rounded bubble, trailing-aligned, with `labelOnColor` text.
/// - `.assistant` (incoming) — a leading bubble on the secondary background, same geometry as outgoing.
///
/// **Geometry is responsive, never a fixed width** (`MessageBubbleGeometry`): an outgoing bubble hugs
/// its text up to 320pt; when the row is narrower than that, the bubble fills what is left. A failed
/// message reserves 52pt on the right for the outside failure control (44pt target + 8pt gap), so at a
/// 320pt screen (288pt row) the bubble is 236pt wide with 204pt of text — the accepted Figma fixture
/// (`2659:21942`).
///
/// **Reaction** (optional, independent of delivery): anchored at the upper corner toward the
/// conversation centre — outgoing upper-left, incoming upper-right — overlapping by 14pt. Top
/// clearance is reserved so the badge never collides with the message above.
///
/// **Delivery** (outgoing only): `.delivered(at:)` / `.read(at:)` show a right-aligned receipt under
/// the bubble; the `at` text is supplied by the host (illustrative in fixtures). `.failed` shows an
/// outlined `exclamationmark.circle` *entirely outside* the bubble on the right — it opens a
/// Try again menu — and "Not delivered" beneath, right-aligned to the bubble edge, with no timestamp.
/// `Read` must only be shown after an explicit acknowledgement from the recipient.
///
/// Compose sibling: `MessageBubble` in `chat/MessageBubble.kt`.
public struct MessageBubble: View {
    /// Who sent the message. Drives alignment, fill, and text treatment.
    public enum Role: Equatable, Sendable {
        case user
        case assistant
    }

    /// Delivery state of an outgoing message. Ignored for `.assistant`.
    public enum Delivery: Equatable, Sendable {
        case none
        /// Delivered; `at` is the host-formatted delivery time (e.g. "10:24").
        case delivered(at: String)
        /// Read after an explicit acknowledgement; `at` keeps the same delivery time.
        case read(at: String)
        /// Not delivered. No timestamp is shown.
        case failed
    }

    private let text: String
    private let role: Role
    private let meta: String?
    private let delivery: Delivery
    private let reaction: MessageReaction?
    private let accessibilityPrefix: String
    private let onRetry: (() -> Void)?
    private let onLongPress: (() -> Void)?
    /// Spoken time ("Sent at 10:24"), so the swipe-revealed timestamp never needs the gesture.
    fileprivate(set) var accessibilityTime: String?
    /// Accessibility name of the long-press action: "React" for a bare reaction host, "Message
    /// actions" when a transcript host presents `MessageActionSheet`.
    fileprivate(set) var longPressLabel = "React"

    /// - Parameters:
    ///   - text: The message body. Plain text.
    ///   - role: `.user` (trailing brand-blue bubble) or `.assistant` (leading bubble on the secondary background).
    ///   - meta: Optional metadata (e.g. a timestamp) shown beneath the message in `chatMeta`.
    ///   - delivery: Outgoing delivery state. `.none` by default.
    ///   - reaction: Optional reaction badge, anchored toward the conversation centre.
    ///   - accessibilityPrefix: Prefix for this message's accessibility identifiers.
    ///   - onRetry: Called from the failure control's Try again menu.
    ///   - onLongPress: Called on long press — hosts present `MessageReactionPicker` from it. The
    ///     `ChatMessageDisplay` initializer emits `.requestActions` instead, for `MessageActionSheet`.
    public init(
        _ text: String,
        role: Role,
        meta: String? = nil,
        delivery: Delivery = .none,
        reaction: MessageReaction? = nil,
        accessibilityPrefix: String = "message",
        onRetry: (() -> Void)? = nil,
        onLongPress: (() -> Void)? = nil
    ) {
        self.text = text
        self.role = role
        self.meta = meta
        self.delivery = delivery
        self.reaction = reaction
        self.accessibilityPrefix = accessibilityPrefix
        self.onRetry = onRetry
        self.onLongPress = onLongPress
    }

    public var body: some View {
        switch role {
        case .user: outgoing
        case .assistant: incoming
        }
    }

    // MARK: Outgoing

    private var failed: Bool { delivery == .failed }

    private var outgoing: some View {
        VStack(alignment: .trailing, spacing: failed ? MessageBubbleGeometry.failureLabelGap : MessageBubbleGeometry.receiptGap) {
            MessageBubbleRowLayout(failed: failed) {
                bubble
                if failed { failureControl }
            }
            if let receipt {
                Text(receipt)
                    .font(DesignTokens.Typography.chatMeta.weight(.semibold))
                    .foregroundStyle(failed ? DesignTokens.Color.systemRed : DesignTokens.Color.labelSecondary)
                    .multilineTextAlignment(.trailing)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .padding(.trailing, failed ? MessageBubbleGeometry.failureReserve : 0)
                    .accessibilityIdentifier("\(accessibilityPrefix).receipt")
            }
            if let meta { metaLine(meta, alignment: .trailing) }
        }
        .padding(.top, reaction == nil ? 0 : MessageBubbleGeometry.reactionOverlap)
        .frame(maxWidth: .infinity, alignment: .trailing)
    }

    private var bubble: some View {
        Text(text)
            .font(DesignTokens.Typography.chatMessage)
            .foregroundStyle(DesignTokens.Color.labelOnColor)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, MessageBubbleGeometry.contentInsetHorizontal)
            .padding(.vertical, MessageBubbleGeometry.contentInsetVertical)
            .background(
                DesignTokens.Color.brandBlue,
                in: RoundedRectangle(cornerRadius: MessageBubbleGeometry.cornerRadius, style: .continuous)
            )
            .modifier(LongPressAction(label: longPressLabel, action: onLongPress))
            .accessibilityElement(children: .combine)
            .modifier(AccessibilityTimeValue(time: accessibilityTime))
            .accessibilityIdentifier("\(accessibilityPrefix).bubble")
            // After the bubble's accessibility element, so the reaction stays its own element.
            .overlay(alignment: .topLeading) {
                if let reaction {
                    MessageReactionBadge(reaction)
                        .offset(x: -MessageBubbleGeometry.reactionOverlap, y: -MessageBubbleGeometry.reactionOverlap)
                        .accessibilityIdentifier("\(accessibilityPrefix).reaction")
                }
            }
    }

    @ViewBuilder
    private var failureControl: some View {
        let glyph = Image(systemName: "exclamationmark.circle")
            .font(.system(size: 22, weight: .regular))
            .foregroundStyle(DesignTokens.Color.systemRed)
            .frame(width: MessageBubbleGeometry.failureControl, height: MessageBubbleGeometry.failureControl)
            .contentShape(Rectangle())
        Group {
            if let onRetry {
                Menu {
                    Button("Try again", action: onRetry)
                } label: { glyph }
                .menuStyle(.button)
                .menuIndicator(.hidden)
                .buttonStyle(.plain)
                .accessibilityHint("Opens the Try again menu")
            } else {
                glyph
            }
        }
        .accessibilityLabel("Delivery failed")
        .accessibilityIdentifier("\(accessibilityPrefix).failure")
    }

    private var receipt: String? {
        switch delivery {
        case .none: return nil
        case .delivered(let at): return "Delivered · \(at)"
        case .read(let at): return "Read · \(at)"
        case .failed: return "Not delivered"
        }
    }

    // MARK: Incoming

    private var incoming: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
            Text(text)
                .font(DesignTokens.Typography.chatMessage)
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .fixedSize(horizontal: false, vertical: true)
                // Rem's message is contained in its own bubble (Samuel, 2026-10-10), in the outgoing
                // bubble's geometry on the secondary background.
                .padding(.horizontal, MessageBubbleGeometry.contentInsetHorizontal)
                .padding(.vertical, MessageBubbleGeometry.contentInsetVertical)
                .background(
                    DesignTokens.Color.backgroundSecondary,
                    in: RoundedRectangle(cornerRadius: MessageBubbleGeometry.cornerRadius, style: .continuous)
                )
                .alignmentGuide(.messageBubbleCenter) { $0[VerticalAlignment.center] }
                .modifier(LongPressAction(label: longPressLabel, action: onLongPress))
                .accessibilityElement(children: .combine)
                .modifier(AccessibilityTimeValue(time: accessibilityTime))
                .accessibilityIdentifier("\(accessibilityPrefix).bubble")
                .overlay(alignment: .topTrailing) {
                    if let reaction {
                        MessageReactionBadge(reaction)
                            .offset(x: MessageBubbleGeometry.reactionOverlap, y: -MessageBubbleGeometry.incomingReactionTop)
                            .accessibilityIdentifier("\(accessibilityPrefix).reaction")
                    }
                }
            if let meta { metaLine(meta, alignment: .leading) }
        }
        .frame(maxWidth: MessageBubbleGeometry.maxWidth, alignment: .leading)
        .padding(.trailing, MessageBubbleGeometry.incomingTrailingReserve)
        .padding(.top, reaction == nil ? 0 : MessageBubbleGeometry.incomingReactionTop)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func metaLine(_ meta: String, alignment: Alignment) -> some View {
        Text(meta)
            .font(DesignTokens.Typography.chatMeta)
            .foregroundStyle(DesignTokens.Color.labelSecondary)
            .frame(maxWidth: .infinity, alignment: alignment)
            .padding(.trailing, role == .user && failed ? MessageBubbleGeometry.failureReserve : 0)
    }
}

extension MessageBubble {
    /// Renders a host-supplied `ChatMessageDisplay` and reports interactions as `ChatTranscriptAction`s.
    /// Delivery is rendered exactly as supplied — the host's adapter derives it from its own acceptance
    /// and acknowledgement evidence. Try again is offered only when the message failed *and* the host
    /// says it can retry (`canRetry`).
    public init(
        _ message: ChatMessageDisplay,
        accessibilityPrefix: String? = nil,
        onAction: @escaping (ChatTranscriptAction) -> Void
    ) {
        let id = message.id
        let retry: (() -> Void)? = message.canRetry && message.delivery == .failed
            ? { onAction(.retry(messageID: id)) }
            : nil
        self.init(
            message.text,
            role: message.role,
            meta: message.meta,
            delivery: message.delivery,
            reaction: message.reaction,
            accessibilityPrefix: accessibilityPrefix ?? "message.\(id)",
            onRetry: retry,
            onLongPress: { onAction(.requestActions(messageID: id)) }
        )
        accessibilityTime = ChatTimestampReveal.accessibilityTime(message)
        longPressLabel = "Message actions"
    }
}

/// Responsive message geometry, shared by the SwiftUI layout, the Compose twin and the unit tests.
/// No value here is a fixed bubble width: every width is derived from the space the row actually has.
public enum MessageBubbleGeometry {
    /// Widest an outgoing bubble (and an incoming text block) may be.
    public static let maxWidth: CGFloat = 320
    public static let contentInsetHorizontal: CGFloat = DesignTokens.Spacing.lg
    public static let contentInsetVertical: CGFloat = DesignTokens.Spacing.md
    /// Figma outgoing master radius. There is no 20pt radius token; this is the accepted value.
    public static let cornerRadius: CGFloat = 20
    /// The failure control's touch target, drawn entirely outside the bubble.
    public static let failureControl: CGFloat = 44
    public static let failureGap: CGFloat = DesignTokens.Spacing.sm
    /// Space a failed row reserves on the right for the failure control: 44 + 8 = 52.
    public static let failureReserve: CGFloat = failureControl + failureGap
    /// Gap between a failed bubble and "Not delivered".
    public static let failureLabelGap: CGFloat = DesignTokens.Spacing.sm
    /// Gap between a bubble and its Delivered / Read receipt.
    public static let receiptGap: CGFloat = DesignTokens.Spacing.xs
    /// How far the reaction badge overlaps outward and upward from an outgoing bubble's corner.
    public static let reactionOverlap: CGFloat = 14
    /// Upward overlap of an incoming reaction (incoming text has 4pt of its own top padding).
    public static let incomingReactionTop: CGFloat = 10
    /// Trailing space an incoming text block keeps so its upper-right reaction stays in the row.
    public static let incomingTrailingReserve: CGFloat = 30

    /// The widest an outgoing bubble may be in a row `available` points wide.
    public static func bubbleLimit(available: CGFloat, failed: Bool) -> CGFloat {
        max(0, min(maxWidth, available - (failed ? failureReserve : 0)))
    }

    /// Outgoing bubble width: hug the content's ideal width up to the limit; content wider than the
    /// limit fills it (Figma FILL) and wraps.
    public static func bubbleWidth(idealWidth: CGFloat, available: CGFloat, failed: Bool) -> CGFloat {
        min(idealWidth, bubbleLimit(available: available, failed: failed))
    }

    /// Text width inside an outgoing bubble of the given width.
    public static func textWidth(bubbleWidth: CGFloat) -> CGFloat {
        max(0, bubbleWidth - 2 * contentInsetHorizontal)
    }

    /// Incoming text block width in a row `available` points wide.
    public static func incomingWidth(available: CGFloat) -> CGFloat {
        max(0, min(maxWidth, available - incomingTrailingReserve))
    }
}

/// Places the outgoing bubble (and, when failed, the outside failure control) using
/// `MessageBubbleGeometry`. The row fills the proposed width; the bubble hugs or fills inside it.
public extension VerticalAlignment {
    private enum MessageBubbleCenter: AlignmentID {
        static func defaultValue(in dimensions: ViewDimensions) -> CGFloat { dimensions[VerticalAlignment.center] }
    }

    /// Vertical centre of the bubble itself, excluding its receipt and meta line. Content placed beside a
    /// message (the swipe-to-reveal time) aligns to this, so a Delivered / Read receipt never shifts it.
    static let messageBubbleCenter = VerticalAlignment(MessageBubbleCenter.self)
}

struct MessageBubbleRowLayout: Layout {
    let failed: Bool

    private func bubbleSize(_ bubble: LayoutSubview, available: CGFloat) -> CGSize {
        let ideal = bubble.sizeThatFits(.unspecified).width
        let width = MessageBubbleGeometry.bubbleWidth(idealWidth: ideal, available: available, failed: failed)
        let height = bubble.sizeThatFits(ProposedViewSize(width: width, height: nil)).height
        return CGSize(width: width, height: height)
    }

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        guard let bubble = subviews.first else { return .zero }
        let reserve = failed ? MessageBubbleGeometry.failureReserve : 0
        let available = proposal.width
            ?? min(bubble.sizeThatFits(.unspecified).width, MessageBubbleGeometry.maxWidth) + reserve
        let size = bubbleSize(bubble, available: available)
        let height = failed ? max(size.height, MessageBubbleGeometry.failureControl) : size.height
        return CGSize(width: available, height: height)
    }

    /// The bubble's own centre (it sits at the bottom; a failure control can make the row taller).
    func explicitAlignment(of guide: VerticalAlignment, in bounds: CGRect, proposal: ProposedViewSize,
                           subviews: Subviews, cache: inout ()) -> CGFloat? {
        guard guide == .messageBubbleCenter, let bubble = subviews.first else { return nil }
        return bounds.maxY - bubbleSize(bubble, available: bounds.width).height / 2
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        guard let bubble = subviews.first else { return }
        let size = bubbleSize(bubble, available: bounds.width)
        let trailing = bounds.maxX - (failed ? MessageBubbleGeometry.failureReserve : 0)
        bubble.place(
            at: CGPoint(x: trailing - size.width, y: bounds.maxY - size.height),
            proposal: ProposedViewSize(size)
        )
        if failed, subviews.count > 1 {
            let side = MessageBubbleGeometry.failureControl
            subviews[1].place(
                at: CGPoint(x: bounds.maxX - side, y: bounds.maxY - side),
                proposal: ProposedViewSize(width: side, height: side)
            )
        }
    }
}

/// Attaches the long-press (and its accessibility equivalent) only when the host supplies one.
private struct AccessibilityTimeValue: ViewModifier {
    let time: String?

    func body(content: Content) -> some View {
        if let time {
            content.accessibilityValue(time)
        } else {
            content
        }
    }
}

private struct LongPressAction: ViewModifier {
    let label: String
    let action: (() -> Void)?

    func body(content: Content) -> some View {
        if let action {
            content
                .onLongPressGesture(perform: action)
                .accessibilityAction(named: Text(label), action)
        } else {
            content
        }
    }
}

#Preview {
    VStack(spacing: DesignTokens.Spacing.lg) {
        MessageBubble("Can you move the planning sync to Thursday?", role: .user, reaction: .heart)
        MessageBubble(
            "Done — the planning sync is now Thursday at 10:00. I let the two attendees know.",
            role: .assistant,
            reaction: .thumbsUp
        )
        MessageBubble("Thanks, that works.", role: .user, delivery: .read(at: "10:24"))
        MessageBubble("Also share the agenda with the group.", role: .user, delivery: .failed, onRetry: {})
    }
    .padding(DesignTokens.Spacing.lg)
    .frame(maxWidth: .infinity)
    .background(DesignTokens.Color.backgroundPrimary)
}
