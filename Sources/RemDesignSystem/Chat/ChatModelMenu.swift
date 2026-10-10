import SwiftUI

/// One model a host can offer in the composer's model menu. Supplied at runtime — the design system
/// never ships a model catalog.
public struct ChatModelOption: Hashable, Identifiable, Sendable {
    public let id: String
    public let name: String

    public init(id: String, name: String) {
        self.id = id
        self.name = name
    }
}

/// A configured provider and the models it offers. Providers with no models are not listed.
public struct ChatModelProvider: Hashable, Identifiable, Sendable {
    public let id: String
    public let name: String
    public let models: [ChatModelOption]

    public init(id: String, name: String, models: [ChatModelOption]) {
        self.id = id
        self.name = name
        self.models = models
    }
}

/// What the composer will use: Automatic (the gateway's default routing) or one explicit model.
public enum ChatModelSelection: Hashable, Sendable {
    case automatic
    case model(id: String)
}

/// **ChatModelMenu** — the composer's model trigger and its native menu.
///
/// The trigger is the secondary pill (`fillTertiary` capsule, `chevron.up.chevron.down` + label). It
/// reads **"Auto"** while Automatic is selected, otherwise the selected model's name (or the host's
/// fallback label while the selection is not among `providers`). The menu is
/// source-aligned with the shipped picker (`SharedRemChatView` model menu): **Automatic** (checkmark
/// when selected), one submenu per configured provider listing its models (checkmark on the
/// selected one), then **Manage Models** when the host supplies a destination.
///
/// Everything listed comes from `providers` — fixtures pass fictional placeholders, production passes
/// the runtime catalog. Disabled (45% opacity) when the environment disables it, e.g. while sending.
///
/// Figma canonical: **Rem/Chat/Model menu** (`2656:128164`) and provider submenu (`2656:128245`);
/// trigger as drawn in **Composer** (`2071:11555`). Compose sibling: `chat/ChatModelMenu.kt`.
public struct ChatModelMenu: View {
    private let providers: [ChatModelProvider]
    private let selection: ChatModelSelection
    private let automaticLabel: String
    private let accessibilityPrefix: String
    private let onSelect: (ChatModelSelection) -> Void
    private let onManageModels: (() -> Void)?
    private let fallbackLabel: String?
    /// Set by `RemComposerBar` from `ChatComposerState.modelLabel`; used when `fallbackLabel` is nil.
    @Environment(\.composerModelLabel) private var composerModelLabel

    /// `fallbackLabel` is the host's label for an explicit selection the providers do not (yet) list —
    /// e.g. a bring-your-own-key model whose provider evidence is still pending. When nil, a menu placed
    /// in `RemComposerBar` uses the composer state's `modelLabel`. See `triggerLabel(for:providers:automaticLabel:fallbackLabel:)`.
    public init(
        providers: [ChatModelProvider],
        selection: ChatModelSelection,
        automaticLabel: String = "Auto",
        accessibilityPrefix: String = "composer",
        onSelect: @escaping (ChatModelSelection) -> Void,
        onManageModels: (() -> Void)? = nil,
        fallbackLabel: String? = nil
    ) {
        self.providers = providers
        self.selection = selection
        self.automaticLabel = automaticLabel
        self.accessibilityPrefix = accessibilityPrefix
        self.onSelect = onSelect
        self.onManageModels = onManageModels
        self.fallbackLabel = fallbackLabel
    }

    /// The trigger label for a selection: `automaticLabel` for Automatic, else the model's name. For an
    /// id the providers do not (yet) contain, the host's `fallbackLabel` (typically
    /// `ChatComposerState.modelLabel`) when it is non-blank and not the Automatic label, else the raw
    /// id — never disguised as Automatic.
    public static func triggerLabel(
        for selection: ChatModelSelection,
        providers: [ChatModelProvider],
        automaticLabel: String = "Auto",
        fallbackLabel: String? = nil
    ) -> String {
        switch selection {
        case .automatic:
            return automaticLabel
        case .model(let id):
            if let name = providers.lazy.flatMap(\.models).first(where: { $0.id == id })?.name { return name }
            if let fallback = fallbackLabel?.trimmingCharacters(in: .whitespacesAndNewlines),
               !fallback.isEmpty, fallback != automaticLabel {
                return fallback
            }
            return id
        }
    }

