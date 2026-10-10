import SwiftUI

/// The four states of a contextual connector-authorization card (Figma `State` on `2567:2666`).
/// Presentation only: the host decides when a state is reached. `added` must only be shown after the
/// host has verified the authorization; nothing here simulates a successful connection.
public enum ConnectorCardState: Hashable, Sendable {
    case authorize
    case connecting
    case added
    /// Failed connector authentication/authorization. `message` is the provider-specific error; when
    /// `nil` the card uses the authored default ("<Title> authorization failed. Retry to connect.").
    case error(message: String? = nil)
}

/// Display model for `ConnectorCard`. Pure value: the copy and the per-state action are derived here so
/// both platforms (and their unit tests) agree on them.
public struct ConnectorCardModel: Equatable, Sendable {
    public var provider: ConnectorProvider
    public var title: String
    /// The permanent purpose line, grouped with the title in the content slot.
    public var subtitle: String
    public var state: ConnectorCardState

    public init(provider: ConnectorProvider, title: String? = nil, subtitle: String, state: ConnectorCardState) {
        self.provider = provider
        self.title = title ?? provider.title
        self.subtitle = subtitle
        self.state = state
    }

    /// The single control below the identity row: an actionable Button, a disabled progress Button,
    /// or a non-interactive receipt.
    public enum Control: Equatable, Sendable {
        case authorize(label: String)
        case progress(label: String)
        case receipt(label: String)
        case retry(label: String)
    }

    public var control: Control {
        switch state {
        case .authorize: .authorize(label: "Authorize")
        case .connecting: .progress(label: "Adding…")
        case .added: .receipt(label: "Added")
        case .error: .retry(label: "Retry")
        }
    }

    /// The state-specific error, shown outside the permanent content and above Retry. `nil` otherwise.
    public var errorMessage: String? {
        guard case .error(let message) = state else { return nil }
        if let message, !message.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return message }
        return "\(title) authorization failed. Retry to connect."
    }
}

/// **ConnectorCard** — the in-transcript chat card that authorizes one named provider connection,
/// shows its progress, a verified "Added" receipt, or a connector-specific Retry. Not the Settings
/// `ConnectorRow`: the identity row's trailing slot is explicitly empty and the identity row is never
/// the authorization action — the separate Rem Button is.
///
/// Anatomy (Figma `2567:2666`): canonical ListRow with the provider mark (`ConnectorProviderMark`, exact
/// brand export) in the leading slot and `ListRowLabel` title + subtitle together in the content slot;
/// then `Rect · Blue` Authorize / disabled `Adding…` / success-tinted receipt / error copy + `Rect · Blue`
/// Retry. Retry restarts connector authorization only — it never resends anything.
///
/// Presentation only: no OAuth, credentials or network. Compose sibling: `chat/ConnectorCard.kt`.
public struct ConnectorCard: View {
    private let model: ConnectorCardModel
    private let accessibilityPrefix: String
    private let onAuthorize: () -> Void
    private let onRetry: () -> Void

    public init(
        _ model: ConnectorCardModel,
        accessibilityPrefix: String = "connectorCard",
        onAuthorize: @escaping () -> Void,
        onRetry: @escaping () -> Void
    ) {
        self.model = model
        self.accessibilityPrefix = accessibilityPrefix
        self.onAuthorize = onAuthorize
        self.onRetry = onRetry
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            ListRow(layout: .nativeList, leading: {
                ConnectorProviderMark(model.provider)
            }, content: {
                ListRowLabel(model.title, subtitle: model.subtitle)
            }, trailing: { EmptyView() })
            .frame(minHeight: 60)
            .accessibilityElement(children: .combine)
            .accessibilityIdentifier("\(accessibilityPrefix).identity")

            if let errorMessage = model.errorMessage {
                Text(errorMessage)
                    .font(DesignTokens.Typography.footnote)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityIdentifier("\(accessibilityPrefix).error")
            }
            control
        }
        .chatCardSurface()
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier(accessibilityPrefix)
    }

    @ViewBuilder private var control: some View {
        switch model.control {
        case .authorize(let label):
            Button(label, action: onAuthorize)
                .remButton(.rectBlue)
                .accessibilityLabel("\(label) \(model.title)")
                .accessibilityIdentifier("\(accessibilityPrefix).authorize")
        case .progress(let label):
            Button(label) {}
                .remButton(.rectBlue)
                .disabled(true)
                .accessibilityLabel("Adding \(model.title)")
                .accessibilityIdentifier("\(accessibilityPrefix).progress")
        case .receipt(let label):
            ChatCardReceipt(label)
                .accessibilityLabel("\(model.title) \(label.lowercased())")
                .accessibilityIdentifier("\(accessibilityPrefix).receipt")
        case .retry(let label):
            Button(label, action: onRetry)
                .remButton(.rectBlue)
                .accessibilityLabel("\(label) \(model.title)")
                .accessibilityIdentifier("\(accessibilityPrefix).retry")
        }
    }
}

/// Non-interactive outcome receipt in the Rect Button geometry with the confirmed success tint
/// (Figma ActionReceipt `2566:2645`, Outcome=Confirmed). Exposes status text, never a button.
/// Kept internal to the chat cards until the shared ActionReceipt component lands; then switch over.
struct ChatCardReceipt: View {
    private let label: String
    init(_ label: String) { self.label = label }
    var body: some View {
        Text(label)
            .font(DesignTokens.Typography.bodyBold)
            .foregroundStyle(DesignTokens.Color.labelPrimary)
            .frame(maxWidth: .infinity)
            .padding(DesignTokens.Spacing.md)
            .background(
                DesignTokens.Color.systemGreen.opacity(0.12),
                in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium, style: .continuous)
            )
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(.isStaticText)
    }
}

extension View {
    /// The shared chat-card container: secondary background, 12pt inset, large radius, fills the
    /// transcript column it is placed in (Figma card frame on `2567:2666` / `428:37` / `2577:17461`).
    func chatCardSurface() -> some View {
        padding(DesignTokens.Spacing.md)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                DesignTokens.Color.backgroundSecondary,
                in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.large, style: .continuous)
            )
    }
}

#if DEBUG
#Preview("ConnectorCard — states") {
    ScrollView {
        VStack(spacing: DesignTokens.Spacing.md) {
            ForEach([ConnectorCardState.authorize, .connecting, .added, .error()], id: \.self) { state in
                ConnectorCard(
                    ConnectorCardModel(provider: .gmail, subtitle: "Search, read, draft, and manage email.", state: state),
                    onAuthorize: {}, onRetry: {}
                )
            }
        }
        .padding(DesignTokens.Spacing.lg)
    }
    .background(DesignTokens.Color.backgroundPrimary)
}
#endif
