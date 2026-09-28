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

## The founder does not hand-diff screens

If the reviewer cannot see the renders for a screen-affecting change (no paired evidence), that is
itself a **blocking** finding — the change cannot be judged for parity, so it does not pass. The gate
does the diffing; the founder only spot-checks.
