# Reconciliation map — Figma ⇄ canonical code

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
| `PermissionsOnboardingView` | — (missing) | not built | pending |
| `OnboardingView` (final) | — (missing) | not built | pending |
| `HomeLandingView` | — | not compared | pending |
| `InboxView` | `Screen/Inbox` (existing) | not compared | pending |
| `HistoryView` | `Activity` `413:32`? | not compared | pending |
| `SettingsView` | Settings `130:44` | not compared | pending |
| `TaskEventView` | `New Task` `404:5` | not compared | pending |
| `TaskInspectorSheet` | kit sheets | not compared | pending |

### Retired (confirmed-dead per RemUI README — "do NOT resurrect")
| Dead flow | Figma node | Action |
|---|---|---|
| Gateway/cloud **deploy** onboarding | `Screen/Onboarding — Setting Up (deploying)` `415:15` | ⌦ **retired** — relabeled, dimmed 0.4, moved to retired zone (y=1000) |
| **"start & chat"** device-preview | `Screen/Onboarding — Start Using Rem` `414:15` | ⌦ **retired** — relabeled, dimmed, moved to retired zone |

### Not in RemUI's canonical flow (founder-gated)
| Screen | Figma node | Note |
|---|---|---|
| Privacy — "Privacy by design" | `410:16` | RemUI flow is Initial → ValueProp → Permissions → Onboarding; no Privacy screen. Keep as a legal sub-screen or retire? — founder call. |

## Reconciled drift (track 2)
| Item | Figma node | Status |
|---|---|---|
| DateNav "three dashes" packed to iOS `DateNavigationHeader` code | `43:2` | ✓ |
| ContextualMessage unifies pairing + calendar-access (needs actions swap-slot on `73:39`; place in Agenda) | `73:39` · `537:31` · `537:41` | ~ |

## Chat scenarios (track 3) — see CLEANUP "Founder review 6" for the full list
Built: BrowserLiveCard `524:31`, Add-to-Chat sheet `525:2`, Composer states `527:2`, prompt/status `528:2`,
Developer pill `535:33`. Pending: browser-takeover sheet, result cards, action-lifecycle timeline,
connection/skeleton/tab-bar states.
