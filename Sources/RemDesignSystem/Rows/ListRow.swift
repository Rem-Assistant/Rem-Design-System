import SwiftUI

/// The canonical list row **content**: **[Leading accessory] · Title/Subtitle · [Trailing accessory]**.
/// Figma canonical: **ListRow** (`101:18`) + **ListRowLabel** (`188:2`).
///
/// Designed to sit inside a **native `List`/`Section`** — SwiftUI provides the insetGrouped chrome,
/// row separators, and (via `NavigationLink`) the **disclosure chevron**, so we don't reinvent them.
/// The trailing slot is for *non-disclosure* accessories (a `Switch`, a value label, a badge). Use an
/// explicit chevron only for a non-navigation row (e.g. a Button-based sheet opener that isn't in a
/// `List`). When `action` is set the whole row is a plain button (tap target = full row).
public struct ListRow<Leading: View, Trailing: View>: View {
    let title: String
    let subtitle: String?
    let action: (() -> Void)?
    let leading: () -> Leading
    let trailing: () -> Trailing

    public init(
        _ title: String,
        subtitle: String? = nil,
        action: (() -> Void)? = nil,
        @ViewBuilder leading: @escaping () -> Leading,
        @ViewBuilder trailing: @escaping () -> Trailing
    ) {
        self.title = title
        self.subtitle = subtitle
        self.action = action
        self.leading = leading
        self.trailing = trailing
    }

    public var body: some View {
        if let action {
            Button(action: action) { rowContent }
                .buttonStyle(.plain)
        } else {
            rowContent
        }
    }

    private var rowContent: some View {
        HStack(spacing: DesignTokens.Spacing.md) {
            leading()
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(DesignTokens.Typography.body.weight(.semibold))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                if let subtitle {
                    Text(subtitle)
                        .font(DesignTokens.Typography.caption1)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            Spacer(minLength: DesignTokens.Spacing.sm)
            trailing()
        }
        .padding(.horizontal, DesignTokens.Spacing.md)
        .padding(.vertical, DesignTokens.Spacing.sm)
        .contentShape(Rectangle())
    }
}

/// Explicit disclosure chevron for **non-`List`** rows (e.g. a Button-based sheet opener). Inside a
/// native `List`, prefer `NavigationLink` — its disclosure is automatic. Not a public component.
struct DisclosureChevron: View {
    var body: some View {
        Image(systemName: "chevron.right")
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(DesignTokens.Color.labelTertiary)
    }
}

#if DEBUG
#Preview("ListRow — disclosure card") {
    VStack(spacing: 0) {
        ListRow("Terms of Service",
                subtitle: "How Rem accounts, subscriptions, and approved actions work.",
                action: {},
                leading: { ContainedIcon("doc.text", fill: .subtle) },
                trailing: { DisclosureChevron() })
        Divider().padding(.leading, 60)
        ListRow("Privacy Policy",
                subtitle: "What Rem, your gateway, and AI or voice providers process.",
                action: {},
                leading: { ContainedIcon("shield", fill: .subtle) },
                trailing: { DisclosureChevron() })
    }
    .background(DesignTokens.Color.backgroundSecondary)
    .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xlarge, style: .continuous))
    .padding()
}
#endif
