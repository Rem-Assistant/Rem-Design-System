import SwiftUI
import RemDesignSystem

@main
struct RemSettingsPlaygroundApp: App {
    var body: some Scene { WindowGroup { PlaygroundHome() } }
}

enum LoadFixture: String, CaseIterable { case success = "Success", slow = "Slow", error = "Error" }

struct PlaygroundHome: View {
    @State private var fixture = LoadFixture.success
    var body: some View {
        NavigationStack {
            Form {
                Section("Native component playground") {
                    Text("Settings New · iOS").font(.headline)
                    Text("A local fixture. Only Settings → Rem is connected. Other rows are visual references. Automations is outside this trial.").font(.footnote)
                    Picker("Load fixture", selection: $fixture) {
                        ForEach(LoadFixture.allCases, id: \.self) { Text($0.rawValue).tag($0) }
                    }.pickerStyle(.segmented).accessibilityIdentifier("fixturePicker")
                    NavigationLink("Open Settings") { SettingsPreview(fixture: fixture) }.accessibilityIdentifier("openSettings")
                    NavigationLink("Shared controls") { ControlsPreview() }
                }
            }.navigationTitle("Rem Playground")
        }
    }
}

struct SettingsPreview: View {
    let fixture: LoadFixture
    @State private var openAgent = false
    var body: some View {
        ScrollView { SettingsEntryContent { openAgent = true } }
            .background(DesignTokens.Color.backgroundPrimary)
            .navigationTitle("Settings").navigationBarTitleDisplayMode(.inline)
            .navigationDestination(isPresented: $openAgent) { AgentPreview(fixture: fixture) }
    }
}

struct AgentPreview: View {
    let fixture: LoadFixture
    @Environment(\.dismiss) private var dismiss
    @State private var status = "loading"
    @State private var attempt = 0
    var body: some View {
        Group {
            if status == "ready" {
                ScrollView { AgentSettingsContent() }.accessibilityIdentifier("agentSettings")
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
