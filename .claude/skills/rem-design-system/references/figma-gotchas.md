# Figma `use_figma` plugin gotchas

Hard-won traps from building this system. Each cost real time when hit cold. The plugin
runs JS against the Figma Plugin API; `figma` is the global.

## Instances

- **Nested-instance *size* cannot be overridden.** Inside an instance you can override text,
  visibility, fills, and the swapped main component — but **not** the width/height of a
  nested instance. `node.resize()` on it is a silent no-op. So a Chevron-sized trailing slot
  swapped to a Switch stays chevron-sized and clips.
  - **Fix A (preferred):** make the accessory **HUG-wrap a fixed inner track**. A component
    whose frame HUGs its content re-sizes correctly when swapped via an instance-swap
    property (Button did; Switch did once we wrapped its 51×31 track in a HUG frame).
  - **Fix B:** bake the accessory at the right size in a **dedicated master** (resize *does*
    work inside a master/component, just not inside an instance).
- **Nested-instance *position* cannot be overridden either** (`set_y`: "relative-transform
  cannot be overridden"). To re-lay-out a screen you cloned, **`detachInstance()`** it into a
  frame first, then move children freely.
- **`swapComponent` preserves the current rendered size**; it re-hugs only if the new main is
  HUG-sized. An instance-swap *property* behaves the same. Neither resizes a FIXED accessory.
- **`INSTANCE_SWAP` property default must be a LOCAL component node id** (e.g. `"110:52"`),
  **not** a key. Passing a key → "Property value is incompatible with component property
  type". `preferredValues` (the swap menu) *do* take `{type:"COMPONENT", key}`.
- **`setProperties` is all-or-nothing.** One bad key (e.g. a `Label` that doesn't exist on
  that component, or a TEXT prop set to `null`) throws and **rolls back the whole call** —
  every other prop silently fails too. Set a nested sub-component's props on that nested
  instance, not the parent (e.g. the Button's `Label#…`, the Content slot's `Title#…`).
- **Renaming a property** via `editComponentProperty(oldName, {name})` keeps the `#id`
  suffix, so existing instances keep working. Renamed `Accessory#151:0` → `Trailing
  Accessory` with zero breakage.

## Auto-layout / sizing

- **`resize(w, h)` LOCKS the resized axis to FIXED.** On an auto-layout frame this collapses
  it (a card stuck at height 10 clips its rows). **After any `resize`, re-set
  `primaryAxisSizingMode="AUTO"`** (and/or `counterAxisSizingMode`) to restore hug.
- **Set `layoutSizingHorizontal/Vertical="FILL"` only *after* the node is a child of an
  auto-layout parent** — otherwise "node must be an auto-layout frame or a child of one".
- `appendChild(x)` returns **undefined**, not the node — `parent.appendChild(x).foo = …`
  throws. Capture the node in a var first.
- **Wrapping/`layoutWrap="WRAP"` grids often screenshot stale** right after building (tiles
  look collapsed). The dims are correct — verify with a property read or re-screenshot a
  child node; nudging the frame `x` forces a relayout.
- A toggle/switch hugging only its knob renders as a ring — give the **track** a FIXED size
  and let the component HUG the track.

## SF Symbols (the icon "wall" is not real)

- **`SF Pro` renders SF Symbols** when you use the **correct PUA codepoint**. The earlier
  "SF Pro can't render symbols" belief came from *wrong* codepoints off web tables (never
  trust those). Verified: `chevron.right = U+10018A`, `chevron.left = U+100189`,
  `message.badge.waveform.fill = U+100F02`, `calendar = U+100249`.
- **Sourcing codepoints — the community file IS a lookup table (spatial pairing).** The file
  "SF Symbols | Text objects for Figma" (`eMocgqr193EB694SlKtYZP`) holds two kinds of TEXT
  nodes on its **"Start here"** page: a **caption** node whose `characters` are the ascii
  symbol name (e.g. `"calendar"`), and next to it the **glyph** node whose `characters` are
  the single PUA glyph (its `name` is the glyph, not the symbol name — so you can't grep the
  name). They pair **by position**: for a given caption, the paired glyph is the nearest glyph
  node on the **same row, ~68px to the LEFT** (`dy≈0, dx≈-68`). To resolve a symbol:
  1. `findAllWithCriteria({types:['TEXT']})` on the current page (~28k nodes; don't
     `loadAllPagesAsync`).
  2. Build the glyph list = single-char nodes with `codePointAt(0) >= 0x100000`, recording
     each center from `absoluteBoundingBox`.
  3. Find the caption node whose `characters` equal the symbol name, take the nearest glyph by
     2D distance (the same-row left neighbour wins), read `cp.toString(16)`.
  - **Anchor the method with a known answer** (`chevron.right` must come back `10018a`) before
    trusting a new lookup — the pairing offset was proven that way.
  - **Fallback** (symbol/name absent, or verifying): paste the needed symbols into the
    **icon-request frame** (the user fills it using their Mac's SF Pro Display) and read each
    node's codepoint directly.
  - Reproduce with `figma.createText()` + `{family:"SF Pro", style:"Bold"}` +
    `String.fromCodePoint(cp)`. **Always screenshot the result to confirm the glyph** — a
    wrong codepoint renders a plausible-but-wrong symbol, not tofu, so only the render proves
    it. **Never return raw PUA glyphs in a `use_figma` return value** — the transport proxy
    500s on them; return hex codepoints or booleans.

## Kit / library

- Import kit pieces with `importComponentByKeyAsync` / `importComponentSetByKeyAsync`. Useful
  keys: iPhone 16 Pro bezel `9a6d00e63b15332f53086d3108f8bc143591f074` (screen area 402×874);
  Navigation Bar - iPhone (Compact) `d299571689380910c1e8194200965152c2717993` (Style variant:
  Default/Large + Show Search/Leading/Trailing/Title props); iOS 18 Row set
  `8bb9d297eb2882c75b4d23eb7c90eea45f000f6d` (Height=Regular **44** / Tall **60**).
- **Configure the kit Navigation Bar — don't hand-build a `nav` frame.** Props: `Title` (TEXT),
  `Show Leading/Trailing/Search/Prompt` (BOOL), `Show Background` (BOOL — transparent scroll-edge
  vs. material; this is the "inherit page bg" control), `Style` (Default/Large). Each leading/
  trailing **button exposes a `Symbol#…` TEXT property** — set it to the **SF Pro glyph**
  (`String.fromCodePoint`, e.g. ellipsis `U+100360`) to render any SF Symbol; it does **not**
  need an Apple symbol *component* (an earlier belief that it did was wrong). The nav bundles its
  own status bar (~98pt tall incl. status), so remove the separate status-bar instance when you
  adopt it. `setProperties` on the nested button instance sets its `Symbol#…`.
- `combineAsVariants([comps], parent)` builds a variant set; name members `Prop=Value`.
- **Annotations:** `node.annotations = [{label, properties:[{type:"height"},{type:"padding"},
  {type:"itemSpacing"}, …]}]` shows labels **and measurement values** in Dev Mode (not in a
  design-view screenshot). There is **no** `figma.createMeasurement` (no free-floating
  dimension-arrow API) — annotation properties are the closest.
- `figma.createComponentFromNode`, `page.appendChild(comp)` to move a master between pages
  (instances keep working — they reference by id).
- Change pages with `await figma.setCurrentPageAsync(page)`. Never
  `loadAllPagesAsync`/`setPluginData`/`createImageAsync` (unsupported).

## Screenshots

- Prefer the URL result and download it; the proxy sometimes blocks `figma.com` asset
  downloads, so `enableBase64Response: true` gives an inline image you can actually see.
