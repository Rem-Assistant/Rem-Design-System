# Reconciliation map — Figma ⇄ canonical code

## ⚠ CRUX DECISION (founder) — RemUI is systematically simpler than the shipping app
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
| `InboxView` | `Screen/Inbox` `206:703` | header + tray empty state **aligned** (identical copy); **rows diverge** (RemUI `InboxTaskCard` = circle + title + CategoryBadge, cleaner; existing = shipping `SharedTaskRow` style) | ~ align rows to RemUI once the crux (below) is decided |
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
| ContextualMessage component `73:39` actions-footer slot | `73:39` · example `560:6` | ~ **actions footer documented** as a labeled example beside the set (`73:39` is a COMPONENT_SET, variant-only); the true "Actions" variant/swap-prop on the set is a bounded follow-up. Actionable form is built (`537:31/41`) + placed in Agenda |
| CODE: `runtimePairingRecoveryCard` → adopt `RemContextualMessage` | `SharedRemChatView` L3083 | ? logged for code side |

## Chat scenarios (track 3) — see CLEANUP "Founder review 6" for the full list
Built: BrowserLiveCard `524:31`, Add-to-Chat sheet `525:2`, Composer states `527:2`, prompt/status `528:2`,
Developer pill `535:33`, pairing/calendar ContextualMessage `537:31`/`537:41`, **BottomToolbar tab bar `552:33`**,
**Browser takeover (live) `556:31`**, **Result cards `557:31`**, **Run activity + connection/skeleton `558:31`**
(ActionLifecycle Working/Worked timeline, waking skeleton, unreachable card), **Browser takeover (controlling)
`562:31`** — the paired field-control state: focused input on the surface (blue outline) + control-bar field
editor (fillTertiary field + return glyph) + "You have the controls" + full-width black "Give control back to
Rem". Pending: DeviceStatus card, AssistantMarkdown code/table.
