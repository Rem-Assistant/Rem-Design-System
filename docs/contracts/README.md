# Screen contracts

A **screen contract** is the platform-neutral source of truth for how one screen is
*arranged* — the layer the design system was missing. The system already keeps
*components* consistent (one `RemButton`, one `ContainedIcon`, one `RemAppIcon`); a
contract keeps their **arrangement** consistent, so iOS (SwiftUI) and Android (Compose)
can't drift on placement, order, emphasis, or icon choice.

> **Why this exists.** Consistent components with no arrangement contract still drift:
> the same button lands centered on one platform and bottom-pinned on the other, an error
> card floats above on one and below on the other, a secondary link is loud on one and
> quiet on the other. Every such gap is a missing *arrangement decision*, not a broken
> component. The contract names those decisions once; both platforms build to it; the
> render loop proves parity.

## How a contract is used

1. **Design** writes/approves the contract (this folder). It is platform-neutral.
2. **Builder** implements it on each platform — identical arrangement, real components/tokens.
3. **Reviewer (visual gate)** diffs the paired render against the contract's *Parity
   acceptance* list: arrangement, order, emphasis, icons + FILL. Drift is the Reviewer's
   to catch, not the founder's.

## Contract format (every screen uses this)

- **Outcome / mode** — what the screen does; Reproduce / Extend / Design / Systemize.
- **Authority** — the shipping behavior + reference frame the contract reproduces.
- **Layout** — the single arrangement both platforms match: regions, order, alignment,
  vertical placement, spacing (in design tokens, never raw px).
- **States** — every state (default / empty / loading / error / success / disabled /
  permission / recovery) with its content + button treatment.
- **Rules** — the arrangement/emphasis decisions that are easy to drift on, stated once.
- **Icons** — resolved through the shared [icon registry](./icon-registry.md), FILL pinned.
- **Reuse / Exact / Adaptable / Excluded** — what the Builder must reproduce vs may adapt.
- **Parity acceptance** — the machine-checkable list the visual gate diffs on.

Contracts are the unit of "don't overfit": a fix belongs in the *format* or the *registry*,
not hand-patched into one screen.
