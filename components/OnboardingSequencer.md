---
component: OnboardingSequencer
group: general
kind: screen-template
mirrors: OnboardingFlow.swift (signIn → dataSharingConsent → …) — the native first-run sequencer
status: draft
composed_of: [ContainedIcon]
pending_native: [Button, ListRow]
rules:
  - id: no-deploy-step
    do: "Compose the path from the ordered step slots you pass (Sign-in → Consent → the middle steps)."
    dont: "Reintroduce a deploy/provisioning step anywhere in the sequence — the runtime migration removes it."
    enforced_by: "prose-only (the sequencer injects no steps of its own; the path is exactly its `steps`)"
  - id: reproduce-signin-consent
    do: "Keep the sign-in and consent copy + Sign-in-with-Apple treatment exactly as the authoritative reference (`tasks/refs/onboarding/01-sign-in.png`, `02-consent.png`)."
    dont: "Reword consent copy, restyle the Apple button, or invent a visual treatment for the reproduced steps."
    enforced_by: "prose-only (Reproduce mode — fidelity is verified against the reference frame)"
  - id: real-auth-no-mock
    do: "Drive sign-in from the host's real auth by mapping it to `SignInState` (returning / new / checking / error / recovery); advance only on success."
    dont: "Fake-advance from the button or hard-code a signed-in state in the component."
    enforced_by: "prose-only (the step is state-driven; the host owns auth + advancement)"
  - id: token-bound
    do: "Bind every visual to the generated tokens (RemTokens / DesignTokens) and reuse the canonical ContainedIcon for hero + row leadings. The CTA button and the consent legal rows are hand-rolled natively (no Compose Button / ListRow primitive exists yet) — keep them token-bound too."
    dont: "Introduce call-site literals, or claim canonical Button / ListRow reuse the Compose surface does not perform."
    enforced_by: "SwiftLint ds_* rules (Swift) + oxlint no-raw-hex (web); prose for Compose until a lint lands"
---

# OnboardingSequencer (screen template)

## Overview
The native first-run **sequencer shell**: it drives an ordered flow (progress + Continue/Skip, forward
and backward) and hosts each step in a shared scaffold. It mirrors `OnboardingFlow.swift` — **minus the
legacy deploy/provisioning step**, which the runtime migration removes from the path (the deploy code
itself stays in the app; it is simply never a slot here). This slice ships the shell plus the two
reproduced steps (Sign-in, Consent); the middle steps (Connectors → Check-in → Voice) plug in as
additional ordered slots from issue #12.

## When to use
- As the **container for the first-run journey** — pass it the ordered step slots and it handles order,
  progress, and Continue/Skip/back.

## When *not* to use
- **A single standalone screen with no sequence** → render the step's scaffold directly.
- **A settings sub-flow** → use the settings navigation, not the onboarding sequencer.

## Anatomy
Shared chrome (the `OnboardingScaffold`): a **back** chevron (hidden on the first step) + an **Extend**
progress indicator → a centered **hero** (`ContainedIcon`), **title**, **subtitle** → a scrollable
**content** slot → a bottom-pinned **CTA bar** (a primary button + optional secondary Skip / link —
hand-rolled today, not the canonical `Button`; see *Composed of*) with
an optional **legal footer**. Each step supplies its own content and CTA labels; the sequencer supplies
order, progress, and navigation.

## Composed of
**Reused (canonical):** [`ContainedIcon`](ContainedIcon.md) (hero + consent-row leading).

**Pending native (hand-rolled, not reused):** the CTA button and the consent legal rows are
**not** instances of the canonical [`Button`](Button.md) / [`ListRow`](ListRow.md) — no Compose
primitive exists for either yet, so the shipped surface hand-rolls them token-bound
(`OnboardingActionButton` in `OnboardingScaffold.kt`; `ConsentLegalRow` in `ConsentStep.kt`) and
flags them for extraction. Treat both as forked-pending-native, not as a canonical reuse.

Steps: **Sign-in** (`SignInState` = returning / new / checking / error / recovery) and **Consent**
("Privacy by design").

## Variants & states
| Region | Values |
|---|---|
| Sign-in state | `returning` · `new` · `checking` · `error` · `recovery` |
| Consent | default · accepting (disabled "Accept and Continue") |
| Navigation | first step (no back) · mid-flow (back + Skip) · last step (advance completes) |
| Appearance | light · dark (both bound to tokens) |

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Compose the path from the ordered slots you pass. | Reintroduce a deploy/provisioning step. |
| Keep sign-in + consent copy/treatment 1:1 with the reference. | Reword consent copy or restyle the Apple button. |
| Drive sign-in from real auth via `SignInState`. | Fake-advance from the button or mock a signed-in state. |
| Bind visuals to tokens; reuse canonical `ContainedIcon`. | Add call-site literals, or claim `Button`/`ListRow` reuse the Compose surface hand-rolls. |

## Accessibility
- Back and every CTA carry button semantics + an accessibility label; the hero glyph is decorative.
- Content scrolls so long copy (consent) never clips; the CTA bar stays reachable.
- Sign-in-with-Apple label is the button's accessibility label; error/recovery messages are read inline.
- Contrast follows the reproduced treatments (filled `buttonBackground` with inverted label).

## Tokens used
- `color.buttonBackground`, `color.brand.blue`, `color.background.{primary,secondary}`,
  `color.label.{primary,secondary,tertiary}`, `color.separator`, `color.fillTertiary`, `color.system.red`
- `radius.large` / `radius.medium`, `spacing.*`, `typography.roles.{largeTitle,title1,body,bodyBold,subheadline,footnote}`

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `steps` | ordered slots | — | Sign-in → Consent → (#12 middle steps); no deploy slot |
| `showProgress` | `bool` | `true` | Show the step progress indicator (Extend) |
| `onComplete` | `action` | — | Fires when the last step advances |

_Compose surface: `OnboardingSequencer(steps, state, showProgress, onComplete)` with `signInStep(state:
SignInState, …)` and `consentStep(onAccept, onOpenTerms, onOpenPrivacy)` step builders
(`compose/RemDesignSystem/onboarding/`). SwiftUI mirror: `OnboardingFlow.swift` (deploy step dropped
from the path)._
