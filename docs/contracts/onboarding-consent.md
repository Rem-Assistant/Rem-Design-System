# Contract — Onboarding · Data-sharing consent

**Outcome:** iOS + Android render the consent step *identically* — same top lockup, same grouped
legal list, same bottom-pinned CTA, same icons + FILL. **Mode:** Reproduce (the shipped consent step
+ the founder onboarding reference frame `tasks/refs/onboarding/02-consent.png`).

**Authority:** shipping `OnboardingFlow.dataSharingConsent` + `AIDataSharingConsentView`, with
Terms / Privacy presented as page sheets 1:1 with `LegalDocumentView` (remclaw). Reference frame:
`tasks/refs/onboarding/02-consent.png` (default state). This is a step **inside** the onboarding
sequencer — it carries the scaffold's back-nav and **bottom-pinned CTA**, unlike sign-in, which is
its own centered screen. That difference is intentional, not drift.

## Layout — top lockup, grouped legal list, bottom-pinned CTA

A scaffolded step (sequencer chrome: a back chevron in the nav). Content top → bottom, **centered**:

1. **Hero badge** — the app-icon-style rounded square, `Color.systemBlue` fill, radius
   `CornerRadius.medium`, holding a centered white `lock.shield.fill` glyph. Centered near the top of
   the content area.
2. **Title** — "Privacy by design" — `Typography.largeTitle` semibold, `Color.labelPrimary`, centered,
   `Spacing.md` below the badge.
3. **Body** — "Rem uses your data to answer you and act on the things you ask. You can review or delete
   it anytime in Settings." — `Typography.body`, `Color.labelSecondary`, centered, `Spacing.xs` below
   the title, max content width 560.
4. **Legal list** — an inset **grouped Section** (`Color.secondarySystemGroupedBackground`, radius
   `xlarge` / 24, no outer stroke), full width, `Spacing.lg` below the body, two tappable rows split by an inset divider:
   - **Terms of Service** — leading `doc.text` in a rounded neutral tile; title `Typography.bodyBold`
     `labelPrimary`; subtitle "How Rem accounts, subscriptions, and approved actions work."
     (`Typography.caption1`, `labelSecondary`); trailing `chevron.right` (`labelTertiary`).
   - **Privacy Policy** — leading `shield`; title "Privacy Policy"; subtitle "What Rem, your gateway,
     and AI or voice providers process."; trailing `chevron.right`.
5. **Primary CTA** — "Accept and Continue" — full-width filled (`Color.buttonBackground`,
   `Typography.bodyBold` inverted label, radius `medium`), **pinned above the bottom safe area** (the
   lockup + list stay top-anchored; the CTA does not float up under the list).
6. **Legal footnote** — "By tapping \"Accept and Continue,\" you agree to our Terms of Service and
   Privacy Policy." — `Typography.caption1`, `Color.labelSecondary`, centered, `Spacing.xs` below the
   CTA.

## States

| State | Content | CTA |
|---|---|---|
| **default** | lockup + legal list + footnote | "Accept and Continue" (enabled) |
| **terms sheet** | `LegalDocumentView` for Terms as a page sheet (`.sheet` + inline nav title); Back dismisses | — |
| **privacy sheet** | `LegalDocumentView` for Privacy as a page sheet; Back dismisses | — |

## Rules (the decisions that are easy to drift on — stated once)

- **Bottom-pinned CTA:** this step uses the sequencer's bottom-pinned CTA bar. Contrast sign-in
  (centered, no bottom bar). The two screens differ on purpose; each is identical *across platforms*.
- **Legal rows open page sheets** — never push a nav screen; 1:1 with `LegalDocumentView`.
- **Consent hero is the shield-lock** (`lock.shield.fill` / `shield_lock`, FILL 1) — not a
  shield-check (`Security`), not a plain shield.
