# Settings and onboarding reconciliation

This ledger separates verified Figma work from product-code changes and prototype topology. It lets design
move ahead of production without presenting proposed behavior as shipped behavior.

## Engineering work — design-system informed

- **Connectors inline title:** Figma `Screen/Connectors` (`133:192`) now uses the inline iOS top bar.
  Production currently calls `.navigationTitle("Connectors")` in
  `Shared/Views/Settings/SharedComposioConnectionsView.swift` without
  `.navigationBarTitleDisplayMode(.inline)` on the root view. Add the inline display mode on iOS and keep the
  platform-appropriate top app bar in Compose. Closure evidence: root and pushed Connector screens render the
  intended title hierarchy on both platforms.
- **Button sizing and floating actions:** Figma Button (`377:8`) now exposes `Size=Regular|Large` for the Pill
  style, and `Floating CTA` (`1293:23870`) composes that Button instead of owning bespoke label and symbol
  layers. Add the equivalent size contract to the SwiftUI and Compose Button APIs, then migrate Agenda's
  Jump to Today control to the shared Button while preserving its off-today visibility rule and floating
  shadow. Closure evidence: component previews and Agenda captures use the shared implementation.
- **Flexible slider rows:** Voice's Character & speed section now uses canonical `Section` and `ListRow`
  instances with `ListRowContent/Slider` (`1375:5915`) swapped into the content region. Keep leading and
  trailing regions hugging content; the center content fills and wraps; Section owns inset, divider, spacing,
  and surface. Verify SwiftUI and Compose expose the same composition seam before adding a voice-specific row.
- **Platform wrappers:** confirmation dialogs, subscription purchase surfaces, menus, sliders, pickers, and
  segmented controls retain one product contract with native iOS and Android renderers. Do not copy the iOS
  control into Compose or hand-draw a platform-owned surface.

## Engineering work — core product behavior

- **Connector reconfiguration:** implement the proposed Configure connection action, external Composio handoff,
  return, cancellation, and recoverable failure. The external authorization UI remains externally owned.
- **Models and provider keys:** `SharedBYOKSettingsView` exists, but the inspected production checkout has no
  route that presents it. Decide and implement whether Provider Keys is reached from Models or directly from
  Agent settings.
- **Wallet:** define and implement the real destination after Add payment method. Until the provider and return
  contract are verified, keep the handoff labeled Proposed/External.
- **Cloud Browser site model:** evolve `SharedCloudBrowserSettingsView` from global switches and cookie cleanup
  into a domain-centered information architecture. The root owns default access and the site list; Site detail
  owns that domain's permission, saved logins, and cookies/session data. Add Site starts at the root, while Add
  Login starts inside Site detail. Credentials require secure storage and explicit authorization; deleting site
  data must not silently delete a saved login.
- **Android subscriptions:** build the app-owned offer/plan surface, launch Google Play Billing for purchase,
  reconcile the resulting entitlement with Rem's backend, and deep-link active subscribers to the Play Store
  subscription-management screen. Google's lifecycle and management guidance is the source:
  <https://developer.android.com/google/play/billing/subscriptions>.
- **iOS subscriptions:** the platform-owned purchase surface already exists through `SubscriptionStoreView` in
  `Rem/Sources/Settings/AppleSubscriptionSheet.swift`. Its current product-benefit list is: higher daily and
  monthly AI request limits; subscription status synced to the Rem account; and management/cancellation through
  the App Store. Product decisions may revise the copy, but engineering should not replace StoreKit's owned
  purchase UI with a hand-built checkout.
- **Gateway-era settings:** Paired Devices and remaining gateway-dependent routes must close with the runtime
  migration rather than being cosmetically removed from code. The no-gateway design is direction; feature and
  migration ownership remain core engineering.

## Production changes

- **Connector reconfiguration:** the current manage sheet exposes provider identity and disconnect/revoke.
  Figma now preserves that production-backed component and adds `Screen/Connectors · Manage sheet · Proposed
  configuration` (`1347:5360`) with a canonical Configure connection action. The proposed action hands off to
  the external Composio boundary and returns to the connected state. Owner: Settings/Connectors. Implementation
  trigger: product accepts reconfiguration. Expected code change: add the manage-sheet route and external handoff.
  Closure evidence: a production test or capture proves handoff, return, updated provider context, cancellation,
  and recoverable failure.
- **Paired Devices:** the current design removes the obsolete Gateway prerequisite and uses the canonical
  `ContentUnavailableView`. The inspected production checkout still contains Gateway-dependent copy and must
  be reconciled before implementation claims parity.
- **Models and provider keys:** `SharedBYOKSettingsView` exists, but the inspected checkout has no route that
  presents it. Decide whether Provider Keys is a destination from Models or another Settings row, then wire it
  in production and document that path in Figma.
- **Wallet:** identify the real intermediate destination after Add payment method. Treat an external provider
  handoff as an external boundary until production owns another screen.
- **Billing:** reconcile the product decision to remove the legal footer and verify the Manage Subscription
  action and icon against the production route.
- **Confirmation dialogs:** Sign Out and connector revocation are SwiftUI `confirmationDialog` states.
  Replace the current approximation with the iOS 26 platform wrapper, including the system caret/anchor
  treatment, and add the Material confirmation-dialog implementation under the same product contract.

## Design-system changes completed

- Added `Style=Icon · Accent` enabled and disabled variants to the canonical Button family.
- Added the canonical `ConnectorLogo` family and migrated available connector rows from SF Symbol stand-ins
  to official provider assets. The Available, consent, connected, recoverable-failure, and expanded masters
  now all resolve transparent, centered `ConnectorLogo` instances; stale ContainedIcon fill overrides were
  removed. Gmail, Google Calendar, Notion, Slack, Google Drive, Linear, and Todoist now use verified marks.
