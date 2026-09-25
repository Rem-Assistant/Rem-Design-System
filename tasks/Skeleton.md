# Task: Skeleton — the one loading primitive

## Outcome
- **User outcome:** consistent loading shimmer everywhere, instead of ~7 bespoke skeletons that drift.
- **Scope:** one `Skeleton` primitive (shape + shimmer) with presets for the row/card/list shapes it replaces.
- **Mode:** Systemize (new primitive; absorbs a family).
- **Source concern:** `TARGET-COMPONENTS.md §Gaps #2`.

## Design authority
- **Product authority (absorbs):** `SessionListLoadingSkeleton`, `ConnectorListLoadingSkeleton`,
  `CloudBrowserSettingsSkeleton`, `TaskEventRowSkeleton`, `AutomationInputRowSkeleton`,
  `AutomationOverviewRowSkeleton`, `VoiceSettingsOverviewLoadingSkeleton`, `ChatWakingSkeleton`.
- **Design-system authority:** `Surface`/tokens; radius + `fill.tertiary`; shimmer at token opacity.

## Approved experience
- **Composition:** a shape (line / block / circle / row / card) filled with a shimmer sweep.
- **States:** shimmering (default) · static (reduced-motion) · resolved (host swaps in real content).
- **Accessibility:** honor reduce-motion (static fill); mark as busy/hidden from AX tree.

## System use
- **Reuse:** Surface, tokens. **Exact:** the shape presets that match the real components' silhouettes.
- **Adaptable:** shimmer timing within token motion. **Excluded:** the host views' loading logic.

## Delivery contract
- **Acceptance:** one master; each replaced skeleton reproducible as a preset/instance; reduce-motion path.
- **Evidence:** iOS + Android, light + dark, animated (gif/recording) + static; Preview tile; Code Connect.

## Approval
- **Status:** approved for Build. **Open decisions:** none.
