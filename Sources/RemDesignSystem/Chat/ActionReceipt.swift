import SwiftUI

/// The outcome an `ActionReceipt` reports. Figma property `Outcome` on **ActionReceipt** (`2566:2645`).
public enum ActionReceiptOutcome: String, CaseIterable, Sendable {
    /// The provider confirmed the action (Figma `Outcome=Confirmed`, `2566:2641`).
    case confirmed
    /// The outcome is unknown. It implies neither success, failure nor permission to retry
    /// (Figma `Outcome=Unconfirmed`, `2566:2643`).
    case unconfirmed

    /// The Figma `Outcome` value this maps to (Code Connect parity).
    public var figmaName: String {
        switch self {
        case .confirmed: return "Confirmed"
        case .unconfirmed: return "Unconfirmed"
        }
    }
}

/// **ActionReceipt** — the non-interactive outcome that replaces a chat card's actions once the host
/// reports what happened (for example "Sent" under a `MessageDraftCard`). It is a status, not a control:
/// no tap, no retry, and VoiceOver reads it as static text.
///
/// The visual primitive is the canonical Button `377:8` at **Rect · Secondary / Disabled / Regular**
/// (`909:5569`), so geometry, radius and Body/Bold come from `RemButtonTokenSet(.rectSecondary)`.
/// `Unconfirmed` keeps the stock disabled presentation; `Confirmed` applies the receipt-owned colors
/// (success tint = `systemGreen` at 12% + `labelPrimary`). The label is contextual and supplied by the host.
///
/// Figma canonical: **ActionReceipt** (`2566:2645`). Compose sibling: `chat/ActionReceipt.kt`.
public struct ActionReceipt: View {
    private let outcome: ActionReceiptOutcome
    private let label: String
    private let accessibilityPrefix: String

    /// Success-tint alpha for the Confirmed fill (Figma `fill/success-tint`).
    public static let successTintOpacity: Double = 0.12
    /// Figma geometry of the nested Button (Regular): 48pt tall.
    public static let height: CGFloat = 48

    public init(_ outcome: ActionReceiptOutcome, label: String, accessibilityPrefix: String = "actionReceipt") {
        self.outcome = outcome
        self.label = label
        self.accessibilityPrefix = accessibilityPrefix
    }

    public var body: some View {
        let tokens = RemButtonTokenSet(variant: .rectSecondary, size: .regular)
        let background = outcome == .confirmed
            ? DesignTokens.Color.systemGreen.opacity(Self.successTintOpacity)
            : tokens.backgroundDisabled
        let foreground = outcome == .confirmed ? DesignTokens.Color.labelPrimary : tokens.foregroundDisabled
        Text(label)
            .font(tokens.font)
            .foregroundStyle(foreground)
            .lineLimit(1)
            .padding(.horizontal, tokens.horizontalPadding)
            .frame(maxWidth: .infinity, minHeight: Self.height)
            .background(background, in: RoundedRectangle(cornerRadius: tokens.cornerRadius, style: .continuous))
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
            .accessibilityAddTraits(.isStaticText)
            .accessibilityIdentifier("\(accessibilityPrefix).\(outcome.rawValue)")
    }
}

#Preview {
    VStack(spacing: DesignTokens.Spacing.sm) {
        ActionReceipt(.confirmed, label: "Sent")
        ActionReceipt(.unconfirmed, label: "Unconfirmed")
    }
    .padding(DesignTokens.Spacing.lg)
    .background(DesignTokens.Color.backgroundSecondary)
}
