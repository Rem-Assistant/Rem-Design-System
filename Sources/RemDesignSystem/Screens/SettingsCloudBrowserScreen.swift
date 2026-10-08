import SwiftUI

/// **SettingsCloudBrowserScreen** — the Settings → Agent settings → **Cloud browser** destination.
///
/// A native grouped `List` surface that lets the user review and configure how the agent opens sites:
/// a default permission, a recent-sites list, per-site access, saved logins (view / add / edit /
/// remove), cookies & sessions, and the destructive clear-data paths — each with the authored
/// confirmation. Every nested screen is owned here through the **outer host `NavigationStack`**: this
/// view never nests its own `NavigationStack`; all nested screens use typed value routes
/// registered once at this stable root, so the host chrome supplies
/// native Back, and edit mode swaps the toolbar for Cancel / Save in place.
///
/// Authority: `docs/playground/settings-design/settings-destinations.md` (Cloud browser) and the raw
/// node contexts / screenshots for `1833:5071`, `1868:6148/6153/6176/6197/6214/6227/6245/6250/6255`,
/// `1875:6710/6711/6712/6713/6714`, `1934:9071`, `1956:8160/8161/8162`. Compose sibling:
/// `screens/SettingsCloudBrowserScreen.kt`.
///
/// Prototype-only: all text entry is illustrative fixture data; passwords are always masked and never
/// written to the Keychain, shared preferences, the network, or a real browser. State lives only in
/// the in-memory `CloudBrowserModel` for this playground session.
public struct SettingsCloudBrowserScreen: View {
    @StateObject private var model = CloudBrowserModel()

    /// Deterministic zero-argument entry point. The host registers this as the `cloudBrowser` route
    /// inside its `NavigationStack`; this view owns everything below the Cloud browser root.
    public init() {}

    public var body: some View {
        CloudBrowserRootList(model: model)
            .navigationTitle("Cloud browser")
            .inlineNavTitle()
            .accessibilityIdentifier("cloudBrowser.root")
            .navigationDestination(for: CloudBrowserRoute.self) { route in
                switch route {
                case .sites: CloudSitesList(model: model)
                case .addSite: CloudAddSiteForm(model: model)
                case let .site(id): CloudSiteDetail(model: model, siteID: id)
                case let .addLogin(id): CloudAddLoginForm(model: model, siteID: id)
                case let .cookies(id): CloudCookiesView(model: model, siteID: id)
                case let .savedLogin(siteID, loginID):
                    CloudSavedLoginView(model: model, siteID: siteID, loginID: loginID)
                }
            }
    }
}

private enum CloudBrowserRoute: Hashable {
    case sites, addSite
    case site(UUID), addLogin(UUID), cookies(UUID)
    case savedLogin(siteID: UUID, loginID: UUID)
}

// MARK: - Root

private struct CloudBrowserRootList: View {
    @ObservedObject var model: CloudBrowserModel
    @State private var confirmClearAll = false

    var body: some View {
        List {
            Section {
                CloudPermissionRow(title: "Default permission",
                                   subtitle: "Ask before Rem opens a new site.",
                                   selection: $model.defaultPermission)
            } header: { HStack { Text("Default access").textCase(nil) } }.listRowBackground(DesignTokens.Color.backgroundSecondary)

            Section {
                ForEach(model.recentSites) { site in
                    CloudSiteRow(site: site)
                }
                NavigationLink("See all sites", value: CloudBrowserRoute.sites)
                    .cloudLinkStyle()
                    .accessibilityIdentifier("cloudBrowser.seeAllSites")
            } header: {
                HStack {
                    Text("Sites").textCase(nil)
                    Spacer()
                    NavigationLink("Add site", value: CloudBrowserRoute.addSite)
                        .font(.body).foregroundStyle(DesignTokens.Color.brandBlue).textCase(nil)
                }
                    .accessibilityIdentifier("cloudBrowser.addSite")
            } footer: {
                Text("Recent sites appear here. Add a site to configure its access.")
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)

            Section {
                Button(role: .destructive) { confirmClearAll = true } label: {
                    CloudLabel("Cookies and sessions across every site.",
                               title: "Clear all site data", tint: .red)
                }
                .accessibilityIdentifier("cloudBrowser.clearAllData")
            } header: { HStack { Text("Browser data").textCase(nil) } } footer: {
                Text("Saved passwords remain until you remove them.")
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
        }
        .cloudListStyle()
        .confirmationDialog("Clear data for all sites?", isPresented: $confirmClearAll,
                            titleVisibility: .visible) {
            Button("Clear all site data", role: .destructive) { model.clearAllSiteData() }
                .accessibilityIdentifier("cloudBrowser.confirmClearAll")
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Rem will be signed out of all sites. Saved logins are kept.")
        }
    }
}

// MARK: - All sites

private struct CloudSitesList: View {
    @ObservedObject var model: CloudBrowserModel

