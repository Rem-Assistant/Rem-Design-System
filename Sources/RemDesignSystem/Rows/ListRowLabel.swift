import SwiftUI

public enum ListRowTitleLayout: Sendable { case fill, hug }

/// The editable label slot of ListRow (Figma 1966:60442). Uses semantic Dynamic Type styles.
public struct ListRowLabel: View {
    private let title: String
    private let subtitle: String?
    private let titleLayout: ListRowTitleLayout
    private let titleAccessory: AnyView

    public init(_ title: String, subtitle: String? = nil, titleLayout: ListRowTitleLayout = .fill) {
        self.title = title; self.subtitle = subtitle; self.titleLayout = titleLayout
        self.titleAccessory = AnyView(EmptyView())
    }

    public init<Accessory: View>(_ title: String, subtitle: String? = nil,
        titleLayout: ListRowTitleLayout = .fill, @ViewBuilder titleAccessory: () -> Accessory) {
        self.title = title; self.subtitle = subtitle; self.titleLayout = titleLayout
        self.titleAccessory = AnyView(titleAccessory())
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack(spacing: DesignTokens.Spacing.sm) {
                Text(title).font(.body).foregroundStyle(DesignTokens.Color.labelPrimary)
                    .frame(maxWidth: titleLayout == .fill ? .infinity : nil, alignment: .leading)
                titleAccessory
            }
            if let subtitle {
                Text(subtitle).font(.footnote).foregroundStyle(DesignTokens.Color.labelSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}
