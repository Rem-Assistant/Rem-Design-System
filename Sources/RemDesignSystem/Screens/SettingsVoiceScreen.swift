import SwiftUI

/// Source: shared VoiceControlsContent 2217:1571 and VoiceChooserContent 2217:1901.
/// These are fixture identities, not provider voice identifiers or audio assets.
public enum VoiceChoice: String, CaseIterable, Identifiable, Sendable {
    case aria, sol, rowan, juniper, vale
    public var id: String { rawValue }
    public var name: String { rawValue.capitalized }
    public var character: String {
        switch self {
        case .aria: return "Warm"
        case .sol: return "Bright"
        case .rowan: return "Calm"
        case .juniper: return "Expressive"
        case .vale: return "Neutral"
        }
    }
    public var label: String { "\(name) (\(character))" }
}

public enum VoiceConversationEntry: String, CaseIterable, Sendable {
    case voiceSession = "Voice session", chat = "Chat"
}

/// Normalized values model the authored percentages; they are not provider speed/voice units.
public struct VoiceSettingsFixture: Equatable, Sendable {
    public var selected: VoiceChoice = .aria
    public var conversationEntry: VoiceConversationEntry = .voiceSession
    public var speed: Double = 0.50
    public var consistency: Double = 0.75
    public var likeness: Double = 0.50
    public private(set) var previewing: VoiceChoice?

    public init() {}
    public mutating func select(_ voice: VoiceChoice) { selected = voice }
    public mutating func togglePreview(_ voice: VoiceChoice) {
        previewing = previewing == voice ? nil : voice
    }
    public mutating func stopPreview() { previewing = nil }

    public static let conversationFooter = "Choose whether the center action opens a voice session or a new chat."
    public static let spokenFooter = "Choose how Rem sounds when reading a response or talking with you."
    public static let characterFooter = "Speed applies to the next thing Rem says. Consistency trades expressive range for a steadier delivery, and likeness controls how closely Rem holds to the chosen voice."
    public static let chooserIntro = "Preview a voice, then choose the one Rem should use."
    public static let chooserFooter = "Your choice follows this agent across your devices. Tap a play button to hear a preview."
    public static let previewHint = "Demonstrates preview controls only. No audio is played in this playground."
}

/// Native Settings shell. The hosting app supplies NavigationStack and the Agent settings route.
/// Local previews change controls only; no microphone, audio, TTS, account sync or service is used.
public struct SettingsVoiceScreen: View {
    @State private var fixture = VoiceSettingsFixture()
    public init() {}

    public var body: some View {
        List {
            VoiceControlsContent(fixture: $fixture, showConversationEntry: true,
                onPreview: { fixture.togglePreview($0) },
                voiceDestination: { SettingsVoiceChooser(fixture: $fixture) })
        }
        .settingsDestinationList()
        .navigationTitle("Voice")
        .settingsInlineNavigationTitle()
        .accessibilityIdentifier("settingsVoice")
        .accessibilityHint(PlaygroundMockData.hint)
        .onDisappear { fixture.stopPreview() }
    }
}

/// Controlled shared core: native Section children, intended for a List/Form owned by the shell.
/// Onboarding can hide the entire conversation-entry section; no onboarding shell is created here.
public struct VoiceControlsContent: View {
    @Binding private var fixture: VoiceSettingsFixture
    private let showConversationEntry: Bool
    private let onPreview: (VoiceChoice) -> Void
    private let voiceDestination: AnyView

    public init<Destination: View>(fixture: Binding<VoiceSettingsFixture>,
        showConversationEntry: Bool = true,
        onPreview: @escaping (VoiceChoice) -> Void,
        @ViewBuilder voiceDestination: () -> Destination) {
        self._fixture = fixture
        self.showConversationEntry = showConversationEntry
        self.onPreview = onPreview
        self.voiceDestination = AnyView(voiceDestination())
    }