    var body: some View {
        List {
            Section {
                ForEach(model.sites) { site in
                    CloudSiteRow(site: site)
                }
            } header: { HStack { Text("Sites").textCase(nil) } }.listRowBackground(DesignTokens.Color.backgroundSecondary)
        }
        .cloudListStyle()
        .navigationTitle("Sites")
        .inlineNavTitle()
        .accessibilityIdentifier("cloudBrowser.sitesList")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                NavigationLink("Add", value: CloudBrowserRoute.addSite)
                    .accessibilityIdentifier("cloudBrowser.sites.addSite")
            }
        }
    }
}

// MARK: - Add site

private struct CloudAddSiteForm: View {
    @ObservedObject var model: CloudBrowserModel
    @Environment(\.dismiss) private var dismiss

    @State private var urlDraft = ""
    @State private var permission: CloudSitePermission = .ask
    @State private var username = ""
    @State private var password = ""
    @FocusState private var domainFocused: Bool

    private var canSave: Bool { CloudBrowserModel.canAddSite(urlDraft: urlDraft) }

    var body: some View {
        List {
            Section {
                CloudURLField(label: "Enter domain or URL", placeholder: "https://example.com",
                              text: $urlDraft, focused: $domainFocused)
                    .accessibilityIdentifier("cloudBrowser.addSite.domainField")
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
            Section {
                CloudPermissionRow(title: "Permission",
                                   subtitle: "Choose how Rem should handle this site.",
                                   selection: $permission)
            } header: { HStack { Text("Access").textCase(nil) } }.listRowBackground(DesignTokens.Color.backgroundSecondary)
            Section {
                CloudTextField(placeholder: "Username or email (optional)", text: $username)
                    .accessibilityIdentifier("cloudBrowser.addSite.username")
                CloudSecureField(placeholder: "Password (optional)", text: $password)
                    .accessibilityIdentifier("cloudBrowser.addSite.password")
            } header: { HStack { Text("Login details").textCase(nil) } } footer: {
                Text("Save a login now, or add one later from the site's detail screen.")
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
        }
        .cloudListStyle()
        .navigationTitle("Add site")
        .inlineNavTitle()
        .accessibilityIdentifier("cloudBrowser.addSiteForm")
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") {
                    if model.addSite(urlDraft: urlDraft, permission: permission,
                                     username: username, password: password) != nil {
                        dismiss()
                    }
                }
                .disabled(!canSave)
                .accessibilityIdentifier("cloudBrowser.addSite.save")
            }
        }
        .onDisappear { urlDraft = ""; username = ""; password = "" }
    }
}

// MARK: - Site detail

private struct CloudSiteDetail: View {
    @ObservedObject var model: CloudBrowserModel
    let siteID: UUID

