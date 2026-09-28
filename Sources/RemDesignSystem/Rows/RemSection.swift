import SwiftUI

/// A grouped list surface with optional header/footer text and an open rows slot.
///
/// Figma canonical: **Section** (`741:311`). Inside a native SwiftUI `List` or `Form`, prefer
/// `SwiftUI.Section`; that container receives its chrome from the host. `RemSection` is the adapter
/// for custom `ScrollView` surfaces, such as onboarding, that need the same grouped appearance
/// without adopting `List` behavior. The component intentionally has no outline: its
/// hierarchy comes from `backgroundSecondary`, the 24-point continuous corner radius, and the
/// separators owned by its rows. This mirrors the grouped treatment used by Rem's onboarding and
/// settings surfaces without forcing callers into SwiftUI `List` when they need custom scrolling.
public struct RemSection<Rows: View>: View {
    private let header: String?
    private let footer: String?
    private let rows: () -> Rows

    public init(
        header: String? = nil,
        footer: String? = nil,
        @ViewBuilder rows: @escaping () -> Rows
    ) {
        self.header = header
        self.footer = footer
        self.rows = rows
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let header {
                Text(header.uppercased())
                    .font(DesignTokens.Typography.footnote)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .padding(.horizontal, DesignTokens.Spacing.md)
                    .padding(.bottom, DesignTokens.Spacing.xs)
            }

            VStack(spacing: 0) { rows() }
                .frame(maxWidth: .infinity)
                .background(DesignTokens.Color.backgroundSecondary)
                .clipShape(
                    RoundedRectangle(
                        cornerRadius: DesignTokens.CornerRadius.xlarge,
                        style: .continuous
                    )
                )

            if let footer {
                Text(footer)
                    .font(DesignTokens.Typography.footnote)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .padding(.horizontal, DesignTokens.Spacing.md)
                    .padding(.top, DesignTokens.Spacing.xs)
            }
        }
    }
}

#if DEBUG
#Preview("RemSection") {
    RemSection(
        header: "Section Header",
        footer: "Explanatory footer text that describes this section."
    ) {
        ListRow("Account", subtitle: "avery@example.com", leading: {
            ContainedIcon("person.fill", fill: .subtle)
        }, trailing: {
            Image(systemName: "chevron.right")
        })
        Divider().padding(.leading, 60)
        ListRow("Privacy", subtitle: "Manage your data", leading: {
            ContainedIcon("shield", fill: .subtle)
        }, trailing: {
            Image(systemName: "chevron.right")
        })
    }
    .padding()
}
#endif
