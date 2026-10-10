import SwiftUI

/// Reasoning effort for the next message. Four levels, chosen from a native menu.
public enum ThinkingLevel: String, CaseIterable, Identifiable, Sendable {
    case off, low, medium, high

    public var id: String { rawValue }

    public var title: String {
        switch self {
        case .off: return "Off"
        case .low: return "Low"
        case .medium: return "Medium"
        case .high: return "High"
        }
    }
}

/// **AddToChatSheet** — the composer's `+` sheet: Camera / Photos / Files tiles, the conditional
/// Cloud browser capability row, and the Thinking level menu, under an "Add to Chat" title with Done.
///
/// The sheet only reports intent; the host presents the real system pickers and owns the results,
/// matching the shipped `addToChatSheet` in `SharedRemChatView`:
/// - **Photos** — host presents the system photo picker, images only, at most `maxPhotoSelection` (4).
/// - **Files** — host presents the document picker restricted to image types.
/// - **Camera** — shown only when `showsCamera` is true; the host passes the platform capability
///   (iOS: camera hardware available). There is no Android camera flow in the shipped app, so Android
///   hosts pass `false`.
/// - **Cloud browser** — shown only when `browserAvailable`; tapping adds a removable capability chip
///   to the composer and dismisses (the host does both). It does not launch a browser.
/// - **Thinking** — Off / Low / Medium / High, bound to host state.
///
/// Figma canonical: **Rem/Chat/Add to Chat** (`2656:21214`), Thinking menu (`2660:128572`).
/// Compose sibling: `chat/AddToChatSheet.kt`.
public struct AddToChatSheet: View {
    /// The most photos one message may carry.
    public static let maxPhotoSelection = 4

    private let showsCamera: Bool
    private let browserAvailable: Bool
    @Binding private var thinking: ThinkingLevel
    private let accessibilityPrefix: String
    private let onCamera: () -> Void
    private let onPhotos: () -> Void
    private let onFiles: () -> Void
    private let onCloudBrowser: () -> Void
    private let onDone: () -> Void

    public init(
        showsCamera: Bool,
        browserAvailable: Bool,
        thinking: Binding<ThinkingLevel>,
        accessibilityPrefix: String = "addToChat",
        onCamera: @escaping () -> Void = {},
        onPhotos: @escaping () -> Void,
        onFiles: @escaping () -> Void,
        onCloudBrowser: @escaping () -> Void = {},
        onDone: @escaping () -> Void
    ) {
        self.showsCamera = showsCamera
        self.browserAvailable = browserAvailable
        self._thinking = thinking
        self.accessibilityPrefix = accessibilityPrefix
        self.onCamera = onCamera
        self.onPhotos = onPhotos
        self.onFiles = onFiles
        self.onCloudBrowser = onCloudBrowser
        self.onDone = onDone
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.lg) {
            header
            HStack(spacing: DesignTokens.Spacing.md) {
                if showsCamera { tile("Camera", systemImage: "camera", id: "camera", action: onCamera) }
                tile("Photos", systemImage: "photo.on.rectangle", id: "photos", action: onPhotos)
                tile("Files", systemImage: "folder", id: "files", action: onFiles)
            }
            if browserAvailable { cloudBrowserRow }
            thinkingRow
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.vertical, DesignTokens.Spacing.lg)
        .frame(maxWidth: .infinity, alignment: .topLeading)
    }

    private var header: some View {
        ZStack {
            Text("Add to Chat")
                .font(DesignTokens.Typography.bodyBold)
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .accessibilityAddTraits(.isHeader)
            HStack {
                Spacer()
                Button("Done", action: onDone)
                    .remButton(.textAccent, size: .compact)
                    .fixedSize()
                    .accessibilityIdentifier("\(accessibilityPrefix).done")
            }
        }
        .frame(minHeight: 44)
    }

    private func tile(_ title: String, systemImage: String, id: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: DesignTokens.Spacing.xs) {
                Image(systemName: systemImage)
                    .font(.system(size: 20, weight: .regular))
                Text(title)
                    .font(DesignTokens.Typography.caption1)
            }
            .foregroundStyle(DesignTokens.Color.labelPrimary)
            .frame(maxWidth: .infinity, minHeight: 78)
            .background(
                DesignTokens.Color.fillTertiary,
                in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium, style: .continuous)
            )
            .contentShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("\(accessibilityPrefix).\(id)")
    }

    private var cloudBrowserRow: some View {
        Button(action: onCloudBrowser) {
            HStack(spacing: DesignTokens.Spacing.md) {
                Image(systemName: "globe")
                    .font(.system(size: 20, weight: .regular))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Cloud browser")
                        .font(DesignTokens.Typography.body)
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                    Text("Add Cloud browser to your message.")
                        .font(DesignTokens.Typography.caption1)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                }
                Spacer(minLength: 0)
            }
            .padding(DesignTokens.Spacing.md)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                DesignTokens.Color.fillTertiary,
                in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium, style: .continuous)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("\(accessibilityPrefix).cloudBrowser")
    }

    private var thinkingRow: some View {
        Menu {
            ForEach(ThinkingLevel.allCases) { level in
                Button { thinking = level } label: {
                    if level == thinking {
                        Label(level.title, systemImage: "checkmark")
                    } else {
                        Text(level.title)
                    }
                }
                .accessibilityIdentifier("\(accessibilityPrefix).thinking.\(level.rawValue)")
            }
        } label: {
            HStack(spacing: DesignTokens.Spacing.md) {
                Image(systemName: "brain")
                    .font(.system(size: 20, weight: .regular))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                Text("Thinking")
                    .font(DesignTokens.Typography.body)
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                Spacer(minLength: DesignTokens.Spacing.sm)
                Text(thinking.title)
                    .font(DesignTokens.Typography.body)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                Image(systemName: "chevron.up.chevron.down")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
            }
            .padding(DesignTokens.Spacing.md)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                DesignTokens.Color.fillTertiary,
                in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium, style: .continuous)
            )
            .contentShape(Rectangle())
        }
        .menuStyle(.button)
        .menuIndicator(.hidden)
        .buttonStyle(.plain)
        .accessibilityLabel("Thinking, \(thinking.title)")
        .accessibilityIdentifier("\(accessibilityPrefix).thinking")
    }
}

#Preview {
    AddToChatSheet(
        showsCamera: true, browserAvailable: true, thinking: .constant(.medium),
        onPhotos: {}, onFiles: {}, onDone: {}
    )
    .background(DesignTokens.Color.backgroundPrimary)
}
