package com.rem.designsystem.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * In-memory fixture state behind [SettingsCloudBrowserScreen] — the Compose twin of the SwiftUI
 * `CloudBrowserModel`. It owns every mutation the authored Cloud browser masters express (add site,
 * change permission, add / edit / remove a saved login, clear site data, clear all site data) so the
 * composables stay thin and every scope/cancel/save rule is unit-testable without a device.
 *
 * Authority: `docs/playground/settings-design/settings-destinations.md` (Cloud browser) and the raw
 * node contexts for `1833:5071`, `1868:6148/6153/6176/6197/6214/6227/6245/6250/6255`,
 * `1875:6710/6711/6712/6713/6714`, `1934:9071`, `1956:8160/8161/8162`.
 *
 * Prototype-only: all values are illustrative. No credential, cookie, or permission here is written
 * to the Keychain, shared preferences, the network, or a real browser; passwords are always masked on
 * display and never persisted beyond this playground session.
 */

/** The only two permission policies the authored masters enumerate. */
enum class CloudSitePermission(val label: String) { Ask("Ask"), Allow("Allow") }

/** A saved login. [password] is illustrative fixture data only; it is always rendered masked. */
data class CloudSavedLogin(val id: String, val username: String, val password: String) {
    /** Always-masked, fixed 12-bullet display so length never leaks. */
    val maskedPassword: String get() = "•".repeat(12)
}

/** One site the agent may open. Cookies/sign-in are fixture counters cleared by "Clear site data". */
data class CloudSite(
    val id: String,
    val domain: String,
    val permission: CloudSitePermission,
    val logins: List<CloudSavedLogin> = emptyList(),
    val cookieCount: Int = 0,
    val signedIn: Boolean = false,
) {
    /** Saved-login count when any exist, otherwise sign-in status (matches the authored rows). */
    val loginSummary: String
        get() = if (logins.isNotEmpty()) "${logins.size} saved login${if (logins.size == 1) "" else "s"}"
        else if (signedIn) "Signed in" else "No saved login"

    /** e.g. `Ask · 1 saved login`, `Allow · Signed in`, `Ask · No saved login`. */
    val rowSubtitle: String get() = "${permission.label} · $loginSummary"

    /** e.g. `12 cookies · Signed in` on the site-detail "Cookies & sessions" row. */
    val cookieSummary: String get() = "$cookieCount cookies · ${if (signedIn) "Signed in" else "Signed out"}"

    /** e.g. `12 illustrative cookies` on the Cookies & sessions screen. */
    val illustrativeCookies: String get() = "$cookieCount illustrative cookies"
}

