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
npm ci && node compare.mjs artifacts/swiftui artifacts/figma \
  --threshold=0.3 \
  --maxDiffRatio=0.10 \
  --require=Consent-default-light,Consent-terms-light,Consent-privacy-light
```

`--threshold` is pixelmatch's per-pixel YIQ color-distance tolerance, not a percentage of the screen;
`0.3` absorbs the known CoreAnimation-versus-Figma font rasterization, shadow, and semantic-color
differences. `--maxDiffRatio=0.10` is the actual changed-area ceiling. Consent requires three
light-mode destinations: the consent screen plus Terms and Privacy. The shipping view has no
consent-local loading/error state, and the shared Figma variable mode supplies dark appearance
without duplicating frames. A fresh hosted run must replace the superseded calibration before merge. Reviewer
still performs the paired visual decision; this lane is a fail-closed drift guardrail, not an
automated parity approval. Any threshold change requires a new three-state baseline in this document
and Reviewer approval. The exact approved measurements, Figma node ids, thresholds, and source run
are also committed in
[`baselines/consent-2026-09-28.json`](./baselines/consent-2026-09-28.json) so this rationale is
independently machine-readable.

At least one basename must match between the two folders or the comparison fails;
`manifest.json` supplies those paired names. CI additionally passes all three consent basenames through
`--require`; a missing code snapshot, Figma export, or comparison fails the job. Every run writes
`artifacts/design-drift-report.json`, publishes it in the job summary, and uploads it as the
`design-drift-report` artifact. The report records the exact head, thresholds, required set, missing
set, and per-state diff ratio. Its `status` is `completed` for a finished comparison, `error` when
the comparator itself could not read an input, or `incomplete` when an earlier render/export step
failed before comparison started. The always-run finalizer creates that last form so upstream CI
failures cannot silently remove the diagnostic artifact.

The same run executes `verify-figma-structure.mjs`. Its committed `structure-contract.json`
requires the shared Onboarding page to have the canonical inventory plus a consent documentation/prototype pair and the exact editable hierarchy
`Placeholder Sections` → `Placeholder Section` → `Placeholder Rows` → `Placeholder Flows` →
`Mobile Placeholder` → screen. It also checks that all four documented journey frames share one
sequential row and the three prototype frames are direct children of `01B · Consent · Prototype`. This catches
legacy slot wrappers and visually aligned overlay siblings that image comparison cannot distinguish.
The exact-head structure digest and resolved ancestry are uploaded as
`artifacts/figma-structure-report.json` beside the drift report.

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
