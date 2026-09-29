# Contract — Onboarding · Check-in cadence

**Outcome:** iOS + Android render the Check-in cadence step *identically* — same top lockup, same
grouped time-of-day list with switches, same Body-owned action area that tracks the save lifecycle, same
icons + FILL. **Mode:** Reproduce (the founder onboarding reference frame
`tasks/refs/onboarding/04-checkin.png`) + Extend (the save-lifecycle states around it) + Systemize
(extracts the canonical `RemSwitch`).

**Authority:** the shipping `CheckinsService` / `Checkin` cadence model (RemClaw) and the founder
reference frame `tasks/refs/onboarding/04-checkin.png` (the "Saving…" state). This is a step **inside**
the onboarding sequencer — it carries the scaffold's back-nav and **Body-owned ActionArea**, like consent
(and unlike sign-in, which is its own centered screen). That difference is intentional, not drift.

Parent scope: #12 (onboarding middle steps) and #10.

### Steward resolution of prior review ambiguity (2026-09-29)

This contract is the final interpretation for this delivery and supersedes contrary remarks from
earlier reviewer passes over the same reference digest:

- "Centered" describes horizontal alignment inside `VStack/Content`; it does not vertically center
  Content and Actions together. Content is top-aligned and Actions are bottom-pinned.
- Saving and Saved lock **and visibly de-emphasize** the cadence rows. The Button uses its semantic
  disabled variant; row de-emphasis uses the shared opacity token.
- Native icon parity means the named SF/Material registry pair at the pinned FILL and comparable
  optical weight. It does not require identical cross-platform silhouettes.
- The open time picker is required interaction documentation: the iOS HIG wheel and Android Material
  dial remain platform-native presentations of the same hour/minute value.

## Layout — top lockup, grouped cadence list, Body-owned action region

A scaffolded step (sequencer chrome: a back chevron in the nav, owned by the host, not the template).
Body fills the available height and width with a 24pt inset and contains **two independent vertical
regions**. `VStack/Content` is top-aligned and horizontally centered at a 560pt maximum width.
`VStack/Actions` is bottom-pinned at the same maximum width. Nothing vertically centers the two
regions as one combined block; flexible space lives between them.
Content top → bottom:

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
   `labelPrimary`), and — trailing — an **editable time value, shown only while the row is on**,
   directly left of a **`RemSwitch`** (on-tint `Color.systemGreen`). Tapping the value opens the
   platform time picker: SwiftUI wheel `DatePicker` on iOS and an accent-themed Android
   `TimePickerDialog`. The closed row stays shared; picker form intentionally follows each platform:
   - **Morning** — leading `sunrise` / `wb_twilight`; on by default at **8:00 AM**.
   - **Midday** — leading `sun.max` / `wb_sunny`; off by default (time 12:30 PM when on).
   - **Evening** — leading `moon.stars` / `bedtime`; off by default (time 8:00 PM when on).
5. **Primary CTA** — canonical `ActionArea` with Footnote off, inside Body's `VStack/Actions` and
   full-width within Body's 24pt inset. Its Button uses an explicit semantic disabled state rather
   than whole-control opacity. Its label + treatment track the save lifecycle (see States).
6. **Recoverable-failure Toast** (failure state only) — canonical `Toast` (`Variant=error`), directly
   above the ActionArea. It is brief, non-actionable, announces politely, and auto-dismisses after
   four seconds by default; the retry action remains available after it disappears.

In code, the onboarding scaffold's bottom action region realizes the canonical `ActionArea`
composition. The optional Toast is a preceding sibling inside `VStack/Actions`; it is not part of the
ActionArea itself, whose responsibility remains the primary Button and optional footnote (off here).

## States

| State | Content | CTA |
|---|---|---|
| **default** | cadence as loaded from `CheckinsService` (Morning on @ 8:00 AM, others off) | "Continue" — filled, enabled when ≥1 time is on |
| **edited** | the user changed a toggle (e.g. Midday also on @ 12:30 PM); not yet persisted | "Continue" — filled, enabled |
| **saving** | persisting; rows locked and visibly de-emphasized | "Saving…" — leading spinner, explicit disabled semantic colors |
| **saved** | persisted; rows locked and visibly de-emphasized | "Saved" — leading `checkmark`, explicit disabled semantic colors |
| **recoverable failure** | save failed; rows interactive again; transient error Toast above the CTA | "Try again" — filled, enabled |

**Toast:** neutral capsule, error semantic glyph/tint, `Typography.footnote` message. It carries no
action and does not replace the persistent retry control.

## Rules (the decisions that are easy to drift on — stated once)

- **Body owns layout:** the full-device root order is `StatusBar → TopBar → Body → NavigationIndicator`.
  Body fills the remaining width/height, owns a single 24pt outer inset, and lays out
  `VStack/Content` over `VStack/Actions` with space between. ActionArea has no outer inset of its own.
  NavigationIndicator is device chrome and never moves inside Body.
