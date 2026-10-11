package com.rem.designsystem.screens

import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import com.rem.designsystem.chat.MessageBubbleCenterLine
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rem.designsystem.brand.RemFaceMarkMode
import com.rem.designsystem.chat.ChatHeader
import com.rem.designsystem.chat.ChatReplyContextAccessory
import com.rem.designsystem.tokens.RemTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rem.designsystem.chat.ComposerSendState
import com.rem.designsystem.chat.MessageBubble
import com.rem.designsystem.chat.MessageRole
import com.rem.designsystem.chat.RemComposerBar
import com.rem.designsystem.chat.VoiceBar
import com.rem.designsystem.chat.VoiceBarState
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme

/**
 * **ChatScreen** — Compose sibling of the SwiftUI [ChatScreen]. A scrolling transcript of
 * [MessageBubble]s, an optional [VoiceBar] above the input, and a [RemComposerBar] pinned at the
 * bottom, supplied by the host through [transcript]. Platform chrome comes from the OS. Authority:
 * Chat screen (Figma `71:533`, composer `527:2`) + `SharedRemChatView.swift`.
 */
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    composerText: String = "",
    composerPlaceholder: String = "Ask anything",
    composerState: ComposerSendState = ComposerSendState.Idle,
    voiceBar: VoiceBarState? = null,
    transcript: @Composable ColumnScope.() -> Unit,
) {
    ChatScreenLayout(modifier, voiceBar, transcript) {
        RemComposerBar(text = composerText, placeholder = composerPlaceholder, state = composerState)
    }
}

/**
 * **Full-screen composition** — twin of SwiftUI `ChatScreen(header:composer:…onAction:transcript:)`.
 * Figma shell `2054:21958` / board `2681:21977`: the shared header (`2054:19725`, which owns back and
 * overflow — hosts add no app bar), the Screen state slot (scrolling transcript, or the empty state
 * `2054:22089` with no second face), and bottom chrome docking the one canonical composer (`2071:11555`)
 * above the IME or navigation bar with the optional task-reply accessory (`2682:22298`). Insets come
 * from the host window (`imePadding` / `navigationBarsPadding` / `statusBarsPadding`); the 320dp board
 * is a width stress fixture only. [transcript] is the rich-content slot. The composition also owns
 * **jump to latest** ([ChatJumpToLatest]): a round down-arrow centred above the dock while the person is
 * scrolled more than [ChatJumpToLatest.THRESHOLD] above the newest message. Hosts wire nothing for it.
 */
