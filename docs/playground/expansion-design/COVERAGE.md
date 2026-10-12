# Agenda New / Onboarding New: bounded source coverage

Read-only live Figma audit, 2026-10-08. File `af4yDqCzp57jds9lkFiIaO`. No Figma writes, builds, browser actions or repository edits. Raw contexts and structural evidence sit beside this file. Screenshots returned with the cited design contexts were visually inspected; the raw/ directory preserves exact tool JSON, textual responses and unmodified PNG screenshots.

## Recommended first delivery

**Onboarding Voice as a standalone playground journey** is the smallest reuse-first slice: entry → preview / chooser → select voice → back → change three sliders → Continue or Skip callback. Reuse the existing controlled `VoiceControlsContent`, `VoiceChooserContent` and fixture state from Settings, with `showConversationEntry=false`; add the authored onboarding shell, lockup, native Back and fixed safe-area Continue/Skip group. Keep preview local/simulated as in the Settings playground; no supplied audio asset or real synthesis is established by this source. Continue/Skip can terminate at the playground harness callback; their next product destination is not defined by these masters. Do not invent an end-to-end onboarding order through excluded Check-in.

**First Agenda slice:** loaded/empty Agenda + Suggestions Add/Move/Dismiss/overflow local fixtures. Implement the complete authored Suggestions journey without Inspector date/duration semantics. Other shell actions can be separately scoped; don't claim their destinations are implemented by this slice.

## Canonical inventory

| Area | Canonical IDs | Source states / boundary |
|---|---|---|
| Agenda New page | `1910:40765` | “Agenda, Task & Events New” |
| Agenda reusable section | `2189:11918` | “Rem · Agenda reusable components · Proposed delivery” |
| Screen/Agenda | `2192:12182` | Loading `2049:9976`; Empty `2049:10086`; Loaded `2049:10087`; Loaded with brief `2049:10109`; Empty with brief `2049:10133`; Other date `2049:10153`; Create menu `2049:10336`; Sort menu `2191:12568`. Suggestions and Brief are exposed slots. |
| Suggestions outcomes | `2336:19584` | Initial `2336:19585`; Add `2337:19714`; Move `2337:19928`; Dismiss `2337:20122`; one `2337:20290`; none `2337:20436`; empty-one `2337:20667`; empty-none `2337:20884`; empty-accepted `2337:20986`; overflow `2338:20884`; accepted-open `2338:20977`; empty overflow `2338:21059`; empty→populated overflow `2338:21133`. |
| Agenda other authored sections | `2283:5161`, `2295:13690`, `2315:13649`, `2323:16828`, `2343:33618`, `2363:26053`, `2390:28498`, `2444:47093` | Organization, Scheduling, actions, dates/sorting, Brief, recovery, creation, saved details. Inventory coverage only, not implementation-ready certification. |
| Inspector | set `2431:33984`, shared section `2404:30390`, Task `2404:30391`, Event `2432:34069` | 21 variants confirmed. Unresolved date, minimum-duration and all-day-inclusivity semantics expressly excluded. |
| Onboarding New page | `2213:9168` | Sections: sign-in/consent `2213:9196`; Check-in `2213:9259` (excluded); Connectors `2213:9317`; Voice `2213:9346`; guided `2213:9361`; connected-flow reference `2236:73048`. |
| Voice masters | `2219:22520`, `2219:22800`, `2219:23081`, `2219:23361`, `2219:23641`, `2219:23921` | Default, preview, selected, speed, consistency, likeness. Shared core `2217:1571`; current default instance `2219:22589` explicitly has Show conversation entry=false. |
| Voice chooser | `2221:83686`, `2221:83798`, `2221:84017` | Default, preview, selected; core `2217:1901`. |
| Connector masters | `2229:73858`, `2229:74038`, `2229:74248`, `2229:74404`, `2229:74561` | Default, consent, connected, recoverable failure, expanded. Shared ConnectorRow set `2213:9330`; onboarding connected row uses Switch variant `2247:1963`. |
| Sign-in / guided | set `2213:9205`; guided instances `2213:9363`–`2213:9366` | Sign-in iOS `2213:9206`, Android `2213:9232`. Inventory only; actual auth remains external. Guided three Agenda steps + DailyBriefCard exist; not yet high-fidelity audited. |

## Agenda Suggestions: verified behavior and composition

