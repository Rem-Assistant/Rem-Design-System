# Figma file organization — current state + proposed structure

> Rem Figma file `af4yDqCzp57jds9lkFiIaO`. Founder flagged 2026-09-25: "figma file organization is
> still the piece that's unclear." **✅ EXECUTED 2026-09-25 (founder greenlit "Yes — reorganize").**
> The banded structure below is now live: `Cover · Guide · —FOUNDATIONS— · —COMPONENTS— (Components,
> Primitives, Rows & Controls, Cards, Chat components, Tasks & Agenda) · —SCREENS— (Onboarding,
> Agenda, Inbox, Chat, Task & Events, Settings) · —PATTERNS— (Chat Scenarios, Flows) · —PROPOSED— ·
> —KIT— (Platform Controls, Device Kit) · —ARCHIVE— (Retired)`. The 3 chat pages are disambiguated
> (Chat components / Chat / Chat Scenarios), the 2 dead flows moved to **Retired**, empty Utilities
> deleted. Remaining (content-level, follow-up): gather the scattered "Proposed" frames onto the one
> Proposed band; repurpose Guide's contents.

## Current pages (25, in order) — and the problems

Cover · Getting started *(empty)* · `---` · **Foundations** · `---` · **Components** *(index only)* ·
**Primitives** · **Rows & Controls** · **Chat** *(components `297:2`)* · **Tasks & Agenda** · **Cards**
· `---` · **Agenda** · **Chat** *(screens `356:3`)* · **Task & Events** · **Settings** · **Inbox** ·
**Onboarding** · **Flows** · **Proposed · Agent surfaces** · **Proposed · Onboarding** · `---` ·
**Platform Controls** · **Device Kit** · Utilities *(empty)* · **Chat · Scenarios** `524:2`

**What makes it hard to navigate:**
1. **Three chat homes, two pages literally named "Chat"** (`297:2` components, `356:3` screens) + `Chat
   · Scenarios` `524:2` (where all my recent work landed). No way to tell them apart in the sidebar.
2. **Components scattered over 6 pages** (Components / Primitives / Rows & Controls / Chat / Tasks &
   Agenda / Cards) with no COMPONENTS band. "Components" holds only an index.
3. **Screens scattered over 7 pages** with a half-applied ①–⑤ numbering (Device Kit ①, Agenda ③, Chat
   ④, Inbox ⑤ — ② missing, order doesn't match sidebar).
4. **"Proposed" spread across 4 spots** (Proposed · Agent surfaces, Proposed · Onboarding, "Agenda
   scenarios (proposed)" `530:22`, "General (Proposed)" `509:833`).
5. **Retired dead flows live *inside* the Onboarding page** (`414:15`, `415:15`) mixed with live screens.
6. **Empty pages** (Getting started, Utilities) and three bare `---` dividers.

## Proposed structure (bands via divider pages)

```
📕 Cover
📖 Guide              how to use · token↔code map · Code Connect status   (repurpose "Getting started")

——— FOUNDATIONS ———
🎨 Foundations        Color · Typography · Spacing · Radius · Icons

——— COMPONENTS ———
🔹 Primitives         buttons · pills · badges · controls · indicators
🔹 Rows & Controls    ListRow + accessories (as today)
🔹 Cards              all card components — incl. the 16 tool-result families (see below)
🔹 Chat components    composer · bubble · ContextualMessage · browser cards · run-activity · markdown
🔹 Tasks & Agenda     task row · badges · DateNav · FreeTime

——— SCREENS (canonical · code-with-callers) ———
📱 Onboarding · Agenda · Inbox · Chat · Tasks & Events · Settings   (one page each)

——— PATTERNS ———
🧩 Chat Scenarios     state matrices, as INSTANCES of the Chat components (not one-off frames)
🧭 Flows              navigation map

——— PROPOSED (not-yet-shipped · founder-gated) ———
🟡 Proposed           onboarding future (488:2) · agent surfaces · General · agenda-scenarios

——— KIT ———
⚙️ Platform Controls  iOS 26 sheets/nav/activity   ·   ⚙️ Device Kit  bezel + screen slot

——— ARCHIVE ———
🗄️ Retired            dead flows: start-in-chat (414:15) · deploying (415:15)
```

## The moves (mechanical, reversible)

1. **Disambiguate chat:** rename `297:2` → **"Chat components"**, keep `356:3` → **"Chat"** (screens),
   rename `Chat · Scenarios` `524:2` → **"Chat Scenarios"** under Patterns.
2. **Band the components** (Primitives / Rows & Controls / Cards / Chat components / Tasks & Agenda)
   and **band the screens** (6 screen pages) under CAPS divider pages.
3. **Move retired** `414:15` + `415:15` out of Onboarding → **Retired** (Archive).
4. **Gather Proposed** (`449:56`, `488:2`, `509:833`, `530:22`) under one **Proposed** band.
5. **Repurpose/remove empties:** Getting started → Guide; delete Utilities.

## Tool-result cards (the "tool calls in chat" the chat surface is growing into)

`ParsedToolResult` (`ToolResultParser.swift`) is **16 cases** → a few card *components* with variants,
all belonging on the **Cards** page:

| Family | Cases | Card component | Status |
|---|---|---|---|
| Calendar | events / add / update / delete | CalendarEventsCard + ConfirmationCard | ✓ `570:31` · Confirmation ✓ `557:31` |
| Reminders | list / add / update / delete | RemindersCard + ConfirmationCard | ✓ `570:59` · Confirmation ✓ |
| Tasks | create / update / delete | ConfirmationCard | ✓ `557:31` |
| Device | status / info | DeviceStatusCard · DeviceInfoCard | Status ✓ `566:68` · Info ✓ `570:86` |
| Notify | success | ConfirmationCard | ✓ |
| Error / Unknown | error / unknown | ErrorResultCard · collapsed "Tool result" | ✓ `557:31` |

**All 16 `ParsedToolResult` cases now have built card components.** (Reminders logo is an orange-tile
placeholder — the only brand-asset debt.)
