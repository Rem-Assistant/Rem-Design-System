package com.rem.designsystem.agents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography

/**
 * **AgentProfileHeader** — the identity block at the top of an agent's profile (Compose twin of
 * `Agents/AgentProfileAbout.swift`): avatar, name, green status dot + "Side agent · <lane>". Figma
 * `2718:3060` (Health) and the same block in `2718:3396` (Rem).
 */
@Composable
fun AgentProfileHeader(display: AgentProfileDisplay, modifier: Modifier = Modifier) {
    val colors = RemColors.current
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = RemSpacing.sm)
            .semantics(mergeDescendants = true) { contentDescription = "${display.name}, ${display.statusLine}" }
            .testTag("agentProfile.identity"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        AgentAvatar(display.avatar, size = 96.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(RemSpacing.xs)) {
            Text(
                display.name,
                style = RemTypography.title2Bold,
                color = colors.labelPrimary,
                textAlign = TextAlign.Center,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(colors.systemGreen))
                Text(display.statusLine, style = RemTypography.subheadline, color = colors.labelPrimary, textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * **AgentProfileAbout** — the About tab body (Compose twin of the SwiftUI view): **Lane** (Name,
 * Handles), **Connected services** (each + Add a service) and, for side agents only, **Pause agent** /
 * **Delete agent**. Rem's lane is read-only and it has no lifecycle actions. Figma About block
 * `2718:3205` in `2718:3055`. The tab control belongs to Agent activity (PR #106 reconciliation).
 */
@Composable
fun AgentProfileAbout(
    display: AgentProfileDisplay,
    onAction: (AgentProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RemColors.current
    Column(
        modifier.fillMaxWidth().testTag("agentProfile.about"),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        AgentFormSection(header = AgentProfileCopy.LaneHeader) {
            LaneRow(display, AgentProfileCopy.NameRow, display.name, "name") { onAction(AgentProfileAction.EditName) }
            AgentRowDivider(leadingInset = RemSpacing.lg)
            LaneRow(display, AgentProfileCopy.HandlesRow, display.handles, "handles") { onAction(AgentProfileAction.EditHandles) }
        }
        AgentFormSection(header = AgentServicesCopy.Header) {
            display.services.forEach { service ->
                AgentServiceRow(
                    title = service.name,
                    subtitle = service.status,
                    glyph = service.glyph,
                    modifier = Modifier.testTag("agentProfile.service.${service.id}"),
                    onClick = { onAction(AgentProfileAction.OpenService(service.id)) },
                )
                AgentRowDivider()
            }
            AgentServiceRow(
                title = AgentServicesCopy.AddService,
                subtitle = null,
                glyph = AgentServiceGlyph.Link,
                modifier = Modifier.testTag("agentProfile.addService"),
                onClick = { onAction(AgentProfileAction.AddService) },
            )
        }
        if (display.showsLifecycleActions) {
            RemSection {
                AgentTextAction(AgentProfileCopy.Pause, colors.brandBlue, Modifier.testTag("agentProfile.pause")) {
                    onAction(AgentProfileAction.Pause)
                }
                AgentTextAction(AgentProfileCopy.Delete, colors.systemRed, Modifier.testTag("agentProfile.delete")) {
                    onAction(AgentProfileAction.Delete)
                }
            }
        }
    }
}

@Composable
private fun LaneRow(display: AgentProfileDisplay, title: String, value: String, id: String, onClick: () -> Unit) {
    AgentListRow(
        title = title,
        subtitle = value.ifEmpty { null },
        modifier = Modifier.testTag("agentProfile.lane.$id"),
        onClick = if (display.isLaneEditable) onClick else null,
    )
}
