# Design System — Cleanup Log (deferred)

> **ContainedIcon blank-glyph — FIXED.** `ContainedIcon` (`110:54`) now bakes a centered white SF-Pro
> glyph exposed as a `Symbol` TEXT prop (bg color still per-instance). Settings re-glyphed from
> `SharedSettingsView.swift`: Rem=brain.head.profile, Billing=creditcard.fill, Permissions=hand.raised.fill,
> About=info.circle.fill, Share=square.and.arrow.up, Feedback=envelope.fill, Bug=exclamationmark.triangle.fill.
> Verified. **Connectors re-glyphed** with the app's own SF-Symbol fallbacks
> (`SharedComposioConnectionsView.iconName`): Gmail=envelope.fill, Google Calendar=calendar, Google
> Drive=externaldrive.fill, Slack=number, Notion=list.bullet (approx — app fallback is `note.text`,
> not captioned in the community file). IDEAL: the real app fetches brand logos from
> `logos.composio.dev/api/<toolkit>` (remote SVGs) — embed those for exact fidelity in a later pass.

## Founder review 4 — 2026-09-24 (file organization / auto-layout pass)

Founder: *"A lot of your screens feel disorderly — you're not organizing them in autolayout. Also some
screens are in the wrong page (I saw task edit in the cover page)."*

- [x] **New Task or Event `404:5` was on the Cover page** → moved to **Task & Events** (`356:4`), lined up
  in the screen row after Task detail `299:2` and View history `413:32`.
- [x] **Three accessory components were loose on Cover** (PermissionStatusBadge `383:14`, StatusChevron
  `383:15`, Accessory/Value `389:5`) → moved to **Rows & Controls** (`297:6`) where the other accessories live.
- [x] **Cover page was empty after the moves** → added a proper cover card `452:2` (brand-blue bound to
  `brand/blue`, white title + 85% subtitle "iOS 26 · Figma source of truth for the SwiftUI app").
- [x] **Proposed · Agent surfaces page was a hand-placed scatter with overlaps** (RunningTaskBanner sat on
  top of ApprovalGate; MenuValue + Browser-permissions specimen overlapped ApprovalGate's bottom;
  ContentUnavailable floated). → Rebuilt as an **auto-layout catalog** `449:56`: a **Components** wrap-grid
  (AgentStatusPill, ActionCard, ApprovalGate, RunningTaskBanner, per-capability permission menu,
  ContentUnavailableView+CTA — each in a captioned cell) + a **Screens** row (Execution trace, Connector
  consent, Browser takeover). No overlaps; consistent gaps.
- [x] **Primitives page had frames at negative/random x** (page-headers at x=−209/48/409…, Button at −282,
  Pill at −568). → Tidied into an auto-layout column `451:2` (Button, Pill, ContainedIcon, RemFaceMark, each
  with its header) + a labeled **"Superseded — specimens consolidated"** group `451:8` for the 3 orphaned
  headers.
- [ ] **Primitives orphaned headers — decide keep-as-doc vs delete.** Three page-headers (Text `65:26`,
  Surface `8:12`, Card→Section `11:11`) have no specimen on the page — their primitives were consolidated
  (Text → Foundations/Typography; Surface → dropped; Card → Section on Rows & Controls). Parked in the
  Superseded group with a note for provenance; founder to decide whether to delete them.

> **GOTCHA (recorded for future work): `figma.variables.getVariableByIdAsync` needs the FULL id
> `VariableID:2:13`, not the short `2:13`.** Passing the short form returns **null**, and
> `setBoundVariableForPaint(paint, 'color', null)` silently leaves the paint's **literal** color (black)
> — no throw, no rollback, so the fill renders black/unbound while the call "succeeds". Symptom this pass:
> the cover and 14 catalog captions came out literal black until rebound with the prefixed ids. Always use
> `getLocalVariablesAsync('COLOR')` to confirm the real id, or pass `VariableID:x:y`. (Also: `brand/blue-on-fill`
> `VariableID:2:14` is **not** white — binding white-on-accent text to it turned it blue-on-blue; the
> `label/on-accent` always-white token below is still genuinely needed.)

## Founder review 5 — 2026-09-24 (big audit: onboarding, org, agenda, voice, settings, sheets, flows)

Large voice-transcribed audit. Captured in full so nothing is lost. Legend: [x] done this pass ·
[ ] queued · [?] needs founder input or something the founder will share. **The founder is actively
editing the file concurrently** (e.g. moved the ListRow master to a "Specs ListRow" area at x≈5118),
so re-read node positions before moving things.

### Page organization (auto-layout) — continued from review 4
- [x] **Cards** page → auto-layout column (header+component sections).
- [x] **Chat** page → auto-layout column.
- [x] **Tasks & Agenda** page → auto-layout column + a "Task badges" second column.
- [x] **Rows & Controls** — organized the two loose clusters: **Controls** column (`457:2`, Switch /
  Chevron+None / ContentUnavailable+Error+Loading / SectionHeader+Footer / Toast — removes the ~5000px
  gap) and **Templates & accessories** column (`457:8`). **Left untouched (per founder):** the two spec
  frames — "Specs" `222:137` and "Redlines — ListRow 197:283" `203:153` — plus the ListRow master/showcase
  (`190:100` now "Specs ListRow", `188:2`, instances `197:283`/`222:126`), whose boundary with the redline
  spec is ambiguous. [ ] **Confirm:** should the ListRow master + its two instances + ListRowLabel be
  pulled into a "Rows" auto-layout column too, or are they part of the spec/redline showcase to leave?

### Onboarding (investigated in code — see below)
- [?] **Founder's "connect your apps + set up voice" onboarding flow is NOT wired in code** on any branch
  (only `main`-equiv + `staging`, identical onboarding). What EXISTS: (1) the **GuidedFlow** coach-mark
  engine, ported from the founder's **Munch** portfolio app (`Shared/Views/GuidedFlow.swift`) — engine
  only, deliberately not wired; its DEBUG demo steps are literally "Connect a source" / "Turn on
  notifications"; (2) **Connectors** settings screen (`SharedComposioConnectionsView.swift`) = the
  "connect apps" surface; (3) **Voice** settings screen (`SharedVoiceSettingsView.swift`, nav "Voice":
  hear-this-voice preview + Spoken-responses voice picker sheet + Character/speed sliders) = the "set up
  voice" surface. Plan (epic #1373 + `component-architecture-opportunities.md` §3): replace the
  deploy/education onboarding with the coach-mark overlay driving users to Connectors + Voice.
  → **Proposal:** build the connect+voice onboarding as **Proposed** screens by assembling GuidedFlow
  coach-mark + Connectors + Voice — since there's no existing screen graph to mirror. Awaiting founder OK.
