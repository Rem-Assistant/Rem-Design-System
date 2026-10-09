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

For each reviewed surface, record master/instance IDs, the actual descendant/slot mapping and
natural-size rendered evidence. Re-read mutable masters before an authorized change; the current
Chat core/extended/message targets are linked in the registry and may be changing concurrently.
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
| Canonical anatomy | Master ancestry plus descendant/slot map. List leading icon, Content title/permanent subtitle grouping, trailing slot, and separate transient status helper inspected. |
| Credential and service composition | Before-saved Add login CTA; chevron on the sheet-opening Button; saved outcome with corresponding navigation affordance and no extra standalone ViewDetails; canonical leading asset slot and modest logo weight. |
| Permission request | Inline disclosure/header above slotted request body; in-place expand/collapse; denied collapses to configurable Denied receipt with inspectable history; consequential information visible. Three horizontal canonical ButtonGroup actions pass narrow-width/long-label fit checks without changing existing variants. Native OS handoff is separate; no redundant Rem review sheet or assumed Always Allow policy. |
| Control semantics | Each Button classified as action, disclosure, busy, disabled availability or noninteractive receipt; destination or action owner recorded. |
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
| Permission request / denied | In-place expand/collapse and history inspection; narrow mobile width plus long labels for all three horizontal ButtonGroup actions; inspect existing variants for regressions. |
| Independent statuses | Representative card outcome while agent runs, voice-session state, and composer sending; each displays only its own truth. |

## What is automated

`python3 -m unittest discover -s tools/render-evidence -p 'test_chat_guidance.py'` checks that
local Markdown routes from the changed skills/references resolve to files and headings, including
this checklist and its registry contracts. The existing test runner discovers it. It does **not**
inspect Figma geometry, validate role menus, prove semantic compliance, or implement runtime state.
The checklist is an evidence-backed review gate, not an automated UI enforcement claim. Existing
Figma structure/render checks cover only their declared contracts; do not relabel unrelated
fixtures as proof that these Chat requirements passed.
