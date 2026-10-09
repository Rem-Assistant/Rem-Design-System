# Chat review gates

Use for Chat component, state, screen, or prototype design/review. Read the current
[anatomy and interaction contracts](../../../../REGISTRY.md#chat-interaction-and-anatomy-contracts)
and [status owners](../../../../REGISTRY.md#chat-component-jobs-and-principles) first.
The [connected-flow contract](../../../../FILE-ORG.md#connected-flow-presentation) governs canvas
organization. These gates judge design evidence; they do not authorize implementation or edits
to a Figma file owned by another active worker.

## Evidence and decision authority

Inspect the actual supplied reference pixels before deriving anatomy, geometry, menu items or
states. Record the source node/file, capture date, natural dimensions and what is visible.
Separate **observed reference**, **user-approved Rem direction**, **inferred/proposed state**, and
**verified implementation**. A still image does not prove a transition, backend result or all
states of a component. Missing reference pixels remain an evidence gap; do not fill them from
memory. A later explicit user correction supersedes an earlier agent interpretation.

For each reviewed surface, record master/instance IDs, the actual descendant/slot mapping, instance overrides and
natural-size rendered evidence. Re-read mutable masters before an authorized change; the current
Chat core/extended/message targets and completed review evidence are linked in the registry.
Do not declare a name-only instance lookup an anatomy check.

## Required checklist

Copy the following gates into the design review packet with **pass / fail / not verified /
not applicable**, evidence links and a reason for each non-pass. An unchecked or unsupported gate
is not a pass. Resolve a failed in-scope gate before marking that surface design-accepted;
unresolved product decisions remain proposed, with the affected scope withheld from acceptance.
A documentation-only change can complete while the UI gates remain not verified.

| Gate | Evidence required for acceptance |
|---|---|
| Reference authority | Actual pixels inspected at natural size; observed versus approved versus proposed states identified; no invented long-press role actions. |
| Canonical anatomy | Actual master ancestry, descendant/slot map and instance overrides. List leading icon, Content title/permanent subtitle grouping, trailing slot, and separate transient status helper inspected. |
| Credential and service composition | Before-saved Add login CTA; chevron on the sheet-opening Button; saved outcome with corresponding navigation affordance and no extra standalone ViewDetails; canonical leading asset slot and modest logo weight. |
| Permission request | Inline disclosure/header above one shared request-body slot; in-place expand/collapse; denied collapses to configurable Denied receipt with inspectable history; consequential information visible. Reuse the existing ButtonGroup Actions slot for three horizontal children; preserve existing variants. Test compact and long labels at 320/330/370pt without shrinking type; failed label sets cannot ship. Native OS handoff is separate; no redundant Rem review sheet or assumed Always Allow policy. |
| Control semantics | Identity rows, navigation Buttons and outcome receipts distinguished; each Button classified as action, disclosure, busy, disabled availability or noninteractive receipt; destination or action owner recorded. |
| Status ownership | Agent/run, action card, voice session, message delivery and composer input/send each has an explicit owner; no completion inferred across owners. |
| Message receipts and recovery | Latest Delivered/Read retains delivery time; next-message normal receipt removal; failure text below with no timestamp; retry restricted to definitively failed user messages; unknown outcome reconciled; no external email resend. |
| Message geometry | Reaction overlaps bubble edge; red outline circle-exclamation is entirely outside/right of bubble; Not delivered text stays below. Under-bubble subtext right inset/left shift clears reaction without clipping or collisions. Use existing outline icon asset where available. |
| Long press | Grok bottom-sheet reference inspected; 2×6 reaction grid and grouped contextual actions; user/assistant role-specific items verified; no floating reaction strip. |
| Composer continuity | Existing Send-arrow and circular progress retained through representative input/send states. |
| Contextual composition | Representative full screens consume the shared Chat shell and slots, alongside the connected component/state canvas; keyboard, safe areas, wrapping and neighbors checked. |
| Prototype destinations | For in-scope actions: actual destination, local update or external handoff and return/cancel route verified; unresolved destinations labeled, not invented or silently omitted. |
| Delivery claims | Separate design/prototype/runtime status. Reactions/read/runtime are not implemented by this guidance update; broader activity model, permission policy and exact Read backend remain proposed. |

## Minimum contextual matrix

Render at natural component size and declared logical screen viewport. Record which cells were
actually inspected. Use short and long text in **each** applicable row; inspect the combination,
not just isolated state thumbnails. A design-only combination is not a claim it is reachable in
runtime. Mark unsupported combinations explicitly and explain why.

| Scenario | Required context and checks |
|---|---|
| Latest Delivered and latest Read | With and without reaction; retained actual delivery time; enough subtext clearance. |
| Next message | Previous normal receipt disappears; reaction remains correctly anchored; no inherited stale timestamp. |
| Definitively failed user message | With and without reaction; outline failure icon outside/right; Not delivered below without time; retry action ownership. |
| Unknown user-message delivery | Reconciliation boundary explicit; no unconditional retry. |
| User and assistant long press | Separate role menus; bottom-sheet reaction grid and grouped actions; no unsupported retry or email resend. |
| Credential before saved / saved | Contextual service title and asset; Button disclosure opens actual Add Login/detail destination; outcome remains clear. |
| Permission request / denied | In-place expand/collapse and history inspection; compact and long labels at 320/330/370pt without shrinking type for all three horizontal ButtonGroup actions; inspect existing variants for regressions. |
| Independent statuses | Representative card outcome while agent runs, voice-session state, and composer sending; each displays only its own truth. |

## Completed review evidence and limits

Use the [full Chat review](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO?node-id=2626-20028),
[Grok-style long-press sheet](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO?node-id=2603-19498)
and [full Chat prototype entry](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO?node-id=2630-20783)
for the current review. Compact permission labels fit at 320/330/370pt without shrinking type;
longer labels fail 320pt and are explicitly non-shipping. Preserve that failing specimen as a
limit, not a passing option. The existing ButtonGroup slot supports three children; reuse does
not require a new primitive. Short/wrapped message + reaction + failure specimens are geometry
stress tests, not proof that the combination is reachable in runtime. Keep role-specific menu
and prototype destination checks tied to the actual nodes/transitions inspected. Always allow
scope and runtime behavior remain unresolved. See the
[review record](../../../../docs/design-reconciliation/2026-10-09-chat-guidance-review.md)
for evidence provenance; this guidance test suite cannot certify any of those visual results.

## What is automated

`python3 -m unittest discover -s tools/render-evidence -p 'test_chat_guidance.py'` checks that
local Markdown routes from the changed skills/references resolve to files and headings, including
this checklist and its registry contracts. The existing test runner discovers it. It does **not**
inspect Figma geometry, validate role menus, prove semantic compliance, or implement runtime state.
The checklist is an evidence-backed review gate, not an automated UI enforcement claim. Existing
Figma structure/render checks cover only their declared contracts; do not relabel unrelated
fixtures as proof that these Chat requirements passed.
