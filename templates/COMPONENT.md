<!--
COMPONENT DOC TEMPLATE — Rem Design System
Copy this file to docs/design-system/components/<Name>.md and fill every section.
The `rules` front-matter block is authored ONCE here and is the single source for both
the human "Do's & don'ts" section below AND the machine rule in _adherence.oxlintrc.json
(see SPEC §4). Keep the two in agreement by editing only this block.
-->
---
component: ComponentName
group: general            # general | toolresultcard
mirrors: SwiftTypeName    # the SwiftUI type this 1:1 mirrors (e.g. RemPrimaryActionButtonStyle)
status: draft             # draft | reviewed | stable
rules:
  - id: rule-slug
    do: "What to do."
    dont: "What not to do."
    enforced_by: "adherence rule id, or 'prose-only' if not machine-checkable"
---

# ComponentName

## Overview
One sentence: what it is and the SwiftUI type it mirrors 1:1.

## When to use
- Bullet the situations this is the right component for.

## When *not* to use
- The situations where a different component is correct — **name the alternative**.

## Anatomy
Labeled parts (leading / title / subtitle / trailing / …). A small diagram or list.

## Variants & states
| Variant / prop | Values | Notes |
|---|---|---|
| `variant` | … | … |

States: default, pressed/hover, disabled, loading, error, empty — whichever apply.

## Do's & don'ts
> Rendered from the `rules` front-matter block above. Each row is one authored rule.

| ✅ Do | 🚫 Don't |
|---|---|
| … | … |

## Accessibility
- **Contrast:** token pairing meets WCAG AA (4.5:1 text / 3:1 UI).
- **Hit target:** ≥ 44×44 pt (iOS) / platform minimum.
- **Dynamic Type / scaling:** how it behaves as text size grows (see SPEC §10).
- **Labels / focus:** VoiceOver label, focus order, keyboard (macOS).

## Tokens used
- `color.…`, `spacing.…`, `radius.…`, `typography.…` — link to `tokens/tokens.json`.

## API
Props table, sourced from `<Name>.d.ts`.

| Prop | Type | Default | Description |
|---|---|---|---|
| … | … | … | … |
