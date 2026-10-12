package com.rem.designsystem.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.chat.LoginForm
import com.rem.designsystem.rows.DisclosureChevron
import com.rem.designsystem.rows.ListRowLabel
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * **SettingsCloudBrowserScreen** — the Settings → Agent settings → **Cloud browser** destination
 * (Compose twin of the SwiftUI `SettingsCloudBrowserScreen`). It owns its own scaffold/title and an
 * internal navigation stack: a default permission, a recent-sites list, per-site access, saved logins
 * (view / add / edit / remove), cookies & sessions, and the destructive clear-data paths — each with
 * the authored confirmation. The top-bar back arrow pops the internal stack; at the root it calls
 * [onBack], which the host wires to its own navigation.
 *
 * Authority: `docs/playground/settings-design/settings-destinations.md` (Cloud browser) and the raw
 * node contexts / screenshots for `1833:5071`, `1868:6148/6153/6176/6197/6214/6227/6245/6250/6255`,
 * `1875:6710/6711/6712/6713/6714`, `1934:9071`, `1956:8160/8161/8162`.
 *
 * Prototype-only: all text entry is illustrative; passwords are always masked and never persisted
 * beyond the in-memory [CloudBrowserState] for this playground session.
 */
@Composable
fun SettingsCloudBrowserScreen(onBack: () -> Unit, session: CloudBrowserViewModel = viewModel()) {
    val state = session.state
    val stack = session.stack
    fun push(nav: CloudNav) { session.push(nav) }
    fun pop() { if (!session.pop()) onBack() }
    BackHandler { pop() }

    when (val top = stack.last()) {
        CloudNav.Root -> CloudRootScreen(state, onBack = ::pop,
            onOpenSite = { push(CloudNav.SiteDetail(it)) },
            onSeeAll = { push(CloudNav.Sites) },
            onAddSite = { push(CloudNav.AddSite) })
        CloudNav.Sites -> CloudSitesScreen(state, onBack = ::pop,
            onOpenSite = { push(CloudNav.SiteDetail(it)) }, onAddSite = { push(CloudNav.AddSite) })
        CloudNav.AddSite -> CloudAddSiteScreen(state, session.addSiteDraft, onBack = ::pop, onSaved = ::pop)
        is CloudNav.SiteDetail -> CloudSiteDetailScreen(state, top.siteId, onBack = ::pop,
            onOpenLogin = { push(CloudNav.SavedLogin(top.siteId, it)) },
            onAddLogin = { push(CloudNav.AddLogin(top.siteId)) },
            onCookies = { push(CloudNav.Cookies(top.siteId)) })
        is CloudNav.AddLogin -> CloudAddLoginScreen(state, session.addLoginDraft, top.siteId, onBack = ::pop, onSaved = ::pop)
        is CloudNav.SavedLogin -> CloudSavedLoginScreen(state, session.loginEditor, top.siteId, top.loginId,
            onBack = ::pop, onRemoved = ::pop)
        is CloudNav.Cookies -> CloudCookiesScreen(state, top.siteId, onBack = ::pop)
    }
}

// MARK: - Screens

@Composable
private fun CloudRootScreen(state: CloudBrowserState, onBack: () -> Unit,
                            onOpenSite: (String) -> Unit, onSeeAll: () -> Unit, onAddSite: () -> Unit) {
    var confirmClearAll by remember { mutableStateOf(false) }
    CloudScaffold("Cloud browser", onBack = onBack, testTag = "cloudBrowser.root") {
        CloudSection(header = "Default access") {
            CloudPermissionRow("Default permission", "Ask before Rem opens a new site.",
                selected = state.defaultPermission, onSelect = { state.defaultPermission = it },
                testTag = "cloudBrowser.defaultPermission")
        }
        CloudSection(
            header = "Sites", headerAction = "Add site", onHeaderAction = onAddSite,
            headerActionTag = "cloudBrowser.addSite",
            footer = "Recent sites appear here. Add a site to configure its access.",
        ) {
            state.recentSites.forEachIndexed { index, site ->
                CloudSiteRow(site, onClick = { onOpenSite(site.id) })
                if (index != state.recentSites.lastIndex) CloudRowDivider()
            }
            CloudRowDivider()
            CloudLinkRow("See all sites", onClick = onSeeAll, testTag = "cloudBrowser.seeAllSites")
        }
        CloudSection(header = "Browser data", footer = "Saved passwords remain until you remove them.") {
            CloudDestructiveRow("Clear all site data", "Cookies and sessions across every site.",
                onClick = { confirmClearAll = true }, testTag = "cloudBrowser.clearAllData")
        }
    }
    if (confirmClearAll) {
        CloudConfirm("Clear data for all sites?",
            "Rem will be signed out of all sites. Saved logins are kept.",
            confirmTitle = "Clear all site data", confirmTag = "cloudBrowser.confirmClearAll",
            onConfirm = { state.clearAllSiteData(); confirmClearAll = false },
            onCancel = { confirmClearAll = false })
    }
}

