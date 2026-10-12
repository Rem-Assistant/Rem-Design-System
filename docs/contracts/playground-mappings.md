# Playground mapping repair contract

The 2026-10-09 repair uses Samuel's verified Figma identifiers in file
`af4yDqCzp57jds9lkFiIaO`. These are local source bindings, not a claim of published Code
Connect, live template evaluation, or native screenshot fidelity. Hosted Code Connect
inspection/publication was plan-restricted; no Figma writes or entitlement changes are part of
this repair. Existing native APIs and existing Section templates remain unchanged.

Ordinary read-only Figma screenshots were inspected on 2026-10-09: [DailyBriefCard set](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2190-12237)
shows Read latest brief, Stop reading, Read again, and Try reading again; the latter two confirm
the unsupported API boundary. [Suggestions instance](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2049-10080)
shows the Suggestions header and dashed canonical row. That screenshot does not prove native
overflow behavior or a standalone master. Native source and focused tests cover the local
composition relationship; native visual acceptance is a separate release review.

## DailyBriefCard

Canonical component set: `2190:12237`. Parserless SwiftUI and Compose twins map `Headline`
to `title`, `Summary` to `summary`, and enumerate all `Playback` values:

| Figma value | Shipped API / output |
| --- | --- |
| Ready | `isReading = false`, host supplies `onTap` and `onRead` |
| Reading | `isReading = true`, host supplies `onTap` and `onRead` |
| Finished | Explicit unsupported comment; no substitute invocation |
| Retry | Explicit unsupported comment; no substitute invocation |

The optional counts-only fallback, hidden read action when `onRead` is absent, callbacks, and
playback lifecycle are native/host concerns; the mapping does not fabricate Figma properties
for them. Removing the `DailyBriefCard:figma` baseline means a truthful local binding exists,
not that all four playback states are implemented. Tests execute all four branches and check
unsupported states never emit a Ready call.

### Separate state API proposal — not implemented

A future owner may propose a shared `Playback` enum (`ready`, `reading`, `finished`, `retry`)
and explicit action labels/callback semantics on both native twins. Completion receipts and
failure transitions must come from the real playback host. Keep `isReading` compatibility only
with a documented migration. Approve that API separately, implement native behavior, then add
paired screenshots and interaction tests before changing unsupported template branches. Do not
claim this proposal is shipped or weaken current evidence gates to accommodate it.

## SuggestionSection

This is a behavioral composition. The authority is the "Suggestions region" FRAME `2336:19714`:
SectionHeader `161:68` ("Suggestions"), then **standalone** AgendaSuggestionRow set `2336:19583`
(add/move) instances 4pt apart with no Section or rows surface around them, then the See more
Button `377:8` at Style Text · Accent (`377:4`; the instance layer is named "See more · Plain").
The older Suggestions instance `2049:10080` (a generic Section wrapper) and the region frame are
reference locations, never mapping targets.
No standalone SuggestionSection `.figma.ts` is created, so generic Section's existing bindings
are neither duplicated nor overwritten.

`code-connect/SuggestionSection.composition.json` is the explicit relationship contract.
SwiftUI's VStack directly renders AgendaSuggestionRow; Compose's Column renders
SuggestedTaskRow, which forwards `accept/title/subtitle` to the canonical row's
`action/title/metadata`. Neither native wrapper calls RemSection; the lint rejects one. Both own a sentence-case Suggestions header, at most three
inline rows by default, hidden empty content, and an optional See more action for overflow.
The host orders data and handles accept/dismiss/overflow; no ordering or backend API is invented.

The component lint accepts this exact source only after validating the contract's source/file/
node identities, both platforms' existing constituent templates and source files, Header,
Text · Accent and add/move/text properties, and the native adapter/behavior relationships. Mutation tests
reject missing templates, wrong targets, fake standalone masters, wrong platforms, and broken
adapter forwarding. The `SuggestionSection:figma` baseline is removed. This deliberately narrow
composition rule is not a general exemption for unbound components.

Remaining design boundary: the Figma composition does not publish the native wrapper's data,
inline-limit or overflow API as standalone component properties. A standalone Code Connect chip
would require a separately approved systemization proposal for the existing composition; do not
create a replacement master merely to make lint green. The local constituent contract closes
the mapping traceability gap while keeping that distinction visible.

## Local validation

Run `node --test tools/test-component-code-connect.mjs` (includes the playground mapping fixtures),
`node tools/lint-components.mjs`, and `npm run check-code-connect` with locked dependencies.
These checks exercise mapping identity, exhaustive template output and the composition contract;
they are not native rendering or release receipts. Paired native screenshot review remains the
release owner's gate. No Factory scope-admission code or validation is involved.

Validation at the mapping repair: 18/18 focused checks passed; component lint reports 18
pre-existing gaps and no new violations; TypeScript plus both platform parser passes completed
with Code Connect 1.4.9 from the lockfile. Neither component remains in the lint baseline.
