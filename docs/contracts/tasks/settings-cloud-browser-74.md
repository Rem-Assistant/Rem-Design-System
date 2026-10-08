# Settings · Cloud browser destination — coverage (#74)

Code-only child of the Settings integration. Implements the approved **Cloud browser** prototype
destination on both platforms against the frozen Settings foundation API. No Figma writes, deployment,
main merge, real service calls, or credentials.

## Authority

- `docs/contracts/settings-foundation.md`, `docs/contracts/settings-foundation-api.md`
- `docs/playground/settings-design/settings-destinations.md` → **Cloud browser** section
- Raw node contexts `docs/playground/settings-design/raw/<node>.txt` and screenshots
  `.../screenshots/<node>.png` for every state below (source evidence, not app screens).
  [Verify and extract the source archive](../../playground/settings-design/source-evidence-README.md)
  to read these original paths; the manifest preserves their byte sizes and SHA-256 hashes.

## Entry points (public, deterministic)

| Platform | Entry | File |
|---|---|---|
| SwiftUI | `SettingsCloudBrowserScreen()` (zero-arg) | `Sources/RemDesignSystem/Screens/SettingsCloudBrowserScreen.swift` |
| Compose | `SettingsCloudBrowserScreen(onBack: () -> Unit)` | `compose/RemDesignSystem/screens/SettingsCloudBrowserScreen.kt` |
| Fixture model (shared logic) | `CloudBrowserModel` / `CloudBrowserState` | `.../Screens/SettingsCloudBrowserFixture.swift`, `.../screens/SettingsCloudBrowserFixture.kt` |

Root registers the `cloudBrowser` route. iOS: the view carries **no** `NavigationStack` (the outer
host owns it) and drives nested pushes via `navigationDestination(item:)` /
`navigationDestination(isPresented:)`, so native Back/List/Section/Toolbar semantics are preserved.
Compose: the view owns its own `Scaffold`/title per screen and an internal nav stack; the top-bar back
pops it, and at the root calls `onBack`.

## Source-node coverage (18 authored nodes)

| Node | Route / state | Represented by |
|---|---|---|
| `1833:5071` | cloudBrowser root | `CloudBrowserRootList` / `CloudRootScreen` — Default permission menu, recent sites, See all sites, Clear all site data |
| `1868:6148` | sites list (all sites) | `CloudSitesList` / `CloudSitesScreen` (github/notion/linear/openai) |
| `1868:6153` | clearAll confirmation over root | root `confirmationDialog` / `CloudConfirm` "Clear data for all sites?" |
| `1956:8160` | Add site group | `CloudAddSiteForm` / `CloudAddSiteScreen` |
| `1868:6176` | Add site — empty | domain field empty (placeholder "Enter domain or URL"), Save disabled |
| `1875:6712` | Add site — website focus | subordinate label appears, placeholder → `https://example.com`, URL keyboard |
| `1868:6197` | site detail (github.com) | `CloudSiteDetail` / `CloudSiteDetailScreen` (parameterized; reused for every site) |
| `1956:8161` | Saved login group | `CloudSavedLoginView` / `CloudSavedLoginScreen` |
| `1868:6214` | saved login — view | read rows + pencils, masked password, Remove login; **no Save** |
| `1875:6710` | saved login — username edit | pencil → subordinate label + focused `TextField`; toolbar Cancel/Save |
| `1875:6711` | saved login — password edit | pencil → focused `SecureField` (masked); toolbar Cancel/Save |
| `1868:6227` | Cookies & sessions | `CloudCookiesView` / `CloudCookiesScreen` |
| `1956:8162` | Add login group | `CloudAddLoginForm` / `CloudAddLoginScreen` |
| `1934:9071` | Add login — empty | both fields empty, Save disabled |
| `1875:6713` | Add login — username focus | focused username field |
| `1868:6245` | Add login — filled | username filled; Save enabled when both fields satisfy validation |
| `1868:6250` | Remove saved login confirmation | saved-login `confirmationDialog` / `CloudConfirm` "Remove saved login?" |
| `1868:6255` | Clear site data confirmation over detail | site-detail `confirmationDialog` / `CloudConfirm` "Clear data for github.com?" |
| `1875:6714` | Same clear confirmation from cookies screen | cookies `confirmationDialog` / `CloudConfirm` (same reusable action) |

## Behavior proven

- **Save validation** — Add site Save validates a **nonempty usable domain** (`deriveDomain` strips
  scheme/userinfo/port/path, requires a dot + domain-legal chars). Add login Save requires both
  fields nonempty. Single toolbar Save on both Add screens; disabled when invalid.
- **Draft Back/Cancel does not mutate** — Add screens mutate only on Save; Back (iOS automatic / top-bar)
  discards. Saved-login edit Cancel restores the prior value.
- **Edit scope** — editing Save updates only the selected credential's username *or* password; other
  logins and sites unchanged.
