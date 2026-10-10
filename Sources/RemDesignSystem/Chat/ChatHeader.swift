import SwiftUI

/// **ChatHeader** — the conversation header: the Rem face in a 64pt avatar, and beneath it the agent
/// identity pill — name, a status dot, and the agent's **current activity** (e.g. "Connected", or what
/// it is doing right now), with a disclosure chevron.
///
/// The header **owns** the back and overflow controls (Figma corrections, 2026-10-10): pass `onBack` /
/// `onOverflow` and it draws them as 44pt circular controls level with the avatar, stretched to the
/// screen width (verified at 320pt and 402pt). Hosts must not add a second navigation-title row above
/// it. Without the callbacks the controls are omitted (component catalog use). Activity text is host
/// data for the agent's lifecycle, not transport evidence; the Playground passes neutral fixture copy.
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
    private let onBack: (() -> Void)?
    private let onOverflow: (() -> Void)?
    private let onCall: (() -> Void)?

    /// Height of the header-owned controls; their centre sits on the 64pt avatar's centre.
    public static let controlSize: CGFloat = 44
    public static let avatarSize: CGFloat = 64

    public init(
        name: String = "Rem",
        activity: String,
        status: Status = .connected,
        faceMode: RemFaceMark.Mode = .idle,
        accessibilityPrefix: String = "chatHeader",
        onTap: (() -> Void)? = nil,
        onBack: (() -> Void)? = nil,
        onOverflow: (() -> Void)? = nil,
        onCall: (() -> Void)? = nil
    ) {
        self.name = name
        self.activity = activity
        self.status = status
        self.faceMode = faceMode
        self.accessibilityPrefix = accessibilityPrefix
        self.onTap = onTap
        self.onBack = onBack
        self.onOverflow = onOverflow
        self.onCall = onCall
    }

    public var body: some View {
        identityStack
            .overlay(alignment: .top) {
                if onBack != nil || onOverflow != nil || onCall != nil { controls }
            }
    }

    /// Back (leading) and one trailing action, level with the avatar. The trailing slot holds a single
    /// control: the in-app call entry when the host offers it, which **replaces** overflow (WS1e — never
    /// a second icon beside it), otherwise overflow. The row stretches to the available width so the
    /// trailing control stays visible on narrow screens.
    private var controls: some View {
        HStack {
            if let onBack {
                headerControl("chevron.left", label: "Back", id: "back", action: onBack)
            }
            Spacer(minLength: 0)
            if let onCall {
                headerControl("phone", label: "Call Rem", id: "call", action: onCall)
            } else if let onOverflow {
                headerControl("ellipsis", label: "More", id: "overflow", action: onOverflow)
            }
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.top, (Self.avatarSize - Self.controlSize) / 2)
        .frame(maxWidth: .infinity)
    }

    private func headerControl(_ symbol: String, label: String, id: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .frame(width: Self.controlSize, height: Self.controlSize)
                .background(.regularMaterial, in: Circle())
                .shadow(color: .black.opacity(0.10), radius: 12, x: 0, y: 4)
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .accessibilityIdentifier("\(accessibilityPrefix).\(id)")
    }

    private var identityStack: some View {
        VStack(spacing: 6) {
            RemFaceMark(mode: faceMode, tint: DesignTokens.Color.brandBlue, size: 48)
                // A fresh mark per mode: leaving `.thinking` otherwise keeps its repeat-forever outline
                // draw running (the idle trim target equals the loop's end value, so nothing replaces
                // it) and the idle face can be caught with no outline.
                .id(faceMode)
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