@Composable
private fun CloudSitesScreen(state: CloudBrowserState, onBack: () -> Unit,
                             onOpenSite: (String) -> Unit, onAddSite: () -> Unit) {
    CloudScaffold("Sites", onBack = onBack, testTag = "cloudBrowser.sitesList",
        actionTitle = "Add", actionTag = "cloudBrowser.sites.addSite", onAction = onAddSite) {
        CloudSection(header = "Sites") {
            state.sites.forEachIndexed { index, site ->
                CloudSiteRow(site, onClick = { onOpenSite(site.id) })
                if (index != state.sites.lastIndex) CloudRowDivider()
            }
        }
    }
}

@Composable
private fun CloudAddSiteScreen(state: CloudBrowserState, form: CloudSiteDraft,
                               onBack: () -> Unit, onSaved: () -> Unit) {
    val canSave = CloudBrowserState.canAddSite(form.url)
    CloudScaffold("Add site", onBack = onBack, testTag = "cloudBrowser.addSiteForm",
        saveEnabled = canSave, saveTag = "cloudBrowser.addSite.save",
        onSave = { if (state.addSite(form.url, form.permission, form.username, form.password) != null) onSaved() }) {
        CloudSection {
            CloudUrlField("Enter domain or URL", "https://example.com", form.url, { form.url = it },
                autofocus = false, testTag = "cloudBrowser.addSite.domainField")
        }
        CloudSection(header = "Access") {
            CloudPermissionRow("Permission", "Choose how Rem should handle this site.",
                selected = form.permission, onSelect = { form.permission = it })
        }
        CloudSection(header = "Login details",
            footer = "Save a login now, or add one later from the site's detail screen.") {
            CloudInputField("Username or email (optional)", form.username, { form.username = it },
                testTag = "cloudBrowser.addSite.username")
            CloudRowDivider()
            CloudInputField("Password (optional)", form.password, { form.password = it }, secure = true,
                testTag = "cloudBrowser.addSite.password")
        }
    }
}

@Composable
private fun CloudSiteDetailScreen(state: CloudBrowserState, siteId: String, onBack: () -> Unit,
                                  onOpenLogin: (String) -> Unit, onAddLogin: () -> Unit, onCookies: () -> Unit) {
    val site = state.site(siteId)
    if (site == null) { LaunchedEffect(Unit) { onBack() }; return }
    var confirmClear by remember { mutableStateOf(false) }
    CloudScaffold(site.domain, onBack = onBack, testTag = "cloudBrowser.siteDetail") {
        CloudSection(header = "Access") {
            CloudPermissionRow("Permission", "Controls whether Rem can open this site.",
                selected = site.permission, onSelect = { state.setPermission(siteId, it) },
                testTag = "cloudBrowser.siteDetail.permission")
        }
        CloudSection(header = "Saved logins") {
            site.logins.forEach { login ->
                CloudNavRow(login.username, "Password saved securely", onClick = { onOpenLogin(login.id) },
                    testTag = "cloudBrowser.siteDetail.login")
                CloudRowDivider()
            }
            CloudLinkRow("Add login", onClick = onAddLogin, testTag = "cloudBrowser.siteDetail.addLogin")
        }
        CloudSection(header = "Site data") {
            CloudNavRow("Cookies & sessions", site.cookieSummary, onClick = onCookies,
                testTag = "cloudBrowser.siteDetail.cookies")
            CloudRowDivider()
            CloudDestructiveRow("Clear site data", "Signs Rem out of ${site.domain}.",
                onClick = { confirmClear = true }, testTag = "cloudBrowser.siteDetail.clearSiteData")
        }
    }
    if (confirmClear) {
        CloudConfirm("Clear data for ${site.domain}?",
            "Rem will be signed out of this site. Saved logins are kept.",
            confirmTitle = "Clear site data", confirmTag = "cloudBrowser.siteDetail.confirmClear",
            onConfirm = { state.clearSiteData(siteId); confirmClear = false },
            onCancel = { confirmClear = false })
    }
}