- [x] **Voice screen built** (`476:530`, Settings page) — faithful to `SharedVoiceSettingsView.swift`
  (preview / Spoken responses / Character & speed sliders), on the **kit nav bar**. This is the "set up
  voice" destination for the onboarding flow.
- [x] **Proposed onboarding flow assembled + coach-marked** — page **`Proposed · Onboarding`**: **① Connect
  your apps** (Connectors screen `133:192`) and **② Set up your voice** (Voice screen `476:530`), each with a
  **scrim + blue spotlight ring on its real target** (first "Available" connector row; "Hear this voice") + a
  **Coachmark** instance (Step 1/2 → Step 2/2, Skip/Next). Built from existing pieces per epic #1373; replaces
  the deploy/education onboarding. Reuses `Coachmark` `491:56`.
- [x] **Coachmark tooltip componentized** (`491:56`) — reusable coach-mark bubble (Step X/Y + **Title**/
  **Body** TEXT props + Skip + Next pill + shadow), on the Proposed catalog. NEXT: pair it with a
  scrim+spotlight overlay positioned per target on the connect/voice steps.
- [x] **Onboarding page organized** — the screens overlapped (Login at x=-432, Privacy offset at 19,99);
  laid them in a clean onboarding-order row at y=0 (Login → Privacy → Deploying → Activation, 442px pitch).
  Deploying/Activation still shown (up-for-deprecation, pending founder confirm).
- [ ] Use the **iOS 26 kit Switch** (and kit controls generally), not custom, wherever a toggle is needed.

### Agenda / Tasks & Agenda components
- [?] **DateNavigationHeader arrows — NEEDS FOUNDER CLARIFICATION.** Founder: the arrows "had rectangles
  before … three rectangles beside each arrow." But prod (`SharedAgendaView.swift` `SharedDateNavigationHeader`,
  lines 223–247) renders the arrows as **plain `chevron.left`/`chevron.right` buttons (`.buttonStyle(.plain)`,
  size 14 semibold) with NO rectangle background** and no day-strip. So "three rectangles" can't be matched to
  shipping code — need the founder to point at what they mean (a tap-target bg? a loading skeleton? a week
  day-strip?). Padding: founder said leave top/bottom. Separately, minor drift to reconcile: the Figma DateNav
  `43:2` shows an extra **calendar glyph** next to the date that prod doesn't have. Not guessing — logged.
- [x] **"Add New" → component built (`AgendaAddSchedule` `490:22`).** Matches prod
  (`AgendaView.swift` ~L895): **Add New** (plus) · divider · **Schedule** (calendar.badge.clock + unscheduled
  count badge), 17pt semibold `label/secondary`. Schedule opens a "Schedule Tasks" sheet — build that sheet
  as part of the kit-sheet swap below.
- [ ] **Sheets: use the iOS 26 kit Sheet component** for all sheet interfaces ("all those views are already
  declared there").
- [x] **Agenda screen** (`181:754`) — already clean/organized (DateNav → Brief → Sort → Overdue → To Do →
  Add/Schedule → Suggestions, all in an auto-layout Content). Swapped its plain "+ Add New" for the
  **AgendaAddSchedule** component `490:22` (Add New | Schedule + count). Remaining Agenda item: DateNav arrows
  ("three rectangles") — founder clarification.
- [?] **Remove the unused section on the Agenda page** — founder says one section is unused; **which one?**
  (don't guess-delete — confirm).

### Chat
- [?] **Remove the unused section on the Chat page** — one section unused; **which one?** (confirm before delete).
- [x] **VoiceBar button opacity bug — FIXED (root cause was a Figma limitation).** The mic/end-call button
  backgrounds are `color.opacity(0.2)` (`MiniPlayerBar.swift`). The master rendered the 0.2 tint fine, but
  **Figma does not propagate a variable-bound paint's opacity into instances** — every VoiceBar instance
  reset it to solid, and instance-level opacity overrides on a bound paint don't render either. Fix: created
  two **adaptive low-alpha tint variables** with the alpha baked into the variable VALUE (`fill/label-tint`
  `471:2` = label/primary @ 0.2 light / white @ 0.2 dark; `fill/red-tint` `471:3` = system/red @ 0.2 both
  modes) and bound the master button bgs to them at **paint opacity 1** — which DOES inherit into instances.
  Replaced the corrupt Chat-screen instance with a fresh one; renders tinted. (Reusable pattern for any
  tinted-fill component; recorded in figma-gotchas.)
- [x] **Home-indicator overlap — FIXED (safe-area approach).** The DeviceFrame draws its HomeIndicator at
  screen-local y840–874, but `Screen/Chat 71:533` had the VoiceBar flush to 874. Reserved the bottom 34px
  safe area: shrank Conversation to end 678, moved composer-dock → 678 and VoiceBar → 782 (ends 840). The
  device's home indicator now sits in the reserved zone, clear of the voice bar. (Apply the same bottom
  safe-area inset to other device-framed screens whose content runs to y874.)

### Task & Events
- [x] **New Task or Event**: Content is now a vertical auto-layout form (12/16 padding, 16 gap).
- [x] **Date card de-carded** to match prod: `detailButton` in `TaskEventView.swift` is a plain button (no
  bg, no chevron) — removed the grey card fill/radius + chevron + divider on `404:88`; now a plain clock +
  "Set date, time, repeat" row, both `label/secondary`.
- [ ] **Wrong chrome — replace custom headers with the iOS-26 kit nav bar.** Kit component identified:
  **`Toolbar - Top - iPhone` `277:947`** (variants: Default/Compact Large/Large Title/Title 2 Line/2 Line
  Left; props: `Style`, `Show Subtitle`) — already used on Chat (`277:1004`). Screens still on a hand-built
  nav: New Task/Event (`404:76` Cancel/title/Save), Delete Account (`412:28`), View history/Task activity
  (`413:43`), and others. NOTE: the kit toolbar exposes only `Style`/`Show Subtitle` as props — the
  leading/trailing buttons (Cancel/Save/back) are NOT simple props, so each swap is a per-screen library-
  instance override job (configure leading/trailing, title, subtitle). Do screen-by-screen and verify each.
  **RECIPE (proven on the Voice screen `476:530`):** instance the Default variant → set the **Title** text
  node → set the **Leading** glyph to `chevron.left` AND **rebind its fill to a local variable** (`label/
  primary`), because the kit glyph fill is bound to a kit-library variable that renders invisible in this
  file → hide the **Trailing** frame → insert after the status bar with `layoutSizingHorizontal='FILL'` and
  remove the custom nav. **Done:** Voice `476:530`. **Remaining:** New Task/Event, Delete Account, Task
  activity, Billing, etc.

### Activity (View history)
- [x] **TIMELINE view — DONE.** Built a reusable **`Timeline` base component `482:56`** (status node —
  green ✓ / red ✗ / blue in-progress — on a **connecting rail** + title + right-aligned timestamp + secondary
  description; tail node has no connector) and wired it into the **Activity screen `413:32`** under the task
  header. Own design per founder ("create your own"). On the Proposed catalog as a captioned cell; supersedes
  the rail-less exec-trace body. Resolves the dashed-rail debt (uses a solid rail — dashed is optional later).

### Settings
- [x] **Billing & Usage `341:862` — now uses the canonical Section pattern.** Founder clarified: only
  SectionHeader was componentized; the grouped cards were plain frames and the legal footer plain text.
  Fixed: named/styled the two grouped cards as **ListGroup** (bg/primary, radius 10, clip) and replaced the
  plain legal-footer text with the **SectionFooter** component (`161:70`, "Terms of Service and Privacy
  Policy"). Now SectionHeader + ListGroup + SectionFooter throughout. (Usage progress rows stay custom —
  accepted.)
- [~] _(superseded)_ earlier note: Billing "appears already sectioned; confirm the gap." Prod
  (`BillingSettingsView.swift`) is a `List` of 3 `Section`s: **Current Plan** (Plan row), **Usage** (Today +
  This Month progress rows), and a third section holding the primary button with the **Terms/Privacy legal
  footer as the Section footer**. The current Figma already shows Current Plan + Usage headers with grouped
  cards, the Upgrade button, and the legal footer — so it's largely sectioned. If the founder wants an exact
  mirror, the one refinement is grouping the button + legal footer as a proper third Section (footer), and
  using the canonical SectionHeader/SectionFooter components. Flagged rather than guessing at "not using
  sections" when the screen is already sectioned.
- [x] **Sign Out → kit Action Sheet** (`412:15`). Founder added the kit sheets to the file (Platform
  Controls page). Instanced the kit **Action Sheet** (`493:1771`), set Title "Sign out of Rem?" + description,
  Action 1 = "Sign Out" (Destructive), Action 2 = "Cancel", hid the rest; widened to iPhone full-width and
  placed over a dimmed Settings backdrop. Replaced the hand-built dialog.
- [x] **Delete Account → kit Inspector sheet** (`412:24`). Instanced the kit **Sheet - Inspector - iPhone**
  (`493:3196`): Title "Delete Account", submit-arrow hidden; over a dimmed Settings backdrop, with the delete
  content overlaid in the sheet's content area (warning copy + "type delete" confirm field + full-width red
  **Delete Account** button). The red destructive button now reads clearly (addresses "looks like nothing").
  (Content is a sibling over the instance's slot since instances can't take new children.)
