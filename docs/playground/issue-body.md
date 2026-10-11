Build a bounded native Settings playground in SwiftUI and Compose using the settled Settings New masters. This is an engineering experiment, not a production Rem change.

Source: https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/?node-id=1825-29320
Entry: 1964:86819. Agent Settings: 1827:50855. Automations remains outside implementation scope.

Baseline: claude/ds-flows at 93d8588c8a020f2ed25c1ca69b2144c1c06d5838, plus the narrowly imported existing local files recorded in docs/playground/imported-baseline.json. Preserve all original local changes.

Scope: reusable row/section/icon variants, a component gallery, Settings → Rem → Agent Settings, native Back, cancellable loading and recoverable error fixtures, local control editing with Cancel rollback. Other Settings rows are visual references, not working product features. No backend connection or destructive actions.

Acceptance: both native apps compile and run; focused interaction tests; readable paired native screenshots; independent review of component reuse, Figma fidelity, cancellation and navigation. Report exact code revision and any unverified evidence separately.

Execution: engineering hub owns the bounded local lane. Existing Factory workflow forces main and enables Figma writes, which do not match this baseline or read-only Figma scope. Do not attach dispatch labels or run the builder against the stale issue 69. Capture the routing mismatch and time spent as trial overhead; no Factory overhaul, merges or deployment.