@Composable
private fun CloudAddLoginScreen(state: CloudBrowserState, form: CloudLoginDraft, siteId: String,
                                onBack: () -> Unit, onSaved: () -> Unit) {
    val domain = state.site(siteId)?.domain ?: "this site"
    val canSave = CloudBrowserState.canAddLogin(form.username, form.password)
    CloudScaffold("Add login", onBack = onBack, testTag = "cloudBrowser.addLoginForm",
        saveEnabled = canSave, saveTag = "cloudBrowser.addLogin.save",
        onSave = { if (state.addLogin(siteId, form.username, form.password) != null) onSaved() }) {
        // The shared native form (also opened from the chat LoginCard); fields bind to the draft only.
        LoginForm(domain, form.username, { form.username = it }, form.password, { form.password = it },
            testTagPrefix = "cloudBrowser.addLogin")
    }
}

@Composable
private fun CloudSavedLoginScreen(state: CloudBrowserState, editor: CloudLoginEditor, siteId: String, loginId: String,
                                  onBack: () -> Unit, onRemoved: () -> Unit) {
    val site = state.site(siteId)
    val login = site?.logins?.firstOrNull { it.id == loginId }
    if (site == null || login == null) { LaunchedEffect(Unit) { onBack() }; return }

    val editing = editor.field
    val draft = editor.draft
    // Registered after the destination handler: Back cancels an edit before popping the route.
    BackHandler(enabled = editing != 0) { editor.cancel() }
    var confirmRemove by remember { mutableStateOf(false) }
    val canSave = draft.trim().isNotEmpty()

    val content: @Composable ColumnScope.() -> Unit = {
        CloudSection(header = "Website") {
            CloudValueRow(site.domain, "This credential is scoped to this site.")
        }
        CloudSection(header = "Login details",
            footer = "Illustrative values. Saved credentials require secure storage and explicit authorization.") {
            if (editing == 1) {
                CloudEditField("Username or email", "Username or email", draft, { editor.draft = it },
                    testTag = "cloudBrowser.savedLogin.usernameField")
            } else {
                CloudEditRow("Username or email", login.username, showPencil = editing == 0,
                    onEdit = { editor.startUsername(login.username) }, testTag = "cloudBrowser.savedLogin.editUsername")
            }
            CloudRowDivider()
            if (editing == 2) {
                CloudEditField("Password", "Password", draft, { editor.draft = it }, secure = true,
                    testTag = "cloudBrowser.savedLogin.passwordField")
            } else {
                CloudEditRow("Password", login.maskedPassword, showPencil = editing == 0,
                    onEdit = { editor.startPassword() }, testTag = "cloudBrowser.savedLogin.editPassword")
            }
        }
        CloudSection {
            CloudCenteredDestructive("Remove login", onClick = { confirmRemove = true },
                testTag = "cloudBrowser.savedLogin.remove")
        }
    }

    if (editing == 0) {
        CloudScaffold("Saved login", onBack = onBack, testTag = "cloudBrowser.savedLogin", content = content)
    } else {
        CloudEditScaffold("Saved login",
            onCancel = { editor.cancel() }, cancelTag = "cloudBrowser.savedLogin.cancel",
            saveEnabled = canSave, saveTag = "cloudBrowser.savedLogin.save",
            onSave = { editor.save(state, siteId, loginId) }, content = content)
    }

    if (confirmRemove) {
        CloudConfirm("Remove saved login?", "This removes the saved credential for ${site.domain}.",
            confirmTitle = "Remove login", confirmTag = "cloudBrowser.savedLogin.confirmRemove",
            onConfirm = { state.removeLogin(siteId, loginId); confirmRemove = false; onRemoved() },
            onCancel = { confirmRemove = false })
    }
}

