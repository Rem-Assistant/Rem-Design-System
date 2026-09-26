# Skill-evolution notes — improvement candidates from live sessions

Captured while *using* the skill on a real product (Composa). Fold into SKILL.md on the next
revision. Each entry: the observation, why it matters, the proposed change.

## 2026-07-23 (Composa archetype-test session)

### 1. The spec gate has TWO modes: state-spec vs feature-spec — and design-system UI usually needs the latter
**Observation (owner):** for UI work on an established component system, enumerating entry / empty /
loading / error / exit states is often redundant — **the components already own those states.** What's
actually missing is the *feature* spec: **what happens when I click X, Y, Z** (which dialog opens, with
which fields and buttons; what each menu item does).
**Why it matters:** running a pure state-enumeration gate on such an issue produces busywork and misses
the real gap. The owner named this directly: "I don't think this needs a spec gate, it needs a *feature*
gate."
**Proposed change:** in Bar-1 / the spec gate, branch by issue type. For a NET-NEW flow → enumerate
states (they don't exist yet). For UI built on an existing design system → assume the components supply
empty/loading/error/state and gate on the **interaction/behavior spec** instead (per-action outcomes,
dialogs, destinations). Say which mode you're in.

### 2. A good UI issue names the specific component AND every place it appears
**Observation (owner):** "#214 doesn't mention where or what components to look at — there's a segmented
control on the Assets tab and one in config. That's important detail." Good issues aren't just the
symptom; they name the component (e.g. `SegmentedControl`) and its instances/locations.
**Why it matters:** location makes an issue drivable and stops the builder guessing; fixing at the
component covers all instances, but the issue must list them so verification checks each.
**Proposed change:** add to the "execution basics" of Bar-1: *name the component and every surface it
appears on.* A UI issue without a component/location reference is not yet ready.

### 3. UI readiness needs the resolved VISUAL (menu contents), not just "there's a menu"
**Observation (owner):** an issue that says "opens a menu" reads as ready but isn't — "the gap is really
what *is* the menu; I'll only see it when we can actually see it." The human can't judge readiness of an
unspecified menu until its items/behaviors are pinned (usually from Figma).
**Why it matters:** "prompts that there's a menu" ≠ drivable. The menu's actual items + per-item behavior
are the spec.
**Proposed change:** for UI issues, "ready" includes the resolved visual reference (Figma node / screenshot)
AND the enumerated menu/dialog contents. Absence of the visual is itself a readiness blocker to surface.

### 4. "Open Figma, compare to the running product, write the feature spec" is AI work, not owner work
**Observation (owner):** "I'm doing this by opening Figma and comparing — but that's a role I can hand off
to the AI." The owner is manually reading specs off Figma nodes.
**Why it matters:** this is mechanical (pull node via the Figma MCP → diff against the product → enumerate
the gap) — exactly what the AI should own so the director only rules on taste/priority.
**Proposed change:** make "pull the Figma node, compare to the running product, and draft the feature spec"
an explicit AI responsibility in the loop, feeding the owner a drafted spec to approve rather than the
owner transcribing Figma by voice.

### 5. Capture skill-improvement moments *during* real use
**Observation (owner):** "what we're doing this session is also evaluating the skill — a moment like this,
we should capture what needs to go back to improve the skill."
**Proposed change:** treat live friction as first-class feedback; keep this file and append as it happens
(this entry is the practice).

### 7. Drive-verify the USER's success condition, not the builder's — and INTERROGATE the evidence
**Observation (owner):** two features (#167 Back-to-Files, #214 segmented stroke) were called "done"
when they weren't. Root cause: the drive asserted a **technical proxy** for done, not the user's felt
outcome, and the captured evidence was never examined against intent.
- #167: asserted `URL becomes /projects` (the wired mechanism). The user's condition was "I get back to
  my *usable* files." Locally that dead-ends on the auth wall — and the post-navigation **screenshot
  showing "Sign-in unavailable" was captured but not looked at**; success was reported off the URL
  assertion. (Also shipped with a leading icon the owner didn't want.)
- #214: asserted "the ring class is in the DOM" (component review). The user's condition was "the control
  looks *enclosed*." The flush selected thumb hides the stroke — the reviewer flagged it, and it was
  **demoted to a 'taste call for later'** instead of recognized as defeating the feature's visible purpose.
**Why it matters:** a green trusted-event assertion is still "testing what I built." The two gates exist
to catch exactly this, and they were bypassed by asserting a proxy.
**Proposed change (make "done means the feature" enforceable):**
1. Before building, write the done-condition **as the user's sentence** ("I can leave the editor and land
   on my usable files"; "the segmented control reads as enclosed like the buttons"). The final drive
   asserts THAT sentence, not a mechanism. If the user's condition can't be met (auth wall), that's a
   FINDING, not a pass.
2. **Look at every screenshot against the intent, and say what a user would see** — capturing evidence is
   not examining it. A screenshot that contradicts the claim of done must block the merge.
3. A **reviewer-flagged felt-outcome defect is a completeness bug**, not a deferrable taste nit, whenever
   it defeats the feature's purpose. Don't file the core outcome as "taste for later."
4. Prefer verifying against a **realistic environment** (a real logged-in account) over a stubbed/bypassed
   one — the stub is where "works in the mechanism, fails for the user" hides.

### 8. Don't re-ask a delegated decision — proceeding IS the archetype; asking twice makes the human the bottleneck
**Observation (owner):** "Be more autonomous my guy, I'm not the bottleneck." I had surfaced the same
production-migration decision (apply / M1-only / show-SQL) THREE times across turns instead of acting on
my own stated recommendation. The owner had effectively delegated it ("proceed with your recommendation").
**Why it matters:** the whole two-gate model exists so the human is a *feedback + sampling* gate, NOT a
per-decision approver. Re-asking a decision the owner has delegated re-creates the exact bottleneck the
archetype removes — and it reads as the AI offloading responsibility upward.
**Proposed change:** once the owner signals "use your judgment / proceed on your recommendation," STOP
asking and act; surface only (a) genuinely irreversible actions the first time, (b) real taste/direction
calls, or (c) a true blocker. A recommendation you're confident in is a decision to execute, not a
question to keep posing. Confirm-once for a hard-to-reverse action (e.g. a prod migration), then proceed
through the follow-ons without re-confirming each step.

### 6. Write captured feedback in the owner's first-person voice, not third-person "Director said"
**Observation (owner):** "the language seems strange, write it as me."
**Proposed change:** issue comments that capture owner feedback should read as the owner's own words
(first person), with a light provenance footer — not a third-person "Director (date): ..." framing.
