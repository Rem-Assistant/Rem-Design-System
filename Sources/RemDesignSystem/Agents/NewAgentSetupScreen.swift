import SwiftUI

/// **NewAgentSetupScreen** — the "New agent" sheet, reached from the sidebar's **New agent** button.
///
/// Name + colour only (Samuel, 2026-10-11). Top to bottom: a header with a top-leading close (X)
/// circle button and the centred "New agent" title; a large `AgentAvatar` preview tinted live with the
/// selected colour; the name in a centred, filled pill field (the first, focused field); the
/// `AgentColor` palette as a wrapping grid of swatches (selected = ring); and a bottom-pinned
/// **Create agent** (`Button · Rect · Black`) that stays disabled until the trimmed name is non-blank.
///
/// The screen draws its own header, so the host presents it directly as a sheet (no navigation bar
/// needed). Display + callbacks only — the host owns creating the agent, its pending/error states, and
/// opening the new agent's first chat. Compose sibling: `agents/NewAgentSetupScreen.kt`.
public struct NewAgentSetupScreen: View {
    @Binding private var draft: NewAgentDraft
    private let focusesNameOnAppear: Bool
    private let onAction: (NewAgentSetupAction) -> Void
    @FocusState private var nameFocused: Bool

    public init(
        draft: Binding<NewAgentDraft>,
        focusesNameOnAppear: Bool = true,
        onAction: @escaping (NewAgentSetupAction) -> Void
    ) {
        self._draft = draft
        self.focusesNameOnAppear = focusesNameOnAppear
        self.onAction = onAction
    }

    public var body: some View {
        VStack(spacing: 0) {
            header
            ScrollView {
                VStack(spacing: DesignTokens.Spacing.xl) {
                    AgentAvatar(.side(draft.look), size: NewAgentSetupLayout.avatarSize)
                        .animation(.snappy, value: draft.color)
                        .accessibilityIdentifier("newAgent.avatar")
                    nameField
                    colorGrid
                }
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.top, DesignTokens.Spacing.lg)
                .padding(.bottom, DesignTokens.Spacing.xl)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) { createBar }
        .background(DesignTokens.Color.backgroundPrimary.ignoresSafeArea())
        .onAppear { if focusesNameOnAppear { nameFocused = true } }
    }

    // MARK: Header — close (X) + title

    private var header: some View {
        HStack(spacing: DesignTokens.Spacing.sm) {
            Button { onAction(.cancel) } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .frame(width: 44, height: 44)
                    .background(DesignTokens.Color.backgroundSecondary, in: Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(NewAgentSetupCopy.close)
            .accessibilityIdentifier("newAgent.cancel")

            Text(NewAgentSetupCopy.title)
                .font(DesignTokens.Typography.bodyBold)
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .accessibilityAddTraits(.isHeader)

            // Balances the close button so the title stays optically centred.
            Color.clear.frame(width: 44, height: 44).accessibilityHidden(true)
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.top, DesignTokens.Spacing.md)
    }

    // MARK: Name

    private var nameField: some View {
        TextField(NewAgentSetupCopy.namePlaceholder, text: $draft.name)
            .font(DesignTokens.Typography.title3.weight(.semibold))
            .foregroundStyle(DesignTokens.Color.labelPrimary)
            .multilineTextAlignment(.center)
            .focused($nameFocused)
            .submitLabel(.done)
        #if os(iOS)
            .textInputAutocapitalization(.words)
        #endif
            .padding(.horizontal, DesignTokens.Spacing.xl)
            .padding(.vertical, DesignTokens.Spacing.md)
            .frame(maxWidth: .infinity, minHeight: NewAgentSetupLayout.nameFieldMinHeight)
            .background(DesignTokens.Color.fillTertiary, in: Capsule())
            .accessibilityLabel(NewAgentSetupCopy.nameLabel)
            .accessibilityIdentifier("newAgent.name")
    }

    // MARK: Colour

    private var colorGrid: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            ForEach(Array(NewAgentSetupLayout.swatchRows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: DesignTokens.Spacing.md) {
                    ForEach(row) { color in swatch(color) }
                }
            }
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(NewAgentSetupCopy.colorsLabel)
        .accessibilityIdentifier("newAgent.colors")
    }

    private func swatch(_ color: AgentColor) -> some View {
        let selected = draft.color == color
        return Button { draft.color = color } label: {
            Circle()
                .fill(color.swatch)
                .frame(width: NewAgentSetupLayout.swatchDisc, height: NewAgentSetupLayout.swatchDisc)
                .frame(width: NewAgentSetupLayout.swatchTarget, height: NewAgentSetupLayout.swatchTarget)
                .overlay(Circle().strokeBorder(DesignTokens.Color.labelPrimary, lineWidth: selected ? 2.5 : 0))
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(color.accessibilityName)
        .accessibilityAddTraits(selected ? [.isSelected] : [])
        .accessibilityIdentifier("newAgent.color.\(color.rawValue)")
    }

    // MARK: Create

    private var createBar: some View {
        Button(NewAgentSetupCopy.create) {
            if let action = draft.createAction { onAction(action) }
        }
        .remButton(.rectBlack)
        .disabled(!draft.canCreate)
        .accessibilityHint(draft.canCreate ? "" : NewAgentSetupCopy.createHint)
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.top, DesignTokens.Spacing.sm)
        .padding(.bottom, DesignTokens.Spacing.lg)
        .background(DesignTokens.Color.backgroundPrimary)
        .accessibilityIdentifier("newAgent.create")
    }
}

#if DEBUG
private struct NewAgentSetupPreview: View {
    @State private var draft = NewAgentDraft()
    var body: some View {
        NewAgentSetupScreen(draft: $draft) { _ in }
    }
}

#Preview("NewAgentSetupScreen") {
    NewAgentSetupPreview()
}
#endif
