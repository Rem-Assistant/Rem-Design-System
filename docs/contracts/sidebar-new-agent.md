# Contract — Sidebar · New agent · Agent profile (About)

**Outcome:** the person reaches every agent from one sidebar, creates a side agent by giving it a
name and a colour, and sees what any agent handles — identically on iOS (SwiftUI) and Android (Compose).
**Mode:** Reproduce (approved draft frames) + Systemize (new canonical components).

**Authority:** Figma `af4yDqCzp57jds9lkFiIaO`, page **Proposed · Navigation & Daily Brief (draft)**
(`2693:1474`), plus Samuel's approved product decisions (2026-10):

- Sidebar: **Search is the only top action**; sections **Main agent** (Rem) and **Side agents**, each
  row showing the agent name + lane subtitle; one **New agent** entry; the **account pill**
  ("S · Samuel": initial + name) at the bottom **is** the Settings entry — no separate gear.
- New agent setup (revised 2026-10-11): "Our agent creation does not feel like Grok's… I think just a
  name and some color is fine." Setup asks for **a name and a colour only** — no "What should this
  agent handle?", no services, no icon chooser. Naming stays the first (focused) step.
- Copy is exact: "New agent", "Side agents", "Main agent", "Create agent".

| Surface | Figma node | SwiftUI | Compose |
|---|---|---|---|
| Sidebar (open, Claude-style push) | `2702:2479` (column `2702:2514`, bottom actions `2702:2682`) | `Navigation/RemSidebar.swift` + `RemSidebarContainer.swift` | `navigation/RemSidebar.kt` + `RemSidebarContainer.kt` |
| Chat with sidebar closed | `2702:2351` | `RemSidebarContainer(isOpen: false)` | `RemSidebarContainer(isOpen = false)` |
| Account pill (opens Settings) | `2709:2648` | `Navigation/RemAccountPill.swift` | `navigation/RemAccountPill.kt` |
| New agent — set up (name + colour) | `2714:3538` — **superseded**, see Figma status | `Agents/NewAgentSetupScreen.swift` | `agents/NewAgentSetupScreen.kt` |
| Agent profile — identity | `2718:3060` (and the Rem block in `2718:3396`) | `AgentProfileHeader` (`Agents/AgentProfileAbout.swift`) | `AgentProfileHeader` (`agents/AgentProfileAbout.kt`) |
| Agent profile — About tab | `2718:3205` in `2718:3055` | `AgentProfileAbout` | `AgentProfileAbout` |
| Agent avatar | `2718:3061` (96), Rem disc `2718:3396`; 120 in New agent (no frame yet) | `Agents/AgentAvatar.swift` | `agents/AgentAvatar.kt` |

Display models (no networking, no persistence): `RemSidebarModel` / `AgentModels` /
`NewAgentSetupModel` / `AgentProfileModel` on both platforms, named and shaped identically.

## State contract

- **Source of truth:** the host. The DS renders `RemSidebarDisplay`, `NewAgentDraft`,
  `AgentProfileDisplay` and emits typed actions (`RemSidebarAction`, `NewAgentSetupAction`,
  `AgentProfileAction`). It never creates, renames, pauses, deletes or routes an agent.
- **Selection** is the host's current conversation identity (`.main` / `.side(id)` / none). It is
  never inferred from list order; a stale id highlights nothing rather than falling back to a row.
- **New agent draft:** `NewAgentDraft(name, color)`. `color` is one `AgentColor` and defaults to
  `AgentLook.default.color` (**pink**). `draft.look` is the default glyph (`heart`) on the chosen
  colour; the avatar preview renders it, so it updates live as the selection changes. Selecting a
  swatch only mutates the host-owned draft (binding / `onDraftChange`); it emits no action.
- **Setup actions:** `NewAgentSetupAction` is exactly `.cancel` (the X) and `.create(draft)`.
- **Create:** `.create(draft)` is emitted only when the trimmed name is non-blank and carries the
  draft with its name trimmed and its selected colour. Create is disabled (and announced as such —
  VoiceOver "dimmed" + hint, TalkBack disabled + state description) until then. The host owns the
  request, its pending/failed states, retry, and opening the new agent's first chat (`E2`,
  `2717:2725`). A failure after dispatch is the host's to reconcile — the DS never re-emits Create on
  its own.