    @State private var confirmClearSite = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Group {
            if let site = model.site(siteID) {
                List {
                    Section {
                        CloudPermissionRow(title: "Permission",
                                           subtitle: "Controls whether Rem can open this site.",
                                           selection: Binding(
                                            get: { site.permission },
                                            set: { model.setPermission($0, for: siteID) }))
                    } header: { HStack { Text("Access").textCase(nil) } }.listRowBackground(DesignTokens.Color.backgroundSecondary)

                    Section {
                        ForEach(site.logins) { login in
                            NavigationLink(value: CloudBrowserRoute.savedLogin(siteID: siteID, loginID: login.id)) {
                                CloudLabel("Password saved securely", title: login.username)
                            }
                                .accessibilityIdentifier("cloudBrowser.siteDetail.login")
                        }
                        NavigationLink("Add login", value: CloudBrowserRoute.addLogin(siteID))
                            .cloudLinkStyle()
                            .accessibilityIdentifier("cloudBrowser.siteDetail.addLogin")
                    } header: { HStack { Text("Saved logins").textCase(nil) } }.listRowBackground(DesignTokens.Color.backgroundSecondary)

                    Section {
                        NavigationLink(value: CloudBrowserRoute.cookies(siteID)) {
                            CloudLabel(site.cookieSummary, title: "Cookies & sessions")
                        }
                            .accessibilityIdentifier("cloudBrowser.siteDetail.cookies")
                        Button(role: .destructive) { confirmClearSite = true } label: {
                            CloudLabel("Signs Rem out of \(site.domain).",
                                       title: "Clear site data", tint: .red)
                        }
                        .accessibilityIdentifier("cloudBrowser.siteDetail.clearSiteData")
                    } header: { HStack { Text("Site data").textCase(nil) } }.listRowBackground(DesignTokens.Color.backgroundSecondary)
                }
                .cloudListStyle()
                .navigationTitle(site.domain)
                .inlineNavTitle()
                .accessibilityIdentifier("cloudBrowser.siteDetail")
                .confirmationDialog("Clear data for \(site.domain)?", isPresented: $confirmClearSite,
                                    titleVisibility: .visible) {
                    Button("Clear site data", role: .destructive) { model.clearSiteData(siteID: siteID) }
                        .accessibilityIdentifier("cloudBrowser.siteDetail.confirmClear")
                    Button("Cancel", role: .cancel) {}
                } message: {
                    Text("Rem will be signed out of this site. Saved logins are kept.")
                }
            } else {
                Color.clear.onAppear { dismiss() }
            }
        }
    }
}

// MARK: - Add login

private struct CloudAddLoginForm: View {
    @ObservedObject var model: CloudBrowserModel
    let siteID: UUID
    @Environment(\.dismiss) private var dismiss

    @State private var username = ""
    @State private var password = ""

    private var domain: String { model.site(siteID)?.domain ?? "this site" }
    private var canSave: Bool { CloudBrowserModel.canAddLogin(username: username, password: password) }

    var body: some View {
        List {
            Section {
                CloudLabel("Login will be available only for this site.", title: domain)
            } header: { HStack { Text("Website").textCase(nil) } }.listRowBackground(DesignTokens.Color.backgroundSecondary)
            Section {
                CloudLoginField(label: "Username or email", text: $username)
                    .accessibilityIdentifier("cloudBrowser.addLogin.username")
                CloudLoginField(label: "Password", text: $password, secure: true)
                    .accessibilityIdentifier("cloudBrowser.addLogin.password")
            } header: { HStack { Text("Login details").textCase(nil) } } footer: {
                Text("Rem uses this login only when you authorize access to \(domain).")
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
        }
        .cloudListStyle()
        .navigationTitle("Add login")
        .inlineNavTitle()
        .accessibilityIdentifier("cloudBrowser.addLoginForm")
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") {
                    if model.addLogin(siteID: siteID, username: username, password: password) != nil {
                        dismiss()
                    }
                }
                .disabled(!canSave)
                .accessibilityIdentifier("cloudBrowser.addLogin.save")
            }
        }
        .onDisappear { username = ""; password = "" }
    }
}

// MARK: - Saved login (view / edit)

private struct CloudSavedLoginView: View {
    @ObservedObject var model: CloudBrowserModel
    let siteID: UUID
    let loginID: UUID

    private enum Field { case none, username, password }
    @State private var editing: Field = .none
    @State private var draft = ""
    @State private var confirmRemove = false
    @FocusState private var fieldFocused: Bool
    @Environment(\.dismiss) private var dismiss

    private var domain: String { model.site(siteID)?.domain ?? "this site" }
    private var canSave: Bool { !draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }

