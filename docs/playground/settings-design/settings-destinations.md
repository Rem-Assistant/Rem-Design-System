# Settings prototype design contract

Evidence captured 2026-10-08 from file `af4yDqCzp57jds9lkFiIaO`, section `1833:5014`, **Agent settings · Screens & working masters**. [Open section](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/?node-id=1833-5014).

**Update:** Voice and Connector shared-core component contracts are on hold pending the parent’s read-only comparison with onboarding `1147:9826` and the single authorized writer’s Onboarding New contract. The captures below remain evidence, but do not freeze new mappings or duplicate the core components. The other five destination contracts are ready. Proposed/Working naming does not block authorized prototype reproduction.

This is an implementation handoff, not a claim that the static Figma masters already implement behavior. The user authorized both-platform, deterministic prototype journeys. All state changes below are local fixture mutations; pairing, auth, provider keys, payments, credentials, deletion, model execution, memory generation, voice services and scheduling must not call services. Automations is unresolved and excluded. Chats and Agenda are not part of this implementation.

## Evidence and authority

- Archived `raw/<node-id>.txt`: full `get_design_context` reference code and component descriptions. [Verify and extract the source archive](source-evidence-README.md) before reading these paths. IDs use a hyphen in filenames. This is implementation evidence; adapt to existing SwiftUI/Compose components, never ship generated React.
- Archived `screenshots/<node-id>.png`: Figma target render, not a runtime asset. The same [source archive](source-evidence-README.md) preserves all original bytes and paths. All six single-screen top masters, Voice closed/open, seven Gmail states, child overlays, Cloud editing states, Models key variants, Memory summary, Wallet pre-consent and external shells are preserved.
- `section-metadata.xml`: exact section inventory and visual connector labels. The section contains no additional pairing/auth/provider flows beyond those enumerated here.
- `asset-manifest.json`: 28 references to 20 distinct returned static assets. Original URL downloads returned HTTP 202, `x-amzn-waf-action: challenge`, empty body; empty files were removed. **Resolved for all required provider brands** using exact-node, read-only PNG exports under SwiftUI design-to-code §9. `brand-assets.json` records 10 verified nonempty 4× PNG exports with source/slot dimensions and SHA-256. Use these assets; never screen screenshots or approximate glyphs. `download-assets.py` records the original URL attempt.
- Figma current masters are source of layout/copy. For this Settings lane, the approved **Subtle contained utility icons** amendment overrides relevant remaining Tinted icon instances. This is not permission for blanket app-wide restyling. Voice/Connector intentional shared-core exceptions await the parent’s updated contract. Preserve authentic multicolor provider logos, including Wallet image-filled instances even though their historical main component is ContainedIcon. Record deliberate native/Subtle amendments in implementation evidence.

## Common component contract and reuse

