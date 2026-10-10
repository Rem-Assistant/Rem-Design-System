# Settings New native playground — bounded contract

Source of truth: [Settings New](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/?node-id=1825-29320), [Settings entry 1964:86819](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/?node-id=1964-86819), [Agent Settings 1827:50855](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/?node-id=1827-50855). Read-only inspection; no Figma writes. Engineering trial [#70](https://github.com/Rem-Assistant/Rem-Design-System/issues/70).

## Scope and states

A native gallery opens Settings. The Rem row navigates to Agent Settings; Billing & Usage, Permissions, About and Help & Support open their own pages, and Agent Settings → Automations opens the Automations page. Both settled screen compositions are reusable library views; route state and simulated loading live in the demo hosts. Share Rem, Sign Out and Delete Account keep their existing behavior. Page controls that need a backend explain the prototype boundary; automation behavior, purchases and permission requests are not implemented. No real accounts, persistence, network, sign-out, deletion, purchase or connector actions occur.

Gallery offers Success, Slow and Error. Rem enters a loading destination; success presents Agent Settings. Error presents the same error copy, Retry and Cancel on both platforms. Retry succeeds with the local fixture. Cancel and native Back return to Settings and cancel destination-owned asynchronous work. Reopening starts a fresh load. Gallery controls allow a notification toggle and display-name edit. Cancel discards edits, Save trims nonempty input, and changes last for the controls session. Closing/reopening the controls route resets it on both platforms.

## Visual and parity acceptance

- Match reference copy, content order and grouping: profile; Rem/Connected; Billing & Usage + Permissions; About; Share Rem + Help & Support; Sign Out; Delete Account.
- Agent groups: Capabilities (Paired Devices/2, Connectors, Cloud browser, Automations/Scheduled and triggered work), footer; Intelligence (Memory, Models/Automatic, Wallet); Experience (Voice/Aria).
- Native status/navigation bars and back controls are platform-owned. OS safe-area heights and Android font metrics may differ. No simulated status bars or screenshot UI. Android uses the repository's Inter typography and semantic Material Symbols; iOS uses SF typography/symbols.
- Body inset16, section gap22, minimum row60, Rem row82, row inset16/12, leading tile29 with radius7 and glyph17, section radius24, inset dividers16. Row labels17 regular; profile bold17; subtitles13; section headings17 semibold, sentence case.
- White primary background, semantic secondary grouped surface, semantic foregrounds; dark mode follows existing tokens. Native Dynamic Type/font scale must remain readable and scrollable.
- Reuse canonical ListRow, RemSection, ContainedIcon. New settings-density/icon/header axes must preserve existing defaults. No duplicate standalone primitives.
- Paired screenshots must show entry, Agent Settings, loading, error and controls. Build success alone is insufficient. Tests exercise navigation/back, retry/cancel, cancellation past completion deadline, and edit cancellation/save.

## Icon mapping

See the Settings table in [icon-registry.md](icon-registry.md). These are native semantic translations, not pixel-identical glyph shapes. Link-circle has no exact encircled Material twin: plain link is a recorded candidate and is not evidence of final Android icon design approval.

## Completion boundary

Passing this experiment means runnable local native fixtures with reviewed evidence. It does not imply production Rem integration, completion of every Settings subflow, design approval of open icon mappings, Factory readiness, CI success or merge readiness.