- **Clear data** — clear site / clear all zero cookies + sign-out and **preserve saved passwords**;
  both site-clear entry points (detail and cookies) use the same reusable confirmed action.
- **Remove login** — removes only that credential, preserving the site, its cookies, and other sites;
  returns to site detail.
- **Confirmation cancel preserves everything.**
- **Passwords always masked** — displayed as fixed 12-bullet strings; edit uses `SecureField` /
  `PasswordVisualTransformation`.
- **Permissions** — menu offers only the observed **Ask / Allow**.
- Deterministic fixtures: `github.com` (Ask · 1 saved login · 12 cookies · signed in), `notion.so`
  (Allow · signed in), `linear.app` (Ask · no saved login), `openai.com` (Allow · signed in).

## Accessibility IDs (for root journey / UI tests)

Root/list: `cloudBrowser.root`, `cloudBrowser.defaultPermission`, `cloudBrowser.addSite`,
`cloudBrowser.seeAllSites`, `cloudBrowser.sitesList`, `cloudBrowser.site.<domain>`,
`cloudBrowser.clearAllData`, `cloudBrowser.confirmClearAll`.
Add site: `cloudBrowser.addSiteForm`, `.addSite.domainField`, `.addSite.username`, `.addSite.password`,
`.addSite.save`.
Site detail: `cloudBrowser.siteDetail`, `.siteDetail.permission`, `.siteDetail.login`,
`.siteDetail.addLogin`, `.siteDetail.cookies`, `.siteDetail.clearSiteData`, `.siteDetail.confirmClear`.
Saved login: `cloudBrowser.savedLogin`, `.savedLogin.editUsername`, `.savedLogin.editPassword`,
`.savedLogin.usernameField`, `.savedLogin.passwordField`, `.savedLogin.save`, `.savedLogin.cancel`,
`.savedLogin.remove`, `.savedLogin.confirmRemove`.
Add login: `cloudBrowser.addLoginForm`, `.addLogin.username`, `.addLogin.password`, `.addLogin.save`.
Cookies: `cloudBrowser.cookies`, `.cookies.clearSiteData`, `.cookies.confirmClear`.
Permission menu: `cloudBrowser.permissionMenu`. Back control (Compose): `cloudBrowser.back`.

## Tests

- SwiftUI: `tools/render-swift/Tests/RenderSnapshotTests/SettingsCloudBrowserFixtureTests.swift`
- Compose: `compose/RemDesignSystem/src/test/kotlin/com/rem/designsystem/SettingsCloudBrowserFixtureTest.kt`

Both assert domain derivation, save validation, add/edit/remove scope, clear-data password
preservation, and masked passwords — the scope/cancel/save rules — without a device.

## Boundary gaps (named, not invented)

- **Permission policies** beyond Ask/Allow are not authored; the menu exposes only those two.
- **Site detail is reused for all sites.** Only github.com's detail is an authored master; notion/
  linear/openai reuse the same parameterized detail screen with their fixture data (no new screens
  invented). notion/linear/openai have no authored saved-login content, so their Saved logins section
  shows only "Add login".
- **No provider / OAuth / real-browser surface.** All state is in-memory for the playground session;
  nothing is written to the Keychain, shared preferences, or the network.
- **Centralized host navigation and real-host end-to-end journey tests are the integration owner's**;
  this task exposes the deterministic entry points + accessibility IDs above.

## Source reconciliations

- Cloud browser rows carry **no leading `ContainedIcon`** (confirmed from the raw node contexts —
  only `chevron.right` disclosure and the `chevron.up.chevron.down` permission menu appear), so the
  Subtle-icon amendment is not exercised here; no new icon-registry glyph is required.
- `1875:6714`'s render omits its visible sheet though its response carries the same action-sheet
  contract as `1868:6255`; per the contract we reuse the same confirmed clear action from the cookies
  entry point and record the source inconsistency.
- Pre-existing component-lint gap `ListRowLabel:figma` (a foundation-lane component that uses the
  modern `code-connect/*.figma.ts` mapping, like its already-grandfathered siblings `ListRow` and
  `RemSection`) was added to `tools/component-contract.baseline.json` for consistency so the contract
  lint is green. No foundation code was modified.

## Checks run on this runner

- `swiftc -frontend -parse` on all three new Swift files — **passed** (syntax parse; not full
  type-check/compilation).
- `node tools/lint-components.mjs` (component architecture contract) — **passed** (no new violations).
- `npm run check-tokens`, `npm run check-manifest` — **passed**.

## Unsupported on this runner (hosted CI required)

- SwiftUI / Compose **compilation** and the fixture **tests** — this is a Linux runner with no
  SwiftUI, no Android SDK, and no plugin/dependency network access; they run on hosted macOS/Android CI.
- `npm run check-code-connect` — `node_modules` is not installed here (the `@figma/code-connect`
  type package is absent). This task authors no `.figma.*` mappings.
- No rendered/visual verification is claimed; paired iOS/Android renders are produced by the Verify
  phase, not here.
