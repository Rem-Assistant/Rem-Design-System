# Contract — Chat components (bounded slice)

**Outcome:** iOS (SwiftUI) and Android (Compose) render the Chat message, reaction, composer, model menu,
Add to Chat and header components with the same arrangement and behaviour, from the same data, and the
Playground demonstrates them using local fixture state only. **Mode:** Extend (delivery, reaction and
model-menu states on existing components) + Systemize (reaction badge/picker, model menu, Add to Chat,
header as reusable components).

**Authority:** the Figma masters in `af4yDqCzp57jds9lkFiIaO`: Outgoing `2000:3605`, Reaction badge
`2654:20860`, Composer `2071:11555`, Add to Chat `2656:21214`, Thinking menu `2660:128572`, Model menu
`2656:128164`, Provider submenu `2656:128245`, Header `2054:19725`; narrow / incoming acceptance
`2659:21942`; bounded prototype entry `2658:21368`; long-press sheet `2603:19498`. The shipped app's
picker and sheet behaviour (`SharedRemChatView` model menu and `addToChatSheet`) is the behavioural
reference.

## Scope boundary — what this slice does NOT claim

These stay explicit boundaries. No component here persists, transmits or enforces anything:

- **No durable reactions.** A reaction is host state. The Playground keeps it in page-local state.
- **No runtime model catalog.** Providers and models are supplied by the host. The Playground fixture
  lists fictional placeholders ("Provider A", "Model A1"). The design system ships no catalog.
- **No Manage Models destination.** The menu row calls the host back. The Playground shows a note; the
  app route (Settings → Models) is not wired from Chat in this slice.
- **No real native picker results.** The Playground presents the system photo picker (images, at most 4)
  and image-only document picker, and uses only the *count* of what was picked to show chips. It does
  not read or send content.
- **No new camera flow.** iOS shows Camera only when the device has a camera; tapping it in the
  Playground shows a note. Android shows no Camera tile, because the shipped app has no Android camera flow.
- **No backend, server persistence or delivery claims.** Delivered / Read / Not delivered are display
  states supplied by the host. Read must only be shown after an explicit acknowledgement.
- **No always-allow grant.** Permission or Secure Store visuals, where they appear, are fixtures only.
  No persistent grant exists.
- Out of scope: app adoption, Inbox redesign. The long-press action rows (Reply / Mark as unread / Copy /
  Select Text / Report) are presentation-only actions the host performs; see
  [`chat-adapter.md`](chat-adapter.md) rule 7.

## Layout and geometry — responsive, never a fixed width

`MessageBubbleGeometry` (identical constants in Swift and Kotlin, asserted by `ChatGeometryTests.swift` /
`ChatGeometryTest.kt`):

| Value | Rule |
|---|---|
| Outgoing bubble | Hugs its text up to **320**. If the text is wider than the space available, the bubble **fills** that space and the text wraps. |
| Failed row | Reserves **52** on the right (44 target + 8 gap) for the failure control. |
| Narrow fixture | 320 screen → 288 row → failed bubble **236**, text **204**. |
| Content inset | 16 horizontal, 12 vertical; radius 20 (no 20 radius token exists; accepted Figma value). |
| Incoming | Bubble, width `min(320, row − 30)`; the 30 keeps its upper-right tapback (16 outward) inside the row. |
| Reaction clearance | A reacted message reserves **22** above its bubble (both roles), so the tapback never reaches the row above. |

## States and rules

