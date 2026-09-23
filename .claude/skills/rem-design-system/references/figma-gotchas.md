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
  "SF Pro can't render symbols" belief came from *wrong* codepoints off web tables. Verified:
  `chevron.right = U+10018A`, `chevron.left = U+100189`,
  `message.badge.waveform.fill = U+100F02`.
- The community file "SF Symbols | Text objects for Figma" (`eMocgqr193EB694SlKtYZP`) uses
  **"SF Pro Display"** (not installed in the cloud plugin env — only "SF Pro" is) and lists
  symbols as text nodes **named by glyph**, with **no name→codepoint index**. So it confirms
  the approach but isn't a lookup table.
- **Sourcing codepoints:** paste the needed symbols into the **icon-request frame** (a
  labeled grid the user fills using their Mac's SF Pro Display), then read each node's
  codepoint (`[...t.characters].map(c=>c.codePointAt(0).toString(16))`) and reproduce with
  `figma.createText()` + `{family:"SF Pro"}` + `String.fromCodePoint(cp)`. **Never return raw
  PUA glyphs in a `use_figma` return value** — the transport proxy 500s on them; return
  hex codepoints or booleans.

## Kit / library

- Import kit pieces with `importComponentByKeyAsync` / `importComponentSetByKeyAsync`. Useful
  keys: iPhone 16 Pro bezel `9a6d00e63b15332f53086d3108f8bc143591f074` (screen area 402×874);
  Navigation Bar - iPhone (Compact) `d299571689380910c1e8194200965152c2717993` (Style variant:
  Default/Large + Show Search/Leading/Trailing/Title props); iOS 18 Row set
  `8bb9d297eb2882c75b4d23eb7c90eea45f000f6d` (Height=Regular **44** / Tall **60**).
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
