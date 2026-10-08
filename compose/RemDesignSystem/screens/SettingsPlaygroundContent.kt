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

/** Settled Settings New entry 1964:86819. Host owns navigation/data; other rows are references. */
@Composable
fun SettingsEntryContent(openAgent: () -> Unit) {
    val colors = RemColors.current
    Column(Modifier.padding(horizontal = 16.dp).padding(top = 10.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        RemSection {
            ListRow(leading = { Box(Modifier.size(29.dp).background(Color(0xFFDBDBE5), CircleShape)) }, content = {
                Text("Avery Diaz", style = RemTypography.bodyBold, color = colors.labelPrimary)
                Text("avery@example.com", style = RemTypography.footnote, color = colors.labelSecondary)
            }, trailing = {})
        }
        RemSection { SettingsReferenceRow("Rem", "Connected", Symbols.Info, colors.systemBlue, minHeight = 82, onClick = openAgent, modifier = Modifier.testTag("openAgent")) }
        RemSection {
            SettingsReferenceRow("Billing & Usage", symbol = Symbols.Billing, color = colors.systemBlue, divider = true)
            SettingsReferenceRow("Permissions", symbol = Symbols.Permissions, color = colors.systemBlue)
        }
        RemSection { SettingsReferenceRow("About", symbol = Symbols.Info, color = Color.Gray) }
        RemSection {
            SettingsReferenceRow("Share Rem", symbol = Symbols.Share, color = colors.systemGreen, divider = true, disclosure = false)
            SettingsReferenceRow("Help & Support", symbol = Symbols.Share, color = colors.systemBlue)
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

/** Agent Settings 1827:50855. Automations is a reference only, outside this implementation. */
@Composable
fun AgentSettingsContent() {
    val colors = RemColors.current
    val pink = Color(0xFFFF2D55)
    Column(Modifier.testTag("agentSettings").padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        RemSection(header = "Capabilities", footer = "Manage connected surfaces and how your agent can perform.", settingsHeader = true) {
            SettingsReferenceRow("Paired Devices", "2", Symbols.Devices, colors.systemIndigo, divider = true)
            SettingsReferenceRow("Connectors", symbol = Symbols.Connectors, color = colors.systemPurple, divider = true)
            SettingsReferenceRow("Cloud browser", symbol = Symbols.Browser, color = colors.systemBlue, divider = true)
            SettingsReferenceRow("Automations", "Scheduled and triggered work", Symbols.Automations, pink)
        }
        RemSection(header = "Intelligence", settingsHeader = true) {
            SettingsReferenceRow("Memory", symbol = Symbols.Memory, color = pink, divider = true)
            SettingsReferenceRow("Models", "Automatic", Symbols.Models, colors.systemIndigo, divider = true)
            SettingsReferenceRow("Wallet", symbol = Symbols.Wallet, color = colors.systemBlue)
        }
        RemSection(header = "Experience", settingsHeader = true) {
            SettingsReferenceRow("Voice", "Aria", Symbols.Voice, colors.systemBlue)
        }
    }
}

@Composable
private fun SettingsReferenceRow(
    title: String, subtitle: String? = null, symbol: RemMaterialSymbol, color: Color,
    divider: Boolean = false, disclosure: Boolean = true, minHeight: Int = 60, onClick: (() -> Unit)? = null, modifier: Modifier = Modifier,
) {
    val colors = RemColors.current
    ListRow(modifier = modifier, showsDivider = divider, onClick = onClick,
        leading = { ContainedIcon(symbol, modifier = Modifier.clearAndSetSemantics {}, fill = ContainedIconFill.Tint(color), size = ContainedIconSize.Settings) },
        content = {
            Column(Modifier.heightIn(min = (minHeight - 24).dp), verticalArrangement = Arrangement.Center) {
                Text(title, style = RemTypography.body, color = colors.labelPrimary)
                if (subtitle != null) Text(subtitle, style = RemTypography.footnote, color = colors.labelSecondary)
            }
        }, trailing = { if (disclosure) DisclosureChevron() })
}
