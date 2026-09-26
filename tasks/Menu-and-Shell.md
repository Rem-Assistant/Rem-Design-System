# Task: Menu + app shell (tab bar / FAB)

## Outcome
- **User outcome:** consistent add-menu + primary navigation, instead of ad-hoc toolbar menus.
- **Scope:** a `Menu` primitive (pull-down/action menu) + the **app-shell** component (bottom tab bar +
  center FAB) that hosts it. Founder flagged both as un-inventoried.
- **Mode:** Systemize.
- **Source concern:** `TARGET-COMPONENTS.md §Gaps #3`.

## Design authority
- **Product authority:** add-button menu at `Rem/ContentView.swift:991` & `:1040` (+ `AgendaView.swift:519`);
  the ☰ / center-FAB / + bottom toolbar (RemUI `BottomToolbar`: `line.3.horizontal` · mic FAB · +);
  chat menus at `SharedRemChatView.swift:1012/4038/4050/4312`.
- **Design-system authority:** iOS 26 kit menu + tab bar; `ContainedIcon`, tokens; `screen-track`.

## Approved experience
- **Menu:** trigger (icon/label) → item rows (icon + label, optional destructive/section). States:
  closed · open · item-pressed · disabled item.
- **Shell:** tab bar (Agenda / Chat / Inbox / Settings) + center action (FAB → add-menu). States:
  selected tab, badge, FAB default/pressed.
- **Platform:** iOS uses `Menu` + tab bar; **Android uses Material** (bottom nav + menu/FAB) — same intent.

## System use
- **Reuse:** iOS-26 kit (never hand-build the menu/nav chrome — reuse the kit pieces), ContainedIcon, tokens.
- **Exact:** the menu item set from code; the four tabs + FAB.
- **Excluded:** per-screen content; deep menu logic.

## Delivery contract
- **Acceptance:** Menu master + Shell master; every code menu reproducible as instances; iOS kit chrome (not hand-built).
- **Evidence:** iOS + Android, light + dark; open + closed; Preview; Code Connect.

## Approval
- **Status:** needs decision (which tabs are canonical post-migration — confirm 4-tab set) then Build.
- **Open decisions:** final tab set + whether the ☰ menu survives the shell redesign — founder.
