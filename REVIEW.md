# REVIEW.md — reviewer rubric

Read by the Agent Factory reviewer via `project.context_files`. This turns the paired render from
*evidence* into a *gate* — the rule that moves cross-platform drift-catching off the founder and onto
the Reviewer.

## Cross-platform parity gate (blocking)

For any change that renders a screen on **both** iOS (SwiftUI) and Android (Compose), the reviewer
MUST open the paired screenshots from the render evidence and **diff iOS against Android against that
screen's contract** in `docs/contracts/`. This is not optional and is **not** satisfied by "both
builds are green" — a green build with mismatched screens is a failing review.

A finding is **blocking** when iOS and Android disagree on any item in the contract's *Parity
acceptance* list, specifically:

- **Arrangement** — region order, alignment, and vertical placement (e.g. centered vs bottom-pinned).
- **Emphasis** — which control is primary vs a quiet text link (must match the contract on both).
- **Notice / card placement** — same region on both (e.g. error card below the buttons, not above).
- **Icons** — same registry glyph **and FILL** on both (`docs/contracts/icon-registry.md`). An
  iOS-filled / Android-outline mismatch, or a wrong glyph (e.g. `Security` where the registry says
  `shield_lock`), is blocking.
- **Copy** — same strings, same type roles.

State parity findings as *"iOS shows X, Android shows Y; the contract says Z"*, referencing the two
crops, so the fix is unambiguous.

## No contract, no pass

A screen-affecting change with **no contract in `docs/contracts/`** to diff against is *itself* a
**blocking** finding. Without a contract the comparison degrades to eyeballing — the exact drift the
gate exists to stop — so the change must land (or reference) its `docs/contracts/` entry, including
the icon-registry rows it relies on, before it can pass.

## The founder does not hand-diff screens

If the reviewer cannot see the renders for a screen-affecting change (no paired evidence), that is
itself a **blocking** finding — the change cannot be judged for parity, so it does not pass. The gate
does the diffing; the founder only spot-checks.

## Evidence-specific findings

Read the approved screen contract and icon registry supplied in the repository briefing.
For each required state, compare the labeled SwiftUI and Compose images from the same
current-head delivery. A missing platform or state is a coverage blocker, never a parity pass.

A visual finding must say which state shows what on iOS, what on Android, and which contract
rule is violated. A build/runtime finding must identify a concrete failing path or diagnostic;
“could fail” and “mismatch risk” alone are not observed defects. Successful rendering does not
prove behavior, but do not claim that a rendered path cannot compile without contrary evidence.
Record genuine uncertainty as a verification gap and name the missing proof. Never invent a
mismatch, accept a stale image, or ask the founder to perform the paired comparison.

## Figma delivery gate (blocking for screen delivery)

A screen-delivery PR also delivers the editable Figma screen, required states, and flow per
`SHAPE-OF-A-TASK.md` and `docs/figma-delivery.md`. For consent, the founder's 2026-09-28
scope clarification explicitly supersedes its former Figma exclusion.

Require direct node links, the component ledger, a recorded Figma revision (or canonical
structure digest when a revision is unavailable), and exported evidence associated with
the current implementation. Inspect the exports alongside both platforms and verify
component reuse, variable bindings, state coverage, and the flow's interactions. Bare
links, Code Connect files, screenshots pasted onto the canvas, and a prose claim that
Figma was updated are not proof. Missing or stale Figma evidence blocks approval.

Name the exact missing proof or observed mismatch. If the authoring/export connection is
unavailable, report a capability blocker for Steward; do not prescribe speculative UI
changes or repeatedly spend Builder revisions on an inaccessible tool. Factory-only
or documentation-only changes do not require screen exports unless they change a screen.
