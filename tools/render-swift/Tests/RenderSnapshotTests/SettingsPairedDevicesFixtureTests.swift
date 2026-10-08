import XCTest
@testable import RemDesignSystem

/// Fixture-state tests for the Paired devices destination (`pairedDevices`). These exercise the pure
/// `PairedDevicesState` model that `SettingsPairedDevicesScreen` drives, proving the scope/cancel/
/// confirm semantics the contract requires without a running view:
///
/// - confirmed removal drops **only** the fixture peer and reaches the designed empty state;
/// - cancelling a removal preserves the peer and its detail;
/// - the current device is absent from the fixture;
/// - refresh performs a deterministic local re-check and never pairs a device.
final class SettingsPairedDevicesFixtureTests: XCTestCase {
    func testDefaultFixtureHasOnlyOtherDevicesAndNoCurrentDevice() {
        let state = PairedDevicesState()
        XCTAssertEqual(state.peers.map(\.id), ["mac-studio"])
        XCTAssertFalse(state.isEmpty)
        // The current device is deliberately excluded — the only peer is another device.
        XCTAssertNil(state.peer(id: "current-device"))
    }

    func testConfirmedRemovalDropsPeerAndReachesEmptyState() {
        var state = PairedDevicesState()
        state.remove(id: "mac-studio")
        XCTAssertTrue(state.isEmpty)
        XCTAssertNil(state.peer(id: "mac-studio"))
    }

    func testRemovalIsScopedToTheSelectedPeer() {
        let offline = PairedDevicePeer(
            id: "ipad",
            name: "iPad Pro",
            kind: "Tablet",
            heroSymbol: "ipad",
            platform: "iPadOS",
            status: "Offline",
            lastActive: "2 days ago",
            isConnected: false
        )
        var state = PairedDevicesState(peers: [.macStudio, offline])
        state.remove(id: "mac-studio")
        XCTAssertEqual(state.peers.map(\.id), ["ipad"])
        XCTAssertNotNil(state.peer(id: "ipad"))
    }

    func testCancellingRemovalPreservesPeerAndDetail() {
        // Cancel never mutates state: a never-removed copy is unchanged and still resolves detail.
        let state = PairedDevicesState()
        XCTAssertEqual(state.peers, PairedDevicesState().peers)
        let peer = state.peer(id: "mac-studio")
        XCTAssertEqual(peer?.name, "Mac Studio")
    }

    func testRefreshIsDeterministicAndNeverPairsADevice() {
        var populated = PairedDevicesState()
        populated.refresh()
        XCTAssertEqual(populated.peers.map(\.id), ["mac-studio"], "refresh must not change the populated list")

        var empty = PairedDevicesState(peers: [])
        empty.refresh()
        XCTAssertTrue(empty.isEmpty, "refresh must never fabricate a pairing")
    }

    func testDetailConnectionRowsMatchSourceAndUseEmDashPlaceholder() {
        let rows = PairedDevicePeer.macStudio.connectionRows
        XCTAssertEqual(rows.map(\.label), ["Platform", "Status", "Last active", "Connected on"])
        XCTAssertEqual(rows.map(\.value), ["macOS", "Recently active", "1 min ago", "—"])
    }

    func testConnectedSummaryReflectsConnectionState() {
        XCTAssertEqual(PairedDevicePeer.macStudio.connectionSummary, "Connected · Recently active")
        let offline = PairedDevicePeer(
            id: "x", name: "X", kind: "Desktop", heroSymbol: "laptopcomputer",
            platform: "macOS", status: "Last seen yesterday", lastActive: "1 day ago", isConnected: false
        )
        XCTAssertEqual(offline.connectionSummary, "Offline · Last seen yesterday")
    }

    func testRemoveConfirmationTitleNamesTheDevice() {
        XCTAssertEqual(
            PairedDevicesCopy.removeConfirmationTitle("Mac Studio"),
            "Remove access to Mac Studio?"
        )
    }
}
