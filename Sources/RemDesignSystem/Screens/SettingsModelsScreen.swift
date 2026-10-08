import SwiftUI

// Models destination for the bounded Settings playground — Figma masters `Screen/Models`
// (`1833:5109`) and `Screen/Add provider key · Working states` (`1956:8163`: empty `1934:9067`,
// provider picker `1934:9068`, filled `1934:9069`, editing `1934:9070`) with the recovered provider
// menu `1870:54593`.
//
// Code-only prototype: the "Auto" managed-model toggle, the Anthropic availability switch, and the
// saved-key flag are independent in-memory fixture values. No provider is contacted, no key is
// validated, and no credential is persisted or stored — Save records only a dummy saved-key flag and
// never retains the key draft. Zero-argument, deterministic entry point (`SettingsModelsScreen`);
// nested navigation rides the host's `NavigationStack`.

/// The five API-key providers from the recovered picker menu (`1870:54593`), in source order.
public enum ModelProvider: String, CaseIterable, Hashable, Sendable {
    case anthropic = "Anthropic"
    case openAI = "OpenAI"
    case google = "Google"
    case mistral = "Mistral"
    case openRouter = "OpenRouter"
}

/// Deterministic Models fixture. `autoManagedModel` and the per-provider saved/available flags are
/// independent — the source settles no Auto↔provider mutual-exclusion, so none is invented.
public struct ModelsFixture: Sendable {
    public var autoManagedModel: Bool
    public var savedProviders: Set<ModelProvider>
    public var availableProviders: Set<ModelProvider>

    /// Defaults match the source: Auto on, Anthropic has a saved key, Anthropic availability off.
    public init(autoManagedModel: Bool = true,
                savedProviders: Set<ModelProvider> = [.anthropic],
                availableProviders: Set<ModelProvider> = []) {
        self.autoManagedModel = autoManagedModel
        self.savedProviders = savedProviders
        self.availableProviders = availableProviders
    }

    public func isSaved(_ provider: ModelProvider) -> Bool { savedProviders.contains(provider) }
    public func isAvailable(_ provider: ModelProvider) -> Bool { availableProviders.contains(provider) }

    public mutating func setAvailable(_ provider: ModelProvider, _ on: Bool) {
        if on { availableProviders.insert(provider) } else { availableProviders.remove(provider) }
    }

    /// Records a dummy saved-key flag for the provider. The key value is intentionally NOT a
    /// parameter — nothing about the draft is retained, matching the no-credential-storage boundary.
    public mutating func recordSavedKey(for provider: ModelProvider) {
        savedProviders.insert(provider)
    }

    public static let autoFooter =
        "Rem chooses a managed model for each question based on the task, availability, and cost."
    public static let keyFooter =
        "Enter an API key from your provider to use its models."

    /// Save is enabled only for a nonempty key draft. Validation beyond non-emptiness is a source gap.
    public static func canSave(keyDraft: String) -> Bool {
        !keyDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}

private enum ModelsRoute: Hashable { case addKey(ModelProvider) }

/// Models root (`1833:5109`). Zero-argument, deterministic entry point.
public struct SettingsModelsScreen: View {
    @State private var fixture = ModelsFixture()

    public init() {}

