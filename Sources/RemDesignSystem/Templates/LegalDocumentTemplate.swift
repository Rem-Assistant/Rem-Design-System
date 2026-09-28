import SwiftUI

/// The **legal-document sheet** the consent step's Terms / Privacy rows open as a page sheet — the
/// SwiftUI sibling of the Compose `LegalDocumentScreen`, 1:1 with the shipping `LegalDocumentView`
/// (remclaw). Presentational: an inline nav title + a "Done" dismiss over a scrollable body of
/// ``Section``s. Per `docs/contracts/onboarding-consent.md` the design system owns the sheet chrome;
/// the **legal copy body is owned by `LegalDocumentView`** and injected via `sections` — the text here
/// is representative structure for the paired render, not canonical legal copy.
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

    public init(title: String, sections: [Section], onClose: @escaping () -> Void = {}) {
        self.title = title
        self.sections = sections
        self.onClose = onClose
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Inline nav bar: title leading, "Done" trailing (dismiss). Mirrors the Compose sheet.
            HStack {
                Text(title)
                    .font(DesignTokens.Typography.title1.weight(.semibold))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                Spacer(minLength: DesignTokens.Spacing.sm)
                Button("Done", action: onClose)
                    .font(DesignTokens.Typography.body)
                    .foregroundStyle(DesignTokens.Color.systemBlue)
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

public extension LegalDocumentTemplate {
    /// Representative Terms sections for previews/render evidence. Real copy is `LegalDocumentView`'s.
    static var previewTermsSections: [Section] {
        [
            .init(heading: "1. Your account",
                  body: "Rem accounts let you sign in, sync your data, and manage subscriptions across your devices. You are responsible for keeping your sign-in credentials secure."),
            .init(heading: "2. Subscriptions",
                  body: "Paid features renew automatically until cancelled. You can review or cancel a subscription in Settings at any time; access continues through the end of the current period."),
            .init(heading: "3. Approved actions",
                  body: "When you ask Rem to act on your behalf, it performs only the actions you approve through your personal cloud gateway. You can revoke an approval at any time."),
        ]
    }

    /// Representative Privacy sections for previews/render evidence. Real copy is `LegalDocumentView`'s.
    static var previewPrivacySections: [Section] {
        [
            .init(heading: "What we process",
                  body: "Rem processes the messages, tasks, and connections you give it so it can answer you and act on the things you ask. You can review or delete this data in Settings."),
            .init(heading: "Your gateway",
                  body: "Requests route through your personal cloud gateway. Rem stores only what is needed to keep your assistant working across sessions and devices."),
            .init(heading: "AI and voice providers",
                  body: "To generate answers, relevant content may be sent to AI or voice providers under agreements that limit their use to serving your request."),
        ]
    }
}

#if DEBUG
#Preview("Legal · terms") {
    LegalDocumentTemplate(title: "Terms of Service", sections: LegalDocumentTemplate.previewTermsSections)
}
#Preview("Legal · privacy") {
    LegalDocumentTemplate(title: "Privacy Policy", sections: LegalDocumentTemplate.previewPrivacySections)
}
#endif
