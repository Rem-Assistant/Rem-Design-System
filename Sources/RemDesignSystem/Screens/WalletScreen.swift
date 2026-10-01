import SwiftUI

/// **WalletScreen** — a **net-new product proposal** surface (there is no Figma master or shipping
/// source for this screen yet). It follows the same large-title template as `InboxScreen` /
/// `SettingsScreen`: a large "Wallet" title over a scrolling body of grouped cards, with chrome
/// (status bar, nav bar, home indicator) supplied by the platform. The host supplies the body through
/// `content`, exactly as the sibling screens do — so the hero card, usage summary, and transactions
/// section below are composed from canonical components (`RemSection`, `ListRow`, `ContainedIcon`,
/// `RemButtonStyle`) in the `#Preview`, not baked into the template.
///
/// Product intent (Rem thesis + Muse-informed payments direction): agent work costs money (model
/// usage / credits), so a Wallet home shows **balance / credits** (hero + top-up CTA), a **usage
/// summary** for the period, and a **recent activity** list. Every concrete choice here (dollars +
/// credits dual unit, a monthly budget meter, the transaction taxonomy, a saved payment method) is a
/// *proposal* for the founder to correct — see the `// PROPOSAL:` notes in the preview.
///
/// Compose sibling: `screens/WalletScreen.kt`.
public struct WalletScreen<Content: View>: View {
    private let title: String
    private let content: () -> Content

    public init(title: String = "Wallet", @ViewBuilder content: @escaping () -> Content) {
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
// MARK: - Proposal building blocks (preview-scoped, mirror SettingsScreen's private helpers)

/// The **balance hero** — a prominent card in the shared card language (`backgroundSecondary` fill +
/// `xlarge` continuous radius, the same surface `RemSection` uses), carrying the current balance, a
/// credits equivalent, and the primary **Add funds** CTA. Deliberately a neutral card, not a saturated
/// brand fill — the system reserves loud tone fills for status badges, so the hero stays tasteful and
/// on-system. The balance uses `title1Bold` so it reads as the hero without competing with the page's
/// `largeTitle`.
private struct WalletBalanceCard: View {
    let balance: String
    let credits: String
    var onAddFunds: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.xl) {
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
                HStack(spacing: DesignTokens.Spacing.md) {
                    ContainedIcon("wallet.pass.fill", fill: .tint(DesignTokens.Color.brandBlue))
                    Text("Available balance")
                        .font(DesignTokens.Typography.footnote)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                }
                Text(balance)
                    .font(DesignTokens.Typography.title1Bold)
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                Text(credits)
                    .font(DesignTokens.Typography.subheadline)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
            }
            Button(action: onAddFunds) {
                Text("Add funds").frame(maxWidth: .infinity)
            }
            .remButton(.rectBlack)
        }
        .padding(DesignTokens.Spacing.lg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DesignTokens.Color.backgroundSecondary)
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xlarge, style: .continuous))
    }
}

/// The **usage summary** — a card (same surface tokens) with this period's spend, a slim budget
/// meter, and a caption. PROPOSAL: a monthly budget cap is a plausible v1 guardrail; drop the meter
/// if Wallet should stay purely a balance ledger without a budget concept.
private struct WalletUsageCard: View {
    let spent: String
    let caption: String
    let fraction: Double

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
            HStack {
                Text("Spent this period")
                    .font(DesignTokens.Typography.subheadline)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                Spacer()
                Text(spent)
                    .font(DesignTokens.Typography.body.weight(.semibold))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .monospacedDigit()
            }
            UsageMeter(fraction: fraction)
            Text(caption)
                .font(DesignTokens.Typography.caption1)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
        }
        .padding(DesignTokens.Spacing.lg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DesignTokens.Color.backgroundSecondary)
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xlarge, style: .continuous))
    }
}

