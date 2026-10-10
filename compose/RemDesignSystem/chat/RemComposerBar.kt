package com.rem.designsystem.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.screens.ChatComposerAction
import com.rem.designsystem.screens.ChatComposerState
import com.rem.designsystem.screens.ComposerAvailability
import com.rem.designsystem.screens.ComposerPhase
import com.rem.designsystem.screens.ComposerSendDisplay
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * **RemComposerBar** — Compose sibling of the SwiftUI [RemComposerBar]. The chat / comment composer: a
 * text field in a rounded, borderless pill with a bottom control row `[+] · model · field · [Speak] ·
 * send`, and an optional attachments strip. No border — the material pill is the whole shape. Authority:
 * Figma **RemComposerBar** (`53:2`) + the 3-state doc (`527:2`) and `RemComposerBar.swift`.
 *
 * (The shipping SwiftUI pill uses `.ultraThinMaterial`; Compose has no material blur, so both DS
 * renders use `backgroundSecondary` — the flat grey the Figma pill shows.)
 *
 * Chat slice (Figma **Composer** `2071:11555`): the model control is the secondary-pill **Auto**
 * trigger. Pass [modelMenu] (typically a [ChatModelMenu]) to make it open the runtime-supplied model
 * menu; its `enabled` argument is false while sending (45%), when Speak is also hidden. [attachments]
 * are removable chips owned by the host — a Cloud browser chip is a capability for the next message.
 */
enum class ComposerSendState { Idle, Active, Sending }

/** One item attached to the next message, rendered as a removable chip in [RemComposerBar]. */
data class ComposerAttachment(val id: String, val title: String, val kind: Kind) {
    enum class Kind {
        /** A capability for the next turn (e.g. Cloud browser) — not content, not an immediate launch. */
        Capability,
        Image,
        File,
    }

    companion object {
        val CloudBrowser = ComposerAttachment("cloud-browser", "Cloud browser", Kind.Capability)
    }
}

@Composable
fun RemComposerBar(
    modifier: Modifier = Modifier,
    text: String = "",
    placeholder: String = "Ask anything",
    model: String = "Auto",
    state: ComposerSendState = ComposerSendState.Idle,
    showAttachments: Boolean = false,
    showModel: Boolean = true,
    showSpeak: Boolean = true,
    onTextChange: ((String) -> Unit)? = null,
    onSend: (() -> Unit)? = null,
    onAdd: (() -> Unit)? = null,
    accessibilityPrefix: String = "composer",
    attachments: List<ComposerAttachment> = emptyList(),
    onRemoveAttachment: ((ComposerAttachment) -> Unit)? = null,
    modelMenu: (@Composable (enabled: Boolean) -> Unit)? = null,
    /** Called by the red Stop while sending — never [onSend]. Stop is disabled when null. */
    onCancel: (() -> Unit)? = null,
    /** Makes Speak a button. */
    onSpeak: (() -> Unit)? = null,
) {
    // `showAttachments` is the legacy display flag: it shows the canonical Cloud browser chip.
    val chips = if (attachments.isEmpty() && showAttachments) listOf(ComposerAttachment.CloudBrowser) else attachments
    val rules = ChatComposerState(
        draft = text, placeholder = placeholder, modelLabel = model, attachments = chips,
        phase = if (state == ComposerSendState.Sending) ComposerPhase.Sending else ComposerPhase.Idle,
        showsModel = showModel, voiceAvailable = showSpeak,
    )
    // Display-only renders keep their explicit state (fixtures pass Active with text); interactive
    // renders follow the single send rule in [ChatComposerState] (text or a content attachment).
    val display = if (onTextChange == null) when (state) {
        ComposerSendState.Idle -> ComposerSendDisplay.Unavailable
        ComposerSendState.Active -> ComposerSendDisplay.Send
        ComposerSendState.Sending -> ComposerSendDisplay.Stop
    } else rules.sendDisplay
    ComposerPill(
        modifier = modifier,
        chips = chips,
        text = text,
        placeholder = placeholder,
        onTextChange = onTextChange,
        inputEnabled = true,
        disabledReason = null,
        modelLabel = model,
        showsModel = showModel,
        modelEnabled = display != ComposerSendDisplay.Stop,
        modelMenu = modelMenu,
        showsSpeak = showSpeak && display != ComposerSendDisplay.Stop,
        onSpeak = onSpeak,
        display = display,
        primaryEnabled = when (display) {
            ComposerSendDisplay.Unavailable -> false
            ComposerSendDisplay.Send -> onSend != null
            ComposerSendDisplay.Stop -> onCancel != null
        },
        onPrimary = if (onSend == null && onCancel == null) null else ({ if (display == ComposerSendDisplay.Stop) onCancel?.invoke() else onSend?.invoke() }),
        onAdd = onAdd,
        onRemoveAttachment = onRemoveAttachment,
        accessibilityPrefix = accessibilityPrefix,
        focused = null,
        onFocusChanged = null,
    )
}

