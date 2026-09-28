# Design ↔ code sync

Keeps Figma, SwiftUI, and Compose aligned. The source map, parserless Code Connect templates,
live Figma structure, and visual evidence form one delivery model. [`manifest.json`](./manifest.json)
is the machine-readable `REGISTRY.md`: Figma node-id ↔ platform source.

## 1. Parserless Code Connect and dev-resource links

SwiftUI and Compose mappings live as TypeScript `.figma.ts` templates under
[`code-connect/`](../../code-connect/). Separate configuration files give each platform its correct
language label while the official Figma CLI parses the shared template format.

```bash
npm run check-code-connect
```

Publishing those mappings is a separate Figma account/seat capability. An unavailable publisher
does not create another authoring format.

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
  --maxDiffRatio=0.02 \
  --require=Consent-default-light,Consent-terms-light,Consent-privacy-light \
  --exclusive-prefix=Consent-
```

`--maxDiffRatio` is the fraction of pixels allowed to differ (font hinting / anti-aliasing noise).
`--require` makes every named pair mandatory. The optional, reusable `--exclusive-prefix` rejects
any output in that namespace that is not in the required set, so stale or invented states cannot be
silently accepted.

For screen delivery, `tools/render-evidence/contracts.json` also declares the canonical Figma node
for each required state and any optional flow/documentation waypoint. `design-drift.yml` associates
fresh live exports with the pull request's exact head and uploads a digest manifest. The privileged
`publish-builder-delivery.yml` workflow accepts that artifact only from the matching same-repository
run, trusted base-branch workflow, PR, head SHA, and run attempt, then verifies the exact file set and
every digest before adding a **Figma Reference** column.

The secret-bearing export uses `pull_request_target` with read-only permissions and checks out only
the event's base SHA. It never checks out or executes pull-request code. Trusted code fetches exactly
`manifest.json` and `structure-contract.json` from the PR head through the GitHub Contents API,
validates their schema, size, node ids, collection bounds, and Figma file key, and treats them only as
data. The base-branch exporter uses the trusted render-evidence contract and exports only the selected
contract's references and waypoints. Missing required nodes fail; absent unrelated legacy registry
nodes do not block a scoped delivery.

The base-branch verifier checks the live Figma document against the fetched head structure contract.
Its report, the exact head contract, and the exact head manifest are digest-bound into the reference
artifact. The publisher requires a successful producing run and revalidates those digests plus the
report's head, file, and completed status. Exact-head SwiftUI/Compose parity remains the responsibility
of `screenshots.yml` and Reviewer. The scheduled/manual lane retains the older staging-app visual
comparison as a health check and is not presented as pull-request evidence.

Add one `delivery-scope:<contract-name>` label to a PR when collateral contract edits would otherwise
make the evidence scope ambiguous. The label also explicitly opts that delivery into authenticated
Figma reference and structure evidence; an unscoped component-only or unrelated screen delivery does
not wait for the consent artifact. The publisher accepts at most one label and resolves it only
against the trusted contract registry. A contract can opt into `exclusive_output_prefixes` to reject
stale or invented outputs in its namespace while allowing unrelated regression snapshots. This keeps
the mechanism reusable without embedding consent-specific state names in delivery code.

The current Onboarding structure contract verifies one canonical screen inventory plus the Consent
documentation/prototype pair. It checks the attached `Mobile Flow Documentation` hierarchy through
Sections, Rows, Steps, `Mobile Placeholder`, the exact Screen slot, and its canonical screen instance.
The second Default instance is a branch-return waypoint between Terms and Privacy and is excluded
from evidence counts. The three prototype destinations must remain direct canonical instances.
These strict relationships catch detached lookalikes and overlay siblings that can appear visually
correct while no longer inheriting library changes.

Every touched component family uses horizontal auto-layout documentation columns: canonical masters
first, followed by the attached metadata template. Composition roots inherit their parent surface
unless they explicitly own one, and equal-priority horizontal actions divide the available width.
Theme and Platform remain independent axes. Full prototype reactions are verified in authenticated
Figma/Present inspection because the public REST node schema does not return those interactions.

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