@Composable
private fun CloudCookiesScreen(state: CloudBrowserState, siteId: String, onBack: () -> Unit) {
    val site = state.site(siteId)
    if (site == null) { LaunchedEffect(Unit) { onBack() }; return }
    var confirmClear by remember { mutableStateOf(false) }
    CloudScaffold("Cookies & sessions", onBack = onBack, testTag = "cloudBrowser.cookies") {
        CloudSection(header = site.domain,
            footer = "Clearing cookies signs Rem out of this site. Saved logins are separate.") {
            CloudValueRow("Session", if (site.signedIn) "Signed in" else "Signed out")
            CloudRowDivider()
            CloudValueRow("Cookies", site.illustrativeCookies)
        }
        CloudSection {
            CloudCenteredDestructive("Clear site data", onClick = { confirmClear = true },
                testTag = "cloudBrowser.cookies.clearSiteData")
        }
    }
    if (confirmClear) {
        CloudConfirm("Clear data for ${site.domain}?",
            "Rem will be signed out of this site. Saved logins are kept.",
            confirmTitle = "Clear site data", confirmTag = "cloudBrowser.cookies.confirmClear",
            onConfirm = { state.clearSiteData(siteId); confirmClear = false },
            onCancel = { confirmClear = false })
    }
}

// MARK: - Scaffolds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudScaffold(
    title: String, onBack: () -> Unit, testTag: String,
    saveEnabled: Boolean? = null, saveTag: String = "", onSave: () -> Unit = {},
    actionTitle: String? = null, actionTag: String = "", onAction: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = RemColors.current
    Scaffold(containerColor = colors.backgroundPrimary, topBar = {
        CenterAlignedTopAppBar(
            title = { Text(title, style = RemTypography.bodyBold, color = colors.labelPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("cloudBrowser.back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.labelPrimary)
                }
            },
            actions = {
                if (actionTitle != null) {
                    TextButton(onClick = onAction, modifier = Modifier.testTag(actionTag)) {
                        Text(actionTitle, style = RemTypography.body, color = colors.brandBlue)
                    }
                }
                if (saveEnabled != null) {
                    TextButton(onClick = onSave, enabled = saveEnabled, modifier = Modifier.testTag(saveTag)) {
                        Text("Save", style = RemTypography.bodyBold,
                            color = if (saveEnabled) colors.brandBlue else colors.labelTertiary)
                    }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.backgroundPrimary),
        )
    }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = RemSpacing.lg).padding(top = RemSpacing.sm, bottom = RemSpacing.xl)
                .testTag(testTag),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.xl),
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudEditScaffold(
    title: String, onCancel: () -> Unit, cancelTag: String,
    saveEnabled: Boolean, saveTag: String, onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = RemColors.current
    Scaffold(containerColor = colors.backgroundPrimary, topBar = {
        CenterAlignedTopAppBar(
            title = { Text(title, style = RemTypography.bodyBold, color = colors.labelPrimary) },
            navigationIcon = {
                TextButton(onClick = onCancel, modifier = Modifier.testTag(cancelTag)) {
                    Text("Cancel", style = RemTypography.body, color = colors.brandBlue)
                }
            },
            actions = {
                TextButton(onClick = onSave, enabled = saveEnabled, modifier = Modifier.testTag(saveTag)) {
                    Text("Save", style = RemTypography.bodyBold,
                        color = if (saveEnabled) colors.brandBlue else colors.labelTertiary)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.backgroundPrimary),
        )
    }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = RemSpacing.lg).padding(top = RemSpacing.sm, bottom = RemSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.xl),
            content = content,
        )
    }
}

// MARK: - Sections and rows

@Composable
internal fun CloudSection(
    header: String? = null,
    headerAction: String? = null,
    onHeaderAction: () -> Unit = {},
    headerActionTag: String = "",
    footer: String? = null,
    rows: @Composable ColumnScope.() -> Unit,
) {
    val colors = RemColors.current
    if (header != null || headerAction != null) {
        RemSection(header = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(header ?: "", style = RemTypography.subheadline, color = colors.labelSecondary,
                    modifier = Modifier.weight(1f))
                if (headerAction != null) {
                    Text(headerAction, style = RemTypography.body, color = colors.brandBlue,
                        modifier = Modifier.clickable(onClick = onHeaderAction)
                            .padding(RemSpacing.xs).testTag(headerActionTag))
                }
            }
        }, footer = {
            if (footer != null) Text(footer, style = RemTypography.footnote, color = colors.labelSecondary)
        }, rows = rows)
    } else if (footer != null) {
        RemSection(header = {}, footer = {
            Text(footer, style = RemTypography.footnote, color = colors.labelSecondary)
        }, rows = rows)
    } else {
        RemSection(rows = rows)
    }
}

