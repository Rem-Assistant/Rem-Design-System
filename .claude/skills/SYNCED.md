# Skills in this directory

Two kinds live here. Know which before you edit.

## Native to this repo — edit freely
- **`rem-design-system/`** — the Figma/design-system skill authored and maintained here.

## Vendored (synced) — DO NOT hand-edit
- **`product-design-delivery/`** — how to produce design-ready task packets.
- **`verified-delivery/`** — evidence discipline for delivery.

These two are vendored from **`samuelalake/agent-skills`** at the commit pinned in
[`../../skills.json`](../../skills.json), by [`../../tools/sync-skills.mjs`](../../tools/sync-skills.mjs).
They are the `tokens.json → generated/` of skill vendoring: source-of-truth is elsewhere, the copies
here are generated, and CI (`.github/workflows/skills.yml`) fails on any drift.

To change them: bump `skills.json` `.source.pin` (or land the change upstream first), then
`node tools/sync-skills.mjs --from <checkout of the source at the pin>` and commit the refreshed
trees. A hand-edit will be reverted by the next sync and will fail CI in the meantime.

## Why they live here (not installed)

This repo is the Agent Factory harness. The Factory's `catalog_skills(root, skill_dirs)` reads skill
directories **off disk from the checkout** — it does not install skills. So the process skills must
physically exist here. Claude Code also reads `.claude/skills/`, so one copy serves both consumers,
and all three skills above are cataloged together (fits `max_skills = 3`).

> **Factory-side, out of this repo:** Agent Factory's `skill_dirs` must include `.claude/skills/`
> for it to catalog these (its defaults are `skills/` / `skill/`).