class CloudBrowserState(
    defaultPermission: CloudSitePermission = CloudSitePermission.Ask,
    initialSites: List<CloudSite>? = null,
) {
    var defaultPermission by mutableStateOf(defaultPermission)
    val sites = mutableStateListOf<CloudSite>().also { it.addAll(initialSites ?: fixtureSites()) }

    private var counter = 0
    private fun nextId(prefix: String): String { counter += 1; return "$prefix-$counter" }

    /** The two "recent" sites shown on the root; "See all sites" opens the full list. */
    val recentSites: List<CloudSite> get() = sites.take(2)

    fun site(id: String): CloudSite? = sites.firstOrNull { it.id == id }

    fun setPermission(siteId: String, permission: CloudSitePermission) =
        replace(siteId) { it.copy(permission = permission) }

    /** Add a fixture site from an "Add site" draft, or return null when the domain is not usable. */
    fun addSite(urlDraft: String, permission: CloudSitePermission, username: String = "", password: String = ""): String? {
        val domain = deriveDomain(urlDraft) ?: return null
        val logins = buildList {
            val user = username.trim()
            if (user.isNotEmpty()) add(CloudSavedLogin(nextId("login"), user, password))
        }
        val site = CloudSite(nextId("site"), domain, permission, logins, cookieCount = 0, signedIn = false)
        sites.add(site)
        return site.id
    }

    /** Add a fixture login to one site, or return null when validation fails. */
    fun addLogin(siteId: String, username: String, password: String): String? {
        if (site(siteId) == null || !canAddLogin(username, password)) return null
        val login = CloudSavedLogin(nextId("login"), username.trim(), password)
        replace(siteId) { it.copy(logins = it.logins + login) }
        return login.id
    }

    /** Replace only the selected credential's username; every other login and site is untouched. */
    fun updateUsername(siteId: String, loginId: String, value: String) =
        mutateLogin(siteId, loginId) { it.copy(username = value.trim()) }

    /** Replace only the selected credential's password. Passwords stay masked on display. */
    fun updatePassword(siteId: String, loginId: String, value: String) =
        mutateLogin(siteId, loginId) { it.copy(password = value) }

    /** Remove only the selected credential. Cookies and other sites are preserved. */
    fun removeLogin(siteId: String, loginId: String) =
        replace(siteId) { site -> site.copy(logins = site.logins.filterNot { it.id == loginId }) }

    /** Clear cookies/sessions for one site. Saved logins (passwords) are preserved. */
    fun clearSiteData(siteId: String) = replace(siteId) { it.copy(cookieCount = 0, signedIn = false) }

    /** Clear cookies/sessions across every site. Saved logins (passwords) are preserved everywhere. */
    fun clearAllSiteData() {
        for (i in sites.indices) sites[i] = sites[i].copy(cookieCount = 0, signedIn = false)
    }

    private fun replace(siteId: String, change: (CloudSite) -> CloudSite) {
        val idx = sites.indexOfFirst { it.id == siteId }
        if (idx >= 0) sites[idx] = change(sites[idx])
    }

    private fun mutateLogin(siteId: String, loginId: String, change: (CloudSavedLogin) -> CloudSavedLogin) =
        replace(siteId) { site ->
            site.copy(logins = site.logins.map { if (it.id == loginId) change(it) else it })
        }

    companion object {
        /** The authored github.com / notion.so / linear.app / openai.com fixtures. */
        fun fixtureSites(): List<CloudSite> = listOf(
            CloudSite("github", "github.com", CloudSitePermission.Ask,
                logins = listOf(CloudSavedLogin("github-login", "samuel@example.com", "fixture-only")),
                cookieCount = 12, signedIn = true),
            CloudSite("notion", "notion.so", CloudSitePermission.Allow, cookieCount = 8, signedIn = true),
            CloudSite("linear", "linear.app", CloudSitePermission.Ask, cookieCount = 0, signedIn = false),
            CloudSite("openai", "openai.com", CloudSitePermission.Allow, cookieCount = 5, signedIn = true),
        )

        /**
         * Derive a usable bare domain from a URL or raw domain, or null when nothing usable can be
         * extracted. Strips scheme, userinfo, port, and path/query/fragment, lowercases, and requires
         * at least one dot with only domain-legal characters.
         */
        fun deriveDomain(raw: String): String? {
            var s = raw.trim().lowercase()
            if (s.isEmpty()) return null
            val scheme = s.indexOf("://")
            if (scheme >= 0) s = s.substring(scheme + 3)
            s = s.substringBefore('/').substringBefore('?').substringBefore('#')
            s = s.substringAfterLast('@')
            s = s.substringBefore(':')
            if (s.isEmpty() || !s.contains('.') || s.startsWith('.') || s.endsWith('.')) return null
            if (!s.all { it in "abcdefghijklmnopqrstuvwxyz0123456789.-" }) return null
            if (s.split('.').any { it.isEmpty() }) return null
            return s
        }

        /** A nonempty usable domain is the only requirement to save an "Add site" draft. */
        fun canAddSite(urlDraft: String): Boolean = deriveDomain(urlDraft) != null

        /** Both "Add login" fields must be nonempty fixture data. */
        fun canAddLogin(username: String, password: String): Boolean =
            username.trim().isNotEmpty() && password.isNotEmpty()
    }
}

/** Activity/navigation-owner retained fixture session. No SavedStateHandle or serialization: even
 * passwords entered in drafts remain only in memory and disappear with this owner/process. */
class CloudBrowserViewModel : ViewModel() {
    val state = CloudBrowserState()
    val stack = mutableStateListOf<CloudNav>(CloudNav.Root)
    val addSiteDraft = CloudSiteDraft()
    val addLoginDraft = CloudLoginDraft()
    val loginEditor = CloudLoginEditor()

    fun push(route: CloudNav) {
        clearDrafts()
        stack.add(route)
    }

    /** Returns false at root so the host can leave this destination. */
    fun pop(): Boolean {
        clearDrafts()
        if (stack.size == 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    private fun clearDrafts() {
        addSiteDraft.clear()
        addLoginDraft.clear()
        loginEditor.cancel()
    }
}

sealed interface CloudNav {
    data object Root : CloudNav
    data object Sites : CloudNav
    data object AddSite : CloudNav
    data class SiteDetail(val siteId: String) : CloudNav
    data class AddLogin(val siteId: String) : CloudNav
    data class SavedLogin(val siteId: String, val loginId: String) : CloudNav
    data class Cookies(val siteId: String) : CloudNav
}

class CloudSiteDraft {
    var url by mutableStateOf("")
    var permission by mutableStateOf(CloudSitePermission.Ask)
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    fun clear() { url = ""; permission = CloudSitePermission.Ask; username = ""; password = "" }
}

class CloudLoginDraft {
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    fun clear() { username = ""; password = "" }
}

class CloudLoginEditor {
    // 0 = view, 1 = username, 2 = password; password editing always starts empty.
    var field by mutableStateOf(0)
        private set
    var draft by mutableStateOf("")
    fun startUsername(username: String) { draft = username; field = 1 }
    fun startPassword() { draft = ""; field = 2 }
    fun cancel() { draft = ""; field = 0 }
    fun save(state: CloudBrowserState, siteId: String, loginId: String) {
        if (draft.trim().isEmpty()) return
        when (field) {
            1 -> state.updateUsername(siteId, loginId, draft)
            2 -> state.updatePassword(siteId, loginId, draft)
        }
        cancel()
    }
}
