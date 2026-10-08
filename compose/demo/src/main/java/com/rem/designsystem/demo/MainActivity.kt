package com.rem.designsystem.demo

import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.primitives.*
import com.rem.designsystem.screens.*
import com.rem.designsystem.tokens.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val dark = if (intent.hasExtra("settingsDark")) intent.getBooleanExtra("settingsDark", false) else isSystemInDarkTheme()
            val density = LocalDensity.current
            val scale = if (intent.getBooleanExtra("settingsLargeText", false)) 2f else density.fontScale
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                RemTheme(darkTheme = dark) {
                    val background = RemColors.current.backgroundPrimary
                    SideEffect {
                        window.statusBarColor = background.toArgb()
                        window.navigationBarColor = background.toArgb()
                        WindowCompat.getInsetsController(window, window.decorView).apply {
                            isAppearanceLightStatusBars = !dark
                            isAppearanceLightNavigationBars = !dark
                        }
                    }
                    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) { Playground() }
                }
            }
        }
    }
}

enum class LoadFixture { Success, Slow, Error }
private enum class Route { Home, Settings, Agent, Controls }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Playground() {
    val context = LocalContext.current
    var route by rememberSaveable { mutableStateOf(Route.Home) }
    var destination by rememberSaveable { mutableStateOf<AgentSettingsDestination?>(null) }
    var fixture by rememberSaveable { mutableStateOf(LoadFixture.Success) }
    val back = { route = if (route == Route.Agent) Route.Settings else Route.Home }
    BackHandler(route != Route.Home && destination == null) { back() }
    val title = when (route) { Route.Home -> "Rem Playground"; Route.Settings -> "Settings"; Route.Agent -> "Agent settings"; Route.Controls -> "Shared controls" }
    Scaffold(containerColor = RemColors.current.backgroundPrimary, topBar = {
        if (destination == null) {
        CenterAlignedTopAppBar(title = { Text(title, style = RemTypography.bodyBold) }, navigationIcon = {
            if (route != Route.Home) IconButton(onClick = back, modifier = Modifier.testTag("back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = RemColors.current.backgroundPrimary))
        }
    }) { padding ->
        Box(Modifier.then(if (destination == null) Modifier.padding(padding) else Modifier).fillMaxSize()) {
            when (route) {
                Route.Home -> Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Settings New · Android", style = RemTypography.title3Bold)
                    Text("A local prototype with illustrative data. Paired Devices, Cloud browser, Memory, Models, Wallet and Voice are connected. Voice preview controls are simulated without audio. Connectors is awaiting integration; Automations remains outside this trial.", style = RemTypography.footnote)
                    Text("Load fixture", style = RemTypography.bodyBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LoadFixture.entries.forEach { value -> FilterChip(selected = fixture == value, onClick = { fixture = value }, label = { Text(value.name) }) }
                    }
                    Button(onClick = { route = Route.Settings }, modifier = Modifier.testTag("openSettings")) { Text("Open Settings") }
                    OutlinedButton(onClick = { route = Route.Controls }) { Text("Shared controls") }
                }
                Route.Settings -> Column(Modifier.verticalScroll(rememberScrollState())) {
                    SettingsEntryContent(openAgent = { route = Route.Agent }, onShare = {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Rem — a personal AI assistant. Shared from the local Settings playground.")
                        }
                        context.startActivity(Intent.createChooser(share, null))
                    })
                }
                Route.Agent -> AgentPreview(fixture, destination = destination,
                    onOpenDestination = { destination = it }, onDestinationBack = { destination = null },
                    onCancel = { route = Route.Settings })
                Route.Controls -> ControlsPreview()
            }
        }
    }
}

@Composable
private fun AgentPreview(fixture: LoadFixture, destination: AgentSettingsDestination?,
                         onOpenDestination: (AgentSettingsDestination) -> Unit,
                         onDestinationBack: () -> Unit, onCancel: () -> Unit) {
    var status by remember { mutableStateOf("loading") }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        status = "loading"
        delay(if (fixture == LoadFixture.Slow && attempt == 0) 10000 else 200)
        status = if (fixture == LoadFixture.Error && attempt == 0) "error" else "ready"
    }
    if (status == "ready") {
        when (destination) {
            AgentSettingsDestination.PairedDevices -> SettingsPairedDevicesScreen(onBack = onDestinationBack)
            AgentSettingsDestination.Memory -> SettingsMemoryScreen(onBack = onDestinationBack)
            AgentSettingsDestination.Models -> SettingsModelsScreen(onBack = onDestinationBack)
            AgentSettingsDestination.CloudBrowser -> SettingsCloudBrowserScreen(onBack = onDestinationBack)
            AgentSettingsDestination.Wallet -> SettingsWalletScreen(onBack = onDestinationBack)
            AgentSettingsDestination.Voice -> SettingsVoiceScreen(onBack = onDestinationBack)
            else -> Column(Modifier.verticalScroll(rememberScrollState())) {
                AgentSettingsContent(availableDestinations = setOf(AgentSettingsDestination.PairedDevices,
                    AgentSettingsDestination.CloudBrowser, AgentSettingsDestination.Memory, AgentSettingsDestination.Models,
                    AgentSettingsDestination.Wallet, AgentSettingsDestination.Voice), openDestination = onOpenDestination)
            }
        }
    }
    else Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)) {
        if (status == "loading") {
            CircularProgressIndicator()
            Text("Loading agent settings…")
        } else {
            Text(RemMaterialSymbols.ErrorNotice.glyph, fontFamily = RemMaterialSymbols.family(RemMaterialSymbols.ErrorNotice), fontSize = 34.sp, color = RemColors.current.systemOrange, modifier = Modifier.clearAndSetSemantics {})
            Text("Couldn’t load agent settings", style = RemTypography.title3Bold)
            Text("This is a simulated connection error. Retry to load the local fixture.", textAlign = TextAlign.Center)
            Button(onClick = { attempt += 1 }, modifier = Modifier.testTag("retry")) { Text("Retry") }
        }
        TextButton(onClick = onCancel, modifier = Modifier.testTag("cancelLoad")) { Text("Cancel") }
    }
}

@Composable
private fun ControlsPreview() {
    var enabled by rememberSaveable { mutableStateOf(true) }
    var name by rememberSaveable { mutableStateOf("Avery Diaz") }
    var draft by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Notifications", Modifier.weight(1f)); Switch(enabled, onCheckedChange = { enabled = it }, modifier = Modifier.testTag("notifications").semantics { contentDescription = "Notifications" })
        }
        OutlinedButton(onClick = { draft = name; editing = true }, modifier = Modifier.testTag("editName")) { Text("Display name: $name") }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ContainedIconSize.entries.forEach { size -> ContainedIcon(RemMaterialSymbols.Info, fill = ContainedIconFill.Tint(RemColors.current.systemBlue), size = size) }
        }
        Button(onClick = {}, enabled = false) { Text("Disabled example") }
        Text("Changes are local to this controls session.", style = RemTypography.footnote)
    }
    if (editing) AlertDialog(onDismissRequest = { editing = false }, title = { Text("Edit name") }, text = {
        OutlinedTextField(draft, onValueChange = { draft = it }, label = { Text("Display name") }, modifier = Modifier.testTag("nameField"))
    }, confirmButton = { TextButton(onClick = { name = draft.trim(); editing = false }, enabled = draft.trim().isNotEmpty()) { Text("Save") } }, dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } })
}
