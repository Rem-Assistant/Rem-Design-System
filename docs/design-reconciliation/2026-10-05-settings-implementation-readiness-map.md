# Settings implementation-readiness map — 2026-10-05

This is a design-to-implementation handoff scaffold. It does **not** authorize code work. Samuel has
deferred SwiftUI, Compose, Code Connect, and runtime changes until the Settings product decisions and
canonical Figma screens are approved.

The Figma file is `af4yDqCzp57jds9lkFiIaO`; every verified node below is on page `356:5` (`Settings`).
The corresponding FigJam topology is file `QGFwluZMaWyJZUai2OxlXw`, page `0:1`, Agent Settings
Section `9:49`, plus broader Settings Section `194:6979`.

## Readiness gates

A Settings branch may enter implementation only when all of these are true:

1. Samuel has approved its product behavior, copy, visual hierarchy, and topology placement.
2. Every required state has an editable canonical Figma Design master. A FigJam reference gap does
   not satisfy this gate.
3. Each master is composed from canonical tokens, components, and platform controls; its rendered
   state passes visual review.
4. Every enabled action has a documented state change, destination, overlay, external handoff, or
   intentionally disabled outcome.
5. Create, read, update, delete, cancellation, recovery, and success feedback are covered wherever
   the underlying object supports them.
6. The adopting RemClaw caller and state owner are re-verified at implementation kickoff. The source
   names below are provisional pointers from `VIEW-MAP.md`, not proof of current callers.
7. Samuel explicitly opens the code phase.

## Shared screen contract

All branches depend on the same design-system spine:

- Settings screen shell and invariant navigation title.
- `RemSection` or equivalent grouped Section composition.
- `ListRow` with swappable leading, content, and trailing regions.
- `ContainedIcon` with the branch's semantic color rather than one global blue override.
- Native switch, menu selector, disclosure indicator, and text-field states.
- `ButtonGroup` plus primary, secondary, secondary-destructive, and destructive Button treatments.
- Confirmation sheet and post-action feedback patterns.
- Loading, empty, recoverable-error, and unavailable states where the service can produce them.

The trailing region owns one direct control. When a navigable row also needs a switch, keep the
switch trailing and pair the disclosure chevron with the content cluster.

## Branch map

| Branch | Canonical Figma nodes | Design status | Provisional later RemClaw owner | Required design-system dependencies |
|---|---|---|---|---|
| Settings → Agent settings | `1610:16145`, `1610:16146` | Masters exist; icon color review pending | `SharedSettingsView`; new Agent Settings hierarchy requires caller re-verification | Screen shell, Section, ListRow, ContainedIcon, disclosure |
| Paired Devices | `1610:16148`, `1610:16150`, `1610:16152`, `1610:16154`, `1610:16156` | Masters exist; topology present | Current paired/gateway surfaces are on a runtime-migration boundary; do not bind to deprecated gateway code without a fresh decision | Empty state, device row, status, unlink confirmation, destructive Button Group |
| Connectors | `1610:16158`, `1610:16160`, `1610:16164`; proposed detail master `1768:16771`, overview instance `1768:19547`, bound edge `1768:19740` | Existing list/manage/revoke masters; editable Gmail detail proposal inserted; discovery, account management, permission policy, and per-tool override remain | `SharedComposioConnectionsView`, `ComposioConnectionSheet` are provisional pointers | Connector row, account row, capability list, permission policy selector, per-tool override, confirmation and feedback |
| Cloud Browser | `1610:16166`, `1610:16168`, `1610:16170`, `1610:16172`, `1610:16174`, `1610:16176`, `1610:16178`, `1610:16180`, `1610:16182`, `1610:16184` | Masters exist; Add-site entry/action wording and edit behavior await final review | `SharedCloudBrowserSettingsView` is the provisional owner | Section-header action or Add row, site row, credential row, inline edit, Button Group, delete/clear confirmations |
| Automations / Daily Brief | `1610:16186`, `1610:16188`, `1610:16190` | Masters exist; product review pending | `SharedAutomationsSettingsView`; Daily Brief scheduling/state owner requires re-verification | Schedule row, enable switch, run-history row, empty/loading/error states |
| Memory | `1610:16192`, `1610:16194` | Proposed masters; product review pending | `SharedMemorySettingsView` is provisional | Summary row, detail composition, edit/delete behavior if user-managed memory is supported |
| Models / provider keys | `1672:16280`, `1657:27110`, `1672:16374`, `1661:41489`, `1661:41542`, `1672:16441`, `1672:16524`, `1742:37964` | Masters exist, but final copy, hierarchy, edit controls, success state, and duplicate failure naming require resolution | `SharedModelsSettingsView`, `SharedBYOKSettingsView`, `BYOKAddKeySheet`, `BYOKEditKeySheet` are provisional | Provider menu row, API-key field row, inline pencil/edit state, masked value, provider switch, Button Group, validation/error, revoke success |
| Wallet | `1610:16198` | Master exists; scope and runtime owner unresolved | No verified shipping owner from the current design evidence; re-audit before implementation | Settings-group screen, balance/status rows, external or in-product management outcome |
| Voice | `1610:16200`, `1610:16202`, `1610:16204`, `1610:16206` | Masters exist; product review pending | `SharedVoiceSettingsView` is provisional | Voice row, preview/select control, center-action menu, playback states, native audio behavior |

