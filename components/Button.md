---
component: Button
group: general
mirrors: RemPrimaryActionButtonStyle / RemSettingsCTAButtonStyle
status: draft
rules:
  - id: no-raw-button
    do: "Use the design-system Button for every tappable action."
    dont: "Drop a raw <button> (web) or bare Button {} styling (SwiftUI) into a screen."
    enforced_by: "react/forbid-elements (button) + no-restricted-syntax in _adherence.oxlintrc.json"
  - id: one-primary-per-context
    do: "Use exactly one `primary` button as the main action of a screen or sheet."
    dont: "Place two `primary` buttons in the same row or context — the user can't tell which is the main action."
    enforced_by: "prose-only (candidate for a new adherence rule)"
  - id: destructive-role
    do: "Express destructive actions with role=\"destructive\" on settingsCTA/recovery."
    dont: "Hand-tint a button red to mean 'dangerous'."
    enforced_by: "no-restricted-syntax: Button role must be 'primary' | 'destructive'"
  - id: variant-vocabulary
    do: "Pick the variant that matches the action's job (primary / settingsCTA / recovery / connect)."
    dont: "Invent a new visual treatment or override the button's classes for a one-off look."
    enforced_by: "no-restricted-syntax: Button variant must be 'primary' | 'settingsCTA' | 'recovery' | 'connect'"
---

# Button

## Overview
The one tappable-action control in Rem. Mirrors the SwiftUI button styles 1:1
(`RemPrimaryActionButtonStyle` for `primary`, `RemSettingsCTAButtonStyle` for `settingsCTA`, etc.).

## When to use
- The main action of a screen, sheet, or card (`primary`).
- A settings-row call to action such as "Manage subscription" (`settingsCTA`).
- Recovering from a failure state, e.g. "Reconnect gateway" (`recovery`).
- A compact connect affordance in a row's trailing slot (`connect`).

## When *not* to use
- **A badge or status label** → use `Pill`, not a Button.
- **A whole tappable row** (device row, settings row) → use `ListRow` with a trailing control.
- **A transient confirmation** → use `Toast`.

## Anatomy
`[ optional leading glyph ] label`, inside a filled or tinted container. `primary` and `recovery`
are full-width by default; `settingsCTA` is full-width at `size="regular"` and hugs its label at
`size="compact"`.

## Variants & states
| Prop | Values | Notes |
|---|---|---|
| `variant` | `primary` · `settingsCTA` · `recovery` · `connect` | `primary` = filled, inverted label, full width |
| `role` | `primary` · `destructive` | Applies to `settingsCTA` and `recovery` |
| `size` | `regular` · `compact` | `settingsCTA` only; `compact` hugs its label |

States: default, pressed, disabled. (Loading is not a Button state today — pair with `TypingDots`
or disable + `Toast` for async feedback.)

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Use `Button` for every tappable action. | Drop a raw `<button>` / hand-styled button in. |
| Use exactly one `primary` per screen/sheet. | Put two `primary` buttons in one row. |
| Use `role="destructive"` for dangerous actions. | Hand-tint a button red to signal danger. |
| Pick the variant that matches the job. | Invent a new treatment or override classes. |

## Accessibility
- **Contrast:** `primary` uses `color.buttonBackground` (label color) with an inverted label —
  meets AA in both appearances. `connect`/tinted CTAs on a fill use `color.brand.blueOnFill`,
  which brightens in dark mode to clear AA (~4.7:1).
- **Hit target:** ≥ 44×44 pt on iOS.
- **Dynamic Type:** label follows the type scale; see SPEC §10 (fixed-size vs Dynamic Type is an
  open decision).
- **Labels:** the button's text is its accessibility label; icon-only usage needs an explicit label.

## Tokens used
- `color.buttonBackground`, `color.brand.blue`, `color.brand.blueOnFill`
- `radius.medium`, spacing (`spacing.md` / `spacing.lg`), `typography.roles.body` / `bodyBold`

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `variant` | `"primary" \| "settingsCTA" \| "recovery" \| "connect"` | `primary` | Button treatment |
| `role` | `"primary" \| "destructive"` | `primary` | Tint; applies to `settingsCTA`/`recovery` |
| `size` | `"regular" \| "compact"` | `regular` | `settingsCTA` only |
| `children` | `ReactNode` | — | Label |
| `className`, `id`, `style` | — | — | Passthrough |

_Web API from `Button.d.ts`; the SwiftUI surface is the button-style modifiers named in Overview._
