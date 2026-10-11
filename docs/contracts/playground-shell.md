# Playground app shell (Build 9) — bounded contract

Source of truth: Figma A2 · Chat, sidebar closed [`2702:2351`](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/?node-id=2702-2351), the sidebar [`2702:2479`](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/?node-id=2702-2479) and the account pill [`2709:2648`](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/?node-id=2709-2648). The **state contract** (source of truth, transitions, retention, non-goals, routes and journeys) is [`docs/playground/route-map.md`](../playground/route-map.md); this file is the parity acceptance list the review gate checks both Playgrounds against. Composed surfaces keep their own contracts: [chat.md](chat.md), [chat-adapter.md](chat-adapter.md), [sidebar-new-agent.md](sidebar-new-agent.md), [settings-playground.md](settings-playground.md).

## Scope and states

The native Playgrounds (iOS `tools/playground-ios`, Android `compose/demo`) open on one full-screen conversation with the sidebar behind the chat header's sidebar button and Settings behind the account pill. Shell state is the design-system `PlaygroundShellFixture` (Swift `Sources/RemDesignSystem/Screens/PlaygroundShellFixture.swift`, Kotlin `compose/RemDesignSystem/screens/PlaygroundShellFixture.kt`); the hosts draw native chrome only. The Developer hub (component catalog, fixture pickers, every Build 8 screen) is reached only from Settings. Fixtures only: no backend, auth, voice, telephony, provider or persistence.

## Parity acceptance

Both platforms must behave identically on every line; a difference is a P2 unless recorded here as a platform convention.

- **Retention per route** (route-map "Retention"). The live conversation is identified by `PlaygroundShellFixture.conversationKey` plus the Developer Chat fixture.
  - Sidebar opened and closed, or the selected agent chosen again: same conversation, draft and scroll.
  - Settings (and its pushed pages) → Done / Back: same agent, conversation (sent messages included), draft and scroll.
  - New agent → Cancel: nothing changes.
  - Developer → Close with no fixture change: same agent, conversation, draft and scroll.
- **Reset rules.** Only the live key is kept.
  - Switching agents starts that agent's conversation afresh, and switching back starts the earlier one afresh (Rem → Health → Rem is fresh).
  - New agent → Create selects the new agent: a fresh, empty conversation naming it.
  - Choosing a different Chat fixture in Developer is an explicit reset, and so is choosing the earlier fixture again (Populated → Long → Populated is fresh).
  - iOS: `.id("<conversationKey>|<fixture>")` on the chat. Android: the fixture is hoisted per key and every other key's fixture and `SaveableStateProvider` state are removed on a key change.
- **Covered-surface semantics.** A covered surface is out of VoiceOver, TalkBack and UI automation, and takes no input.
  - Sidebar open: the chat is hidden behind one "Close sidebar" button (`sidebar.close`); sidebar closed: the sidebar is hidden (sidebar-new-agent.md, push container accessibility).
  - Settings open: the shell is hidden (iOS sheet; Android `clearAndSetSemantics` on the shell).
  - Developer open: the shell is not in the tree (iOS full-screen cover; Android not composed), so the hub's own Chat identifiers never collide with the shell's.
- **Focus / IME on route change.** Leaving the conversation (sidebar, Settings, Developer) ends editing: the composer loses focus and the keyboard / IME hides before the next surface shows. The draft stays. New agent focuses its own name field. Journeys: `testShellRouteChangeHidesKeyboardAndKeepsDraft` · `shellRouteChangeHidesKeyboardAndKeepsDraft`.
- **Transcript position.** At rest the latest message sits clear of the composer dock; a restored transcript re-pins near the latest and otherwise keeps its distance from the bottom (chat.md rules 10 and 12).
- **Developer entry placement** (route-map open decision 2). A labelled **Developer** button (`settings.developer`) in Settings' top bar, the only way in, never on the chat, sidebar or Settings rows. Platform position, by each platform's top-bar convention:
  - **iOS:** `ToolbarItem(placement: .topBarLeading)`; Done (`settings.done`) is trailing.
  - **Android:** trailing `actions` of the Settings top app bar; Back (`back`) is the navigation icon (leading).
  The hub has its own Close (`developer.close`); Android system Back at its root closes it.
- **Root header.** The sidebar button (`chat.header.menu`, "Open sidebar", `line.3.horizontal` / `Icons.Filled.Menu`) replaces Back; no overflow (`chat.header.overflow` absent); no fixture picker, fixture-host control or build stamp outside Developer.
- **Render pairs** (iOS `ShellRenderSnapshots.swift` · Compose `ShellEvidence.kt`): `Shell-chat-light`, `Shell-chat-dark`, `Shell-sidebar-light`, `Shell-sidebar-dark`. Journey captures (simulator / emulator): `Shell-chat-light`, `Shell-sidebar-light`, `Shell-newAgent-light`, `Shell-settings-light`, and the keyboard-hidden Settings capture (iOS `Shell-settings-noKeyboard-light`, Android `Shell-settings-noIme-light`).
- **Journeys** prove each line on both platforms; the table in route-map "Routes" names them. Build success alone is insufficient.

## Completion boundary

Passing this contract means runnable local native fixtures with reviewed evidence. It does not imply production Rem integration, a shipped sidebar or agent model, design approval of the open decisions in route-map (header overflow, Developer entry placement, Agenda and Inbox, Search, account name), CI success or merge readiness.