| Figma canonical | Required concept | Existing code path / treatment |
|---|---|---|
| Section `1307:667`, Inset Grouped `741:311` | style, header, footer, optional header action, rows | iOS native `List`/`SwiftUI.Section`; Android existing `compose/RemDesignSystem/rows/RemSection.kt`. Swift custom `Rows/RemSection.swift` remains adapter for custom scroll compositions. |
| ListRow `101:18` | leading / content / trailing slots; row-owned divider; interaction role | `Rows/ListRow.swift`, Compose `rows/ListRow.kt`; native iOS navigation/control owns accessory, avoid double chevrons/padding/separators. |
| ListRowLabel `1966:60442`; Fill `188:2` | title, optional subtitle, optional title accessory; title layout Fill/Hug | Inline Default and Elevated risk pills belong in title accessory slot, not overlays. Trailing accessory hugs intrinsic control dimensions. |
| SectionHeader `161:68`, Footer `161:70` | 17pt semibold title, optional trailing action; 13pt wrapping footer | Parent List/Body owns horizontal inset. Add site is canonical Button in header action slot. |
| ContainedIcon `614:8`, Subtle Small `614:4`, Large `614:6` | semantic symbol, Style, Size | Existing Swift/Compose ContainedIcon. Current API calls this `fill`; Figma descriptions use `Style`. Foundation owner must map real property names rather than assuming old archived mappings are current. |
| Lockup `1861:54960`, centered `773:22`, left `1861:54955` | visual, title, body; center/left alignment; transparent surface | Use reusable composition; Gmail left lockup sits inside Section; Wallet/Device centered hero does not draw an extra card. |
| Button `377:8`, group `773:17` | style/state/size + leading/trailing slots | Existing RemButton family, native toolbar actions as appropriate. |
| ConnectorLogo `1328:440` | Brand selects exact official provider mark | No provider icon substitution. Use exact exported brand assets in this packet; do not confuse the unrelated existing Google sign-in icon with Gmail/Drive/Calendar. |
| Switch `868:210`, accessory `1319:24136` | platform-specific geometry, shared On/Off state | Native iOS Toggle / Compose Material switch, green on tint. |
| RemComposerBar `53:2` | editable text, optional leading/Speak/send slots | Existing `Chat/RemComposerBar.swift` and Compose twin are static and include model/Speak controls. Extend/compose shared component for Memory's simpler interactive variant; do not silently show absent controls. |
| VoiceSliderRows `1328:26737`, ListRowContent/Slider `1375:5915` | label, native slider, min/max captions | iOS Slider / Material equivalent; same state contract, platform geometry allowed. |

Visual baseline: 402×874 iOS specimen; white background; status 62, toolbar 54, bottom safe-area 34; parent horizontal inset 16; grouped surface #f2f2f7, radius 24; row minimum 60, subtitle rows typically 64, horizontal padding 16/vertical 12, gap 12; 17/22 primary, 13/18 secondary. Use semantic tokens and native safe areas, not drawn status/home/keyboard controls. Native iOS List rounding/insets may need explicit approved reconciliation; do not change row semantics to chase screenshots.

## Paired devices

| Node | Route/state | Exact visible copy / contract |
|---|---|---|
| `1833:5031` | pairedDevices.populated | Title `Paired devices`, trailing `Add`; section `Paired devices`; `Mac Studio` / green dot `Connected · Recently active`; footer `Open a device to review its connection or remove its access.` |
| `1833:52316` | pairedDevices.empty | Title `Paired Devices`; centered `No paired devices`; `Open Rem on another device and sign in with the same account. It will appear here automatically.`; `Refresh devices`. Large Subtle `macbook.and.iphone`. |
| `1833:52344` | pairedDevices.device(id) | Title/hero `Mac Studio`, body `Desktop`, large Subtle `laptopcomputer`; Connection rows `Platform` → `macOS`, `Status` → `Recently active`, `Last active` → `1 min ago`, `Connected on` → `—`; destructive `Remove access`; footer `Removing this device revokes its access to Rem.` |
| `1839:52547` | device.removeConfirmation | `Remove access to Mac Studio?`; `This device will be disconnected and will need to pair again.`; destructive `Remove access`, `Cancel`. |

Only OTHER paired devices appear; current device is excluded. Peer status can be connected or offline. `Connected on` maps to approvedAtMs: show only when supplied; the em dash is a specimen placeholder, not a factual date. Current visual has no raw permission-scope editor.

Prototype behavior: select peer → detail; Cancel confirmation preserves state/detail; confirm removes only fixture peer and returns to list, which reaches designed empty state after last peer; Refresh performs deterministic local refresh/retry fixture. **Add is an entry point without an authored pairing destination in this section.** Expose the boundary explicitly, not an invented QR/pairing flow or fake successful pairing. Loading/error/offline are test harness scenarios unless separately designed, not additional Figma-approved screens.

## Connectors and Gmail

Top `1833:5048`: Connected rows `Gmail`, `Google Calendar`, `Notion` with `Connected • Active`; `Slack` with `Connected • Paused`. Available rows `Google Drive`, `Linear`, `Todoist`, each `Not connected` and `Connect`. All use authentic provider marks. Only Gmail's nested destination is designed here; do not duplicate Gmail screens under unrelated providers and claim those flows are approved.