@Composable
internal fun CloudRowDivider() {
    HorizontalDivider(Modifier.padding(start = RemSpacing.lg), thickness = 0.5.dp,
        color = RemColors.current.separator)
}

/** A site row: domain + derived `permission · status`, with a disclosure chevron. */
@Composable
private fun CloudSiteRow(site: CloudSite, onClick: () -> Unit) {
    CloudNavRow(site.domain, site.rowSubtitle, onClick = onClick, testTag = "cloudBrowser.site.${site.domain}")
}

/** A full-row tap target that pushes a nested screen, with a trailing disclosure chevron. */
@Composable
private fun CloudNavRow(title: String, subtitle: String, onClick: () -> Unit, testTag: String) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 60.dp)
            .padding(horizontal = RemSpacing.lg, vertical = RemSpacing.md).testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        Box(Modifier.weight(1f)) { ListRowLabel(title, subtitle) }
        DisclosureChevron()
    }
}

/** A read-only title/value row (e.g. "Session" / "Signed in", "Website" / domain). */
@Composable
internal fun CloudValueRow(title: String, value: String) {
    Box(Modifier.fillMaxWidth().heightIn(min = 60.dp)
        .padding(horizontal = RemSpacing.lg, vertical = RemSpacing.md)) {
        ListRowLabel(title, value)
    }
}

/** A blue text link row (chevron-less), e.g. "See all sites", "Add login". */
@Composable
private fun CloudLinkRow(title: String, onClick: () -> Unit, testTag: String) {
    Box(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 60.dp)
        .padding(horizontal = RemSpacing.lg, vertical = RemSpacing.md).testTag(testTag),
        contentAlignment = Alignment.CenterStart) {
        Text(title, style = RemTypography.body, color = RemColors.current.brandBlue)
    }
}

/** A destructive row with a red title and a secondary subtitle. */
@Composable
private fun CloudDestructiveRow(title: String, subtitle: String, onClick: () -> Unit, testTag: String) {
    val colors = RemColors.current
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 60.dp)
        .padding(horizontal = RemSpacing.lg, vertical = RemSpacing.md).testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = RemTypography.body, color = colors.systemRed)
        Text(subtitle, style = RemTypography.footnote, color = colors.labelSecondary)
    }
}

/** A centered destructive text action (e.g. "Remove login", "Clear site data" on cookies). */
@Composable
private fun CloudCenteredDestructive(title: String, onClick: () -> Unit, testTag: String) {
    Box(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 52.dp)
        .padding(RemSpacing.md).testTag(testTag), contentAlignment = Alignment.Center) {
        Text(title, style = RemTypography.body, color = RemColors.current.systemRed)
    }
}

/** A read row with a trailing pencil edit affordance (the CRUD "update" entry point). */
@Composable
private fun CloudEditRow(title: String, value: String, showPencil: Boolean, onEdit: () -> Unit, testTag: String) {
    val colors = RemColors.current
    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp)
        .padding(horizontal = RemSpacing.lg, vertical = RemSpacing.md).testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm)) {
        Box(Modifier.weight(1f)) { ListRowLabel(title, value) }
        if (showPencil) {
            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp).testTag("$testTag.pencil")) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit $title", tint = colors.labelTertiary,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

// MARK: - Inputs

/** Add-site domain field: subordinate label appears once focused or nonempty; placeholder switches. */
@Composable
private fun CloudUrlField(label: String, example: String, value: String, onValueChange: (String) -> Unit,
                          autofocus: Boolean, testTag: String) {
    CloudFieldShell(label = null, floatingLabel = label, focusedPlaceholder = example,
        placeholder = label, value = value, onValueChange = onValueChange,
        keyboard = KeyboardType.Uri, autofocus = autofocus, testTag = testTag)
}

