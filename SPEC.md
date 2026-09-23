# Rem Design System — Specification

> **Status:** Draft / Phase 0. This document defines the target shape of the Rem design system:
> one platform-neutral token source, components authored per platform, and documentation that
> serves **both humans and agents**. It is grounded in the code that exists today
> (`Shared/Views/DesignTokens.swift`, the Claude Design "Rem Design System" project, and the
> `_adherence.oxlintrc.json` rule set), not a greenfield ideal.
>
> **Scope discipline:** this system is *leverage* — it keeps humans and design-generating agents
> on-rails and makes the app consistent across platforms. It is **not** the product differentiator,
> and it is **not** trying to become Fluent/Carbon in governance or breadth. See §0 and §9.

---

## 0. What this is (and is not)

**Is:** a small, opinionated, multi-platform design system (~25 components) with a single token
source of truth, native implementations per platform, and clean documentation readable by people
*and* by agents that generate Rem UI. Small enough to be excellent; a credible portfolio piece
precisely because it shows judgment, not sprawl.

**Is not:**
- A governance program (RFCs, versioning policy, contribution process, multi-team adoption).
  Rem is one product, not a platform org shipping to thousands of external consumers.
- The thing that wins against competitors. Rem's bet is *proactivity*; the design system serves
  that work, it does not replace it.
- A reason to flatten Apple's adaptive system colors into static hex on device (see §2).

---

## 1. Source of truth — the core problem

### Today (as-is)

| Artifact | Role | Location | Problem |
|---|---|---|---|
| `Shared/Views/DesignTokens.swift` | The values the **shipping app** uses | this repo | De-facto source of truth, but Swift-only |
| `@rem/design-system` (React) + Claude Design bundle | Web mirror + design reference | **not in this repo** (external / generated) | Hand-maintained mirror of the Swift side → drifts |
| `_adherence.oxlintrc.json` | Machine-readable rules (props, enums, no-raw-hex) | Claude Design project | Excellent, but describes the React side only |

There is **no committed single source** that generates both. The Swift tokens and the CSS tokens
are reconciled by hand. That is the drift this branch exists to fix.

### Target (to-be)

```
                    tokens.json  (platform-neutral, the ONE source)
                   /            \
        generate  /              \  generate
                 v                v
   DesignTokens.swift        _ds_bundle.css / tokens.css
   (iOS + macOS app)         (React mirror + doc site)
```

- **Tokens** are single-sourced in `docs/design-system/tokens/tokens.json` and **generated** into
  each platform. Neither generated file is edited by hand; both carry a "generated — do not edit"
  header.
- **Components** stay **hand-authored per platform** (SwiftUI for the app, React for the
  mirror/preview). Components are not generated — only the token layer is. This mirrors how Fluent
  and Material actually work: shared tokens, native component code.
- A **CI drift check** fails the build if a generated file is out of sync with `tokens.json`.

Generation direction is **`tokens.json` → Swift/CSS**, not the reverse, because `tokens.json` can
express things Swift cannot (e.g. the web hex approximation of an Apple system color — see §2).

---

## 2. Token architecture — value tokens vs reference tokens (the key insight)

Rem's tokens are **not all the same kind**, and this is the single most important thing to get
right. Looking at `DesignTokens.swift`, colors fall into two families:

### 2a. Value tokens — brand-owned, real values

These have a literal value that is identical on every platform. They single-source cleanly and
generate to literals everywhere.

- **Spacing:** `xs 4 · sm 8 · md 12 · lg 16 · xl 24 · xxl 48 · xxxl 120`
- **Radius:** `small 8 · medium 12 · large 24 · xlarge 16` *(note the naming smell — see §3)*
- **Type scale (pt):** `largeTitle 34 · title1 28 · title3 20 · body 17 · subheadline 15 ·
  footnote 13 · caption1 12`, each `regular (400)` / `bold (700)`; `title3` tracking `-0.45`;
  `chatCode 17 mono`
- **Opacity:** `deemphasized 0.55`
- **Brand:** `brandBlue #0C50FF`; `brandBlueOnFill` = `#0C50FF` (light) / `#6E9BFF` (dark)

### 2b. Reference tokens — platform-aliased, adaptive

These do **not** have a fixed value. On Apple platforms they are *references to system colors*
(`Color(.systemBackground)`, `Color(.label)`, …). That reference is doing real work:

- free light/dark adaptation,
- automatic high-contrast + accessibility behavior,
- the native "feel" that makes the app look like it belongs on the OS.

**Flattening these to static hex on device would be a regression.** So `tokens.json` records
*both* representations for each reference token:

```jsonc
"color.background.primary": {
  "$type": "color", "$kind": "reference",
  "swift": { "ios": "Color(.systemBackground)", "macos": "Color(nsColor: .windowBackgroundColor)" },
  "web":   { "light": "#FFFFFF", "dark": "#000000", "$approximate": true }
}
```

