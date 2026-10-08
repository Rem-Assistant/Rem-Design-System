import SwiftUI
import UIKit
import RemDesignSystem

@main
struct RemSettingsPlaygroundApp: App {
    @Environment(\.dynamicTypeSize) private var systemTypeSize
    var body: some Scene {
        WindowGroup {
            PlaygroundHome()
                .preferredColorScheme(ProcessInfo.processInfo.arguments.contains("--settings-dark") ? .dark : (ProcessInfo.processInfo.arguments.contains("--settings-light") ? .light : nil))
                .dynamicTypeSize(ProcessInfo.processInfo.arguments.contains("--settings-large-text") ? .accessibility3 : systemTypeSize)
        }
    }
}

enum LoadFixture: String, CaseIterable { case success = "Success", slow = "Slow", error = "Error" }

private enum PlaygroundRoute: Hashable { case settings, controls, agendaSuggestions(AgendaSuggestionsFixture) }

struct PlaygroundHome: View {
    @State private var path = NavigationPath()
    @State private var fixture = LoadFixture.success
    @State private var agendaFixture = AgendaSuggestionsFixture.loaded
    var body: some View {
        NavigationStack(path: $path) {
            Form {
                Section("Native component playground") {
                    Text("Settings New · iOS").font(.headline)
                    Text("A local prototype with illustrative data for all seven designed Agent settings destinations. Voice previews demonstrate controls without audio. Automations remains outside this trial.").font(.footnote)
                    Picker("Load fixture", selection: $fixture) {
                        ForEach(LoadFixture.allCases, id: \.self) { Text($0.rawValue).tag($0) }
                    }.pickerStyle(.segmented).accessibilityIdentifier("fixturePicker")
                    NavigationLink("Open Settings", value: PlaygroundRoute.settings).accessibilityIdentifier("openSettings")
                    NavigationLink("Shared controls", value: PlaygroundRoute.controls)
                }
                Section("Agenda New · Suggestions") {
                    Text("A local Agenda Suggestions journey: Add / Move / Dismiss, overflow, and the empty day becoming populated — all from deterministic fixtures.").font(.footnote)
                    Picker("Agenda fixture", selection: $agendaFixture) {
                        ForEach(AgendaSuggestionsFixture.allCases, id: \.self) { Text($0.title).tag($0) }
                    }.pickerStyle(.segmented).accessibilityIdentifier("agendaFixturePicker")
                    NavigationLink("Open Agenda Suggestions", value: PlaygroundRoute.agendaSuggestions(agendaFixture))
                        .accessibilityIdentifier("openAgendaSuggestions")
                }
            }.navigationTitle("Rem Playground")
                .navigationDestination(for: PlaygroundRoute.self) { route in
                    switch route {
                    case .settings: SettingsPreview(fixture: fixture)
                    case .controls: ControlsPreview()
                    case .agendaSuggestions(let agenda): AgendaSuggestionsPreview(fixture: agenda)
                    }
                }
                .navigationDestination(for: SettingsEntryDestination.self) { _ in
                    AgentPreview(fixture: fixture)
                }
                .navigationDestination(for: AgentSettingsDestination.self) { route in
                    switch route {
                    case .pairedDevices: SettingsPairedDevicesScreen()
                    case .memory: SettingsMemoryScreen()
                    case .models: SettingsModelsScreen()
                    case .cloudBrowser: SettingsCloudBrowserScreen()
                    case .wallet: SettingsWalletScreen()
                    case .voice: SettingsVoiceScreen()
                    case .connectors: SettingsConnectorsScreen()
                    }
                }
        }
    }
}

struct SettingsPreview: View {
    let fixture: LoadFixture
    @State private var sharing = false
    var body: some View {
        SettingsEntryContent(onShare: { sharing = true }, agentRoute: .agentSettings)
            .background(DesignTokens.Color.backgroundPrimary)
            .navigationTitle("Settings").navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $sharing) { PlaygroundShareSheet() }
    }
}