Group `1883:7685` contains:

| Node | State | Entry/exit |
|---|---|---|
| `1883:7678` | Gmail default | Connectors.Gmail → detail, native Back returns. |
| `1883:7679` | connector context menu | Top ellipsis → `Disconnect accounts`; outside dismiss. Overlay child `1876:51201`. |
| `1883:7680` | account context menu | Account ellipsis → `Settings`, `Disconnect account`; outside dismiss. Overlay child `1876:51246`. |
| `1883:7681` | connector permissions | Permissions row → `Permissions`, scope Gmail / `All connected accounts`; choice commits local connector permission. |
| `1883:7682` | account permissions | Account Settings → `Settings`, scope `avery@example.com` / `Gmail · Primary account`; choice commits local account override. |
| `1883:7683` | disconnect one confirmation | Account menu → action sheet, overlay `1876:51319`; Cancel preserves all. |
| `1883:7684` | disconnect all confirmation | Connector menu → action sheet, overlay `1876:51348`; Cancel preserves all. |

Default detail contents (scroll all): left-aligned Gmail logo lockup `Gmail` / `Read and manage Gmail`; `Connected accounts`: avatar + `avery@example.com` / `Primary`, row ellipsis; `Connect another account`; `Permissions` / `Allow low-risk actions`. Read actions: `Batch read email`, `Batch read email threads`, `Search emails`, `Search email ids`, `Search thread ids`. Write actions: `Apply labels to emails`, `Archive emails`, `Batch modify email`, `Create draft`, `Send email`, `Send draft`, `Update draft`. Information: `Category` / `Productivity`, `Website` / `mail.google.com`, `Privacy Policy` / `policies.google.com`, `Report an issue`.

Permission choices (radio, exactly one):

1. `Always ask` — `Ask before reading messages or taking any Gmail action.`
2. `Allow low-risk actions` with blue `Default` inline pill — `Read messages and manage drafts or labels. Ask before sending or deleting email.` (initially selected)
3. `Always allow` with red `Elevated risk` inline pill and warning symbol — `Allow every Gmail action without approval, including sending or deleting email.`
4. `Never allow` — `Block Gmail access until you change this permission.`

Connector footer: `Controls the level of access Rem has across all Gmail accounts connected to this connector.` Account footer: `Controls what Rem can do with avery@example.com. Overrides the connector-wide setting for this account.`

Single confirmation: `Disconnect avery@example.com?` / `Rem will lose Gmail access for avery@example.com. Your emails are not deleted. You can reconnect this account at any time.` / `Disconnect account`, `Cancel`.

All confirmation: `Disconnect all Gmail accounts?` / `Rem will lose Gmail access for every connected account. Your emails are not deleted. You can reconnect accounts at any time.` / `Disconnect accounts`, `Cancel`.

Prototype confirm mutates only fixture account connection(s), never email. Return to appropriate fixture list; non-Gmail account/provider detail, OAuth entry, action-row detail, report issue and external web links have **no authored nested target here**. Keep a named boundary/gap rather than inventing screens. A two-account test fixture can prove single vs all semantics, but default visual fixture remains the one designed account. Account avatar color differs between default/menu/permissions specimens; use canonical Avatar with deterministic fixture color and record any reconciliation.

## Cloud browser

| Node | Route/state |
|---|---|
| `1833:5071` | cloudBrowser root |
| `1868:6148` | sites list |
| `1868:6153` | clearAll confirmation over root |
| `1956:8160` | Add site state group: empty `1868:6176`, website focus `1875:6712` |
| `1868:6197` | site detail, github.com fixture |
| `1956:8161` | Saved login state group: view `1868:6214`, username edit `1875:6710`, password edit `1875:6711` |
| `1868:6227` | Cookies & sessions |
| `1956:8162` | Add login state group: empty `1934:9071`, username focus `1875:6713`, filled `1868:6245` |
| `1868:6250` | Remove saved login confirmation |
| `1868:6255` | Clear site data confirmation over detail |
| `1875:6714` | Same confirmation invoked from cookies screen |

