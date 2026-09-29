---
name: rem-design-system
description: >-
  Build and maintain the Rem design system in Figma via the Figma MCP plugin
  (`mcp__Figma__use_figma`) — iOS-26-grounded components, screens, and docs that
  the SwiftUI app is verified against. Use this whenever the task involves the Rem
  Figma file (key `af4yDqCzp57jds9lkFiIaO`), a Rem component or screen in Figma,
  design tokens/variables, the iOS 26 kit, SF Symbols in Figma, or "make/refresh
  the design system / a component page / a screen". Trigger it even when Figma
  isn't named but the work is clearly Rem design-system work (e.g. "add a Switch
  variant to the row", "document the composer", "put the settings screen in a
  device frame", "why do my swapped switches render tiny in Figma"). It encodes
  the anti-drift discipline and the hard-won Figma-plugin gotchas that make this
  work correctly instead of producing duplicates and broken layouts.
---

# Rem design system (Figma)

Use the repo-generic `../design-system-delivery/SKILL.md` for the portable method. This skill is
the Rem adapter: it supplies Rem's Figma file, node ids, registries, templates, tokens, platform
sources, and fidelity evidence. Other projects should reuse the generic skill and provide their own
adapter instead of forking Rem-specific identifiers.

You are building/maintaining a **living** design system in one Figma file
(`af4yDqCzp57jds9lkFiIaO`) that mirrors the iOS 26 look and is **verified against the
real SwiftUI app**. The single hardest failure mode is **drift** — creating a second
version of something that already exists, or hand-drawing what should be a reused
component. Everything below exists to prevent that and to avoid the Figma-plugin traps
that silently produce broken output.

## Rule 0 — reuse before you create (anti-drift)

Before creating **any** component, style, or screen element:

1. **Check the registry.** `REGISTRY.md` (repo) and the **Component Index** page in Figma
   list every canonical component with its node id + SwiftUI source. If it exists,
   **instance the canonical master** — never re-draw it, never make a "v2".
2. **One canonical per concept, on its own named page.** No second generation, no bespoke
   copy. The worst messes this session came from breaking this (two ListRows, a bespoke
   `DateNav` beside the real `DateNavigationHeader`, a `Card` that should have been
   `Section`).
3. **Name concepts semantically, then structure SwiftUI-first.** Use shared product/component names
   such as `Section`, `ContentUnavailableView`, `StatusBar`, `Body`, `ActionArea`, and
   `NavigationIndicator`. Inside those regions, name otherwise-generic layout layers `VStack`,
   `HStack`, `ZStack`, `LazyVStack`, or `LazyHStack`. Documentation maps those to Compose
   `Column`, `Row`, `Box`, `LazyColumn`, and `LazyRow`; do not duplicate the canvas hierarchy by
   platform. When you add a concept, add its row to `REGISTRY.md` **and** the Figma Component Index
   in the same change.

## Before the first `use_figma` call

Load `/figma-use` (or the `skill://figma/figma-use/SKILL.md` resource). Then read
**`references/figma-gotchas.md`** — it lists the plugin traps that waste hours if you
hit them cold (nested-instance size can't be overridden; `resize()` locks an axis;
`INSTANCE_SWAP` defaults want a *local node id*; SF Pro *does* render SF Symbols; etc.).
Skipping it reliably produces collapsed tiles, tofu glyphs, and rolled-back calls.

## Two tracks — pick by what you're making

The pattern that works for an **atomic component** is different from what a **screen**
needs. Don't force one onto the other.

- **Component** (Row, Button, Section, MessageBubble, a control, or a reusable composition): follow
  **`references/component-track.md`** — arrange the family as a horizontal auto-layout row of
  vertical columns. Each column puts the canonical component/variant set first and an attached
  `Component Documentation` instance second. The template contains documentation metadata only;
  it has no Component slot. Full Specs-plugin output is optional follow-up work.
- **Screen or pattern** (Agenda, Chat, Settings, a flow): follow
  **`references/screen-track.md`** — a full-device (402×874) screen built **native, in
  auto-layout, on the canonical components**, documented with the file's `Component Documentation`
  and attached slot-based flow templates. `Mobile Flow Documentation` → `Mobile Flow` → Sections →
  Rows → Steps → `Mobile Placeholder` → Screen remains an instance chain; replace content through
  slots and never detach the template. Canonical screen components are instanced into documentation
  and prototype so one edit updates both.

On the Onboarding page, `00 · Canonical screen components` (`760:21`) uses `#F5F5F5` as its
canvas contrast surface around white device frames. Canonical Sign-in uses the repository's real
`RemAppIcon` raster and Google SVG; canonical Privacy uses `Section` (`741:311`) for its legal rows.
The structure contract rejects placeholder glyphs, hand-built replacements, or a white inventory.
Consent proves the reusable shape: flow chassis masters live in Device Kit (`769:169` through
`769:282`); Sign-in, Privacy, and the three consent states are all canonical component roots inside
the single `00` Section; `ButtonGroup`,
`Lockup`, and `ActionArea` live in `Composition components` (`773:2`) on the dedicated
`Compositions` page (`826:482`), ordered after Primitives. `Component Index` (`3:5`) remains the
index only. Component-family pages use fill-free, stroke-free canvas Sections with auto-layout
inside; the shared `Component Documentation` master (`663:2270`) owns a variable-bound
`background/primary` surface. These
names are intentionally domain-neutral because the patterns may serve screens beyond onboarding.
They compose primitives and therefore live on Compositions rather than Primitives. Their roots
inherit the parent surface. `ButtonGroup` fills its parent, and horizontal actions fill equally.
`Lockup` and `ActionArea` fill their parent up to the shared 560-point onboarding content cap;
Lockup text, the Button Group slot, and the wrapping Footnote fill that responsive width while the
Lockup visual keeps its intrinsic size. Screen composition owns the horizontal inset. For later
flows, instance these masters and populate slots instead of copying the consent frames.

