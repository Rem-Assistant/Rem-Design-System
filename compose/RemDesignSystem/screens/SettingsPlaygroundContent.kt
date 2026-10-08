package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rem.designsystem.icons.RemMaterialSymbol
import com.rem.designsystem.icons.RemMaterialSymbols as Symbols
import com.rem.designsystem.primitives.*
import com.rem.designsystem.rows.*
import com.rem.designsystem.tokens.*

/** The same seven stable identities as SwiftUI. Automations is deliberately absent. */
enum class AgentSettingsDestination { PairedDevices, Connectors, CloudBrowser, Memory, Models, Wallet, Voice }

/** Settled Settings New entry 1964:86819. Host owns scrolling and navigation. */
@Composable
fun SettingsEntryContent(openAgent: () -> Unit) = SettingsEntryContent(openAgent, onShare = null)

@Composable
fun SettingsEntryContent(openAgent: () -> Unit, onShare: (() -> Unit)?) {
    val colors = RemColors.current
    Column(Modifier.padding(horizontal = 16.dp).padding(top = 10.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        RemSection {
            ListRow(leading = { Box(Modifier.size(29.dp).background(colors.fillTertiary, CircleShape)) }, content = {
                Text("Avery Diaz", style = RemTypography.bodyBold, color = colors.labelPrimary)
                Text("avery@example.com", style = RemTypography.footnote, color = colors.labelSecondary)
            }, trailing = {})
        }
        RemSection { SettingsRowLabel("Rem", "Connected", Symbols.Info, minHeight = 82, onClick = openAgent, modifier = Modifier.testTag("openAgent")) }
        RemSection {
            SettingsRowLabel("Billing & Usage", symbol = Symbols.Billing, showsDivider = true)
            SettingsRowLabel("Permissions", symbol = Symbols.Permissions)
        }
        RemSection { SettingsRowLabel("About", symbol = Symbols.Info) }
        RemSection {
            SettingsRowLabel("Share Rem", symbol = Symbols.Share, showsDivider = true, disclosure = false, onClick = onShare, modifier = Modifier.testTag("shareRem"))
            SettingsRowLabel("Help & Support", symbol = Symbols.Share)
        }
        for (title in listOf("Sign Out", "Delete Account")) {
            RemSection {
                Box(Modifier.fillMaxWidth().heightIn(min = 60.dp).semantics { contentDescription = "$title. Visual reference; unavailable in this playground" }, contentAlignment = Alignment.Center) {
                    Text(title, style = RemTypography.body, color = colors.systemRed)
                }
            }
        }
    }
}

/** Only supplied routes are interactive. Automations remains a visibly unavailable reference. */
@Composable
fun AgentSettingsContent(
    availableDestinations: Set<AgentSettingsDestination> = emptySet(),
    openDestination: (AgentSettingsDestination) -> Unit = {},
) {
    @Composable fun destination(route: AgentSettingsDestination, title: String, subtitle: String? = null,
                                symbol: RemMaterialSymbol, divider: Boolean = false) {
        val available = route in availableDestinations
        SettingsRowLabel(title, subtitle, symbol, showsDivider = divider,
            onClick = if (available) { { openDestination(route) } } else null,
            modifier = Modifier.testTag("agentDestination.${route.name}"))
    }
    Column(Modifier.testTag("agentSettings").padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        RemSection(header = "Capabilities", footer = "Manage connected surfaces and how your agent can perform.", settingsHeader = true) {
            destination(AgentSettingsDestination.PairedDevices, "Paired Devices", "2", Symbols.Devices, true)
            destination(AgentSettingsDestination.Connectors, "Connectors", symbol = Symbols.Connectors, divider = true)
            destination(AgentSettingsDestination.CloudBrowser, "Cloud browser", symbol = Symbols.Browser, divider = true)
            SettingsRowLabel("Automations", "Scheduled and triggered work", Symbols.Automations,
                unavailableReason = "Unavailable. Automations design is awaiting a decision.", modifier = Modifier.testTag("automationsUnavailable"))
        }
        RemSection(header = "Intelligence", settingsHeader = true) {
            destination(AgentSettingsDestination.Memory, "Memory", symbol = Symbols.Memory, divider = true)
            destination(AgentSettingsDestination.Models, "Models", "Automatic", Symbols.Models, true)
            destination(AgentSettingsDestination.Wallet, "Wallet", symbol = Symbols.Wallet)
        }
        RemSection(header = "Experience", settingsHeader = true) {
            destination(AgentSettingsDestination.Voice, "Voice", "Aria", Symbols.Voice)
        }
    }
}

/** Canonical Settings row: explicit Subtle/settings icon, label slot and one optional disclosure. */
@Composable
fun SettingsRowLabel(
    title: String, subtitle: String? = null, symbol: RemMaterialSymbol,
    modifier: Modifier = Modifier, showsDivider: Boolean = false, disclosure: Boolean = true,
    minHeight: Int = 60, onClick: (() -> Unit)? = null,
    unavailableReason: String = "Visual reference; unavailable in this playground",
) {
    val rowModifier = if (onClick == null) modifier.semantics(mergeDescendants = true) {
        contentDescription = "$title. $unavailableReason"
    } else modifier
    ListRow(modifier = rowModifier, showsDivider = showsDivider, onClick = onClick,
        leading = { ContainedIcon(symbol, modifier = Modifier.clearAndSetSemantics {}, fill = ContainedIconFill.Subtle, size = ContainedIconSize.Settings) },
        content = {
            Column(Modifier.heightIn(min = (minHeight - 24).dp), verticalArrangement = Arrangement.Center) {
                ListRowLabel(title, subtitle)
            }
        }, trailing = { if (disclosure && onClick != null) DisclosureChevron() })
}
