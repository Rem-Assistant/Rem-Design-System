# Contract — Chat / Inbox app-adapter boundary

**Outcome:** a consuming app and the Playground drive the **same** canonical Chat, task reply and Inbox
compositions through one typed boundary. The design system renders supplied values and emits typed
actions. The app owns everything that can be true or false about the world.

**Authority:** Samuel's architecture direction (2026-10-10); Figma composition board `2681:21977`, shell
`2054:21958`, header `2054:19725`, composer `2071:11555`, reply context `2682:22298`, empty `2054:22089`,
populated `2054:21981`, receipt / reaction acceptance `2659:21942` (read-only exports, 2026-10-10 15:59
UTC). Component contract: [`chat.md`](chat.md).

## Ownership

| Layer | Owns | Never owns |
|---|---|---|
| **App** | auth, conversations, tool execution, networking, persistence, business state, task store, routing, **authoritative delivery and read evidence**, voice session, attachment content | layout, presentation rules |
| **Thin adapter** (in the app) | app state → DS values; DS actions → app operations; evidence → `MessageBubble.Delivery` | storage, effects of its own |
| **Design system** | layout, full-screen composition, interaction presentation states, presentation rules (send eligibility display, latest-receipt placement) | provider SDKs, credentials, networking, business logic, receipt truth |

## Inputs (Swift / Kotlin names are identical except where noted)

| Type | Purpose |
|---|---|
| `ChatHeaderDisplay` | name, agent activity copy, status (connected / needs you), working face, which header controls are shown |
| `ChatComposerState` | draft, placeholder, model label, attachments, `ComposerAvailability` (`enabled` / `disabled(reason)`), `ComposerPhase` (`idle` / `sending` / `streaming`), `voiceAvailable`, `isFocused` |
| `ChatMessageDisplay` | id, role, text, meta, **host-supplied** `delivery`, reaction, `canRetry`, optional **host-formatted** `time` (swipe-to-reveal timestamp) |
| `ChatTranscriptEntry` | `.message` / `.timestamp` (Kotlin: `Message` / `Timestamp`) |
| `ChatReplyContext` | `targetID` (Kotlin `targetId`), title, summary — the task and conversation ids stay in the app |
| `ChatEmptyState`, `ChatStarter` | empty conversation copy and starters (host lists only what it can do) |
| `InboxItemDisplay`, `InboxItemState` | Inbox rows and their host-reported run state: none / loading / executing / needsApproval / blocked / unknown / completed |

## Actions

`ChatScreenAction`: `back`, `overflow`, `call`, `activityDetails`, `starter(id)`, `dismissReplyContext(targetID)`,
`composer(ChatComposerAction)`, `transcript(ChatTranscriptAction)`.
`ChatComposerAction`: `draftChanged`, `send`, `cancel`, `speak`, `add`, `removeAttachment(id)`, `focusChanged`.
`ChatTranscriptAction`: `retry(messageID)`, `requestReaction(messageID)`, `react(messageID, reaction?)`.
`InboxAction`: `open(itemID)`.

## Rules the DS applies (presentation only)

1. **Send** is available only when input is enabled, no turn is in flight, and there is visible text **or a
   content attachment**. A capability chip (Cloud browser) alone never makes a message.
2. **Stop** is shown while sending or streaming and emits `.cancel` — never `.send`. Cancel remains
   available when input is externally disabled mid-turn.
3. **Speak** is shown only when the host says voice is available, input is enabled and nothing is in flight;
   it emits `.speak`.
4. **Receipt placement:** only the latest outgoing message shows its Delivered / Read receipt; older outgoing
   messages show none; a Not delivered failure stays visible wherever it is.
5. **Header:** one avatar and one identity/activity capsule; the header owns back and one trailing action:
   overflow, or — when the host sets `showsCall` because in-app voice is available — the call entry, which
   **replaces** overflow (never a second icon beside it; in-app voice only, no PSTN). A host that shows the call entry keeps every action it had in overflow reachable through an existing surface it already owns (for example its agent settings or the activity screen). The DS adds no new menu for them. The platform
   navigation bar is hidden by the composition. The empty state has no second face.
6. **Reply context** is an accessory above the same composer. Dismiss reports the target id only.

## Rules the adapter applies (truth)

- `.delivered(at:)` only with durable **host acceptance** evidence; `at` is the host-formatted acceptance time.
- `.read(at:)` only with an **explicit acknowledgement**, keeping the delivered time. Never because a reply appeared.
- `.failed` only when the host reports the send failed; `canRetry` only when the host can actually retry.
- `time` is the host's formatted send or receive time for that message (for example "10:24"). The DS never reads a clock or formats dates; `nil` shows no timestamp. A left swipe on `ChatTranscriptList` reveals these times in a right-side column for incoming and outgoing messages alike, and every row moves together while the header and composer stay fixed. Each time is centred on its own bubble, not on the bubble plus its receipt. The receipt stays below the latest outgoing bubble. The swipe is a temporary peek: on release the rows snap back. Assistive technology hears "Sent at …" or "Received at …" without the gesture. The 64pt column width is provisional, to be validated against long localized times and large text sizes; it is not a specified value. Thresholds and timing follow platform conventions. Snapback is covered by the reveal tests and journeys, not by screenshots.
- Otherwise `.none` — no receipt.
- `ComposerAvailability.disabled`, `voiceAvailable`, attachment content and `InboxItemState` come from runtime
  state; a fixture or mock must not imply capability.

The reference adapter `MockChatAdapter` in `ChatAdapterContractTests.swift` / `ChatAdapterContractTest.kt`
(test targets only) proves these mappings; it is not shipped.

## Fixtures (presentation-only)

`ChatComposerFixture`, `ChatPlaygroundFixture` and `InboxPlaygroundFixture` drive the Playground exactly as an
adapter would. A message sent in a fixture carries **no receipt**. Delivered / Read / Not delivered appear only
through the explicitly named `simulateHost…` controls (Playground: header overflow → *Fixture host*), at the
illustrative `fixtureTime` "10:24". They prove composition and interaction, never runtime behaviour.

## Not claimed

No native compilation, render or journey result is claimed from the cloud session that produced this slice; see
the delivery manifest. Actual-app adoption, runtime receipts, voice, attachments and task routing are verified
only in the app against its runtime.
