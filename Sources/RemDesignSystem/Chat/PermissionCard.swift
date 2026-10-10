import SwiftUI

/// Decision state of an inline action-permission request (Figma `State` on `2577:17461`).
///
/// This is Rem's own *action* permission ("may Rem create this reminder?"). Device / OS permissions
/// (Reminders, Calendar, Notifications access) are a separate concept handed off to the native system
/// prompt and are deliberately not modelled here.
public enum PermissionCardState: Hashable, Sendable {
    case awaiting
    /// Allowed once. "Always allow" is only a proposal the host may act on; no persistent grant exists.
    case allowed
    case denied

    /// Awaiting opens expanded; resolved states (Allowed / Denied) collapse to their concise receipt,
    /// with the request history still one tap away.
    public var defaultExpanded: Bool { self == .awaiting }

    /// The concise receipt in the disclosure header. One status, never a compound such as
    /// "denied • not set".
    public var receipt: String {
        switch self {
        case .awaiting: "Needs your approval"
        case .allowed: "Allowed once"
        case .denied: "Denied"
        }
    }
}

/// The concrete action being requested. Preserved unchanged through every state so a later history
/// view keeps the original request meaning (fixed absolute times, not relative "Tomorrow" labels).
public struct PermissionRequestDetails: Hashable, Sendable {
    public var title: String
    public var schedule: String
    public var source: String

    public init(title: String, schedule: String, source: String) {
        self.title = title
        self.schedule = schedule
        self.source = source
    }
}

/// How risky the requested action is, as marked by the model. `.elevated` shows an "Elevated risk" label
/// above the question; `.standard` shows nothing.
public enum PermissionRisk: Hashable, Sendable {
    case standard
    case elevated
}

/// One raw request parameter in the "Full parameters" disclosure under the card, e.g. `to` / `subject`.
public struct PermissionParameter: Hashable, Sendable {
    public var label: String
    public var value: String

    public init(label: String, value: String) {
        self.label = label
        self.value = value
    }
}

/// Display model for `PermissionCard`.
public struct PermissionCardModel: Hashable, Sendable {
    /// Disclosure header title, e.g. "Reminder permission".
    public var title: String
    /// Request question, e.g. "Allow Rem to create this reminder?".
    public var question: String
    /// One-line summary, e.g. "One reminder in your Personal list.".
    public var summary: String
    public var details: PermissionRequestDetails
    /// The narrow scope an "Always allow" would propose. `nil` hides Always allow entirely.
    public var alwaysAllowScope: String?
    public var state: PermissionCardState
    /// Model-marked risk; `.elevated` adds the "Elevated risk" label above the question.
    public var risk: PermissionRisk
    /// Raw request parameters for the collapsed "Full parameters" disclosure; empty hides it.
    public var parameters: [PermissionParameter]

    public init(title: String, question: String, summary: String, details: PermissionRequestDetails,
                alwaysAllowScope: String? = nil, state: PermissionCardState,
                risk: PermissionRisk = .standard, parameters: [PermissionParameter] = []) {
        self.title = title
        self.question = question
        self.summary = summary
        self.details = details
        self.alwaysAllowScope = alwaysAllowScope
        self.state = state
        self.risk = risk
        self.parameters = parameters
    }

    public var subtitle: String { state.receipt }
    /// Allow once / Always allow / Deny appear only while awaiting a decision.
    public var showsDecisions: Bool { state == .awaiting }
    public var showsAlwaysAllow: Bool { showsDecisions && alwaysAllowScope != nil }
    /// Recovery from Denied reviews the same request again before any new decision.
    public var showsReviewAgain: Bool { state == .denied }
    /// The footnote under the choices that states exactly what Always allow would cover.
    public var alwaysAllowFootnote: String? {
        guard showsAlwaysAllow, let alwaysAllowScope else { return nil }
        return "Proposed Always allow scope: \(alwaysAllowScope)"
    }
    /// The risk label above the question; `nil` (nothing shown) unless the model marked elevated risk.
    public var riskLabel: String? { risk == .elevated ? "Elevated risk" : nil }
    /// "Full parameters" appears only when the request carries raw parameters.
    public var showsParameters: Bool { !parameters.isEmpty }
    /// The key request rows inside the card, as label / value pairs (same payload in every state).
    public var detailRows: [PermissionParameter] {
        [
            PermissionParameter(label: "Action", value: details.title),
            PermissionParameter(label: "When", value: details.schedule),
            PermissionParameter(label: "Source", value: details.source),
        ]
    }
}

