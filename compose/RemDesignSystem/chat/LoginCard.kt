package com.rem.designsystem.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rem.designsystem.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.buttons.RemButton
import com.rem.designsystem.buttons.RemButtonVariant
import com.rem.designsystem.rows.ListRowLabel
import com.rem.designsystem.screens.CloudInputField
import com.rem.designsystem.screens.CloudRowDivider
import com.rem.designsystem.screens.CloudSection
import com.rem.designsystem.screens.CloudValueRow
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/** The two states of the contextual login card (Figma `State` on `428:37`). Twin of SwiftUI `LoginCardState`. */
enum class LoginCardState {
    /** No login for this site yet: the CTA opens the native Add login form. */
    Entry,
    /** A host-reported login exists: the CTA opens its details. Not a sign-in-success claim. */
    Saved,
}

/** Display model for [LoginCard]; copy and the CTA's Rem Button variant derive from the state. */
data class LoginCardModel(val title: String, val site: String, val state: LoginCardState) {
    val detail: String
        get() = when (state) {
            LoginCardState.Entry -> "Add the login this browser task needs."
            LoginCardState.Saved -> "Login saved for this site."
        }
    val buttonLabel: String
        get() = when (state) {
            LoginCardState.Entry -> "Add login"
            LoginCardState.Saved -> "Saved"
        }
    /** Figma Button `377:8` Style: `Rect · Blue` for Add login, `Rect · Secondary` for Saved. */
    val buttonVariant: RemButtonVariant
        get() = when (state) {
            LoginCardState.Entry -> RemButtonVariant.RectBlue
            LoginCardState.Saved -> RemButtonVariant.RectSecondary
        }
}

/**
 * **LoginCard** — Compose sibling of the SwiftUI `LoginCard`: links a bounded, site-specific browser-task
 * login to the native [LoginForm], or to the saved login's details. Never stores, reads or transmits a
 * credential. Anatomy (Figma `428:37`): ListRow identity with the site mark leading and [ListRowLabel]
 * title + site in the content slot (row not interactive), the state copy, then one [RemButton] whose
 * Trailing slot carries the optional chevron. [leading] defaults to the generic [LoginSiteMark]; pass
 * `LoginSiteMark(LoginSiteBrand.GitHub)` for the library's GitHub logo (`1328:433`).
 */
@Composable
fun LoginCard(
    model: LoginCardModel,
    onAddLogin: () -> Unit,
    onOpenSaved: () -> Unit,
    modifier: Modifier = Modifier,
    showsChevron: Boolean = true,
    accessibilityPrefix: String = "loginCard",
    leading: @Composable () -> Unit = { LoginSiteMark() },
) {
    val colors = RemColors.current
    val entry = model.state == LoginCardState.Entry
    val chevron: (@Composable () -> Unit)? = if (showsChevron) {
        { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp)) }
    } else null
    Column(
        modifier = modifier.chatCardSurface().testTag(accessibilityPrefix),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = RemSpacing.md)
                .semantics(mergeDescendants = true) {}.testTag("$accessibilityPrefix.identity"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
        ) {
            leading()
            Box(Modifier.weight(1f)) { ListRowLabel(model.title, model.site) }
        }
        Text(model.detail, style = RemTypography.footnote, color = colors.labelSecondary,
            modifier = Modifier.fillMaxWidth().testTag("$accessibilityPrefix.detail"))
        RemButton(
            model.buttonLabel,
            onClick = if (entry) onAddLogin else onOpenSaved,
            variant = model.buttonVariant,
            modifier = Modifier
                .semantics { contentDescription = if (entry) "Add login for ${model.site}" else "Saved login for ${model.site}" }
                .testTag("$accessibilityPrefix.${if (entry) "addLogin" else "saved"}"),
            trailing = chevron,
        )
    }
}

/** A site whose brand mark is in the library (Figma `Logo/*`). */
enum class LoginSiteBrand {
    /** Figma `Logo/GitHub` `1328:433`; the black source mark is tinted `labelPrimary` for dark mode. */
    GitHub,
}

/**
 * Site icon for the login card's leading slot, in the 26×29 provider-mark slot. A site without a library
 * brand asset gets the generic globe, never an invented mark.
 */
@Composable
fun LoginSiteMark(brand: LoginSiteBrand? = null, modifier: Modifier = Modifier) {
    Box(modifier.size(width = 26.dp, height = 29.dp), contentAlignment = Alignment.Center) {
        when (brand) {
            LoginSiteBrand.GitHub -> Icon(painterResource(R.drawable.login_site_github), contentDescription = null,
                tint = RemColors.current.labelPrimary, modifier = Modifier.size(26.dp))
            null -> Icon(Icons.Outlined.Public, contentDescription = null, tint = RemColors.current.labelSecondary,
                modifier = Modifier.size(22.dp))
        }
    }
}

/** Save is available only for a non-blank username and a non-empty password (same rule as Cloud browser). */
fun loginFormCanSave(username: String, password: String): Boolean =
    username.trim().isNotEmpty() && password.isNotEmpty()

/**
 * **LoginForm** — the native Add login form content the card's CTA opens (Figma `1956:8162`: Website,
 * Login details with Username or email + masked Password, scope footer). Fields are bound to host state
 * only: nothing is stored, validated against a service or transmitted. The host supplies the scaffold,
 * title and a Save gated by [loginFormCanSave], and clears the values on dismiss. Shared with the
 * Cloud browser Add login screen.
 */
@Composable
fun LoginForm(
    site: String,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "loginForm",
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(RemSpacing.xl)) {
        CloudSection(header = "Website") {
            CloudValueRow(site, "Login will be available only for this site.")
        }
        CloudSection(header = "Login details", footer = "Rem uses this login only when you authorize access to $site.") {
            CloudInputField("Username or email", username, onUsernameChange, labelled = true,
                testTag = "$testTagPrefix.username")
            CloudRowDivider()
            CloudInputField("Password", password, onPasswordChange, secure = true, labelled = true,
                testTag = "$testTagPrefix.password")
        }
    }
}

@Preview(name = "LoginCard — states", showBackground = true, widthDp = 402)
@Composable
private fun LoginCardPreview() {
    RemTheme {
        Column(
            Modifier.background(RemColors.current.backgroundPrimary).padding(RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
        ) {
            LoginCardState.entries.forEach { state ->
                LoginCard(LoginCardModel("GitHub login details", "github.com", state), onAddLogin = {}, onOpenSaved = {})
            }
        }
    }
}
