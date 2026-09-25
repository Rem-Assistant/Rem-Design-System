# Reconciliation map — Figma ⇄ canonical code

## ⚠ FOUNDER CORRECTION (2026-09-25) — RemUI is STALE; anchor to code-with-callers
The founder reversed the premise the `/goal` was built on. Verified against the tree, not assumed:
- `docs/prototypes/RemUI` is the founder's own package (commit `290dc8b5`, samuelalake, 2026-09-24,
  imported from a local unpushed `/Volumes/.../RemUI`) — **but a stale snapshot**. Its README even
  claims RemUI is canonical and shipping onboarding is stale; the founder now says the opposite.
- **Nothing in RemUI compiles or has a caller.** `ValuePropOnboardingView` ("How Rem Works"),
  `PermissionsOnboardingView`, `HomeLandingView` appear ONLY under `docs/prototypes/` — 0 refs in
  `Rem/`, `Shared/`, `RemMac/`. The live `InboxView` (`Rem/Sources/Screens/InboxView.swift`,
  caller `ContentView:1123`) is a different view that only shares the name.
- New rule: **reconcile Figma to code that has callers and ships** (a sharper "code is source of
  truth"). RemUI drops from reconciliation target → **non-binding inspiration** (mine clean
  components, then retire). Approved this session: move a *presentational package* into this repo
  built from the **shipping** views + Code Connect; RemUI content does not move.

### 🟢 RULING (2026-09-25, founder) — mirror where we're taking it; design AND code move together
Founder ruling: **"mirror where I am taking it… you'd have to design AND code, not one or the other."**
Figma reflects the *intended* onboarding, and that intent lands in **both** the design and
`OnboardingFlow.swift` — never design-only depicting a flow the binary lacks (that's the drift we're
killing). Practically:
- **Deploy / "personal server" screen → removed from onboarding** in design AND code. It is alive today
  (`OnboardingFlow.deploying`, caller `ContentView:46`), so removal is a real product change, not a
  relabel. `415:15` stays retired (correct after all); the code change is the load-bearing part.
- **How Rem Works `545:40` + Permissions `551:49` → promoted** from "stale prototype" to the *target*
  onboarding — but they must be **coded** into `OnboardingFlow.swift`, not left as dead Figma.
- 🔶 **Open load-bearing Q (founder):** the deploy step is where the per-user gateway is provisioned. If
  it leaves onboarding, provisioning must move (background / post-sign-in / lazy on first agent use).
  Confirm that before ripping the step out. Note the existing `Proposed · Onboarding` page (`488:2`)
  already sketches a Muse-informed "Connect apps → Set up voice" first-run over the real Connectors +
  Voice surfaces — that is the likely target shape.
- **Principle now:** aspirational screens co-evolve design+code via Code Connect; "code-with-callers"
  still governs which of TWO *existing* implementations wins, and still demotes never-coded Figma-only
  flows (e.g. compose→wake/schedule) until built in both.

### Platform reach (2026-09-25, founder) — design system as the cross-platform source
Founder: the system is **HIG-based today, may deviate later; it can be an Android source; Compose can
mirror the same views.** Agreed — the cross-platform source is the **design layer (Figma + tokens)**,
not SwiftUI (Apple-only). Path: `tokens.json` → Style Dictionary → SwiftUI theme **and** Compose theme;
keep views presentational + token-driven so both platforms consume one spec; Code Connect can map
Figma → RemUI (SwiftUI) and Figma → Compose. iPad→macOS stays one SwiftUI codebase (today's Shared/
Views pattern); Android is a parallel Compose implementation of the same Figma/token spec. Standing up
`tokens.json` as the single source is the prerequisite; do it before broad screen build-out if Android
is near-term.

