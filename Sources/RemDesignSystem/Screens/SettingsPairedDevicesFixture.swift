import Foundation

/// Deterministic, in-memory fixture model for the **Paired devices** Settings destination
/// (Agent settings · `pairedDevices`). This is the platform-agnostic state that
/// `SettingsPairedDevicesScreen` drives, factored out of the SwiftUI view — the same shape as
/// `SignInState` — so scope/cancel/remove/refresh behaviour is unit-testable without a running view.
///
/// Source evidence: `docs/playground/settings-design/settings-destinations.md` (Paired devices) and
/// the raw node contexts / screenshots `1833:5031` (populated), `1833:52316` (empty),
/// `1833:52344` (detail), `1839:52547` (remove confirmation).
///
/// Every value here is **illustrative fixture data**. Nothing pairs, authenticates, or revokes a
/// real device: `remove` drops a fixture peer, `refresh` is a deterministic local re-check that
/// finds nothing (it never fabricates a pairing). Only *other* paired devices appear — the current
/// device is deliberately absent from the fixture.

/// One peer device as shown in the list and detail. `approvedAtDisplay == nil` renders the
/// `Connected on` row as the em-dash specimen placeholder, never an invented date.
public struct PairedDevicePeer: Identifiable, Equatable, Sendable {
    public let id: String
    /// Display name + detail hero title, e.g. `Mac Studio`.
    public let name: String
    /// Device class subtitle under the hero, e.g. `Desktop`.
    public let kind: String
    /// SF Symbol for the detail hero tile (iOS). Compose maps to the registry `devices` glyph.
    public let heroSymbol: String
    /// Connection-section values. All illustrative.
    public let platform: String
    public let status: String
    public let lastActive: String
    /// Whether the list row shows the green connected dot; a peer can be connected or offline.
    public let isConnected: Bool
    /// `approvedAtMs` display. `nil` → em dash (no factual date supplied for the fixture).
    public let approvedAtDisplay: String?

    public init(
        id: String,
        name: String,
        kind: String,
        heroSymbol: String,
        platform: String,
        status: String,
        lastActive: String,
        isConnected: Bool,
        approvedAtDisplay: String? = nil
    ) {
        self.id = id
        self.name = name
        self.kind = kind
        self.heroSymbol = heroSymbol
        self.platform = platform
        self.status = status
        self.lastActive = lastActive
        self.isConnected = isConnected
        self.approvedAtDisplay = approvedAtDisplay
    }

    /// The one designed fixture peer (`1833:5031` / `1833:52344`). `Connected on` is the em-dash
    /// specimen placeholder, so `approvedAtDisplay` is left `nil`.
    public static let macStudio = PairedDevicePeer(
        id: "mac-studio",
        name: "Mac Studio",
        kind: "Desktop",
        heroSymbol: "laptopcomputer",
        platform: "macOS",
        status: "Recently active",
        lastActive: "1 min ago",
        isConnected: true,
        approvedAtDisplay: nil
    )

    /// The list-row connection summary, e.g. `Connected · Recently active`.
    public var connectionSummary: String {
        "\(isConnected ? "Connected" : "Offline") · \(status)"
    }

    /// The ordered `Connection` detail rows. The `Connected on` placeholder is centralised here so
    /// both platforms render the identical em dash when no date is supplied.
    public var connectionRows: [PairedDeviceConnectionRow] {
        [
            PairedDeviceConnectionRow(label: "Platform", value: platform),
            PairedDeviceConnectionRow(label: "Status", value: status),
            PairedDeviceConnectionRow(label: "Last active", value: lastActive),
            PairedDeviceConnectionRow(label: "Connected on", value: approvedAtDisplay ?? "—"),
        ]
    }
}

/// A label/value pair in the detail `Connection` section.
public struct PairedDeviceConnectionRow: Identifiable, Equatable, Sendable {
    public var id: String { label }
    public let label: String
    public let value: String
}

/// The in-memory list state. Value type with pure mutations — the view owns one `@State` copy.
public struct PairedDevicesState: Equatable, Sendable {
    public private(set) var peers: [PairedDevicePeer]

    public init(peers: [PairedDevicePeer] = [.macStudio]) {
        self.peers = peers
    }

    public var isEmpty: Bool { peers.isEmpty }

    public func peer(id: String) -> PairedDevicePeer? {
        peers.first { $0.id == id }
    }

    /// Confirmed removal: drops only the fixture peer; reaches the designed empty state after the
    /// last peer. No real device access is revoked.
    public mutating func remove(id: String) {
        peers.removeAll { $0.id == id }
    }

    /// Deterministic local refresh/retry. It re-checks for peers and finds none to add — a prototype
    /// never pairs a real device, so the list is unchanged. Present so the empty-state `Refresh
    /// devices` affordance is honest rather than fabricating a pairing.
    public mutating func refresh() {
        // Intentionally no mutation: no authored pairing handshake exists in this section.
    }
}

/// Static copy for the Paired-devices surfaces, kept in one place so SwiftUI and Compose stay
/// word-for-word identical to the source masters.
public enum PairedDevicesCopy {
    public static let navigationTitle = "Paired devices"
    public static let sectionHeader = "Paired devices"
    public static let listFooter = "Open a device to review its connection or remove its access."

    public static let emptyTitle = "No paired devices"
    public static let emptyMessage =
        "Open Rem on another device and sign in with the same account. It will appear here automatically."
    public static let refreshAction = "Refresh devices"
    /// Empty-state hero (iOS SF Symbol). Compose maps to the registry `devices` glyph.
    public static let emptySymbol = "macbook.and.iphone"

    public static let connectionSectionHeader = "Connection"
    public static let removeAccess = "Remove access"
    public static let detailFooter = "Removing this device revokes its access to Rem."

    public static let cancel = "Cancel"
    public static func removeConfirmationTitle(_ deviceName: String) -> String {
        "Remove access to \(deviceName)?"
    }
    public static let removeConfirmationMessage =
        "This device will be disconnected and will need to pair again."

    // Add is an entry point with no authored pairing destination. This is the explicit named local
    // simulation boundary — not an invented QR / pairing flow and not a fake "paired" success.
    public static let addTitle = "Pair a new device"
    public static let addMessage =
        "Pairing happens on your other device. Open Rem there and sign in with the same account and it will appear here automatically.\n\nThis local prototype does not simulate a pairing handshake and will not add a device."
    public static let addDismiss = "Done"
    public static let addAction = "Add"
}
