# Task: BrowserTakeover — human-in-the-loop browser sheet

## Outcome
- **User outcome:** the person can watch Rem drive a browser and take/return field-level control.
- **Scope:** the `BrowserTakeover` screen template + its parts (surface, remote cursor, address bar,
  control bar). Already built in Figma — reconcile into components/template.
- **Mode:** Reproduce.
- **Source concern:** `TARGET-COMPONENTS.md §Gaps #5`.

## Design authority
- **Product authority:** `Shared/Views/Browser/SharedBrowserLiveView.swift` (`SharedBrowserLiveSheet`,
  `BrowserLiveCard`, `BrowserLiveSurface`, `RemoteCursor`).
- **Task artifact (already built):** Figma live state `556:31`, controlling state `562:31`,
  `BrowserLiveCard` `524:31`. Reconcile these to the token/component-track standard.
- **Design-system authority:** kit sheet chrome (never hand-build), `Surface`, `Pill` (Live badge),
  `Button`, tokens.

## Approved experience
- **Composition:** grabber + nav (Done / "Rem's browser" / End) · address bar (lock + host/path + Live)
  · surface (page + RemoteCursor) · control bar.
- **States:** `live` ("Rem is driving" + Take control) · `controlling` (focused field + field editor +
  "You have the controls" + Give-control-back) · `waking` (skeleton) · `failed` · `ended`.
- **Interaction:** Take control → controlling; Give control back → live; End → ended.

## System use
- **Reuse:** iOS-26 kit sheet, Surface, Pill, Button, tokens. **Exact:** the five states from code.
- **Excluded:** the browser transport/session logic.

## Delivery contract
- **Acceptance:** template + parts as components; five states from one master where possible; kit chrome.
- **Evidence:** iOS + Android, light + dark, per state; Preview; Code Connect.

## Approval
- **Status:** approved for Build (Figma states exist; reconcile to tokens). **Open decisions:** none.
