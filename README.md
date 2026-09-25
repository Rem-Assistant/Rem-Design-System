# Rem Design System — docs

The Rem design system: one token source, native components per platform, documentation for
**humans and agents**. This folder is the human + source-of-truth home; the live component
previews are in the Claude Design "Rem Design System" project (React/CSS mirror).

## Start here

| File | What it is |
|---|---|
| [`SPEC.md`](SPEC.md) | The specification — source-of-truth model, token architecture, current-state audit, multi-platform fidelity, hosting, and the phased build plan. **Read this first.** |
| [`tokens/tokens.json`](tokens/tokens.json) | The single, platform-neutral token source (seed). Generates Swift + CSS. |
| [`templates/COMPONENT.md`](templates/COMPONENT.md) | The per-component documentation template. |
| [`components/Button.md`](components/Button.md) | A worked component page (example of the template filled in). |

## The one-paragraph model

`tokens.json` is the single source of truth for design tokens and **generates** both
`Shared/Views/DesignTokens.swift` (the shipping app) and the CSS token layer (the React mirror /
doc site). Components are **authored per platform**, not generated. Tokens come in two kinds —
*value* (a literal identical everywhere) and *reference* (aliases an Apple system color; Swift keeps
the adaptive reference, web uses an approximate hex). Every usage rule lives once (in a component
doc's front-matter) and renders twice: as prose do's/don'ts for people and as an adherence rule for
agents. See [`SPEC.md`](SPEC.md) for the full reasoning and the open decisions still needing input.

## Code Connect (scaffolded, DORMANT — needs an Org/Enterprise plan)

Code Connect files are authored and co-located with their components (e.g.
[`Sources/RemDesignSystem/Buttons/RemButton.figma.swift`](Sources/RemDesignSystem/Buttons/RemButton.figma.swift)),
and `figma.config.json` points the CLI at `Sources/**/*.figma.swift`. Each `.figma.swift` references
the **real Swift types** (so a rename shows the drift) but is **excluded from the SPM target** (see
[`Package.swift`](Package.swift)) — the shipping library never links `github.com/figma/code-connect`.

**They cannot be published on the current Figma plan.** Verified via the API: using Code Connect
requires a **Full or Dev seat on an Organization or Enterprise plan**. This account has Full seats
but only on **Starter/Pro** teams (Pro is *not* enough — the gate is Org/Enterprise). So publishing
and reading kit Code Connect are blocked until the design-system file lives on an Org/Enterprise team.

**What this does NOT block:** the core loop. Authoring components in Swift and projecting them into
Figma via generate-library works on the current plan and is the source of truth. Code Connect is the
*return trip* (Figma→code returns the real component) plus a drift guard (`figma connect check`) — a
later enhancement, not a dependency.

**To activate later** (once on a qualifying plan, with the `figma` CLI + `FIGMA_ACCESS_TOKEN`):

```bash
figma connect check      # validate mappings against the Figma nodes (drift guard)
figma connect publish    # push the bindings into Figma Dev Mode
```

⚠️ The `figma.config.json` schema was written without running the CLI (no CLI/token here). Legacy
per-framework parsers are unmaintained as of 2026-08-17; the future is `.figma.ts` templates
(`npx figma connect migrate`). `.figma.swift` still works and is the only Swift-native form — verify
the config against current `figma connect` docs on first run.

## Related, existing docs

- `docs/UX-NATIVE-ALIGNMENT.md`, `docs/IOS_MAC_PARITY.md`, `docs/VISUAL_QA.md` — adjacent UI docs.
- `CLAUDE.md` principle 6 — the local **Native** reference app is the compare target for macOS
  token accuracy; reference-token web values here are approximations pending that check.
