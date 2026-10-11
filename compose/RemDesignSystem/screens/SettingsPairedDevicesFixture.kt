package com.rem.designsystem.screens

/**
 * Deterministic, in-memory fixture model for the **Paired devices** Settings destination
 * (`AgentSettingsDestination.PairedDevices`) — the Compose twin of the SwiftUI
 * `PairedDevicesState` / `PairedDevicePeer`.
 *
 * Source evidence: `docs/playground/settings-design/settings-destinations.md` (Paired devices) and
 * the masters `1833:5031` (populated), `1833:52316` (empty), `1833:52344` (detail),
 * `1839:52547` (remove confirmation).
 *
 * Pure Kotlin, no Compose/Android imports, so scope/cancel/remove/refresh is unit-testable off-device
 * (mirrors `SignInState`). Every value is illustrative fixture data; nothing pairs, authenticates, or
 * revokes a real device. Only *other* paired devices appear — the current device is absent.
 */
data class PairedDevicePeer(
    val id: String,
    /** Display name + detail hero title, e.g. `Mac Studio`. */
    val name: String,
    /** Device class subtitle under the hero, e.g. `Desktop`. */
    val kind: String,
    /** Connection-section values. All illustrative. */
    val platform: String,
    val status: String,
    val lastActive: String,
    /** Whether the list row shows the green connected dot; a peer can be connected or offline. */
    val isConnected: Boolean,
    /** `approvedAtMs` display. `null` → em dash (no factual date supplied for the fixture). */
    val approvedAtDisplay: String? = null,
) {
    /** The list-row connection summary, e.g. `Connected · Recently active`. */
    val connectionSummary: String
        get() = "${if (isConnected) "Connected" else "Offline"} · $status"

    /** Ordered `Connection` detail rows; `Connected on` falls back to the em-dash specimen. */
    val connectionRows: List<PairedDeviceConnectionRow>
        get() = listOf(
            PairedDeviceConnectionRow("Platform", platform),
            PairedDeviceConnectionRow("Status", status),
            PairedDeviceConnectionRow("Last active", lastActive),
            PairedDeviceConnectionRow("Connected on", approvedAtDisplay ?: "—"),
        )

    companion object {
        /** The one designed fixture peer (`1833:5031` / `1833:52344`). */
        val macStudio = PairedDevicePeer(
            id = "mac-studio",
            name = "Mac Studio",
            kind = "Desktop",
            platform = "macOS",
            status = "Recently active",
            lastActive = "1 min ago",
            isConnected = true,
            approvedAtDisplay = null,
        )
    }
}

/** A label/value pair in the detail `Connection` section. */
data class PairedDeviceConnectionRow(val label: String, val value: String)

/** In-memory list state. Immutable value type with pure transitions — the screen holds one copy. */
data class PairedDevicesState(val peers: List<PairedDevicePeer> = listOf(PairedDevicePeer.macStudio)) {
    val isEmpty: Boolean get() = peers.isEmpty()

    fun peer(id: String): PairedDevicePeer? = peers.firstOrNull { it.id == id }

    /** Confirmed removal: drops only the fixture peer; reaches the designed empty state. */
    fun removing(id: String): PairedDevicesState = copy(peers = peers.filterNot { it.id == id })

    /**
     * Deterministic local refresh/retry — re-checks for peers and finds none to add. A prototype
     * never pairs a real device, so the list is unchanged. Present so the empty-state `Refresh
     * devices` affordance is honest rather than fabricating a pairing.
     */
    fun refreshed(): PairedDevicesState = this
}

/** Static copy, kept identical to the SwiftUI `PairedDevicesCopy` so both platforms stay in sync. */
object PairedDevicesCopy {
    const val NAVIGATION_TITLE = "Paired devices"
    const val SECTION_HEADER = "Paired devices"
    const val LIST_FOOTER = "Open a device to review its connection or remove its access."

    const val EMPTY_TITLE = "No paired devices"
    const val EMPTY_MESSAGE =
        "Open Rem on another device and sign in with the same account. It will appear here automatically."
    const val REFRESH_ACTION = "Refresh devices"

    const val CONNECTION_SECTION_HEADER = "Connection"
    const val REMOVE_ACCESS = "Remove access"
    const val DETAIL_FOOTER = "Removing this device revokes its access to Rem."

    const val CANCEL = "Cancel"
    fun removeConfirmationTitle(deviceName: String): String = "Remove access to $deviceName?"
    const val REMOVE_CONFIRMATION_MESSAGE =
        "This device will be disconnected and will need to pair again."

    // Add is an entry point with no authored pairing destination: an explicit named local simulation
    // boundary, not an invented QR / pairing flow and not a fake "paired" success.
    const val ADD_TITLE = "Pair a new device"
    const val ADD_MESSAGE =
        "Pairing happens on your other device. Open Rem there and sign in with the same account and it will appear here automatically.\n\nThis local prototype does not simulate a pairing handshake and will not add a device."
    const val ADD_DISMISS = "Done"
    const val ADD_ACTION = "Add"
}
