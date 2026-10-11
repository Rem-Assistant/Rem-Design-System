package com.rem.designsystem.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rem.designsystem.agents.AgentSectionHeader
import com.rem.designsystem.brand.RemFaceMark
import com.rem.designsystem.brand.RemFaceMarkMode
import com.rem.designsystem.icons.RemMaterialSymbol
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography

/**
 * **RemSidebar** — the navigation drawer, Compose twin of `Navigation/RemSidebar.swift`: "Rem" title
 * with **Search** (the only top action), **Main agent** (Rem) and **Side agents** sections with lane
 * subtitles, and floating bottom actions — the account pill (opens Settings) and **New agent**. The
 * list scrolls under the bottom actions.
 *
 * Figma: sidebar column `2702:2514` + bottom actions `2702:2682` in `2702:2479`; account pill
 * `2709:2648`. Host it in [RemSidebarContainer] for the push-open behaviour. Display + callbacks only.
 */
@Composable
fun RemSidebar(
    display: RemSidebarDisplay,
    onAction: (RemSidebarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RemColors.current
    Box(
        modifier
            .fillMaxSize()
            .background(colors.backgroundSecondary)
            .testTag("sidebar"),
    ) {
        Column(Modifier.fillMaxSize().padding(start = RemSpacing.lg, end = RemSpacing.md)) {
            Header(onAction)
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = RemSpacing.md, bottom = 128.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(Modifier.testTag("sidebar.section.main")) {
                    AgentSectionHeader(RemSidebarDisplay.MainAgentHeader)
                    SidebarAgentRow(
                        agent = display.mainAgent,
                        isMain = true,
                        isSelected = display.isMainSelected,
                        modifier = Modifier.testTag("sidebar.agent.main"),
                        onClick = { onAction(RemSidebarAction.SelectMain) },
                    )
                }
                if (display.showsSideAgents) {
                    Column(Modifier.testTag("sidebar.section.side")) {
                        AgentSectionHeader(RemSidebarDisplay.SideAgentsHeader)
                        display.sideAgents.forEach { agent ->
                            SidebarAgentRow(
                                agent = agent,
                                isMain = false,
                                isSelected = display.isSelected(agent),
                                modifier = Modifier.testTag("sidebar.agent.${agent.id}"),
                                onClick = { onAction(RemSidebarAction.SelectSide(agent.id)) },
                            )
                        }
                    }
                }
            }
        }
        BottomActions(display.account, onAction, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Header(onAction: (RemSidebarAction) -> Unit) {
    val colors = RemColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = RemSpacing.xs, top = RemSpacing.sm + 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            RemSidebarDisplay.Title,
            style = RemTypography.title1Bold,
            color = colors.labelPrimary,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(44.dp)
                .shadow(20.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.12f))
                .clip(CircleShape)
                .background(colors.backgroundPrimary)
                .clickable { onAction(RemSidebarAction.Search) }
                .semantics {
                    role = Role.Button
                    contentDescription = RemSidebarDisplay.Search
                }
                .testTag("sidebar.search"),
            contentAlignment = Alignment.Center,
        ) {
            SymbolGlyph(RemMaterialSymbols.Search, 22, colors.labelPrimary)
        }
    }
}

/** One sidebar agent row: leading mark, name, lane subtitle, optional unread dot; selected = fillTertiary. */
@Composable
private fun SidebarAgentRow(
    agent: RemSidebarAgent,
    isMain: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = RemColors.current
    val shape = RoundedCornerShape(RemRadius.medium)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(shape)
            .background(if (isSelected) colors.fillTertiary else Color.Transparent)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                role = Role.Button
                selected = isSelected
                contentDescription = listOfNotNull(
                    agent.name,
                    agent.lane.ifEmpty { null },
                    if (agent.hasUnread) "Unread" else null,
                ).joinToString(", ")
            }
            .padding(horizontal = RemSpacing.md, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        Box(Modifier.size(29.dp), contentAlignment = Alignment.Center) {
            if (isMain) {
                RemFaceMark(mode = RemFaceMarkMode.Idle, tint = colors.brandBlue, size = 24.dp)
            } else {
                SymbolGlyph(RemMaterialSymbols.SideAgentChat, 20, colors.labelPrimary)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                agent.name,
                style = if (isMain) RemTypography.body.copy(fontWeight = FontWeight.SemiBold) else RemTypography.body,
                color = colors.labelPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (agent.lane.isNotEmpty()) {
                Text(agent.lane, style = RemTypography.footnote, color = colors.labelSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (agent.hasUnread) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(colors.brandBlue))
        }
    }
}

@Composable
private fun BottomActions(account: RemSidebarAccount, onAction: (RemSidebarAction) -> Unit, modifier: Modifier) {
    val colors = RemColors.current
    Row(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to colors.backgroundSecondary.copy(alpha = 0f),
                    0.45f to colors.backgroundSecondary,
                    1f to colors.backgroundSecondary,
                ),
            )
            .padding(start = RemSpacing.lg, end = RemSpacing.md, top = 32.dp, bottom = RemSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // New agent keeps its one-line label; a long account name truncates inside the pill instead.
        Box(Modifier.weight(1f, fill = false).padding(end = RemSpacing.sm)) {
            RemAccountPill(account, onClick = { onAction(RemSidebarAction.OpenSettings) })
        }
        Row(
            Modifier
                .height(56.dp)
                .shadow(9.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.18f))
                .clip(CircleShape)
                .background(colors.labelPrimary)
                .clickable { onAction(RemSidebarAction.NewAgent) }
                .semantics(mergeDescendants = true) { role = Role.Button }
                .testTag("sidebar.newAgent")
                .padding(start = 20.dp, end = RemSpacing.xl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                SymbolGlyph(RemMaterialSymbols.Add, 24, colors.backgroundPrimary)
            }
            Text(RemSidebarDisplay.NewAgent, style = RemTypography.bodyBold, color = colors.backgroundPrimary, maxLines = 1, softWrap = false)
        }
    }
}

/** A registry Material Symbol rendered as a font glyph at its pinned FILL (icon-registry rule 1). */
@Composable
internal fun SymbolGlyph(symbol: RemMaterialSymbol, sizeSp: Int, color: Color) {
    Text(
        text = symbol.glyph,
        fontFamily = RemMaterialSymbols.family(symbol),
        fontSize = sizeSp.sp,
        color = color,
        modifier = Modifier.clearAndSetSemantics {},
    )
}
