import SwiftUI

/// The two states of the contextual login card (Figma `State` on `428:37`).
public enum LoginCardState: Hashable, Sendable {
    /// No login for this site yet: the CTA opens the native Add login form.
    case entry
    /// A login exists for this site (host-reported): the CTA opens the saved details. Not a claim that
    /// sign-in succeeded.
    case saved
}

/// Display model for `LoginCard`. Copy and the CTA's Rem Button variant are derived from the state so
/// both platforms agree on them.
public struct LoginCardModel: Hashable, Sendable {
    /// Service-specific title, e.g. "GitHub login details".
    public var title: String
    /// The site the login is scoped to, e.g. "github.com".
    public var site: String
    public var state: LoginCardState

    public init(title: String, site: String, state: LoginCardState) {
        self.title = title
        self.site = site
        self.state = state
    }

    /// State copy shown under the identity row.
    public var detail: String {
        switch state {
        case .entry: "Add the login this browser task needs."
        case .saved: "Login saved for this site."
        }
    }

    public var buttonLabel: String {
        switch state {
        case .entry: "Add login"
        case .saved: "Saved"
        }
    }

    /// Figma Button `377:8` Style: `Rect · Blue` for Add login, `Rect · Secondary` for Saved.
    public var buttonVariant: RemButtonVariant {
        switch state {
        case .entry: .rectBlue
        case .saved: .rectSecondary
        }
    }
}

/// **LoginCard** — the in-transcript chat card that links a bounded, site-specific browser-task login to
/// the native Add login form (`LoginForm`), or to the saved login's details. It never stores, reads or
/// transmits a credential; the host owns navigation and any storage.
///
/// Anatomy (Figma `428:37`, Entry `428:20` / Saved `428:29`): canonical ListRow with the site mark in the
/// leading slot and `ListRowLabel` title + site together in the content slot (trailing empty, row not
/// interactive), the state copy, then one Rem Button — `Rect · Blue` "Add login" or `Rect · Secondary`
/// "Saved" — whose trailing slot carries the optional canonical Chevron.
///
/// The leading slot takes the site's mark. There is no GitHub brand asset in the library yet, so the
/// default is the documented generic site icon (`LoginSiteMark`), never an invented brand mark.
/// Compose sibling: `chat/LoginCard.kt`.
public struct LoginCard<Leading: View>: View {
    private let model: LoginCardModel
    private let showsChevron: Bool
    private let accessibilityPrefix: String
    private let onAddLogin: () -> Void
    private let onOpenSaved: () -> Void
    private let leading: () -> Leading

    public init(
        _ model: LoginCardModel,
        showsChevron: Bool = true,
        accessibilityPrefix: String = "loginCard",
        onAddLogin: @escaping () -> Void,
        onOpenSaved: @escaping () -> Void,
        @ViewBuilder leading: @escaping () -> Leading
    ) {
        self.model = model
        self.showsChevron = showsChevron
        self.accessibilityPrefix = accessibilityPrefix
        self.onAddLogin = onAddLogin
        self.onOpenSaved = onOpenSaved
        self.leading = leading
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            ListRow(layout: .nativeList, leading: leading, content: {
                ListRowLabel(model.title, subtitle: model.site)
            }, trailing: { EmptyView() })
            .padding(.vertical, DesignTokens.Spacing.md)
            .frame(minHeight: 60)
            .accessibilityElement(children: .combine)
            .accessibilityIdentifier("\(accessibilityPrefix).identity")

            Text(model.detail)
                .font(DesignTokens.Typography.footnote)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityIdentifier("\(accessibilityPrefix).detail")

            Button(action: model.state == .entry ? onAddLogin : onOpenSaved) {
                HStack(spacing: DesignTokens.Spacing.sm) {
                    Text(model.buttonLabel)
                    if showsChevron {
                        Image(systemName: "chevron.right")
                            .font(.system(size: 17, weight: .semibold))
                            .accessibilityHidden(true)
                    }
                }
            }
            .remButton(model.buttonVariant)
            .accessibilityLabel(model.state == .entry ? "Add login for \(model.site)" : "Saved login for \(model.site)")
            .accessibilityIdentifier("\(accessibilityPrefix).\(model.state == .entry ? "addLogin" : "saved")")
        }
        .chatCardSurface()
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier(accessibilityPrefix)
    }
}

public extension LoginCard where Leading == LoginSiteMark {
    /// A login card whose leading slot is the generic site icon.
    init(
        _ model: LoginCardModel,
        showsChevron: Bool = true,
        accessibilityPrefix: String = "loginCard",
        onAddLogin: @escaping () -> Void,
        onOpenSaved: @escaping () -> Void
    ) {
        self.init(model, showsChevron: showsChevron, accessibilityPrefix: accessibilityPrefix,
                  onAddLogin: onAddLogin, onOpenSaved: onOpenSaved, leading: { LoginSiteMark() })
    }
}

/// Generic service/site icon for the login card's leading slot, sized to the provider-mark slot (26×29)
/// so a site without a library brand asset never gets an invented mark.
public struct LoginSiteMark: View {
    public init() {}
    public var body: some View {
        Image(systemName: "globe")
            .font(.system(size: 20, weight: .regular))
            .foregroundStyle(DesignTokens.Color.labelSecondary)
            .frame(width: 26, height: 29)
            .accessibilityHidden(true)
    }
}

/// **LoginForm** — the native Add login form the card's CTA opens (Figma working states `1956:8162`:
/// Website section, Login details with Username or email + masked Password, scope footer). The fields
/// are bound to host state only: this view never stores, validates against a service or transmits
/// anything. The host supplies the navigation title ("Add login") and a Save action gated by
/// `LoginForm.canSave(username:password:)`, and clears its state on dismiss.
public struct LoginForm: View {
    private let site: String
    @Binding private var username: String
    @Binding private var password: String
    private let accessibilityPrefix: String

    public init(site: String, username: Binding<String>, password: Binding<String>, accessibilityPrefix: String = "loginForm") {
        self.site = site
        self._username = username
        self._password = password
        self.accessibilityPrefix = accessibilityPrefix
    }

    /// Save is available only for a non-blank username and a non-empty password — the same rule as
    /// `CloudBrowserModel.canAddLogin` (asserted equal in `ChatCardsTests`).
    public static func canSave(username: String, password: String) -> Bool {
        !username.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !password.isEmpty
    }

    public var body: some View {
        List {
            Section {
                CloudLabel("Login will be available only for this site.", title: site)
            } header: { HStack { Text("Website").textCase(nil) } }.listRowBackground(DesignTokens.Color.backgroundSecondary)
            Section {
                CloudLoginField(label: "Username or email", text: $username)
                    .accessibilityIdentifier("\(accessibilityPrefix).username")
                CloudLoginField(label: "Password", text: $password, secure: true)
                    .accessibilityIdentifier("\(accessibilityPrefix).password")
            } header: { HStack { Text("Login details").textCase(nil) } } footer: {
                Text("Rem uses this login only when you authorize access to \(site).")
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
        }
        .cloudListStyle()
    }
}

#if DEBUG
#Preview("LoginCard — states") {
    VStack(spacing: DesignTokens.Spacing.md) {
        LoginCard(LoginCardModel(title: "GitHub login details", site: "github.com", state: .entry), onAddLogin: {}, onOpenSaved: {})
        LoginCard(LoginCardModel(title: "GitHub login details", site: "github.com", state: .saved), onAddLogin: {}, onOpenSaved: {})
    }
    .padding(DesignTokens.Spacing.lg)
    .background(DesignTokens.Color.backgroundPrimary)
}
#endif