- **Colour as theme (later):** the chosen colour is the agent's identity colour. Using it as that
  agent's chat theme (outgoing bubbles / accents) belongs to a future Appearance surface, not here.
- **Delete / Pause:** emitted as intents only; the host confirms Delete before acting.
- **Sidebar open/closed:** host-owned `isOpen`; the container only animates geometry and offers
  tap-to-close on the visible chat card.
- **Non-goals:** Search UI; in setup: the lane description ("What should this agent handle?"),
  services, any icon/glyph chooser, a shape picker, suggestion cards, and the Appearance screen /
  live chat-theme preview; the agent's first chat (E2); the Activity/Approvals tabs (owned by Agent
  activity). Lane description and services remain on the Agent profile About tab.

## Layout

**Sidebar** (`backgroundSecondary`, 16 leading / 12 trailing inset):

1. Header: **"Rem"** (`Typography.title1Bold`) + a 44pt circular **Search** button. Nothing else.
2. **Main agent** section header (17pt semibold `labelSecondary`, sentence case) → one Rem row.
3. **Side agents** section header → one row per side agent. The section is omitted when there are
   none (no lone header).
4. Rows: ≥60pt, 12 h / 14 v padding, 12pt radius; leading 29pt mark (Rem face in `brandBlue` for the
   Main agent, `bubble.left` / `chat_bubble` for side agents); title `body` one line, truncating
   (Main agent semibold); lane subtitle `footnote` `labelSecondary`; optional 8pt `brandBlue` unread
   dot trailing. Selected row = `fillTertiary` fill.
5. Floating bottom actions over a `backgroundSecondary` fade (list scrolls under): account pill
   leading, **New agent** (56pt `labelPrimary` capsule, `plus` + bodyBold) trailing.

**Push container:** open = chat offset by `RemSidebarLayout.sidebarWidth` (container − 70, max 332),
44pt continuous corners + 12% shadow; tapping the visible chat closes.

**New agent setup** — a sheet that draws its own header (no navigation bar; host presents it
directly), `backgroundPrimary`, 16 inset. Metrics live in `NewAgentSetupLayout` on both platforms.

1. Header: 44pt circular **close (X)** button leading (`backgroundSecondary` disc, SF `xmark` /
   Material `close`, label "Close") · centred **"New agent"** (`bodyBold`, header trait) · a 44pt
   spacer trailing so the title stays centred. Mirrors the sidebar's 44pt circular Search button.
2. 24pt below: **avatar preview**, 120pt `AgentAvatar(.side(draft.look))` — the colour updates live.
3. 24pt below: **name pill** — full-width `Capsule` filled `fillTertiary`, centred text in
   `title3` semibold, placeholder **"Agent name"** (`labelTertiary`), accessibility label "Name",
   min height 56 and grows with text size (no fixed height), words capitalisation, first focused.
4. 24pt below: **colour grid** — the 11-colour palette in rows of 6 (6 + 5), centred, 12pt gaps.
   Each swatch is a 44pt target holding a 34pt disc; selected = 2.5pt `labelPrimary` ring at 44pt.
   Label = colour name ("Blue"), selected trait (SwiftUI `.isSelected`; Compose `selectable` radio
   in a `selectableGroup`). Group label "Color".
5. Bottom-pinned **Create agent** (`Button · Rect · Black` / `AgentPrimaryButton`), disabled until
   the trimmed name is non-blank; disabled hint "Name your agent to continue."

**Agent colour palette** (`AgentColor`, both platforms, this order) → token:
red `systemRed` · orange `systemOrange` · yellow `systemYellow` · green `systemGreen` · mint
`systemMint` · teal `systemTeal` · cyan `systemCyan` · blue `systemBlue` · indigo `systemIndigo` ·
purple `systemPurple` · pink `systemPink`. Mint, teal and cyan were added to `tokens/tokens.json` in
this change (SwiftUI `Color.mint/.teal/.cyan`; Compose/CSS Apple light/dark hex: mint `#00C7BE` /
`#63E6E2`, teal `#30B0C7` / `#40C8E0`, cyan `#32ADE6` / `#64D2FF`).

