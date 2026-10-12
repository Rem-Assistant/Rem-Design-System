import SwiftUI

/// Settings About `1827:50839`, reached from Settings → About. The app-icon hero (the canonical
/// `RemAppIcon` asset at 80pt, radius 18), the LEGAL section and the Version row. Terms and Privacy
/// open the shared `LegalDocumentTemplate` sheet with the same summaries the onboarding legal rows
/// use; real legal copy belongs to the shipping app. Hosts pass their real bundle version; the
/// default is the source value. Compose sibling: `screens/SettingsAboutScreen.kt`.
public struct SettingsAboutScreen: View {
    private let version: String
    @State private var document: SettingsLegalDocument?

    public init(version: String = SettingsAboutFixture.sourceVersion) {
        self.version = version
    }

    public var body: some View {
        List {
            Section {
                VStack(spacing: DesignTokens.Spacing.md) {
                    RemAppIcon(size: 80, cornerRadius: 18)
                    VStack(spacing: DesignTokens.Spacing.xs) {
                        Text(SettingsAboutFixture.appName)
                            .font(DesignTokens.Typography.title1Bold)
                            .foregroundStyle(DesignTokens.Color.labelPrimary)
                            .accessibilityAddTraits(.isHeader)
                        Text(SettingsAboutFixture.tagline)
                            .font(DesignTokens.Typography.body)
                            .foregroundStyle(DesignTokens.Color.labelSecondary)
                            .multilineTextAlignment(.center)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 20)
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
                .accessibilityElement(children: .combine)
                .accessibilityIdentifier("about.hero")
            }
            Section {
                ForEach(SettingsAboutFixture.legal) { item in
                    Button { document = item } label: {
                        HStack(spacing: DesignTokens.Spacing.md) {
                            ListRowLabel(item.title)
                            Spacer(minLength: DesignTokens.Spacing.sm)
                            DisclosureChevron()
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("about.legal.\(item.id)")
                }
            } header: {
                HStack { Text(SettingsAboutFixture.legalHeader).textCase(nil) }
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
            Section {
                ListRow(layout: .nativeList, leading: { EmptyView() }, content: {
                    ListRowLabel(SettingsAboutFixture.versionTitle)
                }, trailing: {
                    Text(version)
                        .font(.callout)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                })
                .accessibilityElement(children: .combine)
                .accessibilityIdentifier("about.version")
            }.listRowBackground(DesignTokens.Color.backgroundSecondary)
        }
        .settingsDestinationList()
        .navigationTitle("About")
        .settingsInlineNavigationTitle()
        .accessibilityIdentifier("settingsAbout")
        .sheet(item: $document) { item in
            LegalDocumentTemplate(
                title: item.title,
                sections: [.init(heading: item.title, body: item.summary)],
                onClose: { document = nil }
            )
        }
    }
}

#if DEBUG
#Preview("About") {
    NavigationStack { SettingsAboutScreen() }
}
#endif
