# Contract — Sidebar · New agent · Agent profile (About)

**Outcome:** the person reaches every agent from one sidebar, creates a side agent by naming its
lane first, and sees what any agent handles — identically on iOS (SwiftUI) and Android (Compose).
**Mode:** Reproduce (approved draft frames) + Systemize (new canonical components).

**Authority:** Figma `af4yDqCzp57jds9lkFiIaO`, page **Proposed · Navigation & Daily Brief (draft)**
(`2693:1474`), plus Samuel's approved product decisions (2026-10):

- Sidebar: **Search is the only top action**; sections **Main agent** (Rem) and **Side agents**, each
  row showing the agent name + lane subtitle; one **New agent** entry; the **account pill**
  ("S · Samuel": initial + name) at the bottom **is** the Settings entry — no separate gear.
- New agent setup is like Grok's bot setup: **the first step is naming the lane/agent**.
- Copy is exact: "New agent", "Side agents", "Main agent".

| Surface | Figma node | SwiftUI | Compose |
|---|---|---|---|
| Sidebar (open, Claude-style push) | `2702:2479` (column `2702:2514`, bottom actions `2702:2682`) | `Navigation/RemSidebar.swift` + `RemSidebarContainer.swift` | `navigation/RemSidebar.kt` + `RemSidebarContainer.kt` |
| Chat with sidebar closed | `2702:2351` | `RemSidebarContainer(isOpen: false)` | `RemSidebarContainer(isOpen = false)` |
| Account pill (opens Settings) | `2709:2648` | `Navigation/RemAccountPill.swift` | `navigation/RemAccountPill.kt` |
| New agent — set up | `2714:3538` | `Agents/NewAgentSetupScreen.swift` | `agents/NewAgentSetupScreen.kt` |
| Agent profile — identity | `2718:3060` (and the Rem block in `2718:3396`) | `AgentProfileHeader` (`Agents/AgentProfileAbout.swift`) | `AgentProfileHeader` (`agents/AgentProfileAbout.kt`) |
| Agent profile — About tab | `2718:3205` in `2718:3055` | `AgentProfileAbout` | `AgentProfileAbout` |
| Agent avatar | `2714:3587` (80), `2718:3061` (96), Rem disc `2718:3396` | `Agents/AgentAvatar.swift` | `agents/AgentAvatar.kt` |

Display models (no networking, no persistence): `RemSidebarModel` / `AgentModels` /
`NewAgentSetupModel` / `AgentProfileModel` on both platforms, named and shaped identically.

## State contract

- **Source of truth:** the host. The DS renders `RemSidebarDisplay`, `NewAgentDraft`,
  `AgentProfileDisplay` and emits typed actions (`RemSidebarAction`, `NewAgentSetupAction`,
  `AgentProfileAction`). It never creates, renames, pauses, deletes or routes an agent.
- **Selection** is the host's current conversation identity (`.main` / `.side(id)` / none). It is
  never inferred from list order; a stale id highlights nothing rather than falling back to a row.
- **Create:** `.create(draft)` is emitted only when the trimmed name is non-blank and carries the
  trimmed draft. The host owns the request, its pending/failed states, retry, and opening the new
  agent's first chat (`E2`, `2717:2725`). A failure after dispatch is the host's to reconcile — the
  DS never re-emits Create on its own.
- **Delete / Pause:** emitted as intents only; the host confirms Delete before acting.
- **Sidebar open/closed:** host-owned `isOpen`; the container only animates geometry and offers
  tap-to-close on the visible chat card.
- **Non-goals:** Search UI, the icon chooser behind "Tap to change icon", the service picker, the
  agent's first chat (E2), the Activity/Approvals tabs (owned by Agent activity).

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

**New agent setup** (`backgroundPrimary`, 16 inset): nav bar (Cancel · "New agent") → 80pt avatar
(tap = change icon) + "Tap to change icon" + five 36pt swatches (pink, blue, green, purple, indigo;
selected = 2pt `labelPrimary` ring) → **Name** field (first, focused) + footer → **What should this
agent handle?** multiline + footer → **Connected services** (attached services, then "Add a service")
+ "Optional. You can connect more later." → bottom-pinned **Create agent** (`Button · Rect · Black`),
disabled until named.

**Agent profile About:** **Lane** (Name, Handles — chevron rows for side agents, read-only for Rem)
→ **Connected services** (services + Add a service) → side agents only: a grouped block with
**Pause agent** (accent) and **Delete agent** (destructive).

## Icons (registry rows added in this change)

`magnifyingglass`/`search` 0 · `bubble.left`/`chat_bubble` 0 · `plus`/`add` 0 ·
`chevron.up.chevron.down`/`unfold_more` 0 · `link`/`link` 0 · `calendar`/`calendar_month` 0 ·
`heart.fill`/`favorite` 1. See [icon registry](./icon-registry.md).

## Accessibility identifiers (both platforms)

`sidebar`, `sidebar.search`, `sidebar.section.main|side`, `sidebar.agent.main`,
`sidebar.agent.<id>`, `sidebar.newAgent`, `sidebar.account` (label = name, hint "Opens Settings"),
`sidebar.close` · `newAgent.cancel|icon|colors|color.<color>|name|handles|service.<id>|addService|create`
· `agentProfile.identity|about|lane.name|lane.handles|service.<id>|addService|pause|delete`.

## Parity acceptance (render keys)

`Sidebar-open-light`, `Sidebar-open-dark`, `Sidebar-closed-light`, `Sidebar-empty-light`,
`AccountPill-light`, `NewAgent-empty-light`, `NewAgent-filled-light`, `NewAgent-filled-dark`,
`AgentProfile-side-about-light`, `AgentProfile-main-about-light` — emitted by
`tools/render-swift/Tests/RenderSnapshotTests/SidebarRenderSnapshots.swift` and
`compose/RemDesignSystem/src/test/kotlin/com/rem/designsystem/SidebarSnapshots.kt`. Not yet registered
in `tools/render-evidence/contracts.json` (needs Figma reference exports for each key).

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
