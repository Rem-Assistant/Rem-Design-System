# Settings Connectors / Gmail implementation

Local implementation from integration `8b84f3f`, using the final 2026-10-08 read-only packet (`design-contract/shared-final/CONNECTORS.md`, `connector-nested-props.json`, `avatar.json`, fresh context/render pairs). Source file `af4yDqCzp57jds9lkFiIaO`.

## Shared row contract

`Sources/RemDesignSystem/Rows/ConnectorRow.swift` and `compose/RemDesignSystem/rows/ConnectorRow.kt` implement **ConnectorRow2213:9330** over canonical ListRow/ListRowLabel. Both APIs accept app-controlled state, editable leading/content, subtitle overrides, and an accessory with callbacks/binding; no private connection/switch state. Five authored combinations: Available/Action2213:9331 (Connect), Connecting/Progress2213:9334, Connected/Disclosure2213:9337, Error/Action2213:9340 (Retry), Connected/Switch2247:1963. Settings uses Disclosure; existing onboarding is unchanged. A native SwiftUI NavigationLink supplies disclosure when the row uses `.nativeList`; standalone content includes its own chevron. List owns separators in native contexts. Height is a minimum and grows with Dynamic Type.

Local parserless mappings under `code-connect/{swiftui,compose}/ConnectorRow.figma.ts` read both variant axes, nested Content/Leading Accessory/Trailing Accessory, title/action-label overrides and Show Divider. Content/leading execute their own templates; switch sample reports source On/Off while requiring a caller-owned binding/callback. These local files have not been published or evaluated against Figma. Nested logo snippet generation depends on the connected brand templates; live snippet evaluation remains outstanding. No new lint-baseline exception.

Provider marks use exact unmodified PNG bytes from `docs/playground/settings-design/brand-assets.json`: Gmail, Google Calendar, Notion, Slack, Google Drive, Linear, Todoist. Swift imagesets `Connector*`, Android `connector_*` nodpi resources; helper preserves original colors. Art is 26pt in26×29 slots except Todoist20pt in29×29. Gmail hero centers the unchanged mark in64×64; no stretched raster, tinted brand mark or utility icon substitute.

Avatar185:2 has no child/photo/text/property API. The bounded private helper renders the29pt circle, using systemBlue for the default Gmail instance override and named unbound neutral master color for account permissions. No invented photo/initials. Final raw root context says **Connected • Active/Paused**; CONNECTORS.md's middle-dot wording was a prose erratum acknowledged by the source owner.

## Navigation, scope and source states

Public entry points: `SettingsConnectorsScreen()` (SwiftUI) and `SettingsConnectorsScreen(onBack = ...)` (Compose). Host route registration intentionally belongs to root. SwiftUI uses the outer host stack, native List/Section, native menus, selections and confirmation dialogs. Compose uses Material scaffold, dropdown menus, radio controls/dialogs, and an in-memory ViewModel retaining fixture state/routes through Activity recreation; system Back unwinds scope → Gmail → Connectors → host. No SavedStateHandle, preferences, account or email service.

| Source | Implemented state |
|---|---|
|1833:5048|Connected Gmail/Calendar/Notion/Slack and available Drive/Linear/Todoist, exact marks|
|1883:7678|Gmail detail: lockup, account, connect-another boundary, permission summary, all read/write actions and Information below fold|
|1883:7679 /1876:51201|Connector ellipsis menu: Disconnect accounts|
|1883:7680 /1876:51246|Account ellipsis: Settings, Disconnect account|
|1883:7681|Connector Permissions with exactly four exclusive choices|
|1883:7682|Account Settings, independently scoped override and exact footer|
|1883:7683 /1876:51319|Single-account destructive confirmation; cancellation preserves state|
|1883:7684 /1876:51348|All-account destructive confirmation; cancellation preserves state|

Default account is only `avery@example.com`/Primary. The model supports an injected second account for scope tests. Unset account policy inherits connector policy; choosing an account policy writes a distinct override. Connector changes never overwrite explicit overrides. Single disconnect removes only that account and override; all disconnect removes all Gmail memberships/overrides, retaining connector preference and unrelated providers. Last disconnect returns to Connectors, where Gmail becomes Available. This is a minimal fixture consequence, not a new authored empty screen.

Unsupported provider detail/authentication, Gmail action execution, report and external links show named prototype boundaries. They do not fabricate connections, success screens, email results or real sends. Notion's separate shared pre-consent source is not repurposed for other providers.

## Test hooks and evidence limits

Swift row IDs use `ConnectorProvider.rawValue` (gmail/googleCalendar/etc.); Compose uses enum names (Gmail/GoogleCalendar/etc.). Root tag `settingsConnectors`, rows `connectors.provider.<provider>`, Gmail `gmail.detail`, menus `gmail.connectorMenu` / `gmail.accountMenu.avery`, menu actions `gmail.disconnectAll`, `gmail.disconnectAccount.avery`, `gmail.accountSettings.avery`, confirmation `gmail.confirmDisconnect`; permissions roots `gmail.permissions.connector` / `gmail.permissions.avery`, each choice `gmail.permission.<scope>.<policy>`. Swift policy IDs: alwaysAsk/lowRisk/alwaysAllow/neverAllow; Compose: AlwaysAsk/LowRisk/AlwaysAllow/NeverAllow. Compose back `connectors.back`; boundary `connectors.boundary`. Read/write rows use `gmail.action.<exact action>` in Compose, `gmail.info.<exact action>` in Swift.

Fixture tests cover all four policies and inheritance/override separation, invalid-account no-op, one/all disconnection and default fixture identity. Compose also exercises session Back and retained-owner membership state. They do not prove real native UI cancellation/menu/selection/recreation. Parent owns native host tests, dark/large-text captures and all-state runtime evidence. No builds, simulator runs, Figma writes or remote pushes were made for this change.
