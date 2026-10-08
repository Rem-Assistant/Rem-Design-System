import Foundation

/// The four authored policies. They change this local fixture only; no Gmail authorization occurs.
public enum GmailPermission: String, CaseIterable, Identifiable, Sendable {
    case alwaysAsk, lowRisk, alwaysAllow, neverAllow
    public var id: String { rawValue }
    public var title: String {
        switch self {
        case .alwaysAsk: "Always ask"
        case .lowRisk: "Allow low-risk actions"
        case .alwaysAllow: "Always allow"
        case .neverAllow: "Never allow"
        }
    }
    public var explanation: String {
        switch self {
        case .alwaysAsk: "Ask before reading messages or taking any Gmail action."
        case .lowRisk: "Read messages and manage drafts or labels. Ask before sending or deleting email."
        case .alwaysAllow: "Allow every Gmail action without approval, including sending or deleting email."
        case .neverAllow: "Block Gmail access until you change this permission."
        }
    }
}

public struct GmailAccountFixture: Identifiable, Hashable, Sendable {
    public let id: String
    public let email: String
    public let role: String
    public init(id: String, email: String, role: String = "Primary") {
        self.id = id; self.email = email; self.role = role
    }
    public static let primary = Self(id: "avery", email: "avery@example.com")
}

/// Immutable-value session fixture. Disconnect only removes account membership and its override;
/// it never touches email. A second account may be injected for scope tests, never the visual default.
public struct GmailFixture: Equatable, Sendable {
    public private(set) var accounts: [GmailAccountFixture]
    public private(set) var connectorPermission: GmailPermission = .lowRisk
    public private(set) var accountOverrides: [String: GmailPermission] = [:]
    public init(accounts: [GmailAccountFixture] = [.primary]) { self.accounts = accounts }
    public var isConnected: Bool { !accounts.isEmpty }
    public func account(_ id: String) -> GmailAccountFixture? { accounts.first { $0.id == id } }
    public func permission(for accountID: String) -> GmailPermission {
        accountOverrides[accountID] ?? connectorPermission
    }
    public mutating func setConnectorPermission(_ value: GmailPermission) { connectorPermission = value }
    public mutating func setAccountPermission(_ value: GmailPermission, accountID: String) {
        guard account(accountID) != nil else { return }
        accountOverrides[accountID] = value
    }
    public mutating func disconnect(accountID: String) {
        accounts.removeAll { $0.id == accountID }
        accountOverrides.removeValue(forKey: accountID)
    }
    public mutating func disconnectAll() { accounts.removeAll(); accountOverrides.removeAll() }
}

public enum GmailFixtureCopy {
    public static let readActions = ["Batch read email", "Batch read email threads", "Search emails", "Search email ids", "Search thread ids"]
    public static let writeActions = ["Apply labels to emails", "Archive emails", "Batch modify email", "Create draft", "Send email", "Send draft", "Update draft"]
    public static let connectorFooter = "Controls the level of access Rem has across all Gmail accounts connected to this connector."
    public static func accountFooter(_ email: String) -> String {
        "Controls what Rem can do with \(email). Overrides the connector-wide setting for this account."
    }
    public static let disconnectAllTitle = "Disconnect all Gmail accounts?"
    public static let disconnectAllMessage = "Rem will lose Gmail access for every connected account. Your emails are not deleted. You can reconnect accounts at any time."
    public static func disconnectMessage(_ email: String) -> String {
        "Rem will lose Gmail access for \(email). Your emails are not deleted. You can reconnect this account at any time."
    }
}