- **Swift generation** emits the system-color reference (adaptive, native).
- **Web generation** emits the light/dark hex *approximation* (the browser has no Apple palette).

Reference tokens in Rem today: `background.{primary,secondary,tertiary}`,
`label.{primary,secondary,tertiary}`, `separator`, `fill.tertiary`, `buttonBackground`,
`pillBackground`, and the seven `system.*` accents. Their web values are approximations of Apple's
published system palette and must be checked against the **Native** reference app (CLAUDE.md
principle 6) before they are called canonical.

> **Consequence for the pipeline:** a naive "dump every token to hex" generator is wrong for Rem.
> The generator must branch on `$kind` (`value` → literal on both; `reference` → platform-native on
> Swift, approximate hex on web).

---

## 3. Current-state audit

**Inventory:** 25 components (23 `general` + 2 `toolresultcard`), 58 CSS custom properties.

**Strengths (build on these):**
- **Machine rules already exist.** `_adherence.oxlintrc.json` forbids raw hex / raw px, forbids
  bare `<button>` (use `<Button>`), and pins every component's allowed props + enum values. This is
  the agent-readable layer, already ~70% there.
- **Real system rules, not just components.** The README encodes genuine constraints: cards share
  one material language (`solid` = actionable, `inset` = quiet tool output, `glass` = floating
  status); **one badge only** (`Pill`); style through props, never by overriding classes; use the
  platform system font, no webfont.
- **Small, coherent surface.** 25 components is Carbon-*quality* territory without Carbon-*scale*
  effort.

**Gaps (what this system must close):**
1. **Two sources, hand-synced** (§1). The headline problem.
2. **Typography is fixed-size, not Dynamic Type.** `Font.system(size: 17)` etc. do not scale with
   the user's text-size setting. This is an accessibility gap on iOS. It may be an intentional
   density choice (parity with the Native reference) — so it is an **open decision** (§10), not a
   silent fix.
3. **Radius naming is backwards.** `large = 24` but `xlarge = 16`. Confusing for humans and agents
   alike. Candidate for a documented rename (breaking token change — §10).
4. **No human-facing per-component guidance.** No when-to-use, anatomy, do/don't, or accessibility
   notes. This is the doc-site work and the portfolio surface.
5. **React source provenance.** `@rem/design-system` is referenced but not committed here; where it
   lives and whether it should be in-repo is unresolved (§10).

---

## 4. Human-readable vs agent-readable — the line

**They are two renderings of one source. The line is *presentation*, not *content*.** A rule like
*"never place two primary buttons in one row"* should exist **twice**, authored **once**:

- as a **prose do/don't** with an example, for a person reading the doc site;
- as an **adherence rule / constraint**, for an agent generating UI.

| Layer | Audience | Artifacts | Status |
|---|---|---|---|
| **Token source** | both (via generation) | `tokens/tokens.json` | to build (seeded here) |
| **Agent-readable (web)** | design-generating agents, lint | `_adherence.oxlintrc.json`, `*.d.ts`, `*.prompt.md`, `_ds_manifest.json` | mostly built |
| **Agent-readable (Swift)** | the app + agents editing it | SwiftLint custom rules in RemClaw's `.swiftlint.yml` (`ds_no_raw_color_literal`, `ds_no_raw_system_color`, `ds_no_raw_font_size`, `ds_no_hardcoded_corner_radius`, `ds_no_magic_padding`) | added |
| **Human-readable** | designers, engineers, portfolio viewers | the doc site (per-component pages) | to build |

> The two agent-readable linters are the *same guardrails on both platforms*: oxlint keeps React
> UI on tokens; SwiftLint keeps the SwiftUI app on `DesignTokens`. Token-definition files are
> excluded from the Swift rules (they legitimately hold the literals).

**Authoring model:** each component doc (see `templates/COMPONENT.md`) carries its do's/don'ts in a
structured front-matter block. A small build step can emit the human page *and* contribute the
machine rule from the same block — so the two never disagree. That is the concrete answer to
"where is the line": there is one authored rule, rendered for two readers.

---

## 5. Multi-platform fidelity model

**Fidelity means token + intent parity, not pixel identity.** The systems that do multi-platform
well (Fluent, Material) share the token layer and the *meaning* of each component, and let the
**form diverge to match each platform's native idiom**.

- **Shared across platforms:** tokens (via §1/§2), each component's intent and information
  architecture ("a row showing a task with status + time"), motion feel, and brand.
- **Divergent by design:** the native container. iOS grouped `List`/`Section` (inset, rounded) vs
  Android `LazyColumn` + Material list items vs the web's CSS surfaces. Making iOS chrome appear on
  Android (or vice versa) is the **anti-pattern**, not the goal.
- **The React mirror is the *web* interpretation**, not the canonical look. iOS is Rem's primary
  home; the web rendering must not silently become the reference. Where the web preview diverges
  from the shipping SwiftUI, the SwiftUI wins.

