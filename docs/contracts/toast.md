# Contract — RemToast

**Outcome:** one brief, non-actionable status capsule with the same tone, anatomy, lifetime and
accessibility on iOS and Android. **Mode:** Systemize.

**Authority:** Figma `Toast` `72:24` (properties `Variant`, `Message`). Written from the code on both
platforms (`Feedback/RemToast.swift`, `feedback/RemToast.kt`); differences are listed as questions.

## Anatomy

A capsule on `pillBackground`, inset `Spacing.md` horizontally and `Spacing.sm` vertically: the tone's
filled glyph, `Spacing.sm`, then the message in footnote `labelPrimary` (wraps). No buttons, ever.

## States (`RemToastVariant`) — the tone owns glyph and tint

| Variant | iOS glyph | Compose glyph | Tint |
|---|---|---|---|
| info (default) | `info.circle.fill` | `Icons.Filled.Info` | `systemBlue` |
| success | `checkmark.circle.fill` | `Icons.Filled.CheckCircle` | `systemGreen` |
| warning | `exclamationmark.triangle.fill` | `Icons.Filled.Warning` | `systemOrange` |
| error | `xmark.circle.fill` | `Icons.Filled.Cancel` | `systemRed` |

Callers supply only the message; they cannot restyle the capsule.

## Lifetime

Visible on appearance; after the duration (default **4 s**) it hides and calls `onDismiss` once. A new
message restarts the timer.

## Use

Transient feedback (a saved result, a recoverable failure). Never information that must stay visible and
never anything the person must act on. Current use: the onboarding check-in failure
(`OnboardingCheckinTemplate` / `OnboardingScaffold`).

## Contract questions (platform differences; not rules)

1. **Announcement.** Compose marks the capsule as a polite live region, so TalkBack announces it without
   focus. iOS combines it into one element labelled with the message but posts no announcement, so
   VoiceOver reads it only if focus reaches it, although the iOS doc comment says it is "announced". iOS
   should post an announcement (or the contract should drop the claim).
2. **Restart key.** iOS restarts on a new message; Compose restarts on a new message, variant or duration.
3. **Glyph size.** iOS 14 pt semibold; Compose 18 dp.
4. **Motion.** iOS fades and moves from the bottom on hide (0.2 s ease-out) without a Reduce Motion
   check; Compose removes it without animation.

## Proof

- **Gap:** no render and no unit test on either platform; covered only indirectly by the onboarding
  check-in failure renders (`Checkin-failure-light` pair) and their journeys.

## Parity acceptance

- Same tone → glyph meaning → tint table; same 4 s default; one element whose spoken text is the message.