- **Row icons are outline (FILL 0)** on both platforms: `doc.text` / `description`, `shield` / `shield`.
- **Glyph parity is semantic and rendered, while each platform stays native.** SwiftUI resolves the
  registry mapping through SF Symbols inside `ContainedIcon`; Compose resolves the paired Material
  Symbols codepoint and FILL through its static outlined/filled fonts inside the same design-system
  primitive. This backend difference is approved and hidden behind `ContainedIcon`. The registry row,
  FILL value, size, color, and paired current-head renders are the cross-platform contract.
- **Do not invent consent-local loading or error states.** Current shipping code advances immediately
  after acceptance. The old Deploying screen is being deprecated, so the prototype intentionally
  leaves Accept without a fabricated destination until the replacement onboarding step is defined.
- **Dark mode is a variable-mode review, not a second authored frame.** The Director approved one
  light documentation set; reviewers switch the shared color mode in Figma when checking dark.

## System use

- **Reuse:** `OnboardingScaffold` (bottom-pinned CTA bar), `RemSection` + `ListRow`, page
  sheets + nav (`LegalDocumentView`), the primary `RemButton`, the app-icon-style contained-icon
  treatment for the hero, tokens.
- **Exact:** the top-lockup order, the two legal rows (icons + copy + chevrons, in order), the
  bottom-pinned CTA, the footnote, the shield-lock hero, the icon registry rows + FILL.
- **Adaptable:** per-platform safe-area handling; the exact hero-badge size; the native sheet
  presentation idiom (iOS page sheet ↔ Android modal/full sheet).
- **Excluded:** other onboarding screens (own contracts); the legal copy body (owned by
  `LegalDocumentView`); the sequencer chrome itself.

## Figma delivery (required)

The founder clarified on 2026-09-28 that Figma is part of a finished screen PR. This
supersedes the earlier Figma exclusion; the approved arrangement and product rules above
are unchanged. Follow [Figma delivery](../figma-delivery.md) and `SHAPE-OF-A-TASK.md`.

Update the editable consent screen and legal-sheet flow in the existing Rem file. Cover
`Consent-default-light`, `Consent-terms-light`, and `Consent-privacy-light`, including each platform's intentional
native presentation. The Director clarified on 2026-09-28 that separate dark frames are
redundant: the documented nodes stay in light mode and reviewers can switch the shared
color-variable mode from the right panel. Reuse canonical components and shared variables,
and keep the Terms / Privacy interactions working in Present mode. Include screen, flow,
component/preview links, exported renders for every required state, and the reused/new
component ledger in the PR.

The consent composition uses the domain-neutral `Lockup`, `ActionArea`, and `ButtonGroup` masters.
They were extracted because each owns a stable layout responsibility and has plausible consumers
beyond this flow; onboarding-specific copy remains on the screen instance. Theme (Light/Dark) and
Platform (iOS/Android) stay independent. Every full-device consent destination includes the canonical
`StatusBar` (`785:389`) and `NavigationIndicator` (`793:379`) with Platform switching, while the
documented composition remains Light for review. iOS and Android share the same product layout;
Platform changes native chrome, type metrics, and semantic icon sources. On legal destinations, the
status bar is behind the scrim and sheet rather than drawn above them.

The shared `Onboarding` page (`410:15`) uses a numbered Section pair for this flow:
`01A · Consent · Documentation` (`777:432`) and `01B · Consent · Prototype` (`731:260`). Sign-in
(`788:3183`), Privacy by design (`788:3184`), and the 402×874 consent Default (`777:248`), Terms
(`777:325`), and Privacy (`777:392`) masters are consolidated inside
`00 · Canonical screen components` (`760:21`). Later onboarding flows add `02A` /
`02B` pairs on the same page. Documentation remains an instance of `Mobile Flow Documentation`
(`769:282`) and composes nested Sections, Rows, Steps, Mobile Placeholders, and Screens through slots.
Every documented screen is an instance in the exact 402×874 `Screen` slot; no template layer is
detached. The one `Steps` slot contains Consent → Terms → Consent → Privacy with reusable arrow
instances and 24-point spacing. The second Consent instance is an approved branch-return navigation
waypoint between the Terms and Privacy paths; it reuses the Default master and is excluded from the
required evidence-state set. There is no consent-action or speculative system-state row.

