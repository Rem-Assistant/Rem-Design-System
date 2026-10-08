package com.rem.designsystem

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.rem.designsystem.screens.*
import org.junit.Assert.*
import org.junit.Test

class SettingsConnectorsFixtureTest {
    private val second = GmailAccountFixture("second", "second@example.com", "Secondary")
    @Test fun connectorChoicesInheritExceptExplicitOverride() {
        var fixture = GmailFixture(accounts = listOf(GmailAccountFixture.primary, second))
            .withAccountPermission("avery", GmailPermission.NeverAllow)
        GmailPermission.entries.forEach { policy ->
            fixture = fixture.withConnectorPermission(policy)
            assertEquals(policy, fixture.connectorPermission)
            assertEquals(policy, fixture.permission("second"))
            assertEquals(GmailPermission.NeverAllow, fixture.permission("avery"))
        }
        GmailPermission.entries.forEach { policy ->
            fixture = fixture.withAccountPermission("avery", policy)
            assertEquals(policy, fixture.permission("avery"))
            assertEquals(GmailPermission.NeverAllow, fixture.permission("second"))
        }
        assertEquals(fixture, fixture.withAccountPermission("missing", GmailPermission.AlwaysAllow))
    }
    @Test fun disconnectOneVersusAllPreservesCorrectScope() {
        val start = GmailFixture(accounts = listOf(GmailAccountFixture.primary, second))
            .withConnectorPermission(GmailPermission.AlwaysAsk)
            .withAccountPermission("avery", GmailPermission.NeverAllow)
            .withAccountPermission("second", GmailPermission.AlwaysAllow)
        val one = start.disconnect("avery")
        assertEquals(listOf(second), one.accounts)
        assertNull(one.accountOverrides["avery"])
        assertEquals(GmailPermission.AlwaysAllow, one.permission("second"))
        assertEquals(GmailPermission.AlwaysAsk, one.connectorPermission)
        assertEquals(one, one.disconnect("missing"))
        val all = start.disconnectAll()
        assertFalse(all.isConnected)
        assertTrue(all.accountOverrides.isEmpty())
        assertEquals(GmailPermission.AlwaysAsk, all.connectorPermission)
    }
    @Test fun backFromPermissionReturnsToGmailWithoutDisconnecting() {
        val session = SettingsConnectorsSession()
        session.openGmail()
        session.openPermissions("avery")
        session.selectPermission(GmailPermission.NeverAllow, "avery")
        assertTrue(session.back())
        assertEquals(ConnectorRoute.Gmail, session.route)
        assertEquals(GmailPermission.NeverAllow, session.fixture.permission("avery"))
        assertEquals(GmailPermission.LowRisk, session.fixture.connectorPermission)
        assertTrue(session.fixture.isConnected)
        assertTrue(session.back())
        assertEquals(ConnectorRoute.Root, session.route)
        assertFalse(session.back())
        session.openGmail()
        session.disconnect("avery")
        assertEquals(ConnectorRoute.Root, session.route)
        assertFalse(session.fixture.isConnected)
    }
    @Test fun retainedOwnerDoesNotResurrectDisconnectedAccount() {
        val store = ViewModelStore()
        try {
            fun session() = ViewModelProvider(store, ViewModelProvider.NewInstanceFactory()).get(SettingsConnectorsSession::class.java)
            val first = session()
            first.openGmail()
            first.disconnect()
            val recreated = session()
            assertSame(first, recreated)
            assertFalse(recreated.fixture.isConnected)
            assertEquals(ConnectorRoute.Root, recreated.route)
        } finally { store.clear() }
    }
}