- **CTA lifecycle:** `default`/`edited` → "Continue"; `saving` → disabled "Saving…" + spinner; `saved`
  → disabled "Saved" + check; `failure` → enabled "Try again". The primary is one button whose label +
  leading affordance change; it is not five different controls.
- **"Start with one":** the CTA is inert in `default`/`edited` when **no** time is selected — the copy
  requires at least one. `saving`/`saved` lock it regardless; `failure` re-enables the retry.
- **Rows lock during `saving`/`saved`:** every row is non-interactive **and visibly de-emphasized with
  `Opacity.deemphasized`**, so the persisted set cannot change under the request and the visual state
  communicates the lock. Rows return to full emphasis in `default`/`edited`/`failure`.
- **Switch on-tint is `systemGreen`** on both platforms (iOS `Toggle` tint / Android M3 `Switch`
  checked track) — the native settings-toggle color.
- **Time value shows only while the row is on** and is an edit trigger. Tapping it opens the native
  platform picker; a confirmed value reports the canonical slot id plus hour/minute to the host and
  moves the screen to `edited`. CRUD coverage is explicit: switch-on creates/enables a cadence, the
  picker updates its time, switch-off disables it, and Continue saves the resulting set.
- **The value never wraps.** The selected time (e.g. "8:00 AM") stays on one line at its full type role
  (`Typography.body`). The Morning (populated) row keeps the same height and alignment rhythm as the
  empty Midday / Evening rows; the pill is pinned to its intrinsic width, never clipped or shrunk.
- **Canonical slot identity (`Checkin` contract):** the three rows are the shipping `CheckinsService`
  slots `morning | midday | night`. The third row **displays "Evening"** (per the reference) but its
  **stored/sent id is the canonical `night`** on both platforms — the display label and the persisted
  slot id differ on purpose. The `CheckinSlot` / cadence adapter (Swift
  `OnboardingCheckinTemplate.periods(from:)`, Compose `checkinPeriods`) is the single tested boundary
  that reconciles them; a toggle always reports `night`, never the `"evening"` display label.
- **Do not invent extra consent-style loading/empty states:** the only states are the five above.
- **Icons are semantic + FILL-pinned per the registry.** The hero is FILL 1; the three period leadings
  are FILL 0. Each platform uses the named native glyph from the registry. Acceptance compares meaning,
  FILL, optical weight, and role; it does not require SF Symbols and Material glyphs to share an
  identical silhouette. Android's temporary vector path is documented in `icon-registry.md` ‡.

## System use

- **Reuse:** the onboarding scaffold's Body action region, `RemSection` + `ListRow`, `ActionArea`,
  explicit Button states, `ContainedIcon` (hero + row leading), canonical `Toast`, and tokens.
- **Systemize:** `RemSwitch` — the canonical on/off toggle (Figma set `868:210`; legacy iOS-on
  variant `110:50`), extracted here
  because a grouped-settings toggle recurs across Settings/automations/voice, not just this screen. On
  both platforms it is a thin wrapper over the native switch pinned to `systemGreen`.
- **Exact:** the top-lockup order + copy, the three cadence rows (icons + titles, in order), the switch
  on-tint, the value-pill-only-when-on rule, Body/ActionArea hierarchy, the CTA lifecycle labels, Toast
  placement (above the ActionArea), the icon registry rows + FILL.
- **Adaptable:** per-platform safe-area handling; the exact hero-badge size; the exact value-pill radius
  and fill; the leading-icon active tint (the reference's blue morning glyph is expressed via the switch
  and the shared `.subtle` tile so the icon color is not coupled to row state).
- **Mapping boundary:** the `CheckinSlot` / `Checkin` adapter (`CheckinCadenceAdapter.swift`,
  `CheckinCadenceAdapter.kt`) accepts the **raw shipping `Checkin` fields** — `slot`, `enabled`,
  `deliveryHour`, `deliveryMinute`, `timezone` — validates the slot against `morning | midday | night`
  (rejecting anything else, including the `"evening"` display label), **formats the hour/minute into the
  brief-time label internally** (`8:00 AM`, `12:30 PM` — the host supplies no display string), produces
  the canonical template rows, and forwards toggles as a canonical `CheckinSlot`. It is unit-tested on
  both platforms (`CheckinInteractionTests`, `CheckinCadenceTest` — covering 8:00 AM, a non-zero minute,
  the rejected invalid `evening`, and the `night` toggle/update payload) so the `Checkin` identity
  contract (display "Evening" / send `night`) holds without relying on the host to remap.
- **Excluded:** other onboarding screens (own contracts); the sequencer chrome itself; the
  `CheckinsService` persistence + scheduling implementation (host-owned).

### Platform picker design

The five save-lifecycle references show the picker trigger in its closed state. Interaction
documentation adds one iOS wheel presentation and one Android dial presentation without multiplying
the five persisted save states. iOS nests the published three-column HIG Picker component supplied by
the founder (`npa2Riu1JdiazH2Y9ntrwk`, `Column=3`) and composes a true two-value AM/PM column. Android
uses a Rem-themed derivative of the official Material 3 dial picker: Material structure and behavior,
Rem `brandBlue` accent and neutral surfaces. Runtime uses SwiftUI's wheel `DatePicker` and an Android
`TimePickerDialog` whose native theme binds `colorAccent` to `#0C50FF`.

