import SwiftUI

/// **ChatHeader** — the conversation header: the Rem face in a 64pt avatar, and beneath it the agent
/// identity pill — name, a status dot, and the agent's **current activity** (e.g. "Connected", or what
/// it is doing right now), with a disclosure chevron.
///
/// Back / overflow toolbar buttons are platform navigation chrome supplied by the host, not part of
/// this component. Activity text is host data; the Playground passes neutral fixture copy.
///
/// Figma canonical: **Rem/Chat v2/Header** (`2054:19725`). Compose sibling: `chat/ChatHeader.kt`.
public struct ChatHeader: View {
    /// Drives the status dot: green when connected, orange when Rem needs the person.
    public enum Status: Equatable, Sendable {
        case connected
        case needsYou
    }

    private let name: String
    private let activity: String
    private let status: Status
    private let faceMode: RemFaceMark.Mode
    private let accessibilityPrefix: String
    private let onTap: (() -> Void)?

    public init(
        name: String = "Rem",
        activity: String,
        status: Status = .connected,
        faceMode: RemFaceMark.Mode = .idle,
        accessibilityPrefix: String = "chatHeader",
        onTap: (() -> Void)? = nil
    ) {
        self.name = name
        self.activity = activity
        self.status = status
        self.faceMode = faceMode
        self.accessibilityPrefix = accessibilityPrefix
        self.onTap = onTap
    }

    public var body: some View {
        VStack(spacing: 6) {
            RemFaceMark(mode: faceMode, tint: DesignTokens.Color.brandBlue, size: 48)
                .frame(width: 64, height: 64)
                .background(DesignTokens.Color.backgroundSecondary, in: Circle())
                .accessibilityHidden(true)
            if let onTap {
                Button(action: onTap) { identity }
                    .buttonStyle(.plain)
            } else {
                identity
            }
        }
        .frame(maxWidth: .infinity)
    }

    private var identity: some View {
        VStack(spacing: 1) {
            Text(name)
                .font(DesignTokens.Typography.title3.weight(.semibold))
                .foregroundStyle(DesignTokens.Color.labelPrimary)
            HStack(spacing: 5) {
                Circle()
                    .fill(status == .connected ? DesignTokens.Color.systemGreen : DesignTokens.Color.systemOrange)
                    .frame(width: 6, height: 6)
                Text(activity)
                    .font(DesignTokens.Typography.footnote)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .lineLimit(1)
                Image(systemName: "chevron.right")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
            }
        }
        .padding(.horizontal, 22)
        .padding(.vertical, DesignTokens.Spacing.sm)
        .frame(minWidth: 138)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 30, style: .continuous))
        .shadow(color: .black.opacity(0.12), radius: 20, x: 0, y: 8)
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("\(accessibilityPrefix).identity")
    }
}

#Preview {
    VStack(spacing: DesignTokens.Spacing.xl) {
        ChatHeader(activity: "Connected")
        ChatHeader(activity: "Reading the shared notes", status: .connected, faceMode: .thinking)
        ChatHeader(activity: "Needs you", status: .needsYou)
    }
    .padding(DesignTokens.Spacing.lg)
    .background(DesignTokens.Color.backgroundPrimary)
}
