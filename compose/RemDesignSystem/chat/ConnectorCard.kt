package com.rem.designsystem.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.buttons.RemButton
import com.rem.designsystem.buttons.RemButtonVariant
import com.rem.designsystem.rows.ConnectorProvider
import com.rem.designsystem.rows.ConnectorProviderMark
import com.rem.designsystem.rows.ListRowLabel
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * The four states of a contextual connector-authorization card (Figma `State` on `2567:2666`). Twin of the
 * SwiftUI `ConnectorCardState`. [Added] must only be shown after the host verified the authorization.
 */
sealed interface ConnectorCardState {
    data object Authorize : ConnectorCardState
    data object Connecting : ConnectorCardState
    data object Added : ConnectorCardState
    /** Failed connector authorization; a blank/null [message] uses the authored default copy. */
    data class Error(val message: String? = null) : ConnectorCardState
}

/** Display model for [ConnectorCard]; copy and the per-state control are derived here (twin of SwiftUI). */
data class ConnectorCardModel(
    val provider: ConnectorProvider,
    val subtitle: String,
    val state: ConnectorCardState,
    val title: String = provider.title,
) {
    /** The single control below the identity row. */
    sealed interface Control {
        val label: String
        data class Authorize(override val label: String) : Control
        data class Progress(override val label: String) : Control
        data class Receipt(override val label: String) : Control
        data class Retry(override val label: String) : Control
    }

    val control: Control
        get() = when (state) {
            ConnectorCardState.Authorize -> Control.Authorize("Authorize")
            ConnectorCardState.Connecting -> Control.Progress("Adding…")
            ConnectorCardState.Added -> Control.Receipt("Added")
            is ConnectorCardState.Error -> Control.Retry("Retry")
        }

    /** The state-specific error shown above Retry, outside the permanent content; `null` otherwise. */
    val errorMessage: String?
        get() = (state as? ConnectorCardState.Error)?.let { error ->
            error.message?.takeIf { it.isNotBlank() } ?: "$title authorization failed. Retry to connect."
        }
}

/**
 * **ConnectorCard** — Compose sibling of the SwiftUI `ConnectorCard`: the in-transcript card that
 * authorizes one named provider, shows progress, a verified "Added" receipt, or a connector-specific
 * Retry. Anatomy (Figma `2567:2666`): ListRow identity with [ConnectorProviderMark] leading and
 * [ListRowLabel] title + subtitle in the content slot (trailing empty, row not interactive), then a
 * `Rect · Blue` [RemButton] / disabled "Adding…" / success-tinted receipt / error + Retry.
 * Presentation only: no OAuth, credentials or network.
 */
@Composable
fun ConnectorCard(
    model: ConnectorCardModel,
    onAuthorize: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    accessibilityPrefix: String = "connectorCard",
) {
    val colors = RemColors.current
    Column(
        modifier = modifier.chatCardSurface().testTag(accessibilityPrefix),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
                .semantics(mergeDescendants = true) {}.testTag("$accessibilityPrefix.identity"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
        ) {
            ConnectorProviderMark(model.provider)
            Box(Modifier.weight(1f)) { ListRowLabel(model.title, model.subtitle) }
        }
        model.errorMessage?.let {
            Text(it, style = RemTypography.footnote, color = colors.labelSecondary,
                modifier = Modifier.fillMaxWidth().testTag("$accessibilityPrefix.error"))
        }
        when (val control = model.control) {
            is ConnectorCardModel.Control.Authorize -> RemButton(
                control.label, onClick = onAuthorize, variant = RemButtonVariant.RectBlue,
                modifier = Modifier.semantics { contentDescription = "${control.label} ${model.title}" }
                    .testTag("$accessibilityPrefix.authorize"),
            )
            is ConnectorCardModel.Control.Progress -> RemButton(
                control.label, onClick = {}, variant = RemButtonVariant.RectBlue, enabled = false,
                modifier = Modifier.semantics { contentDescription = "Adding ${model.title}" }
                    .testTag("$accessibilityPrefix.progress"),
            )
            is ConnectorCardModel.Control.Receipt -> ChatCardReceipt(
                control.label,
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "${model.title} ${control.label.lowercase()}"
                }.testTag("$accessibilityPrefix.receipt"),
            )
            is ConnectorCardModel.Control.Retry -> RemButton(
                control.label, onClick = onRetry, variant = RemButtonVariant.RectBlue,
                modifier = Modifier.semantics { contentDescription = "${control.label} ${model.title}" }
                    .testTag("$accessibilityPrefix.retry"),
            )
        }
    }
}

/**
 * Non-interactive outcome receipt in the Rect Button geometry with the confirmed success tint (Figma
 * ActionReceipt `2566:2645`, Outcome=Confirmed). Status text, never a button. Internal until the shared
 * ActionReceipt component lands.
 */
@Composable
internal fun ChatCardReceipt(label: String, modifier: Modifier = Modifier) {
    val colors = RemColors.current
    Box(
        modifier = modifier.fillMaxWidth()
            .background(colors.systemGreen.copy(alpha = 0.12f), RoundedCornerShape(RemRadius.medium))
            .padding(RemSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = RemTypography.bodyBold, color = colors.labelPrimary, textAlign = TextAlign.Center)
    }
}

/** Shared chat-card container: secondary background, 12dp inset, large radius, fills the column. */
@Composable
internal fun Modifier.chatCardSurface(): Modifier = this
    .fillMaxWidth()
    .background(RemColors.current.backgroundSecondary, RoundedCornerShape(RemRadius.large))
    .padding(RemSpacing.md)

@Preview(name = "ConnectorCard — states", showBackground = true, widthDp = 402)
@Composable
private fun ConnectorCardPreview() {
    RemTheme {
        Column(
            Modifier.background(RemColors.current.backgroundPrimary).verticalScroll(rememberScrollState()).padding(RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
        ) {
            listOf(ConnectorCardState.Authorize, ConnectorCardState.Connecting, ConnectorCardState.Added, ConnectorCardState.Error())
                .forEach { state ->
                    ConnectorCard(
                        ConnectorCardModel(ConnectorProvider.Gmail, "Search, read, draft, and manage email.", state),
                        onAuthorize = {}, onRetry = {},
                    )
                }
        }
    }
}