/// The operating system owns recipient selection and sending. Opening this sheet sends nothing.
private struct PlaygroundShareSheet: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: ["Rem — a personal AI assistant. Shared from the local Settings playground."], applicationActivities: nil)
    }
    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}

struct AgendaSuggestionsPreview: View {
    let fixture: AgendaSuggestionsFixture
    var body: some View {
        AgendaSuggestionsPlaygroundView(fixture: fixture)
            .navigationTitle("Agenda New")
            .navigationBarTitleDisplayMode(.inline)
    }
}

struct AgentPreview: View {
    let fixture: LoadFixture
    @Environment(\.dismiss) private var dismiss
    @State private var status = "loading"
    @State private var attempt = 0
    var body: some View {
        // A stable container owns the load task while its loading/ready child changes.
        ZStack {
            if status == "ready" {
                AgentSettingsContent(availableDestinations: [.pairedDevices, .connectors, .cloudBrowser, .memory, .models, .wallet, .voice])
            } else {
                VStack(spacing: 20) {
                    if status == "loading" {
                        ProgressView("Loading agent settings…")
                    } else {
                        Image(systemName: "exclamationmark.triangle.fill").foregroundStyle(.orange).font(.largeTitle)
                        Text("Couldn’t load agent settings").font(.headline)
                        Text("This is a simulated connection error. Retry to load the local fixture.").multilineTextAlignment(.center)
                        Button("Retry") { attempt += 1 }.remButton(.rectBlue).accessibilityIdentifier("retry")
                    }
                    Button("Cancel") { dismiss() }.accessibilityIdentifier("cancelLoad")
                }.padding(24).frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }.background(DesignTokens.Color.backgroundPrimary)
            .navigationTitle("Agent settings").navigationBarTitleDisplayMode(.inline)
            .task(id: attempt) {
                // Native pushes can reattach this host's task. Once loaded, keep the
                // source links stable while a child destination is being presented.
                guard status != "ready" else { return }
                status = "loading"
                do {
                    try await Task.sleep(for: .seconds(fixture == .slow && attempt == 0 ? 10 : 0.2))
                    try Task.checkCancellation()
                    status = fixture == .error && attempt == 0 ? "error" : "ready"
                } catch { /* Navigation cancels this view-owned task. */ }
            }
    }
}

struct ControlsPreview: View {
    @State private var enabled = true
    @State private var name = "Avery Diaz"
    @State private var draft = ""
    @State private var editing = false
    var body: some View {
        Form {
            Section("Shared controls") {
                Toggle("Notifications", isOn: $enabled)
                Button { draft = name; editing = true } label: {
                    HStack { Text("Display name"); Spacer(); Text(name).foregroundStyle(.secondary) }
                }.accessibilityIdentifier("editName")
                HStack {
                    ContainedIcon("info.circle.fill", fill: .tint(.blue), size: .settings)
                    ContainedIcon("info.circle.fill", fill: .tint(.blue), size: .small)
                    ContainedIcon("info.circle.fill", fill: .tint(.blue), size: .large)
                }
                Button("Disabled example") {}.remButton(.rectBlue).disabled(true)
            }
            Text("Changes are local to this controls session.").font(.footnote)
        }.navigationTitle("Shared controls")
            .sheet(isPresented: $editing) {
                NavigationStack {
                    Form { TextField("Display name", text: $draft).accessibilityIdentifier("nameField") }
                        .navigationTitle("Edit name").navigationBarTitleDisplayMode(.inline)
                        .toolbar {
                            ToolbarItem(placement: .cancellationAction) { Button("Cancel") { editing = false } }
                            ToolbarItem(placement: .confirmationAction) {
                                Button("Save") { name = draft.trimmingCharacters(in: .whitespacesAndNewlines); editing = false }
                                    .disabled(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                            }
                        }
                }
            }
    }
}
