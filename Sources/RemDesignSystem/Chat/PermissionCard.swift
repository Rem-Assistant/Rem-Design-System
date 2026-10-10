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

    public init(title: String, question: String, summary: String, details: PermissionRequestDetails,
                alwaysAllowScope: String? = nil, state: PermissionCardState) {
        self.title = title
        self.question = question
        self.summary = summary
        self.details = details
        self.alwaysAllowScope = alwaysAllowScope
        self.state = state
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
}

/// **PermissionCard** — the INLINE action-permission card in the chat transcript: a canonical ListRow
/// disclosure header (title + receipt subtitle, chevron) above one Request body. Expanding and
/// collapsing happen in place; there is never a Rem sheet stacked on the card. Awaiting shows three
/// horizontal decisions (ButtonGroup `773:17` of `Pill · Secondary` Buttons with wrapping labels, no type
/// shrink); Allowed shows the request; Denied shows the request and a `Text · Accent` "Review again".
///
/// Presentation only: the host owns `isExpanded` (seed it with `state.defaultExpanded`) and every
/// decision callback. "Allowed" does not imply the action ran. Figma: `2577:17461`.
/// Compose sibling: `chat/PermissionCard.kt`.
public struct PermissionCard: View {
    private let model: PermissionCardModel
    @Binding private var isExpanded: Bool
    private let accessibilityPrefix: String
    private let onAllow: () -> Void
    private let onAlwaysAllow: (() -> Void)?
    private let onDeny: () -> Void
    private let onReviewAgain: (() -> Void)?

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
            Text(model.question)
                .font(DesignTokens.Typography.body.weight(.semibold))
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .fixedSize(horizontal: false, vertical: true)
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

    private var details: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            Text(model.details.title)
                .font(DesignTokens.Typography.body.weight(.semibold))
                .foregroundStyle(DesignTokens.Color.labelPrimary)
            Text(model.details.schedule)
                .font(DesignTokens.Typography.subheadline)
                .foregroundStyle(DesignTokens.Color.labelPrimary)
            Text(model.details.source)
                .font(DesignTokens.Typography.footnote)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
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

    /// Three horizontal canonical Buttons sharing the width; labels wrap rather than shrink.
    private var decisions: some View {
        HStack(alignment: .top, spacing: DesignTokens.Spacing.sm) {
            decision("Allow once", id: "allowOnce", action: onAllow)
            if model.showsAlwaysAllow, let onAlwaysAllow {
                decision("Always allow", id: "alwaysAllow", action: onAlwaysAllow)
            }
            decision("Deny", id: "deny", action: onDeny)
        }
    }

    private func decision(_ label: String, id: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                // 54 + the pill's 2×5 vertical padding = the authored 64pt choice height.
                .frame(maxWidth: .infinity, minHeight: 54)
        }
        .remButton(.pillSecondary)
        .accessibilityIdentifier("\(accessibilityPrefix).\(id)")
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
