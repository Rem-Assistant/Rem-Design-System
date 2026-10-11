package com.rem.designsystem.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/** One model a host offers in the composer menu. Supplied at runtime; the design system ships no catalog. */
data class ChatModelOption(val id: String, val name: String)

/** A configured provider and its models. Providers with no models are not listed. */
data class ChatModelProvider(val id: String, val name: String, val models: List<ChatModelOption>)

/** Automatic (the gateway's default routing) or one explicit model. */
sealed interface ChatModelSelection {
    data object Automatic : ChatModelSelection
    data class Model(val id: String) : ChatModelSelection
}

/**
 * Trigger label for a selection: [automaticLabel] for Automatic, otherwise the model's name. For an id
 * the providers do not (yet) contain, the host's [fallbackLabel] (typically
 * `ChatComposerState.modelLabel`) when it is non-blank and not the Automatic label, else the raw id —
 * never disguised as Automatic.
 */
fun chatModelTriggerLabel(
    selection: ChatModelSelection,
    providers: List<ChatModelProvider>,
    automaticLabel: String = "Auto",
    fallbackLabel: String? = null,
): String = when (selection) {
    ChatModelSelection.Automatic -> automaticLabel
    is ChatModelSelection.Model ->
        providers.asSequence().flatMap { it.models }.firstOrNull { it.id == selection.id }?.name
            ?: fallbackLabel?.trim()?.takeIf { it.isNotEmpty() && it != automaticLabel }
            ?: selection.id
}

/**
 * The enclosing composer's `ChatComposerState.modelLabel`, provided by `RemComposerBar` around its
 * `modelMenu` slot: the default unresolved-selection label for a [ChatModelMenu] without `fallbackLabel`.
 */
internal val LocalComposerModelLabel = staticCompositionLocalOf<String?> { null }

/**
 * **ChatModelMenu** — Compose sibling of the SwiftUI `ChatModelMenu`: the composer's secondary-pill
 * model trigger ("Auto" while Automatic is selected; the host's fallback label while the selection is
 * not among [providers]) and its menu — **Automatic**, one entry per
 * configured provider opening that provider's models (checkmark on the selection), then **Manage
 * Models** when [onManageModels] is supplied. Everything listed comes from [providers]. Disabled at 45%
 * opacity when [enabled] is false (the composer disables it while sending).
 *
 * [fallbackLabel] is the host's label for an explicit selection the providers do not (yet) list — e.g. a
 * bring-your-own-key model whose provider evidence is still pending. When null, a menu placed in
 * `RemComposerBar` uses the composer state's `modelLabel`. See [chatModelTriggerLabel].
 *
 * Android has no native nested menu, so a provider entry swaps the menu to that provider's models with
 * a back row — the same information architecture as the iOS submenu.
 *
 * Figma: **Rem/Chat/Model menu** (`2656:128164`), provider submenu (`2656:128245`).
 */
@Composable
fun ChatModelMenu(
    providers: List<ChatModelProvider>,
    selection: ChatModelSelection,
    onSelect: (ChatModelSelection) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    automaticLabel: String = "Auto",
    accessibilityPrefix: String = "composer",
    onManageModels: (() -> Unit)? = null,
    fallbackLabel: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var openProvider by remember { mutableStateOf<ChatModelProvider?>(null) }
    val label = chatModelTriggerLabel(selection, providers, automaticLabel, fallbackLabel ?: LocalComposerModelLabel.current)
    val listed = providers.filter { it.models.isNotEmpty() }
    val close = { expanded = false; openProvider = null }

    Box(modifier = modifier) {
        ChatModelTriggerPill(
            label = label,
            enabled = enabled,
            modifier = Modifier
                .clickable(enabled = enabled, role = Role.Button) { expanded = true }
                .testTag("$accessibilityPrefix.modelMenu"),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = close) {
            val current = openProvider
            if (current == null) {
                DropdownMenuItem(
                    text = { Text("Automatic") },
                    trailingIcon = { if (selection == ChatModelSelection.Automatic) Icon(Icons.Filled.Check, contentDescription = "Selected") },
                    onClick = { onSelect(ChatModelSelection.Automatic); close() },
                    modifier = Modifier.testTag("$accessibilityPrefix.modelAutomatic"),
                )
                listed.forEach { provider ->
                    DropdownMenuItem(
                        text = { Text(provider.name) },
                        trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                        onClick = { openProvider = provider },
                        modifier = Modifier.testTag("$accessibilityPrefix.modelProvider.${provider.id}"),
                    )
                }
                if (onManageModels != null) {
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Manage Models") },
                        leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                        onClick = { close(); onManageModels() },
                        modifier = Modifier.testTag("$accessibilityPrefix.manageModels"),
                    )
                }
            } else {
                DropdownMenuItem(
                    text = { Text(current.name) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back") },
                    onClick = { openProvider = null },
                    modifier = Modifier.testTag("$accessibilityPrefix.modelBack"),
                )
                HorizontalDivider()
                current.models.forEach { model ->
                    val selected = selection == ChatModelSelection.Model(model.id)
                    DropdownMenuItem(
                        text = { Text(model.name) },
                        trailingIcon = { if (selected) Icon(Icons.Filled.Check, contentDescription = "Selected") },
                        onClick = { onSelect(ChatModelSelection.Model(model.id)); close() },
                        modifier = Modifier.testTag("$accessibilityPrefix.model.${model.id}"),
                    )
                }
            }
        }
    }
}

/** The secondary-pill trigger, shared with the display-only `RemComposerBar` model pill. */
@Composable
internal fun ChatModelTriggerPill(label: String, enabled: Boolean, modifier: Modifier = Modifier) {
    val colors = RemColors.current
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .background(colors.fillTertiary, CircleShape)
            .padding(horizontal = RemSpacing.sm, vertical = RemSpacing.xs)
            .clearAndSetSemantics {
                contentDescription = "Model, $label"
                text = AnnotatedString(label)
                role = Role.Button
                if (!enabled) disabled()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs),
    ) {
        Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = colors.labelPrimary, modifier = Modifier.size(12.dp))
        Text(text = label, style = RemTypography.caption1, color = colors.labelPrimary, maxLines = 1)
    }
}

@Preview(name = "ChatModelMenu", showBackground = true)
@Composable
private fun ChatModelMenuPreview() {
    val providers = listOf(
        ChatModelProvider("provider-a", "Provider A", listOf(ChatModelOption("a-fast", "Fast model"), ChatModelOption("a-deep", "Deep model"))),
    )
    RemTheme {
        Column(
            modifier = Modifier.background(RemColors.current.backgroundSecondary).padding(RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.lg),
        ) {
            ChatModelMenu(providers, ChatModelSelection.Automatic, onSelect = {}, onManageModels = {})
            ChatModelMenu(providers, ChatModelSelection.Model("a-deep"), onSelect = {})
            ChatModelMenu(providers, ChatModelSelection.Automatic, onSelect = {}, enabled = false)
        }
    }
}
