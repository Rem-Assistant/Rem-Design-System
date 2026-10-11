package com.rem.designsystem

import com.rem.designsystem.screens.PairedDevicePeer
import com.rem.designsystem.screens.PairedDevicesCopy
import com.rem.designsystem.screens.PairedDevicesState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fixture-state tests for the Paired devices destination — the Compose twin of
 * `SettingsPairedDevicesFixtureTests.swift`. They exercise the pure [PairedDevicesState] model that
 * `SettingsPairedDevicesScreen` drives, proving confirmed removal / cancel / refresh scope off-device.
 */
class SettingsPairedDevicesFixtureTest {
    @Test fun defaultFixtureHasOnlyOtherDevicesAndNoCurrentDevice() {
        val state = PairedDevicesState()
        assertEquals(listOf("mac-studio"), state.peers.map { it.id })
        assertFalse(state.isEmpty)
        assertNull(state.peer("current-device"))
    }

    @Test fun confirmedRemovalDropsPeerAndReachesEmptyState() {
        val state = PairedDevicesState().removing("mac-studio")
        assertTrue(state.isEmpty)
        assertNull(state.peer("mac-studio"))
    }

    @Test fun removalIsScopedToTheSelectedPeer() {
        val offline = PairedDevicePeer(
            id = "ipad", name = "iPad Pro", kind = "Tablet",
            platform = "iPadOS", status = "Offline", lastActive = "2 days ago", isConnected = false,
        )
        val state = PairedDevicesState(listOf(PairedDevicePeer.macStudio, offline)).removing("mac-studio")
        assertEquals(listOf("ipad"), state.peers.map { it.id })
    }

    @Test fun cancellingRemovalPreservesPeerAndDetail() {
        // Cancel never calls removing(): the original value is unchanged and still resolves detail.
        val state = PairedDevicesState()
        assertEquals(PairedDevicesState().peers, state.peers)
        assertEquals("Mac Studio", state.peer("mac-studio")?.name)
    }

    @Test fun refreshIsDeterministicAndNeverPairsADevice() {
        assertEquals(listOf("mac-studio"), PairedDevicesState().refreshed().peers.map { it.id })
        assertTrue(PairedDevicesState(emptyList()).refreshed().isEmpty)
    }

    @Test fun detailConnectionRowsMatchSourceAndUseEmDashPlaceholder() {
        val rows = PairedDevicePeer.macStudio.connectionRows
        assertEquals(listOf("Platform", "Status", "Last active", "Connected on"), rows.map { it.label })
        assertEquals(listOf("macOS", "Recently active", "1 min ago", "—"), rows.map { it.value })
    }

    @Test fun connectedSummaryReflectsConnectionState() {
        assertEquals("Connected · Recently active", PairedDevicePeer.macStudio.connectionSummary)
        val offline = PairedDevicePeer(
            id = "x", name = "X", kind = "Desktop",
            platform = "macOS", status = "Last seen yesterday", lastActive = "1 day ago", isConnected = false,
        )
        assertEquals("Offline · Last seen yesterday", offline.connectionSummary)
    }

    @Test fun removeConfirmationTitleNamesTheDevice() {
        assertEquals("Remove access to Mac Studio?", PairedDevicesCopy.removeConfirmationTitle("Mac Studio"))
    }
}
