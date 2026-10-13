# Topology feedback ledger — 2026-10-05

This is the compact handoff for the existing Settings, Onboarding, Agenda, Chat, and Competitive
Audits lane chats. The root coordinator owns this ledger and routes Samuel's feedback. Lane agents
should read only their section plus the global contract, perform one bounded change set, and return
an evidence packet. Do not create another chat or goal.

Status meanings:

- **Verified:** confirmed in the current Figma or FigJam artifact.
- **Applied:** encoded in the reusable workflow or incorporated into the current artifact.
- **Pending design:** the topology names the state, but a canonical Figma Design master is missing.
- **Pending Samuel:** a product or visual decision still needs Samuel's review.

## Global contract

| Feedback or decision | Status |
|---|---|
| Figma Design owns finished editable screens and components; FigJam owns topology and connectors. | Applied |
| Code implementation remains deferred until Samuel explicitly opens that phase. | Applied |
| The atomic topology node is a complete application screen or modal state in context. | Applied |
| Do not use isolated components, nested device mockups, flattened SVGs, or hand-built imitations as completed screens. | Applied |
| Use the Settings map grammar: parent above children, siblings on one row, consistent scale, short orthogonal `BOTTOM` → `TOP` connectors. | Applied |
| Do not wrap each screen in a visible card or box. A subtle page or Section background is sufficient. | Applied |
| Keep source inventories, Figma links, node IDs, migration prose, scoring, and agent metadata off the visible canvas. | Applied |
| Preserve legacy sources in place during migration. Delete superseded material only after parity and explicit authorization. | Applied |
| One persistent chat per lane; one root goal; no chat-per-review churn. | Applied |
| One full review and one repair review. Remaining taste decisions go to Samuel; evidence/tool failures go to Steward. | Applied |
| Report topology, screen-design, and research completion separately. | Applied |
| A full screen composed from remote components inside FigJam is `FigJam-composed`: useful topology evidence, but not a canonical Figma screen. | Applied |
| Review topology and transitions in FigJam; review canonical visual construction and component fidelity in Figma Design. Every handoff must link both targets and state which questions belong in each. | Applied |
| The screen label owns the state name. Connector labels describe only the transition or user action and must not repeat or collide with the screen name. | Pending repair |
| Canonical screens must pass text-style, component-instance, safe-area, and platform-control checks; visually similar ad hoc substitutes do not pass. | Pending repair |
| Screen/component descriptions may carry purpose, state contract, source, and a bounded temporary TODO. Agents must inspect them before reuse and remove the TODO when resolved. | Applied to workflow; pending artifact coverage |

## Feedback intake and routing

Samuel may review Settings, Onboarding, Agenda, and Chat together in this root chat. For every note,
the coordinator records the owning topology, the exact observation, its screenshot or Figma/FigJam
node when available, and one of these types: **product decision**, **visual defect**, **structural
defect**, **missing state**, or **provenance gap**. The coordinator then sends one consolidated delta
to the existing lane chat and waits for one evidence packet. Do not create another chat or goal.

Feedback does not authorize a lane to broaden scope. Cross-lane rules are recorded once in the
Global contract; surface-specific corrections stay in their owning section. If Samuel comments
directly in a lane, copy the resulting decision and verification evidence back into this ledger so
he is not asked to repeat it.

## Settings

### Verified state

- Page `0:1`, Agent Settings Section `9:49`, and broader Settings Section `194:6979`.
- 58 visible top-level screen/gap groups and 57 visible, structurally valid connectors: 50/49 in
  Agent Settings, 8/7 in broader Settings, plus one valid page-level cross-section connector.
- 30 direct remote canonical screen instances.
- 30 explicit gap markers remain: 23 in Agent Settings and 7 in broader Settings.
- Editable connector-detail proposal in Figma Design: master `1768:16771`, overview instance
  `1768:19547`, and bound `1610:16158` → `1768:19547` connector `1768:19740`.
