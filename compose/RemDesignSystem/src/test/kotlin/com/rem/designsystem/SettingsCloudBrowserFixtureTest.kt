package com.rem.designsystem

import com.rem.designsystem.screens.CloudBrowserState
import com.rem.designsystem.screens.CloudSitePermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fixture-state proof for the Compose Cloud browser destination. These assert the scope/cancel/save
 * rules the authored masters require, without a device — the same contract the SwiftUI
 * `SettingsCloudBrowserFixtureTests` prove on iOS.
 */
class SettingsCloudBrowserFixtureTest {

    private fun state() = CloudBrowserState()

    @Test fun deriveDomainStripsSchemeAndPath() {
        assertEquals("example.com", CloudBrowserState.deriveDomain("https://example.com/path?q=1#x"))
        assertEquals("sub.example.co.uk", CloudBrowserState.deriveDomain("HTTP://user@sub.example.co.uk:8080/"))
        assertEquals("github.com", CloudBrowserState.deriveDomain("  github.com  "))
    }

    @Test fun deriveDomainRejectsUnusableInput() {
        assertNull(CloudBrowserState.deriveDomain(""))
        assertNull(CloudBrowserState.deriveDomain("   "))
        assertNull(CloudBrowserState.deriveDomain("not a domain"))
        assertNull(CloudBrowserState.deriveDomain("localhost"))
        assertNull(CloudBrowserState.deriveDomain("https://"))
        assertNull(CloudBrowserState.deriveDomain("a..b"))
    }

    @Test fun addSiteValidatesDomainAndAppends() {
        val s = state()
        val before = s.sites.size
        assertFalse(CloudBrowserState.canAddSite(""))
        assertNull(s.addSite("", CloudSitePermission.Ask))
        assertEquals(before, s.sites.size)

        assertTrue(CloudBrowserState.canAddSite("https://acme.dev"))
        val id = s.addSite("https://acme.dev/login", CloudSitePermission.Allow)
        assertNotNull(id)
        val added = s.site(id!!)!!
        assertEquals("acme.dev", added.domain)
        assertEquals(CloudSitePermission.Allow, added.permission)
        assertEquals(before + 1, s.sites.size)
        assertTrue(added.logins.isEmpty())
    }

    @Test fun addSiteWithUsernameCreatesOneOptionalLogin() {
        val s = state()
        val id = s.addSite("beta.io", CloudSitePermission.Ask, username = "me@beta.io", password = "pw")!!
        assertEquals(1, s.site(id)!!.logins.size)
        assertEquals("me@beta.io", s.site(id)!!.logins.first().username)
    }

    @Test fun rowSubtitleMatchesAuthoredFixtures() {
        val s = state()
        assertEquals("Ask · 1 saved login", s.site("github")!!.rowSubtitle)
        assertEquals("Allow · Signed in", s.site("notion")!!.rowSubtitle)
        assertEquals("Ask · No saved login", s.site("linear")!!.rowSubtitle)
    }

    @Test fun setPermissionIsScopedToOneSite() {
        val s = state()
        s.setPermission("github", CloudSitePermission.Allow)
        assertEquals(CloudSitePermission.Allow, s.site("github")!!.permission)
        assertEquals(CloudSitePermission.Allow, s.site("notion")!!.permission) // unchanged (already Allow)
        assertEquals(CloudSitePermission.Ask, s.site("linear")!!.permission)   // untouched
    }

    @Test fun addLoginRequiresBothFields() {
        val s = state()
        assertFalse(CloudBrowserState.canAddLogin("", "pw"))
        assertFalse(CloudBrowserState.canAddLogin("u", ""))
        assertNull(s.addLogin("linear", "", "pw"))
        assertTrue(s.site("linear")!!.logins.isEmpty())

        assertTrue(CloudBrowserState.canAddLogin("dev@linear.app", "pw"))
        val id = s.addLogin("linear", "dev@linear.app", "pw")
        assertNotNull(id)
        assertEquals(1, s.site("linear")!!.logins.size)
    }

    @Test fun editUsernameUpdatesOnlySelectedCredential() {
        val s = state()
        val second = s.addLogin("github", "alt@github.com", "pw")!!
        val first = s.site("github")!!.logins.first().id

        s.updateUsername("github", first, "changed@github.com")
        assertEquals("changed@github.com", s.site("github")!!.logins.first { it.id == first }.username)
        assertEquals("alt@github.com", s.site("github")!!.logins.first { it.id == second }.username)
    }

    @Test fun editPasswordUpdatesOnlySelectedCredential() {
        val s = state()
        val login = s.site("github")!!.logins.first().id
        s.updatePassword("github", login, "new-secret")
        assertEquals("new-secret", s.site("github")!!.logins.first().password)
        // Display stays masked regardless of value.
        assertEquals("•".repeat(12), s.site("github")!!.logins.first().maskedPassword)
    }

    @Test fun removeLoginAffectsOnlyThatLoginAndPreservesCookies() {
        val s = state()
        val extra = s.addLogin("github", "alt@github.com", "pw")!!
        val cookiesBefore = s.site("github")!!.cookieCount
        s.removeLogin("github", extra)
        assertEquals(1, s.site("github")!!.logins.size)
        assertEquals("samuel@example.com", s.site("github")!!.logins.first().username)
        assertEquals(cookiesBefore, s.site("github")!!.cookieCount)
        assertTrue(s.site("github")!!.signedIn)
    }

    @Test fun clearSiteDataZeroesCookiesButKeepsPasswords() {
        val s = state()
        s.clearSiteData("github")
        val site = s.site("github")!!
        assertEquals(0, site.cookieCount)
        assertFalse(site.signedIn)
        assertEquals(1, site.logins.size) // credential preserved
        // Other sites untouched.
        assertEquals(8, s.site("notion")!!.cookieCount)
    }

    @Test fun clearAllSiteDataKeepsEveryPassword() {
        val s = state()
        s.clearAllSiteData()
        assertTrue(s.sites.all { it.cookieCount == 0 && !it.signedIn })
        assertEquals(1, s.site("github")!!.logins.size) // passwords retained everywhere
    }
}