@Composable
fun ChatScreen(
    header: ChatHeaderDisplay,
    composer: ChatComposerState,
    onAction: (ChatScreenAction) -> Unit,
    modifier: Modifier = Modifier,
    replyContext: ChatReplyContext? = null,
    emptyState: ChatEmptyState? = null,
    voiceBar: VoiceBarState? = null,
    modelMenu: (@Composable (enabled: Boolean) -> Unit)? = null,
    transcript: @Composable ColumnScope.() -> Unit,
) {
    val colors = RemColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundPrimary)
            .statusBarsPadding(),
    ) {
        ChatHeader(
            activity = header.activity,
            name = header.name,
            status = header.status,
            faceMode = if (header.isWorking) RemFaceMarkMode.Thinking else RemFaceMarkMode.Idle,
            accessibilityPrefix = "chat.header",
            onTap = if (header.showsActivityDetails) ({ onAction(ChatScreenAction.ActivityDetails) }) else null,
            onBack = if (header.showsBack) ({ onAction(ChatScreenAction.Back) }) else null,
            onOverflow = if (header.showsOverflow) ({ onAction(ChatScreenAction.Overflow) }) else null,
            onCall = if (header.showsCall) ({ onAction(ChatScreenAction.Call) }) else null,
            modifier = Modifier.padding(bottom = RemSpacing.sm),
        )
        val scroll = rememberScrollState()
        val density = LocalDensity.current
        val context = LocalContext.current
        val reduceMotion = remember(context) {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }
        val jumpEvidence = LocalChatJumpToLatestEvidence.current
        // Previews and screenshot renders (inspection mode) show the settled state, without motion.
        val jumpMotion = !reduceMotion && !LocalInspectionMode.current
        // Jump to latest: how far (dp) the transcript's bottom edge sits below the visible area. Before the
        // first layout `maxValue` is Int.MAX_VALUE (unmeasured), which must not flash the button.
        val showsJump by remember(emptyState == null) {
            derivedStateOf {
                emptyState == null && if (scroll.maxValue == Int.MAX_VALUE) jumpEvidence
                else ChatJumpToLatest.isVisible((scroll.maxValue - scroll.value) / density.density)
            }
        }
        val jumpScope = rememberCoroutineScope()
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = RemSpacing.lg, vertical = RemSpacing.md)
                    .testTag("chat.content"),
                // A short transcript sits just above the composer (Swift: minHeight = viewport, .bottom
                // alignment). fillMaxSize + verticalScroll keep the viewport as the minimum height, so Bottom
                // arrangement pins the rows to the dock; the empty state stays at the top.
                verticalArrangement = Arrangement.spacedBy(RemSpacing.lg, if (emptyState == null) Alignment.Bottom else Alignment.Top),
            ) {
                if (emptyState != null) {
                    ChatEmptyStateView(emptyState, onStarter = { onAction(ChatScreenAction.Starter(it)) })
                } else {
                    transcript()
                }
            }
            // Over the transcript area only, so it never covers the docked composer or the IME. Qualified so
            // the outer Column's scope does not pick the ColumnScope overload.
            androidx.compose.animation.AnimatedVisibility(
                visible = showsJump,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = ChatJumpToLatest.BOTTOM_SPACING.dp),
                enter = if (!jumpMotion) EnterTransition.None
                else fadeIn(tween(ChatJumpToLatest.APPEAR_MILLIS)) + scaleIn(tween(ChatJumpToLatest.APPEAR_MILLIS), initialScale = ChatJumpToLatest.APPEAR_SCALE),
                exit = if (!jumpMotion) ExitTransition.None
                else fadeOut(tween(ChatJumpToLatest.APPEAR_MILLIS)) + scaleOut(tween(ChatJumpToLatest.APPEAR_MILLIS), targetScale = ChatJumpToLatest.APPEAR_SCALE),
            ) {
                ChatJumpToLatestButton(onClick = {
                    jumpScope.launch {
                        if (jumpMotion) scroll.animateScrollTo(scroll.maxValue) else scroll.scrollTo(scroll.maxValue)
                    }
                })
            }
        }
        // Keep the newest content in view (Swift: defaultScrollAnchor(.bottom)) while the person is at or
        // near the latest message. Scrolled further up, their position is kept and jump to latest stays.
        if (emptyState == null && !jumpEvidence) {
            val followedMax = remember { intArrayOf(0) }
            LaunchedEffect(scroll.maxValue) {
                val nearLatest = !ChatJumpToLatest.isVisible((followedMax[0] - scroll.value) / density.density)
                if (nearLatest) scroll.scrollTo(scroll.maxValue)
                followedMax[0] = scroll.maxValue
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.backgroundPrimary)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = RemSpacing.lg, vertical = RemSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
        ) {
            if (voiceBar != null) VoiceBar(state = voiceBar)
            if (replyContext != null) {
                ChatReplyContextAccessory(
                    context = replyContext,
                    accessibilityPrefix = "chat.replyContext",
                    onDismiss = { onAction(ChatScreenAction.DismissReplyContext(replyContext.targetId)) },
                )
            }
            RemComposerBar(
                state = composer,
                onAction = { onAction(ChatScreenAction.Composer(it)) },
                accessibilityPrefix = "chat",
                modelMenu = modelMenu,
            )
        }
    }
}