- [x] **Cross-cutting: kit sheets — DONE.** Sign Out → kit **Action Sheet** (`412:15`) ✓; Delete → kit
  **Inspector sheet** (`412:24`) ✓; **New Task/Event → kit Full-height sheet** (`Screen/New Task (Full sheet)`
  `507:115`) ✓ — kit Full Screen sheet (grabber + **X**=cancel + Title + blue **↑**=save) with New Task's form
  (TypePicker/Title/date/notes) over a dimmed backdrop. NOTE: the kit Full sheet uses X/arrow, not Cancel/Save
  text — founder can flip to text buttons if preferred. The Agenda **"Schedule Tasks"** full sheet is a
  separate net-new screen (build when its content is specified).
- [x] _(orig)_ **Cross-cutting: kit sheets — components identified by founder.** The iOS-26 kit ("iOS and iPadOS 26")
  has **Action Sheet**, **Sheet - Inspector - iPhone** (medium), and **Sheet - Full Screen** (full-height).
  Founder: Rem uses **two sheet types — Inspector + Full-height** — plus the Action Sheet. **Mapping:**
  Sign Out → **Action Sheet** (destructive "Sign Out" + Cancel; Action Sheet props: Title, Description,
  Action N {Mode, Type=Destructive/Secondary, Text}); Delete Account → **Inspector sheet** (X · Title · blue
  action button + confirm content); New Task/Event (Inbox "+") + Agenda **Schedule** → **Full-height sheet**.
  **BLOCKER:** `search_design_system` won't surface the iOS-26 kit's sheet keys (only iOS 15/17/18 versions),
  and the plugin can't instance a library component without its key. → **Founder: insert one instance of each
  (Action Sheet, Sheet-Inspector, Sheet-Full) into the file** (the Assets panel "Insert instance" button) and
  the agent will reuse + configure them for the swaps; or OK an iOS-18 kit fallback. Do NOT hand-build sheet
  look-alikes (drift — the anti-drift rule says reuse the canonical/kit component).
- [x] **Settings page tidied** — the settings screens were already in a clean row (Settings · Connectors ·
  About · Billing · Permissions · Sign Out · Delete · Voice at y=0, 442px pitch); the one stray was the
  "Device — Settings" instance floating at (1440,1210) → tucked below the row at (0,980). The Settings
  *screen* itself (`130:44`) is a grouped List, already organized.
- [ ] **Build the "General" child view** — we renamed Settings "Rem" → **General**; the child screen isn't
  built. (This unblocks the earlier founder-gated rename.)
- [x] **Share / Feedback / Report OS experience** — built an **iOS share-sheet (activity view) specimen**
  `494:56` (grabber + item preview + app-icon row Messages/Mail/Notes/More + actions Copy / Add to Reading
  List / Save to Files) on the Proposed catalog, representing what Share/Feedback/Report open. (Feedback also
  opens a Mail composer — can add a compose specimen if wanted.)

### Inbox
- [x] **Inbox "+" sheet = New Task or Event `404:5`** — both plus buttons (header + toolbar) call
  `onCreateTask()` (`SharedInboxView.swift` L33/L74), which presents the **New Task or Event** create screen
  as a sheet. Screen already built (`404:5`, de-carded + auto-layout). Founder: Rem uses **two kit sheet
  types — an Inspector (medium) and a Full-height sheet** — New Task/compose is the **Full-height** one.

