import SwiftUI

// MARK: - Cloud browser fixture model
//
// The deterministic, in-memory state behind `SettingsCloudBrowserScreen`. It is the single owner of
// every mutation the authored Cloud browser masters express — add site, change permission, add /
// edit / remove a saved login, clear site data, clear all site data — so the SwiftUI views stay thin
// and every scope/cancel/save rule is testable without a running simulator.
//
// Authority: `docs/playground/settings-design/settings-destinations.md` (Cloud browser) and the raw
// node contexts `1833:5071`, `1868:6148/6153/6176/6197/6214/6227/6245/6250/6255`,
// `1875:6710/6711/6712/6713/6714`, `1934:9071`, `1956:8160/8161/8162`.
//
// Prototype-only: every value is illustrative. No credential, cookie, or permission here is written
// to the Keychain, shared preferences, the network, or any real browser — passwords are masked on
// display and never persisted beyond this playground session (see the contract's explicit boundary).

/// The only two permission policies the authored masters enumerate. Additional product policies are a
/// recorded gap, never invented here.
public enum CloudSitePermission: String, CaseIterable, Hashable, Sendable {
    case ask = "Ask"
    case allow = "Allow"
}

/// A saved login for one site. `password` is illustrative fixture data only; it is always rendered
/// masked and never leaves memory.
public struct CloudSavedLogin: Identifiable, Hashable, Sendable {
    public let id: UUID
    public var username: String
    public var password: String

    public init(id: UUID = UUID(), username: String, password: String) {
        self.id = id
        self.username = username
        self.password = password
    }

    /// Passwords are always masked. The authored master shows a fixed 12-bullet field regardless of
    /// the underlying value, so display never leaks length.
    public var maskedPassword: String { String(repeating: "•", count: 12) }
}

/// One site the agent may open. Cookies and sign-in are fixture counters cleared by "Clear site data".
public struct CloudSite: Identifiable, Hashable, Sendable {
    public let id: UUID
    public var domain: String
    public var permission: CloudSitePermission
    public var logins: [CloudSavedLogin]
    public var cookieCount: Int
    public var signedIn: Bool

    public init(id: UUID = UUID(), domain: String, permission: CloudSitePermission,
                logins: [CloudSavedLogin] = [], cookieCount: Int = 0, signedIn: Bool = false) {
        self.id = id
        self.domain = domain
        self.permission = permission
        self.logins = logins
        self.cookieCount = cookieCount
        self.signedIn = signedIn
    }

    /// The second half of a sites-list subtitle: a saved-login count when any exist, otherwise the
    /// sign-in status. Matches the authored rows (`1 saved login`, `Signed in`, `No saved login`).
    public var loginSummary: String {
        if !logins.isEmpty { return "\(logins.count) saved login\(logins.count == 1 ? "" : "s")" }
        return signedIn ? "Signed in" : "No saved login"
    }

    /// e.g. `Ask · 1 saved login`, `Allow · Signed in`, `Ask · No saved login`.
    public var rowSubtitle: String { "\(permission.rawValue) · \(loginSummary)" }

    /// e.g. `12 cookies · Signed in` on the site-detail "Cookies & sessions" row.
    public var cookieSummary: String { "\(cookieCount) cookies · \(signedIn ? "Signed in" : "Signed out")" }

    /// e.g. `12 illustrative cookies` on the Cookies & sessions screen.
    public var illustrativeCookies: String { "\(cookieCount) illustrative cookies" }
}

/// The in-memory owner of Cloud browser state for one playground session.
@MainActor
public final class CloudBrowserModel: ObservableObject {
    @Published public var defaultPermission: CloudSitePermission
    @Published public private(set) var sites: [CloudSite]

    public init(defaultPermission: CloudSitePermission = .ask, sites: [CloudSite]? = nil) {
        self.defaultPermission = defaultPermission
        self.sites = sites ?? CloudBrowserModel.fixtureSites()
    }

    /// The authored github.com / notion.so / linear.app / openai.com fixtures.
    public static func fixtureSites() -> [CloudSite] {
        [
            CloudSite(domain: "github.com", permission: .ask,
                      logins: [CloudSavedLogin(username: "samuel@example.com", password: "fixture-only")],
                      cookieCount: 12, signedIn: true),
            CloudSite(domain: "notion.so", permission: .allow, cookieCount: 8, signedIn: true),
            CloudSite(domain: "linear.app", permission: .ask, cookieCount: 0, signedIn: false),
            CloudSite(domain: "openai.com", permission: .allow, cookieCount: 5, signedIn: true),
        ]
    }

    /// The two "recent" sites shown on the root; "See all sites" opens the full list.
    public var recentSites: [CloudSite] { Array(sites.prefix(2)) }

    public func site(_ id: UUID) -> CloudSite? { sites.first { $0.id == id } }

    // MARK: Domain derivation

