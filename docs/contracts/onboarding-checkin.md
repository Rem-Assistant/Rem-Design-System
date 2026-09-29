# Contract — Onboarding · Check-in cadence

**Outcome:** iOS + Android render the Check-in cadence step *identically* — same top lockup, same
grouped time-of-day list with switches, same bottom-pinned CTA that tracks the save lifecycle, same
icons + FILL. **Mode:** Reproduce (the founder onboarding reference frame
`tasks/refs/onboarding/04-checkin.png`) + Extend (the save-lifecycle states around it) + Systemize
(extracts the canonical `RemSwitch`).

**Authority:** the shipping `CheckinsService` / `Checkin` cadence model (RemClaw) and the founder
reference frame `tasks/refs/onboarding/04-checkin.png` (the "Saving…" state). This is a step **inside**
the onboarding sequencer — it carries the scaffold's back-nav and **bottom-pinned CTA**, like consent
(and unlike sign-in, which is its own centered screen). That difference is intentional, not drift.

Parent scope: #12 (onboarding middle steps) and #10.

## Layout — top lockup, grouped cadence list, bottom-pinned CTA

A scaffolded step (sequencer chrome: a back chevron in the nav, owned by the host, not the template).
Content top → bottom, **centered**, max content width 560:

1. **Hero badge** — the app-icon-style rounded square, `Color.brandBlue` fill, holding a centered
   white check-in glyph (registry `check-in schedule`: `clock.badge.checkmark.fill` / `alarm_on`,
   FILL 1). Centered near the top of the content area.
2. **Title** — "When should Rem check in?" — `Typography.largeTitle` semibold, `Color.labelPrimary`,
   centered, `Spacing.md` below the badge.
3. **Body** — "At each time you pick, Rem writes you a brief on what came in. Start with one; add more
   anytime in Settings." — `Typography.body`, `Color.labelSecondary`, centered, `Spacing.sm` below the
   title.
4. **Cadence list** — an inset grouped **`RemSection`** (`Color.backgroundSecondary`, radius `xlarge` /
   24, no outer stroke), full width, `Spacing.lg` below the body, three rows split by inset dividers.
   Each row: leading `ContainedIcon` (`.subtle`), the period title (`Typography.body` semibold,
   `labelPrimary`), and — trailing — a **value pill with the brief time, shown only while the row is on**,
   directly left of a **`RemSwitch`** (on-tint `Color.systemGreen`):
   - **Morning** — leading `sunrise` / `wb_twilight`; on by default at **8:00 AM**.
   - **Midday** — leading `sun.max` / `wb_sunny`; off by default (time 12:30 PM when on).
   - **Evening** — leading `moon.stars` / `bedtime`; off by default (time 8:00 PM when on).
5. **Primary CTA** — full-width filled (`Color.buttonBackground`, `Typography.bodyBold` inverted label,
   radius `medium`), **pinned above the bottom safe area**. Its label + treatment track the save
   lifecycle (see States). There is **no** legal footnote on this step.
6. **Recoverable-failure notice** (failure state only) — the shared `Color.systemRed` @ 12% card
   (leading `exclamationmark.triangle.fill` / `error`, FILL 1 + `Typography.caption1` message), directly
   **above** the CTA.

## States

| State | Content | CTA |
|---|---|---|
| **default** | cadence as loaded from `CheckinsService` (Morning on @ 8:00 AM, others off) | "Continue" — filled, enabled when ≥1 time is on |
| **edited** | the user changed a toggle (e.g. Midday also on @ 12:30 PM); not yet persisted | "Continue" — filled, enabled |
| **saving** | persisting; rows locked (non-interactive) | "Saving…" — leading spinner, disabled, 40% opacity |
| **saved** | persisted; rows locked | "Saved" — leading `checkmark`, disabled |
| **recoverable failure** | save failed; rows interactive again; notice card above the CTA | "Try again" — filled, enabled |

**Notice card:** `Color.systemRed` @ 12% fill, radius `medium`, leading warning icon (registry:
`exclamationmark.triangle.fill` / `error`, FILL 1) + `Typography.caption1` message — the same treatment
as sign-in's error/recovery notice, so the paired evidence reads as one card wherever it appears.

## Rules (the decisions that are easy to drift on — stated once)

- **Bottom-pinned action area:** this step pins the CTA (and, in failure, the notice above it) as a
  centered bottom action region; the lockup + list stay top-anchored. Contrast sign-in (centered, no
  bottom action area). The two screens differ on purpose; each is identical *across platforms*.
- **CTA lifecycle:** `default`/`edited` → "Continue"; `saving` → disabled "Saving…" + spinner; `saved`
  → disabled "Saved" + check; `failure` → enabled "Try again". The primary is one button whose label +
  leading affordance change; it is not five different controls.
- **"Start with one":** the CTA is inert in `default`/`edited` when **no** time is selected — the copy
  requires at least one. `saving`/`saved` lock it regardless; `failure` re-enables the retry.