Canonical screens use semantic top-level layer names (`StatusBar`, `Body`, `ActionArea`,
`NavigationIndicator`) and SwiftUI-first structural names (`VStack`, `HStack`, `ZStack`,
`LazyVStack`, `LazyHStack`). Compose translates those structures to `Column`, `Row`, `Box`,
`LazyColumn`, and `LazyRow`; it does not require a second layout tree. Auto-layout
`SPACE_BETWEEN` pins top and bottom regions without empty spacer frames.

Reusable compositions are responsive rather than fixed to the 354-point content width of the
reference iPhone. Figma, SwiftUI, and Compose make `Lockup` and `ActionArea` fill the available
parent width up to 560 points. Lockup text, the Button Group, and the wrapping Footnote fill that
capped region; fixed visuals keep their intrinsic size. The surrounding screen owns its horizontal
inset and background.

Reviewer must inspect the Figma exports and structure against the current iOS/Android
evidence and this contract. Existing frame links alone do not establish that the designs
were updated or verified. Missing authoring access is a Steward capability blocker,
not permission to omit Figma or spend repeated screen-revision attempts.

### Figma/code source linkage

`tools/design-sync/manifest.json` is the machine-readable source link for each editable node. The
Default `777:248` maps to `OnboardingConsentTemplate.swift`; Terms `777:325` and Privacy `777:392` map to
`LegalDocumentTemplate.swift`. The template owns the centered title, 44pt
Done affordance, divider, scroll region, spacing, and token bindings. The shipping host owns sheet
presentation and injects the current legal body; render-only legal prose remains in test fixtures.
The Figma sheet nodes mirror that owned chrome and use representative body copy only for layout.
`ConsentInteractionTests` invokes the same legal-row open and Done-dismiss endpoints used by the
production views. Present mode verifies the Terms/Privacy destinations and Back actions; the paired
renders verify the resulting sheet chrome. These three proofs are required together because a static
render alone cannot establish presentation or dismissal behavior.

## Parity acceptance (what the visual gate diffs)

The paired render passes when iOS and Android match on **all** of:

- [ ] Hero = shield-lock badge (`lock.shield.fill` / `shield_lock`, FILL 1), same placement
- [ ] Title + body: same text, same type roles, centered
- [ ] Legal list: grouped card, two rows in order (Terms, then Privacy), same leading icons (outline)
      + same copy + trailing chevrons
- [ ] CTA "Accept and Continue" bottom-pinned, full-width filled, same treatment on both
- [ ] Footnote present below the CTA, same copy + role
- [ ] Icons match the registry glyph **and FILL** on both

**Evidence:** `screenshots.yml` paired table — `Consent-default-light` plus the two sheets
`Consent-terms-light` and `Consent-privacy-light` — iOS ⟷ Android side by side. The PR's Figma table must contain a current
export and editable node link for the same three destinations; the design-drift run must compare all three
registered Figma/code pairs at the PR head. CI passes those three exact basenames through
`compare.mjs --require`, then publishes `artifacts/design-drift-report.json` in the job summary and
as a downloadable workflow artifact. Missing or unmatched required states fail closed.

**Approved drift calibration (2026-09-28):** `pixelmatch --threshold=0.3` with
`--maxDiffRatio=0.10`. The per-pixel threshold is a YIQ color-distance tolerance, not permission for
30% of the screen to differ. The changed-area ceiling remains 10%; a current three-destination hosted run
must replace the superseded baseline in `tools/design-sync/baselines/consent-2026-09-28.json`
before merge. Changing either threshold still requires a fresh baseline plus founder and Reviewer
approval. The pixel gate catches material drift while Reviewer owns the paired visual decision.

**Amendment path:** founder for arrangement / product decisions; Builder may propose a bounded
amendment on the issue. **Status:** state/evidence scope amended by the founder on 2026-09-28; the
paired render is the proof the founder spot-checks. Build behind the live visual-parity gate.
