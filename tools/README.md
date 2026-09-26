# Token pipeline (Phase 1)

`tokens.json` is the single source of truth. This generator emits the per-platform token files
from it. Nothing downstream is edited by hand.

## Usage

```bash
# from docs/design-system/
node tools/generate-tokens.mjs           # regenerate the files in tokens/generated/
node tools/generate-tokens.mjs --check   # CI drift guard: exit 1 if generated files are stale
```

No dependencies — plain Node (tested on v22). 

## What it emits

| Output | Consumer |
|---|---|
| `tokens/generated/DesignTokens.generated.swift` | iOS + macOS app (token **values** only) |
| `tokens/generated/tokens.generated.css` | React mirror + doc site (`--rem-*` custom properties, light + dark) |

The generator branches on each token's `$kind`:
- **value** → a literal on both platforms.
- **reference** → Swift keeps the adaptive Apple system-color reference (`#if os(iOS)/#else`); CSS
  uses the approximate hex from `web.light` / `web.dark`.

## CI drift guard (to wire up)

Add a step to the JS/lint CI job:

```yaml
- run: node tools/generate-tokens.mjs --check
```

It fails the build if someone edits a generated file by hand or forgets to regenerate after changing
`tokens.json`.

## Cutover plan — applying the generated Swift to the app (needs a Mac)

The generated `DesignTokens.generated.swift` is **proof that the pipeline works**; it is not yet
wired into the app. The cutover is deliberately a **separate step done on a Mac**, because it
changes runtime appearance and must be built in Xcode and visually QA'd (this cloud container is
Linux — no Xcode). Steps:

1. **Split the current file.** Move the hand-written helpers out of `Shared/Views/DesignTokens.swift`
   into `DesignTokens+Helpers.swift`: the `Color(hex:)` init, `ShimmerModifier` / `shimmering()`,
   and the macOS `Layout` frame-hint enum. These are *not* generated.
2. **Replace the token values** with the generated file (drop `DesignTokens.generated.swift` in, or
   have the build run the generator).
3. **Radius rename — swap call sites.** The names moved (old `large`=24 → `xlarge`; old `xlarge`=16
   → `large`). Find/replace with a temp token to avoid a double-swap:
   `.CornerRadius.large` → `__TMP__`, `.CornerRadius.xlarge` → `.CornerRadius.large`,
   `__TMP__` → `.CornerRadius.xlarge`. Do the same in the CSS `--rem-radius-*` consumers and the
   `_adherence.oxlintrc.json` enum. Verify no radius visually changed except where intended.
4. **Dynamic Type — verify scaling.** Typography now returns `Font.system(.body, …)` etc. Default
   sizes are unchanged (they equal Apple's defaults), so the base look holds; check large text sizes
   don't break dense rows (`TaskEventRow`, `ListRow`).
5. **Build both targets, screenshot before/after** (CLAUDE.md principle 6 / AGENTS.md both-platform
   rule), then merge.

Until step 5 passes on a Mac, keep the generated Swift here in `tokens/generated/`, not in the app.

# Skill sync

`skills.json` is the single source of truth for the process skills this repo vendors from
`samuelalake/agent-skills`. Same shape as the token pipeline: a data manifest → a `--check`-able
Node script → committed outputs (`.claude/skills/<skill>/`), guarded in CI. Nothing under the
vendored skill dirs is edited by hand.

## Usage

```bash
node tools/sync-skills.mjs --from <src>           # copy the pinned skills into .claude/skills/
node tools/sync-skills.mjs --from <src> --check   # CI drift guard: exit 1 if the copies drift
```

`<src>` is a local checkout of `skills.json` `.source.repo`; its git HEAD must equal `.source.pin`
(the script asserts this, so local and CI land byte-identical trees). No dependencies — plain Node.
The script does **no network I/O**: this container's git proxy is scoped to the Rem repos, so the
source is fetched *outside* the script — locally from a sibling checkout at the pin, in CI by
`actions/checkout` at the pin (see `.github/workflows/skills.yml`).

## Why vendored, not installed

This repo is the Agent Factory harness. The Factory's `catalog_skills(root, skill_dirs)` reads skill
dirs off disk from the checkout — it does not install skills. So the skills must live here as
committed files. `.claude/skills/` is the shared home: Claude Code reads it natively, and Agent
Factory reads it once `.claude/skills/` is added to its `skill_dirs` (a **factory-side** config
change, out of this repo). See `.claude/skills/SYNCED.md`.

## Updating

Bump `skills.json` `.source.pin` (after landing the change upstream), run the sync, commit the
refreshed `.claude/skills/<skill>` trees. CI (`.github/workflows/skills.yml`) fails on drift.
