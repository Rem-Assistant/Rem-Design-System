package com.rem.designsystem.screens

/** Authored Wallet providers. A provider is a presentation choice, never an authenticated account. */
enum class SettingsWalletProvider(val id: String, val title: String, val shortName: String, val domain: String) {
    Link("link", "Link by Stripe", "Link", "app.link.com"),
    ShopPay("shopPay", "Shop Pay", "Shop Pay", "shop.app");

    val consentBody: String get() = "Connect your $shortName wallet for purchases you ask Rem to make."
    val disclosure: String get() = "Next, continue to $shortName to sign in and review access. Rem will exchange info with $shortName; see its terms and privacy policy."
    val benefits: List<SettingsWalletBenefit> get() = listOf(
        SettingsWalletBenefit("wallet", "Use your saved wallet", "Use payment methods and checkout details you authorize through $shortName."),
        SettingsWalletBenefit("control", "You choose what Rem can do", "Rem asks before actions that need review. Disconnect anytime in Settings."),
        SettingsWalletBenefit("review", "Keep an eye on things", "Rem may take unexpected actions. Review purchases carefully."),
    )
}

data class SettingsWalletBenefit(val id: String, val title: String, val body: String)
enum class SettingsWalletStage { Root, Consent, External }

/** Local presentation only. No connected flag, credentials, cookies, or payment data are modelled. */
data class SettingsWalletFixture private constructor(
    val provider: SettingsWalletProvider?,
    val stage: SettingsWalletStage,
) {
    constructor() : this(null, SettingsWalletStage.Root)
    fun open(provider: SettingsWalletProvider) = SettingsWalletFixture(provider, SettingsWalletStage.Consent)
    fun connect() = if (stage == SettingsWalletStage.Consent && provider != null) copy(stage = SettingsWalletStage.External) else this
    fun dismiss() = SettingsWalletFixture()

    /** Saves only the presentation path across Activity recreation. Always nonempty, including root. */
    fun snapshot(): String = provider?.let { "${it.id}:${stage.name}" } ?: "root"

    companion object {
        const val body = "Securely save payment methods for Rem to use when making purchases for you."
        fun restore(snapshot: String): SettingsWalletFixture {
            val parts = snapshot.split(':')
            if (parts.size != 2) return SettingsWalletFixture()
            val provider = SettingsWalletProvider.entries.firstOrNull { it.id == parts[0] } ?: return SettingsWalletFixture()
            val stage = SettingsWalletStage.entries.firstOrNull { it.name == parts[1] && it != SettingsWalletStage.Root }
                ?: return SettingsWalletFixture()
            return SettingsWalletFixture(provider, stage)
        }
    }
}
