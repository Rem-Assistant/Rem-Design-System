import SwiftUI

/// Presentational template for the onboarding **"Set up your voice"** (Voice) screen.
/// Pure: no speech engine, no persistence — the app supplies the current voice, slider bindings, and
/// the hear/select actions, and owns navigation + the first-run coach-mark. Figma: `Screen/Voice`
/// (`488:251`). Composes design-system components: `RemSection` (grouped surfaces), `ListRow`
/// (hear-voice + picker rows), `ContainedIcon` (row leadings), and native `Slider` (the platform
/// control for Character & speed — shared intent, native form). iOS-canonical; adapts on iPadOS/macOS.
public struct OnboardingVoiceTemplate: View {
    var title: String
    var voiceName: String
    var selectedVoice: String
    var onHearVoice: () -> Void
    var onSelectVoice: () -> Void
    @Binding var speed: Double
    @Binding var consistency: Double
    @Binding var likeness: Double

    public init(
        title: String = "Voice",
        voiceName: String = "Aria",
        selectedVoice: String,
        onHearVoice: @escaping () -> Void,
        onSelectVoice: @escaping () -> Void,
        speed: Binding<Double>,
        consistency: Binding<Double>,
        likeness: Binding<Double>
    ) {
        self.title = title
        self.voiceName = voiceName
        self.selectedVoice = selectedVoice
        self.onHearVoice = onHearVoice
        self.onSelectVoice = onSelectVoice
        self._speed = speed
        self._consistency = consistency
        self._likeness = likeness
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: DesignTokens.Spacing.lg) {
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
        .background(DesignTokens.Color.backgroundPrimary.ignoresSafeArea())
        .navigationTitle(title)
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
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
            Slider(value: value, in: 0...1)
                .tint(DesignTokens.Color.brandBlue)
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
