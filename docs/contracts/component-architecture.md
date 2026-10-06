# Component architecture contract

The code shape every Rem Design System component follows, so Button, VoiceBar, and a brand-new
surface read the same way. The standard is **tiered**: a small universal contract every component
owes, plus a stricter split that only cross-product components need. Enforced by
`tools/lint-components.mjs` (baseline-ratcheted — see *Enforcement*).

> Why tiered and not uniform: the token-set / thin-view split (the Microsoft **Fluent** pattern)
> earns its keep when a component resolves many values across a *cross-product* of axes
> (Button = 7 variants × 2 sizes × 3 states). Forcing a separate `*TokenSet.swift` onto a 2-value
> component like `AgentStatusPill` is boilerplate, not clarity. So the split is **required where it
> pays off, optional where it doesn't** — but the universal rules below hold for everything.

## The reference implementations

- **`Buttons/`** — the canonical trio. `RemButtonTokenSet.swift` (every styleable value for a
  `(variant, size)`, all from `DesignTokens`, no literals) + `RemButtonStyle.swift` (a *thin*
  `ButtonStyle` that selects rest/pressed/disabled and renders) + `RemButton.figma.swift` (Code
  Connect: Figma `Style` prop ↔ `RemButtonVariant`).
- **`Primitives/ContainedIcon*`** — the same trio for a non-Button view.
- **`Chat/VoiceBar.swift`** — a cross-product component whose resolver is still **nested** (the
  private `Appearance` struct). It satisfies the *idea* (value-resolution separated from rendering)
  but not the *file shape*; it is on the backfill list to promote `Appearance` → `VoiceBarTokenSet`
  and add `VoiceBar.figma.swift`.

## Tier 1 — universal (every public component)

1. **Token-only values.** Colors, spacing, radius, and type come from `DesignTokens` (Swift) /
   `RemColors`/`RemSpacing`/`RemRadius`/`RemTypography` (Compose). No raw hex (`Color(red:…)`,
   `0xRRGGBB`, `#RRGGBB`) and no ad-hoc magic numbers for themeable values in the view body. Small
   structural constants (a 32pt control diameter, a 1pt hairline) are fine; a color or a type size is
   not.
2. **Code Connect.** A co-located `<Component>.figma.swift` binding the real Swift type to the Figma
   node + properties (see `RemButton.figma.swift`). It must also be listed in `Package.swift`
   `exclude:` (the shipping library never links `github.com/figma/code-connect`).
3. **Cross-platform mirror.** A Compose twin under `compose/RemDesignSystem/<folder>/` with a matching
   public API (same variants/states, mirrored names). New Compose folders go in the
   `gatherDesignSystemSources` list in `compose/RemDesignSystem/build.gradle.kts`.
4. **Evidence + registry.** A render-harness entry (SwiftUI `RenderSnapshots.swift` + Compose
   `EvidenceSnapshots.kt`) and a `REGISTRY.md` row mapping Figma node ↔ code.
5. **Preview.** A `#Preview` / `@Preview` exercising the component's key states.

## Tier 2 — cross-product components (the token-set split)

A component is **cross-product** when it has **≥ 4 variants on one axis, or ≥ 2 style axes**
(variant × size, state × tone, kind × leading, …). Current set:

| Component | Why | Status |
|---|---|---|
| `RemButton` | 7 variants × 2 sizes × 3 states | ✅ reference |
| `ContainedIcon` | fill × size | ✅ reference |
| `VoiceBar` | 6 states | ⏳ resolver nested (`Appearance`) → promote to `VoiceBarTokenSet`; add `.figma` |
| `TaskEventRow` | kind × leading × pills | ⏳ backfill |
| `ExecutionTrace` | status × step-status × lane | ⏳ backfill |

Cross-product components **must**:

- **`<Component>TokenSet.swift`** — a value type that resolves *every* styleable value for the axes,
  entirely from `DesignTokens`. This is the only place per-variant/per-state values live. (A private
  nested resolver like VoiceBar's `Appearance` is a near-miss: promote it to its own file so the
  shape matches Button.)
- **Thin view / style** — the `View` or `ButtonStyle` builds the token set and renders; it holds no
  values, only selection and layout.
- **Flat enums** — one flat `enum` per axis, mapping 1:1 to a single Figma property, so Code Connect
  binds cleanly and `generate-library` has an unambiguous target (see `RemButtonVariant`).

Trivial components (single axis, < 4 cases — `RemPill`, `AgentStatusPill`, `MessageBubble`,
`RemSlider`, `BrowserLiveCard`, `RunningTaskBanner`, `RemComposerBar`) may keep the resolver inline.
They still owe all of Tier 1. If one grows past the threshold, promote it.

## New components

Start from the scaffold: `node tools/scaffold-component.mjs <Folder>/<Name> [--cross-product]`. It
writes the Swift view (+ `TokenSet` when `--cross-product`), the `.figma.swift` stub, the Compose
twin, and prints the Package.swift / gather-list / registry / render-harness lines to add. Building
by hand is fine too — the lint will tell you what's missing.

## Enforcement

`tools/lint-components.mjs` runs in CI (`component-contract.yml`) and locally
(`node tools/lint-components.mjs`). It checks the deterministic parts of this contract:

- every public component view has a paired Compose file and a `.figma.swift` (listed in
  Package.swift `exclude:`);
- no raw hex in component bodies (token-only);
- every cross-product component (the Tier-2 table) has a `*TokenSet.swift`.

It is **baseline-ratcheted**: `tools/component-contract.baseline.json` grandfathers today's known
gaps so CI stays green, and the lint fails only on *new* violations or ones not in the baseline. The
baseline is the backfill TODO — removing an entry and fixing the component is how the set converges.
Never add to the baseline to silence a new component; fix it instead.