/// **PermissionCard** — the INLINE action-permission card in the chat transcript: a canonical ListRow
/// disclosure header (title + receipt subtitle, chevron) above one Request body. Expanding and
/// collapsing happen in place; there is never a Rem sheet stacked on the card.
///
/// The body reads top to bottom: an optional "Elevated risk" label (only when the model marks
/// `.elevated`), the question, the summary, then the key request rows as label / value pairs. Awaiting
/// stacks its decisions full width on the PollCard choice surface — Always allow (only with a scope),
/// Deny, then Allow once as the one filled primary at the bottom — with the Always-allow scope footnote
/// under them. Allowed shows the request; Denied adds a `Text · Accent` "Review again". When the request
/// carries raw `parameters`, a collapsed-by-default "Full parameters" disclosure sits under the card
/// (outside its background) and lists them monospaced; the card owns that expansion.
///
/// Presentation only: the host owns `isExpanded` (seed it with `state.defaultExpanded`) and every
/// decision callback. "Allowed" does not imply the action ran. Figma: `2577:17461` (the stacked
/// decisions, risk label and Full parameters disclosure are code-led and not yet in the master).
/// Compose sibling: `chat/PermissionCard.kt`.
public struct PermissionCard: View {
    private let model: PermissionCardModel
    @Binding private var isExpanded: Bool
    private let accessibilityPrefix: String
    private let onAllow: () -> Void
    private let onAlwaysAllow: (() -> Void)?
    private let onDeny: () -> Void
    private let onReviewAgain: (() -> Void)?
    /// "Full parameters" starts collapsed; it is presentation detail, so the card owns it.
    @State private var showsFullParameters = false

    public init(
        _ model: PermissionCardModel,
        isExpanded: Binding<Bool>,
        accessibilityPrefix: String = "permissionCard",
        onAllow: @escaping () -> Void,
        onAlwaysAllow: (() -> Void)? = nil,
        onDeny: @escaping () -> Void,
        onReviewAgain: (() -> Void)? = nil
    ) {
        self.model = model
        self._isExpanded = isExpanded
        self.accessibilityPrefix = accessibilityPrefix
        self.onAllow = onAllow
        self.onAlwaysAllow = onAlwaysAllow
        self.onDeny = onDeny
        self.onReviewAgain = onReviewAgain
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            card
            if isExpanded && model.showsParameters { fullParameters }
        }
    }

    private var card: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            Button { isExpanded.toggle() } label: {
                ListRow(layout: .nativeList, leading: { EmptyView() }, content: {
                    ListRowLabel(model.title, subtitle: model.subtitle)
                }, trailing: {
                    DisclosureChevron().rotationEffect(.degrees(isExpanded ? 90 : 0))
                })
                .padding(.vertical, DesignTokens.Spacing.md)
                .frame(minHeight: 60)
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .combine)
            .accessibilityValue(isExpanded ? "Expanded" : "Collapsed")
            .accessibilityHint(isExpanded ? "Hides the request" : "Shows the request")
            .accessibilityIdentifier("\(accessibilityPrefix).header")

            if isExpanded { requestBody }
        }
        .chatCardSurface()
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier(accessibilityPrefix)
    }

    private var requestBody: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            if let riskLabel = model.riskLabel {
                Label(riskLabel, systemImage: "exclamationmark.shield")
                    .font(DesignTokens.Typography.footnote.weight(.semibold))
                    .foregroundStyle(DesignTokens.Color.systemRed)
                    .accessibilityIdentifier("\(accessibilityPrefix).risk")
            }
            Text(model.question)
                .font(DesignTokens.Typography.body.weight(.semibold))
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            Text(model.summary)
                .font(DesignTokens.Typography.subheadline)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
                .fixedSize(horizontal: false, vertical: true)
            details
            if model.showsDecisions { decisions }
            if let footnote = model.alwaysAllowFootnote, onAlwaysAllow != nil {
                Text(footnote)
                    .font(DesignTokens.Typography.footnote)
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityIdentifier("\(accessibilityPrefix).scope")
            }
            if model.showsReviewAgain, let onReviewAgain {
                Button("Review again", action: onReviewAgain)
                    .remButton(.textAccent)
                    .fixedSize()
                    .accessibilityIdentifier("\(accessibilityPrefix).reviewAgain")
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("\(accessibilityPrefix).body")
    }

    /// The key request rows: secondary label over the primary value, one pair per row.
    private var details: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            ForEach(model.detailRows, id: \.label) { row in
                VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
                    Text(row.label)
                        .font(DesignTokens.Typography.footnote)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                    Text(row.value)
                        .font(DesignTokens.Typography.subheadline)
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(DesignTokens.Spacing.md)
        .background(
            DesignTokens.Color.backgroundPrimary,
            in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.small, style: .continuous)
        )
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("\(accessibilityPrefix).details")
    }

    /// Stacked full-width decisions on the PollCard choice surface, the least reversible grant first and
    /// the one filled primary (Allow once) last; labels wrap rather than shrink.
    private var decisions: some View {
        VStack(spacing: DesignTokens.Spacing.sm) {
            if model.showsAlwaysAllow, let onAlwaysAllow {
                decision("Always allow", id: "alwaysAllow", emphasis: .standard, action: onAlwaysAllow)
            }
            decision("Deny", id: "deny", emphasis: .standard, action: onDeny)
            decision("Allow once", id: "allowOnce", emphasis: .primary, action: onAllow)
        }
    }

    private func decision(_ label: String, id: String, emphasis: ChatChoiceEmphasis,
                          action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(DesignTokens.Typography.subheadline.weight(.semibold))
                .foregroundStyle(emphasis == .primary ? DesignTokens.Color.labelOnColor : DesignTokens.Color.labelPrimary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity)
                .chatChoiceSurface(emphasis)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("\(accessibilityPrefix).\(id)")
    }

    /// The collapsed-by-default disclosure under the card: every raw parameter as a monospaced
    /// `label: value` row, inset to the card's content edge.
    private var fullParameters: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            Button { showsFullParameters.toggle() } label: {
                HStack(spacing: DesignTokens.Spacing.xs) {
                    Text("Full parameters")
                        .font(DesignTokens.Typography.footnote.weight(.semibold))
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                    DisclosureChevron().rotationEffect(.degrees(showsFullParameters ? 90 : 0))
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .combine)
            .accessibilityValue(showsFullParameters ? "Expanded" : "Collapsed")
            .accessibilityHint(showsFullParameters ? "Hides the full parameters" : "Shows the full parameters")
            .accessibilityIdentifier("\(accessibilityPrefix).parameters")

            if showsFullParameters {
                VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
                    ForEach(Array(model.parameters.enumerated()), id: \.offset) { _, parameter in
                        Text("\(parameter.label): \(parameter.value)")
                            .font(DesignTokens.Typography.footnote.monospaced())
                            .foregroundStyle(DesignTokens.Color.labelPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                            .textSelection(.enabled)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityElement(children: .contain)
                .accessibilityIdentifier("\(accessibilityPrefix).parameterList")
            }
        }
        .padding(.horizontal, DesignTokens.Spacing.md)
    }
}

