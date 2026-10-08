# Icon registry (SF Symbol ↔ Material Symbol)

Icons are **font glyphs on both platforms**, not SVGs:

- **iOS** — SF Symbols (SF Pro, PUA codepoints). Fill is a variant suffix: `shield` vs `shield.fill`.
- **Android** — **Material Symbols** (the *variable font*, not the legacy "Material Icons"),
  with a **FILL axis** (0 = outline, 1 = filled) plus weight / optical-size.

Because both are fonts with a fill notion, an icon is matched by **meaning + FILL + weight** —
never by "whatever Material icon looks close." One text layer per platform; no SVG components.

## The registry

| Meaning | iOS (SF Symbol) | Android (Material Symbol) | FILL | Notes |
|---|---|---|---|---|
| privacy / lock-shield | `lock.shield.fill` | `shield_lock` | **1** | consent hero |
| privacy policy | `shield` | `shield` | **0** | legal row |
| terms / document | `doc.text` | `description` | **0** | legal row |
| disclosure chevron | `chevron.right` | `chevron_right` | **0** | list-row accessory |
| error / warning | `exclamationmark.triangle.fill` | `error` | **1** | notice card |

**Brand marks are assets, not registry glyphs** (they have no font twin):
`RemAppIcon` (raster, both platforms) · Apple mark (SF `apple.logo` / bundled monochrome vector) ·
Google "G" (multicolor asset, both).

## Rules

1. **FILL is pinned per row.** If iOS is filled, Android is FILL 1; if iOS is outline, Android is
   FILL 0. This is what kills the "iOS filled / Android outline" drift — it's a value, not a guess.
2. **No ad-hoc `Icons.Filled.*`.** The legacy Material Icons set is always-filled and has no FILL
   axis, so it cannot match an outline SF Symbol. Use Material **Symbols** (the variable font).
3. **The mapping is not assumed 1:1 — it is researched.** Not every SF Symbol has a clean Material
   twin (and vice-versa). Adding a row is a real lookup, not a guess. When no faithful twin exists,
   record it here as an **open row** with the closest candidate + why it's imperfect, and flag it for
   a human decision rather than shipping a near-miss (e.g. `Security` ≠ `lock.shield.fill` — a
   shield-with-check is a different glyph).
4. **Weight** follows the platform's optical default unless a row overrides it; keep the two sides at
   the same visual weight.

> Status: seeded from the onboarding consent + sign-in needs. Grows per screen; every new row is a
> researched pair or an explicitly-flagged open row.


## Settings New trial mappings

Codepoints verified from Google's official [Material Symbols codepoints](https://github.com/google/material-design-icons/blob/master/variablefont/MaterialSymbolsOutlined%5BFILL%2CGRAD%2Copsz%2Cwght%5D.codepoints). Existing subset generator bakes both fills and the preBuild gate checks every new codepoint. Native outline silhouettes differ; these mappings preserve semantic intent for the bounded playground.

| Meaning | iOS | Android / codepoint | FILL | Status |
|---|---|---|---|---|
| Info / About | info.circle.fill | info / E88E | 1 | semantic native pair |
| Billing | creditcard.fill | credit_card / E8A1 | 1 | semantic native pair |
| Permissions | hand.raised.fill | pan_tool / E925 | 1 | semantic native pair |
| Share / Support | square.and.arrow.up | ios_share / E6B8 | 0 | source uses same glyph for both rows |
| Paired devices | macbook.and.iphone | devices / E326 | 0 | semantic native pair |
| Connectors | link.circle.fill | link / E250 | 1 | **Open:** Material link has no encircling disc; candidate for trial, needs design decision before production |
| Cloud browser | globe | language / EA07 | 0 | semantic native pair |
| Automations | bell.badge.fill | notifications_active / E7F7 | 1 | native notification emphasis differs; visual reference only |
| Memory | brain.head.profile | psychology / EA4A | 0 | semantic native pair |
| Models | cpu | memory / E322 | 0 | semantic native pair |
| Wallet | wallet.pass | wallet / F8FF | 0 | semantic native pair |
| Voice | waveform | graphic_eq / E1B8 | 0 | semantic native pair |

Native app navigation chrome uses platform back-arrow controls; it is not a design-system glyph specimen.