/// A slim capsule meter: `fillTertiary` track + `brandBlue` fill. No canonical Meter component exists
/// yet, so (like `SettingsScreen`'s `ProfileAvatar`) it lives with the proposal rather than the API.
private struct UsageMeter: View {
    let fraction: Double

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(DesignTokens.Color.fillTertiary)
                Capsule()
                    .fill(DesignTokens.Color.brandBlue)
                    .frame(width: geo.size.width * max(0, min(1, fraction)))
            }
        }
        .frame(height: 8)
    }
}

/// Trailing amount label for a transaction row: a credit (top-up / plan grant) is `systemGreen` with a
/// `+`; a debit (agent spend) is `labelPrimary`. Monospaced digits keep the column aligned.
private struct TransactionAmount: View {
    let amount: String
    let isCredit: Bool

    var body: some View {
        Text(amount)
            .font(DesignTokens.Typography.body.weight(.semibold))
            .foregroundStyle(isCredit ? DesignTokens.Color.systemGreen : DesignTokens.Color.labelPrimary)
            .monospacedDigit()
    }
}

/// A recent-activity entry. PROPOSAL: the taxonomy is agent-spend vs. credit (top-up / plan grant);
/// `symbol` + `tint` categorise the leading `ContainedIcon`.
private struct WalletTransaction: Identifiable {
    let id = UUID()
    let title: String
    let date: String
    let amount: String
    let isCredit: Bool
    let symbol: String
    let tint: Color
}

private struct WalletScreenPreview: View {
    private let transactions: [WalletTransaction] = [
        .init(title: "Agent run · Inbox triage", date: "Today, 9:24 AM", amount: "-$0.42",
              isCredit: false, symbol: "bolt.fill", tint: DesignTokens.Color.systemBlue),
        .init(title: "Agent run · Draft email reply", date: "Today, 8:10 AM", amount: "-$0.18",
              isCredit: false, symbol: "bolt.fill", tint: DesignTokens.Color.systemBlue),
        .init(title: "Top-up", date: "Yesterday", amount: "+$20.00",
              isCredit: true, symbol: "plus.circle.fill", tint: DesignTokens.Color.systemGreen),
        .init(title: "Agent run · Calendar summary", date: "Sep 28", amount: "-$0.31",
              isCredit: false, symbol: "bolt.fill", tint: DesignTokens.Color.systemBlue),
        .init(title: "Monthly plan credit", date: "Sep 25", amount: "+$5.00",
              isCredit: true, symbol: "arrow.triangle.2.circlepath", tint: DesignTokens.Color.systemGreen),
    ]

    var body: some View {
        WalletScreen {
            // Balance hero — PROPOSAL: show both a dollar balance and a credits equivalent so the
            // user reads the number either way agent cost is eventually billed.
            WalletBalanceCard(balance: "$42.75", credits: "≈ 1,710 credits remaining")

            // Usage — PROPOSAL: $50 monthly budget; 36% used.
            WalletUsageCard(spent: "$18.20",
                            caption: "$18.20 of $50.00 monthly budget",
                            fraction: 18.20 / 50.00)

            // Recent activity — multi-row RemSection, so it draws canonical inset separators.
            RemSection(header: "Recent activity", rows: transactions) { tx in
                ListRow(tx.title,
                        subtitle: tx.date,
                        leading: { ContainedIcon(tx.symbol, fill: .tint(tx.tint)) },
                        trailing: { TransactionAmount(amount: tx.amount, isCredit: tx.isCredit) })
            }

            // Payment method — PROPOSAL (optional): single-row section, no divider. Drop if top-up
            // always routes through the OS payment sheet (Apple Pay) with no stored method.
            RemSection(header: "Payment") {
                ListRow("Visa ending 4242",
                        subtitle: "Expires 08/27",
                        action: {},
                        leading: { ContainedIcon("creditcard.fill", fill: .tint(DesignTokens.Color.systemIndigo)) },
                        trailing: { DisclosureChevron() })
            }
        }
    }
}

#Preview("WalletScreen") {
    WalletScreenPreview()
}
#endif
