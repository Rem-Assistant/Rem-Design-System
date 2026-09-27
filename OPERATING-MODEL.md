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
| **Orchestrator** | This Claude session | Shape issues, close system gaps, drive PRs to green, run/repair verification, keep this map current | Build features or arrange Figma by hand |
| **Builder** | `agent-factory-builder[bot]` | Implement code (+ Figma) from an issue; produce render **evidence** | Decide product/IA; approve or merge |
| **Reviewer** | `agent-factory-reviewer[bot]` | Judge the diff **and the render** (once visual evidence is on) | — |
| **Steward** | `agent-factory-steward[bot]` | Integration policy, landing authorization | Merge without gates passing |

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
  → Builder builds code + iOS/Android render evidence
  → Reviewer judges diff + visual evidence          ← the gate that catches fidelity
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
- **State:** the five `visual_evidence` flags in `.agent-factory/config.json` were **off** (that's why
  the facemark-vs-app-icon bug passed review). Config now enabled + **validated against the pinned
  Factory ref**. Remaining: a render→delivery pipeline (`verify` + `publish-builder-delivery`),
  mirroring swami's hardened publisher, in the "deliver renders for Reviewer judgment" model (no
  SSIM gate). **Gate #1 — nothing else fans out until this catches things.**

---

## 7. Gap register (prioritized)

| # | Gap | Owner | Status |
|---|---|---|---|
| 1 | Reviewer can't see renders → fidelity bugs pass | Orchestrator | config validated; delivery pipeline in progress |
| 2 | Layers implicit → "system vs use case" confusion; "where does X go" unanswerable | Orchestrator + product-design | this doc starts it; rename patterns toward shape |
| 3 | Only onboarding is filed; the rest of the app isn't tracked | Orchestrator files / Builder builds | pending **gate #1** (don't mass-build blind) |
| 4 | Two live fidelity bugs: sign-in shows the **app icon** not the face; **Google icon** must transfer from RemClaw | Builder | identified — the **first test** the loop must catch |
| 5 | Onboarding modeled as a monolith, not named patterns | Orchestrator + product-design | task #38 |

---

## 8. The goal

**Rem's core surfaces ship through the Factory, machine-verified — the Director approves, doesn't QA.**

- **Proving milestone (do first):** the sign-in fidelity fix goes issue → Builder → **the Reviewer
  catches the app-icon/Google-icon miss from the render** → merges, and you never eyeball it. One
  view through the full loop = wheels off, proven.
- **Coverage:** every core surface (onboarding steps, chat + states, agenda, settings/automations,
  task detail) exists as a **pattern in code + iOS/Android render + Figma page + Code Connect**,
  each an issue, burned down.
- **The metric that matters — first-pass fidelity:** % of Factory-built views that merge without you
  filing a visual correction. Today ≈ 0 (you catch everything). Wheels are off when it's
  consistently high and you're spot-checking, not driving.