### Flows
- [x] **Navigation map redrawn VERTICALLY** — `496:2` on the Flows page: an indented vertical tree
  (onboarding chain → Main → tabs → sub-screens) with connectors; Deploying/Activation dashed-orange
  (deprecation), General purple (proposed). Scales vertically as the founder wanted. The old horizontal map
  (`420:15` boxes) is superseded — remove it when confirmed. (If the founder still wants it in FigJam, provide
  the Figma **plan key** and I'll regenerate via `generate_diagram`.)

### New card types (founder, with screenshots) — BUILT
- [x] **Code hunt: neither exists.** Confirmed both are net-new (no `PollCard`/`ChoiceCard`, no
  `DraftCard`/`ComposeCard`; "None of these" not in code). Closest existing: `RemProposalCardView`
  (single Approve/Dismiss), `PollParams` (non-UI outbound-poll payload), `RemRemoteLogoView` (provider logos).
- [x] **PollCard** `458:56` — question header + option rows (title wraps, right-aligned status "On your
  list"/"New") + "None of these"; on the Proposed page catalog. Matches the founder screenshot.
- [x] **MessageDraftCard** `458:69` — provider header (tile + Gmail) + To/Subject/body + full-width Send;
  on the Proposed catalog. Logo is a neutral-tile placeholder (brand-logo fetch debt applies).
- [ ] When wired in code later, both graduate Proposed → Foundation; fetch the real Gmail/Slack brand logo.

## Founder review 3 — 2026-09-24 (screens/components deep pass)

- [x] **Avatar was orphaned on the Cover page** → moved to **Rows & Controls** (founder couldn't find it;
  the old ContainedIcon page is gone). Add it to the Component Index.
- [x] **ListRow separator** — CONFIRMED it's the iOS-26 source: bound to the local `separator` variable
  = `{60,60,67}@0.29` (`UIColor.separator`). Lightened the weight **1px → 0.5px** (true hairline) since
  it read too heavy; color unchanged.
- [x] **RemFaceMark on Task detail** — now contained in the app's avatar treatment: a `brand/blue @0.15`
  **circle** with the face at 60% tinted `brand/blue` (matches `TaskCommentsSection.avatar`,
  `avatarSize=28`). Reads blobby at 1× (tiny) but faithful; on retina the eyes/smile resolve.
- [ ] **Permissions + Billing use custom rows, not canonical ListRow / SectionHeader / SectionFooter.**
  Rebuild both on ListRow + the Section components (grouped list).
- [ ] **Settings first row (Account) is a custom "Profile" card → should be a ListRow** (Avatar leading +
  name/email as the ListRowLabel title/subtitle).
- [x] **Connectors rebuilt to the shipping pattern** (`133:192`): **Connected / Available** grouped
  sections; each row = ContainedIcon glyph + `displayName` + **status subtitle** ("Connected • Active" /
  "Connected • Paused" / "Not connected") + **chevron** (all on canonical ListRow); tight header→card
  spacing (6px). Fuller list (Gmail, Google Calendar, Notion, Slack, Google Drive, Linear, Todoist).
  Real brand logos remain a follow-up — `logos.composio.dev` is network-blocked here (403); SF-Symbol
  fallbacks in place, fetch logos in CI. Original note kept below.
- [~] **Connectors — real logos (follow-up).** The shipping `SharedComposioConnectionsView` pattern: row =
  brand logo + `displayName` + **status subtitle** ("Connected • Active" / "Not connected") + a
  **chevron** (opens the connect/disconnect sheet) — NOT a "Connect" button + chevron mix. Also: use
  real **Sections** (the "CONNECTED APPS" header is spaced too far from the rows), and show the **fuller
  Composio list** (gmail, googlecalendar, googledrive, googledocs, googlesheets, github, slack, discord,
  whatsapp, telegram, notion, linear, todoist, asana). Real brand logos come from `logos.composio.dev`
  (SVG) — **network-blocked here (403)**; only `google-icon` is a local asset, so use the app's
  SF-Symbol fallbacks in the file and fetch real logos in CI (see design-drift issue).
- [ ] **Button is under-spec.** Only the small bordered "Connect" accessory exists. The app also uses a
  **rectangular filled prominent** button (sign-in) and a **modifier style** (`remPrimaryActionButton`,
  `RemSettingsCTAButtonStyle`). Add these as Button variants/styles.
- [ ] **Inbox/Connectors section label far from screens** — content-organization nit (the ⑤-section
  label sits well above the frames). NOTE: founder confirmed the Inbox "layout collapse" and the About
  "blocked view" were **Figma render-cache artifacts** (fixed on page refresh), not real bugs.

Debt found while building. **Do not fix inline** — note it here and defer to a focused cleanup
pass/PR. Guiding principles: **code is the source of truth (screenshots can be stale)**;
**everything is a component property/variant**; **one canonical per concept (no duplicates across
pages)**.

## Row decision — KEEP custom `ListRow` (swappable leading), re-base to iOS 26 *(reversed)*
- **The kit Row can't take our `ContainedIcon` leading.** The iOS 26 `Row`
  (`5587f1ebded12290ac2731cef9d68f98a4a0ac61`) has **no instance-swap props**; its leading is a
  nested `Image` instance (`Type` = Fill/Circular/Rounded/Symbol) — image fills or a plain symbol,
  never our colored-square `ContainedIcon` component, and nested mains can't be swapped without an
  exposed swap prop. Settings rows need `ContainedIcon` (branded, variable-bound), so **adopting the
  kit Row wholesale is rejected.** (Founder caught this before build — good.)
- **Decision:** keep our custom **`ListRow`** (`101:18`) with its swappable slots (Leading
  Accessory = ContainedIcon/Avatar/Symbol/None · Content = ListRowLabel · Trailing Accessory =
  Chevron/Switch/Button/None), and **re-base it to iOS 26**: match the kit Row's metrics
  (Regular ~52 / Tall), bind fills to the local **Color** variables, use real SF Symbols. "Built on
  the iOS 26 Row" = its metrics/tokens/look, while keeping the swappable leading the app requires.
- [ ] Re-base `ListRow`: iOS 26 metrics + bind colors (bg→`background/primary`, hairline→`separator`,
  labels→`label/*`) + real chevron/switch symbols. Then screens re-verify on it.
- Standalone kit pieces still worth using where there's no swappable-leading need: `Section Title`
  (`216bce9b…`), `Header` (`e86f40bf…`), `Grouped Table Footer` (`19a81fe0…`).

## iOS 18 → iOS 26 kit re-base (HIGH — core fidelity)
Kit components were imported from the **iOS 18** library (nav `d299571689…`, Row
`8bb9d297eb…`). The **official "iOS and iPadOS 26"** library is subscribed to the file AND
importable by key (confirmed: imported iOS 26 `Status bar - iPhone` live). **Do not publish a
copy** of iOS 26 — the live subscription tracks Apple's updates; a copy is a fork that drifts.
Re-base every kit usage to iOS 26:
- [ ] **Nav → `Toolbar - Top - iPhone`** (`e68d32a867ed6d4d8620f1614cd2d4b9d544b92d`, a set;
  `Style` = Default / Compact Large / Large Title / Title 2 Line / Title 2 Line Left — use
  **Default** for inline). Rebuild Chat (`256:35`) + Settings/Connectors/About navs on it; retire
  the iOS 18 `Navigation Bar`. (In iOS 26 the nav bar *is* the Toolbar-Top, as the founder noted.)
- [ ] **Row / List → the iOS 26 Row/List** (find key in the iOS 26 library); rebuild `ListRow`
  on it (currently on the iOS 18 Row set).
- [ ] Audit any other iOS 18 imports (controls, bezel) and move to iOS 26.

## Figma
- [ ] **Nav reconciliation (drift).** File mixes two nav approaches: hand-built `nav` frames
  (e.g. Chat `71:544` — transparent + baked hairline) and the imported kit `NavigationBar`
  instance (e.g. Settings `154:790`). Standardize on ONE. Prefer **configuring the kit nav**
  (mirror-upstream): transparent bg that inherits the page bg, hairline only in a `Scrolled`
  state, title/leading/trailing via props. Build a thin `RemNavigationBar` wrapper **only** if the
  kit can't express transparent-bg + hairline-on-scroll — and if so, document the exact kit
  limitation in its description + `REGISTRY.md`. Verify kit capability before deciding.
- [ ] **Global page-bg variable.** Introduce `Surface/Background` (primary vs secondary/grouped)
  so screens + navs inherit it instead of hardcoding white. Platform-mode ready.
- [ ] **Move spec sheets to the Specs page.** The plugin's ListRow spec (`222:137`) and future
  sheets belong under `———  SPECS  ———`, not loose on a component page.
- [ ] **Audit duplicate components across pages.** Confirm single-canonical; fold any dupes.
- [ ] **REGISTRY.md sync:** `ComposerBar` → `RemComposerBar` (renamed in Figma).

## Nav — RESOLVED (standardize on the kit nav)
- [x] **Kit nav trailing DOES take our glyphs.** (Earlier "needs an Apple symbol component" was
  wrong.) The kit `NavigationBar` trailing/leading button has a **`Symbol#…` TEXT property** — set
  it to the SF Pro glyph directly (ellipsis `U+100360`). Config the kit nav via its props: `Title`
  (text), `Show Leading/Trailing`, the buttons' `Symbol` text, and `Show Background`
  (transparent-vs-material). **No `RemNavigationBar` wrapper needed** — standardize every screen on
  the kit nav; retire hand-built `nav` frames.
- [ ] Minor: Chat nav ⋯ renders blue (kit default tint); `06-chat` shows it in a grey circle
  button. Polish the trailing button style/color to match if we want exact parity.
- [ ] **Home indicator absent.** DeviceFrame bezel (`128:46`) has none; the iOS 26 kit omits it
  (system overlay). Decide whether to add a simple home-indicator bar to the bezel (would overlay
  bottom bars like the voice bar, as on-device) or leave it kit-faithful.

## Structure
- [x] **Component domain pages — DONE.** Consolidated ~20 one-per-page components into 5 domain
  pages (Primitives · Rows & Controls · Chat · Tasks & Agenda · Cards) + Platform Controls, under
  the real `COMPONENTS` divider. Each stacked component-under-page-header, verified no overlap.
  File down from ~33 → 15 pages. Specs live per domain page (no separate Specs section).
- [x] **Per-screen pages DONE** — split the single `Screens` page into one page per screen under the
  `SCREENS` divider: Agenda · Chat · Task detail · Settings · Connectors · About · Inbox · Billing ·
  Permissions · Device Kit. Old `Screens` page + empty ② Settings section removed. Cross-page instance
  refs (DeviceFrame master → Device Kit page) verified intact.

## Components still needed
- [x] **RemFaceMark built** (`361:7`, Primitives) — the real Rem brand face (scalloped `CustomFaceShape`
  blob outline + 2 bar eyes + `RemSmileShape` smile), parsed from `Shared/Views/RemFaceMark.swift`'s
  SVG path via `createNodeFromSvg`, ink bound to `labelPrimary`. Swapped into **Task detail**'s activity
  avatar (was a drawn placeholder). Verified. **`.thinking` mode added** — RemFaceMark is now a set
  (`362:7`) with `Mode` = idle (outline + eyes + smile) / thinking (outline only, heavier stroke — the
  self-drawing signature, static). FOLLOW-UP: reuse the mark in the chat thinking-indicator / empty-state.

## Icons
- [ ] `mic.fill` — no caption in the community SF Symbols file; source via the icon-request frame
  (needs a Mac). Voice bar currently renders its own mic.
- [ ] Brief speaker: confirm exact variant (`speaker.wave.2.fill` `U+1002A7` vs `.3.fill`) from
  source before swapping the 🔊 emoji.

## App code (Rem / RemClaw — verified PR, never blind-delete)
- [ ] Dead default-bubble path: `ChatMessageViews.swift` (`ChatMessageBubble` / `ChatBubbleShape`)
  + `RemGatewayChatView` — `SharedRemChatView` replaced them and draws its own bubbles
  (speechBubble @3400, streamingBubble @4879). grep-confirm truly unreferenced, then deprecate in a
  focused PR with build + test. (Confirmed already: there is **no** legacy `ComposerBar` — only
  `RemComposerBar` — so that one is just the Figma rename above.)

## Founder review — 2026-09-24 (catch-all; fidelity-first, screens paused)

Standing redirect from this review: **component fidelity before more screens.** Founder does not
trust state fidelity — **do not build speculative states** (Inbox-Loading explicitly cut; Agenda
loading to be deleted). Verify every state against the **running app**, not just code, before
building it. Main intent restated: (1) **code-sync** so the file is true to the app *today*;
(2) design is cheap — a faithful design lets an agent execute faster. Link component descriptions to
their **codebase source** (path) so reviewers can find them.

### Primitives / set membership (decide — some are removals)
- [x] **Removed Text, Surface, AND Card** primitives (founder confirmed all three). Swept all pages
  first — **zero instances** anywhere, so no screens/components broke. Type ramp lives in the local
  text styles; surfaces/containers come from the color/background foundation + Section header/footer.
  Dropped from REGISTRY.
- [x] **ErrorBanner — answered: it IS used.** No shared `ErrorBanner` component in code; it's a
  private `errorBanner(_:)` view duplicated inline in 4 files — `SharedRemChatView` (3849),
  `RemConversationView` (86), `TaskCommentThreadView` (178), `TaskCommentsSection` (357) — shown on a
  send/post **failure** in chat + task comments. Recommend **keep** (and the app should extract it into
  a shared component). Description now points at these usages.
- [x] **Descriptions → code links (first pass).** Backfilled `node.description` with source paths for
  13 canonical components (ListRow, TaskEventRow, SuggestedTaskRow, MessageBubble, RemComposerBar,
  VoiceBar, CalendarEventsCard, RemindersCard, ProposalCard, DateNavigationHeader, TypingDots,
  ErrorBanner, ContentUnavailableView). Remaining components: backfill in a follow-up.

### Component fidelity bugs (do these — verifiable vs code)
- [x] **VoiceBar button backgrounds fixed** (all 6 states verified). Bugs were: (1) button-bg
  circles at opacity 1 on Speaking/Muted/Reading → set to **0.2** per `MiniPlayerBar` (`color.opacity(0.2)`);
  (2) **Muted** mic glyph was `labelPrimary` but muted mic color = **`.red`** → rebound red;
  (3) **Reading** stop icon was `phone.down.fill` but reading uses **`stop.fill`** (`U+1006F7`) → fixed.
- [x] **CalendarEventsCard icon fixed** — drawn vectors → real `calendar` glyph (`U+100249`),
  `.subheadline` 15pt, **systemRed** (per `CalendarCard.headerView`). Verified.
- [x] **RemindersCard icon fixed** — drawn vectors → the real branded **AppleRemindersLogo** image
  (`Rem/Assets.xcassets/AppleRemindersLogo.imageset/Apple_Reminders_Logo.png`), embedded via
  `figma.createImage` (the `upload_assets` host `mcp.figma.com` is **network-blocked** here — 403 CONNECT).
  Verified. Header copy **fixed** → "3 reminders" (matches `Text("\(count) reminder(s)")`).
- [x] **ProposalCard header icon fixed** — drawn vectors → real `checklist` glyph (`U+100DFE`),
  `.subheadline` 15pt, **brandBlue** (per `RemProposalCardView.header`), across all 4 states
  (pending/succeeded/failed/stale). **Footer status icons also fixed**: succeeded=checkmark.circle.fill
  (green), failed=xmark.octagon.fill (red), stale=clock.badge.exclamationmark.fill (orange) — real
  glyphs, bound tints, per `RemProposalCardView.terminalIcon/terminalTint`. All 4 states verified.
- [x] **Glyphs sourced + verified** (added to sf-symbols-map): `checklist`=`U+100DFE`,
  `calendar.badge.clock`=`U+1009DE`, `arrow.uturn.backward`=`U+100C4D`. (Glyph-filter gotcha: PUA
  codepoints > U+FFFF are **surrogate pairs** — filter on `[...ch].length===1`, not `ch.length===1`,
  or the lookup finds zero glyphs.)
- [x] **TaskEventRow reworked (3-slot Row model + pills).** Both variants restructured to
  **Leading · Content · (trailing = list-level chevron)**; Content is now a VStack of a title row
  (status + title) + a **Pills row** (canonical Pill: `list` badge for tasks, `dot` calendar badge for
  events). Agenda instances show pills (filed); Inbox instances have the Pills row hidden per-instance
  (unfiled → faithful). Verified component + Agenda + Inbox (no regression; fixed a 2-line-title
  collision by hiding pills on Inbox). **Pills now a proper `Pills` BOOLEAN component property**
  (default true; both variants' Pills frames bound to it; Inbox rows set `Pills=false` via the prop
  instead of a per-instance hide) — verified. **Leading-state variant added**: `Leading` = Time /
  Schedule (calendar.badge.plus) / Clock (task only; events are always Time). Inbox rows now use the
  canonical **Leading=Schedule** variant instead of a per-instance glyph override. All 4 variants
  verified. MINOR: the `Pills` boolean binding didn't clone cleanly onto the Schedule/Clock variants,
  so Inbox pills are hidden by a direct `visible=false` (works); re-bind Pills on those two variants
  in a later pass.
- [~] **Height-100 layout bug.** Frames default to fixed height 100 instead of hug.
  **Agenda rows FIXED** (`182:2/15/28` were `counterAxisSizingMode=FIXED` h100 → set to hug; now 64/64/58;
  spacing verified). **Task detail** still pending — the frame named "action" + dates/meta (founder
  fixed some manually; re-read live state before touching to avoid clobbering their edits).
- [ ] **Card icons wrong.** CalendarEventsCard, RemindersCard, **ProposalCard** icons don't match
  fidelity — replace drawn/placeholder icons with the real SF Symbols the app uses.
- [ ] **Row hierarchy idea.** TaskEventRow is conceptually a **Row** with re-slotted
  leading/content/trailing (leading = time/schedule/clock, content = status+title, trailing = pills).
  Consider modeling it as a descendant of the base Row rather than a parallel component.

### Screens (paused — fidelity + arrangement first)
- [x] **Real page dividers** — replaced the dash-name section pages (`—  FOUNDATIONS  —`, etc.) with
  actual `createPageDivider()` dividers (`isPageDivider:true`) at Foundations / Components / Screens.
  NOTE: Figma ties `isPageDivider` to the name — a real divider **can't carry a custom label** (setting
  one reverts it to a regular page), so section identity now comes from the content pages after each
  divider. Verified the page order.
- [x] **Sections/screens resolved** by the per-screen-page split above — each screen (with its device
  frame + states) now lives on its own dedicated page, so the loose-frames / sections-don't-enclose
  problem is moot.
- [ ] **Arrangement is scattered** — "things are all over the place, I have to hunt for the right
  screen." Impose a clean, findable layout before adding more.
- [x] **Home indicator adopted.** Built canonical **HomeIndicator** (`333:102`, Platform Controls):
  transparent band + 144×5 labelPrimary pill, added to the **DeviceFrame master** (`128:46`) so every
  device-framed screen shows it. Transparent band = bg inherits whatever's docked behind it. Verified
  on Device — Inbox and Device — Agenda. **Chat bg-sync verified OK**: the VoiceBar sits at y=816–874
  (reaches the screen bottom edge), so the transparent home-indicator band already inherits the
  voice-bar material — no white gap, nothing to fix. Same holds for any docked bar that reaches y=874.
- [x] **Agenda no-state rebuilt** from the real `AgendaNullStateView`: was a `◔` emoji + wrong copy;
  now real `calendar.badge.plus` (64pt), "No agenda yet" (title1Bold), "Create a new task or schedule
  existing ones" (body), "+ Add New" button. Verified. (Schedule button — only shows when
  `hasAvailableTasks` — deferred pending the `calendar.badge.clock` glyph.) **Screen/Agenda-Loading
  deleted** (140:1643).