    /// Derive a usable bare domain from a URL or raw domain string, or `nil` when nothing usable can
    /// be extracted. Strips scheme, userinfo, port, and path/query/fragment, lowercases, and requires
    /// at least one dot with only domain-legal characters — so "Save" can validate a nonempty usable
    /// domain before mutating fixture state.
    public static func deriveDomain(from raw: String) -> String? {
        var s = raw.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !s.isEmpty else { return nil }
        if let scheme = s.range(of: "://") { s = String(s[scheme.upperBound...]) }
        // Drop path / query / fragment.
        if let cut = s.firstIndex(where: { $0 == "/" || $0 == "?" || $0 == "#" }) { s = String(s[..<cut]) }
        // Drop userinfo (user:pass@host).
        if let at = s.lastIndex(of: "@") { s = String(s[s.index(after: at)...]) }
        // Drop a trailing :port.
        if let colon = s.firstIndex(of: ":") { s = String(s[..<colon]) }
        guard !s.isEmpty, s.contains("."), !s.hasPrefix("."), !s.hasSuffix(".") else { return nil }
        let allowed = CharacterSet(charactersIn: "abcdefghijklmnopqrstuvwxyz0123456789.-")
        guard s.unicodeScalars.allSatisfy({ allowed.contains($0) }) else { return nil }
        // No empty labels (guards against "a..b").
        guard !s.components(separatedBy: ".").contains(where: { $0.isEmpty }) else { return nil }
        return s
    }

    /// Whether an "Add site" draft would save. A nonempty usable domain is the only requirement; the
    /// login fields are optional on this screen.
    public static func canAddSite(urlDraft: String) -> Bool { deriveDomain(from: urlDraft) != nil }

    // MARK: Mutations

    /// Add a fixture site from an "Add site" draft. Returns the new site id, or `nil` when the domain
    /// is not usable (so the caller does not dismiss). A nonempty username adds one optional login.
    @discardableResult
    public func addSite(urlDraft: String, permission: CloudSitePermission,
                        username: String = "", password: String = "") -> UUID? {
        guard let domain = Self.deriveDomain(from: urlDraft) else { return nil }
        var logins: [CloudSavedLogin] = []
        let user = username.trimmingCharacters(in: .whitespacesAndNewlines)
        if !user.isEmpty { logins.append(CloudSavedLogin(username: user, password: password)) }
        let site = CloudSite(domain: domain, permission: permission, logins: logins,
                             cookieCount: 0, signedIn: false)
        sites.append(site)
        return site.id
    }

    public func setPermission(_ permission: CloudSitePermission, for siteID: UUID) {
        guard let index = sites.firstIndex(where: { $0.id == siteID }) else { return }
        sites[index].permission = permission
    }

    /// Whether an "Add login" draft would save: both fields must be nonempty fixture data.
    public static func canAddLogin(username: String, password: String) -> Bool {
        !username.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !password.isEmpty
    }

    /// Add a fixture login to one site. Returns the new login id, or `nil` when validation fails.
    @discardableResult
    public func addLogin(siteID: UUID, username: String, password: String) -> UUID? {
        guard Self.canAddLogin(username: username, password: password),
              let index = sites.firstIndex(where: { $0.id == siteID }) else { return nil }
        let login = CloudSavedLogin(username: username.trimmingCharacters(in: .whitespacesAndNewlines),
                                    password: password)
        sites[index].logins.append(login)
        return login.id
    }

    /// Replace only the selected credential's username. Leaves every other login and site untouched.
    public func updateUsername(siteID: UUID, loginID: UUID, to value: String) {
        mutateLogin(siteID: siteID, loginID: loginID) {
            $0.username = value.trimmingCharacters(in: .whitespacesAndNewlines)
        }
    }

    /// Replace only the selected credential's password. Passwords stay masked on display.
    public func updatePassword(siteID: UUID, loginID: UUID, to value: String) {
        mutateLogin(siteID: siteID, loginID: loginID) { $0.password = value }
    }

    /// Remove only the selected credential. Cookies and other sites are preserved.
    public func removeLogin(siteID: UUID, loginID: UUID) {
        guard let index = sites.firstIndex(where: { $0.id == siteID }) else { return }
        sites[index].logins.removeAll { $0.id == loginID }
    }

    /// Clear cookies/sessions for one site. Saved logins (passwords) are preserved.
    public func clearSiteData(siteID: UUID) {
        guard let index = sites.firstIndex(where: { $0.id == siteID }) else { return }
        sites[index].cookieCount = 0
        sites[index].signedIn = false
    }

    /// Clear cookies/sessions across every site. Saved logins (passwords) are preserved everywhere.
    public func clearAllSiteData() {
        for index in sites.indices {
            sites[index].cookieCount = 0
            sites[index].signedIn = false
        }
    }

    private func mutateLogin(siteID: UUID, loginID: UUID, _ change: (inout CloudSavedLogin) -> Void) {
        guard let siteIndex = sites.firstIndex(where: { $0.id == siteID }),
              let loginIndex = sites[siteIndex].logins.firstIndex(where: { $0.id == loginID }) else { return }
        change(&sites[siteIndex].logins[loginIndex])
    }
}
