import SwiftUI

/// **SettingsScreen** — the root Settings surface: a large "Settings" title over grouped, inset
/// `RemSection` cards. The first card is the **profile row** (an avatar leading + name/email); the
/// rest are grouped `ListRow`s with `ContainedIcon` leadings and chevron / toggle / value trailings.
/// Composed from canonical components; the host supplies the sections through `content`, exactly as
/// `InboxScreen` takes its rows. Chrome (status bar, nav bar, home indicator) comes from the platform,
/// as in the shipping `SharedSettingsView` (large nav title + insetGrouped `List`).
///
/// The single-row-section rule is honoured for free: a `RemSection` with one `ListRow` draws no
/// divider (there is no sibling to separate), while a multi-row section draws the canonical inset
/// separators between its rows.
///
/// Figma canonical: `Screen/Settings` (`130:44`, page "Settings"). Source: `SharedSettingsView.swift`.
/// Compose sibling: `screens/SettingsScreen.kt`.
public struct SettingsScreen<Content: View>: View {
    private let title: String
    private let content: () -> Content

    public init(title: String = "Settings", @ViewBuilder content: @escaping () -> Content) {
        self.title = title
        self.content = content
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(title)
                .font(DesignTokens.Typography.largeTitle.weight(.bold))
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.top, DesignTokens.Spacing.md)
                .padding(.bottom, DesignTokens.Spacing.sm)
            ScrollView {
                VStack(spacing: DesignTokens.Spacing.xl) {
                    content()
                }
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.bottom, DesignTokens.Spacing.xl)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .background(DesignTokens.Color.backgroundPrimary)
    }
}

#if DEBUG
/// A trailing value label + disclosure chevron — the "value" row accessory (e.g. Voice → "Aria").
private struct ValueDisclosure: View {
    let value: String
    var body: some View {
        HStack(spacing: DesignTokens.Spacing.xs) {
            Text(value)
                .font(DesignTokens.Typography.body)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
            DisclosureChevron()
        }
    }
}

/// Circular profile avatar — a settings-specific accessory (no canonical Avatar component yet), so it
/// lives in the preview rather than the screen API. Mirrors `SharedSettingsView.fallbackAvatar`.
private struct ProfileAvatar: View {
    var body: some View {
        Circle()
            .fill(DesignTokens.Color.fillTertiary)
            .frame(width: 44, height: 44)
            .overlay {
                Image(systemName: "person.fill")
                    .font(.system(size: 20))
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
            }
    }
}

private struct SettingsScreenPreview: View {
    @State private var proactiveSuggestions = true

    var body: some View {
        SettingsScreen {
            // Profile — single-row section, no divider.
            RemSection {
                ListRow("Avery Diaz",
                        subtitle: "avery@example.com",
                        leading: { ProfileAvatar() },
                        trailing: { EmptyView() })
            }

            // Core surfaces — a multi-row section, so RemSection draws inset separators.
            RemSection(header: "General") {
                ListRow("Connectors",
                        subtitle: "3 connected",
                        leading: { ContainedIcon("cable.connector", fill: .tint(DesignTokens.Color.systemIndigo)) },
                        trailing: { DisclosureChevron() })
                Divider().overlay(DesignTokens.Color.separator).padding(.leading, 60)
                ListRow("Voice",
                        leading: { ContainedIcon("mic.fill", fill: .tint(DesignTokens.Color.systemOrange)) },
                        trailing: { ValueDisclosure(value: "Aria") })
                Divider().overlay(DesignTokens.Color.separator).padding(.leading, 60)
                ListRow("General",
                        leading: { ContainedIcon("gearshape.fill", fill: .tint(DesignTokens.Color.systemBlue)) },
                        trailing: { DisclosureChevron() })
            }

            // A toggle row — single-row section, no divider. iOS-green tint per the design system.
            RemSection(header: "Notifications",
                       footer: "Rem surfaces new suggestions as they're ready.") {
                ListRow("Proactive suggestions",
                        leading: { ContainedIcon("bell.badge.fill", fill: .tint(DesignTokens.Color.systemRed)) },
                        trailing: {
                            Toggle("", isOn: $proactiveSuggestions)
                                .labelsHidden()
                                .tint(DesignTokens.Color.systemGreen)
                        })
            }
        }
    }
}

#Preview("SettingsScreen") {
    SettingsScreenPreview()
}
#endif