- [x] **Agenda bottom toolbar removed** (was glass icon buttons — wrong). The app has **no** bottom
  toolbar; the only bottom element is a floating **Jump to Today** capsule (`.ultraThinMaterial`,
  `arrow.uturn.backward` + "Jump to Today"), shown **only when not on today**. TODO: build that capsule
  (needs `arrow.uturn.backward` glyph) as the not-today state.
- [x] **DateNavigationHeader** — removed stray `- - -` rectangles (`208:4/208:18`) from the master's
  side frames; app has chevron-only sides. Cleans all agenda instances.
- [x] **Task detail composer fixed** — was a hand-built field; now an instance of the canonical
  **RemComposerBar** (`53:2`) with `leading` + `trailing-speak` hidden and placeholder "Continue in
  chat…", exactly matching `TaskCommentComposer` (leading/trailing = EmptyView, doorway placeholder).
  Verified. Also hugged the Task-detail `activity` (→112) and `action` (→14) frames off fixed-100.
- [x] **Section shown in combination with a list** — built a "Section (grouped list — in use)"
  specimen on Rows & Controls (`360:204`): SectionHeader + a grouped card of 3 ListRows (separators
  between, none on the last) + SectionFooter. Verified.
- [ ] **List styles (open design Q).** SwiftUI has many list styles (plain/inset/grouped/insetGrouped/
  sidebar). Still open how the system represents them component-side — founder may share docs. The
  grouped/insetGrouped style is now shown (Settings screens + the Section specimen); plain is shown
  (Inbox/Agenda). Sidebar/inset variants TBD.
