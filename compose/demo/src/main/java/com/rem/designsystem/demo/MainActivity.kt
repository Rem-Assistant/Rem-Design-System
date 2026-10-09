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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import com.rem.designsystem.onboarding.*
import com.rem.designsystem.primitives.*
import com.rem.designsystem.screens.*
import com.rem.designsystem.rows.DisclosureChevron
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.rows.ListRow
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
                    val rem = RemColors.current
                    val nativeColors = (if (dark) darkColorScheme() else lightColorScheme()).copy(
                        primary = rem.brandBlue, onPrimary = rem.labelOnColor,
                        primaryContainer = rem.backgroundSecondary, onPrimaryContainer = rem.labelPrimary,
                        secondary = rem.brandBlue, onSecondary = rem.labelOnColor,
                        secondaryContainer = rem.backgroundSecondary, onSecondaryContainer = rem.labelPrimary,
                        surface = rem.backgroundPrimary, onSurface = rem.labelPrimary,
                        surfaceVariant = rem.backgroundSecondary, onSurfaceVariant = rem.labelSecondary,
                        surfaceTint = rem.backgroundSecondary,
                        surfaceContainer = rem.backgroundSecondary,
                        surfaceContainerHigh = rem.backgroundSecondary,
                        background = rem.backgroundPrimary, onBackground = rem.labelPrimary,
                        error = rem.systemRed,
                    )
                    MaterialTheme(colorScheme = nativeColors) { Playground() }
                }
            }
        }
    }
}

enum class LoadFixture { Success, Slow, Error }
private enum class Route { Home, Components, Controls, Rows, CatalogAgenda, Chat, AgentCatalog, Brand, Loading, Settings, Agent, AgendaSuggestions, Onboarding }

/** Catalog pages, in browse order: title, route and the tag tests use to open each. */
private val catalogPages = listOf(
    Triple("Controls", Route.Controls, "openControls"),
    Triple("Rows", Route.Rows, "openRows"),
    Triple("Agenda", Route.CatalogAgenda, "openCatalogAgenda"),
    Triple("Chat", Route.Chat, "openChat"),
    Triple("Agent", Route.AgentCatalog, "openAgentCatalog"),
    Triple("Brand & empty states", Route.Brand, "openBrand"),
    Triple("Loading", Route.Loading, "openLoading"),
)