## Connectors completion contract

The current canonical masters cover the Connector list, Manage Notion sheet, and revoke
confirmation. An editable `Screen/Connector detail · Gmail · Proposed` master now exists at
`1768:16771`; its `1768:19547` overview instance is connected from `1610:16158` by the real bound
connector `1768:19740`. It uses two canonical Sections and seven canonical ListRows to show accounts,
permission policy, read actions, and write actions. This proves the existing branch can accept a new
screen in place without rebuilding the flow or flattening the design.

The Settings FigJam topology also carries the proposal in place at group `166:7242`, using screen
reference `171:8715`. The deprecated Manage Notion group `171:9264` and its incoming connector are
hidden. Active routes now run from Connectors `166:7238` to detail `166:7242`, then to Accounts
`166:7244`, Permissions `166:7246`, Tool overrides `166:7248`, or Revoke access `166:7250`. The
detail state did not require rebuilding the connector map.

The proposal still needs Samuel's product and visual review. Four connector surfaces remain missing:

1. **Connector discovery** — browse or search available connectors and start connection.
2. **Connected accounts** — current identities and account-level connection controls.
3. **Permission policy** — Always ask, Allow read actions, Allow low-risk actions, and Allow all
   actions with an elevated-risk warning.
4. **Per-tool override** — Blocked, Needs approval, and Always allow for one tool.

The implementation map cannot close this branch until those four states are designed in Figma and
their action outcomes are connected in FigJam. Successful revoke/removal feedback is the fifth gap and
must return to the canonical connected-account or connector-detail state.

## Models completion contract

The Models branch follows these decisions:

- The managed option is **Auto**, with the subtitle **Rem's managed model**. Do not name an internal
  model such as MiniMax in product UI.
- Auto explanation remains generic: Rem selects a model using the request and operating factors such
  as capability and cost.
- A provider row keeps its enable switch trailing. The disclosure chevron sits with the content
  cluster so navigation and the direct switch action remain distinct.
- Add/Edit Provider Key uses one two-row Section: Provider menu, then API-key field.
- Read state uses a trailing pencil. Editing replaces the value in the same row with an active text
  field while keeping the navigation title unchanged.
- Save/Done and Remove/Revoke use a full-width vertical Button Group. The safe forward action is
  primary; the destructive action is secondary-destructive until its confirmation state.
- Cover add, saved/masked, edit, draft-confirmed, validation/storage failure, remove confirmation,
  removal failure, removal success, cancellation, and return-to-models outcomes.

Two nodes currently share the visible name `Models · Validation / storage unavailable`
(`1672:16524` and `1742:37964`). Before approval, determine whether they are distinct failure states
and rename them by outcome, or remove the duplicate from the canonical flow.

## Cloud Browser completion contract

- The main Cloud Browser page must expose Add Site directly. Samuel accepts either a trailing
  section-header Add action or an `Add site` list row; the chosen treatment still needs final visual
  approval.
- The Add Site screen's completion action says **Save** or **Done**, because the preceding action
  already communicated Add.
- Use an inline completion button for a short page. Reserve a sticky action for content long enough
  to scroll materially.
- Saved login editing uses the standard trailing-pencil → in-place text-field transition, including
  cancellation, confirmation, deletion, and feedback.

## Future implementation slices

When Samuel opens the code phase, issue work by approved branch rather than implementing all
Settings at once:

1. Shared Settings shell and row/accessory contracts.
2. Models and provider-key management.
3. Connectors discovery, detail, accounts, and permission policy.
4. Cloud Browser site and credential management.
5. Paired Devices after the runtime-migration ownership decision.
6. Automations, Memory, Wallet, and Voice as independent slices.

Each slice must re-verify its current RemClaw callers, produce platform renders, and carry its exact
approved Figma-node set. This ordering is a handoff proposal, not implementation authorization.
