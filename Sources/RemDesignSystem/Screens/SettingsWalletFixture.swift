import Foundation

/// Settings Wallet's authored providers. These values identify local presentation only; they never
/// represent an authenticated account or a saved payment method.
public enum SettingsWalletProvider: String, CaseIterable, Identifiable, Sendable {
    case link
    case shopPay
    public var id: String { rawValue }
    public var title: String { self == .link ? "Link by Stripe" : "Shop Pay" }
    public var shortName: String { self == .link ? "Link" : "Shop Pay" }
    public var domain: String { self == .link ? "app.link.com" : "shop.app" }
    public var consentBody: String {
        "Connect your \(shortName) wallet for purchases you ask Rem to make."
    }
    public var disclosure: String {
        "Next, continue to \(shortName) to sign in and review access. Rem will exchange info with \(shortName); see its terms and privacy policy."
    }
    public var benefits: [SettingsWalletBenefit] {
        [
            .init(id: "wallet", symbol: "list.bullet", title: "Use your saved wallet",
                  body: "Use payment methods and checkout details you authorize through \(shortName)."),
            .init(id: "control", symbol: "hand.raised.fill", title: "You choose what Rem can do",
                  body: "Rem asks before actions that need review. Disconnect anytime in Settings."),
            .init(id: "review", symbol: "eye", title: "Keep an eye on things",
                  body: "Rem may take unexpected actions. Review purchases carefully."),
        ]
    }
}

public struct SettingsWalletBenefit: Identifiable, Sendable {
    public let id: String
    public let symbol: String
    public let title: String
    public let body: String
}

/// Only presentation state is held. Connect advances to the authored blank provider boundary;
/// cancel/close/swipe always returns to Wallet without inventing a linked or successful state.
public struct SettingsWalletFixture: Equatable, Sendable {
    public enum Stage: String, Sendable { case root, consent, external }
    public private(set) var provider: SettingsWalletProvider?
    public private(set) var stage: Stage = .root
    public init() {}
    public mutating func open(_ provider: SettingsWalletProvider) {
        self.provider = provider
        stage = .consent
    }
    public mutating func connect() {
        guard stage == .consent, provider != nil else { return }
        stage = .external
    }
    public mutating func dismiss() {
        provider = nil
        stage = .root
    }
    public static let body = "Securely save payment methods for Rem to use when making purchases for you."
}
