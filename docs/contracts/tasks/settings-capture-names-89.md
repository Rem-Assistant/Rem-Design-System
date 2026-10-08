# Settings capture-name repair — issue 89

## Outcome

Repair screenshot filenames so the existing Settings gallery and legacy onboarding library captures coexist without duplicate or invalid normalized keys. Naming only: product behavior, assertions, capture content, and strict evidence validation stay unchanged.

Related work: Settings PRs #80–#82. Issue #84 reserves OnboardingVoice-light for the new onboarding runtime, so legacy library captures need a separate LegacyOnboarding prefix.

## Branch and execution

Use the existing seeded draft Factory route:
- Base: codex/settings-integration, verified at 972256263286031fd46525f12490bcf580f05c65.
- Task branch: agent-factory/settings-issue-89.
- Draft PR targeting codex/settings-integration.
- Existing agent-builder.yml, dispatched on codex/settings-integration with only issue and base_ref inputs.

Seed only a task-contract file if needed to open the draft. Do not seed from Settings PR81 head 207cb4b164b59d14cf841a6053d36e4f77f95c78, whose history diverges from integration.

## Three-file implementation boundary

1. compose/RemDesignSystem/src/test/kotlin/com/rem/designsystem/EvidenceSnapshots.kt
   Verified blob: 4e124ca583577cf090543f0d4007e1a7a339c24d.
   - Rename the legacy onboarding connectors snapshot from Connectors-light to LegacyOnboardingConnectors-light.
   - Rename the legacy onboarding voice snapshot from Voice-light to LegacyOnboardingVoice-light.

2. tools/render-swift/Tests/RenderSnapshotTests/RenderSnapshots.swift
   Verified blob: 5394eaaa8e4972310cef8595ca5d6c1416bd40cc.
   - Apply the same two legacy snapshot-label changes.
   - Update only the immediately adjacent pairing comment if necessary.

3. compose/demo/src/androidTest/java/com/rem/designsystem/demo/SettingsPlaygroundTest.kt
   Integration blob: 28f2266c011d6da43a4e59666509c5c6959206af.
   - At the retained-login capture, replace dots with hyphens only inside the screenshot-name interpolation.
   - Preserve the original domain value for navigation tags, fixture data, assertions, and all behavior.

Keep Settings runtime Connectors-light and Voice-light unchanged. Preserve every test case and capture invocation.

No other implementation edits, workflow/config changes, force pushes, validator weakening, evidence deletion, new infrastructure/access, Figma calls, merges, or deployments. Keep the PR draft. Do not propagate changes to PR81 or PR82.

## Evidence and acceptance