/** The established onboarding order: Sign in → Consent → Connectors → Check-in → Voice. */
enum class PlaygroundOnboardingStep(val title: String, val tag: String) {
    SignIn("Sign in", "openOnboardingSignIn"),
    Consent("Privacy", "openOnboardingConsent"),
    Connectors("Connectors", "openOnboardingConnectors"),
    CheckIn("Check-in", "openOnboardingCheckIn"),
    Voice("Voice", "openOnboardingVoice"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Playground() {
    val context = LocalContext.current
    var route by rememberSaveable { mutableStateOf(Route.Home) }
    var destination by rememberSaveable { mutableStateOf<AgentSettingsDestination?>(null) }
    var fixture by rememberSaveable { mutableStateOf(LoadFixture.Success) }
    var agendaFixture by rememberSaveable { mutableStateOf(AgendaSuggestionsFixture.Loaded) }
    var checkInFailsOnce by rememberSaveable { mutableStateOf(false) }
    // Pushed onboarding steps (ordinals), mirroring the iOS navigation stack: Continue / Skip push the
    // next step, Back pops, and an empty stack is the onboarding hub.
    var onboardingStack by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var onboardingComplete by rememberSaveable { mutableStateOf<String?>(null) }
    val inOnboardingFlow = route == Route.Onboarding && (onboardingStack.isNotEmpty() || onboardingComplete != null)
    // Settings destinations and onboarding steps own their chrome and Back handling.
    val fullScreen = destination != null || inOnboardingFlow
    val back = {
        route = when (route) {
            Route.Agent -> Route.Settings
            Route.Controls, Route.Rows, Route.CatalogAgenda, Route.Chat, Route.AgentCatalog, Route.Brand, Route.Loading -> Route.Components
            else -> Route.Home
        }
    }
    BackHandler(route != Route.Home && !fullScreen) { back() }
    val title = when (route) {
        Route.Home -> "Rem Playground"; Route.Components -> "Components"; Route.Controls -> "Controls"
        Route.Rows -> "Rows"; Route.CatalogAgenda -> "Agenda"; Route.Chat -> "Chat"; Route.AgentCatalog -> "Agent"
        Route.Brand -> "Brand & empty states"
        Route.Loading -> "Loading"; Route.Settings -> "Settings"; Route.Agent -> "Agent settings"
        Route.AgendaSuggestions -> "Agenda New"; Route.Onboarding -> "Onboarding"
    }
    Scaffold(containerColor = RemColors.current.backgroundPrimary, topBar = {
        if (!fullScreen) {
        CenterAlignedTopAppBar(title = { Text(title, style = RemTypography.bodyBold) }, navigationIcon = {
            if (route != Route.Home) IconButton(onClick = back, modifier = Modifier.testTag("back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = RemColors.current.backgroundPrimary))
        }
    }) { padding ->
        Box(Modifier.then(if (!fullScreen) Modifier.padding(padding) else Modifier).fillMaxSize()) {
            when (route) {
                Route.Home -> Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    RemSection(header = "Components") {
                        NavRow("Component catalog", "openComponents") { route = Route.Components }
                    }
                    Column {
                        RemSection(header = "Screens") {
                            NavRow("Settings", "openSettings") { route = Route.Settings }
                            ChipRow { LoadFixture.entries.forEach { value -> FilterChip(selected = fixture == value, onClick = { fixture = value }, label = { Text(value.name) }) } }
                            RowDivider()
                            NavRow("Agenda", "openAgendaSuggestions") { route = Route.AgendaSuggestions }
                            ChipRow { AgendaSuggestionsFixture.entries.forEach { value -> FilterChip(selected = agendaFixture == value, onClick = { agendaFixture = value }, label = { Text(value.name) }) } }
                            RowDivider()
                            NavRow("Onboarding", "openOnboarding") { onboardingStack = emptyList(); onboardingComplete = null; route = Route.Onboarding }
                        }
                        Text("Version ${BuildConfig.VERSION_NAME} · ${BuildConfig.PLAYGROUND_SOURCE_SHA.take(12)}", style = RemTypography.footnote, color = RemColors.current.labelSecondary,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp).testTag("playground.build"))
                    }
                }
                Route.Components -> Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                    RemSection {
                        catalogPages.forEachIndexed { index, (label, page, tag) ->
                            if (index > 0) RowDivider()
                            NavRow(label, tag) { route = page }
                        }
                    }
                }
                Route.Controls -> ControlsPreview()
                Route.Rows -> CatalogRows()
                Route.CatalogAgenda -> CatalogAgenda()
                Route.Chat -> CatalogChat()
                Route.AgentCatalog -> CatalogAgent()
                Route.Brand -> CatalogBrand()
                Route.Loading -> LoadingPreview()
                Route.Settings -> Column(Modifier.verticalScroll(rememberScrollState())) {
                    SettingsEntryContent(openAgent = { route = Route.Agent }, onShare = {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Rem — a personal AI assistant.")
                        }
                        context.startActivity(Intent.createChooser(share, null))
                    })
                }
                Route.Agent -> AgentPreview(fixture, destination = destination,
                    onOpenDestination = { destination = it }, onDestinationBack = { destination = null },
                    onCancel = { route = Route.Settings })
                Route.AgendaSuggestions -> AgendaSuggestionsPlayground(fixture = agendaFixture)
                Route.Onboarding -> {
                    val completed = onboardingComplete
                    val current = onboardingStack.lastOrNull()?.let { PlaygroundOnboardingStep.entries[it] }
                    when {
                        completed != null -> OnboardingCompletePreview(completed) {
                            onboardingComplete = null; onboardingStack = emptyList()
                        }
                        current != null -> {
                            val pop = { onboardingStack = onboardingStack.dropLast(1) }
                            BackHandler { pop() }
                            OnboardingStepPreview(current, checkInFailsOnce, onBack = pop) { action ->
                                val next = current.ordinal + 1
                                if (next < PlaygroundOnboardingStep.entries.size) onboardingStack = onboardingStack + next
                                else onboardingComplete = action
                            }
                        }
                        else -> Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                            RemSection {
                                PlaygroundOnboardingStep.entries.forEachIndexed { index, step ->
                                    if (index > 0) RowDivider()
                                    NavRow(step.title, step.tag) { onboardingStack = listOf(step.ordinal) }
                                    if (step == PlaygroundOnboardingStep.CheckIn) ChipRow {
                                        // The Check-in step's save fixture, beside its row like the root's data pickers.
                                        listOf(false to "Succeeds", true to "Fails once").forEach { (fails, label) ->
                                            FilterChip(selected = checkInFailsOnce == fails, onClick = { checkInFailsOnce = fails },
                                                label = { Text(label) }, modifier = Modifier.testTag("checkInSave.$label"))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Navigation row: the shared ListRow with a disclosure chevron, inside a RemSection. */
@Composable
private fun NavRow(label: String, tag: String, onClick: () -> Unit) {
    ListRow(title = label, onClick = onClick, trailing = { DisclosureChevron() }, modifier = Modifier.testTag(tag))
}

/** Data pickers for the screen row above, inside the same section. */
@Composable
private fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
private fun RowDivider() {
    HorizontalDivider(Modifier.padding(start = 16.dp), color = RemColors.current.separator)
}

/** Skeleton → content for a content load, and an inline progress indicator for an action. */
@Composable
private fun LoadingPreview() {
    var attempt by rememberSaveable { mutableIntStateOf(0) }
    var loaded by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotion()
    val rows = listOf("Paired devices", "Connectors", "Memory", "Voice")
    LaunchedEffect(attempt) { loaded = false; delay(3000); loaded = true }
    LaunchedEffect(refreshing) { if (refreshing) { delay(2500); refreshing = false } }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        androidx.compose.animation.Crossfade(targetState = loaded, animationSpec = androidx.compose.animation.core.tween(if (reduceMotion) 0 else 250), label = "loading") { ready ->
            if (ready) {
                Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite; contentDescription = "Content loaded" }) {
                    rows.forEach { Text(it, Modifier.padding(vertical = 14.dp)) }
                }
            } else {
                SkeletonList(label = "Loading content", rows = rows.size, modifier = Modifier.testTag("loading.skeleton"))
            }
        }
        OutlinedButton(onClick = { refreshing = true }, enabled = loaded && !refreshing, modifier = Modifier.fillMaxWidth().testTag("loading.refresh")) {
            Text(if (refreshing) "Refreshing" else "Refresh", Modifier.weight(1f))
            if (refreshing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        }
        TextButton(onClick = { attempt += 1 }, enabled = loaded, modifier = Modifier.testTag("loading.reload")) { Text("Reload") }
    }
}

/**
 * Check-in cadence (PR #53's shared screen) over local fixture state: switches and the platform time
 * picker edit the cadence, and Continue runs the save lifecycle (Saving → Saved) before the flow
 * moves on. With the hub's "Fails once" fixture the first save fails, so Try again exercises
 * recovery. Nothing is scheduled or persisted.
 */
@Composable
private fun CheckInStepPreview(failFirstSave: Boolean, progress: OnboardingProgress, onBack: () -> Unit, onContinue: () -> Unit) {
    var checkins by remember { mutableStateOf(checkinDefaultCadence()) }
    var status by remember { mutableStateOf<CheckinStatus>(CheckinStatus.Default) }
    var failedOnce by remember { mutableStateOf(false) }
    var saveAttempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(saveAttempt) {
        if (saveAttempt == 0) return@LaunchedEffect
        delay(1000)
        if (failFirstSave && !failedOnce) {
            failedOnce = true
            status = CheckinStatus.Failure("We couldn't save your check-in times. Check your connection and try again.")
            return@LaunchedEffect
        }
        status = CheckinStatus.Saved
        delay(800)
        onContinue()
    }
    val save = { status = CheckinStatus.Saving; saveAttempt += 1 }
    fun update(slot: String, change: (Checkin) -> Checkin) {
        checkins = checkins.map { if (it.slot == slot) change(it) else it }
        status = CheckinStatus.Edited
    }
    OnboardingCheckinScreen(
        status = status,
        periods = checkinPeriods(checkins),
        onToggle = { slot, on -> update(slot) { it.copy(enabled = on) } },
        onTimeChange = { slot, hour, minute -> update(slot) { it.copy(deliveryHour = hour, deliveryMinute = minute) } },
        onContinue = save,
        onRetry = save,
        progress = progress,
        onBack = onBack,
    )
}

/** One onboarding step, wired so Continue / Skip move the flow on with the action taken. */
@Composable
private fun OnboardingStepPreview(step: PlaygroundOnboardingStep, checkInFailsOnce: Boolean, onBack: () -> Unit, advance: (String) -> Unit) {
    val scope = object : OnboardingStepScope {
        override val index = step.ordinal
        override val count = PlaygroundOnboardingStep.entries.size
        override val isFirst = step.ordinal == 0
        override val isLast = step.ordinal == PlaygroundOnboardingStep.entries.lastIndex
        override val progress = OnboardingProgress(current = step.ordinal, total = PlaygroundOnboardingStep.entries.size)
        override val onBack: (() -> Unit)? = onBack
        override fun advance() = advance("continue")
        override fun skip() = advance("skip")
    }
    var document by rememberSaveable { mutableStateOf<String?>(null) }
    var connected by rememberSaveable { mutableStateOf(listOf("Gmail")) }
    when (step) {
        // Auth is unresolved for the playground: both providers advance without signing in.
        PlaygroundOnboardingStep.SignIn -> OnboardingSignInScreen(
            state = SignInState.New,
            onContinue = { advance("continue") },
            onUseDifferentAccount = {},
            onUseGoogle = { advance("continue") },
        )
        PlaygroundOnboardingStep.Consent -> consentStep(
            onAccept = { advance("continue") },
            onOpenTerms = { document = "Terms of Service" },
            onOpenPrivacy = { document = "Privacy Policy" },
        ).content(scope)
        PlaygroundOnboardingStep.Connectors -> {
            val catalog = listOf(
                Triple(Icons.Filled.Email, Color(0xFFEA4335), "Gmail"),
                Triple(Icons.Filled.DateRange, Color(0xFF1A73E8), "Google Calendar"),
                Triple(Icons.Filled.Notifications, Color(0xFF6B4FBB), "Slack"),
            )
            OnboardingConnectorsScreen(
                connectors = catalog.map { (icon, tint, name) ->
                    val isConnected = name in connected
                    Connector(icon, tint, name, if (isConnected) "Connected" else "Not connected", isConnected) {
                        connected = if (isConnected) connected - name else connected + name
                    }
                },
                onContinue = { advance("continue") },
                onSkip = { advance("skip") },
                showSeeMore = false,
            )
        }
        PlaygroundOnboardingStep.CheckIn -> CheckInStepPreview(
            failFirstSave = checkInFailsOnce,
            progress = scope.progress,
            onBack = onBack,
            onContinue = { advance("continue") },
        )
        PlaygroundOnboardingStep.Voice -> OnboardingVoiceScreen(
            onBack = onBack,
            onContinue = { advance("continue") },
            onSkip = { advance("skip") },
        )
    }
    val open = document
    if (open != null) {
        // The sheet body reuses the row's own description; real legal copy belongs to the shipping app.
        val summary = if (open == "Terms of Service") "How Rem accounts, subscriptions, and approved actions work."
            else "What Rem, your gateway, and AI or voice providers process."
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { document = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary)) {
                LegalDocumentScreen(title = open, sections = listOf(LegalSection(open, summary)), onClose = { document = null })
            }
        }
    }
}

/** The flow's completion state. Done returns to the onboarding hub. */
@Composable
private fun OnboardingCompletePreview(lastAction: String, onDone: () -> Unit) {
    BackHandler { onDone() }
    Column(Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
        Text(
            "Onboarding complete",
            style = RemTypography.title3Bold,
            modifier = Modifier.testTag("onboarding.complete").semantics { stateDescription = lastAction; liveRegion = LiveRegionMode.Polite },
        )
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().testTag("onboarding.done")) { Text("Done") }
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
            AgentSettingsDestination.Connectors -> SettingsConnectorsScreen(onBack = onDestinationBack)
            AgentSettingsDestination.Memory -> SettingsMemoryScreen(onBack = onDestinationBack)
            AgentSettingsDestination.Models -> SettingsModelsScreen(onBack = onDestinationBack)
            AgentSettingsDestination.CloudBrowser -> SettingsCloudBrowserScreen(onBack = onDestinationBack)
            AgentSettingsDestination.Wallet -> SettingsWalletScreen(onBack = onDestinationBack)
            AgentSettingsDestination.Voice -> SettingsVoiceScreen(onBack = onDestinationBack)
            else -> Column(Modifier.verticalScroll(rememberScrollState())) {
                // Announces the resolved load once, politely (the skeleton's own live region is gone).
                Box(Modifier.size(1.dp).semantics { contentDescription = "Agent settings loaded"; liveRegion = LiveRegionMode.Polite })
                AgentSettingsContent(availableDestinations = setOf(AgentSettingsDestination.PairedDevices, AgentSettingsDestination.Connectors,
                    AgentSettingsDestination.CloudBrowser, AgentSettingsDestination.Memory, AgentSettingsDestination.Models,
                    AgentSettingsDestination.Wallet, AgentSettingsDestination.Voice), openDestination = onOpenDestination)
            }
        }
    }
    else if (status == "loading") Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SkeletonList(label = "Loading agent settings", rows = 7, modifier = Modifier.testTag("agentSettings.skeleton"))
        TextButton(onClick = onCancel, modifier = Modifier.testTag("cancelLoad")) { Text("Cancel") }
    }
    else Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)) {
        Text(RemMaterialSymbols.ErrorNotice.glyph, fontFamily = RemMaterialSymbols.family(RemMaterialSymbols.ErrorNotice), fontSize = 34.sp, color = RemColors.current.systemOrange, modifier = Modifier.clearAndSetSemantics {})
        Text("Couldn’t load agent settings", style = RemTypography.title3Bold, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        Button(onClick = { attempt += 1 }, modifier = Modifier.testTag("retry")) { Text("Retry") }
        TextButton(onClick = onCancel, modifier = Modifier.testTag("cancelLoad")) { Text("Cancel") }
    }
}

@Composable
private fun ControlsPreview() {
    var enabled by rememberSaveable { mutableStateOf(true) }
    var name by rememberSaveable { mutableStateOf("Avery Diaz") }
    var draft by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf(false) }
    var level by rememberSaveable { mutableFloatStateOf(0.5f) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Notifications", Modifier.weight(1f)); Switch(enabled, onCheckedChange = { enabled = it }, modifier = Modifier.testTag("notifications").semantics { contentDescription = "Notifications" })
        }
        OutlinedButton(onClick = { draft = name; editing = true }, modifier = Modifier.testTag("editName")) { Text("Display name: $name") }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ContainedIconSize.entries.forEach { size -> ContainedIcon(RemMaterialSymbols.Info, fill = ContainedIconFill.Tint(RemColors.current.systemBlue), size = size) }
        }
        Button(onClick = {}, enabled = false) { Text("Disabled") }
        Text("Slider", style = RemTypography.footnote, color = RemColors.current.labelSecondary)
        RemSlider(value = level, onValueChange = { level = it }, modifier = Modifier.testTag("controls.slider"))
        Text("${(level * 100).toInt()}%", style = RemTypography.footnote)
        Text("Pills", style = RemTypography.footnote, color = RemColors.current.labelSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RemPill("3 tasks", kind = RemPillKind.List)
            RemPill("Standup", kind = RemPillKind.Dot(RemColors.current.systemBlue))
            RemPill("Personal")
        }
    }
    if (editing) AlertDialog(onDismissRequest = { editing = false }, title = { Text("Edit name") }, text = {
        OutlinedTextField(draft, onValueChange = { draft = it }, label = { Text("Display name") }, modifier = Modifier.testTag("nameField"))
    }, confirmButton = { TextButton(onClick = { name = draft.trim(); editing = false }, enabled = draft.trim().isNotEmpty()) { Text("Save") } }, dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } })
}
