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

## Related, existing docs

- `docs/UX-NATIVE-ALIGNMENT.md`, `docs/IOS_MAC_PARITY.md`, `docs/VISUAL_QA.md` — adjacent UI docs.
- `CLAUDE.md` principle 6 — the local **Native** reference app is the compare target for macOS
  token accuracy; reference-token web values here are approximations pending that check.
