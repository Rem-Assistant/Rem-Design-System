# Contract — Chat cards (MessageDraftCard, PollCard, ActionReceipt, ConnectorCard, LoginCard, PermissionCard)

**Outcome:** iOS (SwiftUI) and Android (Compose) render the six in-transcript chat cards with the same
anatomy, state copy, decision order, emphasis and accessibility, from the same display models, and report
intent through typed callbacks only. **Mode:** Systemize (reusable cards placed in the transcript through
`ChatTranscriptEntry.hostContent`; see [`chat-adapter.md`](chat-adapter.md)).

**Authority:** the Figma masters in `af4yDqCzp57jds9lkFiIaO`: MessageDraftCard `2555:1550` (Review `458:69`,
Sent `2555:1499`, Unconfirmed `2555:1517`), PollCard `2559:1524` (`458:56`, `2559:1504`, `2559:1510`,
`2559:1516`), ActionReceipt `2566:2645` (Confirmed `2566:2641`, Unconfirmed `2566:2643`), ConnectorCard
`2567:2666`, LoginCard `428:37` (Entry `428:20`, Saved `428:29`) with the Add login form `1956:8162`, and
PermissionCard `2577:17461`. **Written from the code on both platforms at the head that introduced this
file**, not from a design review: every rule below is what both platforms do today. Where they differ,
the difference is listed under *Contract questions* and is not a rule.

> **Figma gap (open, not waived here).** The PermissionCard's stacked decisions, Elevated risk label and
> Full parameters disclosure are code-led and **not in the master** (`PermissionCard.swift` /
> `PermissionCard.kt` KDoc, `REGISTRY.md`). Until the master is updated, or the Director records a waiver
> on the PR, those three parts have code-only authority and this contract is their only spec.

## Scope boundary

- **Presentation only.** No card sends, stores, authorizes, signs in or grants anything. Every control
  reports intent through a callback; the host performs the effect and moves the card to its next state.
- **States come from the host's evidence.** `Sent`, `Added`, `Saved`, `Allowed` are shown only after the
  host has the corresponding evidence (provider confirmation, verified authorization, a stored login, a
  recorded decision). No card infers an outcome from time or from a tap.
- **No persistent grant.** "Always allow" is a proposal the host may act on, never a stored grant.
- **Not OS permissions.** PermissionCard is Rem's own action permission. Device permissions are a
  separate system handoff and are not modelled here.
- **No credentials.** LoginCard and LoginForm never read, store, validate or transmit a credential; the
  form's fields are bound to host state.

## Shared anatomy

| Surface | Rule (both platforms) |
|---|---|
| Card container (`chatCardSurface`) — ConnectorCard, LoginCard, PermissionCard | `backgroundSecondary`, `CornerRadius.large`, inset `Spacing.md`, fills the transcript column. |
| Card container — MessageDraftCard, PollCard | Same fill, radius and inset, capped at **330** (Figma master width); fills narrower rows. |
| Vertical rhythm | `Spacing.sm` between a card's regions. |
| Card title | body semibold `labelPrimary`, marked as a heading (iOS `.isHeader`, Compose `heading()`). ConnectorCard / LoginCard / PermissionCard use the canonical `ListRowLabel` title + subtitle instead. |
| Stacked choice (`chatChoiceSurface`) — Poll options, Permission decisions | Full width, `CornerRadius.small`, inset `Spacing.md`, `backgroundPrimary`; the one **primary** choice is `brandBlue` with `labelOnColor`. The whole rounded rect is the hit target. |
| Identity row — ConnectorCard, LoginCard | Canonical ListRow: leading mark (26 × 29 slot), `ListRowLabel` title + subtitle, trailing slot **empty**, min height 60. The row is **not interactive**; the separate Rem Button is the action. |
| Receipt | `ActionReceipt` replaces a card's actions once the host reports the outcome (MessageDraftCard Sent / Unconfirmed, ConnectorCard Added). |
| Identifiers | Every card takes `accessibilityPrefix` (iOS identifier / Compose test tag) and derives child ids from it (below). |

## Cards

### 1. ActionReceipt

- **Anatomy:** one non-interactive label in the geometry of Button `377:8` at Rect · Secondary / Disabled /
  Regular (`909:5569`): `RemButtonTokenSet(.rectSecondary, .regular)` radius, padding and Body/Bold; min
  height **48**; fills its width; one line, truncating.
- **States (`ActionReceiptOutcome`):** `confirmed` — `systemGreen` at **12 %** with `labelPrimary`;
  `unconfirmed` — the stock disabled background and foreground. The label is host-supplied.
