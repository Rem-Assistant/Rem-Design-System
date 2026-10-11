package com.rem.designsystem.agents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.navigation.SymbolGlyph
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography

/**
 * **NewAgentSetupScreen** — the "New agent" sheet, the Compose twin of `Agents/NewAgentSetupScreen.swift`.
 *
 * Name + colour only (Samuel, 2026-10-11). Top to bottom: a header with a top-leading close (X) circle
 * button and the centred "New agent" title; a large [AgentAvatar] preview tinted live with the selected
 * colour; the name in a centred, filled pill field (first, focused); the [AgentColor] palette as a
 * wrapping grid of swatches (selected = ring); and a bottom-pinned **Create agent** that stays disabled
 * until the trimmed name is non-blank.
 *
 * The screen draws its own header, so the host presents it directly (e.g. in a full-height sheet).
 * State is hoisted: the host owns [draft] and applies [onDraftChange]; every other intent arrives as a
 * [NewAgentSetupAction]. Display + callbacks only — no networking or persistence.
 */
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
    Column(
        modifier
            .fillMaxSize()
            .background(colors.backgroundPrimary)
            .imePadding(),
    ) {
        Header(onAction)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = RemSpacing.lg, end = RemSpacing.lg, top = RemSpacing.lg, bottom = RemSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RemSpacing.xl),
        ) {
            AgentAvatar(
                AgentAvatarIdentity.Side(draft.look),
                size = NewAgentSetupLayout.AvatarSize,
                modifier = Modifier.testTag("newAgent.avatar"),
            )
            NameField(
                value = draft.name,
                modifier = Modifier.focusRequester(nameFocus).testTag("newAgent.name"),
                onValueChange = { onDraftChange(draft.copy(name = it)) },
            )
            ColorGrid(selected = draft.color, onSelect = { onDraftChange(draft.copy(color = it)) })
        }
        Box(
            Modifier
                .fillMaxWidth()
                .background(colors.backgroundPrimary)
                .padding(start = RemSpacing.lg, end = RemSpacing.lg, top = RemSpacing.sm, bottom = RemSpacing.lg),
        ) {
            AgentPrimaryButton(
                label = NewAgentSetupCopy.Create,
                enabled = draft.canCreate,
                modifier = Modifier
                    .testTag("newAgent.create")
                    .semantics { if (!draft.canCreate) stateDescription = NewAgentSetupCopy.CreateHint },
                onClick = { draft.createAction?.let(onAction) },
            )
        }
    }
}

/** Close (X) circle leading, centred title, and a matching spacer so the title stays centred. */
@Composable
private fun Header(onAction: (NewAgentSetupAction) -> Unit) {
    val colors = RemColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = RemSpacing.lg, end = RemSpacing.lg, top = RemSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.backgroundSecondary)
                .clickable { onAction(NewAgentSetupAction.Cancel) }
                .semantics {
                    role = Role.Button
                    contentDescription = NewAgentSetupCopy.Close
                }
                .testTag("newAgent.cancel"),
            contentAlignment = Alignment.Center,
        ) {
            SymbolGlyph(RemMaterialSymbols.Close, 22, colors.labelPrimary)
        }
        Text(
            NewAgentSetupCopy.Title,
            style = RemTypography.bodyBold,
            color = colors.labelPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Spacer(Modifier.size(44.dp))
    }
}

/** The name in a centred, filled pill (`fillTertiary`), large semibold text; grows with font scale. */
@Composable
private fun NameField(value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    val colors = RemColors.current
    val style = RemTypography.title3.copy(fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = style.copy(color = colors.labelPrimary),
        cursorBrush = SolidColor(colors.brandBlue),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = NewAgentSetupLayout.NameFieldMinHeight)
            .clip(CircleShape)
            .background(colors.fillTertiary)
            .semantics { contentDescription = NewAgentSetupCopy.NameLabel },
        decorationBox = { inner ->
            Box(
                Modifier.padding(horizontal = RemSpacing.xl, vertical = RemSpacing.md),
                contentAlignment = Alignment.Center,
            ) {
                if (value.isEmpty()) {
                    Text(
                        NewAgentSetupCopy.NamePlaceholder,
                        style = style,
                        color = colors.labelTertiary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                inner()
            }
        },
    )
}

/** The palette in rows of [NewAgentSetupLayout.SwatchesPerRow]; selected swatch = `labelPrimary` ring. */
@Composable
private fun ColorGrid(selected: AgentColor, onSelect: (AgentColor) -> Unit) {
    val colors = RemColors.current
    Column(
        Modifier.selectableGroup().testTag("newAgent.colors"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        NewAgentSetupLayout.swatchRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(RemSpacing.md)) {
                row.forEach { color ->
                    val isSelected = color == selected
                    Box(
                        Modifier
                            .size(NewAgentSetupLayout.SwatchTarget)
                            .clip(CircleShape)
                            .then(if (isSelected) Modifier.border(2.5.dp, colors.labelPrimary, CircleShape) else Modifier)
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(color) })
                            .semantics { contentDescription = color.accessibilityName }
                            .testTag("newAgent.color.${color.id}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(NewAgentSetupLayout.SwatchDisc).clip(CircleShape).background(color.swatch()))
                    }
                }
            }
        }
    }
}
