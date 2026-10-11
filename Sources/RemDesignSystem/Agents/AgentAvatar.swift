import SwiftUI

/// **AgentAvatar** — the circular agent identity mark. The **Main agent** is always the Rem face on a
/// `backgroundSecondary` disc (`2718:3396`); a **Side agent** is its chosen glyph in white on its
/// chosen colour (`2718:3061` at 96pt; 120pt as the New agent preview).
///
/// Decorative: callers pair it with the agent name, so it is hidden from accessibility.
/// Compose twin: `agents/AgentAvatar.kt`.
public struct AgentAvatar: View {
    public enum Identity: Equatable, Sendable {
        case main
        case side(AgentLook)
    }

    private let identity: Identity
    private let size: CGFloat

    public init(_ identity: Identity, size: CGFloat = 96) {
        self.identity = identity
        self.size = size
    }

    public var body: some View {
        Group {
            switch identity {
            case .main:
                RemFaceMark(mode: .idle, tint: DesignTokens.Color.brandBlue, size: size * 0.75)
                    .frame(width: size, height: size)
                    .background(DesignTokens.Color.backgroundSecondary, in: Circle())
            case .side(let look):
                Image(systemName: look.glyph.systemName)
                    .font(.system(size: size * 0.45, weight: .semibold))
                    .foregroundStyle(DesignTokens.Color.labelOnColor)
                    .frame(width: size, height: size)
                    .background(look.color.swatch, in: Circle())
            }
        }
        .accessibilityHidden(true)
    }
}

public extension AgentColor {
    /// The token each swatch resolves to.
    var swatch: Color {
        switch self {
        case .red: return DesignTokens.Color.systemRed
        case .orange: return DesignTokens.Color.systemOrange
        case .yellow: return DesignTokens.Color.systemYellow
        case .green: return DesignTokens.Color.systemGreen
        case .mint: return DesignTokens.Color.systemMint
        case .teal: return DesignTokens.Color.systemTeal
        case .cyan: return DesignTokens.Color.systemCyan
        case .blue: return DesignTokens.Color.systemBlue
        case .indigo: return DesignTokens.Color.systemIndigo
        case .purple: return DesignTokens.Color.systemPurple
        case .pink: return DesignTokens.Color.systemPink
        }
    }
}

// MARK: - Shared grouped-form chrome (internal)

/// Figma **SectionHeader** (`2709:132175`): 17pt semibold `labelSecondary`, sentence case, 16pt
/// horizontal inset, 6pt below. Main's `RemSection(header:)` still renders the older uppercased
/// footnote header, so these agent surfaces place this header above a header-less `RemSection`.
/// Reconcile: once `RemSection.settingsHeader()` lands (PR #106), replace with
/// `RemSection(header:footer:).settingsHeader()` and delete this helper.
struct AgentSectionHeader: View {
    let title: String

    var body: some View {
        Text(title)
            .font(DesignTokens.Typography.body.weight(.semibold))
            .foregroundStyle(DesignTokens.Color.labelSecondary)
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.bottom, 6)
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityAddTraits(.isHeader)
    }
}

/// Figma **SectionFooter**: footnote `labelSecondary`, 16pt inset, 6pt above.
struct AgentSectionFooter: View {
    let text: String

    var body: some View {
        Text(text)
            .font(DesignTokens.Typography.footnote)
            .foregroundStyle(DesignTokens.Color.labelSecondary)
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.top, 6)
            .frame(maxWidth: .infinity, alignment: .leading)
            .fixedSize(horizontal: false, vertical: true)
    }
}

/// A grouped block: optional header, a `RemSection` surface, optional footer.
struct AgentFormSection<Rows: View>: View {
    let header: String?
    let footer: String?
    @ViewBuilder let rows: () -> Rows

    init(header: String? = nil, footer: String? = nil, @ViewBuilder rows: @escaping () -> Rows) {
        self.header = header
        self.footer = footer
        self.rows = rows
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let header { AgentSectionHeader(title: header) }
            RemSection { rows() }
            if let footer { AgentSectionFooter(text: footer) }
        }
    }
}

/// Inset separator between rows inside one `RemSection` (aligned with row text after a tile).
struct AgentRowDivider: View {
    var leadingInset: CGFloat = 60

    var body: some View {
        Divider()
            .overlay(DesignTokens.Color.separator)
            .padding(.leading, leadingInset)
    }
}

/// A connected-service row (`2718:3335`) or the "Add a service" row (`2714:3744`): blue tinted tile,
/// title (+ status), disclosure chevron. The whole row is one button.
struct AgentServiceRow: View {
    let title: String
    let subtitle: String?
    let glyph: AgentServiceGlyph
    let action: () -> Void

    var body: some View {
        ListRow(title, subtitle: subtitle, action: action, leading: {
            ContainedIcon(glyph.systemName, fill: .tint(DesignTokens.Color.systemBlue))
        }, trailing: {
            DisclosureChevron()
        })
        .frame(minHeight: 60)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }
}

#if DEBUG
#Preview("AgentAvatar") {
    HStack(spacing: 24) {
        AgentAvatar(.main)
        AgentAvatar(.side(.default))
        AgentAvatar(.side(AgentLook(glyph: .heart, color: .indigo)), size: 80)
    }
    .padding()
}
#endif