## Figma delivery (delivered; exact-head export is CI-gated)

Figma is part of a finished screen PR (founder, 2026-09-28; `docs/figma-delivery.md`). The editable
Check-in screen, its five states, and its place in the onboarding flow are to be authored in the
existing Rem file (`af4yDqCzp57jds9lkFiIaO`) on the shared `Onboarding` page, reusing the canonical
`Section` / `ListRow` / `RemSwitch` (`868:210`; legacy iOS-on variant `110:50`) instances, the
`Lockup` / `ActionArea` compositions, shared
variables, and text styles, following `SHAPE-OF-A-TASK.md` and the `rem-design-system` skill.

On 2026-09-29, a founder-authorized direct Codex Figma session mutated and read back the live editable
canvas. It repaired the shared `Section` / `ListRow`, `Button`, and `RemSwitch` masters; rebuilt
the five Check-in variants around official platform chrome and canonical compositions; and made
inventory Section `00` self-healing. A Builder-App operator-assisted attestation binds this live
record to the exact PR head. That attestation authenticates the readback; it does not claim that the
hosted Factory writer authored these canvas changes. The `delivery-scope:onboarding-checkin` gate
exports the five live nodes and verifies the editable structure against the current PR head before
the candidate is published.

### Delivery record

- **File:** [Rem Design System](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System)
- **Canonical component set:** [`Screen/Check-in` · `876:1121`](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=876-1121)
  — horizontal auto layout, hug contents, five `Status` variants, shared component instances,
  variables, and text styles.
- **Documentation:** [`02A · Check-in · Documentation` · `890:1502`](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=890-1502)
- **Prototype:** [`02B · Check-in · Prototype` · `885:1121`](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=885-1121)
  — Default is the flow starting point; Default → Edited → Saving → Saved and Failure → Saving
  exercise the normal and recoverable paths.
- **Picker states:** [`Screen/Check-in/Time Picker` · `932:4350`](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=932-4350)
  — iOS wheel and Android dial presentations, each over the same Check-in screen.
- **Picker composition:** [`TimePicker` · `939:258`](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=939-258)
  — `Platform=iOS|Android`; iOS retains the published HIG source instance and Android retains the
  official Material structure in a Rem-themed derivative.
- **Evidence instances:** [Default `885:1123`](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=885-1123),
  [Edited `885:1198`](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=885-1198),
  [Saving `885:1275`](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=885-1275),
  [Saved `885:1352`](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=885-1352), and
  [Failure `885:1429`](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=885-1429).
- **Readback:** every evidence node is a direct 402×874 instance of the canonical set. Default,
  Edited, Saving, Saved, and Failure expose the matching `Status` value. Each root orders
  `StatusBar`, official iOS `TopBar`, fill-height `Body`, and `NavigationIndicator`; Body owns one 24pt
  inset and contains `VStack/Content` plus `VStack/Actions`, keeping the lockup centered horizontally
  while remaining top-anchored per this contract.
  Cadence rows use the repaired HUG `Section` with transparent `ListRow` roots and the official
  platform-backed `RemSwitch`. `VStack/Actions` uses `ActionArea` with Footnote off; Saving and Saved
  select explicit disabled Button variants. Failure adds canonical `Toast` above the ActionArea. Inventory
  Section `00` contains one transparent auto-layout HStack so deletion/insertion heals spacing.
  Captured 2026-09-29 in the live editable file.

## Parity acceptance (what the visual gate diffs)

The paired render passes when iOS and Android match on **all** of:

- [ ] Hero = check-in badge (`clock.badge.checkmark.fill` / `alarm_on`, FILL 1), same placement
- [ ] Title + body: same text, same type roles, centered
- [ ] Cadence list: grouped card, three rows in order (Morning, Midday, Evening), same leading icons
      (outline) + same titles + trailing switches
- [ ] Switch on-tint is `systemGreen`; a row's value pill shows only while that row is on
- [ ] ActionArea sits inside Body and fills its inset width; label + affordance track the state
      (Continue / Saving… + spinner / Saved + check / Try again)
- [ ] Failure Toast present in the failure state, above ActionArea, same copy + role
- [ ] Each platform uses its named registry glyph and **FILL** at comparable optical weight; native
      SF/Material silhouettes may differ
- [ ] Tapping an enabled time is documented by the iOS wheel and Rem-blue Android dial picker states

**Evidence:** `screenshots.yml` paired table — `checkin-default-light`, `checkin-edited-light`,
`checkin-saving-light`, `checkin-saved-light`, `checkin-failure-light` — iOS ⟷ Android side by side.
Missing or unmatched required states fail closed.

**Amendment path:** founder for arrangement / product decisions; Builder may propose a bounded amendment
on the issue. **Status:** the paired render, authenticated exact-head Figma exports, structure report,
and editable node references above are the founder spot-check surface.