/**
 * The host-driven composer: renders [state] exactly and reports every interaction as a typed
 * [ChatComposerAction]. Availability (externally disabled), phase (sending / streaming), voice and focus
 * are host decisions; the rules that turn them into the control row live in [ChatComposerState]. Stop
 * emits [ChatComposerAction.Cancel], never Send. The Figma Sending (progress) vs Streaming (Stop) visual
 * split is not drawn yet: both show the red Stop, pending review.
 */
@Composable
fun RemComposerBar(
    state: ChatComposerState,
    onAction: (ChatComposerAction) -> Unit,
    modifier: Modifier = Modifier,
    accessibilityPrefix: String = "composer",
    modelMenu: (@Composable (enabled: Boolean) -> Unit)? = null,
) {
    ComposerPill(
        modifier = modifier,
        chips = state.attachments,
        text = state.draft,
        placeholder = state.placeholder,
        onTextChange = { onAction(ChatComposerAction.DraftChanged(it)) },
        inputEnabled = state.availability.isEnabled,
        disabledReason = (state.availability as? ComposerAvailability.Disabled)?.reason,
        modelLabel = state.modelLabel,
        showsModel = state.showsModel,
        modelEnabled = state.modelEnabled,
        modelMenu = modelMenu,
        showsSpeak = state.showsSpeak,
        onSpeak = { onAction(ChatComposerAction.Speak) },
        display = state.sendDisplay,
        primaryEnabled = state.primaryAction != null,
        onPrimary = { state.primaryAction?.let(onAction) },
        onAdd = { onAction(ChatComposerAction.Add) },
        onRemoveAttachment = { onAction(ChatComposerAction.RemoveAttachment(it.id)) },
        accessibilityPrefix = accessibilityPrefix,
        focused = state.isFocused,
        onFocusChanged = { if (it != state.isFocused) onAction(ChatComposerAction.FocusChanged(it)) },
    )
}

