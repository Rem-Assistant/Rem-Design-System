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

### View-history Rem avatar render (2026-09-24)
- [ ] **Rem activity avatar renders dark.** On View history (`413:32`), the Rem rows' avatar
  (`RemFaceMark` `362:7` idle in a `brand/blue`@0.15 circle) renders as a **dark navy blob**, not the
  intended small blue face on a light-blue tint (`TaskCommentsSection`: `Circle().fill(accent.opacity(0.15))`
  + `RemFaceMark(tint: accent)`). The `person.fill` "You" avatars are correct. Likely the RemFaceMark
  default variant's fill/size — fix the instance tint+size (or wrap), then reuse for the task-detail
  activity avatar too.

### Login / onboarding brand-icon debt (2026-09-24)
- [ ] **Google multicolor "G" logo** on the Login "Continue with Google" button (`411:15`) is a **white
  "G" placeholder** — the real brand asset host is network-blocked here (same as Notion/Slack/Reminders).
  Fetch + embed in CI. Apple side is exact (apple.logo = `U+F8FF`).
- [ ] **SignInButton should graduate to a Button variant.** The two sign-in buttons are composed frames
  (neutral/black fill + icon + label), faithful to `SignInButton.swift`, but per the code-arch doc they
  should become a **Button "SignIn" variant with an icon slot** (RemButton). Until then they're screen-local.

### Button emphasis tier — token follow-ups (2026-09-24, Muse audit)
- [ ] **`on-accent` label token missing.** The new **Primary Blue** / **Primary Gradient** tiers use
  **raw white** text (a blue/gradient fill is the same in light+dark, so `background/primary` — which
  inverts to black in dark — is wrong here). Add an `label/on-accent` (always-white) token to the
  color collection + `RemButtonTokenSet`, then rebind these two tiers' text to it.
- [ ] **Gradient stops not variable-bound.** Primary Gradient's stops use the **resolved RGB** of
  `brand/blue` → `system/purple` (Figma variable-binding on gradient stops is limited via the plugin).
  When the `RemButtonTokenSet` lands, express the gradient as two bound token stops so a token change
  reskins it. Visually correct + token-derived today; just not live-bound.

## Done (kept for trail)
- [x] DateNavigationHeader: real `calendar` glyph, brand blue `#0C50FF`, H-padding removed.
- [x] Agenda + SuggestedTaskRow: drawn icons → real SF Symbols; toolbar overflow fixed.
- [x] Pages reorganized into labeled sections (META · FOUNDATIONS · COMPONENTS · SCREENS · SPECS).
- [x] Figma `ComposerBar` → `RemComposerBar`.