Root: `Default access` → `Default permission` / `Ask before Rem opens a new site.` → menu value `Ask`; `Sites` header action `Add site`; github.com `Ask · 1 saved login`, notion.so `Allow · Signed in`, `See all sites`; footer `Recent sites appear here. Add a site to configure its access.`; `Browser data` → destructive `Clear all site data` / `Cookies and sessions across every site.`; footer `Saved passwords remain until you remove them.`

Sites: trailing `Add`; rows github.com and notion.so as above, linear.app `Ask · No saved login`, openai.com `Allow · Signed in`.

Add site: top title `Add site`, a **single toolbar Save** (disabled empty, enabled valid fixture domain). Empty first field `Enter domain or URL`. On focus/filled, that exact phrase becomes small subordinate label above primary `https://example.com`. `Access`: `Permission` / `Choose how Rem should handle this site.` → `Ask`. `Login details`: `Username or email (optional)`, `Password (optional)` (inline optionality, no duplicate value/help line). Footer `Save a login now, or add one later from the site's detail screen.` Native keyboard/caret. Back discards draft; Save adds only fixture site and returns to source list/root. Permission picker options beyond observed Ask/Allow are **not enumerated in these authored masters**; do not invent extra product policies.

Site detail: title `github.com`; `Access`: `Permission` / `Controls whether Rem can open this site.` → Ask; `Saved logins`: `samuel@example.com` / `Password saved securely`, `Add login`; `Site data`: `Cookies & sessions` / `12 cookies · Signed in`, `Clear site data` / `Signs Rem out of github.com.`

Saved login view: title `Saved login`; `Website`: `github.com` / `This credential is scoped to this site.`; `Login details`: `Username or email` / `samuel@example.com`, `Password` / 12 masked bullets, each trailing pencil; footer `Illustrative values. Saved credentials require secure storage and explicit authorization.`; destructive text `Remove login`. **No Save in view state.** Pencil focuses that field: top-left `Cancel`, top-right `Save`, subordinate label + native input/caret/keyboard; passwords always masked. Cancel discards draft and returns to same view; Save replaces fixture value and returns to view. Never record real passwords or write secure storage.

Add login: title `Add login`, Back + Save; `Website`: `github.com` / `Login will be available only for this site.`; `Login details`: username/email + masked password. Footer `Rem uses this login only when you authorize access to github.com.` Empty Save disabled; filled/focused Save enabled when fixture fields satisfy validation. Back discards; Save adds fixture login to this site and returns to site detail. Mock credentials only.

Cookies: title `Cookies & sessions`; section `github.com`; `Session` / `Signed in`; `Cookies` / `12 illustrative cookies`; footer `Clearing cookies signs Rem out of this site. Saved logins are separate.`; `Clear site data` below section.

Confirmations:

- All: `Clear data for all sites?` / `Rem will be signed out of all sites. Saved logins are kept.` / `Clear all site data`, `Cancel`.
- Site (both entry points): `Clear data for github.com?` / `Rem will be signed out of this site. Saved logins are kept.` / `Clear site data`, `Cancel`.
- Saved login: `Remove saved login?` / `This removes the saved credential for github.com.` / `Remove login`, `Cancel`.

Confirmed clear sets relevant fixture cookie counts to zero/signedOut and **retains credentials**; cancel changes nothing. Confirmed remove affects selected fixture credential only, never cookies or another site, then returns to site detail. Verify both clear entry points and navigation back. `1875:6714` render currently omits the visible sheet despite its response containing the same action-sheet contract; use the reusable confirmed action from `1868:6255` and record this source inconsistency.

## Memory

Root `1833:5096`: `Memory controls`: `Search and reference chats` / `Use details from past chats` (on), `Generate memory` / `Update the summary from chats` (on), `Sensitive topics` / `Allow sensitive details` (off). Footer `Rem can learn from conversations. You stay in control of what it keeps.` `Overview`: `Memory summary` / `Generated from your conversations`; footer `Open the generated summary to ask Rem to correct, add, or forget something.`

