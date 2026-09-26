# Task: Onboarding template — first-run journey

## Outcome
- **User outcome:** a clean first run — sign in, consent, connect apps, set up voice — with **no gateway
  deploy step** (the runtime migration removes it).
- **Scope:** the Onboarding screen template (composed states), reusing existing surfaces.
- **Mode:** Reproduce (of the *intended* flow — design + code move together per the mirror-the-future ruling).
- **Source concern:** `TARGET-COMPONENTS.md §Gaps #6`; the "first end-to-end journey" the other agent named.

## Design authority
- **Product authority:** `Rem/Sources/Onboarding/OnboardingFlow.swift` (signIn → dataSharingConsent →
  deploying⚠️ → activation) + `AIDataSharingConsentView`. **Future target** (mirror-the-future): the
  `Proposed · Onboarding` Figma page (Connect apps → Set up voice) over the real Connectors + Voice
  surfaces. The gateway `deploying` step is **deprecating** — do not design it fresh.
- **Task artifact / evidence:** founder onboarding recording —
  https://samuelalake.com/projects/rem-evidence/rem-onboarding-web.mp4 (authoritative source for the
  target flow). The consent flow is already rebuilt in Figma (Terms/Privacy as iOS page sheets, 1:1 with
  `.sheet` + inline nav title in `LegalDocumentView`) — reuse it.
- **Design-system authority:** kit page sheets + nav (never hand-build), `Button` (SignInButton variant),
  `ContextualMessage`, `ListRow` (connectors), tokens.
- **Conflicts resolved:** deploy screen retired; "How Rem Works"/RemUI Permissions value-props retired
  (stale, no callers). Real permissions = system prompts + Settings→Permissions.

## Approved experience
- **Composition (target):** Sign in (logo + "Rem" + "Turn your thoughts into actions" + Apple/Google) →
  Data-sharing consent (page sheets) → Connect your apps (Connectors) → Set up your voice (Voice).
- **States:** each step's default / loading / error; consent Terms + Privacy sheets; coach-mark overlay
  (GuidedFlow) spotlighting each step.
- **Interaction:** provider sign-in, sheet present/dismiss (Back), step advance; recovery on sign-in error.

## System use
- **Reuse:** the built consent flow, kit sheets/nav, Connectors + Voice settings surfaces, tokens.
- **Extension:** the Connect-apps → Set-up-voice first-run (assembled from existing surfaces).
- **Excluded:** the deploy/provisioning step (migration); RemUI value-props.

## Delivery contract
- **Known constraints:** deploy-step removal is a staged migration slice in code — design mirrors the
  no-deploy future now; code removal lands with the migration's *Product cleanup*, not this design task.
- **Acceptance:** the target flow as a composed template with states; consent flow matches `LegalDocumentView`.
- **Evidence:** iOS + Android, light + dark, per step + the two consent sheets; recording of the flow.

## Approval
- **Status:** needs decision (confirm the target step order vs the recording) then Build.
- **Open decisions:** does a permissions primer survive in the future flow, or only system prompts? — founder.