#if DEBUG
private func previewRequest(_ state: PermissionCardState) -> PermissionCardModel {
    PermissionCardModel(
        title: "Reminder permission",
        question: "Allow Rem to create this reminder?",
        summary: "One reminder in your Personal list.",
        details: PermissionRequestDetails(title: "Send investor update", schedule: "Oct 10, 2026 · 9:00 AM UTC", source: "Reminders · Personal"),
        alwaysAllowScope: "create reminders in Personal only.",
        state: state
    )
}

private let previewEmailRequest = PermissionCardModel(
    title: "Email permission",
    question: "Allow Rem to send this email?",
    summary: "Sends from your Gmail account. This can’t be undone.",
    details: PermissionRequestDetails(title: "Q3 investor update", schedule: "Sends immediately", source: "Gmail · samuel@example.com"),
    state: .awaiting,
    risk: .elevated,
    parameters: [
        PermissionParameter(label: "to", value: "investors@example.com"),
        PermissionParameter(label: "subject", value: "Q3 investor update"),
    ]
)

#Preview("PermissionCard — elevated risk") {
    PermissionCard(previewEmailRequest, isExpanded: .constant(true), onAllow: {}, onDeny: {})
        .padding(DesignTokens.Spacing.lg)
        .background(DesignTokens.Color.backgroundPrimary)
}

#Preview("PermissionCard — states") {
    ScrollView {
        VStack(spacing: DesignTokens.Spacing.md) {
            ForEach([PermissionCardState.awaiting, .allowed, .denied], id: \.self) { state in
                PermissionCard(previewRequest(state), isExpanded: .constant(true), onAllow: {}, onAlwaysAllow: {}, onDeny: {}, onReviewAgain: {})
                PermissionCard(previewRequest(state), isExpanded: .constant(false), onAllow: {}, onAlwaysAllow: {}, onDeny: {}, onReviewAgain: {})
            }
        }
        .padding(DesignTokens.Spacing.lg)
    }
    .background(DesignTokens.Color.backgroundPrimary)
}
#endif
