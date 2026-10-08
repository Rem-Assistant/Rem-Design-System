import SwiftUI

/// Shared controlled row for ConnectorRow 2213:9330. Settings uses Connected/Disclosure;
/// onboarding may later opt into Connected/Switch. This component never connects or stores state.
public enum ConnectorRowState: Sendable {
    case available, connecting, connected, error
    public var subtitle: String {
        switch self {
        case .available: "Not connected"
        case .connecting: "Connecting…"
        case .connected: "Connected"
        case .error: "Couldn’t connect · Try again"
        }
    }
}
public enum ConnectorRowAccessory {
    case action(String, () -> Void)
    case progress
    case disclosure
    case toggle(Binding<Bool>)
}
public struct ConnectorRow<Leading: View>: View {
    private let title: String
    private let subtitle: String
    private let accessory: ConnectorRowAccessory
    private let layout: ListRowLayout
    private let showsDivider: Bool
    private let leading: () -> Leading
    private var customContent: AnyView?
    private var actionIdentifier: String?
    public init(_ title: String, state: ConnectorRowState, subtitle: String? = nil,
                accessory: ConnectorRowAccessory, layout: ListRowLayout = .standalone, showsDivider: Bool = false,
                @ViewBuilder leading: @escaping () -> Leading) {
        self.title = title; self.subtitle = subtitle ?? state.subtitle
        self.accessory = accessory; self.layout = layout; self.leading = leading; self.showsDivider = showsDivider
    }
    /// Editable nested ListRow content for Code Connect overrides and other app contexts.
    public init<Content: View>(_ title: String, state: ConnectorRowState,
                accessory: ConnectorRowAccessory, layout: ListRowLayout = .standalone, showsDivider: Bool = false,
                @ViewBuilder leading: @escaping () -> Leading, @ViewBuilder content: () -> Content) {
        self.title = title; self.subtitle = state.subtitle; self.accessory = accessory
        self.layout = layout; self.showsDivider = showsDivider; self.leading = leading; self.customContent = AnyView(content())
    }
    /// Identify the native action itself; a composite row identifier can propagate to its brand image.
    /// Internal to the Settings integration so the shared public variant API stays unchanged.
    func actionAccessibilityIdentifier(_ identifier: String) -> Self {
        var row = self
        row.actionIdentifier = identifier
        return row
    }
    public var body: some View {
        ListRow(showsDivider: showsDivider, layout: layout, leading: leading, content: {
            if let customContent { customContent } else { ListRowLabel(title, subtitle: subtitle) }
        }, trailing: {
            switch accessory {
            case .action(let label, let action):
                let button = Button(label, action: action).remButton(.pillSecondary)
                    .accessibilityLabel("\(label) \(title)")
                if let actionIdentifier {
                    button.accessibilityIdentifier(actionIdentifier)
                } else {
                    button
                }
            case .progress:
                ProgressView().accessibilityLabel("Connecting \(title)")
            case .disclosure:
                // The wrapping NavigationLink supplies the native List chevron.
                if layout == .standalone { DisclosureChevron() }
            case .toggle(let value):
                Toggle(title, isOn: value).labelsHidden().tint(DesignTokens.Color.systemGreen)
            }
        })
        .frame(minHeight: layout == .nativeList ? 40 : 64)
    }
}

/// Exact unmodified 4× node exports from brand-assets.json; no utility-icon tint/background.
public enum ConnectorProvider: String, CaseIterable, Identifiable, Sendable {
    case gmail, googleCalendar, notion, slack, googleDrive, linear, todoist
    public var id: String { rawValue }
    public var title: String {
        switch self {
        case .gmail: "Gmail"
        case .googleCalendar: "Google Calendar"
        case .notion: "Notion"
        case .slack: "Slack"
        case .googleDrive: "Google Drive"
        case .linear: "Linear"
        case .todoist: "Todoist"
        }
    }
    fileprivate var asset: String {
        switch self {
        case .gmail: "ConnectorGmail"
        case .googleCalendar: "ConnectorGoogleCalendar"
        case .notion: "ConnectorNotion"
        case .slack: "ConnectorSlack"
        case .googleDrive: "ConnectorGoogleDrive"
        case .linear: "ConnectorLinear"
        case .todoist: "ConnectorTodoist"
        }
    }
}
public struct ConnectorProviderMark: View {
    private let provider: ConnectorProvider
    public init(_ provider: ConnectorProvider) { self.provider = provider }
    public var body: some View {
        Image(provider.asset, bundle: .module).resizable().renderingMode(.original).scaledToFit()
            .frame(width: provider == .todoist ? 20 : 26, height: provider == .todoist ? 20 : 26)
            .frame(width: provider == .todoist ? 29 : 26, height: 29)
            .accessibilityHidden(true)
    }
}
