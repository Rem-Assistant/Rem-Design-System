package com.rem.designsystem.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LaptopMac
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rem.designsystem.icons.RemMaterialSymbols as Symbols
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.primitives.ContainedIconSize
import com.rem.designsystem.rows.DisclosureChevron
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.*

/**
 * **Paired devices** — the Compose twin of SwiftUI `SettingsPairedDevicesScreen`, reproducing the
 * approved masters: populated list (`1833:5031`), empty state (`1833:52316`), device detail
 * (`1833:52344`), and the remove-access confirmation (`1839:52547`).
 *
 * This destination **owns its own scaffold, title, and nested stack** (list ↔ detail) with a standard
 * [onBack] callback for the host. Native Material semantics: a center-aligned top bar with a back
 * control, an [AlertDialog] confirmation for removal, and an [AlertDialog] boundary for Add. `Add` has
 * no authored pairing flow, so it states pairing is not simulated — it never fabricates a device.
 *
 * State is in-memory for the session (see [PairedDevicesState]). The Android device glyph uses the
 * registry `devices` Material Symbol for both the empty hero and the detail hero; iOS distinguishes
 * `macbook.and.iphone` (empty) and `laptopcomputer` (detail). See the task coverage doc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPairedDevicesScreen(onBack: () -> Unit) {
    var state by rememberSaveable(stateSaver = listSaver<PairedDevicesState, String>(
        save = { it.peers.map { peer -> peer.id } },
        restore = { ids -> PairedDevicesState(PairedDevicesState().peers.filter { it.id in ids }) },
    )) { mutableStateOf(PairedDevicesState()) }
    var selectedPeerId by rememberSaveable { mutableStateOf<String?>(null) }
    var showingAddBoundary by rememberSaveable { mutableStateOf(false) }

    val selectedPeer = selectedPeerId?.let { state.peer(it) }
    val onDetail = selectedPeer != null
    val back = { if (onDetail) selectedPeerId = null else onBack() }
    BackHandler(enabled = true) { back() }

    val colors = RemColors.current
    Scaffold(
        modifier = Modifier.testTag("pairedDevices"),
        containerColor = colors.backgroundPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(selectedPeer?.name ?: PairedDevicesCopy.NAVIGATION_TITLE, style = RemTypography.bodyBold)
                },
                navigationIcon = {
                    IconButton(onClick = back, modifier = Modifier.testTag("pairedDevices.back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!onDetail && !state.isEmpty) {
                        TextButton(onClick = { showingAddBoundary = true }, modifier = Modifier.testTag("pairedDevices.add")) {
                            Text(PairedDevicesCopy.ADD_ACTION, style = RemTypography.bodyBold, color = colors.brandBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.backgroundPrimary),
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (selectedPeer != null) {
                PairedDeviceDetail(peer = selectedPeer, onRemove = {
                    state = state.removing(it)
                    selectedPeerId = null
                })
            } else if (state.isEmpty) {
                PairedDevicesEmpty(onRefresh = { state = state.refreshed() })
            } else {
                PairedDevicesList(state = state, onOpen = { selectedPeerId = it })
            }
        }
    }

    if (showingAddBoundary) {
        AddBoundaryDialog(onDismiss = { showingAddBoundary = false })
    }
}

@Composable
private fun PairedDevicesList(state: PairedDevicesState, onOpen: (String) -> Unit) {
    val colors = RemColors.current
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 24.dp)) {
        RemSection(
            header = PairedDevicesCopy.SECTION_HEADER,
            footer = PairedDevicesCopy.LIST_FOOTER,
            settingsHeader = true,
        ) {
            state.peers.forEachIndexed { index, peer ->
                ListRow(
                    modifier = Modifier.testTag("pairedDevices.peer.${peer.id}"),
                    showsDivider = index < state.peers.lastIndex,
                    onClick = { onOpen(peer.id) },
                    leading = {
                        Icon(Icons.Outlined.LaptopMac, contentDescription = null,
                            modifier = Modifier.size(29.dp), tint = colors.labelPrimary)
                    },
                    content = {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(peer.name, style = RemTypography.body, color = colors.labelPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    Modifier.size(8.dp).background(
                                        if (peer.isConnected) colors.systemGreen else colors.labelTertiary,
                                        CircleShape,
                                    ).clearAndSetSemantics {},
                                )
                                Text(peer.connectionSummary, style = RemTypography.footnote, color = colors.labelSecondary)
                            }
                        }
                    },
                    trailing = { DisclosureChevron() },
                )
            }
        }
    }
}

@Composable
private fun PairedDevicesEmpty(onRefresh: () -> Unit) {
    val colors = RemColors.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(RemSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RemSpacing.lg, Alignment.CenterVertically),
    ) {
        ContainedIcon(
            symbol = Symbols.Devices,
            modifier = Modifier.clearAndSetSemantics {},
            fill = ContainedIconFill.Subtle,
            size = ContainedIconSize.Large,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(RemSpacing.sm)) {
            Text(PairedDevicesCopy.EMPTY_TITLE, style = RemTypography.title1Bold, color = colors.labelPrimary, textAlign = TextAlign.Center)
            Text(PairedDevicesCopy.EMPTY_MESSAGE, style = RemTypography.subheadline, color = colors.labelSecondary, textAlign = TextAlign.Center)
        }
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth().testTag("pairedDevices.refresh"),
            colors = ButtonDefaults.buttonColors(containerColor = colors.brandBlue, contentColor = colors.labelOnColor),
        ) {
            Text(PairedDevicesCopy.REFRESH_ACTION, style = RemTypography.bodyBold)
        }
    }
}

@Composable
private fun PairedDeviceDetail(peer: PairedDevicePeer, onRemove: (String) -> Unit) {
    val colors = RemColors.current
    var showingConfirmation by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(RemSpacing.xs)) {
            ContainedIcon(
                symbol = Symbols.Devices,
                modifier = Modifier.clearAndSetSemantics {},
                fill = ContainedIconFill.Subtle,
                size = ContainedIconSize.Large,
            )
            Text(peer.name, style = RemTypography.title1Bold, color = colors.labelPrimary)
            Text(peer.kind, style = RemTypography.subheadline, color = colors.labelSecondary)
        }

        RemSection(header = PairedDevicesCopy.CONNECTION_SECTION_HEADER, settingsHeader = true) {
            peer.connectionRows.forEachIndexed { index, row ->
                ListRow(
                    showsDivider = index < peer.connectionRows.lastIndex,
                    leading = {},
                    content = { Text(row.label, style = RemTypography.body, color = colors.labelPrimary) },
                    trailing = { Text(row.value, style = RemTypography.body, color = colors.labelSecondary) },
                )
            }
        }

        RemSection(footer = PairedDevicesCopy.DETAIL_FOOTER) {
            ListRow(
                modifier = Modifier.testTag("pairedDevices.removeAccess"),
                onClick = { showingConfirmation = true },
                leading = {},
                content = {
                    Text(
                        PairedDevicesCopy.REMOVE_ACCESS,
                        style = RemTypography.body,
                        color = colors.systemRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                trailing = {},
            )
        }
    }

    if (showingConfirmation) {
        AlertDialog(
            modifier = Modifier.testTag("pairedDevices.removeConfirmation"),
            onDismissRequest = { showingConfirmation = false },
            title = { Text(PairedDevicesCopy.removeConfirmationTitle(peer.name)) },
            text = { Text(PairedDevicesCopy.REMOVE_CONFIRMATION_MESSAGE) },
            confirmButton = {
                TextButton(
                    onClick = { showingConfirmation = false; onRemove(peer.id) },
                    modifier = Modifier.testTag("pairedDevices.confirmRemove"),
                ) {
                    Text(PairedDevicesCopy.REMOVE_ACCESS, color = colors.systemRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showingConfirmation = false }) { Text(PairedDevicesCopy.CANCEL) }
            },
        )
    }
}

@Composable
private fun AddBoundaryDialog(onDismiss: () -> Unit) {
    AlertDialog(
        modifier = Modifier.testTag("pairedDevices.addBoundary"),
        onDismissRequest = onDismiss,
        title = { Text(PairedDevicesCopy.ADD_TITLE) },
        text = { Text(PairedDevicesCopy.ADD_MESSAGE) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(PairedDevicesCopy.ADD_DISMISS) } },
    )
}
