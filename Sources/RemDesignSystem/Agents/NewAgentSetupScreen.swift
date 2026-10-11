import SwiftUI

/// **NewAgentSetupScreen** — "New agent" setup, reached from the sidebar's **New agent** button.
///
/// Figma: **E · New agent — set up** (`2714:3538`). Top to bottom: the agent icon (tap to change) with
/// the colour swatches, **Name** (the first, focused field — naming the lane comes first), **What should
/// this agent handle?**, optional **Connected services**, and a bottom-pinned **Create agent**
/// (`Button · Rect · Black`) that stays disabled until the lane has a name.
///
/// Chrome is the platform navigation bar: the host presents this inside its own `NavigationStack`
/// (sheet or push); the screen supplies the "New agent" title and the Cancel action.
/// Display + callbacks only — the host owns creating the agent, its pending/error states, and opening
/// the new agent's first chat. Compose sibling: `agents/NewAgentSetupScreen.kt`.
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
        ScrollView {
            VStack(spacing: DesignTokens.Spacing.lg) {
                iconPicker
                nameSection
                handlesSection
                servicesSection
            }
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.top, DesignTokens.Spacing.sm)
            .padding(.bottom, DesignTokens.Spacing.lg)
        }
        .scrollDismissesKeyboard(.interactively)
        .safeAreaInset(edge: .bottom, spacing: 0) { createBar }
        .background(DesignTokens.Color.backgroundPrimary.ignoresSafeArea())
        .navigationTitle(NewAgentSetupCopy.title)
    #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
    #endif
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button(NewAgentSetupCopy.cancel) { onAction(.cancel) }
                    .accessibilityIdentifier("newAgent.cancel")
            }
        }
        .onAppear { if focusesNameOnAppear { nameFocused = true } }
    }

    // MARK: Icon + colour

    private var iconPicker: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Button { onAction(.changeIcon) } label: {
                AgentAvatar(.side(draft.look), size: 80)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(NewAgentSetupCopy.changeIcon)
            .accessibilityIdentifier("newAgent.icon")

            Text(NewAgentSetupCopy.changeIcon)
                .font(DesignTokens.Typography.footnote)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
                .accessibilityHidden(true)

            HStack(spacing: DesignTokens.Spacing.sm) {
                ForEach(AgentColor.allCases) { color in swatch(color) }
            }
            .accessibilityElement(children: .contain)
            .accessibilityIdentifier("newAgent.colors")
        }
        .frame(maxWidth: .infinity)
    }

    private func swatch(_ color: AgentColor) -> some View {
        let selected = draft.look.color == color
        return Button { draft.look.color = color } label: {
            Circle()
                .fill(color.swatch)
                .frame(width: 28, height: 28)
                .padding(4)
                .overlay(Circle().strokeBorder(DesignTokens.Color.labelPrimary, lineWidth: selected ? 2 : 0))
                .frame(width: 36, height: 36)
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(color.accessibilityName)
        .accessibilityAddTraits(selected ? [.isSelected] : [])
        .accessibilityIdentifier("newAgent.color.\(color.rawValue)")
    }

    // MARK: Fields

    private var nameSection: some View {
        AgentFormSection(header: NewAgentSetupCopy.nameHeader, footer: NewAgentSetupCopy.nameFooter) {
            TextField(NewAgentSetupCopy.namePlaceholder, text: $draft.name)
                .font(DesignTokens.Typography.body)
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .focused($nameFocused)
                .submitLabel(.next)
            #if os(iOS)
                .textInputAutocapitalization(.words)
            #endif
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .frame(minHeight: 52)
                .accessibilityIdentifier("newAgent.name")
        }
    }

    private var handlesSection: some View {
        AgentFormSection(header: NewAgentSetupCopy.handlesHeader, footer: NewAgentSetupCopy.handlesFooter) {
            TextField(NewAgentSetupCopy.handlesPlaceholder, text: $draft.handles, axis: .vertical)
                .font(DesignTokens.Typography.body)
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .lineLimit(2...6)
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.vertical, DesignTokens.Spacing.md)
                .frame(minHeight: 88, alignment: .topLeading)
                .accessibilityIdentifier("newAgent.handles")
        }
    }

    private var servicesSection: some View {
        AgentFormSection(header: AgentServicesCopy.header, footer: NewAgentSetupCopy.servicesFooter) {
            ForEach(draft.services) { service in
                AgentServiceRow(title: service.name, subtitle: service.status, glyph: service.glyph) {
                    onAction(.openService(id: service.id))
                }
                .accessibilityIdentifier("newAgent.service.\(service.id)")
                AgentRowDivider()
            }
            AgentServiceRow(title: AgentServicesCopy.addService, subtitle: nil, glyph: .link) {
                onAction(.addService)
            }
            .accessibilityIdentifier("newAgent.addService")
        }
    }

    // MARK: Create

    private var createBar: some View {
        Button(NewAgentSetupCopy.create) {
            if let action = draft.createAction { onAction(action) }
        }
        .remButton(.rectBlack)
        .disabled(!draft.canCreate)
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
        NavigationStack {
            NewAgentSetupScreen(draft: $draft) { _ in }
        }
    }
}

#Preview("NewAgentSetupScreen") {
    NewAgentSetupPreview()
}
#endif
