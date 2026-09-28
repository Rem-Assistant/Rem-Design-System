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
4. **Legal list** — an inset **grouped card** (`Color.secondarySystemGroupedBackground`, radius
   `medium`), full width, `Spacing.lg` below the body, two tappable rows split by an inset divider:
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
| **loading** | after accept: CTA shows spinner, disabled, 40% opacity, non-interactive (matches sign-in `checking`) | — |
| **retryable error** | failed consent submission; notice card directly **above** the CTA | "Try again" |

**Notice card:** `Color.systemRed` @ 12% fill, radius `medium`, leading warning icon
(`exclamationmark.triangle.fill` / `error`, FILL 1) + `Typography.caption1` message.

## Rules (the decisions that are easy to drift on — stated once)

- **Bottom-pinned CTA:** this step uses the sequencer's bottom-pinned CTA bar. Contrast sign-in
  (centered, no bottom bar). The two screens differ on purpose; each is identical *across platforms*.
- **Notice sits adjacent to the primary action, on the side that keeps both on-screen:** *below*
  centered buttons (sign-in), *above* a bottom-pinned CTA (consent). Same principle, placement follows
  the CTA. (Promote to the format if a third screen repeats it.)
- **Legal rows open page sheets** — never push a nav screen; 1:1 with `LegalDocumentView`.
- **Consent hero is the shield-lock** (`lock.shield.fill` / `shield_lock`, FILL 1) — not a
  shield-check (`Security`), not a plain shield.
- **Row icons are outline (FILL 0)** on both platforms: `doc.text` / `description`, `shield` / `shield`.
- **Glyph parity is semantic and rendered, while each platform stays native.** SwiftUI resolves the
  registry mapping through SF Symbols inside `ContainedIcon`; Compose resolves the paired Material
  Symbols codepoint and FILL through its static outlined/filled fonts inside the same design-system
  primitive. This backend difference is approved and hidden behind `ContainedIcon`. The registry row,
  FILL value, size, color, and paired current-head renders are the cross-platform contract.
- **The error state is retryable by construction.** It represents a failed consent submission and
  always keeps the notice, "Try again" CTA, and original accept action together. A terminal,
  authorization, or recovery state must be modeled separately; it must not reuse this state.

## System use

- **Reuse:** `OnboardingScaffold` (bottom-pinned CTA bar), the grouped `ListRow` treatment, page
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
`Consent-default-light`, `Consent-error-light`, `Consent-loading-light`,
`Consent-terms-light`, and `Consent-privacy-light`, including each platform's intentional
native presentation. The Director clarified on 2026-09-28 that separate dark frames are
redundant: the documented nodes stay in light mode and reviewers can switch the shared
color-variable mode from the right panel. Reuse canonical components and shared variables,
and keep the Terms / Privacy interactions working in Present mode. Include screen, flow,
component/preview links, exported renders for every required state, and the reused/new
component ledger in the PR.

The page is organized with the existing `Mobile Flow (Detach This)` template in section `695:138`.
It is the page's only top-level section. Every rendered screen is a child of the corresponding
`Mobile Placeholder` frame's `Device / Screen slot`; screens must never be aligned as loose overlay
siblings. The main row contains the two legal branches. A second row uses the same placeholders for
the light loading and submit-failure system states. There is no separate consent-action showcase.

The retry notice is a state of the consent action after Accept, not another step in the privacy
flow. It remains adjacent to the retry CTA in its full-screen Mobile Placeholder, while the primary
flow contains only Consent, Terms, and Privacy destinations.

Reviewer must inspect the Figma exports and structure against the current iOS/Android
evidence and this contract. Existing frame links alone do not establish that the designs
were updated or verified. Missing authoring access is a Steward capability blocker,
not permission to omit Figma or spend repeated screen-revision attempts.

### Figma/code source linkage

`tools/design-sync/manifest.json` is the machine-readable source link for each editable node. The
default `609:3`, loading `700:109`, and submit-failure `700:147` nodes map to
`OnboardingConsentTemplate.swift`; Terms `638:28` and Privacy `638:65` map to
`LegalDocumentTemplate.swift`. The template owns the centered title, 44pt
Done affordance, divider, scroll region, spacing, and token bindings. The shipping host owns sheet
presentation and injects the current legal body; render-only legal prose remains in test fixtures.
The Figma sheet nodes mirror that owned chrome and use representative body copy only for layout.

## Parity acceptance (what the visual gate diffs)

The paired render passes when iOS and Android match on **all** of:

- [ ] Hero = shield-lock badge (`lock.shield.fill` / `shield_lock`, FILL 1), same placement
- [ ] Title + body: same text, same type roles, centered
- [ ] Legal list: grouped card, two rows in order (Terms, then Privacy), same leading icons (outline)
      + same copy + trailing chevrons
- [ ] CTA "Accept and Continue" bottom-pinned, full-width filled, same treatment on both
- [ ] Footnote present below the CTA, same copy + role
- [ ] Loading CTA shows a spinner, is disabled, and renders at 40% opacity on both
- [ ] Error notice (when present) directly above the CTA on both
- [ ] Icons match the registry glyph **and FILL** on both

**Evidence:** `screenshots.yml` paired table — `Consent-default-light`,
`Consent-loading-light`, `Consent-error-light`, plus the two sheets `Consent-terms-light`,
`Consent-privacy-light` — iOS ⟷ Android side by side. The PR's Figma table must contain a current
export and editable node link for the same five states; the design-drift run must compare all five
registered Figma/code pairs at the PR head. CI passes those five exact basenames through
`compare.mjs --require`, then publishes `artifacts/design-drift-report.json` in the job summary and
as a downloadable workflow artifact. Missing or unmatched required states fail closed.

**Approved drift calibration (2026-09-28):** `pixelmatch --threshold=0.3` with
`--maxDiffRatio=0.10`. The per-pixel threshold is a YIQ color-distance tolerance, not permission for
30% of the screen to differ. The changed-area ceiling remains 10%; a current five-state hosted run
must replace the retired dark-frame baseline in `tools/design-sync/baselines/consent-2026-09-28.json`
before merge. Changing either threshold still requires a fresh baseline plus founder and Reviewer
approval. The pixel gate catches material drift while Reviewer owns the paired visual decision.

**Amendment path:** founder for arrangement / product decisions; Builder may propose a bounded
amendment on the issue. **Status:** drafted from the reference frame + onboarding packet; the paired
render is the proof the founder spot-checks. Build behind the live visual-parity gate.