    var body: some View {
        Group {
            if let site = model.site(siteID), let login = site.logins.first(where: { $0.id == loginID }) {
                List {
                    Section {
                        CloudLabel("This credential is scoped to this site.", title: site.domain)
                    } header: { HStack { Text("Website").textCase(nil) } }.listRowBackground(DesignTokens.Color.backgroundSecondary)

                    Section {
                        usernameRow(login)
                        passwordRow(login)
                    } header: { HStack { Text("Login details").textCase(nil) } } footer: {
                        Text("Illustrative values. Saved credentials require secure storage and explicit authorization.")
                    }.listRowBackground(DesignTokens.Color.backgroundSecondary)

                    Section {
                        Button(role: .destructive) { confirmRemove = true } label: {
                            Text("Remove login").frame(maxWidth: .infinity)
                        }
                        .accessibilityIdentifier("cloudBrowser.savedLogin.remove")
                    }.listRowBackground(DesignTokens.Color.backgroundSecondary)
                }
                .cloudListStyle()
                .navigationTitle("Saved login")
                .inlineNavTitle()
                .accessibilityIdentifier("cloudBrowser.savedLogin")
                .hideBackButton(editing != .none)
                .toolbar {
                    if editing != .none {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { cancelEditing() }
                                .accessibilityIdentifier("cloudBrowser.savedLogin.cancel")
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Save") { commit() }
                                .disabled(!canSave)
                                .accessibilityIdentifier("cloudBrowser.savedLogin.save")
                        }
                    }
                }
                .onChange(of: editing) { _, newValue in fieldFocused = newValue != .none }
                .onDisappear { cancelEditing() }
                .confirmationDialog("Remove saved login?", isPresented: $confirmRemove,
                                    titleVisibility: .visible) {
                    Button("Remove login", role: .destructive) {
                        model.removeLogin(siteID: siteID, loginID: loginID)
                        dismiss()
                    }
                    .accessibilityIdentifier("cloudBrowser.savedLogin.confirmRemove")
                    Button("Cancel", role: .cancel) {}
                } message: {
                    Text("This removes the saved credential for \(domain).")
                }
            } else {
                Color.clear.onAppear { dismiss() }
            }
        }
    }

    @ViewBuilder private func usernameRow(_ login: CloudSavedLogin) -> some View {
        if editing == .username {
            CloudEditableField(label: "Username or email", placeholder: "Username or email",
                               text: $draft, focused: $fieldFocused, secure: false)
                .accessibilityIdentifier("cloudBrowser.savedLogin.usernameField")
        } else {
            CloudEditRow(title: "Username or email", value: login.username,
                         editIdentifier: "cloudBrowser.savedLogin.editUsername",
                         showsPencil: editing == .none) { start(.username, with: login.username) }
        }
    }

    @ViewBuilder private func passwordRow(_ login: CloudSavedLogin) -> some View {
        if editing == .password {
            CloudEditableField(label: "Password", placeholder: "Password",
                               text: $draft, focused: $fieldFocused, secure: true)
                .accessibilityIdentifier("cloudBrowser.savedLogin.passwordField")
        } else {
            CloudEditRow(title: "Password", value: login.maskedPassword,
                         editIdentifier: "cloudBrowser.savedLogin.editPassword",
                         showsPencil: editing == .none) { start(.password, with: "") }
        }
    }

    private func start(_ field: Field, with value: String) {
        draft = value
        editing = field
    }

    private func commit() {
        switch editing {
        case .username: model.updateUsername(siteID: siteID, loginID: loginID, to: draft)
        case .password: model.updatePassword(siteID: siteID, loginID: loginID, to: draft)
        case .none: break
        }
        cancelEditing()
    }

    private func cancelEditing() {
        draft = ""
        editing = .none
        fieldFocused = false
    }
}

// MARK: - Cookies & sessions

private struct CloudCookiesView: View {
    @ObservedObject var model: CloudBrowserModel
    let siteID: UUID
    @State private var confirmClear = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Group {
            if let site = model.site(siteID) {
                List {
                    Section {
                        CloudLabel(site.signedIn ? "Signed in" : "Signed out", title: "Session")
                        CloudLabel(site.illustrativeCookies, title: "Cookies")
                    } header: { HStack { Text(site.domain).textCase(nil) } } footer: {
                        Text("Clearing cookies signs Rem out of this site. Saved logins are separate.")
                    }.listRowBackground(DesignTokens.Color.backgroundSecondary)
                    Section {
                        Button(role: .destructive) { confirmClear = true } label: {
                            Text("Clear site data").frame(maxWidth: .infinity, alignment: .center)
                        }
                        .accessibilityIdentifier("cloudBrowser.cookies.clearSiteData")
                    }.listRowBackground(DesignTokens.Color.backgroundSecondary)
                }
                .cloudListStyle()
                .navigationTitle("Cookies & sessions")
                .inlineNavTitle()
                .accessibilityIdentifier("cloudBrowser.cookies")
                .confirmationDialog("Clear data for \(site.domain)?", isPresented: $confirmClear,
                                    titleVisibility: .visible) {
                    Button("Clear site data", role: .destructive) { model.clearSiteData(siteID: siteID) }
                        .accessibilityIdentifier("cloudBrowser.cookies.confirmClear")
                    Button("Cancel", role: .cancel) {}
                } message: {
                    Text("Rem will be signed out of this site. Saved logins are kept.")
                }
            } else {
                Color.clear.onAppear { dismiss() }
            }
        }
    }
}

