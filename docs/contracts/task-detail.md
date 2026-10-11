# Contract — TaskDetailScreen

**Outcome:** the task / event detail renders the same arrangement on iOS (SwiftUI) and Android (Compose):
a scrolling body (title, date and meta, then host sections) above the canonical composer used as the
task reply. **Mode:** Reproduce (shipping `TaskCommentsThread` + `TaskCommentComposer`).

**Authority:** Figma Task detail screen `299:2` (page "Task & Events"); shipping
`Shared/Views/Tasks/TaskCommentsSection.swift`. Written from the code on both platforms
(`Screens/TaskDetailScreen.swift`, `screens/TaskDetailScreen.kt`); differences are listed as questions.

## Layout

1. **Chrome is the platform's.** Back and the "Task" title come from the host's navigation bar / app bar.
   The template draws no navigation bar.
2. **Body:** one vertical scroll, inset `Spacing.lg`, regions `Spacing.lg` apart: the header, then the
   host's `content` slot (for example Last activity, Notes).
3. **Header:** the title in `title1Bold` `labelPrimary` (wraps); under it, only when there is a date or
   meta, one row `Spacing.sm` apart: the date in subheadline `labelSecondary`, then each meta value as a
   canonical `RemPill`.
4. **Composer:** pinned under the scroll, inset `Spacing.lg` horizontally and `Spacing.md` below; the
   background is `backgroundPrimary`.

## States

| Initializer | Composer | Rule |
|---|---|---|
| Static (`composerText`, `composerPlaceholder`, `composerState`) | `RemComposerBar(text:placeholder:state:)`; placeholder "Reply or ask Rem…" | Display only. |
| Host-driven (`composer: ChatComposerState`, `onComposerAction`) | The **same canonical composer** as Chat, `accessibilityPrefix: "task"` | Send follows `ChatComposerState.canSend`, so an empty reply can never act as a navigation doorway; any "open in Chat" route is a separate host action. The model control appears only when the host's state shows it. |

## Scope boundary

The DS renders supplied values only. Task data, comments, run state and reply routing stay in the host
(see [`chat-adapter.md`](chat-adapter.md)). The task-context reply accessory (`2682:22298`) belongs to the
Chat composition, not this template.

## Contract questions (platform differences; not rules)

1. **Heading semantics.** Neither platform marks the title as a heading (iOS `.isHeader`, Compose
   `heading()`), unlike the chat cards. Decide whether it should.
2. **Keyboard and system insets.** Neither template applies keyboard or navigation-bar insets to the
   composer itself; both rely on the host (iOS safe area of the pushed view, Compose host scaffold).
   `ChatScreen` docks its composer with explicit insets. Decide whether TaskDetailScreen should match.
3. **Meta row alignment.** Compose centres the date and pills vertically; iOS uses the HStack default
   (centre). Equivalent today; recorded so a change on one side is caught.

## Proof

- Render pair `TaskDetailScreen-light` (`RenderSnapshots.swift`, 402 × 820 / `EvidenceSnapshots.kt`).
- The host-driven composer's send rule is covered by the composer suites
  (`ChatComposerFixtureTests` / `ChatComposerFixtureTest`).
- **Gap:** no journey opens TaskDetailScreen on either platform; no dark or large-text render.

## Parity acceptance

- Title, then date + pills on one row, then host content, all left-aligned in one scroll.
- The composer is pinned below the scroll on both platforms and is the canonical `RemComposerBar`.
- No navigation bar drawn by the template.
