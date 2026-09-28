package com.rem.designsystem

import com.rem.designsystem.onboarding.LegalSection

/**
 * Render-only fixture prose. These values are excluded from the packaged Android library and are
 * not canonical Terms or Privacy content; they only make the reusable sheet chrome measurable.
 */
internal val previewTermsSections = listOf(
    LegalSection(
        "1. Your account",
        "Rem accounts let you sign in, sync your data, and manage subscriptions across your devices. " +
            "You are responsible for keeping your sign-in credentials secure.",
    ),
    LegalSection(
        "2. Subscriptions",
        "Paid features renew automatically until cancelled. You can review or cancel a subscription " +
            "in Settings at any time; access continues through the end of the current period.",
    ),
    LegalSection(
        "3. Approved actions",
        "When you ask Rem to act on your behalf, it performs only the actions you approve through your " +
            "personal cloud gateway. You can revoke an approval at any time.",
    ),
)

internal val previewPrivacySections = listOf(
    LegalSection(
        "What we process",
        "Rem processes the messages, tasks, and connections you give it so it can answer you and act " +
            "on the things you ask. You can review or delete this data in Settings.",
    ),
    LegalSection(
        "Your gateway",
        "Requests route through your personal cloud gateway. Rem stores only what is needed to keep " +
            "your assistant working across sessions and devices.",
    ),
    LegalSection(
        "AI and voice providers",
        "To generate answers, relevant content may be sent to AI or voice providers under agreements " +
            "that limit their use to serving your request.",
    ),
)
