# FigJam page-creation capability correction — 2026-10-05

## Evidence

- Installed `figma-use-figjam` guidance said not to call `figma.createPage`.
- In live FigJam file `QGFwluZMaWyJZUai2OxlXw`, the method was exposed and a
  successful authorized call created node `63:7040` as a real `PAGE`.

## Reusable correction

When a FigJam page is requested, inspect `typeof figma.createPage` in the live
file. If it is available, create exactly one requested or test page and read
back the node's type and parent type before continuing. Use Sections as the
fallback only when the live capability is absent or the verified call fails.
Live verified capability outranks stale adapter assumptions.