**Agent profile About:** **Lane** (Name, Handles — chevron rows for side agents, read-only for Rem)
→ **Connected services** (services + Add a service) → side agents only: a grouped block with
**Pause agent** (accent) and **Delete agent** (destructive).

## Icons (registry rows added in this change)

`magnifyingglass`/`search` 0 · `bubble.left`/`chat_bubble` 0 · `plus`/`add` 0 ·
`chevron.up.chevron.down`/`unfold_more` 0 · `link`/`link` 0 · `calendar`/`calendar_month` 0 ·
`heart.fill`/`favorite` 1 · `xmark`/`close` 0 (New agent close). See [icon registry](./icon-registry.md).

## Accessibility identifiers (both platforms)

`sidebar`, `sidebar.search`, `sidebar.section.main|side`, `sidebar.agent.main`,
`sidebar.agent.<id>`, `sidebar.newAgent`, `sidebar.account` (label = name, hint "Opens Settings"),
`sidebar.close` · `newAgent.cancel|avatar|name|colors|color.<color>|create` (`<color>` = the lowercase colour id)
· `agentProfile.identity|about|lane.name|lane.handles|service.<id>|addService|pause|delete`.

## Parity acceptance (render keys)

`Sidebar-open-light`, `Sidebar-open-dark`, `Sidebar-closed-light`, `Sidebar-empty-light`,
`AccountPill-light`, `NewAgent-empty-light` (no name, Create disabled, default pink),
`NewAgent-filled-light` / `NewAgent-filled-dark` ("Health", **teal** selected),
`NewAgent-largeText-light` (filled; SwiftUI `.accessibility2`, Compose font scale 2.0),
`AgentProfile-side-about-light`, `AgentProfile-main-about-light` — emitted by
`tools/render-swift/Tests/RenderSnapshotTests/SidebarRenderSnapshots.swift` and
`compose/RemDesignSystem/src/test/kotlin/com/rem/designsystem/SidebarEvidence.kt` (run from
`EvidenceSnapshots`, so files are named `com.rem.designsystem_EvidenceSnapshots_<method>_<key>.png`).
Not yet registered in `tools/render-evidence/contracts.json` (needs Figma reference exports for each
key).

## Figma status (deferred)

The referenced frames on page 2693:1474 are drafts and lag the approved product decisions this code
follows: the sidebar frame still shows Agenda and Inbox rows, which Samuel's "Search only" decision
removed. Figma authoring and reference exports for these surfaces are deferred until Samuel decides
where Agenda and Inbox live; the render keys are registered in `tools/render-evidence/contracts.json`
only once those exports exist. Until then the paired iOS/Android renders are the review evidence.

**New agent frame `2714:3538` is superseded.** It shows the earlier icon + five swatches + Name +
"What should this agent handle?" + Connected services layout. The name + colour sheet above is the
authority until the frame is redrawn in Figma (pending; not part of this change).

## Reconciliation (open, owned by whoever lands second)

- **Agent activity (PR #106).** `AgentActivityScreen` / `AgentActivityTab` are not on main. When both
  land: add `.about` to `AgentActivityTab`, render `AgentProfileHeader` as its identity block (it
  generalizes the Rem-only face to any agent), and render `AgentProfileAbout` for the About tab.
- **Section header.** These surfaces use an internal `AgentSectionHeader` above a header-less
  `RemSection` because main's `RemSection(header:)` is the older uppercased footnote style. Replace
  with `RemSection(...).settingsHeader()` (Swift) / `settingsHeader = true` (Compose) from PR #106.
- **Row / tile metrics.** Main's `ListRow` renders a semibold title + caption subtitle and
  `ContainedIcon(.small)` is 38pt; Figma's current ListRow uses a regular title + footnote and a 29pt
  tile (`.settings` on PR #106). Compose mirrors main's Swift values (`AgentListRow`,
  `AgentPrimaryButton`) until `rows/ListRow.kt` / `buttons/RemButton.kt` land from PR #106.
- **Icon fonts.** This change and PR #106 both re-subset the Material Symbols `.ttf`s. Resolve by
  taking the union of both `GLYPHS` maps in `tools/material-symbols/subset.py` and re-running it.
