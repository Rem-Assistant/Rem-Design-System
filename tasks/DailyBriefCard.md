# Task: DailyBriefCard — the brief entry point (stateful)

## Outcome
- **User outcome:** From the Agenda, the person can open, listen to, stop, and re-open their daily
  brief — the entry point communicates its own state instead of looking static.
- **Scope:** One reusable `DailyBriefCard` component (master + state variants + preview) in Figma +
  Code Connect. Not the Agenda template, not the playback engine.
- **Mode:** Systemize (a reusable component with a full state family; it already ships in code).
- **Source concern:** `TARGET-COMPONENTS.md §Gaps #1`; founder called this out explicitly as the
  "brief entry point as a component with states." It is the **Code Connect reference implementation**.

## Design authority
- **Product authority:** `Rem/Sources/Components/DailyBriefCard.swift` (shipping) + its README entry in
  `Rem/Sources/Components/README.md` (authoritative behavior: header = authored `DailyBrief.briefHeadline`,
  falling back to the time-of-day greeting; separate open-chat vs Read/Stop actions; completion receipt
  flips Read → Read again).
- **Design-system authority:** `Card`/`Surface` (container), `Button` (actions), `ContainedIcon`,
  `Pill` (count capsules), tokens; `component-track` page format.
- **Task artifact:** none yet — this task creates the Figma master. The founder flagged "Wednesday
  Evening" header renders **too large** in the current hand-built Agenda; the correct header size is
  whatever `DailyBriefCard` uses in code, not the oversized Figma value.
- **Conflicts resolved:** header uses the code's font role (reduce from the oversized Figma). The
  Agenda "Read latest brief" **emoji is a Figma-only artifact** — use the code's SF Symbol.

## Approved experience
- **Composition:** header (icon + headline/greeting) → optional brief summary/count capsules →
  action row (open chat · Read/Stop) on a `Card`.
- **States (the point of this task):** `Read latest brief` (default) · `Stop` (playing, mic-muted) ·
  `Read again` (after the completion receipt) · `Retry` (synthesis/player failure) · plus
  loading and "no canonical brief" (action hidden) states.
- **Interaction:** Read starts playback (mic muted, trailing Stop); completion unmutes + records the
  receipt → Read again; intentional Stop hands to listening without a receipt; Retry re-validates.
- **Content:** headline from the artifact; greeting fallback ("Wednesday Evening"); action labels above.
- **Accessibility:** accessibility label uses the resolved brief prose; reduced-motion for shimmer.

## System use
- **Reuse:** `Card`, `Button`, `ContainedIcon`, `Pill`, tokens.
- **Extension:** none expected — should compose from existing primitives.
- **Exact:** the four action states + their transitions (from code).
- **Adaptable:** capsule layout, spacing within token scale.
- **Excluded:** the Agenda template, the voice/playback engine, the MiniPlayerBar (its own packet).

## Delivery contract
- **Known constraints:** state is driven by playback + completion-receipt in code; `PlaybackController`
  owns transitions. On Android, Compose form uses Material container; intent identical.
- **Acceptance:** master + all state variants render from one master; Code Connect maps Figma →
  `DailyBriefCard`; header size matches code; no emoji (SF Symbol).
- **Evidence:** iOS + Android screenshots of each state, light + dark; Preview tile; Code Connect chip.
- **Amendment path:** design authority (founder) rules on any state the code exposes that the design misses.

## Approval
- **Status:** approved for Build (Reproduce/Systemize from shipping code; no open product decision).
- **Approver:** founder. **Open decisions:** none.
