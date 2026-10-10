package com.rem.designsystem

import com.rem.designsystem.buttons.RemButtonVariant
import com.rem.designsystem.chat.ConnectorCardModel
import com.rem.designsystem.chat.ConnectorCardState
import com.rem.designsystem.chat.LoginCardModel
import com.rem.designsystem.chat.LoginCardState
import com.rem.designsystem.chat.PermissionCardModel
import com.rem.designsystem.chat.PermissionCardState
import com.rem.designsystem.chat.PermissionParameter
import com.rem.designsystem.chat.PermissionRisk
import com.rem.designsystem.chat.PermissionRequestDetails
import com.rem.designsystem.chat.loginFormCanSave
import com.rem.designsystem.rows.ConnectorProvider
import com.rem.designsystem.screens.CloudBrowserState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Display-model contract for the chat cards (ConnectorCard / LoginCard / PermissionCard). Paired with the
 * SwiftUI `ChatCardsTests.swift`; both assert the same strings so the two platforms cannot drift silently.
 */
class ChatCardsTest {
    private fun connector(state: ConnectorCardState) =
        ConnectorCardModel(ConnectorProvider.Gmail, "Search, read, draft, and manage email.", state)

    @Test fun connectorControlPerState() {
        assertEquals(ConnectorCardModel.Control.Authorize("Authorize"), connector(ConnectorCardState.Authorize).control)
        assertEquals(ConnectorCardModel.Control.Progress("Adding…"), connector(ConnectorCardState.Connecting).control)
        assertEquals(ConnectorCardModel.Control.Receipt("Added"), connector(ConnectorCardState.Added).control)
        assertEquals(ConnectorCardModel.Control.Retry("Retry"), connector(ConnectorCardState.Error()).control)
    }

    @Test fun connectorTitleDefaultsToProviderAndPurposeIsPermanent() {
        listOf(ConnectorCardState.Authorize, ConnectorCardState.Connecting, ConnectorCardState.Added, ConnectorCardState.Error())
            .forEach {
                assertEquals("Gmail", connector(it).title)
                assertEquals("Search, read, draft, and manage email.", connector(it).subtitle)
            }
    }

    @Test fun connectorErrorCopyOnlyInErrorState() {
        assertNull(connector(ConnectorCardState.Authorize).errorMessage)
        assertNull(connector(ConnectorCardState.Connecting).errorMessage)
        assertNull(connector(ConnectorCardState.Added).errorMessage)
        assertEquals("Gmail authorization failed. Retry to connect.", connector(ConnectorCardState.Error()).errorMessage)
        assertEquals("Gmail authorization failed. Retry to connect.", connector(ConnectorCardState.Error("  ")).errorMessage)
        assertEquals("Access was revoked.", connector(ConnectorCardState.Error("Access was revoked.")).errorMessage)
    }

    @Test fun loginCopyAndButtonVariantPerState() {
        val entry = LoginCardModel("GitHub login details", "github.com", LoginCardState.Entry)
        assertEquals("Add the login this browser task needs.", entry.detail)
        assertEquals("Add login", entry.buttonLabel)
        assertEquals(RemButtonVariant.RectBlue, entry.buttonVariant)
        val saved = LoginCardModel("GitHub login details", "github.com", LoginCardState.Saved)
        assertEquals("Login saved for this site.", saved.detail)
        assertEquals("Saved", saved.buttonLabel)
        assertEquals(RemButtonVariant.RectSecondary, saved.buttonVariant)
    }

    @Test fun loginFormSaveRuleMatchesCloudBrowser() {
        listOf("" to "", "  " to "pw", "dev@example.com" to "", "dev@example.com" to "pw", "u" to " ").forEach { (u, p) ->
            assertEquals("$u/$p", CloudBrowserState.canAddLogin(u, p), loginFormCanSave(u, p))
        }
        assertFalse(loginFormCanSave("  ", "pw"))
        assertTrue(loginFormCanSave("dev@example.com", "pw"))
    }

