import SwiftUI

/// **RemAccountPill** — the signed-in account at the bottom of the sidebar: initial disc + name +
/// `chevron.up.chevron.down`, on a floating glass capsule. It **is** the Settings entry (there is no
/// separate gear), so VoiceOver reads it as "<name>, button, Opens Settings".
///
/// Figma: **Account · opens Settings** (`2709:2648`). Compose twin: `navigation/RemAccountPill.kt`.
public struct RemAccountPill: View {
    private let account: RemSidebarAccount
    private let action: () -> Void

    public init(_ account: RemSidebarAccount, action: @escaping () -> Void) {
        self.account = account
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Text(account.initial)
                    .font(DesignTokens.Typography.subheadline.weight(.semibold))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .frame(width: 29, height: 29)
                    .background(DesignTokens.Color.backgroundSecondary, in: Circle())
                HStack(spacing: 3) {
                    Text(account.displayName)
                        .font(DesignTokens.Typography.subheadline.weight(.semibold))
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                        .lineLimit(1)
                    Image(systemName: "chevron.up.chevron.down")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                }
            }
            .padding(.leading, DesignTokens.Spacing.xs)
            .padding(.trailing, DesignTokens.Spacing.sm)
            .frame(height: 44)
            .background(DesignTokens.Color.backgroundPrimary.opacity(0.85), in: Capsule())
            .shadow(color: .black.opacity(0.12), radius: 20, y: 8)
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(account.displayName)
        .accessibilityHint(RemSidebarDisplay.accountHint)
        .accessibilityAddTraits(.isButton)
        .accessibilityIdentifier("sidebar.account")
    }
}

#if DEBUG
#Preview("RemAccountPill") {
    RemAccountPill(.init(name: "Samuel")) {}
        .padding()
        .background(DesignTokens.Color.backgroundSecondary)
}
#endif
