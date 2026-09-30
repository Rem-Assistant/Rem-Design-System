import SwiftUI

/// Presentational template for the onboarding **"Set up your voice"** (Voice) screen.
/// Pure: no speech engine, no persistence — the app supplies the current voice, slider bindings, and
/// the hear/select actions, and owns navigation + the first-run coach-mark. Figma: `Screen/Voice`
/// (`488:251`). Composes design-system components: `RemSection` (grouped surfaces), `ListRow`
/// (hear-voice + picker rows), `ContainedIcon` (row leadings), and native `Slider` (the platform
/// control for Character & speed — shared intent, native form). iOS-canonical; adapts on iPadOS/macOS.
public struct OnboardingVoiceTemplate: View {
    var heroSymbol: String
    var voiceName: String
    var selectedVoice: String
    var onHearVoice: () -> Void
    var onSelectVoice: () -> Void
    @Binding var speed: Double
    @Binding var consistency: Double
    @Binding var likeness: Double
    var onContinue: () -> Void

    public init(
        heroSymbol: String = "waveform",
        voiceName: String = "Aria",
        selectedVoice: String,
        onHearVoice: @escaping () -> Void,
        onSelectVoice: @escaping () -> Void,
        speed: Binding<Double>,
        consistency: Binding<Double>,
        likeness: Binding<Double>,
        onContinue: @escaping () -> Void = {}
    ) {
        self.heroSymbol = heroSymbol
        self.voiceName = voiceName
        self.selectedVoice = selectedVoice
        self.onHearVoice = onHearVoice
        self.onSelectVoice = onSelectVoice
        self._speed = speed
        self._consistency = consistency
        self._likeness = likeness
        self.onContinue = onContinue
    }

    public var body: some View {
        VStack(spacing: 0) {
        ScrollView {
            VStack(spacing: DesignTokens.Spacing.lg) {
                // Hero — matches the onboarding hero-lockup pattern (Consent/Connectors): a blue
                // ContainedIcon tile at the top of the step.
                ContainedIcon(heroSymbol, fill: .tint(DesignTokens.Color.brandBlue), size: .large)
                    .padding(.top, DesignTokens.Spacing.md)
                RemSection {
                    ListRow(
                        "Hear this voice",
                        subtitle: voiceName,
                        action: onHearVoice,
                        leading: { ContainedIcon("play.fill", fill: .tint(DesignTokens.Color.brandBlue)) },
                        trailing: { EmptyView() }
                    )
                }

                RemSection(
                    header: "Spoken responses",
                    footer: "Choose how Rem sounds when reading a response or talking with you."
                ) {
                    ListRow(
                        "Voice",
                        action: onSelectVoice,
                        leading: { ContainedIcon("speaker.wave.2.fill", fill: .tint(DesignTokens.Color.brandBlue)) },
                        trailing: {
                            HStack(spacing: DesignTokens.Spacing.xs) {
                                Text(selectedVoice)
                                    .font(DesignTokens.Typography.body)
                                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                                DisclosureChevron()
                            }
                        }
                    )
                }

                RemSection(
                    header: "Character & speed",
                    footer: "Speed applies to the next thing Rem says. Consistency trades expressive range for a steadier delivery, and likeness controls how closely Rem holds to the chosen voice."
                ) {
                    VStack(spacing: 0) {
                        sliderRow("Speed", min: "Slower", max: "Faster", value: $speed)
                        rowSeparator
                        sliderRow("Consistency", min: "More expressive", max: "More consistent", value: $consistency)
                        rowSeparator
                        sliderRow("Likeness", min: "Looser", max: "Closer", value: $likeness)
                    }
                }
            }
            .padding(DesignTokens.Spacing.lg)
            .frame(maxWidth: 560)
        }
        bottomBar
        }
        .background(DesignTokens.Color.backgroundPrimary.ignoresSafeArea())
    }

    private var bottomBar: some View {
        Button("Continue", action: onContinue)
            .remPrimaryActionButton()
            .frame(maxWidth: 560)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.top, DesignTokens.Spacing.sm)
            .padding(.bottom, DesignTokens.Spacing.md)
            .background(DesignTokens.Color.backgroundPrimary)
    }

    private var rowSeparator: some View {
        Divider()
            .overlay(DesignTokens.Color.separator)
            .padding(.horizontal, DesignTokens.Spacing.md)
    }

    private func sliderRow(_ label: String, min: String, max: String, value: Binding<Double>) -> some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
            Text(label)
                .font(DesignTokens.Typography.body.weight(.semibold))
                .foregroundStyle(DesignTokens.Color.labelPrimary)
            RemSlider(value: value, in: 0...1)
            HStack {
                Text(min)
                Spacer()
                Text(max)
            }
            .font(DesignTokens.Typography.caption1)
            .foregroundStyle(DesignTokens.Color.labelSecondary)
        }
        .padding(.horizontal, DesignTokens.Spacing.md)
        .padding(.vertical, DesignTokens.Spacing.sm)
    }
}

#if DEBUG
private struct OnboardingVoiceTemplatePreviewHost: View {
    @State private var speed = 0.45
    @State private var consistency = 0.7
    @State private var likeness = 0.6
    var body: some View {
        NavigationStack {
            OnboardingVoiceTemplate(
                selectedVoice: "Aria (Warm)",
                onHearVoice: {},
                onSelectVoice: {},
                speed: $speed,
                consistency: $consistency,
                likeness: $likeness
            )
        }
    }
}

#Preview("OnboardingVoiceTemplate") {
    OnboardingVoiceTemplatePreviewHost()
}
#endif
