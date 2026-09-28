# Contract — Onboarding · Sign-in

**Outcome:** iOS + Android sign-in render *identically* — same arrangement, emphasis, and icons —
provable in one paired render. **Mode:** Reproduce (returning state) + Extend (new / checking / error
/ recovery) + Systemize (first instance of the [screen-contract format](./README.md)).

**Authority:** shipping `OnboardingFlow.signInContent` + `SignInButton` + `OnboardingLogoView`
(remclaw), and the founder reference frame `tasks/refs/onboarding/01-sign-in.png` (returning state).
Approved by the founder (arrangement + icon rule), 2026-09-28.

## Layout — one centered block, identical on both platforms

A single block **centered in the safe area** (equal space above and below — **never**
bottom-pinned), contents **left-aligned**, max content width 560. Top → bottom, with the lockup
group using `Spacing.md` between members:

1. `RemAppIcon` — 40pt, corner radius `CornerRadius.small`
2. **"Rem"** — `Typography.largeTitle` semibold, `Color.labelPrimary`
3. **"Turn your thoughts into actions"** — `Typography.title1`, `Color.labelSecondary`, 2 lines,
   `Spacing.xs` below the title
4. **Action group** — full-width buttons, `Spacing.md` below the lockup
5. **Notice card** (error / recovery only) — directly **below** the action group, `Spacing.md` gap

## States

| State | Primary (filled `Color.buttonBackground`, `bodyBold` inverted label, radius `medium`, full-width) | Secondary | Notice |
|---|---|---|---|
| **returning** | `Continue as {name}` · leading Apple mark | "Sign in with a different account" — quiet text link | — |
| **new** | `Continue with Google` · leading Google "G" **(untinted)** → then `Continue with Apple` · leading Apple mark | legal footnote (`Typography.caption1`, `labelSecondary`, centered) | — |
| **checking** | `Signing in…` · leading spinner, disabled, 40% opacity, non-interactive | — | — |
| **error** | `Try again` (no leading icon, centered label) | "Sign in with a different account" — quiet text link | notice below buttons |
| **recovery** | `Try again` (no leading icon, centered label) — re-auth is the primary action | "Sign in with a different account" — quiet text link | notice below buttons |

**Notice card:** `Color.systemRed` @ 12% fill, radius `medium`, leading warning icon (registry:
`exclamationmark.triangle.fill` / `error`, FILL 1) + `Typography.caption1` message.

## Rules (the decisions that drifted — stated once)

- **Emphasis:** "Sign in with a different account" is *always* a quiet `labelSecondary` text link —
  never filled or emphasized, in any state. Emphasis is reserved for the primary provider action.
- **Notice placement:** the error/recovery notice card sits *directly below the action group*,
  never up in the lockup.
- **Provider order:** Google, then Apple (matches the shipping app).
- **Legal footnote:** shown in the `new` state only (the account-creation moment) — *not* in
  returning / checking / error / recovery. `Typography.caption1`, `labelSecondary`, centered.
- **Vertical placement:** the block is centered, not bottom-pinned — on both platforms.

> **Amendment 2026-09-28 (#27) — founder-confirmed.** An earlier draft of the states table listed
> "Sign in with a different account" as recovery's *filled primary*, which directly contradicted the
> emphasis rule above (and issue #27's acceptance criterion that different-account is *always* a quiet
> link). Resolved in favour of the emphasis rule — the invariant #27 exists to enforce: recovery's
> filled primary is **"Try again"** (re-auth), matching the error state, and "Sign in with a different
> account" stays the quiet link. **Confirmed by the founder 2026-09-28** ("proceed with your
> recommendations"); the paired render is the spot-check, not a Builder-only decision.

## System use

- **Reuse:** `RemAppIcon`, the Google "G" asset, the filled provider-button treatment (`SignInButton`
  parity: `buttonBackground` fill, `medium` radius, `bodyBold` inverted, 18pt leading glyph), tokens.
- **Extension:** sign-in becomes its *own centered screen* on both platforms — it does **not** use the
  multi-step flow scaffold's bottom-pinned CTA bar (this is the arrangement fix). Add Material Symbols
  to the Compose module so the icon registry resolves on Android.
- **Exact:** arrangement order, centered placement, button order, secondary-as-quiet-link,
  notice-below-buttons, the icon registry rows + FILL.
- **Adaptable:** the exact vertical-centering offset (nudge to match the reference optically);
  per-platform safe-area handling.
- **Figma:** the canonical Sign-in screen (`411:15`) uses the real `RemAppIcon` raster at `411:28`
  and the repository's editable Google SVG at `765:237`. Reusable component masters, remaining
  states, and broader Sign-in Figma authoring stay in #40.
- **Excluded:** other onboarding screens (own contracts); the sequencer chrome.

## Parity acceptance (what the visual gate diffs)

The paired render passes when iOS and Android match on **all** of:

- [ ] Mark = `RemAppIcon` (app icon, not the face), same size/placement
- [ ] Title + tagline: same text, same type roles, left-aligned
- [ ] Block is centered (not bottom-pinned) on both
- [ ] Same buttons, same order (Google→Apple in `new`), same primary treatment
- [ ] "Different account" is a quiet text link on both (emphasis rule)
- [ ] Notice card present in error/recovery, **below** the buttons on both
- [ ] Icons match the registry glyph **and FILL** on both

**Evidence:** `screenshots.yml` paired table — `SignIn-returning-light`, `SignIn-new-light`,
`SignIn-checking-light`, `SignIn-error-dark`, `SignIn-recovery-*` — iOS ⟷ Android side by side.

**Amendment path:** founder for arrangement/product decisions; Builder may propose a bounded
amendment recorded on the issue. **Status:** approved for Build (arrangement + icons); gate must be
live before the Builder's render is judged.