- All four approved slots `2049:10103`, `2049:10127`, `2052:11463`, `2191:12585` have **paddingTop=24**, paddingBottom=0. Their parent stack spacing is0; slots immediately follow AgendaAddSchedule. Remove the slot with the section to remove its gap.
- Initial inline titles: “Prep for tonight’s rehearsal”; “Move rehearsal check-in to 3:00 PM”; “Review venue notes”. Overflow adds “Bring the updated set list”. Exact copy is in saved contexts.
- Live source notes `2338:21758` / `2338:21759`: up to3inline, all live rows in overflow. The authored example fixtures have Add create a5PM task today and Move update existing rehearsal confirmation to3PM; these are not general app defaults. Successful Add/Move and dismiss remove optimistically; fetch failure may restore a row. No dedicated row spinner, success badge, error card or Retry. Last removal hides section and closes overflow; sheet remains open across empty→populated while suggestions remain. Prototype should model this locally, not invoke real fetches.
- Overflow uses native default sheet, custom Suggestions/Done header, Divider and ScrollView. Source notes explicitly specify no fixed detents, custom drag-indicator modifier or NavigationStack requirement.
- Reusable Figma dependencies: DateNavigationHeader `43:2`; TaskEventRow set `46:21`; AgendaSortTrigger `2190:12239`; AgendaAddSchedule `2189:11919`; Section set `1307:667`; SectionHeader `161:68`; current base-loaded suggestion set `48:25`. New outcome flow uses AgendaSuggestionRow set `2336:19583`, variants add `2336:19561` / move `2336:19572`, Title/Metadata text props. Do not infer a global Plain rule from this section's local Plain variant.
- Test proof: three-inline versus four-sheet; Add task5PM; Move target3PM; Dismiss one; final removal removes title/24pt gap and closes sheet; empty→populated preserves sheet when rows remain; Done exits unchanged; state reset deterministic.

## Onboarding reuse and exact differences

Voice lockup: “Choose how Rem sounds” / “Preview a voice, choose the one Rem uses, then fine-tune its delivery.” Shared Lockup `773:22`, ActionArea `773:28`, Continue + Skip. Default Aria/Warm; other voices Sol/Bright, Rowan/Calm, Juniper/Expressive, Vale/Neutral. Shared control copy/default50/75/50 and prior state evidence remain in `../shared-final/VOICE.md`. Live root and chooser contexts confirm the same cores. Native sliders, SF symbols and existing utility-icon treatment should be reused; onboarding hero is currently blue, not evidence for a blanket icon amendment.

Connector lockup: “Connectors” / “Connect Rem to the tools you use so it can keep you up to date and surface what needs doing.” Initial Gmail connected/on; Google Calendar and Slack available. Expanded adds Notion and GitHub; See more becomes Show less. Connected example changes Google Calendar to Connected/Switch; failure shows “Couldn’t connect · Try again”, row Retry, bottom Try again + Skip. Consent master instead embeds **Notion** `2235:1840` from Provider set `1898:54528`. These are separate authored examples: do not present Notion consent as Google Calendar permission or invent missing Google/Slack/GitHub OAuth UI. Screen reactions arrays were empty; connected-flow diagram provides associations, not executable navigation wiring.

Existing Settings components available for reuse (from the delivered contracts): `VoiceControlsContent`, `VoiceChooserContent`, `VoiceSettingsFixture`; `ConnectorRow`, `ConnectorProviderMark`; `ProviderPreConsentContent`; `ListRow`, `ListRowLabel`, `ContainedIcon`, `SectionHeader/Footer`, RemSection/RemButton equivalents. Prefer native List/Section/NavigationStack/sheet on SwiftUI and native Material controls on Compose while preserving these shared core APIs.

Assets: Voice needs no new bitmap; native speaker/waveform/play.fill/pause.fill/checkmark.circle.fill semantics were already verified. Gmail, Google Calendar, Slack and Notion marks have exact saved assets under `../assets` / `../shared-final/assets`. Expanded Onboarding introduces **GitHub**, absent from the earlier seven-provider Settings enum; fresh expanded context contains its actual vector asset references. Export its exact node asset before implementation, do not approximate. Sliders' Figma ticks/platform status/home indicators are native UI references, not app image assets.

## Scope exclusions / remaining source work

Check-in, Automations, Chat redesign and exploratory Jack/Jill context gathering are excluded. No new Plain/global filled-icon policy. No invented authentication, service calls, audio samples or inspector date rules. Before implementing the wider Agenda shell, creation/scheduling/detail/Brief/recovery must receive their own bounded high-fidelity state audit. Before full Onboarding Connectors implementation, resolve provider-specific transition scope and capture GitHub mark. The Voice slice and Agenda Suggestions fixture slice can proceed independently of these gaps.
