# Icon registry (SF Symbol ↔ Material Symbol)

Icons are **font glyphs on both platforms**, not SVGs:

- **iOS** — SF Symbols (SF Pro, PUA codepoints). Fill is a variant suffix: `shield` vs `shield.fill`.
- **Android** — **Material Symbols** (the *variable font*, not the legacy "Material Icons"),
  with a **FILL axis** (0 = outline, 1 = filled) plus weight / optical-size.

Because both systems have a fill notion, an icon is matched by **meaning + FILL + optical weight** —
never by "whatever Material icon looks close." Native SF Symbols and Material glyphs are not expected
to share an identical outline; platform fidelity is part of the match.

## The registry

| Meaning | iOS (SF Symbol) | Android (Material Symbol) | FILL | Notes |
|---|---|---|---|---|
| privacy / lock-shield | `lock.shield.fill` | `shield_lock` | **1** | consent hero |
| privacy policy | `shield` | `shield` | **0** | legal row |
| terms / document | `doc.text` | `description` | **0** | legal row |
| disclosure chevron | `chevron.right` | `chevron_right` | **0** | list-row accessory |
| error / warning | `exclamationmark.triangle.fill` | `error` | **1** | notice card |
| check-in schedule | `clock.badge.checkmark.fill` | `alarm_on` | **1** | check-in hero — a clock/alarm with a confirmation check ‡ |
| morning / sunrise | `sunrise` | `wb_twilight` | **0** | check-in row leading ‡ |
| midday / sun | `sun.max` | `wb_sunny` | **0** | check-in row leading ‡ |
| evening / moon | `moon.stars` | `bedtime` | **0** | check-in row leading ‡ |

‡ **Android render path for the four check-in rows.** These four glyphs are **not** in the baked
`RemMaterialSymbols` static subset (which carries only the five consent/sign-in codepoints), and
regenerating that subset needs the offline font toolchain (`tools/material-symbols/subset.py` +
`fonttools` + the upstream variable font). Until a subset regeneration lands, Android renders these
rows from the **Material Icons** vector set (`androidx.compose.material:material-icons-extended`) via
the `ContainedIcon(icon:)` / `OnboardingHero(icon:)` ImageVector path already used for non-registry
heroes — **`Icons.Outlined.*` for the FILL-0 rows, `Icons.Filled.AlarmOn` for the FILL-1 hero**, so
the pinned FILL is still honoured (outline vs filled is a value, not a guess). This is a deliberate,
documented divergence from the Material Symbols *font* path used by consent, flagged for a future
subset regeneration to unify the check-in rows onto the font path. The pairings are researched
semantic twins (a clock-with-check hero; sunrise / sun / moon time-of-day markers), not near-misses —
but `clock.badge.checkmark` ↔ `alarm_on` is the closest available clock+check pair on both sides and
is the one row a designer may want to revisit if a truer twin appears.

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
   comparable visual weight. Do not reject a correct native pair solely because its silhouette differs.

> Status: seeded from the onboarding consent + sign-in needs. Grows per screen; every new row is a
> researched pair or an explicitly-flagged open row.
