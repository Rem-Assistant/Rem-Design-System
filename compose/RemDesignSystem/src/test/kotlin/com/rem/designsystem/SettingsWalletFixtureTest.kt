package com.rem.designsystem

import com.rem.designsystem.screens.SettingsWalletFixture
import com.rem.designsystem.screens.SettingsWalletProvider
import com.rem.designsystem.screens.SettingsWalletStage
import org.junit.Assert.*
import org.junit.Test

class SettingsWalletFixtureTest {
    @Test fun externalBoundaryRequiresConsentAndUsesChosenProvider() {
        assertEquals(SettingsWalletFixture(), SettingsWalletFixture().connect())
        SettingsWalletProvider.entries.forEach { provider ->
            val consent = SettingsWalletFixture().open(provider)
            assertEquals(SettingsWalletStage.Consent, consent.stage)
            assertEquals(provider, consent.provider)
            val external = consent.connect()
            assertEquals(SettingsWalletStage.External, external.stage)
            assertEquals(if (provider == SettingsWalletProvider.Link) "app.link.com" else "shop.app", external.provider?.domain)
        }
    }

    @Test fun cancellingOrClosingNeverProducesConnectedState() {
        SettingsWalletProvider.entries.forEach { provider ->
            val consent = SettingsWalletFixture().open(provider)
            assertEquals(SettingsWalletFixture(), consent.dismiss())
            val closed = consent.connect().dismiss()
            assertEquals(SettingsWalletFixture(), closed)
            assertEquals(SettingsWalletStage.Consent, closed.open(provider).stage)
        }
    }

    @Test fun recreationRetainsOnlyTheExactPresentationPath() {
        val states = listOf(SettingsWalletFixture()) + SettingsWalletProvider.entries.flatMap {
            val consent = SettingsWalletFixture().open(it)
            listOf(consent, consent.connect())
        }
        states.forEach {
            assertTrue(it.snapshot().isNotEmpty())
            assertEquals(it, SettingsWalletFixture.restore(it.snapshot()))
            assertEquals(SettingsWalletFixture(), SettingsWalletFixture.restore(it.snapshot()).dismiss())
        }
    }

    @Test fun malformedOrUnknownRestoredStateCannotInventAProviderOrSuccess() {
        listOf("", "link:Root", "link:Connected", "unknown:Consent", "shopPay:External:extra").forEach {
            assertEquals(SettingsWalletFixture(), SettingsWalletFixture.restore(it))
        }
    }

    @Test fun switchingProvidersRequiresFreshConsentWithItsOwnCopy() {
        val shop = SettingsWalletFixture().open(SettingsWalletProvider.Link).connect().open(SettingsWalletProvider.ShopPay)
        assertEquals(SettingsWalletStage.Consent, shop.stage)
        assertEquals("Connect your Shop Pay wallet for purchases you ask Rem to make.", shop.provider?.consentBody)
        assertTrue(shop.provider!!.benefits.first().body.contains("through Shop Pay."))
    }
}
