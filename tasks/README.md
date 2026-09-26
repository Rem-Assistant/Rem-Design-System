# Design-ready task packets — roster gaps

One packet per gap in `TARGET-COMPONENTS.md §Gaps`, written in the
[`design-ready-task`](https://github.com/samuelalake/agent-skills/blob/main/product-design-delivery/references/design-ready-task.md)
shape so they drop into the Agent Factory loop (Steward dispatches → Builder → Verify → Reviewer → Gate).
Each is **code-authoritative** (the shipping SwiftUI is the product authority) and **cross-platform**
(SwiftUI/iOS + Compose/Android from one `tokens.json`; form diverges to native idiom). Mac is out.

| Packet | Mode | Priority | Authority (code) |
|---|---|---|---|
| [DailyBriefCard](DailyBriefCard.md) | Systemize | ⭐ P0 | `Rem/Sources/Components/DailyBriefCard.swift` |
| [Skeleton](Skeleton.md) | Systemize | P1 | ~7 `*LoadingSkeleton` views |
| [ToolResultCard-variants](ToolResultCard-variants.md) | Extend | P1 | `Shared/Views/Chat/ToolResultCards/*` |
| [Menu-and-Shell](Menu-and-Shell.md) | Systemize | P1 | `ContentView.swift` menus + tab bar |
| [PlayerBar](PlayerBar.md) | Reproduce | P2 | `MiniPlayerBar` (brief playback) |
| [BrowserTakeover](BrowserTakeover.md) | Reproduce | P2 | `Shared/Views/Browser/SharedBrowserLiveView.swift` |
| [Template-Onboarding](Template-Onboarding.md) | Reproduce | P1 | `Rem/Sources/Onboarding/OnboardingFlow.swift` |
| [Template-TaskDetail](Template-TaskDetail.md) | Reproduce | P2 | `Rem/Sources/Screens/TaskEventView.swift` |
| [Template-ChatHistory](Template-ChatHistory.md) | Reproduce | P2 | `Rem/Sources/Chat/ChatHistoryView.swift` |

**Evidence standard (every packet):** master + variants + Preview tile per `component-track`; iOS +
Android screenshots in **light and dark**; Code Connect `.figma.swift`; component-ledger row; verified
against app screenshots in `FIDELITY.md`. Anatomy annotation is a manual plugin step, not part of the task.

**Founder evidence (2026-09-25):**
- Onboarding recording → `Template-Onboarding` source: https://samuelalake.com/projects/rem-evidence/rem-onboarding-web.mp4
- Gmail message card ("you already have") → evidence that `MessageDraftCard` (Figma `458:69`) exists:
  https://samuelalake.com/projects/rem-evidence/rem-work-web.mp4 — **open decision, not a packet:** it's
  net-new with no code caller today; it needs a code home (a tool-result/proposal variant) before it
  becomes a roster component. Flagged for founder, not dispatched.

_Note: `SHAPE-OF-A-TASK.md` referenced in the prior handoff is not committed to this repo on any branch;
these packets follow the `design-ready-task` reference in `agent-skills` directly._