    private fun permission(state: PermissionCardState, scope: String? = "create reminders in Personal only.") =
        PermissionCardModel(
            title = "Reminder permission", question = "Allow Rem to create this reminder?",
            summary = "One reminder in your Personal list.",
            details = PermissionRequestDetails("Send investor update", "Oct 10, 2026 · 9:00 AM UTC", "Reminders · Personal"),
            state = state, alwaysAllowScope = scope,
        )

    @Test fun awaitingExpandsAndResolvedStatesCollapse() {
        assertTrue(PermissionCardState.Awaiting.defaultExpanded)
        assertFalse(PermissionCardState.Allowed.defaultExpanded)
        assertFalse(PermissionCardState.Denied.defaultExpanded)
    }

    @Test fun receiptIsOneConciseStatusNeverDeniedNotSet() {
        assertEquals("Needs your approval", permission(PermissionCardState.Awaiting).subtitle)
        assertEquals("Allowed once", permission(PermissionCardState.Allowed).subtitle)
        assertEquals("Denied", permission(PermissionCardState.Denied).subtitle)
        PermissionCardState.entries.forEach {
            val receipt = permission(it).subtitle.lowercase()
            assertFalse(receipt, receipt.contains("not set"))
            assertFalse(receipt, receipt.contains("•"))
            assertFalse(receipt, receipt.contains("·"))
        }
    }

    @Test fun decisionsOnlyWhileAwaitingAndReviewAgainOnlyWhenDenied() {
        assertTrue(permission(PermissionCardState.Awaiting).showsDecisions)
        assertFalse(permission(PermissionCardState.Allowed).showsDecisions)
        assertFalse(permission(PermissionCardState.Denied).showsDecisions)
        assertTrue(permission(PermissionCardState.Denied).showsReviewAgain)
        assertFalse(permission(PermissionCardState.Awaiting).showsReviewAgain)
        assertFalse(permission(PermissionCardState.Allowed).showsReviewAgain)
    }

    @Test fun alwaysAllowIsAScopedProposalOnlyWhileAwaiting() {
        assertTrue(permission(PermissionCardState.Awaiting).showsAlwaysAllow)
        assertEquals("Proposed Always allow scope: create reminders in Personal only.",
            permission(PermissionCardState.Awaiting).alwaysAllowFootnote)
        assertFalse(permission(PermissionCardState.Awaiting, scope = null).showsAlwaysAllow)
        assertNull(permission(PermissionCardState.Awaiting, scope = null).alwaysAllowFootnote)
        assertNull(permission(PermissionCardState.Allowed).alwaysAllowFootnote)
        assertNull(permission(PermissionCardState.Denied).alwaysAllowFootnote)
    }

    @Test fun requestPayloadIsPreservedAcrossStates() {
        assertEquals(1, PermissionCardState.entries.map { permission(it).details }.toSet().size)
    }

    @Test fun riskLabelOnlyWhenElevated() {
        assertEquals(PermissionRisk.Standard, permission(PermissionCardState.Awaiting).risk)
        assertNull(permission(PermissionCardState.Awaiting).riskLabel)
        assertEquals("Elevated risk", permission(PermissionCardState.Awaiting).copy(risk = PermissionRisk.Elevated).riskLabel)
    }

    @Test fun fullParametersHiddenWhenEmpty() {
        assertFalse(permission(PermissionCardState.Awaiting).showsParameters)
        assertTrue(permission(PermissionCardState.Awaiting)
            .copy(parameters = listOf(PermissionParameter("to", "investors@example.com"))).showsParameters)
    }

    @Test fun detailRowsAreLabelValuePairsOfTheRequest() {
        assertEquals(
            listOf(
                PermissionParameter("Action", "Send investor update"),
                PermissionParameter("When", "Oct 10, 2026 · 9:00 AM UTC"),
                PermissionParameter("Source", "Reminders · Personal"),
            ),
            permission(PermissionCardState.Awaiting).detailRows,
        )
    }
}