    public var body: some View {
        Section {
            ListRow(layout: .nativeList, leading: { EmptyView() }, content: {
                ListRowLabel("Hear this voice", subtitle: fixture.selected.name + (isPreviewingSelected ? " · Playing" : ""))
            }, trailing: {
                Button { onPreview(fixture.selected) } label: {
                    Image(systemName: isPreviewingSelected ? "pause.fill" : "play.fill")
                }
                .buttonStyle(.borderedProminent)
                .buttonBorderShape(.roundedRectangle(radius: 10))
                .controlSize(.large)
                .tint(DesignTokens.Color.brandBlue)
                .accessibilityLabel(isPreviewingSelected ? "Pause \(fixture.selected.name) preview" : "Preview \(fixture.selected.name)")
                .accessibilityHint(VoiceSettingsFixture.previewHint)
                .accessibilityIdentifier("voice.previewSelected")
            })
        }.listRowBackground(DesignTokens.Color.backgroundSecondary)
        if showConversationEntry {
            Section {
                Picker(selection: $fixture.conversationEntry) {
                    ForEach(VoiceConversationEntry.allCases, id: \.self) { entry in
                        Text(entry.rawValue).tag(entry)
                    }
                } label: {
                    ListRow(layout: .nativeList, leading: {
                        ContainedIcon("speaker", fill: .subtle, size: .settings, glyphWeight: .regular)
                            .accessibilityHidden(true)
                    }, content: { ListRowLabel("Center button starts") }, trailing: { EmptyView() })
                }
                .pickerStyle(.menu)
                .tint(DesignTokens.Color.labelSecondary)
                .accessibilityIdentifier("voice.conversationEntry")
            } header: { HStack { Text("Conversation entry").textCase(nil) } } footer: { Text(VoiceSettingsFixture.conversationFooter) }
                .listRowBackground(DesignTokens.Color.backgroundSecondary)
        }
        Section {
            NavigationLink { voiceDestination } label: {
                SettingsRowLabel("Voice", subtitle: fixture.selected.label, symbol: "waveform")
            }
            .accessibilityIdentifier("voice.chooseVoice")
        } header: { HStack { Text("Spoken responses").textCase(nil) } } footer: { Text(VoiceSettingsFixture.spokenFooter) }
            .listRowBackground(DesignTokens.Color.backgroundSecondary)
        Section {
            slider("Speed", minimum: "Slower", maximum: "Faster", value: $fixture.speed, id: "speed")
            slider("Consistency", minimum: "Creative", maximum: "Consistent", value: $fixture.consistency, id: "consistency")
            slider("Likeness", minimum: "Flexible", maximum: "Faithful", value: $fixture.likeness, id: "likeness")
        } header: { HStack { Text("Character & speed").textCase(nil) } } footer: { Text(VoiceSettingsFixture.characterFooter) }
            .listRowBackground(DesignTokens.Color.backgroundSecondary)
    }

    private var isPreviewingSelected: Bool { fixture.previewing == fixture.selected }
    private func slider(_ title: String, minimum: String, maximum: String,
                        value: Binding<Double>, id: String) -> some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
            Text(title).font(DesignTokens.Typography.bodyBold)
            RemSlider(value: value)
                .accessibilityLabel(title)
                .accessibilityValue("\(Int((value.wrappedValue * 100).rounded())) percent")
                .accessibilityIdentifier("voice.slider.\(id)")
            HStack {
                Text(minimum)
                Spacer()
                Text(maximum)
            }
            .font(DesignTokens.Typography.caption1)
            .foregroundStyle(DesignTokens.Color.labelSecondary)
        }
        .padding(.vertical, DesignTokens.Spacing.sm)
    }
}

private struct SettingsVoiceChooser: View {
    @Binding var fixture: VoiceSettingsFixture
    var body: some View {
        List {
            VoiceChooserContent(fixture: $fixture, onPreview: { fixture.togglePreview($0) })
        }
        .settingsDestinationList()
        .navigationTitle("Choose a voice")
        .settingsInlineNavigationTitle()
        .accessibilityIdentifier("voiceChooser")
        .accessibilityHint(PlaygroundMockData.hint)
        .onDisappear { fixture.stopPreview() }
    }
}

/// Controlled chooser core. Preview and selection have independent hit targets; selection stays
/// here until native Back. Selection's blue check is intentional, utility preview tiles are Subtle.
public struct VoiceChooserContent: View {
    @Binding private var fixture: VoiceSettingsFixture
    private let onPreview: (VoiceChoice) -> Void
    public init(fixture: Binding<VoiceSettingsFixture>, onPreview: @escaping (VoiceChoice) -> Void) {
        self._fixture = fixture
        self.onPreview = onPreview
    }
    public var body: some View {
        Section {
            ForEach(VoiceChoice.allCases) { voice in
                ListRow(layout: .nativeList, leading: {
                    Button { onPreview(voice) } label: {
                        ContainedIcon(fixture.previewing == voice ? "pause.fill" : "play.fill",
                                      fill: .subtle, size: .settings, glyphWeight: .regular)
                            .frame(minWidth: 44, minHeight: 44)
                    }
                    .buttonStyle(.borderless)
                    .accessibilityLabel(fixture.previewing == voice ? "Pause \(voice.name) preview" : "Preview \(voice.name)")
                    .accessibilityHint(VoiceSettingsFixture.previewHint)
                    .accessibilityIdentifier("voice.preview.\(voice.id)")
                }, content: {
                    Button { fixture.select(voice) } label: {
                        HStack(spacing: DesignTokens.Spacing.sm) {
                            ListRowLabel(voice.name, subtitle: voice.character)
                            Image(systemName: fixture.selected == voice ? "checkmark.circle.fill" : "circle")
                                .font(.system(size: 20))
                                .foregroundStyle(fixture.selected == voice ? DesignTokens.Color.brandBlue : DesignTokens.Color.labelSecondary)
                                .frame(width: 28, height: 24)
                                .accessibilityHidden(true)
                        }.frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
                    }
                    .buttonStyle(.borderless)
                    .accessibilityLabel("\(voice.name), \(voice.character)")
                    .accessibilityAddTraits(fixture.selected == voice ? .isSelected : [])
                    .accessibilityIdentifier("voice.select.\(voice.id)")
                }, trailing: { EmptyView() })
            }
        } header: {
            HStack { Text(VoiceSettingsFixture.chooserIntro).font(.subheadline).textCase(nil) }
        } footer: {
            Text(VoiceSettingsFixture.chooserFooter).font(.subheadline)
        }.listRowBackground(DesignTokens.Color.backgroundSecondary)
    }
}
