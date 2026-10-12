# Contract — WalletScreen (payment-methods proposal)

**Outcome:** a payment-methods screen — **not** a balance or credits ledger — that lists the providers a
person can link for the agent to use when purchasing, with the same arrangement on iOS and Android.
**Mode:** Design (Wave 4 proposal). The shipping Settings → Wallet destination is
`SettingsWalletScreen` (see the settings contracts), not this template.

**Authority:** the reference Wallet screen (Settings → Wallet; "Securely save payment methods for
{agent} to use when making purchases for you"; Link by Stripe / Shop Pay). Written from the code on both
platforms (`Screens/WalletScreen.swift`, `screens/WalletScreen.kt`); differences are listed as questions.

## Layout (top to bottom, centred)

1. **Optional back control** (only when the host passes `onBack`): a 36 circle on `backgroundSecondary`
   with a left chevron in `labelPrimary`, leading, inset `Spacing.lg` / top `Spacing.md`.
2. **Hero:** `ContainedIcon` wallet glyph, `fill: .subtle`, `size: .large`, `Spacing.xl` from the top;
   the title "Wallet" in title1 bold; the subtitle "Securely save payment methods for <agentName> to use
   when making purchases for you." in subheadline `labelSecondary`, centred, `Spacing.sm` under the title.
3. **Provider list:** one `RemSection`; each row is a 32 tile (`CornerRadius.small`, the provider's tile
   colour, a white glyph), the provider name in body (`labelPrimary`, or `labelSecondary` when coming
   soon), then the trailing action. Rows are inset `Spacing.md`; dividers are inset
   `Spacing.xxl + Spacing.md` and omitted after the last row.

## States (per provider, `WalletProviderAction`)

| Action | Trailing | Name colour |
|---|---|---|
| `add` | "Add" in `brandBlue`, emphasised; calls the host | `labelPrimary` |
| `comingSoon` | "Coming soon" in body `labelTertiary`, not interactive | `labelSecondary` |

## Scope boundary

No payment method is stored, linked or validated by the DS; `add` reports intent only. Provider tile
colours and glyphs are placeholders until real logo assets land (logo debt); the brand-colour literals in
`referenceProviders()` / `walletReferenceProviders()` are part of that debt.

## Contract questions (platform differences; not rules)

1. **Back control.** Compose labels it "Back" and uses a 22 dp chevron; iOS uses a 16 pt semibold
   `chevron.left` with no explicit label. iOS should carry the "Back" label.
2. **"Add" control.** iOS is a plain `Button` (semibold, the text is the target); Compose is clickable
   text (bold) with `Spacing.xs` padding and no button role. Pin the weight, the role and a 44 target.
3. **Hero glyph.** iOS `wallet.pass`; Compose `Icons.Filled.AccountBalanceWallet`. The icon registry pairs
   `wallet.pass` with the Material Symbol `wallet` (`F8FF`).
4. **Spacing above the list.** iOS: `Spacing.lg` stack spacing plus `Spacing.md` section padding (28);
   Compose: `Spacing.xl` (24).
5. **Provider glyph size.** iOS 16 pt semibold; Compose 18 dp.
6. **Figma master.** `REGISTRY.md` maps WalletScreen to master `1075:121` (doc `1079:2`) and describes a
   balance hero with Add funds, usage and budget meter. Both platforms now build the payment-methods screen
   above; the master, or this contract, needs to be reconciled.

## Proof

- Renders: `WalletScreen-light` on both platforms; Compose also renders `WalletScreen-dark`.
- **Gap:** no journey drives this template (the Wallet journeys cover `SettingsWalletScreen`).

## Parity acceptance

- Hero centred above one grouped provider list; Add only on linkable providers; Coming soon not
  interactive and visibly quieter.