// MARK: - Shared row pieces

/// A plain title/subtitle label, reusing the canonical `ListRowLabel`. `title` is the primary 17pt
/// line; the leading argument is the 13pt secondary line (so call sites read top-to-bottom).
private struct CloudLabel: View {
    let subtitle: String
    let title: String
    var tint: Color?
    init(_ subtitle: String, title: String, tint: Color? = nil) {
        self.subtitle = subtitle; self.title = title; self.tint = tint
    }
    var body: some View {
        if let tint {
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.body).foregroundStyle(tint)
                Text(subtitle).font(.footnote).foregroundStyle(DesignTokens.Color.labelSecondary)
            }
            .frame(minHeight: 40)
        } else {
            ListRowLabel(title, subtitle: subtitle).frame(minHeight: 40)
        }
    }
}

/// Native value link; the List supplies its disclosure and the root resolves the route.
private struct CloudSiteRow: View {
    let site: CloudSite
    var body: some View {
        NavigationLink(value: CloudBrowserRoute.site(site.id)) {
            ListRowLabel(site.domain, subtitle: site.rowSubtitle)
        }
        .accessibilityIdentifier("cloudBrowser.site.\(site.domain)")
    }
}

/// A read row with a trailing pencil edit affordance (the CRUD "update" entry point). The whole row is
/// not tappable; only the pencil starts editing, matching the authored masters.
private struct CloudEditRow: View {
    let title: String
    let value: String
    let editIdentifier: String
    let showsPencil: Bool
    let onEdit: () -> Void
    var body: some View {
        HStack(spacing: DesignTokens.Spacing.sm) {
            ListRowLabel(title, subtitle: value).frame(maxWidth: .infinity, alignment: .leading)
            if showsPencil {
                Button(action: onEdit) {
                    Image(systemName: "pencil")
                        .font(.body)
                        .foregroundStyle(DesignTokens.Color.labelTertiary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Edit \(title.lowercased())")
                .accessibilityIdentifier(editIdentifier)
            }
        }
        .frame(minHeight: 44)
    }
}

/// The inline-edit field: a subordinate label over a focused native input (the row keeps its geometry
/// while the persistent label demotes to secondary). Passwords use a `SecureField`, always masked.
private struct CloudEditableField: View {
    let label: String
    let placeholder: String
    @Binding var text: String
    var focused: FocusState<Bool>.Binding
    let secure: Bool
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label).font(.footnote).foregroundStyle(DesignTokens.Color.labelSecondary)
            Group {
                if secure {
                    SecureField(placeholder, text: $text, prompt: Text(placeholder).foregroundStyle(DesignTokens.Color.labelSecondary))
                } else {
                    TextField(placeholder, text: $text, prompt: Text(placeholder).foregroundStyle(DesignTokens.Color.labelSecondary)).noAutocap()
                }
            }
            .font(.body)
            .focused(focused)
        }
        .frame(minHeight: 44)
    }
}

/// Add-site domain field: a subordinate label appears above the input once focused or nonempty, and
/// the placeholder switches from the empty prompt to the URL example — matching the authored empty vs
/// focused states.
private struct CloudURLField: View {
    let label: String
    let placeholder: String
    @Binding var text: String
    var focused: FocusState<Bool>.Binding
    private var showsLabel: Bool { focused.wrappedValue || !text.isEmpty }
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            if showsLabel {
                Text(label).font(.footnote).foregroundStyle(DesignTokens.Color.labelSecondary)
            }
            TextField(showsLabel ? placeholder : label, text: $text,
                      prompt: Text(showsLabel ? placeholder : label).foregroundStyle(DesignTokens.Color.labelSecondary))
                .font(.body)
                .urlKeyboard()
                .noAutocap()
                .focused(focused)
        }
        .frame(minHeight: 44)
    }
}

/// Add-login rows retain their label when filled; focusing demotes it above the editable value.
/// Empty forms remain unfocused until the user taps, preserving both authored empty/focus states.
private struct CloudLoginField: View {
    let label: String
    @Binding var text: String
    var secure = false
    @FocusState private var focused: Bool

