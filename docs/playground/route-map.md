# Playground route map (Build 9 app shell)

The native Playgrounds (iOS `tools/playground-ios`, Android `compose/demo`) open on an **app shell**.
It is one full-screen conversation, with the sidebar behind the chat header and Settings behind the
account pill. Everything else that used to sit on the Playground root (component catalog, screens with
their fixture pickers, onboarding) now lives in a separate **Developer** hub.

**Fixtures only.** Nothing in the shell talks to a backend. There is no auth, voice, telephony or
provider behaviour, and nothing is persisted:
- conversations come from `ChatPlaygroundFixture`;
- the shell's selection, sidebar and presentation come from `PlaygroundShellFixture`
  (`Sources/RemDesignSystem/Screens/PlaygroundShellFixture.swift`,
  `compose/RemDesignSystem/screens/PlaygroundShellFixture.kt`);
- Settings uses the Build 8 Settings fixtures.

Approved designs used:
- Figma A2 · Chat, sidebar closed (`2702:2351`) and the sidebar (`2702:2479`);
- the account pill (`2709:2648`);
- New agent as a name + colour sheet (`docs/contracts/sidebar-new-agent.md`);
- `ChatScreen` (`docs/contracts/chat.md`);
- the Build 8 Settings destinations (`docs/contracts/settings-playground.md`).

## Shell state contract

- **Source of truth:** `PlaygroundShellFixture`, one value per app run. The hosts render it and add
  native chrome only. Its state:
  - `selection` (`.main` or `.side(id)`, always a listed agent);
  - `isSidebarOpen`;
  - `presented` (`newAgent` or `settings`, at most one at a time);
  - the New agent `draft`;
  - the side-agent list.
- **Transitions:**
  - The header sidebar button sets `openSidebar()`.
  - Choosing a row selects that agent and closes the sidebar.
  - New agent and the account pill close the sidebar and present their surface.
  - New agent Cancel dismisses and changes nothing.
  - New agent Create (trimmed, non-blank name) appends the agent, selects it and dismisses.
  - Settings Done or Back at its root dismisses it.
  - Search has no approved surface yet, so it changes nothing.
- **Retention:** the shell stays alive under Settings and New agent. Returning finds the same agent,
  the same conversation (sent messages and drafts included) and the same scroll position. Closing
  Developer returns to the same selected agent. On iOS the conversation and scroll position are kept too.
  On Android the conversation reopens fresh: the hub hosts its own Chat with the same test tags, and
  Compose's `clearAndSetSemantics` hides covered nodes only from the merged tree, so the shell is not
  composed under the hub.
  Switching agents shows that agent's conversation. A side agent's conversation is a fresh fixture; a
  conversation is not kept per agent.
- **Developer-owned inputs:** the Chat conversation fixture (Populated, Empty or Long) and the Settings
  load fixture (Success, Slow or Error). They are chosen only in the Developer hub, and the shell reads
  them. Choosing a different Chat fixture restarts Rem's conversation.
- **Non-goals:** a Search surface; Agenda and Inbox in the sidebar (Samuel removed them from the
  sidebar; where they live is still open); a header overflow menu (the shell omits it, see Open
  decisions); persistence; real agent creation.

## Routes