- [x] **Switch / Button need no glyphs** — Switch is a track+knob toggle, Button is a text label; the
  "symbol pass" gap was **ContainedIcon only** (now fixed).
- [~] **Settings sub-screens** (founder: build them). **Billing & Usage DONE** (`341:862`, Free-plan
  Upgrade CTA, Current Plan + Usage progress bars; from `BillingSettingsView.swift`; verified).
  **About DONE** (`134:242`) — added the missing **hero** (real Rem AppIcon embedded via createImage +
  "Rem" + "Turn your thoughts into actions"), reordered to Hero → Legal → Version, and hugged the
  Version card off fixed-100. Matches `SharedAboutView` iOS. Verified.
  **Permissions DONE** (`349:905`): 3 grouped sections (Notifications / Device Data / Media & Voice)
  with footers; ContainedIcon rows — Notifications (bell.fill/red), Calendar (calendar/red), Reminders
  (real AppleRemindersLogo image), Microphone (mic.fill/orange), Speech Recognition (waveform/indigo),
  Camera (camera.fill/gray). Verified. **All 3 Settings sub-screens built (Billing · About · Permissions),
  and the ContainedIcon blank-glyph blocker is fixed (see top of file).**
- [x] **Inbox nav actions removed** — header now title-only ("Inbox" Large Title), Leading/Trailing
  control frames hidden, matching `InboxHeader` (title-only). Populated list = TaskEventRow rows with
  `calendar.badge.plus` unscheduled leading. **Inbox-Loading intentionally NOT built** (founder cut).

