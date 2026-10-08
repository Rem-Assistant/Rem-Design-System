# First expansion tasks: fixture-only contracts

Source snapshot: 2026-10-08, Figma file af4yDqCzp57jds9lkFiIaO. This directory is a versionable evidence packet outside every app repository. `raw/<node-id>.json` preserves full design-context tool responses, `.txt` extracts their text, and `.png` is the exact returned screenshot. Structural reads and inventories are separate JSON files. SHA256SUMS hashes packet files; these are snapshot-content hashes, not a Figma revision identifier. `reused-settings-evidence/` is earlier shared-core evidence explicitly labeled as reused, not fresh capture.

## Onboarding Voice

Section2213:9346 on page2213:9168. Fresh contexts/screenshots cover all9screen states:

| Node | State | Fixture difference |
|---|---|---|
|2219:22520|Default|Aria, Speed50%, Consistency75%, Likeness50%|
|2219:22800|Previewing|Aria · Playing, pause.fill|
|2219:23081|Selected|Sol / Sol (Bright), preview stopped|
|2219:23361|Speed adjusted|Speed75%, remaining defaults|
|2219:23641|Consistency adjusted|Consistency50%, remaining defaults|
|2219:23921|Likeness adjusted|Likeness75%, remaining defaults|
|2221:83686|Chooser default|Aria checked|
|2221:83798|Chooser preview|Aria pause, independent check remains|
|2221:84017|Chooser selected|Sol checked|

Reuse VoiceControlsContent2217:1571 (Show conversation entry=false) and VoiceChooserContent2217:1901. The clipped screen viewport does not mean the lower sliders/footer are absent; live context and shared-core source retain all three. Use scrolling plus safe-area bottom actions, not hardcoded heights or screenshot overlays. Fixed actions remain reachable at large Dynamic Type, and content must scroll fully above them.

**Navigation contract:** tapping Voice opens the chooser; native Back returns to the control shell with controlled selection/slider state retained. Selection and preview are separate targets. Outer Back exits this playground journey to the host. Continue and Skip call separately observable host callbacks, without inventing subsequent product screens or Check-in flow. The masters provide no prototype reaction wiring for these destinations; these host boundaries are the bounded playground implementation contract, not a claim of authored downstream navigation. Preserve the Settings fixture's explicit no-audio boundary; local play/pause state is not an audio service.

Prove: default → chooser → preview → Sol select → Back reflects Sol; three slider changes persist; preview can stop; Back/Continue/Skip invoke correct callbacks; light/dark/large type show all content and fixed actions. Reuse existing core API and assets; no duplicated Voice control tree.

## Agenda Suggestions

Use section2336:19584 and its authored outcomes; base Screen/Agenda2192:12182 supplies shell states. Fresh PNGs include initial2336:19585, Add2337:19714, Move2337:19928, Dismiss2337:20122, no suggestions2337:20436, empty-one2337:20667, empty-none2337:20884, empty accepted2337:20986, overflow2338:20884, accepted-open2338:20977, empty overflow2338:21059, empty→populated sheet2338:21133.

The5PMcreated task and3PMmoved rehearsal are **specific authored example fixtures**, not app-wide defaults. Preserve row IDs and other task/event content when applying actions. At most3inline; overflow has all live rows. Last removal removes whole Suggestions slot including24pt top padding and closes overflow. Source-defined optimistic restoration can be a deterministic fixture mode; no network fetch. No invented loading/error cards or retry buttons.

Parent's approved slice excludes Inspector, date validation, minimum duration and all-day inclusivity. Use host callbacks or explicitly separate playground route boundaries for unrelated shell affordances rather than implying those product flows are complete. No Check-in/Automations/Chat redesign or global icon-style change.

## Connector blocker (do not implement yet)

Consent screen2229:74038 embeds Notion Provider2235:1840, while Connected2229:74248 and Recoverable failure2229:74404 change Google Calendar. This does not establish a valid provider-specific consent→success journey. Expanded2229:74561 also requires exact GitHub logo asset support beyond the prior Settings seven-provider set. These are source-scope gaps; no guessed OAuth/provider screens. Root must resolve before dispatching this lane.
