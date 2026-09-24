# Rem Design System — Evolution Plan (Muse audit + product direction)

> **Context for future agents.** This captures the founder's direction for evolving the Rem design
> system beyond faithful reproduction of today's app — informed by an audit of **Meta's Muse** app
> (agent persona "Arlo"). It is strategy + open debates, not shipped fact. Screens are built in the
> live iOS 26 Figma file (`af4yDqCzp57jds9lkFiIaO`). Personal data from the source screenshots is
> **never** reproduced here or in Figma — use placeholders.

## Thesis: design-first product development

Design is the **cheap iteration surface**. Nail the frontend in Figma (fast to iterate, review, and
reason about), *then* define the backend to serve it. The design system is the source of truth for
the product's UI vocabulary; the **design→code translation** must stay easy (Code Connect, the
snapshot drift check in `tools/design-sync/`, and the Fluent-style `TokenSet` structure — see
`RemClaw:docs/architecture/component-architecture-opportunities.md`). Foundation first, then product.

## Two partitioned tracks (never blur as-built with proposed)

- **FOUNDATION** — the faithful mirror of shipping code (verifiable screenshot-vs-code, drift-checkable)
  **plus** the component-architecture upgrades. This stays the trustworthy source of truth.
- **EVOLUTION (Proposed)** — net-new, not-yet-shipped surfaces, on their own clearly-labeled pages.
  A Proposed screen graduates into Foundation only when the code ships it.

---

## Muse audit → what we adopt / adapt / already have / avoid

**The headline:** Muse is an *agentic, human-in-the-loop, transparency-first* app. Its biggest deltas
vs. Rem are an entire **agentic surface layer** we don't model yet — not "polish our components."

### Adopt (net-new Proposed surfaces)
1. **Agent avatar + live status vocabulary in chat.** A photo/mascot with a **glass status pill**
   under it cycling through a state machine: `Generating PDF` · `Reviewing guidance` · `Working` ·
   **`Needs approval`** · **`Needs you`**. We have nothing like it — highest-signal AI-native move.
2. **Agent hub (opened by tapping the avatar).** Muse's profile is a segmented control
   (Activity / Permissions / History / Identity). Adopt this as the home for agent-scoped surfaces —
   see IA proposal below.
3. **Execution trace** (entry point = tapping the agent photo). A step **timeline grouped by agent**
   (`MAIN`, `SUBAGENT 01`), each step = status icon (green ✓ / red ✗) + bold title + gray description
   + disclosure chevron, joined by a dashed rail, ending in a live "Working" footer. This is the
   "show your work" surface; our task activity is one-comment-deep by comparison.
4. **In-chat approval gate** (permission pattern). "Allow Arlo to …?" card with a **Details** raw-command
   block and **Allow (commit) / Always allow / Deny**. Beyond our ProposalCard.
5. **Action cards** — agent drops a mini-card *into the chat stream* requesting input (e.g. "Secure
   Store · Create a password for …" with an Add CTA), with **active → completed ("Added", green ✓)**
   states.
6. **Running-task banner** — a Live-Activity-style pill ("Browser · Needs you · …" + Stop) for a
   background agent task. The agent-task analog of our voice mini-player.
7. **Browser takeover (human-in-the-loop)** — surface the live embedded browser with
   **Take control / Stop the task** + coachmark, when the agent hits something it can't do.
8. **Connector-consent pre-screen.** Before connecting a tool: logo tile + name + tagline + **3
   icon-rows (icon + title + description)** + legal + gradient Connect. We have no pre-screen today.
9. **Per-capability permission menu** (`Ask` / `Allow` / `Deny` pull-down) on settings rows — richer
   than our grant/deny status badge.
10. **Empty-state-with-CTA** — `ContentUnavailableView` should carry an optional action slot (Muse's
    "No info saved → Add login info").

### Already have / validates our direction
- Hero (icon tile + title + subtitle), grouped ListGroup cards, grouped forms, centered empty states,
  the connector-row pattern (`SharedComposioConnectionsView`).

### Button emphasis tiers (adopt as a cross-cutting axis)
Muse's primary CTA reads in tiers: **gradient = the commit/authorize action** (Allow, Connect, Take
control, Add-credential) · **solid blue = standard/entry** (Add login info) · vs. our black `.label`.
→ Make **emphasis** a cross-cutting dimension in the Button `TokenSet`, available along *every* style,
not a separate style. (Founder: "I like the tiering system … it could exist along every style.")

### Avoid / debate (don't blindly copy)
- Gradient everywhere would fight iOS-native restraint — reserve gradient for the commit tier only.
- Emoji reactions on messages — likely out of scope for Rem.

---

## Information architecture proposal (answers "where does Wallet go?")

Today "everything is in REM." Muse suggests a cleaner split: **app/account settings** vs
**agent-scoped capabilities**, with the agent surfaces reached from the **avatar**, not buried in Settings.

