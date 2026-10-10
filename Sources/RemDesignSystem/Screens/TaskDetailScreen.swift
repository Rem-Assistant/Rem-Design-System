import SwiftUI

/// **TaskDetailScreen** — the task / event detail surface: a scrolling body (title · date · meta, then
/// the host-supplied sections — the "Last activity" log, notes, etc.) with a `RemComposerBar` pinned at
/// the bottom as the comment / reply composer. Composed from Wave-2 components; the host supplies the
/// body through the `content` slot and drives the composer through the composer parameters.
///
/// Chrome comes from the **platform**, exactly as the shipping detail relies on the OS: iOS 26 pushes
/// this view under a navigation stack, so the back button and the "Task" title are drawn by
/// `NavigationStack`/`UINavigationBar` — this template never hand-draws a nav bar (a platform-native
/// control), matching `ChatScreen`, `AgendaScreen`, and `InboxScreen`.
///
/// Figma canonical: Task detail screen (`299:2`, page "Task & Events"). Source: the shipping detail's
/// `TaskCommentsThread` + `TaskCommentComposer` (`Shared/Views/Tasks/TaskCommentsSection.swift`).
/// Compose sibling: `screens/TaskDetailScreen.kt`.
///
/// Task reply: `init(title:…composer:onComposerAction:content:)` uses the **same canonical composer**
/// as Chat (`RemComposerBar(state:onAction:)`), so an empty reply can never act as a navigation doorway:
/// Send follows `ChatComposerState.canSend`, and any "open in Chat" route is a separate host action. The
/// task-context reply accessory (Figma `2682:22298`) is pending the canonical full-screen review.
public struct TaskDetailScreen<Content: View>: View {
    private let title: String
    private let dateText: String?
    private let metaPills: [String]
    private let composerText: String
    private let composerPlaceholder: String
    private let composerState: RemComposerBar.SendState
    private let content: () -> Content
    private var hostComposer: ChatComposerState?
    private var onComposerAction: ((ChatComposerAction) -> Void)?

    public init(
        title: String,
        dateText: String? = nil,
        metaPills: [String] = [],
        composerText: String = "",
        composerPlaceholder: String = "Reply or ask Rem…",
        composerState: RemComposerBar.SendState = .idle,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.title = title
        self.dateText = dateText
        self.metaPills = metaPills
        self.composerText = composerText
        self.composerPlaceholder = composerPlaceholder
        self.composerState = composerState
        self.content = content
    }

    /// The host-driven detail: the reply composer is the canonical interactive composer, rendering
    /// `composer` and reporting every interaction through `onComposerAction`. Whether a task reply shows
    /// the model control is the host's `ChatComposerState.showsModel`.
    public init(
        title: String,
        dateText: String? = nil,
        metaPills: [String] = [],
        composer: ChatComposerState,
        onComposerAction: @escaping (ChatComposerAction) -> Void,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.init(title: title, dateText: dateText, metaPills: metaPills,
                  composerText: composer.draft, composerPlaceholder: composer.placeholder, content: content)
        self.hostComposer = composer
        self.onComposerAction = onComposerAction
    }

    @ViewBuilder
    private var composer: some View {
        if let hostComposer, let onComposerAction {
            RemComposerBar(state: hostComposer, accessibilityPrefix: "task", onAction: onComposerAction)
        } else {
            RemComposerBar(text: composerText, placeholder: composerPlaceholder, state: composerState)
        }
    }

    public var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: DesignTokens.Spacing.lg) {
                    header
                    content()
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(DesignTokens.Spacing.lg)
            }
            composer
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.bottom, DesignTokens.Spacing.md)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .background(DesignTokens.Color.backgroundPrimary)
    }

    /// Title · date · meta. The meta line pairs the date (quiet secondary label) with any list / folder
    /// membership rendered as canonical `RemPill`s.
    private var header: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            Text(title)
                .font(DesignTokens.Typography.title1Bold)
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .fixedSize(horizontal: false, vertical: true)

            if dateText != nil || !metaPills.isEmpty {
                HStack(spacing: DesignTokens.Spacing.sm) {
                    if let dateText {
                        Text(dateText)
                            .font(DesignTokens.Typography.subheadline)
                            .foregroundStyle(DesignTokens.Color.labelSecondary)
                    }
                    ForEach(metaPills, id: \.self) { RemPill($0) }
                }
            }
        }
    }
}

#if DEBUG
/// Preview body — composes the detail's two host sections from canonical pieces: a plain titled
/// "Last activity" group (matching the shipping `TaskCommentsThread`, which sits on the primary
/// background, not a card) and a grouped `RemSection` "Notes" card (single row → no divider).
private struct TaskDetailPreviewBody: View {
    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.lg) {
            lastActivity
            notes
        }
    }

    private var lastActivity: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
            Text("Last activity")
                .font(DesignTokens.Typography.caption1Bold)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
                .textCase(.uppercase)

            // Activity row: Rem's face-mark avatar · author · body · timestamp + Reply.
            HStack(alignment: .top, spacing: DesignTokens.Spacing.sm) {
                ZStack {
                    Circle().fill(DesignTokens.Color.brandBlue.opacity(0.15))
                    RemFaceMark(tint: DesignTokens.Color.brandBlue, size: 17)
                }
                .frame(width: 28, height: 28)

                VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
                    Text("Rem")
                        .font(DesignTokens.Typography.caption1Bold)
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                    Text("Drafted the investor update and pulled last quarter's metrics — want me to send it?")
                        .font(DesignTokens.Typography.body)
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                        .lineLimit(3)
                    HStack(spacing: DesignTokens.Spacing.md) {
                        Text("2h ago")
                            .font(DesignTokens.Typography.caption1)
                            .foregroundStyle(DesignTokens.Color.labelSecondary)
                        Text("Reply")
                            .font(DesignTokens.Typography.caption1Bold)
                            .foregroundStyle(DesignTokens.Color.brandBlue)
                    }
                    .padding(.top, DesignTokens.Spacing.xs)
                }
                Spacer(minLength: 0)
            }

            // Run now (left) · View history (right) — both understated labels, no filled container.
            HStack(spacing: DesignTokens.Spacing.md) {
                HStack(spacing: DesignTokens.Spacing.xs) {
                    Image(systemName: "play.fill").font(.system(size: 12))
                    Text("Run now").font(DesignTokens.Typography.caption1Bold)
                }
                .foregroundStyle(DesignTokens.Color.labelSecondary)
                Spacer(minLength: DesignTokens.Spacing.md)
                Text("View history")
                    .font(DesignTokens.Typography.caption1Bold)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
            }
        }
    }

    private var notes: some View {
        RemSection(header: "Notes") {
            Text("Include the updated runway chart and the two hires we closed. Keep it to one screen.")
                .font(DesignTokens.Typography.body)
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)
                .padding(DesignTokens.Spacing.md)
        }
    }
}

#Preview {
    TaskDetailScreen(
        title: "Draft the investor update",
        dateText: "Oct 1 2026",
        metaPills: ["Fundraise"],
        composerState: .idle
    ) {
        TaskDetailPreviewBody()
    }
}
#endif
