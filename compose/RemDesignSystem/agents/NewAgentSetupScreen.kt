package com.rem.designsystem.agents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography

/**
 * **NewAgentSetupScreen** — "New agent" setup, the Compose twin of `Agents/NewAgentSetupScreen.swift`.
 *
 * Figma: **E · New agent — set up** (`2714:3538`). Top app bar (Cancel · "New agent"), the agent icon
 * (tap to change) with colour swatches, **Name** (first, focused — naming the lane comes first), **What
 * should this agent handle?**, optional **Connected services**, and a bottom-pinned **Create agent**
 * that stays disabled until the lane has a name.
 *
 * State is hoisted: the host owns [draft] and applies [onDraftChange]; every other intent arrives as a
 * [NewAgentSetupAction]. Display + callbacks only — no networking or persistence.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAgentSetupScreen(
    draft: NewAgentDraft,
    onDraftChange: (NewAgentDraft) -> Unit,
    onAction: (NewAgentSetupAction) -> Unit,
    modifier: Modifier = Modifier,
    focusesNameOnAppear: Boolean = true,
) {
    val colors = RemColors.current
    val nameFocus = remember { FocusRequester() }
    if (focusesNameOnAppear) {
        LaunchedEffect(Unit) { runCatching { nameFocus.requestFocus() } }
    }
    Scaffold(
        modifier = modifier,
        containerColor = colors.backgroundPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        NewAgentSetupCopy.Title,
                        style = RemTypography.bodyBold,
                        color = colors.labelPrimary,
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = { onAction(NewAgentSetupAction.Cancel) },
                        modifier = Modifier.testTag("newAgent.cancel"),
                    ) {
                        Text(NewAgentSetupCopy.Cancel, style = RemTypography.body, color = colors.labelPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.backgroundPrimary),
            )
        },
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(colors.backgroundPrimary)
                    .padding(start = RemSpacing.lg, end = RemSpacing.lg, top = RemSpacing.sm, bottom = RemSpacing.lg),
            ) {
                AgentPrimaryButton(
                    label = NewAgentSetupCopy.Create,
                    enabled = draft.canCreate,
                    modifier = Modifier.testTag("newAgent.create"),
                    onClick = { draft.createAction?.let(onAction) },
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = RemSpacing.lg, end = RemSpacing.lg, top = RemSpacing.sm, bottom = RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.lg),
        ) {
            IconPicker(draft, onDraftChange, onAction)
            AgentFormSection(header = NewAgentSetupCopy.NameHeader, footer = NewAgentSetupCopy.NameFooter) {
                SetupField(
                    value = draft.name,
                    placeholder = NewAgentSetupCopy.NamePlaceholder,
                    singleLine = true,
                    modifier = Modifier.focusRequester(nameFocus).testTag("newAgent.name"),
                    onValueChange = { onDraftChange(draft.copy(name = it)) },
                )
            }
            AgentFormSection(header = NewAgentSetupCopy.HandlesHeader, footer = NewAgentSetupCopy.HandlesFooter) {
                SetupField(
                    value = draft.handles,
                    placeholder = NewAgentSetupCopy.HandlesPlaceholder,
                    singleLine = false,
                    modifier = Modifier.testTag("newAgent.handles"),
                    onValueChange = { onDraftChange(draft.copy(handles = it)) },
                )
            }
            AgentFormSection(header = AgentServicesCopy.Header, footer = NewAgentSetupCopy.ServicesFooter) {
                draft.services.forEach { service ->
                    AgentServiceRow(
                        title = service.name,
                        subtitle = service.status,
                        glyph = service.glyph,
                        modifier = Modifier.testTag("newAgent.service.${service.id}"),
                        onClick = { onAction(NewAgentSetupAction.OpenService(service.id)) },
                    )
                    AgentRowDivider()
                }
                AgentServiceRow(
                    title = AgentServicesCopy.AddService,
                    subtitle = null,
                    glyph = AgentServiceGlyph.Link,
                    modifier = Modifier.testTag("newAgent.addService"),
                    onClick = { onAction(NewAgentSetupAction.AddService) },
                )
            }
        }
    }
}

@Composable
private fun IconPicker(
    draft: NewAgentDraft,
    onDraftChange: (NewAgentDraft) -> Unit,
    onAction: (NewAgentSetupAction) -> Unit,
) {
    val colors = RemColors.current
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        Box(
            Modifier
                .clip(CircleShape)
                .clickable { onAction(NewAgentSetupAction.ChangeIcon) }
                .semantics {
                    role = Role.Button
                    contentDescription = NewAgentSetupCopy.ChangeIcon
                }
                .testTag("newAgent.icon"),
        ) {
            AgentAvatar(AgentAvatarIdentity.Side(draft.look), size = 80.dp)
        }
        Text(NewAgentSetupCopy.ChangeIcon, style = RemTypography.footnote, color = colors.labelSecondary)
        Row(
            horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
            modifier = Modifier.testTag("newAgent.colors"),
        ) {
            AgentColor.entries.forEach { color ->
                val selected = draft.look.color == color
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .then(if (selected) Modifier.border(2.dp, colors.labelPrimary, CircleShape) else Modifier)
                        .clickable { onDraftChange(draft.copy(look = draft.look.copy(color = color))) }
                        .semantics {
                            role = Role.RadioButton
                            this.selected = selected
                            contentDescription = color.accessibilityName
                        }
                        .testTag("newAgent.color.${color.id}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(color.swatch()))
                }
            }
        }
    }
}

@Composable
private fun SetupField(
    value: String,
    placeholder: String,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    val colors = RemColors.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        maxLines = if (singleLine) 1 else 6,
        textStyle = RemTypography.body.copy(color = colors.labelPrimary),
        cursorBrush = SolidColor(colors.brandBlue),
        keyboardOptions = KeyboardOptions(
            capitalization = if (singleLine) KeyboardCapitalization.Words else KeyboardCapitalization.Sentences,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (singleLine) 52.dp else 88.dp)
            .padding(horizontal = RemSpacing.lg, vertical = if (singleLine) 15.dp else RemSpacing.md),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = RemTypography.body, color = colors.labelTertiary)
                }
                inner()
            }
        },
    )
}