| Route | Entry | Exit | Back / dismiss | Journeys (iOS `PlaygroundNavigationUITests` · Android `PlaygroundNavigationTest`) |
|---|---|---|---|---|
| **Chat (root)**: the selected agent's full-screen conversation, header with the sidebar button (`chat.header.menu`), no Back, no overflow | App launch; every surface below returns here | The surfaces below | Android system Back leaves the app (root) | `testShellSidebarSelectsAnAgentAndClosesIt` · `shellSidebarSelectsAnAgentAndBackCloses` |
| **Sidebar** (`sidebar`): Main agent, Side agents, New agent, account pill | `chat.header.menu` | Choose an agent; tap the visible chat card (`sidebar.close`) | Android system Back closes it; selection kept | as above |
| **New agent** sheet (`newAgent.*`) | Sidebar `sidebar.newAgent` | Close (`newAgent.cancel`); Create (`newAgent.create`) opens the new agent's chat | iOS swipe down and Android system Back / scrim tap all cancel | `testShellNewAgentCancelsAndCreates` · `shellNewAgentCancelsAndCreates` |
| **Settings**: Build 8 entry over the conversation (iOS sheet with `settings.done`; Android full screen with Back `back`) | Sidebar account pill `sidebar.account` | Done / Back to the same chat | Pushed pages pop (top-bar Back and Android system Back); iOS swipe down dismisses the sheet | `testShellSettingsFromAccountReturnsToTheSelectedChat` · `shellSettingsFromAccountReturnsToTheSelectedChat`; scroll kept: `testShellKeepsScrollPositionAcrossSettings` · `shellKeepsScrollPositionAcrossSettings` |
| Settings → **Agent settings** → destination (Paired devices, Connectors, Cloud browser, Memory, Models, Wallet, Voice, Automations) | `openAgent` → `agentDestination.*` | Back | Each level pops one level | as above; destinations in depth: `SettingsPlaygroundUITests` / `SettingsPlaygroundTest` |
| Settings → **Billing, Permissions, About, Help & Support** | `settingsDestination.*` | Back | Pops to Settings | as above (About) |
| Chat → **Agent activity** (pushed) | Header identity `chat.header.identity` | Back (`Chat`, `agentActivity.back`) | Pops to the same conversation | `testShellChatSheetsDismissAndActivityPops` · `shellChatSheetsDismissAndActivityPops` |
| Chat → **Add to Chat** sheet, **message actions** sheet | `chat.composerAdd`; long press on a message | Done, an action, or Android system Back | Sheets dismiss back to the chat | as above |
| **Developer** hub: the earlier Playground root, titled "Developer" | Settings' **Developer** button (`settings.developer`); `--developer` at launch on iOS (journey seam) | Close (`developer.close`) | Android system Back at its root closes it; inside, every route behaves as in Build 8 | `testShellDeveloperEntryStaysSeparate` · `shellDeveloperEntryStaysSeparate`; every Build 8 journey runs inside it |

## Developer entry

The developer tools are clearly separate from ordinary navigation:
- The shell's chat, sidebar and Settings show no catalog row, fixture picker, fixture-host control or
  build stamp.
- The only way in is a labelled **Developer** button in Settings' top bar.
- The hub keeps its own navigation and its own Close.

Inside the hub, everything from Build 8 is unchanged:
- the component catalog;
- Settings, Agenda, Chat and Inbox, each with its data picker;
- the onboarding hub;
- the Chat fixture-host controls (header overflow on the hub's Chat).

The shell's Chat uses the Developer-chosen conversation fixture.

How the journeys reach the hub:
- **iOS:** the established journeys launch with `--developer`, which opens straight into the hub. This
  launch seam is like `--loading-hold`.
- **Android:** a shared `@Before` (`PlaygroundShellSupport.kt`) walks the real route: header button →
  account pill → Developer.

The shell journeys close the hub and drive the shell itself.

## Layout

- **Title:** the conversation's title is the header identity (the agent's name and activity). There is
  no navigation bar or app bar above it. Settings and the Developer hub use native, inline, centred
  titles.
- **Safe areas:** `ChatScreen` owns its edges (#122). The transcript runs under the status bar and the
  dock, and the composer docks above the keyboard or IME and the home indicator. The sidebar sits
  inside the safe area: iOS through the container, Android with status-bar and navigation-bar padding.
- **Composer:** the same composer on every agent. Side-agent chats open empty, with the starters naming
  the agent.

## Open decisions (Samuel)

1. **Header overflow on the root conversation.** The approved frame `2702:2351` shows `⋯`, but no menu
   content is approved. The shell omits it, and the fixture-host controls it used to open stay in the
   Developer hub's Chat.
2. **Developer entry placement.** A labelled button in Settings' top bar. The alternatives are a
   "Developer" section at the end of Settings, or a hidden gesture with a visible label.
3. **Agenda and Inbox.** They are reachable only under Developer until their place in the shell is
   decided. The sidebar draft still shows them; the approved decision removed them.
4. **Search** in the sidebar has no surface, and tapping it does nothing.
5. **Account name.** The pill shows "Avery Diaz", the Settings profile fixture, rather than the Figma
   sample "Samuel", so the pill and Settings agree.