Summary `1865:6193`: title `Memory summary`; metadata `Updated just now · Generated from your conversations`; sections and exact paragraphs:

- `Overview`: `You prefer direct, practical help and clear product decisions. Rem should use prior conversations when they are relevant, keep durable preferences current, and avoid treating short-lived details as permanent facts.`
- `How Rem should work`: `Be resourceful before asking. Use the tools and context already available, show what changed, and keep proposed ideas distinct from behavior that exists in the product.`
- `Current focus`: `You are simplifying Rem around a user-centered UI layer, connected capabilities, automations, and a memory model that is useful without exposing infrastructure as product.`

Bottom composer placeholder `Ask or update memory`, plus and Send only. Root toggles update fixtures independently. Summary composer can demonstrate deterministic simulated submission/cancel and fixture update, but **post-send conversational response UI and attachment picker are not authored here**. Do not invent a full chat destination or imply generated text/backend memory persistence. Parent should accept explicit prototype-only feedback/state contract before adding visible response layouts.

## Models

Root `1833:5109`: `Rem` → `Auto` / `Rem’s managed model` (on). Footer `Rem chooses a managed model for each question based on the task, availability, and cost.` `API keys`: `Anthropic` with saved status property, provider availability switch off, `Add provider key` disclosure. Root screenshot's provider saved subtitle/picker glyph does not render cleanly; raw response contains `Saved`. Keep exact intended saved/provider status semantics and report visual mismatch.

Key group `1956:8163`: empty `1934:9067`, provider picker `1934:9068`, filled `1934:9069`, editing `1934:9070`. Child menu `1870:54593` recovers full list absent from parent generated code. `Add provider key` title, Back and one Save; section `API key`; `Provider` → `Anthropic`; key field placeholder `API key`; footer `Enter an API key from your provider to use its models.` Picker: Anthropic, OpenAI, Google, Mistral, OpenRouter. Save disabled empty; filled/focus key masked, Save enabled. Password-style native input and keyboard/caret in focus state.

Prototype: provider selection updates fixture draft; Back discards; Save records a **dummy saved-key flag** and returns to Models. Do not request/store actual keys, validate via provider, install credentials or run models. Figma does not settle Auto/provider mutual-exclusion, deleting/editing saved keys, exact validation rules or provider model lists. Expose those gaps rather than invent production policies.

## Wallet

Root `1833:51817`: Wallet title/centered lockup. Symbol explicitly `wallet.pass` (Figma render resembles clipboard; use intended semantic symbol, do not redraw). Body `Securely save payment methods for Rem to use when making purchases for you.` Rows exact Link and Shop Pay marks with `Link by Stripe`, `Shop Pay`.

Pre-consent Link `1898:53941`, Shop `1898:54081`: native inspector sheet over dimmed Wallet. Provider mark 60×60 rounded14; title provider; body `Connect your [Link/Shop Pay] wallet for purchases you ask Rem to make.` Three benefit rows:

- list.bullet: `Use your saved wallet` / `Use payment methods and checkout details you authorize through [Link/Shop Pay].`
- hand.raised.fill: `You choose what Rem can do` / `Rem asks before actions that need review. Disconnect anytime in Settings.`
- eye: `Keep an eye on things` / `Rem may take unexpected actions. Review purchases carefully.`

Disclosure: `Next, continue to [Link/Shop Pay] to sign in and review access. Rem will exchange info with [Link/Shop Pay]; see its terms and privacy policy.` Blue `Connect`, text `Cancel`. Cancel/swipe dismiss to Wallet without mutation; Connect enters designed external boundary.

External Link `1926:53854` and Shop `1926:53931` are **blank provider web-content shells**, not provider sign-in/payment designs. Title domains `app.link.com`, `shop.app`; close, text-size, back/forward, share, refresh browser chrome. Preserve boundary and close return; do not load/authenticate actual provider or fabricate provider-owned login/payment content/connected success. Native Share equivalence: same payload/action contract, iOS activity sheet vs Android Sharesheet; fixture URL/text payload only and no automatic send. The exact external result/return-success flow is not designed.