    private var resolvedLabel: String {
        Self.triggerLabel(for: selection, providers: providers, automaticLabel: automaticLabel,
                          fallbackLabel: fallbackLabel ?? composerModelLabel)
    }

    public var body: some View {
        Menu {
            Button { onSelect(.automatic) } label: {
                checkmarkLabel("Automatic", selected: selection == .automatic)
            }
            .accessibilityIdentifier("\(accessibilityPrefix).modelAutomatic")

            ForEach(providers.filter { !$0.models.isEmpty }) { provider in
                Menu(provider.name) {
                    ForEach(provider.models) { model in
                        Button { onSelect(.model(id: model.id)) } label: {
                            checkmarkLabel(model.name, selected: selection == .model(id: model.id))
                        }
                        .accessibilityIdentifier("\(accessibilityPrefix).model.\(model.id)")
                    }
                }
                .accessibilityIdentifier("\(accessibilityPrefix).modelProvider.\(provider.id)")
            }

            if let onManageModels {
                Divider()
                Button(action: onManageModels) {
                    Label("Manage Models", systemImage: "slider.horizontal.3")
                }
                .accessibilityIdentifier("\(accessibilityPrefix).manageModels")
            }
        } label: {
            ChatModelTriggerPill(label: resolvedLabel)
        }
        .menuStyle(.button)
        .menuIndicator(.hidden)
        .buttonStyle(.plain)
        // Vertical only: a host-supplied model name truncates (the pill's lineLimit(1)) instead of
        // pushing the composer's control row past a narrow screen.
        .fixedSize(horizontal: false, vertical: true)
        .accessibilityLabel("Model, \(resolvedLabel)")
        .accessibilityIdentifier("\(accessibilityPrefix).modelMenu")
    }

    @ViewBuilder
    private func checkmarkLabel(_ title: String, selected: Bool) -> some View {
        if selected {
            Label(title, systemImage: "checkmark")
        } else {
            Text(title)
        }
    }
}

private struct ComposerModelLabelKey: EnvironmentKey {
    static let defaultValue: String? = nil
}

extension EnvironmentValues {
    /// The enclosing composer's `ChatComposerState.modelLabel`, the default unresolved-selection label
    /// for a `ChatModelMenu` placed in `RemComposerBar`.
    var composerModelLabel: String? {
        get { self[ComposerModelLabelKey.self] }
        set { self[ComposerModelLabelKey.self] = newValue }
    }
}

/// The secondary-pill trigger, shared by `ChatModelMenu` and the display-only `RemComposerBar`.
/// Dims to 45% when the environment disables it (the composer disables it while sending).
struct ChatModelTriggerPill: View {
    let label: String
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        HStack(spacing: DesignTokens.Spacing.xs) {
            Image(systemName: "chevron.up.chevron.down")
                .font(.system(size: 10, weight: .semibold))
            Text(label)
                .font(DesignTokens.Typography.caption1)
                .lineLimit(1)
        }
        .foregroundStyle(DesignTokens.Color.labelPrimary)
        .padding(.horizontal, DesignTokens.Spacing.sm)
        .padding(.vertical, DesignTokens.Spacing.xs)
        .background(DesignTokens.Color.fillTertiary, in: Capsule())
        .opacity(isEnabled ? 1 : 0.45)
    }
}

/// Preview fixture. Kept out of the `#Preview` body: a multi-statement body cannot take part in the
/// macro's overload resolution (View / UIView / UIViewController), which Xcode 15 reports as
/// "Ambiguous use of 'init(_:traits:body:)'".
private enum ChatModelMenuPreviewData {
    static let providers = [
        ChatModelProvider(id: "provider-a", name: "Provider A", models: [
            ChatModelOption(id: "a-fast", name: "Fast model"),
            ChatModelOption(id: "a-deep", name: "Deep model"),
        ]),
    ]
}

#Preview {
    VStack(spacing: DesignTokens.Spacing.lg) {
        ChatModelMenu(providers: ChatModelMenuPreviewData.providers, selection: .automatic, onSelect: { _ in }, onManageModels: {})
        ChatModelMenu(providers: ChatModelMenuPreviewData.providers, selection: .model(id: "a-deep"), onSelect: { _ in })
        ChatModelMenu(providers: ChatModelMenuPreviewData.providers, selection: .automatic, onSelect: { _ in }).disabled(true)
    }
    .padding(DesignTokens.Spacing.lg)
    .background(DesignTokens.Color.backgroundSecondary)
}
