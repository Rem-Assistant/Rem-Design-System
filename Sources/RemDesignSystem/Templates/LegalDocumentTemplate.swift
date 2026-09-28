import SwiftUI

/// The **legal-document sheet** the consent step's Terms / Privacy rows open as a page sheet — the
/// SwiftUI sibling of the Compose `LegalDocumentScreen`, 1:1 with the shipping `LegalDocumentView`
/// (remclaw). Presentational: an inline nav title + a "Done" dismiss over a scrollable body of
/// ``Section``s. Per `docs/contracts/onboarding-consent.md` the design system owns the sheet chrome;
/// the **legal copy body is owned by `LegalDocumentView`** and injected via `sections`. This shipping
/// target contains no sample legal prose; render-only fixture copy lives in `RenderSnapshotTests`.
public struct LegalDocumentTemplate: View {
    /// One titled block of a legal document.
    public struct Section: Identifiable {
        public let id = UUID()
        public let heading: String
        public let body: String
        public init(heading: String, body: String) {
            self.heading = heading
            self.body = body
        }
    }

    var title: String
    var sections: [Section]
    var onClose: () -> Void

    public init(title: String, sections: [Section], onClose: @escaping () -> Void) {
        self.title = title
        self.sections = sections
        self.onClose = onClose
    }

    /// The single dismiss endpoint used by the production Done button and interaction tests.
    func dismiss() {
        onClose()
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Inline nav bar: centered title + a 44pt Done affordance, matching the Compose sheet.
            ZStack {
                Text(title)
                    .font(DesignTokens.Typography.title1.weight(.semibold))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                HStack {
                    Spacer(minLength: 0)
                    Button("Done", action: dismiss)
                        .font(DesignTokens.Typography.body)
                        .foregroundStyle(DesignTokens.Color.systemBlue)
                        .frame(minWidth: 44, minHeight: 44)
                }
            }
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .frame(height: 56)
            Divider()
            ScrollView {
                VStack(alignment: .leading, spacing: DesignTokens.Spacing.lg) {
                    ForEach(sections) { section in
                        VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
                            Text(section.heading)
                                .font(DesignTokens.Typography.bodyBold)
                                .foregroundStyle(DesignTokens.Color.labelPrimary)
                            Text(section.body)
                                .font(DesignTokens.Typography.body)
                                .foregroundStyle(DesignTokens.Color.labelSecondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                .padding(DesignTokens.Spacing.lg)
            }
        }
        .background(DesignTokens.Color.backgroundPrimary.ignoresSafeArea())
    }
}