1. **Reaction — tapback** (independent of delivery). Mirrors the iMessage tapback reference Samuel shared
   (2026-10-11); geometry is `MessageReactionBadgeMetrics` in both platforms, asserted by
   `ChatGeometryTests.swift` / `ChatGeometryTest.kt`:
   - **Badge:** a **34** disc with a centred **19pt** emoji, filled with **solid `backgroundSecondary`**
     (#F2F2F7 light / #1C1C1E dark) inside a **2** ring of **solid `backgroundPrimary`** (page
     background) that cuts it out of the bubble it overlaps. Both fills are opaque — no translucency
     or material — so the bubble never shows through (Samuel, 2026-10-11).
   - **Placement:** overlapping the upper corner toward the conversation centre — **outgoing
     top-leading, incoming top-trailing**. The disc extends **14** outward past the bubble's side and
     rises **20** above its top, so 20 × 14 of it overlaps the bubble. The whole set (ring included)
     reaches 16 outward and 22 up; the message reserves that 22 above itself.
   - **Thought-bubble tail:** two solid dots, **8** and **4**, with the same fill and ring, trailing
     below-outward of the front badge (centres at (8, 34) and (3, 41) in the outgoing badge disc's
     coordinates; mirrored for incoming).
   - **Stacking:** `ChatMessageDisplay.reaction` (the person's own, the sheet's selection) is the front
     badge at the corner; `additionalReactions` (additive, default empty) stack behind it, **30**
     further toward the bubble's interior: the discs overlap by 4 so it reads as a stack, and the
     back badge's emoji stays fully visible. At most **2** badges are drawn, because a chat has two
     participants, the person and Rem (Samuel, 2026-10-11).
   - **Accessibility:** one element for the set, id `<prefix>.reaction`, labelled "Reaction: Heart" for
     one and "Reactions: Thumbs up, Heart" for several (every reaction is named, including any past
     the two drawn).
   - Renders: `MessageReactionTapback-light` / `-dark` (single and stacked, both sides, the two-badge cap).
2. **Long press** on a transcript message (accessibility "Message actions") asks the host to present
   **`MessageActionSheet`** (`2603:19498`): a 2 × 6 reaction grid — the approved row `👍 👎 ❤️ 😂 🎉 😮`,
   then `🔥 👀 🙏 😢 💯` and a brand-blue `+` — 44 circles, `fillTertiary`, 27pt emoji, rows 12 apart;
   then inset-grouped `RemSection`s of `ListRow`s (29 leading symbol, 60 min height, divider inset 56):
   [Reply, Mark as unread], [Copy, Select Text], [Report] for an assistant message; [Reply],
   [Copy, Select Text] for the person's own message (never Report). Choosing the current reaction again
   clears it. The choices are data (`sheetChoices`, `standardChoices`); hosts may pass others. A bare
   `MessageBubble(onLongPress:)` still offers the single six-choice `MessageReactionPicker` row
   (accessibility "React"), as in the component catalog.
3. **Failure:** an outlined `exclamationmark.circle` / `ErrorOutline` in `systemRed`, **entirely outside
   the bubble on the right**, bottom-aligned. It opens a **Try again** menu. Below the bubble, gap 8:
   **"Not delivered"** in footnote semibold `systemRed`, right-aligned to the bubble's edge, with
   **no timestamp**.
4. **Delivered / Read:** "Delivered · {at}" / "Read · {at}" in footnote semibold `labelSecondary`,
   right-aligned, gap 4. `at` is host-formatted; Read keeps the same delivered time.
5. **Composer Auto:** a secondary-pill trigger (`fillTertiary` capsule, 8/4 padding, `chevron.up.chevron.down`
   + caption label). It reads **"Auto"** while **Automatic** is selected. For a model it reads, in order:
   the model's name from the host's catalog; otherwise the host's own label (`ChatComposerState.modelLabel`,
   or `fallbackLabel` passed to `ChatModelMenu`) unless it is blank or the automatic label; otherwise the
   raw id (`ChatModelMenu.triggerLabel` / `chatModelTriggerLabel`). While sending, the trigger is disabled
   at 45% and Speak is hidden.
6. **Model menu:** Automatic (checkmark when selected) → one submenu per provider with models
   (checkmark on selection) → divider → Manage Models (only when the host supplies it). Providers
   with no models are omitted. Android has no nested native menu, so a provider swaps the menu to its
   models with a back row; the information architecture is the same.
7. **Attachments:** removable chips (title + blue ×). A **Cloud browser** chip is a capability for the
   next message, never a browser launch.
8. **Add to Chat:** title + Done; Camera (conditional) / Photos / Files tiles (`fillTertiary`, radius
   12, min height 78); Cloud browser row when `browserAvailable` (adds the chip and dismisses);
   Thinking row → Off / Low / Medium / High menu.
