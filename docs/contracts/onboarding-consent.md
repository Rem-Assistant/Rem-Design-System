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

## Rules (the decisions that are easy to drift on — stated once)

- **Bottom-pinned CTA:** this step uses the sequencer's bottom-pinned CTA bar. Contrast sign-in
  (centered, no bottom bar). The two screens differ on purpose; each is identical *across platforms*.
- **Legal rows open page sheets** — never push a nav screen; 1:1 with `LegalDocumentView`.
- **Consent hero is the shield-lock** (`lock.shield.fill` / `shield_lock`, FILL 1) — not a
  shield-check (`Security`), not a plain shield.
- **Row icons are outline (FILL 0)** on both platforms: `doc.text` / `description`, `shield` / `shield`.
- **Do not invent consent-local loading or error states.** Current shipping code advances immediately
  after acceptance. The old Deploying screen is being deprecated, so Accept has no fabricated
  destination until the replacement onboarding step is defined.
- **Dark mode is a variable-mode review, not a second authored frame.** The Director approved one
  light documentation set; reviewers switch the shared color mode in Figma when checking dark.

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
`Consent-default-light`, `Consent-terms-light`, and `Consent-privacy-light`, including each
platform's intentional native presentation. The Director clarified on 2026-09-28 that separate dark
frames are redundant because reviewers can switch the shared color-variable mode in Figma. Reuse
canonical components and shared variables. Include screen, flow, component/preview links and the
reused/new component ledger in the PR.

Reviewer must inspect the Figma exports and structure against the current iOS/Android
evidence and this contract. Existing frame links alone do not establish that the designs
were updated or verified. Missing authoring access is a Steward capability blocker,
not permission to omit Figma or spend repeated screen-revision attempts.

## Parity acceptance (what the visual gate diffs)

The paired render passes when iOS and Android match on **all** of:

- [ ] Hero = shield-lock badge (`lock.shield.fill` / `shield_lock`, FILL 1), same placement
- [ ] Title + body: same text, same type roles, centered
- [ ] Legal list: grouped card, two rows in order (Terms, then Privacy), same leading icons (outline)
      + same copy + trailing chevrons
- [ ] CTA "Accept and Continue" bottom-pinned, full-width filled, same treatment on both
- [ ] Footnote present below the CTA, same copy + role
- [ ] Icons match the registry glyph **and FILL** on both

**Evidence:** `screenshots.yml` paired table — `Consent-default-light`, `Consent-terms-light`, and
`Consent-privacy-light` — iOS ⟷ Android side by side.

**Amendment path:** founder for arrangement / product decisions; Builder may propose a bounded
amendment on the issue. **Status:** state/evidence scope amended by the founder on 2026-09-28; the
paired render is the proof the founder spot-checks. Build behind the live visual-parity gate.
