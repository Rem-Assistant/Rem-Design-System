# SF Symbols → PUA codepoint map (verified)

The name→codepoint lookup for rendering real SF Symbols in Figma with **`SF Pro`** +
`String.fromCodePoint(cp)`. Every row below was **screenshot-verified** (rendered at SF Pro
Bold and eyeballed against its name) — a wrong codepoint renders a plausible-but-wrong symbol,
not tofu, so the render is the only proof. Names come from the real app's `Image(systemName:)`
usages (`grep -rho 'systemName: "..."' Rem/**.swift`).

**How these were sourced:** spatial pairing in the community file
"SF Symbols | Text objects for Figma" (`eMocgqr193EB694SlKtYZP`, "Start here" page). Each
symbol has an ascii **caption** node and, on the **same row ~68–120px to its LEFT**, its
**glyph** node (single PUA char). Pair caption→nearest-left glyph; the left offset grows with
name length (glyph fixed-left, caption grows rightward). Anchor with a known answer
(`chevron.right → 10018a`) before trusting a batch. Full method: `figma-gotchas.md`.

## Verified map

| SF Symbol | Codepoint | SF Symbol | Codepoint |
|---|---|---|---|
| `chevron.right` | `U+10018A` | `chevron.left` | `U+100189` |
| `chevron.down` | `U+100188` | `chevron.up.chevron.down` | `U+10018F` |
| `calendar` | `U+100249` | `calendar.badge.plus` | `U+10024A` |
| `line.3.horizontal` | `U+100307` | `list.bullet` | `U+1002F2` |
| `plus` | `U+10017C` | `xmark` | `U+100184` |
| `xmark.circle.fill` | `U+100061` | `checkmark` | `U+100185` |
| `checkmark.circle.fill` | `U+100063` | `magnifyingglass` | `U+1002AB` |
| `ellipsis` | `U+100360` | `ellipsis.circle` | `U+100361` |
| `arrow.up` | `U+100128` | `arrow.up.circle.fill` | `U+100077` |
| `arrow.clockwise` | `U+100148` | `waveform` | `U+10066B` |
| `message.badge.waveform.fill` | `U+100F02` | `paperclip` | `U+100262` |
| `exclamationmark.triangle.fill` | `U+1001FF` | `exclamationmark.circle.fill` | `U+10005F` |
| `clock` | `U+10042B` | `tray` | `U+100223` |
| `person.fill` | `U+10026A` | `stop.fill` | `U+1006F7` |
| `play.fill` | `U+100284` | `brain.head.profile` | `U+100BCF` |
| `arrow.turn.up.right` | `U+100139` | `speaker.wave.2.fill` | `U+1002A7` |
| `speaker.wave.2` | `U+1002A6` | `mic.fill` | `U+1002B1` |
| `mic.slash.fill` | `U+1002B3` | `phone.down.fill` | `U+100347` |
| `checklist` | `U+100DFE` | `calendar.badge.clock` | `U+1009DE` |
| `arrow.uturn.backward` | `U+100C4D` | `calendar.badge.plus` | `U+10024A` |
| `creditcard.fill` | `U+100370` | `hand.raised.fill` | `U+10027C` |
| `info.circle.fill` | `U+100175` | `envelope.fill` | `U+100356` |
| `square.and.arrow.up` | `U+100202` | `bell.fill` | `U+1002DA` |
| `camera.fill` | `U+10031F` | `externaldrive.fill` | `U+100903` |
| `number` | `U+100183` | `xmark.octagon.fill` | `U+10048A` |
| `clock.badge.exclamationmark.fill` | `U+100E77` | `lock.shield.fill` | `U+10079A` |
| `text.document` (= `doc.text`) | `U+10023F` | `shield` | `U+100666` |
| `eye.fill` | `U+1002EE` | `eye` | `U+1002ED` |
| `server.rack` | `U+1031B3` | `lock.iphone` | `U+100F13` |
| `gearshape.fill` | `U+10094F` | `gearshape` | `U+10094E` |
| `checkmark.seal.fill` | `U+1001FB` | `flag.fill` | `U+100DB8` |
| `hand.thumbsup.fill` | `U+100280` | `arrow.right` | `U+10012B` |
| `circle` | `U+100000` | | |

**Batch 2 (2026-09-24, anchored on `chevron.right → 10018a`).** `lock.shield.fill`, `text.document`,
`shield` are **screen-verified** (rendered on the Privacy screen `410:16`). The rest are pairing-sourced
and not yet individually screenshot-checked — screenshot before shipping in a prominent spot.
**`doc.text` was renamed `text.document`** in current SF Symbols (same glyph `U+10023F`) — the community
file only indexes the new name. **`apple.logo` is NOT in the community file** (nor `doc.*` short names) —
source it via the icon-request fallback (a Mac's SF Pro), or try the legacy Apple PUA `U+F8FF`.

**Caption-naming gotcha:** the community file labels captions with the **full descriptive** SF
Symbols name, not the SwiftUI shorthand — `mic.fill` is captioned **`microphone.fill`** (`microphone`,
`microphone.slash.fill`, `microphone.circle.fill` also exist). If a SwiftUI name (`mic…`, etc.)
returns `no-caption`, try the long form before assuming it's absent.

`arrow.turn.up.right` (SuggestedTaskRow "Move") and `speaker.wave.2.fill` (Agenda brief read-aloud)
were sourced from the row-component / brief SwiftUI. `circle` resolves to `U+100000` but is left out
here pending a screenshot check (TaskEventRow status ring — verify before use).

## Still needed (not in the community file's caption index)

- `mic.fill` — no ascii caption on the "Start here" page (the voice bar / composer mic). Source via
  the **icon-request frame** fallback (paste it using a Mac's SF Pro Display, then read the node's
  codepoint). Do **not** guess it from a web table.

## Usage

```js
await figma.loadFontAsync({family:'SF Pro', style:'Bold'});
const t = figma.createText();
t.fontName = {family:'SF Pro', style:'Bold'};
t.characters = String.fromCodePoint(0x100249); // calendar
t.fontSize = 22;
```

Never return raw PUA glyphs from a `use_figma` return value (the transport proxy 500s) — return
hex codepoints or booleans.