9. **Header** (corrected 2026-10-10, handoff notes): the header **owns back and overflow** (44pt circular
   controls level with the avatar, stretched to the screen width so overflow stays visible at 320pt) and is
   used unchanged in ordinary and task chat. No navigation-title row above it; no face in empty content.
   On a **root conversation** (`showsSidebar`, Build 9 shell, Figma `2702:2351`) the one leading control
   is the sidebar button (`line.3.horizontal` / `Menu`, "Open sidebar") instead of Back.
   64 avatar (`backgroundSecondary`) with the 48 Rem face in brand blue. Beneath it, the
   identity pill shows the name (title3 semibold) and a status row: a 6pt dot (green when Connected,
   orange when Needs you), the **agent's current activity** in footnote `labelSecondary`, and a chevron.
   The status label is the agent's lifecycle copy supplied by the host, not transport evidence.
   **Capsule and control surfaces (noted decision, review of #106 P3-9):** iOS draws the identity capsule
   and the 44pt controls on `.regularMaterial`; Compose draws them on opaque `backgroundPrimary` with a
   shadow (Android has no system material). Both are intended. The iOS layer-render harness cannot
   capture materials, so the capsule looks almost invisible in the iOS `ChatHeader-light` render; judge
   it on device or simulator screenshots, not that render.

10. **Jump to latest** (`ChatJumpToLatest`, Swift and Kotlin share the constants; reference: the Claude iOS
   app). The full composition shows a round down-arrow button **centred horizontally just above the docked
   composer** while the person has scrolled away from the newest message; tapping it scrolls (animated) to
   the latest message, and it disappears at or near the bottom. The composition owns it — hosts wire nothing.
   - **Visibility:** shown when the transcript's bottom edge is more than **200pt/dp** below the visible
     area (`threshold`), hidden at or below it (including overscroll). A fixed distance (about four short
     rows) rather than a third of the viewport, so the rule reads the same with the keyboard open or closed.
     Never shown in the empty state.
   - **Look:** 44 glass circle (Samuel, 2026-10-11): iOS / macOS 26 system Liquid Glass
     (`.glassEffect(.regular.interactive())`), earlier `.regularMaterial`; Compose blurs the transcript behind it
     inside the composition on API 32+ (Haze, `backgroundTertiary` tint at 0.6 — `ChatJumpToLatest.GLASS_TINT_ALPHA`)
     and is solid `backgroundTertiary` below API 32, with high contrast, or outside the composition (catalog).
     0.5 hairline `separator`, soft shadow; `arrow.down` / `ArrowDownward` in `labelPrimary`
     (17pt semibold SF Symbol; 20dp Material icon). 12 (`Spacing.md`) above the top of the dock chrome.
     It overlays the transcript area only, so it never covers the composer, the reply accessory or the
     keyboard-docked composer. Large text: fixed icon-only chrome; iOS offers the Large Content Viewer.
   - **Motion:** fade + scale from 0.8 over 0.2s; with Reduce Motion (iOS) or animator scale 0 (Android) it
     appears, disappears and jumps without animation.
   - **Accessibility:** a button labelled "Scroll to latest message", identifier / test tag
     `chat.jumpToLatest`; absent from the accessibility tree while hidden.
   - **New messages while scrolled up:** the person's position is kept and the button stays. At or near
     the bottom the transcript still follows new content (iOS `defaultScrollAnchor(.bottom)`, unchanged;
     Compose follows only while within the threshold, where it previously always snapped to the bottom).
     No unread indicator yet (follow-up).
   - **Restored transcript** (configuration change, or a host's saved state while the screen was away): the
     position is restored **relative to the bottom**, because the layout may differ (rotation changes the
     viewport and so the scroll range). Saved at or near the latest message (within the threshold) it re-pins
     to the latest; saved further up it keeps the same distance from the bottom. Compose saves that distance
     (dp) at save time (`ChatJumpToLatest.restoredScroll`; `ChatScreenModelTest`,
     `ChatScreenRestorationTest`); iOS keeps `defaultScrollAnchor(.bottom)` across size changes.

11. **Swipe down to dismiss the keyboard** (`ChatKeyboardDismiss`, Swift and Kotlin share the constants and
   the rule; Samuel, 2026-10-11). While the composer field is focused, a clear downward swipe that **starts on
   the composer** (the pill, field included) ends editing: focus becomes false and the keyboard / IME hides.
   The composer owns it — hosts wire nothing; host-driven composers hear `focusChanged(false)`.
   - **Rule:** at least **24pt/dp** down (`threshold`) with vertical travel more than **1.5×** horizontal
     (`verticalDominance`), decided on release. Upward, sideways or short drags do nothing. A drag that only
     starts moving after the finger was held still for **0.35s** (`holdLimit`) is text selection or a caret
     drag and never dismisses.
   - **Never breaks:** taps, cursor placement, selection, Send / Stop / Add / Speak, the model menu, the
     attachment chips or the reply accessory (the gesture covers the composer pill only). iOS attaches a
     simultaneous `DragGesture` (behind a `LongPressGesture` that wins on a hold) only while focused;
     Compose observes on the Final pointer pass, consumes nothing, and abandons the swipe when a child
     consumed any part of it (a click, a selection drag, an overflowed field's scroll).
   - **Transcript:** iOS keeps `.scrollDismissesKeyboard(.interactively)`. Compose matches it: a user drag
     toward older messages of at least the threshold while the IME is open clears focus and hides the IME
     (a `NestedScrollConnection` that consumes nothing, so scrolling and jump to latest are unchanged;
     programmatic scrolls never dismiss).
   - **Accessibility:** VoiceOver and TalkBack keep the keyboard's own dismiss; the focused field also offers
     a **"Dismiss keyboard"** action (iOS `accessibilityActions`, Compose `CustomAccessibilityAction`). No
     focus trap: nothing moves or holds accessibility focus.

12. **Edge backdrops — the transcript scrolls under the header and the composer dock** (`ChatEdgeBackdrop`,
   internal, Swift and Kotlin share the constants; reference: iOS bars and the Claude iOS app). The full composition draws the transcript under the status bar and the header, and under the
   composer dock, the home indicator / navigation bar and (Compose) the IME, so messages scroll behind both
   instead of clipping at solid bands. The composition owns it — hosts wire nothing.
   - **Geometry:** the transcript's top content inset equals the **chrome height** (status bar + header,
     including the header's 8 bottom gap) and its bottom inset equals the **dock height** (voice bar, reply
     accessory and composer with their 8 padding, plus the home indicator / navigation bar, or measured above
     the keyboard on iOS and including the IME on Compose). At rest the first and the latest message sit wholly
     clear of the chrome and the fades. `defaultScrollAnchor(.bottom)`, short-conversation bottom alignment,
     `scrollDismissesKeyboard(.interactively)` and the keyboard / IME docking are unchanged.
   - **Jump to latest** (rule 10) keeps its 12 gap above the dock's top edge (bottom padding = dock height +
     12), and its distance counts the bottom inset, so it is 0 — hidden — at rest at the latest message.
   - **iOS backdrops:** progressive blurs — `.regularMaterial` masked by vertical gradients. Header: opacity
     **1** over the status bar and header, fading to **0** over **20pt** that ends **12pt** below the header.
     Dock: **0** from **12pt** above the dock's top edge, reaching **1** 8pt below it (about 20pt above the
     composer pill), opaque to the screen edge. The dock itself no longer paints `backgroundPrimary`. Header
     controls, the identity pill and the composer pill keep their own materials / fills.
   - **Compose backdrops:** backdrop blurs of the transcript on **API 32+** through
     [Haze](https://github.com/chrisbanes/haze) 1.1.1 (Apache-2.0; `RenderEffect` on a `GraphicsLayer` copy of the
     transcript): blur radius 20dp, `backgroundPrimary` tint at 0.7, under the same masks as iOS. Below API 32
     Haze draws the masks as `backgroundPrimary` scrims at **0.85** alpha at the screen edge to **0**. (Haze turns
     blur on from API 32: API 31's `RenderEffect` path is unreliable.)
   - **Reduce Transparency (iOS):** each edge is solid `backgroundPrimary` with a 0.5 `separator` hairline on
     its inner edge (under the header, on top of the dock), no fade. **Android** has no reduce-transparency
     setting; the same fallback follows the system contrast level above standard (Android 14+
     `UiModeManager.contrast`); below Android 14 the scrims always show.
   - **Accessibility:** the backdrops are decoration (hidden, not interactive; touches reach the transcript or
     the chrome above them); the header is still read first.

13. **Transcript gestures — scrolling always wins.** A vertical drag that starts anywhere in
   `ChatTranscriptList` — on a bubble, beside one (within touch slop), on a timestamp row or in the gutter —
   is the scroll view's own scroll.
   - **Time peek** (`ChatTimestampReveal`, chat-adapter `time`): only a predominantly horizontal **leftward**
     drag (horizontal more than **1.5×** vertical, at least 10pt before rows move) starts it; rows snap back on
     end, cancel or failure; Reduce Motion clamps the travel at the column width and snaps back without
     animation.
   - **Long press:** a 0.5s hold that moves under 10pt asks for message actions (rule 2); travel past 10pt
     fails it. Neither gesture ever claims the touch from the scroll.
   - **iOS:** the peek is a `UIPanGestureRecognizer` added to the transcript's hosting `UIScrollView`
     (`ChatTimestampRevealPan`, found by a non-interactive probe). It begins only for leftward-horizontal
     motion (velocity, else translation), recognizes simultaneously, and cancels or delays no touches. It is
     never a SwiftUI `DragGesture` on the rows: on iOS 17 that kept the scroll view's pan from starting for
     touches on or near a row (run 38181697796: a slow 300pt drag from the centre of `message.a1.bubble`
     moved it 0pt; earlier runs, 4pt beside an incoming bubble). A `ChatTranscriptList` outside any scroll
     view has no peek; the times stay spoken through the bubble. macOS keeps the simultaneous `DragGesture`.
   - **Compose:** unchanged; its pointer input already passes vertical scrolls.
   - Journeys: the bubble-drag pair under Jump to latest below, and `testChatSwipeLeftPeeksTimesAndSnapsBack` /
     `chatSwipeLeftPeeksTimesAndSnapsBack`; long press: `testChatScreenLongPressMessageActions` /
     `chatScreenLongPressMessageActions`.

## Paired native fixtures

`ChatFixture` in `tools/playground-ios/App/CatalogPages.swift` and in
`compose/demo/.../CatalogPages.kt` hold identical neutral copy, ids and placeholder providers. Journey
tests: `testCatalogChatReactionsDeliveryModelMenuAndAttachments` (XCUITest) /
`catalogChatReactionsDeliveryModelMenuAndAttachments` (instrumentation). Render pairs:
`ChatMessageStates-narrow-light`, `ChatComposerAuto-light`, `AddToChatSheet-light`, `ChatHeader-light`,
`MessageReactionPicker-light`, `MessageReactionTapback-light`, `MessageReactionTapback-dark`,
`MessageActionSheet-assistant-light`, `MessageActionSheet-own-light`.

Jump to latest: the root Chat fixture picker's **Long** conversation (`ChatPlaygroundFixture` `.long` /
`Conversation.Long`, earlier history above the populated sample). Journeys:
`testChatJumpToLatestAppearsWhenScrolledUpAndReturns` (XCUITest) /
`chatJumpToLatestAppearsWhenScrolledUpAndReturns` (instrumentation). While a reply is in flight (Stop shown, the
shell's simulated reply held): `testShellScrollsWhileAReplyIsInFlight` / `shellScrollsWhileAReplyIsInFlight` —
the transcript still scrolls, jump to latest appears and the message just sent leaves view. Their drags start in
the leading gutter clear of every row (iOS `clearGutterStart`); a vertical drag that starts on a transcript bubble,
or beside one, must scroll as well (rule 13). Android journeys: `chatTranscriptDragThatStartsOnABubbleScrolls`
(centre of incoming `a1`) and `chatTranscriptDragBesideABubbleScrolls` (4dp outside its leading edge). On iOS this
is a **device check**: the Simulator does not scroll this transcript for a synthesized drag that starts on a bubble
(a gutter drag does; run 38199510600, with a scroll-disabled negative control), while a device does. Render pair: `ChatJumpToLatest-light`,
`ChatJumpToLatest-dark` (opened at the oldest message through an internal evidence seam, so a still render
shows the scrolled-up state).

Swipe down to dismiss the keyboard: journeys `testChatComposerSwipeDownDismissesKeyboard` (XCUITest) /
`chatComposerSwipeDownDismissesKeyboard` (instrumentation; also covers the transcript drag). No render pair: the
behaviour is a gesture, covered by the paired rule tests and the journeys, not by screenshots.

Edge backdrops: render pair `ChatEdgeBackdrop-light`, `ChatEdgeBackdrop-dark` — the Long conversation opened
96pt/dp below its oldest message through internal evidence seams (`chatEdgeBackdropEvidence` /
`LocalChatEdgeBackdropEvidence`, with the jump-to-latest seam), so messages sit under the header and under the
composer dock. Geometry rules: `testEdgeBackdrop…` (`ChatScreenModelTests.swift`) / `edgeBackdrop…`
(`ChatScreenModelTest.kt`).

## Chat cards

The six in-transcript cards (MessageDraftCard, PollCard, ActionReceipt, ConnectorCard, LoginCard,
PermissionCard) have their own contract: [`chat-cards.md`](chat-cards.md).

## Full-screen composition and app boundary

See [`chat-adapter.md`](chat-adapter.md): typed inputs and actions, the latest-outgoing receipt rule, the reply
context accessory (`2682:22298`), the empty state (`2054:22089`) and the presentation-only Playground fixtures.

## Known differences from Figma (reported, not hidden)

- The composer's Sending (progress, `2071:11453`) and Streaming (Stop, `2071:11485`) variants were not in the
  supplied exports; both render the red Stop and emit `.cancel`. Read-only (`2071:11520`) is not implemented.
- In the 2026-10-10 exports the Speak pill reads grey, while code keeps the brand-blue Speak pill. Not changed
  without the variant spec; flagged for review.
- Reply-context source-master sizing polish was cancelled in Figma; code is content-sized with a 44pt dismiss
  target and fills the dock (370 × 58 at 402pt in the task specimen).
- The default fixture's "Today 3:25 PM" vs "Delivered · 10:24" mismatch is sample data and is not reproduced.
- The Inbox status pill (`AgentStatusPill`) and row separators are not in the supplied exports and are pending
  review.
- Chat slice glyph pairs and their pinned FILL are recorded as **open rows** in
  [`icon-registry.md`](icon-registry.md#chat-slice--open-rows-recorded-as-implemented-not-yet-graduated)
  (Android uses the Material Icons vector fallback until the Symbols subset carries them).

- The long-press sheet reference has a 34pt top inset that includes its grabber. Code relies on the platform
  sheet's grabber / drag handle and adds 24pt (iOS) or 8pt (Android, whose drag handle carries its own
  padding) above the grid; flagged for review against device evidence. iOS Copy uses `doc.on.doc`, the
  iOS 17 name of the reference's `document.on.document`.
- The Figma composer has separate Sending (progress) and Streaming (Stop) states. Code keeps the
  existing `SendState` (`.sending` = red Stop) and adopts the Figma rules for disabled Auto and hidden
  Speak.
- The Figma **Reaction badge** master (`2654:20860`) still shows the earlier 28 `fillTertiary` chip. Code
  follows the 2026-10-11 tapback direction (rule 1); the Figma master has not been updated.
- The Figma Code Connect property names could not be read (the context API needs a Dev seat). The
  templates therefore emit the real call statically and bind no properties.

## Parity acceptance

- The same geometry constants on both platforms, and both unit suites pass.
- The outgoing reaction is left of and above the bubble's origin; the incoming reaction is right of and
  above the bubble's top-trailing corner. The failure control's left edge is at
  or past the bubble's right edge. "Not delivered" is right-aligned to the bubble.
- Auto ↔ model-name round trip through the menu. The Cloud browser chip can be added and removed.
  The Thinking value changes. Done dismisses.
- No network, persistence or permission prompt in any Playground Chat interaction.