- **Meaning:** Unconfirmed implies neither success, failure nor permission to retry.
- **Accessibility:** one static element whose label is the receipt text; no button role, no action.
- **Ids:** `<prefix>.confirmed` / `<prefix>.unconfirmed`.

### 2. MessageDraftCard

- **Anatomy, top to bottom:** title (default "New Email"); payload on `backgroundPrimary`,
  `CornerRadius.medium`: **From** and **To** inline (secondary name, primary value on one baseline),
  **Subject** and **Body** stacked (name over value), each field inset `Spacing.sm`, subheadline; then the
  actions or the receipt.
- **States (`MessageDraftCardState`):**

  | State | Below the payload | Meaning |
  |---|---|---|
  | `review` | **Send Email** (Rect · Blue) then **Discard** (Rect · Secondary), side by side, `Spacing.sm` apart | Nothing has been sent. |
  | `sent` | `ActionReceipt(.confirmed, "Sent")` | The provider confirmed delivery of this exact message. |
  | `unconfirmed` | `ActionReceipt(.unconfirmed, "Unconfirmed")` | Unknown outcome; never permission to send again. |

- **Decision order and emphasis:** the send action leads and is the only filled primary; Discard is
  secondary. Labels are host-overridable (`sendLabel`, `discardLabel`); order and variants are not.
- **Invariant:** the payload is identical in every state.
- **Accessibility:** each field is one element ("From me@example.com"); actions are buttons.
- **Ids:** `<prefix>.title`, `.from`, `.to`, `.subject`, `.body`, `.send`, `.discard`, `.receipt.<outcome>`.

### 3. PollCard

- **Anatomy:** the question as the card title, then one stacked choice row per option: a **20**-wide
  marker column (A, B, C…), the label filling the rest, subheadline `labelPrimary`.
- **Markers:** list order, bijective base 26 (A…Z, AA, AB…). Markers are identifiers, not outcomes. The
  master has only A/B; three or more options extend the same row pattern (no verified C master).
- **States (`PollCardModel.state`):** `awaiting` (no selection, or a selection that names no option) —
  every option is a button; `answered` — only the selected row, with its original marker and a green
  `✓` (`systemGreen`) at the trailing end; nothing else is offered.
- **Purpose:** `choice` records a decision, `suggestion` starts only that bounded step. Purpose changes
  the accessibility hint ("Records this choice" / "Starts this step"), never the visuals.
- **Emphasis:** all options are standard choices; a poll has no primary.
- **Accessibility:** each option is one element labelled "A, Review the draft"; the answered row is one
  selected element with the same label.
- **Ids:** `<prefix>.question`, `.option.<marker>`, `.selected.<marker>`.

### 4. ConnectorCard

