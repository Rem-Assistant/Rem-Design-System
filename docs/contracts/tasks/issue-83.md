Implement the approved Agenda Suggestions playground on native SwiftUI and Compose, with deterministic local fixtures. This is a bounded UI slice of **Agenda New**, not production Rem integration.

## Source and reuse

Figma file `af4yDqCzp57jds9lkFiIaO`, page `1910:40765` (Agenda, Task & Events New), Suggestions section `2336:19584`. The trusted base contains the versioned source archive, coverage inventory, builder boundaries, and exact structural contract documented in `docs/contracts/playground-expansion.md`. Read and extract that packet before implementing; its source screenshots and authored notes supersede older Agenda references.

Use initial `2336:19585`, Add `2337:19714`, Move `2337:19928`, Dismiss `2337:20122`, none `2337:20436`, empty-one `2337:20667`, empty-none `2337:20884`, empty-accepted `2337:20986`, overflow `2338:20884`, accepted-open `2338:20977`, empty-overflow `2338:21059`, and empty-to-populated sheet `2338:21133`. Read authored rules `2338:21758` and `2338:21759`.

Reuse the existing AgendaScreen, DateNavigationHeader, TaskEventRow, RemSection/SectionHeader, SortTrigger and AddNew/Schedule components where applicable. Implement the current AgendaSuggestionRow (`2336:19583`) contract rather than blindly reusing old SuggestionRow visuals. Existing `Sources/RemDesignSystem/Screens/AgendaScreen.swift` and `compose/RemDesignSystem/screens/AgendaScreen.kt` are older generic hosts: preserve compatible public callers while introducing the current composition. Do not replace unrelated surfaces.

Extract `docs/playground/expansion-design/source-evidence.tar.gz` to a temporary directory and verify its SHA256 `494a323503cf281b37df737c9e987c9b97b48fcb3a79511aba33c8de1b7da2ae` and enclosed file hashes. Read the adjacent coverage and boundary documents; trusted structural identity checks live in `tools/design-sync/playground-source-contracts.json`.

## Behavior to implement and prove

- Present authored loaded and empty-day fixtures. The Suggestions slot follows AddNew/Schedule with exactly 24pt spacing while present. Show at most three inline suggestions and all live suggestions in overflow, including those shown inline.
- Initial suggestion titles: “Prep for tonight’s rehearsal”, “Move rehearsal check-in to 3:00 PM”, “Review venue notes”; fourth overflow item: “Bring the updated set list”. Preserve source metadata and native component appearance.
- Add creates the authored 5PM task; Move changes the intended existing rehearsal to 3PM; Dismiss removes its suggestion. Those times are fixture examples, not global defaults. Preserve stable IDs, other rows and unrelated content.
- Remove accepted/dismissed suggestions optimistically. Last removal hides the entire Suggestions slot including its 24pt gap and closes overflow. Overflow remains open while other suggestions remain, including an empty Agenda becoming populated after acceptance.
- Use native sheet presentation with the authored Suggestions/Done header, divider and scrollable content. Do not invent fixed detents, drag indicators or extra navigation hierarchy. A deterministic optimistic-restoration fixture may follow the authored notes; no network fetch or invented spinner, success badge, error card or Retry affordance.
- Shell actions outside this slice use explicitly observable host callbacks or clearly separate playground boundaries. Do not invent Inspector, calendar or backend flows.

## Delivery contract

Work only on the seeded `agent-factory/playground-issue-<issue>` draft PR targeting `codex/playground-expansion`. Delivery scope is `agenda-suggestions`. Keep Settings behavior and Settings PRs unchanged. Wire a discoverable route in both existing native playground hosts. Reuse host infrastructure; register new iOS UI-test classes in the Xcode project.

Produce paired platform captures for every state required by the trusted `tools/render-evidence/contracts.json` entry, including `AgendaSuggestions-inline-light`, `AgendaSuggestions-overflow-light`, `AgendaSuggestions-none-light`, dark/large-text variants and behavioral journey outcomes. Every capture name must be unique per run. Test Add/Move/Dismiss, stable IDs, three inline versus four in the initial sheet, overflow persistence, last-item removal/gap removal, empty-to-populated transition, Done/back and large-text reachability. Render screenshots alone do not prove these interactions.

Update bounded source coverage and fidelity notes, identifying exactly which supplied states were implemented and visually compared. Never claim whole Agenda or live services are complete. Do not bypass missing evidence or weaken review caps, workflow/artifact authentication, required checks or image limits. Stop at a reviewable draft; no merge, promotion or Figma writes.

Excluded: Inspector date/minimum-duration/all-day semantics, Check-in, Automations, Chat redesign, global Plain/filled icon rules, real scheduling, backend/authentication, scheduled conversations and CallKit. Existing issue #13 covers a different activation/coach-mark scope and is not completed by this task.
