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
     badge at the corner; `additionalReactions` (additive, default empty) stack behind it, each **20**
     (≈ 60% of a badge) further toward the bubble's interior. At most **3** badges are drawn.
   - **Accessibility:** one element for the set, id `<prefix>.reaction`, labelled "Reaction: Heart" for
     one and "Reactions: Thumbs up, Heart" for several (every reaction is named, including any past
     the three drawn).
   - Renders: `MessageReactionTapback-light` / `-dark` (single and stacked, both sides, the cap).
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
   + caption label). It reads **"Auto"** while **Automatic** is selected, otherwise the model name. An id
   the catalog does not contain is shown as-is. While sending, the trigger is disabled at 45% and
   Speak is hidden.
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
   64 avatar (`backgroundSecondary`) with the 48 Rem face in brand blue. Beneath it, the
   identity pill shows the name (title3 semibold) and a status row: a 6pt dot (green when Connected,
   orange when Needs you), the **agent's current activity** in footnote `labelSecondary`, and a chevron.
   The status label is the agent's lifecycle copy supplied by the host, not transport evidence.

## Paired native fixtures

`ChatFixture` in `tools/playground-ios/App/CatalogPages.swift` and in
`compose/demo/.../CatalogPages.kt` hold identical neutral copy, ids and placeholder providers. Journey
tests: `testCatalogChatReactionsDeliveryModelMenuAndAttachments` (XCUITest) /
`catalogChatReactionsDeliveryModelMenuAndAttachments` (instrumentation). Render pairs:
`ChatMessageStates-narrow-light`, `ChatComposerAuto-light`, `AddToChatSheet-light`, `ChatHeader-light`,
`MessageReactionPicker-light`, `MessageReactionTapback-light`, `MessageReactionTapback-dark`,
`MessageActionSheet-assistant-light`, `MessageActionSheet-own-light`.

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