/**
 * Evidence seam (internal, never set by hosts): open the transcript at its oldest message instead of
 * following the latest, and present jump to latest from the first frame — a single-frame screenshot render
 * cannot wait for the post-layout pass that measures the distance. Use only with a transcript taller than
 * the screen plus [ChatJumpToLatest.THRESHOLD]; the visibility rule itself is covered by unit and
 * instrumentation tests. Not part of the public API.
 */
internal val LocalChatJumpToLatestEvidence = staticCompositionLocalOf { false }

/** The empty conversation (`2054:22089`): title, message and starters as full-width capsules. No face. */
@Composable
fun ChatEmptyStateView(state: ChatEmptyState, onStarter: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = RemColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(top = RemSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        Text(state.title, style = RemTypography.title3.copy(fontWeight = FontWeight.Bold), color = colors.labelPrimary, textAlign = TextAlign.Center)
        Text(state.message, style = RemTypography.subheadline, color = colors.labelSecondary, textAlign = TextAlign.Center)
        state.starters.forEach { starter ->
            Box(
                modifier = Modifier
                    .padding(top = RemSpacing.xs)
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .shadow(12.dp, CircleShape, clip = false)
                    .background(colors.backgroundPrimary, CircleShape)
                    .clickable(role = Role.Button) { onStarter(starter.id) }
                    .testTag("chat.starter.${starter.id}")
                    .padding(horizontal = RemSpacing.lg, vertical = RemSpacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(starter.title, style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold), color = colors.labelPrimary, textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * The canonical transcript for host-supplied entries — twin of SwiftUI `ChatTranscriptList`: centred
 * timestamps, [MessageBubble]s, and [ChatTranscriptRules] receipt placement (latest outgoing only;
 * failures stay visible). Delivery values are rendered as supplied.
 */
/** Right-side swipe-to-reveal time column (fits "10:24 AM" at footnote size). Provisional: to be validated against long localized times and large text; not a specified value. */
private val TimeColumnWidth = 64.dp

@Composable
fun ChatTranscriptList(
    entries: List<ChatTranscriptEntry>,
    onAction: (ChatTranscriptAction) -> Unit,
    modifier: Modifier = Modifier,
    /** Renders each [ChatTranscriptEntry.HostContent] in place; hosts without such entries omit it. */
    hostContent: @Composable (id: String) -> Unit = {},
) {
    val colors = RemColors.current
    val latest = ChatTranscriptRules.latestOutgoingId(entries)
    // Swipe left to reveal timestamps (WS1d): one shared offset moves every row together; the header,
    // composer and keyboard live outside the transcript and stay put. Rows snap back on release.
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val columnPx = with(LocalDensity.current) { TimeColumnWidth.toPx() }
    val revealEvidence = LocalChatTimestampRevealEvidence.current
    val reveal = remember { Animatable(ChatTimestampReveal.SETTLED + revealEvidence.coerceIn(0f, 1f) * columnPx) }
    // RTL mirrors the peek: a rightward drag moves the rows right and the times come in from the left
    // edge. Drags are converted to layout-relative travel (negative = toward the start edge), so
    // ChatTimestampReveal stays direction-free.
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val scope = rememberCoroutineScope()
    var dragX by remember { mutableFloatStateOf(0f) }
    val settle: () -> Unit = {
        scope.launch {
            if (reduceMotion) reveal.snapTo(ChatTimestampReveal.SETTLED)
            else reveal.animateTo(ChatTimestampReveal.SETTLED, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(reduceMotion, columnPx, direction) {
                // Horizontal-axis detector: vertical drags stay with the enclosing scroll.
                detectHorizontalDragGestures(
                    onDragStart = { dragX = 0f },
                    onDragEnd = settle,
                    onDragCancel = settle,
                ) { change, amount ->
                    dragX += amount * direction
                    if (reveal.value > 0f || dragX <= -ChatTimestampReveal.MIN_TRAVEL) {
                        change.consume()
                        scope.launch { reveal.snapTo(ChatTimestampReveal.reveal(dragX, columnPx, reduceMotion)) }
                    }
                }
            }
            // `offset {}` is layout-direction aware: rows move toward the start edge in LTR and RTL alike.
            .offset { IntOffset(-reveal.value.roundToInt(), 0) },
        verticalArrangement = Arrangement.spacedBy(RemSpacing.lg),
    ) {
        // Keyed by entry id (SwiftUI ForEach is keyed by id): remembered per-entry state, such as a host
        // card's expansion, stays with its entry when rows are inserted above it.
        entries.forEach { entry -> key(entry.id) {
            when (entry) {
                is ChatTranscriptEntry.HostContent -> Box(Modifier.fillMaxWidth()) { hostContent(entry.id) }
                is ChatTranscriptEntry.Timestamp -> Text(
                    entry.text, style = RemTypography.footnote, color = colors.labelSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().testTag("chat.timestamp.${entry.id}"),
                )
                is ChatTranscriptEntry.Message -> {
                    val m = entry.message
                    val shown = m.copy(delivery = ChatTranscriptRules.displayedDelivery(m, latest))
                    val time = m.time
                    // The time sits just past the row's trailing edge (off-screen at rest, slides in outside the
                    // bubble), centred on the bubble itself rather than on the bubble plus its receipt.
                    Layout(
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            MessageBubble(message = shown, onAction = onAction)
                            if (time != null) {
                                Text(
                                    time, style = RemTypography.footnote, color = colors.labelSecondary,
                                    textAlign = TextAlign.End, maxLines = 1,
                                    modifier = Modifier
                                        .graphicsLayer { alpha = (reveal.value / columnPx).coerceIn(0f, 1f) }
                                        // Visual only: the bubble speaks the time ("Sent at 10:24") through its
                                        // merged semantics, so TalkBack never needs the gesture.
                                        .clearAndSetSemantics { testTag = "message.${m.id}.time" },
                                )
                            }
                        },
                    ) { measurables, constraints ->
                        val row = measurables[0].measure(constraints)
                        val timeText = measurables.getOrNull(1)?.measure(Constraints.fixedWidth(columnPx.roundToInt()))
                        layout(row.width, row.height) {
                            row.place(0, 0)
                            if (timeText != null) {
                                val centre = row[MessageBubbleCenterLine].takeIf { it != AlignmentLine.Unspecified } ?: (row.height / 2)
                                // Relative placement: past the trailing edge in LTR, past the left edge in RTL.
                                timeText.placeRelative(row.width, centre - timeText.height / 2)
                            }
                        }
                    }
                }
            }
        } }
    }
}

/**
 * Evidence seam (internal, never set by hosts): start the timestamp peek at this fraction (0–1) of the
 * time column, so a single-frame render can show revealed times. Not part of the public API.
 */
internal val LocalChatTimestampRevealEvidence = staticCompositionLocalOf { 0f }

@Composable
private fun ChatScreenLayout(
    modifier: Modifier,
    voiceBar: VoiceBarState?,
    transcript: @Composable ColumnScope.() -> Unit,
    composer: @Composable () -> Unit,
) {
    val colors = RemColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.backgroundPrimary)) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.lg),
        ) {
            transcript()
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = RemSpacing.lg).padding(bottom = RemSpacing.md),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
        ) {
            if (voiceBar != null) {
                VoiceBar(state = voiceBar)
            }
            composer()
        }
    }
}

@Preview(name = "ChatScreen", showBackground = true, widthDp = 402, heightDp = 760)
@Composable
private fun ChatScreenPreview() {
    RemTheme {
        ChatScreen(composerState = ComposerSendState.Idle) {
            MessageBubble("Can you tidy up my inbox before I start my day?", role = MessageRole.User)
            MessageBubble(
                "Done — I archived 38 newsletters and snoozed 5 low-priority threads. " +
                    "Want me to draft replies to the two that still need you?",
                role = MessageRole.Assistant,
            )
            MessageBubble("Yes, go ahead.", role = MessageRole.User)
        }
    }
}