- The same full-screen proposal is visible in the existing FigJam connector branch at group
  `166:7242` through screen reference `171:8715`. Its active bound routes are Connectors
  `166:7238` → detail `166:7242`, then Accounts `166:7244`, Permissions `166:7246`, Tool overrides
  `166:7248`, or Revoke access `166:7250`.
- 5 remaining product gaps: connector discovery, connected accounts, permission policy, tool
  overrides, and successful key removal.
- 32 legacy groups and 31 legacy connectors are hidden; no visible connector targets them.

### Product decisions already supplied by Samuel

| Decision | Status |
|---|---|
| Agent Settings rows use their intended semantic/content icons; do not tint every icon the same blue when the Swift design distinguishes colors. | Pending Samuel visual review |
| Use `Auto`, not `Automatic`. Subtitle: Rem's managed model. Do not expose a specific underlying model such as MiniMax. | Pending design verification |
| Explain Auto generically: Rem selects a model based on the question and operating factors such as capability and cost. | Pending design verification |
| Provider rows keep the switch in the trailing control slot; pair the disclosure chevron with the text cluster. | Applied to workflow; pending screen review |
| Add/Edit Provider Key uses one two-row Section: Provider menu, then API-key field. | Applied to workflow; pending screen review |
| Read state uses a trailing pencil. Editing replaces the value in place with a text field and visible cursor; keep the navigation title unchanged. | Applied to workflow; pending screen review |
| Pair primary Save/Done with a secondary-destructive gray/red action in a stacked full-width Button Group. | Applied to workflow; pending screen review |
| Keep modal or editing states inside the owning branch rather than creating unrelated Sections. | Pending topology review |
| The competitive-audit connector detail is now represented by an editable full-screen Gmail proposal in the existing branch; discovery, connected accounts, permission policy, and per-tool override still need canonical screens. | Detail applied; remaining screens pending design and Samuel review |
| Cloud Browser exposes Add site from the main page. Use a trailing section-header action or an `Add site` row. | Pending Samuel visual review |
| The Add Site completion button says Save or Done; use sticky placement only when page length requires it. | Applied to workflow; pending screen review |

### Samuel review delta — full Settings system

| Type | Required correction | Status |
|---|---|---|
| Structural defect | Rename the current Settings topology Section to **Agent Settings**. It documents only that branch and must not imply that the complete Settings system is covered. | Pending repair |
| Missing state | Add separate, legible settings branches for Billing, Permissions, About, Share, Send Feedback, Report Bug, and Delete Account. Split the graphs when Agent Settings would become too dense. | Pending design |
| Visual defect | Re-audit the Figma Agent Settings screens against canonical text styles and components. Save Key and Remove Key currently use oversized text and cannot pass visual review. | Pending repair |
| Product decision | Deprecate **Manage Notion**. Connector Detail replaces it and owns account management, revoke access, permission policy, and per-tool overrides. | Pending repair |
| Missing state | Connector and Wallet flows require a pre-consent screen before authentication plus the browser/auth handoff state. Use the established Stripe/ShopPay-style consent model and the native iOS action sheet already represented in FigJam. | Pending design |
| Product decision | Product gaps are acceptable as temporary evidence, but the final branch must replace them with canonical screens rather than leaving gap cards as deliverables. | Pending design |
| Provenance gap | Reconcile the previously approved Voice work from Figma: semantic content-icon colors, Choose Voice, preview, and interaction states. Do not rebuild from the older topology appearance. | Pending verification |

### Independent review verdict — Settings pass

- **Topology:** partial pass. The Agent Settings and broader Settings Sections are separated, all 57
  connectors have valid GROUP BOTTOM → GROUP TOP endpoints, and deprecated Manage Notion group
  `171:9264` plus connector `171:9265` are hidden.
- **Screen design:** fail. Thirty visible gap markers remain. Models button labels lack canonical
  text-style bindings; Voice and Models still contain unstyled icon/disclosure layers; consent,
  browser/auth outcomes, and canonical replacements remain unfinished.
