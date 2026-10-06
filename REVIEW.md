# REVIEW.md — reviewer rubric

Read by the Agent Factory reviewer via `project.context_files`. This turns the paired render from
*evidence* into a *gate* — the rule that moves cross-platform drift-catching off the founder and onto
the Reviewer.

## Review contract and stopping rule

Review the exact lane evidence packet supplied for the current revision. Do not reconstruct the
project from chat history, inspect unrelated surfaces, or expand the assignment into a new product
design exercise. One full pass may report observed blockers across the submitted scope. A repair
pass reviews only the changed nodes and directly affected transitions. After that pass, return any
unresolved product or taste decision to the Director and any evidence or capability blocker to the
Steward. Do not continue a standing feedback loop.

Keep these verdicts separate:

- **Topology complete:** required states are present and the connector graph is structurally valid.
- **Screen design complete:** every counted state is an editable canonical screen or an explicitly
  proposed editable screen that passes the visual contract.
- **Research complete:** competitive evidence is source-authentic and remains on the single
  research-only FigJam page; it is not a Rem screen.

A lane may pass one verdict and fail another. Never collapse them into a general `done` status.
Screenshots used as FigJam references do not prove editable Figma completion; verify the referenced
Figma master. Reject hand-built imitations, flattened screenshots, SVG reconstructions, blank
frames, and component-only fragments when they are presented as finished screens.

A full-screen state composed in FigJam from valid remote components is useful topology evidence,
but still fails the screen-design verdict until an editable Figma Design master owns that
composition. Report it as `FigJam-composed`, not as a fake screen and not as complete.

## Design-only visual review gate

Do not approve a design lane from a zoomed-out topology overview. Inspect a readable native-size
render of every changed screen and at least one screen from each unchanged state family affected by
the change. The overview proves hierarchy and routing; the closeups prove visual fidelity.

For a surface with many states, identify the canonical base screen and its invariants—navigation,
safe area, background, composer or toolbar, typography, and recurring content regions. Compare
siblings against that base. Only the cue named by the state may vary unless product evidence
requires another change. An unexplained missing model control, altered composer, absent user bubble,
different keyboard generation, missing home indicator, or shifted safe-area treatment is blocking.

Inspect Figma structure as well as pixels. Text must use canonical styles; reusable controls must be
component instances; platform menus, keyboards, pickers, and progress indicators must use approved
current-platform assets; semantic icon colors must remain intentional. A visually similar ad hoc
substitute does not pass. Verify that screen/component descriptions state the purpose,
distinguishing state, and canonical source, and that any temporary TODO is bounded and removed after
repair.

Topology labels name screens and states. Connector labels describe only the action or transition.
Reject connector text that duplicates or overlaps a screen label.

Compare the submitted state inventory with the preservation inventory from before the pass. A
previously visible canonical screen that became a gap card, lost its image/component provenance, or
disappeared without an explicit approved removal is a blocking regression. The same rule applies to
deleted state-family siblings and connectors. When preservation evidence is unavailable, fail the
affected state as unverified instead of accepting the replacement.

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
