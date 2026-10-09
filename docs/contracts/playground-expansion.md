# Playground expansion bootstrap

This isolated UI playground starts from Settings snapshot
`b0fb1abde3ec70033ed3b6055e83ab0c477d13d2` on
`codex/playground-expansion`. It does not modify the Settings review branches or
PRs #80, #81, and #82, or the production Rem app.

The first bounded lanes are **Agenda Suggestions** and **Onboarding Voice**.
Implement approved source compositions with native SwiftUI and canonical Compose
components, shared semantic tokens, and deterministic local fixtures. Reuse
existing components and the shared Voice content where their contracts fit;
Onboarding owns its own surrounding navigation and actions. Existing Settings
source and behavior remain unchanged. No backend, authentication, real audio,
service integration, or production rollout is authorized by this bootstrap.

## Dispatch boundary

Factory Builder uses only `codex/playground-expansion` with task branches named
`agent-factory/playground-issue-<issue>`. Manual dispatch retains the exact-ref
preflight and requires exactly one existing open draft task PR targeting this
base. The Factory pin remains
`8522d84d9cd429c9b8cb9d028cf665a6136403c0`; model choices, budgets, revision caps,
required review, and delivery gates remain unchanged. Figma writing stays
disabled and automatic promotion stays disabled. The root coordinator owns
GitHub issues, seeded draft PRs, and dispatch; this bootstrap does not dispatch.

## Source and evidence contract

The approved read-only snapshot is versioned in
[expansion-design](../playground/expansion-design/README.md), including raw context,
PNGs, exact source identities and fixture boundaries. Read its COVERAGE and
BUILDER-BOUNDARIES before implementation. The trusted machine contract is
`tools/design-sync/playground-source-contracts.json`; do not replace it with a
candidate branch's source claims. The archive is source evidence, not native
runtime proof.

Exactly one PR label is required: `delivery-scope:agenda-suggestions` or
`delivery-scope:onboarding-voice`. These labels are admitted only for the expansion
base and numeric task prefix above, with an open same-repository draft PR. Existing
Settings manual pairs remain bound to `settings-foundation`.

`tools/render-evidence/contracts.json` defines three exact Figma references per
lane. Its `runtimeGroups` require dark, large-text and journey captures when the
lane or shared host changes. Capture names are case-insensitive delivery keys;
use the names below in native tests:

| Lane | Canonical reference captures | Additional native captures |
|---|---|---|
| Agenda Suggestions | `AgendaSuggestions-inline-light`, `AgendaSuggestions-overflow-light`, `AgendaSuggestions-none-light` | `AgendaSuggestions-dark`, `AgendaSuggestions-large-text`, `AgendaSuggestions-added-light`, `AgendaSuggestions-moved-light`, `AgendaSuggestions-dismissed-light`, `AgendaSuggestions-last-removal-light`, `AgendaSuggestions-restored-light` |
| Onboarding Voice | `OnboardingVoice-light`, `OnboardingVoice-chooser-light`, `OnboardingVoice-selected-light` | `OnboardingVoice-dark`, `OnboardingVoice-large-text`, `OnboardingVoice-chooser-preview-light`, `OnboardingVoice-sliders-adjusted-light`, `OnboardingVoice-continue-light`, `OnboardingVoice-skip-light`, `OnboardingVoice-back-light` |

Both platform host test targets run automatically for the exact expansion PR pair.
New iOS test source files must be registered in the checked-in Xcode project;
Android instrumented classes are discovered by the existing demo test target.
Capture callback outcomes as explicit local host boundaries, not invented downstream
screens. Keep existing Settings test coverage and capture paths intact.

Read-only Figma verification checks page/section/root identity, direct ancestry,
source copy and order, canonical instances, Voice's disabled conversation entry,
and the four canonical Agenda slots' 24-point top padding with zero parent gap.
Documentation-instance and prototype gates are not required for these two authored
source scopes; their absence does not authorize invented product navigation.
The publisher retains exact-head, trusted-workflow, media-digest and current-PR
bindings and additionally matches the source-contract digest to the trusted base.

## Remaining delivery work

This control-plane diff needs review and promotion to the isolated base before
candidate evidence can be accepted. The lanes still need implementation, actual
native journeys, authenticated live Figma verification, and independent code and
visual review. Missing or stale evidence remains blocking. REST source verification
fails closed if a required source property cannot be read. Existing Settings proof
does not prove these new lanes; no build or live verification has run in this task.

## Exclusions and unresolved decisions

- Connectors is pending reconciliation of the source consent/provider mismatch.
- Check-in and Automations are excluded while their rules remain unsettled.
- Inspector with unsettled rules, Chat, and the Plain proposal are excluded.
- No Figma writes, new product decisions, production app changes, backend rewrite,
  scheduled-conversation feature, or CallKit work is included.

This is a reviewable bootstrap only, not a claim that either lane is implemented,
build-ready, visually verified, or released.