    private var showsLabel: Bool { focused || !text.isEmpty }
    private var filledResting: Bool { !focused && !text.isEmpty }

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            if showsLabel {
                Text(label)
                    .font(filledResting ? .body : .footnote)
                    .foregroundStyle(filledResting ? DesignTokens.Color.labelPrimary : DesignTokens.Color.labelSecondary)
            }
            Group {
                if secure {
                    SecureField(label, text: $text,
                                prompt: Text(showsLabel ? "" : label).foregroundStyle(DesignTokens.Color.labelSecondary))
                } else {
                    TextField(label, text: $text,
                              prompt: Text(showsLabel ? "" : label).foregroundStyle(DesignTokens.Color.labelSecondary)).noAutocap()
                }
            }
            .font(filledResting ? .footnote : .body)
            .frame(maxWidth: .infinity, minHeight: 20, alignment: .leading)
            .foregroundStyle(filledResting ? DesignTokens.Color.labelSecondary : DesignTokens.Color.labelPrimary)
            .focused($focused)
            .onSubmit { focused = false }
        }
        .frame(minHeight: 44)
    }
}

private struct CloudTextField: View {
    let placeholder: String
    @Binding var text: String
    var focused: FocusState<Bool>.Binding? = nil
    var body: some View {
        let field = TextField(placeholder, text: $text, prompt: Text(placeholder).foregroundStyle(DesignTokens.Color.labelSecondary)).font(.body).noAutocap().frame(minHeight: 44)
        if let focused { field.focused(focused) } else { field }
    }
}

private struct CloudSecureField: View {
    let placeholder: String
    @Binding var text: String
    var body: some View {
        SecureField(placeholder, text: $text, prompt: Text(placeholder).foregroundStyle(DesignTokens.Color.labelSecondary)).font(.body).frame(minHeight: 44)
    }
}

/// The trailing value menu ("Ask ⌄"), restricted to the observed Ask / Allow policies.
private struct CloudPermissionRow: View {
    let title: String
    let subtitle: String
    @Binding var selection: CloudSitePermission
    var body: some View {
        HStack(spacing: DesignTokens.Spacing.sm) {
            ListRowLabel(title, subtitle: subtitle).frame(maxWidth: .infinity, alignment: .leading)
            Picker("Permission", selection: $selection) {
                ForEach(CloudSitePermission.allCases, id: \.self) { Text($0.rawValue).tag($0) }
            }
            .labelsHidden()
            .pickerStyle(.menu)
            .accessibilityIdentifier("cloudBrowser.permissionMenu")
        }
        .frame(minHeight: 44)
    }
}

// MARK: - Platform helpers (iOS-only modifiers no-op elsewhere)

private extension View {
    @ViewBuilder func cloudListStyle() -> some View {
        #if os(iOS)
        self.listStyle(.insetGrouped).scrollContentBackground(.hidden)
            .background(DesignTokens.Color.backgroundPrimary).environment(\.defaultMinListRowHeight, 44).textCase(nil)
        #else
        self.listStyle(.inset).scrollContentBackground(.hidden)
            .background(DesignTokens.Color.backgroundPrimary).environment(\.defaultMinListRowHeight, 44).textCase(nil)
        #endif
    }

    @ViewBuilder func cloudLinkStyle() -> some View {
        self.font(.body).foregroundStyle(DesignTokens.Color.brandBlue)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder func inlineNavTitle() -> some View {
        #if os(iOS)
        self.navigationBarTitleDisplayMode(.inline)
        #else
        self
        #endif
    }

    @ViewBuilder func hideBackButton(_ hidden: Bool) -> some View {
        #if os(iOS)
        self.navigationBarBackButtonHidden(hidden)
        #else
        self
        #endif
    }

    @ViewBuilder func urlKeyboard() -> some View {
        #if os(iOS)
        self.keyboardType(.URL)
        #else
        self
        #endif
    }

    @ViewBuilder func noAutocap() -> some View {
        #if os(iOS)
        self.textInputAutocapitalization(.never).autocorrectionDisabled()
        #else
        self.autocorrectionDisabled()
        #endif
    }
}

#if DEBUG
#Preview("Cloud browser") {
    NavigationStack { SettingsCloudBrowserScreen() }
}
#endif