/** The one canonical pill both overloads render. */
@Composable
private fun ComposerPill(
    modifier: Modifier,
    chips: List<ComposerAttachment>,
    text: String,
    placeholder: String,
    onTextChange: ((String) -> Unit)?,
    inputEnabled: Boolean,
    disabledReason: String?,
    modelLabel: String,
    showsModel: Boolean,
    modelEnabled: Boolean,
    modelMenu: (@Composable (enabled: Boolean) -> Unit)?,
    showsSpeak: Boolean,
    onSpeak: (() -> Unit)?,
    display: ComposerSendDisplay,
    primaryEnabled: Boolean,
    onPrimary: (() -> Unit)?,
    onAdd: (() -> Unit)?,
    onRemoveAttachment: ((ComposerAttachment) -> Unit)?,
    accessibilityPrefix: String,
    focused: Boolean?,
    onFocusChanged: ((Boolean) -> Unit)?,
) {
    val colors = RemColors.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var fieldHasFocus by remember { mutableStateOf(false) }
    if (focused != null) {
        // Host-owned focus: follow the requested value; report changes through onFocusChanged.
        LaunchedEffect(focused) {
            if (focused && !fieldHasFocus) focusRequester.requestFocus()
            if (!focused && fieldHasFocus) focusManager.clearFocus()
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.backgroundSecondary, RoundedCornerShape(30.dp))
            .padding(RemSpacing.md),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        if (chips.isNotEmpty()) {
            AttachmentsStrip(chips, accessibilityPrefix, onRemoveAttachment)
        }

        if (onTextChange != null) {
            Box {
                if (text.isEmpty()) Text(placeholder, style = RemTypography.chatMessage, color = colors.labelTertiary)
                BasicTextField(
                    value = text, onValueChange = onTextChange,
                    enabled = inputEnabled,
                    textStyle = RemTypography.chatMessage.copy(color = colors.labelPrimary),
                    cursorBrush = SolidColor(colors.brandBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            // Report real changes only: the attach callback (false while unfocused) must not
                            // override a host that starts focused.
                            if (it.isFocused != fieldHasFocus) {
                                fieldHasFocus = it.isFocused
                                onFocusChanged?.invoke(it.isFocused)
                            }
                        }
                        .then(if (disabledReason != null) Modifier.semantics { stateDescription = disabledReason } else Modifier)
                        .testTag("$accessibilityPrefix.composerField"),
                )
            }
        } else {
            Text(
                text = text.ifEmpty { placeholder },
                style = RemTypography.chatMessage,
                color = if (text.isEmpty()) colors.labelTertiary else colors.labelPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
        ) {
            if (onAdd != null) {
                IconButton(onClick = onAdd, enabled = inputEnabled, modifier = Modifier.testTag("$accessibilityPrefix.composerAdd")) {
                    Icon(Icons.Filled.Add, contentDescription = "Add", tint = colors.labelSecondary, modifier = Modifier.size(20.dp))
                }
            } else {
                Icon(Icons.Filled.Add, contentDescription = "Add", tint = colors.labelSecondary, modifier = Modifier.size(20.dp))
            }
            if (showsModel) {
                if (modelMenu != null) modelMenu(modelEnabled) else ChatModelTriggerPill(label = modelLabel, enabled = modelEnabled)
            }
            Box(modifier = Modifier.weight(1f))
            if (showsSpeak) {
                if (onSpeak != null) {
                    Box(
                        modifier = Modifier
                            .clickable(role = Role.Button, onClick = onSpeak)
                            .testTag("$accessibilityPrefix.composerSpeak"),
                    ) { SpeakPill() }
                } else SpeakPill()
            }
            if (onPrimary != null) {
                IconButton(onClick = onPrimary, enabled = primaryEnabled,
                    modifier = Modifier.testTag("$accessibilityPrefix.composerSend")) {
                    SendButton(display)
                }
            } else { SendButton(display) }
        }
    }
}

@Composable
private fun SpeakPill() {
    val colors = RemColors.current
    Row(
        modifier = Modifier
            .background(colors.brandBlue, CircleShape)
            .padding(horizontal = RemSpacing.md, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs),
    ) {
        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = colors.labelOnColor, modifier = Modifier.size(15.dp))
        Text(text = "Speak", style = RemTypography.subheadline.copy(fontWeight = FontWeight.Bold), color = colors.labelOnColor)
    }
}

@Composable
private fun SendButton(display: ComposerSendDisplay) {
    val colors = RemColors.current
    val fill = when (display) {
        ComposerSendDisplay.Unavailable -> colors.fillTertiary
        ComposerSendDisplay.Send -> colors.brandBlue
        ComposerSendDisplay.Stop -> colors.systemRed
    }
    val fg = if (display == ComposerSendDisplay.Unavailable) colors.labelSecondary else colors.labelOnColor
    val stop = display == ComposerSendDisplay.Stop
    Box(
        modifier = Modifier.size(32.dp).background(fill, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (stop) Icons.Filled.Stop else Icons.Filled.ArrowUpward,
            contentDescription = if (stop) "Stop" else "Send",
            tint = fg,
            modifier = Modifier.size(if (stop) 15.dp else 18.dp),
        )
    }
}

@Composable
private fun AttachmentsStrip(
    attachments: List<ComposerAttachment>,
    prefix: String,
    onRemove: ((ComposerAttachment) -> Unit)?,
) {
    val colors = RemColors.current
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        attachments.forEach { attachment ->
            Row(
                modifier = Modifier
                    .background(colors.backgroundPrimary, RoundedCornerShape(RemRadius.small))
                    .padding(horizontal = RemSpacing.sm, vertical = RemSpacing.xs)
                    .testTag("$prefix.attachment.${attachment.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs),
            ) {
                Text(text = attachment.title, style = RemTypography.footnote, color = colors.labelPrimary, maxLines = 1)
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .then(
                            if (onRemove != null) Modifier
                                .clickable(role = Role.Button) { onRemove(attachment) }
                                .testTag("$prefix.removeAttachment.${attachment.id}")
                            else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = if (onRemove != null) "Remove ${attachment.title}" else null,
                        tint = colors.brandBlue,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

@Preview(name = "RemComposerBar", showBackground = true, widthDp = 420)
@Composable
private fun RemComposerBarPreview() {
    RemTheme {
        Column(
            modifier = Modifier.background(RemColors.current.backgroundPrimary).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            RemComposerBar()
            RemComposerBar(text = "Remind me to send the investor update tomorrow", state = ComposerSendState.Active, showAttachments = true)
            RemComposerBar(text = "Plan the rest of my day", state = ComposerSendState.Sending)
        }
    }
}
