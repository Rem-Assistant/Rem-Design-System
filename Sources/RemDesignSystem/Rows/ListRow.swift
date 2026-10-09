import SwiftUI

/// Semantic visual emphasis for a row. Callers describe intent while `ListRow` owns the shared
/// foundation treatment, keeping state styling consistent across every row composition.
public enum ListRowEmphasis: Sendable {
    case standard
    case deemphasized

    fileprivate var opacity: Double {
        switch self {
        case .standard: 1
        case .deemphasized: DesignTokens.Opacity.deemphasized
        }
    }
}

/// The canonical list row **content**: **[Leading accessory] · Title/Subtitle · [Trailing accessory]**.
/// Figma canonical: **ListRow** (`101:18`) + **ListRowLabel** (`188:2`).
///
/// Designed to sit inside a **native `List`/`Section`** — SwiftUI provides the insetGrouped chrome,
/// row separators, and (via `NavigationLink`) the **disclosure chevron**, so we don't reinvent them.
/// The trailing slot is for *non-disclosure* accessories (a `Switch`, a value label, a badge). Use an
/// explicit chevron only for a non-navigation row (e.g. a Button-based sheet opener that isn't in a
/// `List`). When `action` is set the whole row is a plain button (tap target = full row).
public enum ListRowLayout: Sendable {
    /// Preserve the existing standalone row padding and optional divider.
    case standalone
    /// Native List/Form owns insets and separators. No custom disclosure is added.
    case nativeList
}

public struct ListRow<Leading: View, Trailing: View>: View {
    let title: String
    let subtitle: String?
    let action: (() -> Void)?
    let emphasis: ListRowEmphasis
    let leading: () -> Leading
    let trailing: () -> Trailing
    private var settingsContent: AnyView?
    private var supporting: AnyView?
    private var showsDivider = false
    private var layout: ListRowLayout = .standalone

    public init(
        _ title: String,
        subtitle: String? = nil,
        action: (() -> Void)? = nil,
        emphasis: ListRowEmphasis = .standard,
        @ViewBuilder leading: @escaping () -> Leading,
        @ViewBuilder trailing: @escaping () -> Trailing
    ) {
        self.title = title
        self.subtitle = subtitle
        self.action = action
        self.emphasis = emphasis
        self.leading = leading
        self.trailing = trailing
    }

    /// A row with an interactive accessory **under the title** (Material's `ListItem` calls this
    /// `supportingContent`). Used when a trailing value cannot share one line with the title — e.g. a
    /// time value at narrow widths or accessibility text sizes — so the title never wraps.
    public init<Supporting: View>(
        _ title: String,
        subtitle: String? = nil,
        action: (() -> Void)? = nil,
        emphasis: ListRowEmphasis = .standard,
        @ViewBuilder leading: @escaping () -> Leading,
        @ViewBuilder supporting: () -> Supporting,
        @ViewBuilder trailing: @escaping () -> Trailing
    ) {
        self.init(title, subtitle: subtitle, action: action, emphasis: emphasis, leading: leading, trailing: trailing)
        self.supporting = AnyView(supporting())
    }

    /// Canonical settings density with an editable Content slot. The caller owns the divider,
    /// and may supply native fields, labels, or a menu without creating a competing row.
    public init<Content: View>(
        showsDivider: Bool = false,
        layout: ListRowLayout = .standalone,
        @ViewBuilder leading: @escaping () -> Leading,
        @ViewBuilder content: () -> Content,
        @ViewBuilder trailing: @escaping () -> Trailing
    ) {
        self.title = ""
        self.subtitle = nil
        self.action = nil
        self.emphasis = .standard
        self.leading = leading
        self.trailing = trailing
        self.settingsContent = AnyView(content())
        self.showsDivider = showsDivider
        self.layout = layout
    }

    public var body: some View {
        if let action {
            Button(action: action) { rowContent.opacity(emphasis.opacity) }
                .buttonStyle(.plain)
        } else {
            rowContent.opacity(emphasis.opacity)
        }
    }

    @ViewBuilder private var rowContent: some View {
        if let settingsContent, layout == .nativeList {
            HStack(spacing: DesignTokens.Spacing.md) {
                leading()
                settingsContent.frame(maxWidth: .infinity, alignment: .leading)
                trailing()
            }.contentShape(Rectangle())
        } else if let settingsContent {
            VStack(spacing: 0) {
                HStack(spacing: DesignTokens.Spacing.md) {
                    leading()
                    settingsContent.frame(maxWidth: .infinity, alignment: .leading)
                    trailing()
                }
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.vertical, DesignTokens.Spacing.md)
                .frame(minHeight: 60)
                .contentShape(Rectangle())
                if showsDivider {
                    Divider().overlay(DesignTokens.Color.separator)
                        .padding(.leading, DesignTokens.Spacing.lg)
                }
            }
        } else {
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
                    if let supporting {
                        supporting.padding(.top, DesignTokens.Spacing.xs)
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
