import RemDesignSystem

/// Render-only fixture prose. These values cannot be imported by the shipping library and are not
/// canonical Terms or Privacy content; they exist only to make the reusable sheet chrome measurable.
enum LegalDocumentFixtures {
    static let terms: [LegalDocumentTemplate.Section] = [
        .init(
            heading: "1. Your account",
            body: "Rem accounts let you sign in, sync your data, and manage subscriptions across your devices. You are responsible for keeping your sign-in credentials secure."
        ),
        .init(
            heading: "2. Subscriptions",
            body: "Paid features renew automatically until cancelled. You can review or cancel a subscription in Settings at any time; access continues through the end of the current period."
        ),
        .init(
            heading: "3. Approved actions",
            body: "When you ask Rem to act on your behalf, it performs only the actions you approve through your personal cloud gateway. You can revoke an approval at any time."
        ),
    ]

    static let privacy: [LegalDocumentTemplate.Section] = [
        .init(
            heading: "What we process",
            body: "Rem processes the messages, tasks, and connections you give it so it can answer you and act on the things you ask. You can review or delete this data in Settings."
        ),
        .init(
            heading: "Your gateway",
            body: "Requests route through your personal cloud gateway. Rem stores only what is needed to keep your assistant working across sessions and devices."
        ),
        .init(
            heading: "AI and voice providers",
            body: "To generate answers, relevant content may be sent to AI or voice providers under agreements that limit their use to serving your request."
        ),
    ]
}