During every screen build, inspect its anatomy for reusable concepts without waiting for founder
prompting. Extract a concept when it owns one stable responsibility and has either two plausible
consumers or observed recurrence across flows. Keep one-off composition local when reuse is merely
hypothetical or the wrapper would own no layout, behavior, semantics, or slot contract. This is the
portable `design-system-delivery` heuristic applied through Rem's registry and evidence rules.

Do not pause a screen run to cosmetically reorganize every legacy component page. When a Builder
touches a component family, migrate that family into the canonical master-first / specimen-second
documentation shape and update its consumers. Do not bulk-delete legacy components: keep them as
migration evidence until their consumers point at the replacement, then move superseded masters to
Retired and remove only proven duplicates. Steward can schedule unrelated legacy families as separate
cleanup work after the proving loop is reliable.

## Foundations (the layers everything binds to)

- **Tokens are the source.** `tokens/tokens.json` generates the Figma variable collections
  (Color Light/Dark, Spacing, Radius, Typography) plus a separate **Platform** collection (iOS /
  Android modes) that swaps values like `font/family` (SF Pro ↔ Roboto). Theme and Platform are
  independent axes; changing platform must not force a theme change. Components bind to
  variables/styles, never hard-coded values, so either axis can switch the whole file.
- **Type and color foundations should be Rem-owned.** Use the copied iOS 26 kit styles as the source
  baseline for a later foundations pass, then map their full Dynamic Type metrics and semantic colors
  into Rem-owned variables/styles. Rem components must not depend on an external kit library at
  runtime after that mapping. Bind every text node to a Rem style rather than a raw size.
- **Foundations remains the visible Rem token/style reference.** Do not paste the whole iOS 26 page
  into Rem. Treat that kit as attributed upstream source material, adopt only the semantic colors,
  type metrics, spacing, radius, and chrome Rem uses, and add the Android mapping beside the same Rem
  concept. Update Foundations when a local variable or style is added. The empty Guide page is removed
  because its durable rules live in the repository contracts and Component Index.
- **System chrome switches by Platform.** `StatusBar` (`785:389`) and `NavigationIndicator`
  (`793:379`) expose iOS and Android treatments while retaining dark/light content switching inside
  each. They sit inside one shared product layout; Platform changes native chrome, type metrics, and
  semantic icon sources, not the screen hierarchy. Use platform-specific nested icon/text components or
  verified semantic mappings; do not assume an SF Symbols glyph remains correct after only changing
  the font family to Material Symbols.
- **Components may be sourced from the iOS 26 kit, then adopted.** Copy the kit's Row/List/controls
  as source material, put Rem's custom components on top, and bind both to the Rem-owned
  styles/variables above.

## Verification (this is why it's "verified against the app")

- The **`visual-verify` CI** (`Rem-Assistant/Rem`, `.github/workflows/visual-verify.yml`)
  builds the iOS app on a macOS runner and screenshots each `--rem-<screen>-fixture`. Those
  screenshots are the ground truth; compare each Figma component/screen to them and fix
  mismatches. Record the pass in `FIDELITY.md`.
- **Code Connect** (Figma↔SwiftUI) needs a paid Dev/Enterprise seat. Until then, record the
  binding in each component's Figma **description** and the `REGISTRY.md` mapping table —
  that's the manual equivalent, and it keeps the "app verified against" contract legible.

## After a meaningful change

Update `FIDELITY.md` and `REGISTRY.md`, refresh the Figma Component Index row, and (for app
changes) let visual-verify run. Commit docs to the design-system repo. Keep the file's page
list clean: canonical components stay in their documentation containers, screens stay inside
their documented flows, and empty or scratch pages are removed. Keep a `Guide` page only if it is a
concise entry point for file structure, modes, source mapping, and operating rules; once that content
is canonical elsewhere, migrate any unique guidance and delete the redundant page.

Re-read the live canonical Figma node before each write; its current anatomy is authoritative, while
this adapter and `REGISTRY.md` store Rem-specific identifiers and durable intent. Run the same
component-quality scorecard on every touched or consumed family: typography, semantic color,
responsive layout, canonical reuse, and taxonomy placement. Canonical Button (`377:8`) is the first
machine-verified typography fixture: filled labels bind `Body/Bold`, text actions bind
`Body/Emphasized`, and pills bind `Subheadline/Emphasized`. It is one entry in a data-driven audit,
not a Button-specific definition of quality.

## Reference files

- `references/figma-gotchas.md` — the `use_figma` plugin traps + how to work around each.
- `references/component-track.md` — the Fluent component-page recipe (anatomy, slots, matrices).
- `references/screen-track.md` — the device-bezel screen recipe (native auto-layout, sections, states).
- `references/sf-symbols-map.md` — verified SF Symbol → PUA codepoint table (the app's icon set)
  + how it was sourced. Read it before adding any icon so you reuse a verified codepoint.