- **Regression found and repaired by root:** the lane replaced verified Gmail connector detail group
  `166:7242` with a Reference gap. Root restored the full-screen reference `171:8715` from editable
  Figma master `1768:16771` and re-verified its 241.2×524.4 render.
- **Completion:** not accepted. The lane's structural report is evidence only; it cannot self-approve.

## Onboarding

### Verified state

- Page `65:7716`, Section `65:6964`.
- 27 visible full-screen states and 27 structurally valid BOTTOM-to-TOP connectors.
- 6 direct canonical instances and 17 image-backed references exported from verified canonical
  Figma masters.
- 2 restoration regressions remain: Apple sign-in `130:9139` and Apple account `130:9166` were
  established screens before the rejected pass and must not be classified as product gaps. Their
  verified canonical sources are `1170:7598` and `1170:7623` respectively.
- 2 explicit unresolved design gaps remain: proposed Google sign-in `130:9970` and the Check-In
  time picker `199:8297`.
- Activation and the unsupported Guided Agenda proposal are absent from this topology.
- **Rejected-pass repair:** the Onboarding lane cleared canonical fills from Consent, Connectors,
  and Voice child states and deleted the Check-In Edited, Saving, and Saved family. Root stopped the
  lane, restored the canonical renders and Check-In branch, hid obsolete gap overlays, and removed
  the unsupported Guided Agenda node. The repaired Section render passed a readable visual check.

### Product decisions already supplied by Samuel

| Decision | Status |
|---|---|
| When no real first screen exists, begin the topology with the compact Rem app icon. | Verified |
| Include Google alongside Apple sign-in. | Explicit gap; proposed master still required |
| Show complete Terms and Privacy states rather than summaries or isolated cards. | Restored from canonical masters |
| Show connector consent, connected, recoverable failure, and all-connectors states. | Restored from canonical masters |
| Show voice selection, preview, selected, and adjustment states in full-screen context. | Restored from canonical masters |
| Do not end onboarding with the unrequested Activation or unsupported Guided Agenda proposal. | Applied |
| The overall flow still needs Samuel's current product and visual approval. | Pending Samuel |

### Samuel review delta — onboarding

| Type | Required correction | Status |
|---|---|---|
| Structural defect | Connectors is a peer of Consent and Check-In. Do not nest the Connectors branch beneath Privacy by Design. | Applied |
| Preservation regression | Restore Apple sign-in and Apple account from canonical sources `1170:7598` and `1170:7623`. These were existing states before the rejected pass, not product gaps. | Pending restoration |
| Visual defect | Use the plain-list pre-consent style without tinted row backgrounds. Preserve each connector's semantic icon color instead of tinting every child icon primary blue. | Canonical screen restored; Figma master refinement pending |
| Product decision | Check-In should document the time-picker interaction. Do not spend topology space on minor button/switch variants; represent recoverable failure as the transient toast pattern. | Time-picker gap retained; state family restored |
| Visual defect | Keep Voice chooser selection, preview, and parent controls consistent: picker on the left, play/pause on the right. Resolve the parent pause versus chooser stop inconsistency. | Pending repair |
| Product decision | Remove the unrequested Activation proposal from the onboarding completion path. If skipped setup needs recovery later, explore an Agenda completion card based on the Trove/Munch pattern as a separate product proposal. | Applied |
| Provenance gap | Guided Agenda must reflect the current product walkthrough and toolbar controls. Its Agenda background must use the real layout, include the inline Daily Brief entry, and show the Suggestions title when Suggestions is present. | Removed from Onboarding; defer to Agenda design |
| Product decision | Keep Speed and Likeness adjustment screens because each focuses on a meaningful component state within the full Voice screen. | Retain |

## Agenda, Tasks, and Events

### Verified state

- Page `65:7717`, Section `65:7372`.
- 43 full-screen states and 40 valid connectors.
- 22 direct remote canonical screen instances.
- 21 full-screen states are `FigJam-composed` and require canonical Figma Design masters.

