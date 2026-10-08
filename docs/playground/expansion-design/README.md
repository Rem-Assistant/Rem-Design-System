# Agenda Suggestions and Onboarding Voice source snapshot

Read-only Figma evidence captured on 2026-10-08 in file `af4yDqCzp57jds9lkFiIaO`.
Start with [COVERAGE.md](COVERAGE.md), [BUILDER-BOUNDARIES.md](BUILDER-BOUNDARIES.md)
and [STRUCTURAL-SCHEMA.md](STRUCTURAL-SCHEMA.md). The compressed packet preserves
24 fresh context/PNG sets, three authenticated structural reads and separately
labeled reused Settings core evidence. No Figma revision ID was returned; hashes
identify saved bytes only.

`source-evidence.tar.gz` is the source owner's immutable archive, 2,266,257 bytes.
SHA-256: `494a323503cf281b37df737c9e987c9b97b48fcb3a79511aba33c8de1b7da2ae`.
The archive's `expansion/SHA256SUMS` covers 99 files; the archive also includes that
checksum file and the packet manifest. No runtime captures are included.

Extract into a task-local temporary directory to read the individual original PNGs
and raw responses (run from the repository root):

```sh
mkdir -p /tmp/rem-playground-source
shasum -a 256 docs/playground/expansion-design/source-evidence.tar.gz
tar -xzf docs/playground/expansion-design/source-evidence.tar.gz -C /tmp/rem-playground-source
(cd /tmp/rem-playground-source/expansion && shasum -a 256 -c SHA256SUMS)
```

The expected archive digest is above; extraction is not needed by CI. The exact
trusted verifier contract is also checked in as
`tools/design-sync/playground-source-contracts.json` (SHA-256
`fa4cdc0c367517158173a7731636ba9ed04a073f9b3e477ef072fbf7bb2a7cde`).
The read-only verifier uses bounded Figma REST GET requests from the trusted base,
not executable files or assertions supplied by a candidate PR. REST page type
`CANVAS` corresponds to the snapshot's plugin type `PAGE`; local first-child `y`
is checked via REST `relativeTransform` with `geometry=paths`. Missing fields fail
verification rather than being assumed conformant. Live REST verification remains
pending; source checks do not establish native layout or runtime correctness.

Scope excludes Connectors until its provider mismatch is reconciled, and excludes
Check-in, Automations, Inspector rules, Chat and the proposed Plain icon policy.
See [playground-expansion.md](../../contracts/playground-expansion.md) for exact
branch/label permissions, native capture names and delivery gates.
