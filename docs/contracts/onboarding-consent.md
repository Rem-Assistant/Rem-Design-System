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
| **error** | notice card directly **above** the CTA | "Try again" |

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

## System use

- **Reuse:** `OnboardingScaffold` (bottom-pinned CTA bar), the grouped `ListRow` treatment, page
  sheets + nav (`LegalDocumentView`), the primary `RemButton`, the app-icon-style contained-icon
  treatment for the hero, tokens.
- **Exact:** the top-lockup order, the two legal rows (icons + copy + chevrons, in order), the
  bottom-pinned CTA, the footnote, the shield-lock hero, the icon registry rows + FILL.
- **Adaptable:** per-platform safe-area handling; the exact hero-badge size; the native sheet
  presentation idiom (iOS page sheet ↔ Android modal/full sheet).
- **Excluded:** other onboarding screens (own contracts); the legal copy body (owned by
  `LegalDocumentView`); Figma authoring; the sequencer chrome itself.

## Parity acceptance (what the visual gate diffs)

The paired render passes when iOS and Android match on **all** of:

- [ ] Hero = shield-lock badge (`lock.shield.fill` / `shield_lock`, FILL 1), same placement
- [ ] Title + body: same text, same type roles, centered
- [ ] Legal list: grouped card, two rows in order (Terms, then Privacy), same leading icons (outline)
      + same copy + trailing chevrons
- [ ] CTA "Accept and Continue" bottom-pinned, full-width filled, same treatment on both
- [ ] Footnote present below the CTA, same copy + role
- [ ] Error notice (when present) directly above the CTA on both
- [ ] Icons match the registry glyph **and FILL** on both

**Evidence:** `screenshots.yml` paired table — `Consent-default-light`, `Consent-default-dark`,
`Consent-error-dark`, plus the two sheets `Consent-terms-light`, `Consent-privacy-light` — iOS ⟷
Android side by side.

**Amendment path:** founder for arrangement / product decisions; Builder may propose a bounded
amendment on the issue. **Status:** drafted from the reference frame + onboarding packet; the paired
render is the proof the founder spot-checks. Build behind the live visual-parity gate.