Authenticated baseline:
- [Screenshots run 37799337419](https://github.com/Rem-Assistant/Rem-Design-System/actions/runs/37799337419)
- Artifact 11561722747
- [Assembly job 113400927259](https://github.com/Rem-Assistant/Rem-Design-System/actions/runs/37799337419/job/113400927259)

Its complete media list contains 342 paths: SwiftUI 164 and Compose 178. Preserve the entire baseline inventory in verification.

The unchanged delivery.py normalizes Compose stems by stripping the EvidenceSnapshots class/method prefix, lowercases the key, allows only [a-z0-9-]+, and rejects duplicate keys per platform.

Baseline defects:
- Compose duplicates: connectors-light and voice-light, each shared by legacy library and Settings runtime captures.
- Invalid Compose names: CloudBrowser-clear-all-retained-login-github.com-light and CloudBrowser-clear-all-retained-login-notion.so-light.
- SwiftUI assembly copies runtime captures after library captures, overwriting the two old same-named library shots.

Required proof:
- Retain all 342 baseline captures under the four explicit Compose filename mappings.
- Restore both previously overwritten SwiftUI library captures under distinct LegacyOnboarding names. The modeled union is 344 images, but verify actual output rather than forcing that count.
- Check the entire filename inventory for normalized uniqueness and permitted characters.
- Preserve capture content, test assertions, behavior, and all path/media/contract/authentication checks.
- Verify the legacy names do not conflict with current contracts or the new OnboardingVoice runtime contract.
- Run applicable checks against the produced head. Report passed, failed, and unrun checks separately.
- Retain full fresh-head runtime requirements and independent visual/behavior review. Filename replay alone does not establish image integrity, UI fidelity, or merge readiness.

Return the exact commit, three-file implementation diff, seed-file changes, complete inventory mappings/counts, and check/artifact links.

Porting warning: the third file differs between integration and PR81 because PR81 contains repeated-root capture fixes. Any later authorized propagation must apply only the reviewed naming hunk to freshly verified PR81 content, never replace the entire file.

## Complete authenticated baseline filename inventory

Extracted without omissions from the media list in assembly job 113400927259 of run 37799337419. This is filename evidence, not downloaded image-byte verification. Baseline: 342 paths, SwiftUI 164, Compose 178.

```text
swiftui/AgendaEmpty-light.png
swiftui/AgendaScreen-light.png
swiftui/AgentSettings-dark.png
swiftui/AgentSettings-large-text-bottom.png
swiftui/AgentSettings-large-text.png
swiftui/AgentSettings-light.png
swiftui/AgentStatusPill-dark.png
swiftui/AgentStatusPill-light.png
swiftui/BrowserLiveCard-light.png
swiftui/ChatScreen-light.png
swiftui/CloudBrowser-add-login-empty-light.png
swiftui/CloudBrowser-add-login-filled-light.png
swiftui/CloudBrowser-add-login-focused-light.png
swiftui/CloudBrowser-add-site-empty-light.png
swiftui/CloudBrowser-add-site-filled-light.png
swiftui/CloudBrowser-add-site-focused-light.png
swiftui/CloudBrowser-added-site-permission-light.png
swiftui/CloudBrowser-clear-all-confirmation-light.png
swiftui/CloudBrowser-clear-all-empty-site-light.png
swiftui/CloudBrowser-cookies-cleared-light.png
swiftui/CloudBrowser-cookies-confirmation-light.png
swiftui/CloudBrowser-cookies-light.png
swiftui/CloudBrowser-dark.png
swiftui/CloudBrowser-default-permission-menu-light.png
swiftui/CloudBrowser-detail-clear-confirmation-light.png
swiftui/CloudBrowser-large-text-bottom.png
swiftui/CloudBrowser-large-text.png
swiftui/CloudBrowser-light.png
swiftui/CloudBrowser-login-edit-password-masked-light.png
swiftui/CloudBrowser-login-edit-username-light.png
swiftui/CloudBrowser-login-removed-scoped-light.png
swiftui/CloudBrowser-remove-login-confirmation-light.png
swiftui/CloudBrowser-saved-login-light.png
swiftui/CloudBrowser-site-detail-light.png
swiftui/CloudBrowser-sites-light.png
swiftui/ComposerBar-light.png
swiftui/Connectors-bottom-dark.png
swiftui/Connectors-bottom-large-text.png
swiftui/Connectors-brand-rows-bottom-light.png
swiftui/Connectors-dark.png
swiftui/Connectors-large-text.png
swiftui/Connectors-light.png
swiftui/Connectors-unsupported-provider-light.png
swiftui/Consent-default-light.png
swiftui/Consent-privacy-light.png
swiftui/Consent-terms-light.png
swiftui/ContainedIcon-dark.png
swiftui/ContainedIcon-light.png
swiftui/DateNavigationHeader-light.png
swiftui/ExecutionTrace-light.png
swiftui/Gmail-account-menu-after-policy-light.png
swiftui/Gmail-account-menu-light.png
swiftui/Gmail-account-override-retained-light.png
swiftui/Gmail-account-permissions-bottom-dark.png
swiftui/Gmail-account-permissions-bottom-large-text.png
swiftui/Gmail-account-permissions-inherited-light.png
swiftui/Gmail-account-permissions-light.png
swiftui/Gmail-connector-menu-light.png
swiftui/Gmail-connector-permissions-bottom-dark.png
swiftui/Gmail-connector-permissions-bottom-large-text.png
swiftui/Gmail-connector-permissions-dark.png
swiftui/Gmail-connector-permissions-large-text.png
swiftui/Gmail-connector-permissions-light.png
swiftui/Gmail-connector-policy-alwaysAllow-light.png
swiftui/Gmail-connector-policy-alwaysAsk-light.png
swiftui/Gmail-connector-policy-lowRisk-light.png
swiftui/Gmail-connector-policy-neverAllow-light.png
swiftui/Gmail-default-dark.png
swiftui/Gmail-default-large-text.png
swiftui/Gmail-default-light.png
swiftui/Gmail-disconnect-account-light.png
swiftui/Gmail-disconnect-accounts-light.png
swiftui/Gmail-disconnect-all-result-light.png
swiftui/Gmail-disconnect-one-result-light.png
swiftui/Gmail-information-dark.png
swiftui/Gmail-information-large-text.png
swiftui/InboxScreen-light.png
swiftui/ListRow-dark.png
swiftui/ListRow-light.png
swiftui/Memory-composer-feedback-initial-light.png
swiftui/Memory-composer-feedback-light.png
swiftui/Memory-composer-focused-light.png
swiftui/Memory-controls-changed-light.png
swiftui/Memory-dark.png
swiftui/Memory-large-text-bottom.png
swiftui/Memory-large-text.png
swiftui/Memory-light.png
swiftui/Memory-summary-light.png
swiftui/MessageBubble-light.png
swiftui/Models-add-key-empty-light.png
swiftui/Models-dark.png
swiftui/Models-key-masked-light.png
swiftui/Models-large-text-bottom.png
swiftui/Models-large-text.png
swiftui/Models-light.png
swiftui/Models-provider-picker-light.png
swiftui/PairedDevices-add-boundary-light.png
swiftui/PairedDevices-dark.png
swiftui/PairedDevices-detail-light.png
swiftui/PairedDevices-empty-light.png
swiftui/PairedDevices-large-text-bottom.png
swiftui/PairedDevices-large-text.png
swiftui/PairedDevices-light.png
swiftui/PairedDevices-remove-confirmation-light.png
swiftui/Pill-dark.png
swiftui/Pill-light.png
swiftui/RemButton-dark.png
swiftui/RemButton-light.png
swiftui/RemFaceMark-dark.png
swiftui/RemFaceMark-light.png
swiftui/RunningTaskBanner-light.png
swiftui/SettingsEntry-dark.png
swiftui/SettingsEntry-large-text.png
swiftui/SettingsEntry-light.png
swiftui/SettingsScreen-light.png
swiftui/SignIn-checking-light.png
swiftui/SignIn-error-dark.png
swiftui/SignIn-new-light.png
swiftui/SignIn-recovery-light.png
swiftui/SignIn-returning-light.png
swiftui/Slider-dark.png
swiftui/Slider-light.png
swiftui/TaskDetailScreen-light.png
swiftui/TaskEventRow-light.png
swiftui/Voice-chooser-dark-footer.png
swiftui/Voice-chooser-dark.png
swiftui/Voice-chooser-large-text-footer.png
swiftui/Voice-chooser-large-text.png
swiftui/Voice-chooser-light.png
swiftui/Voice-chooser-preview-light.png
swiftui/Voice-chooser-selected-light.png
swiftui/Voice-dark-footer.png
swiftui/Voice-dark.png
swiftui/Voice-independent-preview-light.png
swiftui/Voice-independent-selection-light.png
swiftui/Voice-large-text-choice-row.png
swiftui/Voice-large-text-footer.png
swiftui/Voice-large-text-speed.png
swiftui/Voice-large-text.png
swiftui/Voice-light.png
swiftui/Voice-menu-light.png
swiftui/Voice-preview-selected-light.png
swiftui/Voice-sliders-adjusted-light.png
swiftui/VoiceBar-light.png
swiftui/Wallet-dark.png
swiftui/Wallet-large-text.png
swiftui/Wallet-light.png
swiftui/Wallet-link-boundary-light.png
swiftui/Wallet-link-consent-dark.png
swiftui/Wallet-link-consent-large-text-benefit.png
swiftui/Wallet-link-consent-large-text-footer.png
swiftui/Wallet-link-consent-large-text.png
swiftui/Wallet-link-consent-light.png
swiftui/Wallet-shopPay-boundary-light.png
swiftui/Wallet-shopPay-consent-dark.png
swiftui/Wallet-shopPay-consent-large-text-benefit.png
swiftui/Wallet-shopPay-consent-large-text-footer.png
swiftui/Wallet-shopPay-consent-large-text.png
swiftui/Wallet-shopPay-consent-light.png
swiftui/WalletScreen-light.png
swiftui/ios-cancelled-load.png
swiftui/ios-controls.png
swiftui/ios-load-error.png
swiftui/ios-loading.png
compose/AgentSettings-dark.png
compose/AgentSettings-large-text-bottom.png
compose/AgentSettings-large-text.png
compose/AgentSettings-light.png
compose/CloudBrowser-add-login-empty-light.png
compose/CloudBrowser-add-login-filled-light.png
compose/CloudBrowser-add-login-focused-light.png
compose/CloudBrowser-add-site-empty-light.png
compose/CloudBrowser-add-site-filled-light.png
compose/CloudBrowser-add-site-focused-light.png
compose/CloudBrowser-added-site-permission-light.png
compose/CloudBrowser-clear-all-confirmation-light.png
compose/CloudBrowser-clear-all-empty-site-light.png
compose/CloudBrowser-clear-all-retained-login-github.com-light.png
compose/CloudBrowser-clear-all-retained-login-notion.so-light.png
compose/CloudBrowser-cookies-cleared-light.png
compose/CloudBrowser-cookies-cleared-recreated-light.png
compose/CloudBrowser-cookies-confirmation-light.png
compose/CloudBrowser-cookies-light.png
compose/CloudBrowser-dark.png
compose/CloudBrowser-default-permission-menu-light.png
compose/CloudBrowser-detail-clear-confirmation-light.png
compose/CloudBrowser-large-text-bottom.png
compose/CloudBrowser-large-text.png
compose/CloudBrowser-light.png
compose/CloudBrowser-login-edit-password-masked-light.png
compose/CloudBrowser-login-edit-username-light.png
compose/CloudBrowser-login-removed-scoped-light.png
compose/CloudBrowser-login-retained-light.png
compose/CloudBrowser-remove-login-confirmation-light.png
compose/CloudBrowser-root-before-login-add-light.png
compose/CloudBrowser-root-before-login-edit-light.png
compose/CloudBrowser-root-before-login-remove-light.png
compose/CloudBrowser-saved-login-light.png
compose/CloudBrowser-site-detail-light.png
compose/CloudBrowser-sites-light.png
compose/Connectors-bottom-dark.png
compose/Connectors-bottom-large-text.png
compose/Connectors-brand-rows-bottom-light.png
compose/Connectors-dark.png
compose/Connectors-large-text.png
compose/Connectors-light.png
compose/Connectors-unsupported-provider-light.png
compose/Gmail-account-menu-after-policy-light.png
compose/Gmail-account-menu-light.png
compose/Gmail-account-override-retained-light.png
compose/Gmail-account-permissions-bottom-dark.png
compose/Gmail-account-permissions-bottom-large-text.png
compose/Gmail-account-permissions-inherited-light.png
compose/Gmail-account-permissions-light.png
compose/Gmail-account-permissions-recreated-light.png
compose/Gmail-connector-menu-light.png
compose/Gmail-connector-permissions-bottom-dark.png
compose/Gmail-connector-permissions-bottom-large-text.png
compose/Gmail-connector-permissions-dark.png
compose/Gmail-connector-permissions-large-text.png
compose/Gmail-connector-permissions-light.png
compose/Gmail-connector-policy-AlwaysAllow-light.png
compose/Gmail-connector-policy-AlwaysAsk-light.png
compose/Gmail-connector-policy-LowRisk-light.png
compose/Gmail-connector-policy-NeverAllow-light.png
compose/Gmail-default-dark.png
compose/Gmail-default-large-text.png
compose/Gmail-default-light.png
compose/Gmail-disconnect-account-light.png
compose/Gmail-disconnect-accounts-light.png
compose/Gmail-disconnect-all-result-light.png
compose/Gmail-disconnect-one-recreated-light.png
compose/Gmail-disconnect-one-result-light.png
compose/Gmail-information-dark.png
compose/Gmail-information-large-text.png
compose/Memory-composer-feedback-light.png
compose/Memory-dark.png
compose/Memory-large-text-bottom.png
compose/Memory-large-text.png
compose/Memory-light.png
compose/Memory-summary-light.png
compose/Models-add-key-empty-light.png
compose/Models-dark.png
compose/Models-key-masked-light.png
compose/Models-large-text-bottom.png
compose/Models-large-text.png
compose/Models-light.png
compose/Models-provider-picker-light.png
compose/PairedDevices-add-boundary-light.png
compose/PairedDevices-dark.png
compose/PairedDevices-detail-light.png
compose/PairedDevices-empty-light.png
compose/PairedDevices-empty-recreated-light.png
compose/PairedDevices-large-text-bottom.png
compose/PairedDevices-large-text.png
compose/PairedDevices-light.png
compose/PairedDevices-remove-confirmation-light.png
compose/SettingsEntry-dark.png
compose/SettingsEntry-large-text.png
compose/SettingsEntry-light.png
compose/Voice-chooser-dark-footer.png
compose/Voice-chooser-dark.png
compose/Voice-chooser-large-text-footer.png
compose/Voice-chooser-large-text.png
compose/Voice-chooser-light.png
compose/Voice-chooser-preview-light.png
compose/Voice-chooser-selected-light.png
compose/Voice-dark-footer.png
compose/Voice-dark.png
compose/Voice-independent-preview-light.png
compose/Voice-independent-selection-light.png
compose/Voice-large-text-choice-row.png
compose/Voice-large-text-footer.png
compose/Voice-large-text-speed.png
compose/Voice-large-text.png
compose/Voice-light.png
compose/Voice-menu-light.png
compose/Voice-preferences-recreated-light.png
compose/Voice-preview-selected-light.png
compose/Voice-sliders-adjusted-light.png
compose/Wallet-dark.png
compose/Wallet-large-text.png
compose/Wallet-light.png
compose/Wallet-link-boundary-light.png
compose/Wallet-link-consent-dark-actions.png
compose/Wallet-link-consent-dark.png
compose/Wallet-link-consent-large-text-actions.png
compose/Wallet-link-consent-large-text-benefit.png
compose/Wallet-link-consent-large-text-footer.png
compose/Wallet-link-consent-large-text.png
compose/Wallet-link-consent-light.png
compose/Wallet-shopPay-boundary-light.png
compose/Wallet-shopPay-consent-dark-actions.png
compose/Wallet-shopPay-consent-dark.png
compose/Wallet-shopPay-consent-large-text-actions.png
compose/Wallet-shopPay-consent-large-text-benefit.png
compose/Wallet-shopPay-consent-large-text-footer.png
compose/Wallet-shopPay-consent-large-text.png
compose/Wallet-shopPay-consent-light.png
compose/android-controls.png
compose/android-load-error.png
compose/android-loading.png
compose/com.rem.designsystem_EvidenceSnapshots_agendaEmpty_agendaempty-light.png
compose/com.rem.designsystem_EvidenceSnapshots_agendaScreen_agendascreen-light.png
compose/com.rem.designsystem_EvidenceSnapshots_agendaToday_agendatoday-light.png
compose/com.rem.designsystem_EvidenceSnapshots_agentStatusPillDark_agentstatuspill-dark.png
compose/com.rem.designsystem_EvidenceSnapshots_agentStatusPill_agentstatuspill-light.png
compose/com.rem.designsystem_EvidenceSnapshots_browserLiveCard_browserlivecard-light.png
compose/com.rem.designsystem_EvidenceSnapshots_chatScreen_chatscreen-light.png
compose/com.rem.designsystem_EvidenceSnapshots_composerBar_composerbar-light.png
compose/com.rem.designsystem_EvidenceSnapshots_connectors_connectors-light.png
compose/com.rem.designsystem_EvidenceSnapshots_consentPrivacy_consent-privacy-light.png
compose/com.rem.designsystem_EvidenceSnapshots_consentTerms_consent-terms-light.png
compose/com.rem.designsystem_EvidenceSnapshots_consent_consent-default-light.png
compose/com.rem.designsystem_EvidenceSnapshots_containedIcon_containedicon-dark.png
compose/com.rem.designsystem_EvidenceSnapshots_containedIcon_containedicon-light.png
compose/com.rem.designsystem_EvidenceSnapshots_dailyBriefCard_dailybriefcard-light.png
compose/com.rem.designsystem_EvidenceSnapshots_dateNavigationHeader_datenavigationheader-light.png
compose/com.rem.designsystem_EvidenceSnapshots_executionTrace_executiontrace-light.png
compose/com.rem.designsystem_EvidenceSnapshots_inboxScreen_inboxscreen-light.png
compose/com.rem.designsystem_EvidenceSnapshots_messageBubble_messagebubble-light.png
compose/com.rem.designsystem_EvidenceSnapshots_pill_pill-dark.png
compose/com.rem.designsystem_EvidenceSnapshots_pill_pill-light.png
compose/com.rem.designsystem_EvidenceSnapshots_remFaceMark_remfacemark-dark.png
compose/com.rem.designsystem_EvidenceSnapshots_remFaceMark_remfacemark-light.png
compose/com.rem.designsystem_EvidenceSnapshots_runningTaskBanner_runningtaskbanner-light.png
compose/com.rem.designsystem_EvidenceSnapshots_settingsScreen_settingsscreen-dark.png
compose/com.rem.designsystem_EvidenceSnapshots_settingsScreen_settingsscreen-light.png
compose/com.rem.designsystem_EvidenceSnapshots_signInChecking_signin-checking-light.png
compose/com.rem.designsystem_EvidenceSnapshots_signInError_signin-error-dark.png
compose/com.rem.designsystem_EvidenceSnapshots_signInNew_signin-new-light.png
compose/com.rem.designsystem_EvidenceSnapshots_signInRecovery_signin-recovery-light.png
compose/com.rem.designsystem_EvidenceSnapshots_signInReturning_signin-returning-light.png
compose/com.rem.designsystem_EvidenceSnapshots_slider_slider-dark.png
compose/com.rem.designsystem_EvidenceSnapshots_slider_slider-light.png
compose/com.rem.designsystem_EvidenceSnapshots_suggestionSection_suggestionsection-light.png
compose/com.rem.designsystem_EvidenceSnapshots_taskDetailScreen_taskdetailscreen-light.png
compose/com.rem.designsystem_EvidenceSnapshots_taskEventRow_taskeventrow-light.png
compose/com.rem.designsystem_EvidenceSnapshots_voiceBar_voicebar-light.png
compose/com.rem.designsystem_EvidenceSnapshots_voice_voice-light.png
compose/com.rem.designsystem_EvidenceSnapshots_walletScreen_walletscreen-dark.png
compose/com.rem.designsystem_EvidenceSnapshots_walletScreen_walletscreen-light.png
```

## Expected mapping to verify

```json
{
  "baselineCount": 342,
  "baselineSet": 342,
  "mappings": {
    "compose/com.rem.designsystem_EvidenceSnapshots_connectors_connectors-light.png": "compose/com.rem.designsystem_EvidenceSnapshots_connectors_legacyonboardingconnectors-light.png",
    "compose/com.rem.designsystem_EvidenceSnapshots_voice_voice-light.png": "compose/com.rem.designsystem_EvidenceSnapshots_voice_legacyonboardingvoice-light.png",
    "compose/CloudBrowser-clear-all-retained-login-github.com-light.png": "compose/CloudBrowser-clear-all-retained-login-github-com-light.png",
    "compose/CloudBrowser-clear-all-retained-login-notion.so-light.png": "compose/CloudBrowser-clear-all-retained-login-notion-so-light.png"
  },
  "added": [
    "swiftui/LegacyOnboardingConnectors-light.png",
    "swiftui/LegacyOnboardingVoice-light.png"
  ],
  "projectedCount": 344,
  "projectedUniquePaths": 344,
  "projectedUniqueNormalizedKeys": 344,
  "projectedPlatformCounts": {
    "swiftui": 166,
    "compose": 178
  },
  "invalidNormalizedKeys": [],
  "duplicateNormalizedKeys": [],
  "retainedBaselineEntries": 342,
  "contractsUseLegacyPrefix": false
}
```

The expected mapping is a complete baseline-derived naming replay, not a substitute for fresh-head runtime captures. Do not force the actual count or remove captures. Keep known integration runtime differences and every required gate visible.
