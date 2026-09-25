import SwiftUI

/// Trailing disclosure chevron. Figma canonical: **Chevron** (`110:52`).
public struct Chevron: View {
    public init() {}
    public var body: some View {
        Image(systemName: "chevron.right")
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(DesignTokens.Color.labelTertiary)
    }
}

/// The canonical list row: **[Leading accessory] · Title/Subtitle · [Trailing accessory]**.
/// Figma canonical: **ListRow** (`101:18`) + **ListRowLabel** (`188:2`). The accessory slots are
/// generic so leading can be a `ContainedIcon` or `Avatar` and trailing a `Chevron`, `Switch`, or a
/// value label. When `action` is set the whole row is a plain button (tap target = full row).
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

#if DEBUG
#Preview("ListRow — disclosure") {
    VStack(spacing: 0) {
        ListRow("Terms of Service",
                subtitle: "How Rem accounts, subscriptions, and approved actions work.",
                action: {},
                leading: { ContainedIcon("doc.text", fill: .subtle) },
                trailing: { Chevron() })
        Divider().padding(.leading, 60)
        ListRow("Privacy Policy",
                subtitle: "What Rem, your gateway, and AI or voice providers process.",
                action: {},
                leading: { ContainedIcon("shield", fill: .subtle) },
                trailing: { Chevron() })
    }
    .background(DesignTokens.Color.backgroundSecondary)
    .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xlarge, style: .continuous))
    .padding()
}
#endif