- **Agent hub** (tap avatar → segmented control):
  - **Activity** — execution trace / agent action log.
  - **Permissions** — browser permissions (Ask/Allow/Deny menu) + connector permissions.
  - **Wallet & Credentials** — **Wallet** (payment methods the agent uses) + **Secure credentials
    store** (login info) + the **cloud browser** surface. These are all "capabilities the agent uses,"
    so they cluster here rather than in app Settings.
  - **Identity** — agent name/photo/persona.
- **Settings** becomes app/account (**rename "Rem" → "General"**; the `Rem + Connected` runtime row
  made sense under OpenClaw but the agent identity now lives on the avatar/hub). Cloud-browser view in
  Settings is the natural home for **Secure credentials** and **Browser permissions** (attach there).
- **Wallet placement decision:** under the Agent hub's *Wallet & Credentials* section (not top-level,
  not app Settings) — it's an agent capability.

> Verify the existing entry points in code before moving anything: the **cloud browser settings view**
> and where the runtime "Rem" row lives (`SharedSettingsView` / `SettingsView`). Founder noted the
> cloud browser view already exists in Settings.

---

## Chats tab (to design + study — we have NOT designed this yet)

We have a Chats/sessions tab today but haven't designed it in the system. Muse's model:
**one "Main chat" (the orchestrator) + optional "Side chats" (topic-organized)**, with a clean
empty state ("Start a side chat …"). Founder is debating a **main orchestrator chat + side chats**
model — study Muse's pattern before committing. Design the Chats tab in the Proposed track.

---

## Open debates (resolve later → then move to code / DECISIONS.md)

- **Deprecate the Calendar / Reminders tool-result cards?** Founder: "could probably be solved by
  text … got them from a Codex reference." Leave built for now; flag as a candidate for removal if
  text renders these well enough. (Code: `CalendarCard.swift`, `RemindersCard.swift`.)
- **Composio in the consent pre-screen?** Should the pre-screen say we connect via **Composio**?
  Default **no for now** — Composio is still on the table and may change; don't bake the vendor into
  user-facing copy. Revisit when the connector backend is settled. (Documented here so the decision
  has context; when settled, record in `RemClaw:docs/product/DECISIONS.md`.)

---

## Component-architecture direction (foundation)

Per `RemClaw:docs/architecture/component-architecture-opportunities.md`: adopt Fluent's
**folder-per-component + first-class `TokenSet`** (structure / tokens / style separated), starting
with the Button system, and generalize to `RemCard` / `RemBanner` / `StatusDotBadge`. The Button
emphasis tiers above live in `RemButtonTokenSet`.

---

## Near-term build order (once Figma reconnects + goal is on)

**Foundation:** finish the shipping screens (Login, onboarding + Privacy, Sign-out/Delete confirmations,
View-history, task/event state variants, settings-page consolidation, nav map) → land the Button
emphasis-tier axis + TokenSet structure.
**Proposed track (new pages, labeled "Proposed"):** agent avatar + status pill → execution trace →
Agent hub IA → approval gate + action cards → running-task banner → browser takeover → connector
consent pre-screen → per-capability permission menu → Wallet → Chats tab (main/side).

Self-verify every deliverable (screenshot + property read + code check for fidelity claims);
log-don't-fix debt in `CLEANUP.md`; keep `REGISTRY.md` + Code Connect in sync; commit + push; leave
written review summaries (founder is often away).

## Build status (2026-09-24 autonomous run)

**FOUNDATION — complete.** Button emphasis tier (Neutral/Standard/Commit) `377:8`; onboarding flow
(Login `411:15`, Privacy `410:16`, Deploying `415:15`, Activation `414:15`); New Task `404:5`;
View history `413:32`; Sign Out `412:15` + Delete Account `412:24`; task state badges
(RunStatus `417:60`, Deemphasis `419:41`, Overdue `419:42`, Priority `419:48`); settings-page
consolidation; nav map `420:15`; RemFaceMark component fix; Component Index synced.

**PROPOSED — 9 of ~10 built** (on `Proposed · Agent surfaces`, clearly labeled, code-unverified):
AgentStatusPill `427:21`, ActionCard `428:37`, ApprovalGate `429:20`, Execution trace `431:21`,
RunningTaskBanner `432:39`, Connector consent pre-screen `434:21`, MenuValue permission accessory
`435:22`, ContentUnavailableView+CTA `436:67`, Browser takeover `437:68`.

**Remaining — founder-gated:**
- **Chats tab** (main orchestrator + side chats). Founder is still deciding the model ("haven't thought
  this through … could study Muse's pattern"); the as-built version also needs a `ChatHistoryView.swift`
  read + a few new glyphs (chat bubble, compose, archive). Not built — awaiting the model decision.
- **IA/placement**: Agent hub structure, Wallet home, Settings "Rem"→"General" — held.
- **Debates**: deprecate Calendar/Reminders cards?; Composio in the consent pre-screen?; drop the deploy
  step (still in code)? — held.

_Seeded 2026-09-24 from the Muse audit (15 screenshots) + founder direction._