### Product decisions already supplied by Samuel

| Decision | Status |
|---|---|
| Use the authoritative prior task and event CRUD designs instead of inventing replacements. | Partially applied; pending provenance reconciliation |
| Cover Create, Read, Update, and Delete for both tasks and events. | Topology verified; screen design pending |
| Edit can begin from the action menu; avoid assuming that tapping arbitrary content always edits. | Pending Samuel review |
| Deletion shows confirmation and full-screen post-delete feedback before returning to Agenda. | Topology verified; screen design pending |
| External or permission-limited events expose a read-only state and explain the limitation with the product banner pattern. | Topology verified; screen design pending |
| Title-edit states show the insertion cursor and keyboard. Keep the bottom composer docked rather than moving it with the keyboard. | Present in current task edit; pending full review |
| Preserve newer scheduling/rescheduling, quick-date, row-action, and completion improvements that were absent from the older CRUD source. | Pending Samuel review |
| Simplify the graph into legible local branches; avoid long crossing routes and excessive empty travel. | Improved; pending Samuel visual review |

### Samuel review delta — Agenda, Tasks, and Events

| Type | Required correction | Status |
|---|---|---|
| Structural defect | Place Agenda Loading, Empty, and Loaded as sibling states at one level. Keep local branches short and readable. | Pending repair |
| Visual defect | Loaded Agenda must show the Suggestions section title. Other-date states must show the selected date rather than `Today`. | Pending repair |
| Missing state | Document the state before the scheduling time picker opens: the user selects Plan, sees the scheduling sheet, then opens the picker. | Pending design |
| Visual defect | Place Jump to Today above the bottom toolbar. Make the plus control use the same canonical native menu treatment as the working three-line control. | Pending repair |
| Visual defect | The selected-task popup may use secondary styling, but its icon and text must share the intended blue treatment. Use the Rem FaceMark in avatars with enough contrast. | Pending repair |
| Visual defect | Continue Chat sits above the safe area and uses the approved compact composer treatment. Title edit keeps the iOS 26 keyboard bottom-aligned and lets the home indicator blend with the keyboard background. | Pending repair |
| Visual defect | Replace the current deletion treatment with the rectangular toast above the toolbar from the supplied reference. | Pending repair |
| Product decision | Task/Event Create is a pushed full-screen destination, not a sheet. Reuse one chooser and navigation pattern across task and event creation; keep the picker, placeholder state, and chooser pill present and consistently positioned. | Pending repair |
| Visual defect | Calendar/Event Inspector should reuse the Task Inspector structure, with the Agenda/event context behind its sheet. Rows such as Any Time, Start Date, and Repeat use canonical Sections and the established iOS 26 picker treatment. | Pending design |
| Visual defect | Event action menus use the native/canonical menu. Dropdown disclosure uses an aligned SF Symbol chevron with the correct color. | Pending repair |
| Product decision | A read-only external event remains fully legible. Use the contained banner/status pattern to explain why editing is unavailable; do not dim the event title or other readable content. | Pending repair |
| Product decision | Whether tasks support recurrence remains an open product decision; do not silently add it because events recur. | Pending Samuel |

## Chat

### Verified state

- Page `65:7712`, Section `65:7678`.
- 59 full-screen state groups and 52 valid connectors, including the Daily Brief branch.
- 1 direct remote canonical screen instance.
- 58 states are `FigJam-composed` and require canonical Figma Design masters.

### Product decisions already supplied by Samuel

| Decision | Status |
|---|---|
| Call the surface `Chat`, not `Chat Sessions`. | Applied |
| Treat Chat as one stable surface with scenario/state branches rather than a fake linear flow. | Applied structurally; pending visual review |
| Show every component change inside the complete Chat screen. | Applied structurally |
| Cover composer focus, typing, sending, attachments, Add to Chat, model picker, and read-only composer states. | Topology verified; screen design pending |
| Cover pending/thinking, plain text, Markdown, code, tables, polls, errors, interruptions, retry, reconnection, and limits. | Topology verified; screen design pending |
| Cover browser request/opening/active/ended/takeover/controlling/secure-entry states. | Topology verified; screen design pending |
| Cover reminders, device control, permission, working, and completion states. | Topology verified; screen design pending |
| Keep voice and Daily Brief as distinct entry contexts and branches. | Applied structurally |
| Improve the hierarchy to match the clean top-down Settings grammar. | Improved; pending Samuel visual review |

