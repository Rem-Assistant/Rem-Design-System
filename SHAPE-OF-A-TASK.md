# Shape of a Rem design task

The **Rem binding** for a design-system task. It does not restate the process — it points to the
process and fills in Rem's specifics. Read it together with:

- **`product-design-delivery`** (agent-skills) — the process: classify (Reproduce / Extend / Design /
  Systemize), explore at the right fidelity *within the system*, produce a **design-ready task
  packet**, hand off, review against version-bound evidence. **This is the loop.**
- **`verified-delivery`** (agent-skills) — tracking + the two-gate verification (GitHub as the record).
- **`rem-design-system`** (this repo's skill) — the Rem specifics the process points at: the Figma
  file, tokens, component/screen tracks, and the plugin gotchas.
- Runs under **Agent Factory** (Steward → Builder → Verify → Reviewer → Gate). **Builder's PR
  description is the canonical delivery record**; Verify publishes evidence into it and Reviewer fails
  closed on missing/stale evidence.

## Where a task starts
**One screen** — its flow and the components inside it — picked from **`VIEW-MAP.md`** (the verify
map). Classify it per `product-design-delivery` before building. Components are extracted as a
byproduct when the screen needs them; we don't ship lone components.

## Rem's local context (fills product-design-delivery's blanks)
- **Authority:** the shipped SwiftUI app + the Figma file (`af4yDqCzp57jds9lkFiIaO`, organized per
  `FILE-ORG.md`). When they disagree, the shipped app wins and the conflict is surfaced, not silently
  resolved.
- **Ground truth for fidelity:** `visual-verify` screenshots (below).
- **Approver:** the founder. An agent may recommend a direction; it never approves its own.
- **One design system, generated per platform** (see `SPEC.md`): `tokens.json` → `DesignTokens.swift`
  (SwiftUI) **and** `RemTokens.kt` (Compose). Clients consume generated tokens; they don't hand-roll a
  theme.

## What a task delivers (Rem Definition of Done)
1. **Code — both platforms, token-driven.** SwiftUI **+** Compose from the generated tokens. New
   components extracted with **TokenSet + Code Connect** (the `RemButton` / `ContainedIcon` pattern) —
   reused, not re-invented.
2. **Figma.** The component placed per **`component-track`** — master + variants + a **Preview** tile.
   The annotated **Anatomy** and full spec are the **founder's manual plugin step, not the agent's** —
   a task ships the component and a preview, not the anatomy. The screen built per **`screen-track`**
   on its own page (`FILE-ORG` SCREENS band); its **flow** on the Flows page, wired so Present works.
   Registered in `REGISTRY.md` + the Figma Component Index.
3. **Evidence in the PR (the delivery record).** `visual-verify` renders the screen on **iOS and
   Android** (light + dark) and publishes the screenshots into the PR. Screenshots prove states;
   recordings prove transitions. A green build or a bare link is never proof by itself.
4. **The record.** One screen per PR: the diff; Figma links (screen · flow · component page); the
   iOS + Android screenshots; a **component ledger** — each component: name · category · link ·
   reused vs newly added.

## Not part of the agent task
- The annotated **Anatomy** / full component spec — founder, manual, via plugin.
- Product direction — chosen by the design authority, not invented mid-build.