- **Rows lock during `saving`/`saved`:** the switches are non-interactive so the persisted set can't
  change out from under the request; they are interactive again in `default`/`edited`/`failure`.
- **Switch on-tint is `systemGreen`** on both platforms (iOS `Toggle` tint / Android M3 `Switch`
  checked track) — the native settings-toggle color.
- **Value pill shows only while the row is on** and is **display-only** in onboarding; editing a time is
  a Settings concern ("add more anytime in Settings"), so no time-picker state is invented here.
- **Do not invent extra consent-style loading/empty states:** the only states are the five above.
- **Icons are semantic + FILL-pinned per the registry.** The hero is FILL 1; the three period leadings
  are FILL 0. Android renders the four check-in glyphs from the Material Icons vector set (Outlined /
  Filled) because they are not in the baked Material Symbols subset — a documented, FILL-honouring
  divergence (see `icon-registry.md` ‡).

## System use

- **Reuse:** the onboarding scaffold's bottom-pinned action area, `RemSection` + `ListRow`, the primary
  filled button treatment, `ContainedIcon` (hero + row leading), the shared error-notice treatment,
  tokens.
- **Systemize:** `RemSwitch` — the canonical on/off toggle (Figma `Switch` `110:50`), extracted here
  because a grouped-settings toggle recurs across Settings/automations/voice, not just this screen. On
  both platforms it is a thin wrapper over the native switch pinned to `systemGreen`.
- **Exact:** the top-lockup order + copy, the three cadence rows (icons + titles, in order), the switch
  on-tint, the value-pill-only-when-on rule, the bottom-pinned CTA, the CTA lifecycle labels, the notice
  placement (above the CTA), the icon registry rows + FILL.
- **Adaptable:** per-platform safe-area handling; the exact hero-badge size; the exact value-pill radius
  and fill; the leading-icon active tint (the reference's blue morning glyph is expressed via the switch
  and the shared `.subtle` tile so the icon color is not coupled to row state).
- **Excluded:** other onboarding screens (own contracts); the sequencer chrome itself; inline time
  editing / a time-picker (a Settings concern); the `CheckinsService` persistence + scheduling (host).

## Figma delivery (required — capability-pending)

Figma is part of a finished screen PR (founder, 2026-09-28; `docs/figma-delivery.md`). The editable
Check-in screen, its five states, and its place in the onboarding flow are to be authored in the
existing Rem file (`af4yDqCzp57jds9lkFiIaO`) on the shared `Onboarding` page, reusing the canonical
`Section` / `ListRow` / `Switch` (`110:50`) instances, the `Lockup` / `ActionArea` compositions, shared
variables, and text styles, following `SHAPE-OF-A-TASK.md` and the `rem-design-system` skill.

Native Figma canvas authoring requires the authenticated Figma MCP / Codex Builder runtime (a Full seat
with file edit access; see `docs/figma-delivery.md`). That connection is **not available on the hosted
build runner used for this delivery**, whose `FIGMA_TOKEN` drives export/drift only — so the canvas
write is a **Steward capability blocker**, not an omission. This PR therefore ships the complete paired
code, contract, registry, tests, and current-head render evidence; the editable Figma screen + prototype
flow + component ledger and the exact-head exports are the outstanding capability-gated step, to be
completed on the authenticated runtime before the founder spot-check. This contract is deliberately
registered as render-evidence-only in `tools/render-evidence/contracts.json` (no Figma `references`), so
the drift gate does not fail closed on a Figma artifact that the authoring runtime has not yet produced.

## Parity acceptance (what the visual gate diffs)

The paired render passes when iOS and Android match on **all** of:

- [ ] Hero = check-in badge (`clock.badge.checkmark.fill` / `alarm_on`, FILL 1), same placement
- [ ] Title + body: same text, same type roles, centered
- [ ] Cadence list: grouped card, three rows in order (Morning, Midday, Evening), same leading icons
      (outline) + same titles + trailing switches
- [ ] Switch on-tint is `systemGreen`; a row's value pill shows only while that row is on
- [ ] CTA bottom-pinned, full-width filled; label + affordance track the state
      (Continue / Saving… + spinner / Saved + check / Try again)
- [ ] Failure notice present in the failure state, **above** the CTA, same copy + role
- [ ] Icons match the registry glyph **and FILL** on both

**Evidence:** `screenshots.yml` paired table — `checkin-default-light`, `checkin-edited-light`,
`checkin-saving-light`, `checkin-saved-light`, `checkin-failure-light` — iOS ⟷ Android side by side.
Missing or unmatched required states fail closed.

**Amendment path:** founder for arrangement / product decisions; Builder may propose a bounded amendment
on the issue. **Status:** approved for Build (arrangement + states + icons per the reference); the paired
render is the proof the founder spot-checks; the Figma canvas author is capability-gated as above.