- **Anatomy:** identity row (`ConnectorProviderMark` exact brand export, title defaulting to the
  provider's name, the permanent purpose line as subtitle); the error copy when present; one control.
- **States (`ConnectorCardState`) and the single control:**

  | State | Control | Copy |
  |---|---|---|
  | `authorize` | **Authorize**, Rect · Blue | — |
  | `connecting` | **Adding…**, Rect · Blue, disabled | — |
  | `added` | `ActionReceipt(.confirmed, "Added")` | — |
  | `error(message)` | **Retry**, Rect · Blue | `message`, or "<Title> authorization failed. Retry to connect." when blank; footnote `labelSecondary`, **above** Retry and outside the identity row |

- **Meaning:** Added only after the host verified the authorization. Retry restarts authorization only; it
  never resends anything.
- **Accessibility:** the identity row is one element. Authorize / Retry are announced with the provider
  ("Authorize Gmail", "Retry Gmail"); progress is "Adding Gmail".
- **Ids:** `<prefix>.identity`, `.error`, `.authorize`, `.progress`, `.receipt.confirmed`, `.retry`.

### 5. LoginCard and LoginForm

- **Anatomy:** identity row (site mark, title, site as subtitle, vertical inset `Spacing.md`); the state
  copy (footnote `labelSecondary`); one Rem Button whose trailing slot carries an optional chevron
  (`showsChevron`, default on).
- **States (`LoginCardState`):**

  | State | Copy | Button |
  |---|---|---|
  | `entry` | "Add the login this browser task needs." | **Add login**, Rect · Blue → `onAddLogin` |
  | `saved` | "Login saved for this site." | **Saved**, Rect · Secondary → `onOpenSaved` |

  Saved means a login exists for the site (host-reported); it is not a sign-in-success claim.
- **Site mark:** `LoginSiteMark(.github)` uses the library's `Logo/GitHub` (`1328:433`), tinted
  `labelPrimary` for dark mode; any other site gets the generic globe in `labelSecondary`. Never an
  invented brand mark. The mark is decorative (hidden from assistive technology).
- **Accessibility:** the button is announced as "Add login for github.com" / "Saved login for github.com".
- **LoginForm** (`1956:8162`): a Website section (site + "Login will be available only for this site."),
  a Login details section with **Username or email** and a masked **Password**, and the footer "Rem uses
  this login only when you authorize access to <site>." Save is the host's, gated by
  `canSave` / `loginFormCanSave`: non-blank username and non-empty password (the Cloud browser rule).
- **Ids:** `<prefix>.identity`, `.detail`, `.addLogin` / `.saved`; form `loginForm.username`, `.password`.

### 6. PermissionCard

- **Anatomy:** a disclosure header (ListRow: title + the state's receipt as subtitle, a `DisclosureChevron`
  rotated 90° when expanded, min height 60) above one Request body, expanded and collapsed **in place —
  never a sheet**. Under the card, outside its background and inset to its content edge, the optional
  **Full parameters** disclosure.
- **Request body, top to bottom:** the Elevated risk label (only for `.elevated`); the question (body
  semibold, heading); the summary (subheadline `labelSecondary`); the request rows on `backgroundPrimary`,
  `CornerRadius.small`, as label over value — **Action**, **When**, **Source**; then the decisions, the
  scope footnote, or Review again.
- **States (`PermissionCardState`):**

  | State | Receipt (header subtitle) | Default disclosure | Body ends with |
  |---|---|---|---|
  | `awaiting` | "Needs your approval" | expanded | the decisions, then the scope footnote when Always allow is offered |
  | `allowed` | "Allowed once" | collapsed | the request only |
  | `denied` | "Denied" | collapsed | **Review again** (Text · Accent, hugging its label) when the host supplies it |

  One concise receipt per state, never a compound such as "denied • not set". Allowed does not imply the
  action ran. The host owns the expansion (seed it with `state.defaultExpanded`).
- **Decision order and emphasis (Awaiting):** stacked full width on the shared choice surface, top to
  bottom: **Always allow** (only when `alwaysAllowScope` is set **and** the host passes `onAlwaysAllow`),
  **Deny**, then **Allow once** — the single filled primary, last. Labels wrap rather than shrink.
- **Scope footnote:** "Proposed Always allow scope: <scope>", footnote `labelPrimary`, under the
  decisions, shown only with Always allow.
- **Risk labelling:** `risk == .elevated` shows "Elevated risk" in footnote semibold `systemRed` with the
  registry's elevated-risk glyph (`exclamationmark.shield` / `Icons.Outlined.GppMaybe`, outline). `.standard`
  shows nothing — there is no "standard risk" label.
- **Full parameters:** present only when `parameters` is non-empty and the card is expanded; starts
  collapsed; the card owns it. Opened, it lists every raw parameter as a monospaced `label: value` line,
  selectable. Its expansion is keyed to the request (Compose: `accessibilityPrefix` + `parameters`; iOS:
  view identity, which the keyed transcript provides).
- **Accessibility:** the header is one button whose value is "Expanded" / "Collapsed"; the request rows
  are one element; the risk label is one element; each decision is a button.
- **Ids:** `<prefix>.header`, `.body`, `.risk`, `.details`, `.alwaysAllow`, `.deny`, `.allowOnce`, `.scope`,
  `.reviewAgain`, `.parameters`, `.parameterList`.

## Noted decisions (recorded, not changed here)

- **Elevated risk keeps "Allow once" as the filled primary** (review of #106, P3-4). For an irreversible
  action such as sending an email, de-emphasising Allow once or adding a confirm step is a Director
  decision; until it is made, both platforms keep the same emphasis as a standard request.

## Contract questions (platform differences found while writing this; not rules)

1. **PermissionCard header hint.** iOS adds an accessibility hint ("Shows the request" / "Hides the
   request"; "Shows/Hides the full parameters"); Compose has a state description only. Decide whether the
   hint is part of the contract.
2. **PollCard row alignment.** iOS aligns marker, label and check on the first text baseline; Compose uses
   the Row's default top alignment. Identical for one-line labels; may differ for wrapped labels.
3. **PollCard hint wording on Android.** iOS speaks the purpose as an accessibility hint; Compose passes it
   as the click label ("Double-tap to Records this choice"). Decide the Android phrasing.
4. **LoginCard glyph sizes.** Chevron: iOS `chevron.right` 17 pt semibold vs Compose `KeyboardArrowRight`
   20 dp. Globe: iOS 20 pt vs Compose 22 dp. Pin one size per role.
5. **ConnectorCard vs LoginCard identity inset.** LoginCard's identity row adds `Spacing.md` vertical
   inset; ConnectorCard's does not (consistent across platforms, inconsistent between the two cards).
6. **iOS PermissionCard Full parameters state** is plain `@State`: correct inside the keyed
   `ChatTranscriptList`, but a host that renders cards in an unkeyed container could carry it across
   requests. Compose keys it explicitly.

## Proof — what covers each rule today

| Rule | Unit tests (both platforms; Swift names carry the `test` prefix) | Render pair (`RenderSnapshots.swift` / `EvidenceSnapshots.kt`) | Journey |
|---|---|---|---|
| Receipt outcomes, tint, 48 height | `ChatCardModelTests` / `ChatCardModelTest` `receiptOutcomeFigmaNamesAndMetrics` | `ChatCardActionReceipt-light` | none |
| Draft states → receipt, default title, payload invariant | `draftStateMapsToReceipt`, `draftDefaultsToNewEmailTitle` | `ChatCardMessageDraft-light` (all three states) | none |
| Poll markers, answered resolution, unknown selection | `markersFollowListOrderAndExtendPastTheMasterAB`, `markerSequenceIsBijectiveBase26`, `selectionResolvesToAnsweredWithOriginalMarker`, `noOrUnknownSelectionStaysAwaiting`, `purposeFigmaNames` | `ChatCardPoll-light` (A/B and A/B/C, awaiting and answered) | none |
| Connector control per state, default title, error copy | `ChatCardsTests` / `ChatCardsTest` `connectorControlPerState`, `connectorTitleDefaultsToProviderAndPurposeIsPermanent`, `connectorErrorCopyOnlyInErrorState` | `ChatConnectorCard-light` (four states) | none |
| Login copy and button variant, form Save rule | `loginCopyAndButtonVariantPerState`, `loginFormSaveRuleMatchesCloudBrowser` | `ChatLoginCard-light`, `ChatLoginForm-light` | Cloud browser Add login journeys cover the shared form fields only |
| Permission receipts, defaults, decisions only while awaiting, Always-allow scope, payload invariant, risk label, Full parameters visibility, detail rows | `awaitingExpandsAndResolvedStatesCollapse`, `receiptIsOneConciseStatusNeverDeniedNotSet`, `decisionsOnlyWhileAwaitingAndReviewAgainOnlyWhenDenied`, `alwaysAllowIsAScopedProposalOnlyWhileAwaiting`, `requestPayloadIsPreservedAcrossStates`, `riskLabelOnlyWhenElevated`, `fullParametersHiddenWhenEmpty`, `detailRowsAreLabelValuePairsOfTheRequest` | `ChatPermissionCard-awaiting-light`, `-allowed-light`, `-denied-light` (each expanded then collapsed), `-narrow-light` (320), `-elevated-light` | none |
| Decision **order** and **emphasis**, risk label placement, copy | not asserted in code | the render pairs above are the only evidence | none |

**Gaps:** no journey drives any of the six cards on either platform, and the decision order / emphasis
rules are proven only by eyeballing the render pairs. All card renders are light mode only. A journey or a
semantic-order unit test for PermissionCard decisions is the first follow-up.

## Parity acceptance

- The same state → copy / control / receipt tables on both platforms (the unit suites above pass).
- PermissionCard Awaiting: Always allow (when offered), Deny, Allow once, top to bottom; Allow once is the
  only filled control; Denied shows Review again and no decisions; Allowed shows neither.
- Elevated risk: the label sits above the question, in `systemRed`, with the outline shield glyph, on both
  platforms; Standard shows no label.
- Full parameters sits under the card, starts collapsed, and appears only with parameters.
- MessageDraftCard Review: Send Email (filled) leads Discard; Sent / Unconfirmed show only the receipt.
- PollCard Answered shows exactly one row, with its original marker and a green check.
- ConnectorCard Error: the error copy is above Retry; Added shows the confirmed receipt, never a button.
- LoginCard: Entry is Rect · Blue "Add login"; Saved is Rect · Secondary "Saved".
- No card performs a network call, stores a value or requests an OS permission.
