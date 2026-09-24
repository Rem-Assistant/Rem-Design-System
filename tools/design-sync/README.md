# Design ↔ code sync (no Code Connect required)

Keeps the Figma design faithful to the shipping SwiftUI. Code is the source of truth; the design
is verified/generated against it. Three pieces, all driven by [`manifest.json`](./manifest.json)
(the machine-readable `REGISTRY.md`: Figma node-id ↔ SwiftUI source).

> Code Connect solves the *opposite* direction (surfacing code in Figma Dev Mode) and isn't used
> here. This is code→design fidelity.

## 1. Dev-resource links (the lightweight map)

Attaches a **"View source"** link to every canonical component in Figma Dev Mode, pointing at its
SwiftUI file on GitHub. Runs headlessly via the REST API (the plugin API `addDevResourceAsync` is
Dev-Mode-only).

```bash
FIGMA_TOKEN=<pat> node dev-resources.mjs
```

Idempotent-ish: re-running adds fresh links, so clear old ones in Dev Mode if you re-run often, or
extend the script to `GET /v1/files/:key/dev_resources` and de-dupe first.

## 2. Snapshot drift-check (the guardrail — recommended)

Fails CI when the design and code diverge — the class of bug you keep catching by eye (badge gap,
title clipping, wrong spacing token).

```
SwiftUI previews ──ImageRenderer──▶ artifacts/swiftui/<Name>.png   (macOS + Xcode)
Figma components ──REST /images──▶ artifacts/figma/<Name>.png      (export-figma.mjs)
                         │
                    compare.mjs  ──perceptual diff──▶ pass / fail + artifacts/diff/<Name>.diff.png
```

```bash
# design side (any runner with a token):
FIGMA_TOKEN=<pat> node export-figma.mjs artifacts/figma
# code side (macOS runner): render previews to artifacts/swiftui/<Name>.png
#   -> see snapshots/RemDesignSnapshots.swift.example (copy into the RemClaw test target)
# then:
npm i && node compare.mjs artifacts/swiftui artifacts/figma --maxDiffRatio=0.02
```

`--maxDiffRatio` is the fraction of pixels allowed to differ (font hinting / anti-aliasing noise);
start at `0.02` and tighten. Names must match between the two folders — that's what `manifest.json`
guarantees.

## 3. Generator (code → Figma)

You already own the primitive layer: `tokens/tokens.json` → Figma variables (Style Dictionary /
Tokens Studio). The structural layer (components/screens) is generated with the Figma plugin from a
spec. See [`../figma-gen/`](../figma-gen/) for the contract, the reusable build primitives, and a
worked example. Full "parse SwiftUI → emit Figma" codegen is a large custom build; the practical
path is: **tokens auto-generate**, **structure regenerates from the manifest/spec on demand**, and
**#2 catches anything that drifts in between.**

## What runs where

| step | needs | runner |
|---|---|---|
| dev-resources.mjs | `FIGMA_TOKEN` (Dev resources: write) | any |
| export-figma.mjs  | `FIGMA_TOKEN` (File content: read) | any |
| SwiftUI snapshots | Xcode + iOS simulator | **macOS** |
| compare.mjs       | node + `pixelmatch`,`pngjs` | any |

Token: figma.com → Settings → Personal access tokens. Put it in CI as the `FIGMA_TOKEN` secret.
