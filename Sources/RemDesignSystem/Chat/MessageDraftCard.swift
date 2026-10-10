import SwiftUI

/// The outgoing message a `MessageDraftCard` shows. Display data only: the host owns the real draft,
/// its account and its delivery. The payload stays identical across every card state.
public struct MessageDraft: Equatable, Sendable {
    public var title: String
    public var from: String
    public var to: String
    public var subject: String
    public var body: String

    public init(title: String = "New Email", from: String, to: String, subject: String, body: String) {
        self.title = title
        self.from = from
        self.to = to
        self.subject = subject
        self.body = body
    }
}

/// Figma property `State` on **MessageDraftCard** (`2555:1550`).
public enum MessageDraftCardState: String, CaseIterable, Sendable {
    /// Awaiting the user's explicit Send Email or Discard (`458:69`). Nothing has been sent.
    case review
    /// The provider confirmed delivery of this exact message (`2555:1499`). Not inferred from time.
    case sent
    /// The outcome is unknown (`2555:1517`). It never implies permission to send again.
    case unconfirmed

    /// The Figma `State` value this maps to.
    public var figmaName: String {
        switch self {
        case .review: return "Review"
        case .sent: return "Sent"
        case .unconfirmed: return "Unconfirmed"
        }
    }

    /// The receipt that replaces the actions, or `nil` while the draft is in review.
    public var receiptOutcome: ActionReceiptOutcome? {
        switch self {
        case .review: return nil
        case .sent: return .confirmed
        case .unconfirmed: return .unconfirmed
        }
    }

    /// The receipt's contextual label (Figma `2555:1499` / `2555:1517`), or `nil` while in review.
    public var receiptLabel: String? {
        switch self {
        case .review: return nil
        case .sent: return "Sent"
        case .unconfirmed: return "Unconfirmed"
        }
    }

    /// Only the review state offers Send Email and Discard.
    public var showsActions: Bool { self == .review }
}

/// **MessageDraftCard** — a chat card that shows an outgoing email for review: title, then a payload of
/// From / To / Subject / Body fields, then either the actions (**Send Email** = Button `377:8`
/// `Rect · Blue`, **Discard** = `Rect · Secondary`) or an `ActionReceipt` once the host reports the
/// outcome. Presentation only: `onSend` / `onDiscard` report intent; the host sends, and the host moves
/// the card to `.sent` only on provider confirmation. The card's state belongs to this message and is
/// independent of agent and voice status.
///
/// The masters carry no leading icon; the Buttons keep their Leading/Trailing slots hidden, as in Figma.
///
/// Figma canonical: **MessageDraftCard** (`2555:1550`; State `Review` `458:69`, `Sent` `2555:1499`,
/// `Unconfirmed` `2555:1517`). Compose sibling: `chat/MessageDraftCard.kt`.
public struct MessageDraftCard: View {
    private let draft: MessageDraft
    private let state: MessageDraftCardState
    private let sendLabel: String
    private let discardLabel: String
    private let accessibilityPrefix: String
    private let onSend: () -> Void
    private let onDiscard: () -> Void

    /// Figma master width; the card fills narrower rows.
    public static let maxWidth: CGFloat = 330

    public init(
        _ draft: MessageDraft,
        state: MessageDraftCardState = .review,
        sendLabel: String = "Send Email",
        discardLabel: String = "Discard",
        accessibilityPrefix: String = "messageDraft",
        onSend: @escaping () -> Void = {},
        onDiscard: @escaping () -> Void = {}
    ) {
        self.draft = draft
        self.state = state
        self.sendLabel = sendLabel
        self.discardLabel = discardLabel
        self.accessibilityPrefix = accessibilityPrefix
        self.onSend = onSend
        self.onDiscard = onDiscard
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            Text(draft.title)
                .font(DesignTokens.Typography.body.weight(.semibold))
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityAddTraits(.isHeader)
                .accessibilityIdentifier("\(accessibilityPrefix).title")
            payload
            if state.showsActions {
                HStack(spacing: DesignTokens.Spacing.sm) {
                    Button(sendLabel, action: onSend)
                        .remButton(.rectBlue)
                        .accessibilityIdentifier("\(accessibilityPrefix).send")
                    Button(discardLabel, action: onDiscard)
                        .remButton(.rectSecondary)
                        .accessibilityIdentifier("\(accessibilityPrefix).discard")
                }
            } else if let outcome = state.receiptOutcome, let label = state.receiptLabel {
                ActionReceipt(outcome, label: label, accessibilityPrefix: "\(accessibilityPrefix).receipt")
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

    private var payload: some View {
        VStack(alignment: .leading, spacing: 0) {
            inlineField("From", draft.from, id: "from")
            inlineField("To", draft.to, id: "to")
            stackedField("Subject", draft.subject, id: "subject")
            stackedField("Body", draft.body, id: "body")
        }
        .font(DesignTokens.Typography.subheadline)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            DesignTokens.Color.backgroundPrimary,
            in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium, style: .continuous)
        )
    }

    private func inlineField(_ name: String, _ value: String, id: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: DesignTokens.Spacing.sm) {
            Text(name).foregroundStyle(DesignTokens.Color.labelSecondary)
            Text(value)
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(DesignTokens.Spacing.sm)
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("\(accessibilityPrefix).\(id)")
    }

    private func stackedField(_ name: String, _ value: String, id: String) -> some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            Text(name).foregroundStyle(DesignTokens.Color.labelSecondary)
            Text(value)
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(DesignTokens.Spacing.sm)
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("\(accessibilityPrefix).\(id)")
    }
}

#Preview {
    let draft = MessageDraft(
        from: "me@example.com", to: "alex@example.com",
        subject: "Re: Next steps", body: "Hi Alex,\n\nThanks for reaching out. I've put time on the calendar.\n\nBest"
    )
    ScrollView {
        VStack(spacing: DesignTokens.Spacing.lg) {
            ForEach(MessageDraftCardState.allCases, id: \.self) { state in
                MessageDraftCard(draft, state: state)
            }
        }
        .padding(DesignTokens.Spacing.lg)
    }
    .background(DesignTokens.Color.backgroundPrimary)
}
