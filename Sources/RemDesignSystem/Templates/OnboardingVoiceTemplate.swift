import SwiftUI

/// Onboarding **"Choose how Rem sounds"** (Voice) shell. Source: Figma `Screen/Voice · Onboarding`
/// (`2219:22520`), `Screen/Voice · Choose a voice` (`2221:83686`) and `Screen/Voice · Voice selected`
/// (`2219:23081`) on the Onboarding New page `2213:9168`, Voice section `2213:9346`.
///
/// This is a bounded playground slice, not the production voice integration or the onboarding
/// sequencer. It **reuses the shared Settings cores** — `VoiceControlsContent` (`2217:1571`) with
/// `showConversationEntry: false` and `VoiceChooserContent` (`2217:1901`) — so there is a single
/// controlled Voice control tree across Settings and Onboarding. Onboarding owns only its surrounding
/// navigation and actions: a centered `Lockup` (`773:22`) and a safe-area `ActionArea` (`773:28`)
/// with Continue + Skip. No audio, speech engine, persistence, or downstream product screen exists
/// here: preview is the Settings fixture's local no-audio play/pause state, and Continue / Skip call
/// separately observable host callbacks.
public struct OnboardingVoiceTemplate: View {
    @Binding private var fixture: VoiceSettingsFixture
    private let onPreview: (VoiceChoice) -> Void
    private let onContinue: () -> Void
    private let onSkip: () -> Void

    /// Authored lockup copy (`2219:22520` · `773:20` / `773:21`).
    public static let title = "Choose how Rem sounds"
    public static let subtitle = "Preview a voice, choose the one Rem uses, then fine-tune its delivery."

    public init(
        fixture: Binding<VoiceSettingsFixture>,
        onPreview: @escaping (VoiceChoice) -> Void,
        onContinue: @escaping () -> Void = {},
        onSkip: @escaping () -> Void = {}
    ) {
        self._fixture = fixture
        self.onPreview = onPreview
        self.onContinue = onContinue
        self.onSkip = onSkip
    }

    public var body: some View {
        VStack(spacing: 0) {
            List {
                lockup
                VoiceControlsContent(
                    fixture: $fixture,
                    showConversationEntry: false,
                    onPreview: onPreview,
                    voiceDestination: { OnboardingVoiceChooser(fixture: $fixture) }
                )
            }
            .settingsDestinationList()
            actionArea
        }
        .background(DesignTokens.Color.backgroundPrimary.ignoresSafeArea())
        .navigationTitle("")
        .settingsInlineNavigationTitle()
        .accessibilityIdentifier("onboardingVoice")
        .accessibilityHint(PlaygroundMockData.hint)
        .onDisappear { fixture.stopPreview() }
    }

    /// Centered hero + title + subtitle on the plain primary surface (no grouped card).
    private var lockup: some View {
        Section {
            VStack(spacing: DesignTokens.Spacing.md) {
                ContainedIcon("waveform", fill: .tint(DesignTokens.Color.brandBlue), size: .large)
                    .accessibilityHidden(true)
                VStack(spacing: DesignTokens.Spacing.xs) {
                    Text(Self.title)
                        .font(DesignTokens.Typography.largeTitle.weight(.semibold))
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                    Text(Self.subtitle)
                        .font(DesignTokens.Typography.body)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                }
                .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, DesignTokens.Spacing.sm)
            .padding(.bottom, DesignTokens.Spacing.xs)
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets(top: 0, leading: DesignTokens.Spacing.md,
                                      bottom: 0, trailing: DesignTokens.Spacing.md))
            .listRowSeparator(.hidden)
        }
    }

    /// Bottom, safe-area-pinned actions. Continue is the primary (Rect · Black); Skip is a quiet
    /// accent text link. Both scroll independently of the content above and stay reachable at large
    /// Dynamic Type because they live outside the scrolling `List`.
    private var actionArea: some View {
        VStack(spacing: DesignTokens.Spacing.xs) {
            Button(action: onContinue) {
                Text("Continue").frame(maxWidth: .infinity)
            }
            .remPrimaryActionButton()
            .accessibilityIdentifier("onboardingVoice.continue")
            Button("Skip", action: onSkip)
                .remButton(.textAccent)
                .accessibilityIdentifier("onboardingVoice.skip")
        }
        .frame(maxWidth: 560)
        .frame(maxWidth: .infinity)
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.top, DesignTokens.Spacing.sm)
        .padding(.bottom, DesignTokens.Spacing.sm)
        .background(DesignTokens.Color.backgroundPrimary)
    }
}

/// Host-facing onboarding Voice screen. Owns the controlled fixture and forwards the host's
/// Continue / Skip callbacks. The hosting app supplies the `NavigationStack` (its back button is the
/// outer Back that exits to the playground host); the chooser is pushed inside that same stack.
public struct OnboardingVoicePlaygroundScreen: View {
    @State private var fixture = VoiceSettingsFixture()
    private let onContinue: () -> Void
    private let onSkip: () -> Void

    public init(onContinue: @escaping () -> Void = {}, onSkip: @escaping () -> Void = {}) {
        self.onContinue = onContinue
        self.onSkip = onSkip
    }

    public var body: some View {
        OnboardingVoiceTemplate(
            fixture: $fixture,
            onPreview: { fixture.togglePreview($0) },
            onContinue: onContinue,
            onSkip: onSkip
        )
    }
}

/// Onboarding chooser destination. Reuses the public `VoiceChooserContent` core; selection stays
/// controlled here until native Back returns to the shell, and preview stops when the chooser leaves.
private struct OnboardingVoiceChooser: View {
    @Binding var fixture: VoiceSettingsFixture
    var body: some View {
        List {
            VoiceChooserContent(fixture: $fixture, onPreview: { fixture.togglePreview($0) })
        }
        .settingsDestinationList()
        .navigationTitle("Choose a voice")
        .settingsInlineNavigationTitle()
        .accessibilityIdentifier("onboardingVoiceChooser")
        .accessibilityHint(PlaygroundMockData.hint)
        .onDisappear { fixture.stopPreview() }
    }
}

#if DEBUG
private struct OnboardingVoiceTemplatePreviewHost: View {
    @State private var fixture = VoiceSettingsFixture()
    var body: some View {
        NavigationStack {
            OnboardingVoiceTemplate(
                fixture: $fixture,
                onPreview: { fixture.togglePreview($0) }
            )
        }
    }
}

#Preview("OnboardingVoiceTemplate") {
    OnboardingVoiceTemplatePreviewHost()
}
#endif