    public var body: some View {
        List {
            Section {
                Toggle(isOn: $fixture.autoManagedModel) {
                    ListRowLabel("Auto", subtitle: "Rem\u{2019}s managed model")
                }
                .tint(DesignTokens.Color.systemGreen)
                .accessibilityIdentifier("models.toggle.auto")
            } header: {
                Text("Rem").textCase(nil)
            } footer: {
                Text(ModelsFixture.autoFooter)
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)

            Section {
                // Navigable content (opens the key editor) paired with its disclosure chevron, with the
                // availability switch reserved for the trailing slot — one interaction per region.
                HStack(spacing: DesignTokens.Spacing.md) {
                    NavigationLink(value: ModelsRoute.addKey(.anthropic)) {
                        ListRowLabel("Anthropic",
                                     subtitle: fixture.isSaved(.anthropic) ? "Saved" : nil)
                    }
                    .accessibilityIdentifier("models.providerRow.anthropic")
                    Toggle(isOn: Binding(
                        get: { fixture.isAvailable(.anthropic) },
                        set: { fixture.setAvailable(.anthropic, $0) }
                    )) { EmptyView() }
                    .labelsHidden()
                    .tint(DesignTokens.Color.systemGreen)
                    .accessibilityLabel("Anthropic availability")
                    .accessibilityIdentifier("models.toggle.anthropicAvailable")
                }

                NavigationLink(value: ModelsRoute.addKey(.anthropic)) {
                    ListRowLabel("Add provider key")
                }
                .accessibilityIdentifier("models.addProviderKey")
            } header: {
                Text("API keys").textCase(nil)
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
        }
        .settingsDestinationList()
        .navigationTitle("Models")
        .settingsInlineNavigationTitle()
        .accessibilityIdentifier("settingsModels")
        .accessibilityHint(PlaygroundMockData.hint)
        .navigationDestination(for: ModelsRoute.self) { route in
            switch route {
            case let .addKey(provider):
                AddProviderKeyView(initialProvider: provider) { fixture.recordSavedKey(for: $0) }
            }
        }
    }
}

/// Add provider key (`1956:8163`). Native nav push: Back discards the draft, a single toolbar Save is
/// disabled until the key draft is nonempty. The key uses a native password-style field (always
/// masked); the draft lives only in this view's `@State` and is never written to the Keychain, shared
/// preferences, or the network. Save records the dummy saved-key flag and pops.
private struct AddProviderKeyView: View {
    let initialProvider: ModelProvider
    let onSave: (ModelProvider) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var provider: ModelProvider
    @State private var keyDraft = ""
    @FocusState private var keyFocused: Bool

    init(initialProvider: ModelProvider, onSave: @escaping (ModelProvider) -> Void) {
        self.initialProvider = initialProvider
        self.onSave = onSave
        _provider = State(initialValue: initialProvider)
    }

    var body: some View {
        List {
            Section {
                HStack(spacing: DesignTokens.Spacing.md) {
                    Text("Provider")
                        .font(DesignTokens.Typography.body)
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                    Spacer(minLength: DesignTokens.Spacing.sm)
                    Menu {
                        ForEach(ModelProvider.allCases, id: \.self) { candidate in
                            Button {
                                provider = candidate
                            } label: {
                                if candidate == provider {
                                    Label(candidate.rawValue, systemImage: "checkmark")
                                } else {
                                    Text(candidate.rawValue)
                                }
                            }
                            .accessibilityIdentifier("models.providerOption.\(candidate.rawValue)")
                        }
                    } label: {
                        HStack(spacing: DesignTokens.Spacing.xs) {
                            Text(provider.rawValue)
                                .foregroundStyle(DesignTokens.Color.labelSecondary)
                            Image(systemName: "chevron.up.chevron.down")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(DesignTokens.Color.labelSecondary)
                        }
                    }
                    .accessibilityIdentifier("models.providerPicker")
                }

                VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
                    if keyFocused || !keyDraft.isEmpty {
                        Text("API key")
                            .font(DesignTokens.Typography.footnote)
                            .foregroundStyle(DesignTokens.Color.labelSecondary)
                            .accessibilityHidden(true)
                    }
                    SecureField("API key", text: $keyDraft)
                        .focused($keyFocused)
                        .font(DesignTokens.Typography.body)
                        .textContentType(.password)
                        .autocorrectionDisabled()
                        #if os(iOS)
                        .textInputAutocapitalization(.never)
                        #endif
                        .accessibilityIdentifier("models.keyField")
                }
            } header: {
                Text("API key").textCase(nil)
            } footer: {
                Text(ModelsFixture.keyFooter)
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
        }
        .settingsDestinationList()
        .navigationTitle("Add provider key")
        .settingsInlineNavigationTitle()
        .accessibilityIdentifier("modelsAddKey")
        .accessibilityHint(PlaygroundMockData.hint)
        .onDisappear { keyDraft = "" }
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") {
                    onSave(provider)
                    keyDraft = ""
                    dismiss()
                }
                .disabled(!ModelsFixture.canSave(keyDraft: keyDraft))
                .accessibilityIdentifier("models.saveKey")
            }
        }
    }
}

#if DEBUG
#Preview("Models") {
    NavigationStack { SettingsModelsScreen() }
}
#Preview("Add provider key") {
    NavigationStack { AddProviderKeyView(initialProvider: .anthropic) { _ in } }
}
#endif