@Composable
internal fun CloudInputField(placeholder: String, value: String, onValueChange: (String) -> Unit,
                            secure: Boolean = false, autofocus: Boolean = false, labelled: Boolean = false,
                            testTag: String) {
    CloudFieldShell(label = null, placeholder = placeholder, value = value, onValueChange = onValueChange,
        floatingLabel = if (labelled) placeholder else null, demoteFilledValue = labelled,
        secure = secure, autofocus = autofocus, testTag = testTag)
}

/** Inline editable field with a subordinate label (saved-login edit mode). */
@Composable
private fun CloudEditField(label: String, placeholder: String, value: String, onValueChange: (String) -> Unit,
                           secure: Boolean = false, testTag: String) {
    CloudFieldShell(label = label, placeholder = placeholder, value = value, onValueChange = onValueChange,
        secure = secure, autofocus = true, testTag = testTag)
}

@Composable
private fun CloudFieldShell(
    label: String?, placeholder: String, value: String, onValueChange: (String) -> Unit,
    secure: Boolean = false, keyboard: KeyboardType = KeyboardType.Text,
    autofocus: Boolean = false, testTag: String,
    floatingLabel: String? = null, focusedPlaceholder: String = "", demoteFilledValue: Boolean = false,
) {
    val colors = RemColors.current
    var focused by remember { mutableStateOf(false) }
    val showFloatingLabel = floatingLabel != null && (focused || value.isNotEmpty())
    val displayLabel = if (showFloatingLabel) floatingLabel else label
    val filledResting = demoteFilledValue && !focused && value.isNotEmpty()
    val displayPlaceholder = if (showFloatingLabel) focusedPlaceholder else placeholder
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    if (autofocus) LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Column(Modifier.fillMaxWidth().heightIn(min = 60.dp)
        .padding(horizontal = RemSpacing.lg, vertical = RemSpacing.md),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (displayLabel != null) Text(displayLabel,
            style = if (filledResting) RemTypography.body else RemTypography.footnote,
            color = if (filledResting) colors.labelPrimary else colors.labelSecondary)
        BasicTextField(
            value = value, onValueChange = onValueChange, singleLine = true,
            textStyle = if (filledResting) RemTypography.footnote.copy(color = colors.labelSecondary)
                else RemTypography.body.copy(color = colors.labelPrimary),
            cursorBrush = SolidColor(colors.brandBlue),
            visualTransformation = if (secure) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (secure) KeyboardType.Password else keyboard,
                imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }.testTag(testTag),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(displayPlaceholder, style = RemTypography.body, color = colors.labelTertiary)
                inner()
            },
        )
    }
}

/** The trailing value menu ("Ask ⌄"), restricted to the observed Ask / Allow policies. */
@Composable
private fun CloudPermissionRow(title: String, subtitle: String, selected: CloudSitePermission,
                               onSelect: (CloudSitePermission) -> Unit, testTag: String = "") {
    val colors = RemColors.current
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp)
        .padding(horizontal = RemSpacing.lg, vertical = RemSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm)) {
        Box(Modifier.weight(1f)) { ListRowLabel(title, subtitle) }
        Box {
            Row(Modifier.clickable { expanded = true }.padding(RemSpacing.xs)
                .testTag(testTag.ifEmpty { "cloudBrowser.permissionMenu" }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs)) {
                Text(selected.label, style = RemTypography.body, color = colors.labelSecondary)
                Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = colors.labelTertiary,
                    modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                CloudSitePermission.entries.forEach { option ->
                    DropdownMenuItem(text = { Text(option.label) },
                        onClick = { onSelect(option); expanded = false })
                }
            }
        }
    }
}

// MARK: - Confirmation

@Composable
private fun CloudConfirm(title: String, message: String, confirmTitle: String, confirmTag: String,
                         onConfirm: () -> Unit, onCancel: () -> Unit) {
    val colors = RemColors.current
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title, style = RemTypography.bodyBold) },
        text = { Text(message, style = RemTypography.footnote) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(confirmTag)) {
                Text(confirmTitle, color = colors.systemRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel", color = colors.brandBlue) }
        },
    )
}

@Preview(name = "SettingsCloudBrowserScreen", showBackground = true, widthDp = 402, heightDp = 860)
@Composable
private fun SettingsCloudBrowserScreenPreview() {
    val session = remember { CloudBrowserViewModel() }
    RemTheme { SettingsCloudBrowserScreen(onBack = {}, session = session) }
}
