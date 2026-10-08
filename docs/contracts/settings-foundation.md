# Settings foundation — approved implementation contract

Approved by Samuel on 2026-10-08 for an isolated native playground; no production service integration, main merge, deployment, or Figma writes. Extends trial #70 from preserved baseline b08400d1b2db10ec3d1f19c838ceb049f4636cb3.

## Authority and amendments

Figma file af4yDqCzp57jds9lkFiIaO, Settings New entry 1964:86819, Agent Settings 1827:50855, destination section 1833:5014. Source extraction is retained under docs/playground/design-context. Figma was read on 2026-10-08. Native navigation chrome, font scaling, keyboard, menus and system sheets follow each platform.

Samuel explicitly approved two amendments to the current visual source: iOS uses native SwiftUI List/Section rather than ScrollView/RemSection cards; all Settings ContainedIcon consumers use Subtle fill rather than their current explicit tinted overrides. Samuel subsequently refined Subtle to use semantic labelPrimary glyphs on the verified backgroundSecondary gray container; preserve explicit brand/status/accent exceptions. Preserve icon identities, settings size 29/radius7/glyph17, text, order, grouping and semantic tokens. Native grouped corners/section chrome may follow OS behavior; do not reconstruct custom cards to imitate native List.

## Foundation owner

One owner implements this foundation before destination work. Reuse ListRow, ContainedIcon and Compose RemSection. Preserve existing consumers/defaults. SwiftUI row content must support native containers without duplicated horizontal/vertical insets, separators or chevrons; NavigationLink owns navigation disclosure, Button owns direct actions, controls own their semantics. SwiftUI native Section supplies headers/footers; the existing custom RemSection remains available for genuinely custom ScrollView surfaces. Compose retains its canonical section and row slots with matching intent.

Expose an explicit typed Agent Settings route contract for pairedDevices, connectors, cloudBrowser, memory, models, wallet, voice. Automations remains visible as an unavailable design reference with no invented destination and an accessible explanation. The foundation may expose callbacks/route identifiers while destination implementation follows; it must not pretend unimplemented destinations work. Centralized host navigation belongs to the integration owner.

## Behavior to preserve

Gallery -> Settings -> Rem -> Agent settings. Native Back, fixture loading, error/Retry/Cancel, cancellation before a slow fixture completes, fresh load on reopening. Shared controls retain Toggle and display-name draft Save/Cancel semantics. Root non-Agent destinations stay references except Share Rem, whose system-handoff implementation is an integration task. No real sign-out/deletion/pairing/auth/payment/key/service operations.

## Design/code mapping

Author parserless .figma.ts mappings for current ContainedIcon, ListRow/ListRowLabel and Section on SwiftUI/Compose, using verified live property names, complete enum values and dynamic nested component/Rows slots. Map native Section and custom adapter as clearly distinguished contexts. Do not update archived .figma.swift/.figma.kt examples as the new delivery path. Live Code Connect metadata/publication remains plan-blocked; ordinary authorized canvas property reads may establish the local contract. Do not claim local templates are published. Preserve existing consent mappings.

## Verification

Tests and rendered evidence must target SettingsEntryContent and AgentSettingsContent in the actual playground hosts, not the older SettingsScreen reference. Check entry and Agent default states in light/dark, large text scrolling, single native disclosure per navigable row, no separators between sections, Subtle icons, and the preserved navigation/error/cancel flows. Run relevant Kotlin/Swift build and interaction tests plus parserless type/parse checks. Record exact candidate SHA and evidence gaps. Hosted CI runs from the isolated branch; a failed or unrun gate is not a pass.

## Exclusions

Automations behavior, Chats, Agenda, Trove, production app/backend integration, provider OAuth/payment/credential entry, Figma mutation, main merge and deployment. Destination builders begin only after this API contract is implemented and reviewed.