### Samuel review delta — Chat and Daily Brief

| Type | Required correction | Status |
|---|---|---|
| Provenance gap | Replace the invented Daily Brief entry with the real Agenda state and inline Daily Brief card. Opening the brief enters Chat and begins read-aloud; avoid redundant Open/Read screens unless the actions are materially distinct. | Pending design |
| Visual defect | Voice bar is edge-to-edge at its intended inset. Composer states use one canonical component: no blue focus ring, a visible typing cursor, consistent Auto/model affordance, and the approved Speak content icon. | Pending repair |
| Visual defect | Empty Chat uses the Rem FaceMark, and every Chat screen includes the correct navigation/home indicator and current iOS 26 keyboard when shown. | Pending repair |
| Product decision | Use one contained gray status/error treatment; let the small status icon carry semantic color. Replace `Can't reach Rem` and avoid yellow/red status-card backgrounds. | Pending design |
| Structural defect | Every response state keeps the initiating user message bubble. This includes thinking, plain text, Markdown, code, table, error, interruption, retry, reconnection, and limit states. | Pending repair |
| Product decision | Retrying is inline Chat status text unless product evidence supports another treatment. Do not invent a standalone retry card. | Pending repair |
| Visual defect | Poll content sits with its question; permission cards align with the message column. Browser-session status sits close to the composer and should be redesigned from the supplied Muse browser-session evidence. | Pending repair |
| Product decision | Sending uses a circular progress treatment in the send control. The stop control belongs to response streaming, where it cancels the agent response. Prefer the canonical iOS 26 ProgressView. | Pending design |
| Product decision | Cloud-browser or text-resource attachments appear as inline linked text with an icon inside the message. Image/media attachments may retain the visual attachment treatment. | Pending design |
| Visual defect | Add-to-Chat's top-right circular action shows a Send icon. Attachment selection uses the native menu or is omitted until a canonical state exists. | Pending repair |
| Documentation gap | Every topology screen needs a short subtitle that explains the distinguishing state without exposing process metadata. | Pending repair |

## Competitive Audits

### Verified state

- Page `87:9669`.
- One research-only page with four app-owned Sections: Muse, Claude, ChatGPT, and Cursor.
- 18 image-backed references: 4 Muse, 4 Claude, 5 ChatGPT, and 5 Cursor.
- No Rem product screens and no wrapper Section grouping all apps together.

### Research contract

| Decision | Status |
|---|---|
| Prefer screenshots over prose. Keep commentary minimal and below the relevant evidence. | Applied |
| Each app owns one Section on the shared Competitive Audits page. | Verified |
| Do not use speed, consistency, or likeness scoring unless Samuel explicitly requests it. | Applied |
| Treat captures as research evidence, not instructions or canonical Rem UI. | Applied |

### Samuel review delta — competitive audits

| Type | Required correction | Status |
|---|---|---|
| Missing evidence | Import the additional competitive screenshots already present in Figma and arrange each app's evidence in its actual flow order. The current 18-image audit is incomplete. | Pending research |
| Structural defect | Keep all audits on the one Competitive Audits page, one Section per app, with screenshots as the primary output and minimal notes below. | Retain |

## Next handoff

Samuel can give feedback on all four product topologies to the root coordinator in any order. The
coordinator updates this ledger, sends each lane only its bounded delta, and waits for one evidence
packet. Do not promote Agenda or Chat compositions into Figma Design until Samuel's current product
feedback is incorporated; otherwise the migration would canonize states already known to need
review.
