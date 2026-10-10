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
| check-in schedule | `clock.badge.checkmark.fill` | `alarm_on` | **1** | check-in hero — provisional Android vector fallback ‡ |
| morning / sunrise | `sunrise` | `wb_twilight` | **0** | check-in row — provisional Android vector fallback ‡ |
| midday / sun | `sun.max` | `wb_sunny` | **0** | check-in row — provisional Android vector fallback ‡ |
| evening / moon | `moon.stars` | `bedtime` | **0** | check-in row — provisional Android vector fallback ‡ |

‡ **Provisional Android vector fallback for the four check-in rows.** These four glyphs are **not** in the baked
`RemMaterialSymbols` static subset (which carries only the five consent/sign-in codepoints), and
regenerating that subset needs the offline font toolchain (`tools/material-symbols/subset.py` +
`fonttools` + the upstream variable font). Until a subset regeneration lands, Android renders these
rows from the **Material Icons** vector set (`androidx.compose.material:material-icons-extended`) via
the `ContainedIcon(icon:)` / `OnboardingHero(icon:)` ImageVector path already used for non-registry
heroes — **`Icons.Outlined.*` for the FILL-0 rows, `Icons.Filled.AlarmOn` for the FILL-1 hero**, so
the pinned FILL is still honoured (outline vs filled is a value, not a guess). This is a deliberate,
documented divergence from the Material Symbols *font* path used by consent, flagged for a future
subset regeneration and standalone evidence before the Android pairings graduate from provisional.
The current screen evidence proves the semantic role and fill treatment in context; it does not prove
the final font-path glyph pairing. `clock.badge.checkmark` ↔ `alarm_on` remains the closest available
clock+check candidate and should be revisited if a truer twin appears.

### Chat slice — open rows (recorded as implemented, not yet graduated)

Recorded from the Chat / Inbox composition sources so the cross-platform pairing and FILL are pinned and
cannot drift silently. **All rows are open:** Android currently draws these from the Material Icons vector
set (`material-icons-extended`), like the provisional check-in rows above, not from the `RemMaterialSymbols`
font subset (Rule 2). They graduate only after a subset regeneration and standalone paired evidence.

| Meaning | iOS (SF Symbol) | Android (as implemented) | FILL | Notes |
|---|---|---|---|---|
| header back | `chevron.left` | `Icons.AutoMirrored.Filled.KeyboardArrowLeft` | **0** | `ChatHeader`; stroke glyph, no fill variant |
| header overflow | `ellipsis` | `Icons.Filled.MoreHoriz` | **0** | `ChatHeader`; dots, no fill variant |
| header call | `phone` | `Icons.Outlined.Call` | **0** | `ChatHeader` (WS1e); outline handset, in-app voice entry only (not PSTN); replaces the trailing overflow control, never beside it |
| activity disclosure | `chevron.right` | `Icons.AutoMirrored.Filled.KeyboardArrowRight` | **0** | `ChatHeader` capsule; matches the registry disclosure row |
| add to chat | `plus` | `Icons.Filled.Add` | **0** | `RemComposerBar`; stroke glyph |
| model trigger | `chevron.up.chevron.down` | `Icons.Filled.UnfoldMore` | **0** | `ChatModelMenu` / composer Auto pill |
| model selected | `checkmark` | `Icons.Filled.Check` | **0** | `ChatModelMenu` rows |
| speak | `waveform` | `Icons.Filled.GraphicEq` | **0** | composer Speak pill; matches the Voice row's semantic pair |
| send | `arrow.up` | `Icons.Filled.ArrowUpward` | **0** | composer send circle |
| stop (cancel turn) | `stop.fill` | `Icons.Filled.Stop` | **1** | composer stop circle |
| remove / dismiss | `xmark` | `Icons.Filled.Close` | **0** | attachment chip, reply-context dismiss |
| not delivered | `exclamationmark.circle` | `Icons.Outlined.ErrorOutline` | **0** | `MessageBubble` failure control (outline on both) |
| camera | `camera` | `Icons.Outlined.PhotoCamera` | **0** | `AddToChatSheet` (iOS only; no Android camera tile) |
| photos | `photo.on.rectangle` | `Icons.Outlined.PhotoLibrary` | **0** | `AddToChatSheet` |
| files | `folder` | `Icons.Outlined.Folder` | **0** | `AddToChatSheet` |
| cloud browser | `globe` | `Icons.Outlined.Public` | **0** | `AddToChatSheet` row; matches the Browser row's meaning |
| thinking | `brain` | `Icons.Outlined.Psychology` | **0** | `AddToChatSheet` Thinking row; Memory uses `brain.head.profile` / `psychology` |
| message reply | `arrowshape.turn.up.left` | `Icons.AutoMirrored.Outlined.Reply` | **0** | `MessageActionSheet` Reply row |
| mark as unread | `message` | `Icons.Outlined.ChatBubbleOutline` | **0** | `MessageActionSheet` Mark as unread row |
| copy | `doc.on.doc` | `Icons.Outlined.ContentCopy` | **0** | `MessageActionSheet` Copy row; iOS 17 name of the reference's `document.on.document` |
| select text | `selection.pin.in.out` | `Icons.Outlined.SelectAll` | **0** | `MessageActionSheet` Select Text row; closest Material meaning, not the same outline |
| report | `flag.fill` | `Icons.Filled.Flag` | **1** | `MessageActionSheet` Report row (assistant messages only) |
| more reactions | `plus` | `Icons.Filled.Add` | **0** | `MessageReactionPicker` `+` cell, brand blue; same pair as add to chat |

Open questions for the human decision: `brain` vs `brain.head.profile` (Thinking vs Memory may want distinct
glyphs), and whether stroke-only glyphs drawn from `Icons.Filled.*` vectors are acceptable until the font
subset carries them.

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