- Expanded `Section` with `Spacing=Regular|Compact`; `ListRow.Show Divider` owns row separator visibility,
  while the parent Body/List owns the 24-point screen inset.
- Consolidated task run status, deemphasis, overdue, and priority badges into `Pill` variants.
- Reused the iOS 26 Slider source for Voice settings, removed its internal separator, and rebuilt the slider
  group from canonical Section + ListRow instances with a reusable content swap. The preview affordance uses
  the canonical icon-only Button.
- Folded Paired Devices into `ContentUnavailableView` rather than maintaining a context-specific empty-state
  component.
- Added `Size=Large` to the canonical Pill Button and rebuilt Agenda's Jump to Today as `Floating CTA`, a
  composition that contains the Button rather than duplicating it.
- Consolidated the Cards documentation section into Chat components and removed the empty Cards page.
- Strengthened Mobile Flow's collection slots, added `Flow Connector` horizontal/continuation variants, and
  rebuilt Settings 02A as nine explicit Settings → Agent settings → destination flows. Removed the Retired page.
- Rebuilt Tasks & Agenda to match the live component-page documentation model.
- Rebuilt `Screen/Cloud browser` (`1317:4660`) around the proposed site-centered model and added canonical Site
  detail (`1457:253`), Add site (`1457:511`), Add login (`1457:740`), and bounded Sites (`1464:977`)
  screens. All five use the shared Section, ListRow, MenuValue, top-bar, and safe-area components. Each screen
  owns `background/primary`; its clipped `Body` and nested `Auto Layout Content` remain transparent so sections
  inherit the surface while retaining a future scroll viewport. Direct section children fill the content width.
- Added local `TopBarAction/Label` (`1462:28282`) with `label/on-color` for short Add/Save editor actions. Add
  site exposes optional login fields inline; Add login remains available from Site detail for additional saved
  credentials.
- Normalized Cloud Browser section headers to sentence case and bound `Clear all site data` to semantic
  `system/red` rather than a hardcoded color.
- Restructured `Muse · Settings · Reference` (`1459:77`) around one Settings root. Five numbered connector
  lines leave the visible Settings tap targets and terminate at nested destination branches for Connectors,
  Devices, Wallet, Secure credentials, and Permissions. Help & support and report-issue captures remain in a
  nested support subtree marked `root source uncaptured` rather than receiving an invented root connector.
  Thirteen original captures remain contained, including the Link web handoff and Notion's scrolled viewport.
- Restructured `ChatGPT · Settings & Codex Connections · Reference` (`1459:501`) around three vertically
  stacked viewport states of the same Settings root. Numbered connector lines leave the visible Voice, Codex,
  and Cloud browser rows and terminate at their nested destination stacks. Voice now records the observed
  `Start with Voice` preference; the Codex QR/manual pairing route and the reference-only SSH path remain
  separate. Thirteen total captures are contained, and the ChatGPT section sits beside Muse with a 160 px gap.
- Normalized every Settings screen root to semantic `background/primary`; structural `Body`, scroll-content,
  TopBar, and VStack/HStack frames remain transparent. Direct Section children fill the content width while
  the parent content frame owns the screen inset and inter-section spacing. A structural audit confirms the
  canonical status bar and NavigationIndicator on every standard full-device Settings screen.
- Expanded Paired Devices from its empty state into the code-backed populated list, pending review, current
  device detail, other-device detail, QR handoff, scanner, manual-code fallback, and pairing-recovery states.
  The numbered-only `07A · Paired Devices · Documentation` section (`1499:6567`) orders destinations to match
  their source rows and uses simple axis-aligned lines from the tap-target edge to each destination frame.
  Repeated title/value rows are instances of the token-bound `SettingsValueRow` component set (`1508:8051`);
  scanner, QR, and recovery heroes remain custom regions because they do not share that row anatomy.
- Completed the Settings Voice branch with the code-backed Choose a voice destination (`1501:7029`), a
  proposed local `Center button starts` menu (`1501:7097`), and the Chat-selected outcome (`1503:7731`). The numbered
  `08A · Voice · Documentation` section is `1501:7351`.
- Added default (`1501:7279`) and open (`1496:6296`) app-shell states plus numbered `09A · App navigation ·
  Documentation` (`1501:7754`). Persistent destinations use a full-width navigation sheet; compact action
  lists continue to use the platform Menu primitive.
- Closed the remaining apparent mobile gaps by ownership: Share Rem, Send Feedback, and Report a Bug are
  platform/external handoffs; Backup is a conditional Mac-only destination; Provider Keys exists in source but
  has no production route. These boundaries are recorded rather than represented as invented mobile screens.

## Prototype topology

- Domain pages own canonical screen masters and documentation.
- The Prototypes page owns the interconnected runtime graph and shared global navigation.
- Reuse one destination instance across branches when presentation context is unchanged.
- Guided Flow is a real three-step overlay sequence over the canonical Agenda root and is documented through
  three Mobile Placeholder screen instances. Each spotlight reveals an existing App Action Bar control through
  the scrim rather than duplicating the control above the screen.
- Settings prototype `1347:5543` shows the proposed connector reconfiguration state while the current manage
  state remains separate; the external Composio UI is documented as a boundary rather than duplicated in Rem.
- Agent settings documentation now shows Cloud Browser as one shared root leading to Site detail and Add login,
  plus an Add site sibling branch. The root is not duplicated to explain the second branch.
- Remove duplicate prototype destinations only after every incoming interaction is repointed.
