# Rem — Operating Model

*How this design system gets built, documented, and verified — and who does what — so new
views become cheap and the human stops being the QA. This is the 30,000-ft map; the per-task
detail lives in `SHAPE-OF-A-TASK.md`, `REGISTRY.md`, and the skills under `.claude/skills/`.*

---

## 1. The mission, in one line

Take a scattered app (**RemClaw**) → extract a **cross-platform design system** (iOS + Android
from one token/primitive source) → **document it faithfully in Figma** → run it through the
**Agent Factory** so any view can be filed as an issue and shipped **built + machine-verified**,
without the human hand-checking each one.

Onboarding is the *first* case — the one we use to shake out the machine. The goal is not
"finish onboarding," it's **make the machine trustworthy enough to build the next 20 views.**

---

## 2. The layering — "is it a system, or a codified use case?"

A real design system is layered. A "codified use case" is a bespoke screen wearing a component's
clothes. The test: *can privacy, terms, and a future consent screen all be the same pattern with
different content?* If yes, it's a system. Today we are **halfway** — the code generalizes, the
naming and registry don't yet make the layers explicit.

| Layer | What it is | What exists today (verified) |
|---|---|---|
| **Tokens** | The spine: color/space/type/radius, one source | `tokens.json` → `DesignTokens.generated.swift` + `RemTokens.kt` |
| **Primitives** | Atomic, content-free, reused everywhere | `RemButton`, `ListRow`, `ContainedIcon`, `RemFaceMark` (code); Avatar, Pill, badges (Figma) |
| **Patterns** | A screen *shape* with slots, not content | `OnboardingConsentTemplate` (parameterized: `heroSymbol`/`title`/`message`/`legalItems`/…); screen-templates in `agent/props.json` (Agenda, Settings, Chat, Conversation, Inbox, OnboardingSequencer) |
| **Instances** | A pattern + real content | **Privacy** = the consent pattern's default content; **Terms** = another instance |
| **Flows** | *Documentation only*, in Figma | onboarding sequence as a documented flow — **not** app runtime navigation |

> **Naming rule:** a pattern is named for its *shape* (`ConsentTemplate`, `SettingsGroupScreen`),
> never its first instance (`PrivacyScreen`). Rename toward shape as we go — that's what turns a
> use-case into a system.

### The "where does X go?" decision rule

Walk down the layers and stop at the first that answers:

1. **Is it a new atom?** (a control that doesn't exist) → new **primitive**.
2. **Is it a new screen shape?** (a layout no pattern covers) → new **pattern** (a product-design call).
3. **Otherwise it's content** → a new **instance** placed on an existing pattern/screen.

Worked examples:
- **Wallet** → not an atom, not a new shape. It's an **instance** of the **settings-group screen**,
  a new section on the Settings/automations/voice surface. *(If you later want the Muse-style
  dedicated wallet surface, that's step 2 — a new screen pattern, and a product-design decision.)*
- **Chat states** (thinking, streaming, error, tool-result) → **states/variants** on the existing
  chat pattern, documented as instances in Figma. Not new components.

---

## 3. Roles — who does what (the human stops driving)

| Role | Who | Owns | Does NOT |
|---|---|---|---|
| **Director** | You (Samuel) | Direction, taste calls flagged to you, final approve, spot-checks | QA every render; drive each PR |
| **Orchestrator / Steward** | `agent-factory-steward[bot]` + Factory workflows | Shape and dispatch issues, own durable work state, route build/review failures, arbitrate evidence, and authorize landing after gates pass | Delegate routine QA to the Director; bypass failing gates |
| **Builder** | `agent-factory-builder[bot]` | Implement code (+ Figma) from an issue; produce render **evidence** | Decide product/IA; approve or merge |
| **Reviewer** | `agent-factory-reviewer[bot]` | Judge the diff **and the render** (once visual evidence is on) | — |
| **Factory maintainer** | Codex / Claude development sessions | Repair reusable Factory machinery and consumer wiring; prove fixes through the loop | Become the permanent driver of each screen |

The correction that produced this table: **Figma/feature building is the Builder's lane, not the
Orchestrator's.** The Orchestrator sets the machine up and keeps it honest.

---

## 4. The skills — each one's unique value

| Skill | Fires when | Unique value |
|---|---|---|
| **product-design-delivery** | A change needs a UX/visual decision, or must reproduce an existing design faithfully | Decides the **contract before code**: IA ("where does wallet go"), the state set, the taste calls (and which are *yours*) |
| **rem-design-system** | Any Figma / token / component / SF-Symbol work | Builds & maintains the **Figma library** and keeps Figma == code; the anti-drift discipline + plugin gotchas |
| **verified-delivery** | Standing up tracking, or running the build→verify loop | The **tracking + two-gate method**: issues → Builder builds → an agent drives the real product & captures evidence → human spot-checks. *This is the skill that takes the training wheels off.* |

They compose: **product-design shapes → rem-design-system builds it in Figma/code → verified-delivery
proves it shipped right.**

---

## 5. The task shape (one view, end to end)

```
Issue (pre-coding summary: upstream pattern · user outcome · in/out of scope)
  → Builder builds code + editable Figma screen/states/flow
  → Verify captures current iOS/Android renders + Figma exports/structure
  → Reviewer judges diff + Figma + both platform renders
  → Steward integrates (policy passes)
  → Director approves / spot-checks
```

The **pre-coding summary** (from `CLAUDE.md` principle 4) is the issue's spine: what upstream/RemClaw
pattern it mirrors, the user outcome in user words, and explicit in/out of scope.

---

## 6. Verification loop — the training wheel we're removing

- **Training wheel (today):** a screenshots comment for *your* eyes; the Orchestrator eyeballs; you
  catch the bugs.
- **The bike:** the **Reviewer** sees the renders. Mechanism (Agent Factory): the Builder publishes a
  head-bound **delivery** of runner-produced renders; the Reviewer fetches them and judges fidelity.
- **State:** the render and authenticated delivery pipeline exists. The previous Factory
  silently supplied only its first six images to Reviewer, which could exclude Android.
  The recovery update removes that truncation, supplies the approved contracts to agents,
  publishes one paired current-head table, and enables visual feedback for review,
  revision, and arbitration. **These changes require live validation after adoption;
  configuration and unit tests alone do not prove the loop.**
- **Delivery ownership:** the trusted publisher updates the PR's canonical Delivery section
  after each render. A new delivery triggers review; stale/missing evidence waits within
  the configured deadline and remains blocking if no valid replacement arrives.
- **Final authority:** `automatic_promotion: false` retains Director sign-off. Steward must
  bring the current head to a green, evidence-backed decision; no force-merge past a red gate.

---

## 7. Gap register (prioritized)

| # | Gap | Owner | Status |
|---|---|---|---|
| 1 | Reviewer silently received six images, potentially excluding Android | Factory maintainer | fix prepared; live validation pending |
| 2 | Builder lacked failing CI diagnostics | Factory maintainer | bounded exact-head diagnostic handoff prepared |
| 3 | Delivery went stale during fresh renders; two competing evidence surfaces | Steward workflows + publisher | bounded waiting, paired canonical delivery, and re-review update prepared |
| 4 | Contract and icon registry were linked but absent from Reviewer briefing | Consumer configuration | explicit context added; live review pending |
| 5 | Consent Figma authoring and verification | Builder + Reviewer, coordinated by Steward | canvas authored by the authenticated Codex Builder; exact-head structure/export review pending |
| 6 | Additional screens | Steward after proving the complete consent loop | deferred |

---

## 8. The goal

**Rem's core surfaces ship through the Factory, machine-verified — the Director approves, doesn't QA.**

- **Proving milestone (do first):** consent #30 goes issue → Builder (code + Figma) → platform renders + Figma evidence → Reviewer →
  automated correction (where needed) → green Steward integration → Director approval.
  No manual feature repairs or stale-evidence bypasses count as proof. Three consecutive
  screens meeting this standard are the later scaling checkpoint.
- **Coverage:** every core surface (onboarding steps, chat + states, agenda, settings/automations,
  task detail) exists as a **pattern in code + iOS/Android render + Figma page + Code Connect**,
  each an issue, burned down.
- **The metric that matters — first-pass fidelity:** % of Factory-built views that merge without you
  filing a visual correction. Baseline not measured. The loop is ready to scale when it is
  consistently high and you're spot-checking, not driving.

### Figma scope clarification — 2026-09-28

The Director explicitly requires Figma changes as part of a finished screen PR. Consent
is the proving case for that complete delivery. Earlier notes deferring Figma do not
apply to consent. This does not authorize additional screens or change product decisions.
See `docs/figma-delivery.md` for the acceptance contract and current proving status.
