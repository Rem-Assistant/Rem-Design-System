# Figma generator (code → design, #3)

The Rem design system is **generated**, not hand-drawn — so it can be regenerated from the source of
truth and stays consistent. Two layers:

## Layer 1 — tokens (fully automated)

`tokens/tokens.json` is the single source. Style Dictionary / Tokens Studio generate **both**:
- the Figma **variables** (colors, spacing, radius, type), and
- the app's `DesignTokens.swift`.

"No hardcoded values" exists so every generated/hand-placed component *binds* these — change a token,
every instance updates, no re-generation needed. This layer already round-trips (`tokens: in sync ✓`).

## Layer 2 — components & screens (spec-driven, run via the plugin)

Structure is built with the Figma Plugin API (what the MCP `use_figma` tool runs). The repeatable
recipe lives in the **`rem-design-system` skill** (`.claude/skills/rem-design-system/`):

- `references/figma-gotchas.md` — the hard-won plugin traps (instance-swap defaults, `resize` locking
  the axis to FIXED, `setBoundVariableForPaint` resetting opacity, dynamic-page loading, SVG import…).
- `references/sf-symbols-map.md` — verified name→PUA-codepoint map, so symbols render as real glyphs.
- `manifest.json` (in `../design-sync/`) — node-id ↔ source, the regeneration index.

[`primitives.mjs`](./primitives.mjs) is the shared helper set every build uses (variable binding,
text, auto-layout rows/cols, SF-Symbol glyph). Inject it at the top of a `use_figma` call, then build
from a per-component spec that cites the SwiftUI source.

### The contract for a generated component

Each canonical component is: built on the iOS-26 kit · **variable-bound** (no hardcoded colors) ·
**real SF-Symbol glyphs** · **all real states as variant props** · **single-canonical** · and carries
a `description` linking its SwiftUI source. Verify with a screenshot **and** a property read before
declaring done (a wrong codepoint renders a plausible-but-wrong glyph — only the render proves it).

### Why not full "parse SwiftUI → emit Figma" codegen

That's a large, brittle custom compiler (SwiftUI layout ≠ Figma auto-layout 1:1). The pragmatic
system that actually holds: **tokens auto-generate (layer 1)**, **structure regenerates from the spec
on demand (layer 2)**, and the **snapshot drift-check (`../design-sync`, #2) fails CI the moment code
and design diverge** — so you get the safety of codegen without maintaining a compiler.