## Founder review 4 / code-sync — 2026-09-24 (rows → canonical ListRow)

Driven off "convert the remaining custom rows to ListRow" + the login-button confirmation.

- [x] **Button rebuilt to the real Rem taxonomy** (`377:8`). Was Accessory/Prominent(blue)/Bordered —
  none matched shipping code. Now **Style = Primary / CTA / Connect / Destructive**, variable-bound:
  - **Primary** = filled `.label` (black in light / white in dark), `.systemBackground` text, radius 10
    — this is `RemPrimaryActionButtonStyle`, the **login / onboarding / consent CTA** (its own doc
    comment: "mirrors the sign-in button style"). This is the black button the founder asked about.
  - **CTA** = accent text-only (`RemSettingsCTAButtonStyle` .primary) · **Destructive** = red text-only
    (.destructive) · **Connect** = `fillTertiary` capsule + brand-blue text (`RemRowConnectCTA`).
  Verified (screenshot + property read: `2:6`/`2:3` Primary, `2:13` CTA/Connect, `2:17` Destructive).
- [x] **Permissions rebuilt on canonical ListRow** (`349:905`). All 6 rows are now `ListRow` instances
  (ContainedIcon leading glyph+color · title · **StatusChevron** trailing = badge + chevron), both
  sections use **SectionHeader/SectionFooter** components. Fixed a footer clip (hug to 2 lines).
  New canonical components: **PermissionStatusBadge** (`383:14`, Status Enabled/Denied/Limited/Not Set,
  from `PermissionUtils.swift`) and **StatusChevron** (`383:15`). Verified.
- [x] **Billing Plan row → ListRow** (`341:862`). Plan is now a `ListRow` (Leading off · title "Plan" ·
  **Accessory/Value** `389:5` trailing = "Free"); both headers use `SectionHeader`. Usage progress
  rows stay custom (accepted — inherently non-row). Verified.
- [x] **Settings Account/Profile → ListRow** (`130:44`). The custom `ProfileRow` is now a `ListRow`
  (Avatar leading @44 · bold name · email subtitle · no trailing). Also fixed the section header
  "YOUR AGENT RUNTIME" → "Your agent runtime" (title-case, via SectionHeader; source is title-case). Verified.
- [x] **Component Index synced** (`166:2`): Button row fixed (`110:47`→`377:8`), stale **Text** and
  **Surface/Card** rows retired (both deleted per founder decisions), added **RemFaceMark /
  PermissionStatusBadge / StatusChevron / Accessory/Value**, corrected Avatar's page → Rows & Controls.