### Gateway runtime migration (2026-09-25, founder Q: "still provision a gateway?")
Answer from the migration contract `RemClaw:docs/rebuild/07-REM-RUNTIME-MIGRATION.md`: the **OpenClaw→Rem
de-brand is done** (OpenClawKit → in-repo `Packages/RemKit`, 129 types renamed), but the **gateway
runtime is NOT removed yet** — *"OpenClaw remains the production adapter until… direct evidence."*
Today only **signal-relevance** runs gateway-free on `rem_shared`; *"the general assignment still returns
the OpenClaw adapter."* So per-user gateways **are still provisioned today.** The committed target
**eliminates** provisioning (shared multi-tenant Rem runtime; *"will not ask a person to deploy, choose,
pair, wake, update, or repair a gateway"*), with two **open** slices: *Product cleanup* ("Onboarding and
Settings contain no gateway… surfaces") and *Infrastructure retirement*.
- **Design implication:** Figma may mirror the **no-deploy** future now (it's the contract, not a guess) —
  deploy screen stays **Retired**; onboarding target = `Proposed · Onboarding` (Connect apps → Set up
  voice). The **code** removal from `OnboardingFlow.swift` is a staged migration slice gated on feature
  parity — **do not rip it out ahead of that slice.**
- **Watch-list:** gateway-oriented Settings surfaces are on the same deprecation path — incl. the
  **"General / agent-runtime hub"** (`509:833`, `SharedGatewayDetailView`) and any pairing/deploy/wake
  Settings rows. Reconcile these against the migration, not against today's gateway code, once *Product
  cleanup* lands. Flagged, not yet changed.

Track 2 (ContextualMessage/DateNav) and track 3 (chat scenarios) are grounded in `Shared/Views/**` +
`RemChatUI` — all code-with-callers — so they stand under the corrected rule.

---

## ⚠ CRUX DECISION (founder) — RemUI is systematically simpler than the shipping app
_(superseded by the FOUNDER CORRECTION above — kept for the reasoning trail)_
Every core screen shows it: RemUI Home/Inbox/etc. are **cleaner, simpler** than what ships. Two readings:
- **(A) Simplify toward RemUI** — RemUI is the target; make the app cleaner (match RemUI everywhere; the
  richer shipping bits are what we're *removing*). The goal text ("build/update Figma to **match RemUi**")
  reads this way.
- **(B) Keep the richer shipping app** — RemUI is inspiration; the shipping app (which the founder validated
  via screenshot: DateNav dashes, "No agenda yet" + Suggestions) is canonical; fold RemUI's clean components in.
The founder has verbally overridden toward (B) once (DateNav **dashes**, kept in `43:2`). **This one answer sets
how every remaining core screen reconciles.** Onboarding was unambiguous (RemUI = the new flow) and is done;
the tension is only on the core app screens (Home, Inbox, Settings, TaskEvent). Default assumed: **(B)** —
shipping-canonical, fold RemUI's clean components in — until told otherwise.

---


> Goal (2026-09-25): make the Rem Figma design system the faithful single source of truth by
> reconciling it to the app's canonical code. **Canonical sources:** the founder-authored **RemUI**
> prototype (`RemClaw:docs/prototypes/RemUI`, branch `claude/rem-ui-prototype-reference`) for
> onboarding + core screens, and **RemChatUI** (`RemClaw:Packages/RemKit/Sources/RemChatUI`) +
> `Shared/Views/**` for the shipping app. Format: **code source → Figma node · drift found · action**.
> `✓` reconciled · `~` in progress · `⌦` retired (confirmed-dead) · `?` founder-gated.

## Tokens
- RemUI `DesignTokens.swift` ≈ Figma Color/Spacing/Type variables (same iOS-semantic base). One
  known delta: RemUI `CornerRadius` = small 8 / medium 12 / **large 24**; Figma+app = small 8 /
  medium 12 / large 16 / **xlarge 24**. RemUI's "large" == Figma "xlarge". Keep Figma's 4-step scale;
  map RemUI `large` → Figma `xlarge` when reconciling. `systemBlue 0x0C50FF` == `brand/blue` `2:13`. ✓

## RemUI screens (track 1)
| RemUI screen | Figma node | Drift | Action |
|---|---|---|---|
| `InitialOnboardingView` (sign-in) | `Screen/Login` `411:15` | logo was a plain rect | ~ **network glyph added** on the blue square (LogoView); buttons still to reconcile to `SecondaryButton` style |
| `ValuePropOnboardingView` ("How Rem Works", 3 steps) | `Screen/Onboarding — How Rem Works` `545:40` | was missing | ✓ **built** — header + Capture/Schedule/Start `StepView`s (plus.circle.fill/calendar.circle.fill/clock.fill, systemBlue) + black `PrimaryButton` "Continue →" |
| `PermissionsOnboardingView` | `Screen/Onboarding — Permissions` `551:49` | was missing | ✓ **built** — Voice Capture/Smart Scheduling/Stay focused `PermissionView`s (icon + title/desc + fillTertiary blue enable button) + black Continue |
| `OnboardingView` (container) | — | it's a paged `TabView` of Initial→ValueProp→Permissions, **not a separate screen** | ✓ n/a — the 3 screens above ARE the flow (page-dots implied) |
| `HomeLandingView` | Agenda `181:754` (shipping) | **RemUI Home is a simpler *prototype* than the shipping Agenda** — see divergences below | ? founder call which is canonical; **BottomToolbar tab bar built** `552:33` |
| `InboxView` | `Screen/Inbox` `206:703` | crux settled → rows follow **shipping `SharedTaskRow`**, not RemUI. The `TaskEventRow` **set `46:21` has an invented `Leading` axis** (`Time`/`Schedule`/`Clock`, glyphs calendar.badge.plus `U+10024A` + clock `U+10042B`) with **no basis in `SharedTaskRow`** (whose real axes are `showTimeIndicator`/`isEvent`/`isCompleted`+pills). Inbox instances point at **`Leading=Schedule`** (→ the stray calendar-badge-plus); Inbox is `showTimeIndicator:false` ⇒ **no leading column**, just **solid `circle`** + title (**headline**) + pills. The status circle was drawn as a **dashed VECTOR**. | ✓ **done (founder: "match code")** — set `46:21` reduced to a clean `Kind × Leading` matrix: retired the invented `Leading=Schedule`/`Clock` variants, added `Kind=task/event, Leading=None` (`574:2`/`574:15`), repointed all 5 Inbox instances to `Leading=None`, cleared the circle's dash. Inbox rows now render **solid circle + headline title** (verified). |
| `HistoryView` | **Chat Sessions** `438:15` | RemUI History = a *placeholder* ("Chat history"); shipping `ChatHistoryView` = the real sessions list `438:15` | (B) ✓ `438:15` canonical; RemUI is its empty-state placeholder |
| `SettingsView` | Settings `130:44` | RemUI = simpler proto (3 sections: **General / Date & Time / Integrations**); shipping = fuller grouped list | (B) ✓ `130:44` canonical; RemUI's grouping is an **IA idea → founder** |
| `TaskEventView` | Task detail `299:2` · New Task `404:5` | RemUI (title circle + Badges/DateInfo/AlertRepeat/Notes; editable "Set date, time, repeat" + bell Alert) ≈ shipping | (B) ✓ existing screens canonical; close match |
| `TaskInspectorSheet` | kit Inspector sheet `493:3199` | RemUI = date/alert/duration inspector (Alert/Duration rows + Cancel/Done) | (B) ~ map to kit Inspector sheet; build if a distinct one is wanted |

### Retired (confirmed-dead per RemUI README — "do NOT resurrect")
| Dead flow | Figma node | Action |
|---|---|---|
| Gateway/cloud **deploy** onboarding | `Screen/Onboarding — Setting Up (deploying)` `415:15` | ⌦ **retired** — relabeled, dimmed 0.4, moved to retired zone (y=1000) |
| **"start & chat"** device-preview | `Screen/Onboarding — Start Using Rem` `414:15` | ⌦ **retired** — relabeled, dimmed, moved to retired zone |

### Not in RemUI's canonical flow (founder-gated)
| Screen | Figma node | Note |
|---|---|---|
| Privacy — "Privacy by design" | `410:16` | RemUI flow is Initial → ValueProp → Permissions → Onboarding; no Privacy screen. Keep as a legal sub-screen or retire? — founder call. |

## Prototype ⇄ shipping divergences (founder call — RemUI is simpler than the shipped app)
The RemUI prototype and the shipping app disagree on the Home/Agenda surface. Which is canonical?
| Element | RemUI prototype | Shipping app (founder validated via screenshot) |
|---|---|---|
| DateNav | plain chevrons, no dashes | **three dashes packed by each chevron** (`AgendaView.swift:643`) — kept `43:2` per founder |
| Empty state | "No tasks scheduled" (icon + 2 lines) | "No agenda yet" + Add New/Schedule + **Suggestions** + See more (richer) |
| Bottom FAB | `mic.fill` (voice) | chat/voice bubble (message + waveform) |
| Schedule rows | `TaskCard` (TimeLabel + Category/Duration badges + FreeTimeCard) | `TaskEventRow` |
→ **Recommendation:** treat the shipping app as canonical for Agenda (founder validated it, it's richer), and
fold RemUI's clean components (TimeLabel, CategoryBadge, DurationBadge, FreeTimeCard, BottomToolbar) in where they
improve it. Confirm.

## Reconciled drift (track 2)
| Item | Figma node | Status |
|---|---|---|
| DateNav "three dashes" packed to iOS `DateNavigationHeader` code | `43:2` | ✓ |
| ContextualMessage unifies pairing + calendar-access | `537:31` · `537:41` | ✓ both built in RemContextualMessage form |
| ContextualMessage state placed at top of Agenda | Agenda `181:754` · banner `554:336` | ✓ pairing banner inserted after status bar / before DateNav, w/ Review-Reset actions |
| ContextualMessage component `73:39` actions-footer slot | `73:39` · `ButtonGroup 576:31` · instances `577:2`/`577:31` | ✓ **done** — added a boolean **`Actions`** property + a swappable **`ButtonGroup`** footer to all 5 Tone variants (matches `RemContextualMessage`'s `@ViewBuilder actions()` slot). Pairing (`577:2`, Tone=warning, Review/Reset) + calendar (`577:31`, Tone=info, Enable) are now **true instances** of the unified set, not bespoke frames. Supersedes the `537:31/41` RemContextualMessage-form frames. |
| CODE: `runtimePairingRecoveryCard` → adopt `RemContextualMessage` | `SharedRemChatView` L3083 | ? logged for code side |

## Chat scenarios (track 3) — see CLEANUP "Founder review 6" for the full list
Built: BrowserLiveCard `524:31`, Add-to-Chat sheet `525:2`, Composer states `527:2`, prompt/status `528:2`,
Developer pill `535:33`, pairing/calendar ContextualMessage `537:31`/`537:41`, **BottomToolbar tab bar `552:33`**,
**Browser takeover (live) `556:31`**, **Result cards `557:31`**, **Run activity + connection/skeleton `558:31`**
(ActionLifecycle Working/Worked timeline, waking skeleton, unreachable card), **Browser takeover (controlling)
`562:31`** — the paired field-control state: focused input on the surface (blue outline) + control-bar field
editor (fillTertiary field + return glyph) + "You have the controls" + full-width black "Give control back to
Rem", **AssistantMarkdown — code + table `566:31`** (`AssistantMarkdownRenderer.swift`: bold heading + `swift`
code block + real 1px-grid GFM table From/Subject/Time), **Device status card `566:68`/`566:78`**
(`ToolResultCardView.DeviceStatusCard`: battery glyph + tinted status pills + storage line; healthy + stressed).
**Track 3 chat scenarios: complete.** Known gap: the Figma file has no SF Mono/Menlo face, so the code block
renders proportional (grey container + indentation still read as code) — install SF Mono in Figma to close it.
