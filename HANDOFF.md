# Handoff — Rem design system reconciliation (for the next agent)

_Written 2026-09-25. Read this first, then `RECONCILIATION.md` (the screen-by-screen map),
`FILE-ORG.md` (Figma page structure), `REGISTRY.md` (component → node), `EVOLUTION.md`._

## TL;DR — direction decided
**Go code-first.** Stop reconciling Figma to code by hand (design→code). Instead: clean the shipping
SwiftUI into a Fluent-organized, token-driven **presentational library**, annotate with **Code Connect**
(`.figma.swift`), then **generate the Figma library from the code**. Figma becomes a bound projection,
not a hand-maintained twin. Proof this is right: `Rem/Sources/Components/DailyBriefCard.swift` already
has the right stateful component (Read latest brief → Stop → Read again → Retry); the emoji + oversized
"Wednesday Evening" title the founder flagged exist **only in the hand-built Figma**, not in code. That
drift is exactly what design-first produces.

## The one rule that governs everything
**Reconcile to code that has callers and ships. Never to `docs/prototypes/RemUI`.** RemUI is a stale,
caller-less prototype (founder-confirmed). Its onboarding screens ("How Rem Works", "Unblock Rem's
power", Permissions value-prop) have zero callers and are deprecated. If a Figma screen's "source" has
no caller in `Rem/`, `Shared/`, `RemMac/`, it is not a reconciliation target.

## Where we are (done, verified against code-with-callers)
- **Screens verified vs shipping code:** Login (`411:15`↔`OnboardingFlow.signIn`), Agenda
  (`181:754`↔`SharedAgendaView`), Inbox (`206:703`↔`SharedInboxView`/`SharedTaskRow`), History
  (`438:15`↔`ChatHistoryView`), Settings (`130:44`↔`SharedSettingsView`), TaskEvent
  (`299:2`↔`TaskEventView`), **TaskInspectorSheet built** (`581:61`↔`TaskEventView.swift:961`).
- **TaskEventRow** (`46:21`) reduced to a clean `Kind × Leading` matrix; invented `Schedule`/`Clock`
  leading variants retired; Inbox now solid-circle + headline (matches `SharedTaskRow`).
- **ContextualMessage** (`73:39`) unified: boolean `Actions` prop + swappable `ButtonGroup` (`576:31`);
  pairing (`577:2`) + calendar (`577:31`) are true instances.
- **Chat scenarios / tool cards:** all 16 `ParsedToolResult` cases have components (Confirmation,
  Error, DeviceStatus `566:68`, Calendar `570:31`, Reminders `570:59`, DeviceInfo `570:86`),
  AssistantMarkdown code+table `566:31`, browser takeover live `556:31` + controlling `562:31`, run
  activity `558:31`, result cards `557:31`.
- **Figma file reorganized** into banded pages (see `FILE-ORG.md`): FOUNDATIONS / COMPONENTS / SCREENS
  / PATTERNS / PROPOSED / KIT / ARCHIVE. Dead flows in **Retired**.

## Open decisions (founder-gated — do NOT act unilaterally)
1. **Packaging location:** `Packages/RemDesignSystem` SPM target inside RemClaw, vs the separate
   `rem-design-system` repo as a shippable package. Drives where the code cleanup lands.
2. **Gateway Settings surfaces** (the "Your agent runtime → Rem·Connected" section + hub `509:833`):
   they deprecate with the runtime migration but the gateway is **still the production adapter today**
   — removing them from Figma now makes it stop matching shipping code. Retire-now (mirror-future) vs
   hold-for-migration-slice is a timing call.
3. **Tool-call cards:** **Do NOT deprecate yet (founder, 2026-09-25).** They look out-of-system today;
   the path is **restyle to tokens**, not deletion. Keep all callers. Revisit deprecation only if/when
   the runtime migration actually removes the underlying tool.

## Runtime migration context (critical)
`docs/rebuild/07-REM-RUNTIME-MIGRATION.md`: OpenClaw→Rem **de-brand is done** (OpenClawKit →
`Packages/RemKit`), but the **per-user gateway is still provisioned today** ("OpenClaw remains the
production adapter until… direct evidence"; only signal-relevance runs gateway-free). Target
**eliminates** provisioning (shared multi-tenant runtime). Two slices still OPEN: *Product cleanup*
(onboarding + Settings drop gateway surfaces) and *Infrastructure retirement*. **Design may mirror the
no-deploy future now; do not remove the corresponding CODE ahead of the migration slice.**

## Fluent-style target (what "clean up the code" means)
Mirror Fluent UI's organization in BOTH code and Figma: **tokens → primitives → components (with
explicit state variants) → patterns/templates**. Screens/templates get state variants too
(loading/empty/populated/error/reading). `tokens.json` (Style Dictionary) is the shared spine →
SwiftUI theme + Figma variables (+ Compose later — SwiftUI is Apple-only; Android is a parallel Compose
implementation of the same token/Figma spec, not shared SwiftUI). Code Connect doc:
https://developers.figma.com/docs/code-connect/swiftui/ — the mechanism is a `<Component>.figma.swift`
connection file mapping the Figma node URL ↔ the SwiftUI view + prop/variant values, published to Dev
Mode via `figma connect publish`.

## Immediate code fixes the founder called out (fix in CODE, then regenerate Figma)
- `DailyBriefCard`: it's the brief entry point; formalize its states (Read/Stop/Read again/Retry) as a
  Code Connect component. The Agenda "Wednesday Evening" header is too big — reduce it there.
- Agenda "Read latest brief" icon: code uses an SF Symbol; the **emoji is a Figma-only artifact** — it
  disappears once Figma is generated from code.
- Menus not yet inventoried: the **add-button menu** and the **left menu on the bottom toolbar**.

## Figma plugin API gotchas (these cost the most round-trips — read before building)
1. **Empty auto-layout spacer frames default to 100px tall and don't shrink.** Never use spacer frames
   for alignment — use `parent.primaryAxisAlignItems='SPACE_BETWEEN'`. (This broke the inspector
   toolbar + rows on first build.)
2. **`resize()` locks that axis to FIXED.** Re-set `primaryAxisSizingMode`/`counterAxisSizingMode` to
   `'AUTO'` after a resize if you want hug-contents.
3. **Fresh `createText()` + SF Pro fontName throws "wdth is not a valid variation setting"** (SF Pro is
   a variable font). Fix: **clone an existing text node** and set `characters`/`fontSize` only; never
   reassign `fontName` to a named SF Pro style.
4. **`appendChild()` returns null/undefined.** Capture the node in a var first; use a
   `place(parent,node,x,y)` helper. Never chain `parent.appendChild(mk()).x = …`.
5. **`setBoundVariableForPaint` drops your `opacity`.** Set it AFTER binding:
   `paint = {...boundPaint, opacity: 0.12}`. (Status pills / tints render solid otherwise.)
6. **Screenshots cache.** Re-screenshot the **specific node** (not the parent) to bust it; verify
   layout via property reads (`x/y/width/height`), not just the image — a "clipped" render was stale
   while the geometry was actually fine.
7. **COMPONENT_SET can't hold non-COMPONENT children.** Variant footers/slots go **inside each variant
   COMPONENT**; add cross-variant props via `set.addComponentProperty(name, 'BOOLEAN'|'INSTANCE_SWAP',
   default)` then `node.componentPropertyReferences = {visible: propId}`.
8. **SF Symbols:** look up PUA codepoints in the community file `eMocgqr193EB694SlKtYZP` by **spatial
   pairing** — find the TEXT node whose `characters` === the symbol name, then the nearest glyph node
   (`codePointAt(0) >= 0xE000`). Naming is `battery.100percent`, not `battery.100`. Verified this
   session: chevron.left `100189` / down `100188` / up `100187` / right `10018a` / up.down `10018f`;
   circle `100000`; checkmark.circle.fill `100063`; calendar `100249`; iphone `1007dc`;
   battery.100percent `1006e8` / .25percent `1006e9`.
9. **Variables:** `getLocalVariablesAsync()` → name→Variable map; names are `label/primary`,
   `fill/tertiary`, `brand/blue`, `system/green`, `separator`, `background/primary|secondary`, etc.
10. **Reorg/pages:** rename via `page.name`; reorder via `figma.root.insertChild(i, page)`; move a
    frame across pages via `targetPage.appendChild(node)` (load both pages first); can't delete the
    current page.

## Process learning (the "misdirection" the founder felt)
Design→code reconciliation is intrinsically round-trippy: you build in Figma, screenshot, discover it
drifted from code, fix, re-screenshot. That loop **is** the misdirection. Code-first removes it: the
code is authored/cleaned once, Code Connect binds it, and the Figma library is generated — no chase.
The reconciliation work already done is not wasted: it validated the correct targets, found the real
drift (TaskEventRow, DailyBriefCard emoji/title), and produced the component inventory the code-first
library should contain.