### New minor debt (log-don't-fix)
- [ ] **Reminders leading regressed to a glyph.** The Permissions ListRow rebuild cleared the old card,
  which held the **real Apple Reminders logo image** (`createImage`); the new row uses a purple
  `list.bullet` ContainedIcon as a stand-in. Restore the bundled `Apple_Reminders_Logo.png` as that
  row's leading (bundled asset, not network-blocked — feasible now).
- [ ] **Avatar fallback has no `person.fill`.** The profile ListRow shows a plain gray circle; shipping
  `fallbackAvatar` is a gray circle + `person.fill` (20pt, secondary). Add the glyph to the Avatar
  master (need a verified `person.fill` codepoint — not yet in sf-symbols-map.md, so not guessed).
- [ ] **Billing CTAs use stock SwiftUI buttons** (`.bordered` / `.borderedProminent`), not Rem's
  `RemPrimaryActionButtonStyle`. Faithful in Figma (blue-filled), but a **RemClaw code-consistency**
  candidate: unify Billing on the Rem button system. This is *app-code* debt → a GitHub issue if the
  founder wants it tracked, not Figma debt.

### Task state badges — minor (2026-09-24)
- [ ] **Working badge uses `arrow.clockwise` (`U+100148`)** as a stand-in for `arrow.triangle.2.circlepath`
  (the pulsing two-arrow loop in `TaskRunStatusBadge.swift`) — not captioned in the community symbol
  file. Visually reads as "working/syncing"; swap to the exact glyph via the icon-request fallback.
- [ ] **All-day event TaskEventRow variant** not yet added (Leading shows "All day", no time). The
  status badges (RunStatus/Deemphasis/Overdue/Priority) are built; the all-day *row* variant is the
  remaining "event type" from the Muse-audit list.

### View-history Rem avatar render (2026-09-24)
- [x] **RemFaceMark master fixed (was a black square).** Root cause: the `RemFaceMark` component **frame
  had a `label/primary` (black) fill** and the face-blob vector was unfilled — so it rendered as a black
  squircle. Fixed both variants (`361:7`/`362:2`): frame → transparent, blob → `brand/blue`, eyes+smile →
  white. Now renders a brand-blue face with white features; **all RemFaceMark uses across the file inherit
  this.** View-history avatar circle opacity also fixed (was solid from the bindFill bug); eyes reset to
  white on those instances. Remaining nit: at 17px the `#0C50FF` face reads dark-but-blue (faithful to the
  brand tint). **NB the inline `bindFill` helper had a one-step opacity-reset bug — tinted (<1 opacity)
  fills went solid** (grabbers @0.5, inactive pager dots @0.4); use the two-step re-apply pattern (as the
  badges/avatars now do) in future builds.

### Login / onboarding brand-icon debt (2026-09-24)
- [ ] **Google multicolor "G" logo** on the Login "Continue with Google" button (`411:15`) is a **white
  "G" placeholder** — the real brand asset host is network-blocked here (same as Notion/Slack/Reminders).
  Fetch + embed in CI. Apple side is exact (apple.logo = `U+F8FF`).
- [ ] **SignInButton should graduate to a Button variant.** The two sign-in buttons are composed frames
  (neutral/black fill + icon + label), faithful to `SignInButton.swift`, but per the code-arch doc they
  should become a **Button "SignIn" variant with an icon slot** (RemButton). Until then they're screen-local.

### Proposed agentic surfaces — polish follow-ups (2026-09-24)
- [ ] **Always-dark chrome token.** The Browser takeover (`437:68`) uses **raw dark RGB** for its top/
  bottom chrome (a takeover surface is always dark, but `label/primary`/`background` invert with mode).
  Add an `surface/always-dark` (+ on-dark label) token and bind the chrome to it. Same for the coachmark.
- [ ] **Execution trace dashed rail.** The step timeline (`431:21` reference specimen **and its merged
  product surface, View history `413:32`**) has status icons + steps but not the **dashed vertical rail**
  connecting them (Muse's timeline connector). Add a per-step dashed line segment in the leading column
  (or a LINE node with `dashPattern`) — decorative, deferred. Apply to both nodes when done.
- [ ] **ApprovalGate / ActionCard connector logos** use a neutral tile + glyph, not the real brand logo
  (Notion, etc.) — network-blocked; fetch in CI (same as the other brand-logo debt).

### Button emphasis tier — Label binding fixed (2026-09-24)
- [x] **Primary Blue / Primary Gradient tiers didn't respond to the `Label` prop.** Cloning Primary to
  make the tiers dropped the text node's `componentPropertyReferences` (came back `{}`), so instances
  showed the static "Continue". Re-bound both variants' text to `Label#377:0` (`408:15`/`408:17`); the
  tiers now honor the Label component property.

### Button reworked to Type · Tier (2026-09-24) — gradient removed
- [x] **Gradient removed** (founder: "we don't need Muse's gradient"). Button restructured to the
  **Type {Rectangular/Pill/Text} × Tier {Black/Secondary/Blue/Destructive}** model, destructive matching
  SwiftUI (`Rect·Destructive` = `.borderedProminent .red`, `Text·Destructive` = `.plain .destructive`).
  The 3 instances that used the gradient (Allow / Connect / Take control) were repointed to `Rect·Blue`.
  The gradient-stop-binding debt is void.
- [ ] **`label/on-accent` token still needed.** `Rect·Blue` and `Rect·Destructive` use **raw white** text
  (a blue/red fill is the same in light+dark, so `background/primary` — which inverts — is wrong). Add a
  `label/on-accent` (always-white) token + `RemButtonTokenSet`, then bind these tiers' text to it.
- [ ] **Adopt GuidedFlow coach-mark overlay** (`GuidedFlow.swift`) as a canonical component — spotlight
  scrim + cutout + Step X/Y tooltip (Skip/Next). Replaces the deprecated activation/deploy onboarding
  screens; documented in RemClaw `docs/architecture/component-architecture-opportunities.md` §3.

## Done (kept for trail)
- [x] DateNavigationHeader: real `calendar` glyph, brand blue `#0C50FF`, H-padding removed.
- [x] Agenda + SuggestedTaskRow: drawn icons → real SF Symbols; toolbar overflow fixed.
- [x] Pages reorganized into labeled sections (META · FOUNDATIONS · COMPONENTS · SCREENS · SPECS).
- [x] Figma `ComposerBar` → `RemComposerBar`.