**Android is out of scope now** and aspirational. The reason to get the token layer right anyway is
that a clean token contract is exactly what makes a future Android target cheap instead of a
rewrite.

---

## 6. Documentation template (per component)

Every component page follows one structure (full template in `templates/COMPONENT.md`, worked
example in `components/Button.md`):

1. **Overview** — one sentence: what it is, and the SwiftUI type it mirrors.
2. **When to use / When not to use** — and what to reach for instead.
3. **Anatomy** — labeled parts.
4. **Variants & states** — every legal value, with a preview.
5. **Do's & don'ts** — side-by-side, authored as the structured block that also feeds the lint.
6. **Accessibility** — focus, labels, contrast, Dynamic Type, hit target.
7. **Tokens used** — links back to `tokens.json`.
8. **API** — props table (sourced from `*.d.ts`).

---

## 7. Repo layout & hosting

### Layout (this branch introduces `docs/design-system/`)

```
docs/design-system/
  README.md                 index / how to navigate
  SPEC.md                   this document
  tokens/
    tokens.json             THE single token source (seed committed here)
  templates/
    COMPONENT.md            the per-component doc template
  components/
    Button.md               worked example (more to follow in Phase 2)
```

### Hosting the human doc site — a decision to make (§10)

- **(a) Markdown-in-repo (start here).** Zero infra; GitHub renders it; humans can read it today.
  This is where Phase 0–2 live.
- **(b) A static site (portfolio-grade).** Astro Starlight / Nextra / VitePress built from the same
  Markdown, with **live component previews reused from the Claude Design bundle** (`_ds_bundle.js`
  + `styles.css`). This is the shareable portfolio artifact.
- **(c) Claude Design as the rendered viewer.** Already exists; link to it for live cards while the
  static site is built.

**Recommendation:** start (a) now, reuse (c)'s bundle for live previews, graduate to (b) when the
component docs are written and it's worth polishing as a portfolio piece.

---

## 8. Build plan (phased)

- **Phase 0 — this PR.** Spec (this file), `tokens.json` seed, doc template, one worked component
  example (Button). No app code touched; docs-only, low risk.
- **Phase 1 — token pipeline.** Choose the generator (Style Dictionary with a custom format, or a
  small purpose-built script — the `$kind` branching in §2 is simple enough that a ~100-line script
  may beat pulling in Style Dictionary). Generate `DesignTokens.swift` and the CSS token layer from
  `tokens.json`; add the "generated — do not edit" header; add a CI drift check.
- **Phase 2 — human docs.** Author all 25 component pages from the template, pulling the API from
  `*.d.ts` and the rules from `_adherence.oxlintrc.json`. This is parallelizable across agents.
- **Phase 3 — doc site.** Stand up option (b); wire live previews from the Claude Design bundle.
- **Phase 4 — product-design skill (separate goal/session).** A skill that reads `tokens.json` +
  adherence rules + screen templates so a design-generating agent stays on-rails.

---

## 9. Non-goals

- Fluent/Carbon-scale governance, versioning policy, or contribution process.
- Android support **now** (the token layer keeps it cheap **later**).
- Making the design system the product's competitive moat.
- Flattening Apple system colors to static hex on device.
- Generating component *code* (only tokens are generated; components are authored per platform).

---

## 10. Decisions (resolved 2026-09-23)

1. **Dynamic Type — ADOPTED.** Type roles map to Apple `Font.TextStyle` and Swift emits scaling
   fonts. Current sizes already equal Apple's default Dynamic Type sizes, so the default appearance
   is unchanged — the app just gains scaling. Encoded in `tokens.json` (`textStyle` per role).
2. **Radius rename — DONE (in source).** Monotonic scale `small 8 · medium 12 · large 16 ·
   xlarge 24` in `tokens.json`. Value multiset unchanged; names moved. Call-site swap is part of the
   Mac cutover (see `tools/README.md`).
3. **React `@rem/design-system` source — EXTERNAL.** It does **not** live in this repo. Provenance:
   its own home under the **Rem-Assistant** org (see open item below on where the *design-system
   docs* themselves live).
4. **Generator — small custom script.** `tools/generate-tokens.mjs`, dependency-free Node. The
   `$kind` branching is simple enough that Style Dictionary would be overkill.

5. **Home — DEDICATED REPO.** This tree + the token pipeline live in **`Rem-Assistant/rem-design-system`**
   (this repo). RemClaw (the app) consumes the generated Swift tokens and keeps a pointer to here.
6. **Naming — "Rem Design System"** (kept; matches the Claude Design project + `@rem/design-system`).

### Still open

- **Doc-site hosting** — leaning Carbon's look via `gatsby-theme-carbon` (or Starlight themed to
  match). Phase 3.
- **Component parity verification** — how the SwiftUI / React (and future Compose) renders are
  confirmed to match each component's `.prompt.md` spec. Token parity is automated (drift check);
  component parity is visual (screenshots against the spec). See the app repo for the render/QA path.
