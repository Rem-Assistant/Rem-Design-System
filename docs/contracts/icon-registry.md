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
