# Task: PlayerBar — brief voice playback bar

## Outcome
- **User outcome:** a clear mini control while a brief is being read aloud (stop / mute / retry).
- **Scope:** the `PlayerBar` component (paired with `DailyBriefCard`'s playing state).
- **Mode:** Reproduce (exists in code as `MiniPlayerBar`).
- **Source concern:** `TARGET-COMPONENTS.md §Gaps #4`.

## Design authority
- **Product authority:** `MiniPlayerBar` + the brief playback flow in `ContentView.swift`
  (`LatestBriefPlaybackController`); mic-muted leading control + trailing Stop; Retry on failure.
- **Design-system authority:** `Surface` (bar), `Button`/`ContainedIcon`, tokens.

## Approved experience
- **Composition:** leading mic (barge-in) · label/progress · trailing Stop.
- **States:** playing (muted) · chunk-transition (keeps Stop + muted) · failed → `Retry` · hidden.
- **Interaction:** Stop → hands to listening (no receipt); completion → unmute + resume listening.
- **Accessibility:** announce playback start/stop; reduced-motion for any progress animation.

## System use
- **Reuse:** Surface, Button, ContainedIcon, tokens. **Exact:** the state set + transitions from code.
- **Excluded:** the playback engine; DailyBriefCard itself (its own packet).

## Delivery contract
- **Acceptance:** master + states from code; pairs with DailyBriefCard.
- **Evidence:** iOS + Android, light + dark, per state; Preview; Code Connect.

## Approval
- **Status:** approved for Build. **Open decisions:** none.
