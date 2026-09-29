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

New Code Connect work uses parserless `.figma.ts` templates, following Figma's current guidance.
`Section` has separate SwiftUI and Compose templates under `code-connect/`, selected by
`figma.swiftui.config.json` and `figma.compose.config.json`. The Figma `Rows` Slot is read dynamically,
so Dev Mode can render nested connected rows instead of hardcoding examples. Local authoring and
validation use the parserless `code-connect/*.figma.ts` templates and `npm run check-code-connect`.
Any co-located `.figma.swift` / `.figma.kt` files are archived implementation examples: shipping
targets exclude them and Code Connect configs do not parse or validate them.

**They cannot be published on the current Figma plan.** Verified via the API: using Code Connect
requires a **Full or Dev seat on an Organization or Enterprise plan**. This account has Full seats
but only on **Starter/Pro** teams (Pro is *not* enough — the gate is Org/Enterprise). So publishing
and reading kit Code Connect are blocked until the design-system file lives on an Org/Enterprise team.

**What this does NOT block:** the core loop. Authoring components in Swift and projecting them into
Figma via generate-library works on the current plan and is the source of truth. Source links,
Dev Resources, and local parserless validation remain available. Publishing Code Connect mappings,
and using published mappings for the Figma-to-code return trip and hosted drift checks, remain
unavailable until the plan requirement above is satisfied.

**To activate later** (once on a qualifying plan, with the `figma` CLI + `FIGMA_ACCESS_TOKEN`):

```bash
npx figma connect parse --config figma.swiftui.config.json
npx figma connect parse --config figma.compose.config.json
npx figma connect publish --config figma.swiftui.config.json
npx figma connect publish --config figma.compose.config.json
```

The publish commands remain blocked until the file lives on an Organization or Enterprise plan with
a Dev or Full seat. The parserless templates can still be reviewed and parsed locally in the meantime.

## Related, existing docs

- `docs/UX-NATIVE-ALIGNMENT.md`, `docs/IOS_MAC_PARITY.md`, `docs/VISUAL_QA.md` — adjacent UI docs.
- `CLAUDE.md` principle 6 — the local **Native** reference app is the compare target for macOS
  token accuracy; reference-token web values here are approximations pending that check.