## Voice

Group `1904:56535`: closed `1904:56533`, open `1904:56534`. Closed code + open screenshot + child menu `1904:55842` cover full surface (open parent get_design_context errors). `Hear this voice` / `Aria`, trailing play. `Conversation entry`: `Center button starts` → `Voice session`; footer `Choose whether the center action opens a voice session or a new chat.` Native menu choices **Voice session, Chat**. `Spoken responses`: `Voice` / `Aria (Warm)`; footer `Choose how Rem sounds when reading a response or talking with you.`

`Character & speed`: native labeled sliders Speed (`Slower` / `Faster`), Consistency (`Creative` / `Consistent`), Likeness (`Flexible` / `Faithful`). Footer `Speed applies to the next thing Rem says. Consistency trades expressive range for a steadier delivery, and likeness controls how closely Rem holds to the chosen voice.` Screen scrolls; do not omit third slider/footer below first fold. Numeric ranges/units/service values are not specified; use normalized deterministic fixture values and identify them as implementation choices.

Chooser `1882:42599`: `Choose a voice`; `Preview a voice, then choose the one Rem should use.` Rows each separate play action and radio selection: Aria / Warm (selected), Sol / Bright, Rowan / Calm, Juniper / Expressive, Vale / Neutral. Footer `Your choice follows this agent across your devices. Tap a play button to hear a preview.` Native Back returns; selecting updates fixture voice and prior summary. Preview action changes local preview state; no voice service or recording. No audio preview files are supplied, so audible voice fidelity remains a gap unless approved local sample assets are provided. Do not silently claim these are real provider voices or account-wide sync.

## Minimum interaction/visual evidence

Each platform must show real entry → each of seven destinations → back, plus all authored nested route/state IDs above. Keep centralized navigation ownership and isolated builders. Tests should assert user-visible state transitions and scope, not implementation strings alone:

- Device cancel vs confirmed removal/empty, absence of current device.
- Gmail both menus, all four permission choices scoped correctly, single/all disconnect cancel/confirm.
- Cloud both Add entry points, disabled empty Save, focus/keyboard, save/back, edit Cancel vs Save, password masking, remove one credential, both site-clear entry points, clear-all retaining credentials.
- Memory toggles independent, summary reachable, composer enabled/disabled and deterministic submission boundary.
- Models picker five choices, masked dummy key, empty Save disabled, back discards, save changes fixture status only.
- Wallet both consent sheets, cancel/swipe dismissal, external blank boundary and close, equivalent native share presentation if wired.
- Voice menu selection without launching Chats, preview state separate from radio selection, all five voices, three sliders and scroll footer.

Render baseline and relevant overlays/editing states at exact fixture values. Native host differences are expected for iOS versus Material controls, but product hierarchy/copy/semantic state and asset identity are shared. Do not mark visual verification passed until exact exported brand assets are installed at the recorded geometry and menu/field source gaps are reported.

## Explicit gaps, not permission requests

1. Automations requires design decision; Chats/Agenda separate later work.
2. Pairing Add flow, non-Gmail provider detail/auth, Gmail action detail/report target, Memory post-send/attachment UI, Cloud permission choices beyond observed Ask/Allow, Models saved-key lifecycle/Auto policy, Wallet provider success screens and Voice audio samples are not authored here.
3. Static asset URL endpoint remains WAF-challenged, but exact provider logos are delivered via read-only isolated-node PNG export. Full-screen screenshots remain evidence only. Wallet row Link source screenshot emitted no image; use the exact same official image fill exported at the 60pt pre-consent node, rendered in the 29pt row slot.
4. `get_design_context` occasionally omits native controls, text inputs, toolbars or overlays despite full response. Child contexts recovered Gmail menus/action sheets, Models picker and Voice menu. Preserve screenshot correlation and use native controls; never infer unseen product destinations from generated omissions.
5. This extraction made no repository/Figma changes, no builds, no Factory dispatches and no Trove interactions.
