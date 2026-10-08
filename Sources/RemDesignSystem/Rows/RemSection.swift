import SwiftUI

private enum RemSectionMetrics {
    /// Aligns the separator with row text after the leading accessory.
    static let separatorLeadingInset: CGFloat = 60
}

/// Visual treatment for the rows surface inside ``RemSection``.
public enum RemSectionStyle: Sendable {
    /// Rounded secondary surface used for inset grouped lists.
    case insetGrouped
    /// Unboxed rows that inherit the parent surface.
    case plain
}

/// A list section with optional header/footer text and an open rows slot.
///
/// Figma canonical: **Section** (`1307:667`). Inside a native SwiftUI `List` or `Form`, prefer
/// `SwiftUI.Section`; that container receives its chrome from the host. `RemSection` is the adapter
/// for custom `ScrollView` surfaces, such as onboarding, that need the same grouped appearance
/// without adopting `List` behavior. The component intentionally has no outline: its
/// hierarchy comes from `backgroundSecondary`, the 24-point continuous corner radius, and its
/// canonical inset row separators. This mirrors the grouped treatment used by Rem's onboarding and
/// settings surfaces without forcing callers into SwiftUI `List` when they need custom scrolling.
public struct RemSection: View {
    private let header: String?
    private let footer: String?
    private let style: RemSectionStyle
    private var prominentHeader = false
    private var headerCase: Text.Case? = .uppercase
    private let rows: AnyView
    private var headerContent: AnyView?
    private var footerContent: AnyView?

    public init<Rows: View>(
        header: String? = nil,
        footer: String? = nil,
        style: RemSectionStyle = .insetGrouped,
        @ViewBuilder rows: () -> Rows
    ) {
        self.header = header
        self.footer = footer
        self.style = style
        self.rows = AnyView(rows())
    }

    /// Slot-based custom-scroll adapter for configurable Figma header/footer children.
    /// Native List/Form callers should compose SwiftUI.Section instead.
    public init<Header: View, Footer: View, Rows: View>(
        style: RemSectionStyle = .insetGrouped,
        @ViewBuilder header: () -> Header,
        @ViewBuilder footer: () -> Footer,
        @ViewBuilder rows: () -> Rows
    ) {
        self.header = nil; self.footer = nil; self.style = style
        self.headerContent = AnyView(header()); self.footerContent = AnyView(footer())
        self.rows = AnyView(rows())
    }

    /// Creates a canonical grouped row surface. `RemSection` inserts the inset separator between
    /// rows so callers cannot accidentally omit it or give the group a different separator style.
    public init<Data, ID, Row>(
        header: String? = nil,
        footer: String? = nil,
        style: RemSectionStyle = .insetGrouped,
        rows data: Data,
        id: KeyPath<Data.Element, ID>,
        @ViewBuilder row: @escaping (Data.Element) -> Row
    ) where Data: RandomAccessCollection, ID: Hashable, Row: View {
        self.header = header
        self.footer = footer
        self.style = style
        self.rows = AnyView(
            CanonicalRowGroup(data: data, id: id, row: row)
        )
    }

    /// Identifiable convenience for the common collection-backed Section.
    public init<Data, Row>(
        header: String? = nil,
        footer: String? = nil,
        style: RemSectionStyle = .insetGrouped,
        rows data: Data,
        @ViewBuilder row: @escaping (Data.Element) -> Row
    ) where Data: RandomAccessCollection, Data.Element: Identifiable, Row: View {
        self.init(header: header, footer: footer, style: style, rows: data, id: \.id, row: row)
    }

    /// Preserve sentence case when the approved screen uses it; existing consumers retain uppercase.
    public func headerTextCase(_ textCase: Text.Case?) -> Self {
        var section = self
        section.headerCase = textCase
        return section
    }

    /// Settings New uses a sentence-case body-sized section heading.
    public func settingsHeader() -> Self {
        var section = self
        section.prominentHeader = true
        section.headerCase = nil
        return section
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let headerContent {
                headerContent.padding(.horizontal, DesignTokens.Spacing.md).padding(.bottom, DesignTokens.Spacing.xs)
            } else if let header {
                Text(header)
                    .textCase(headerCase)
                    .font(prominentHeader ? DesignTokens.Typography.body.weight(.semibold) : DesignTokens.Typography.footnote)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .padding(.horizontal, prominentHeader ? 16 : DesignTokens.Spacing.md)
                    .padding(.bottom, prominentHeader ? 6 : DesignTokens.Spacing.xs)
            }

            rowsSurface

            if let footerContent {
                footerContent.padding(.horizontal, DesignTokens.Spacing.md).padding(.top, DesignTokens.Spacing.xs)
            } else if let footer {
                Text(footer)
                    .font(DesignTokens.Typography.footnote)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .padding(.horizontal, DesignTokens.Spacing.md)
                    .padding(.top, DesignTokens.Spacing.xs)
            }
        }
    }

    @ViewBuilder
    private var rowsSurface: some View {
        switch style {
        case .insetGrouped:
            VStack(spacing: 0) { rows }
                .frame(maxWidth: .infinity)
                .background(DesignTokens.Color.backgroundSecondary)
                .clipShape(
                    RoundedRectangle(
                        cornerRadius: DesignTokens.CornerRadius.xlarge,
                        style: .continuous
                    )
                )
        case .plain:
            VStack(spacing: 0) { rows }
                .frame(maxWidth: .infinity)
        }
    }
}

private struct CanonicalRowGroup<Data, ID, Row>: View
where Data: RandomAccessCollection, ID: Hashable, Row: View {
    let data: Data
    let id: KeyPath<Data.Element, ID>
    let row: (Data.Element) -> Row

    var body: some View {
        let lastID = data.last.map { $0[keyPath: id] }
        VStack(spacing: 0) {
            ForEach(data, id: id) { element in
                row(element)
                if let lastID, element[keyPath: id] != lastID {
                    Divider()
                        .overlay(DesignTokens.Color.separator)
                        .padding(.leading, RemSectionMetrics.separatorLeadingInset)
                }
            }
        }
    }
}

#if DEBUG
private struct RemSectionPreviewRow: Identifiable {
    let id: String
    let title: String
    let subtitle: String
    let symbol: String
}

private struct RemSectionPreview: View {
    private let rows = [
        RemSectionPreviewRow(id: "account", title: "Account", subtitle: "avery@example.com", symbol: "person.fill"),
        RemSectionPreviewRow(id: "privacy", title: "Privacy", subtitle: "Manage your data", symbol: "shield"),
    ]

    var body: some View {
        RemSection(
            header: "Section Header",
            footer: "Explanatory footer text that describes this section.",
            rows: rows,
            row: { row in
                ListRow(row.title, subtitle: row.subtitle, leading: {
                    ContainedIcon(row.symbol, fill: .subtle)
                }, trailing: {
                    Image(systemName: "chevron.right")
                })
            }
        )
        .padding()
    }
}

#Preview("RemSection") {
    RemSectionPreview()
}
#endif
