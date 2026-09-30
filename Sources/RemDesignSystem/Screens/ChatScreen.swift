import SwiftUI

/// **ChatScreen** — the conversation surface: a scrolling transcript of `MessageBubble`s, an optional
/// `VoiceBar` above the input when voice is live, and a `RemComposerBar` pinned at the bottom. Composed
/// from Wave-2 components; the host supplies the transcript through `transcript`. Chrome comes from the
/// platform, as in the shipping `SharedRemChatView`.
///
/// Figma canonical: Chat screen (`71:533`) + composer states (`527:2`). Source: `SharedRemChatView.swift`.
/// Compose sibling: `screens/ChatScreen.kt`.
public struct ChatScreen<Transcript: View>: View {
    private let composerText: String
    private let composerPlaceholder: String
    private let composerState: RemComposerBar.SendState
    private let voiceBar: VoiceBarState?
    private let transcript: () -> Transcript

    public init(
        composerText: String = "",
        composerPlaceholder: String = "Ask anything",
        composerState: RemComposerBar.SendState = .idle,
        voiceBar: VoiceBarState? = nil,
        @ViewBuilder transcript: @escaping () -> Transcript
    ) {
        self.composerText = composerText
        self.composerPlaceholder = composerPlaceholder
        self.composerState = composerState
        self.voiceBar = voiceBar
        self.transcript = transcript
    }

    public var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: DesignTokens.Spacing.lg) {
                    transcript()
                }
                .padding(DesignTokens.Spacing.lg)
            }
            VStack(spacing: DesignTokens.Spacing.sm) {
                if let voiceBar {
                    VoiceBar(voiceBar)
                }
                RemComposerBar(text: composerText, placeholder: composerPlaceholder, state: composerState)
            }
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.bottom, DesignTokens.Spacing.md)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DesignTokens.Color.backgroundPrimary)
    }
}

#Preview {
    ChatScreen(composerText: "", composerState: .idle) {
        MessageBubble("Can you tidy up my inbox before I start my day?", role: .user)
        MessageBubble(
            "Done — I archived 38 newsletters and snoozed 5 low-priority threads. Want me to draft replies to the two that still need you?",
            role: .assistant
        )
        MessageBubble("Yes, go ahead.", role: .user)
    }
}
