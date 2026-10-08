package com.rem.designsystem.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rem.designsystem.R
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.primitives.ContainedIconSize
import com.rem.designsystem.rows.DisclosureChevron
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.rows.ListRowLabel
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography

private val WalletPresentationSaver = Saver<SettingsWalletFixture, String>(
    save = { it.snapshot() }, restore = { SettingsWalletFixture.restore(it) },
)

/** Wallet1833:51817 + Pre-consent/Provider1898:54528. ModalBottomSheet supplies adaptive native
 * modality rather than the source's 623pt specimen height. The external stage is a blank local
 * boundary; no browser, auth, payment or successful connection is started or claimed. The utility
 * Wallet hero follows the explicit Subtle amendment (labelPrimary/backgroundSecondary) despite the
 * frozen blue Figma hero. Provider marks retain their authentic source colors. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsWalletScreen(onBack: () -> Unit) {
    var fixture by rememberSaveable(stateSaver = WalletPresentationSaver) { mutableStateOf(SettingsWalletFixture()) }
    val colors = RemColors.current
    BackHandler {
        if (fixture.stage == SettingsWalletStage.Root) onBack() else fixture = fixture.dismiss()
    }
    Scaffold(
        containerColor = colors.backgroundPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Wallet", style = RemTypography.bodyBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("wallet.back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.backgroundPrimary),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = RemSpacing.lg).padding(bottom = RemSpacing.xl)
                .testTag("settingsWallet"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RemSpacing.lg),
        ) {
            Spacer(Modifier.height(RemSpacing.sm))
            ContainedIcon(symbol = RemMaterialSymbols.Wallet, fill = ContainedIconFill.Subtle, size = ContainedIconSize.Large)
            Text("Wallet", style = RemTypography.title1.copy(fontWeight = FontWeight.Bold), color = colors.labelPrimary,
                modifier = Modifier.semantics { heading() })
            Text(SettingsWalletFixture.body, style = RemTypography.body, color = colors.labelSecondary,
                textAlign = TextAlign.Center)
            RemSection {
                SettingsWalletProvider.entries.forEachIndexed { index, provider ->
                    ListRow(
                        modifier = Modifier.testTag("wallet.provider.${provider.id}"),
                        showsDivider = index == 0,
                        onClick = { fixture = fixture.open(provider) },
                        leading = { WalletProviderMark(provider, isHero = false) },
                        content = { ListRowLabel(provider.title) },
                        trailing = { DisclosureChevron() },
                    )
                }
            }
        }
    }
    val contentDensity = LocalDensity.current
    fixture.provider?.let { provider ->
        ModalBottomSheet(
            onDismissRequest = { fixture = fixture.dismiss() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.backgroundPrimary,
        ) {
            // Dialog windows establish their own density. Preserve the host's accessibility scale.
            CompositionLocalProvider(LocalDensity provides contentDensity) {
                if (fixture.stage == SettingsWalletStage.External) {
                    WalletExternalBoundary(provider, onClose = { fixture = fixture.dismiss() })
                } else {
                    WalletProviderConsent(provider, onConnect = { fixture = fixture.connect() },
                        onCancel = { fixture = fixture.dismiss() })
                }
            }
        }
    }
}

@Composable
private fun WalletProviderMark(provider: SettingsWalletProvider, isHero: Boolean) {
    val resource = when (provider) {
        SettingsWalletProvider.Link -> R.drawable.settings_wallet_link
        SettingsWalletProvider.ShopPay -> if (isHero) R.drawable.settings_wallet_shop_pay_consent else R.drawable.settings_wallet_shop_pay_row
    }
    Image(painterResource(resource), contentDescription = null, modifier = Modifier.size(if (isHero) 60.dp else 29.dp))
}

@Composable
private fun WalletProviderConsent(provider: SettingsWalletProvider, onConnect: () -> Unit, onCancel: () -> Unit) {
    val colors = RemColors.current
    ProviderPreConsentContent(
        payload = ProviderPreConsentPayload(provider.title, provider.consentBody,
            provider.benefits.map { ProviderPreConsentBenefit(it.id, it.title, it.body) }, provider.disclosure),
        onConnect = onConnect, onCancel = onCancel,
        modifier = Modifier.testTag("wallet.consent.${provider.id}"), actionAccessibilityPrefix = "wallet.consent",
        providerMark = { WalletProviderMark(provider, isHero = true) },
        benefitIcon = { benefit ->
            when (benefit.id) {
                "wallet" -> Icon(Icons.AutoMirrored.Outlined.FormatListBulleted, null, Modifier.size(26.dp), tint = colors.labelPrimary)
                "control" -> Text(RemMaterialSymbols.Permissions.glyph, fontFamily = RemMaterialSymbols.family(RemMaterialSymbols.Permissions),
                    fontSize = 24.sp, color = colors.labelPrimary, modifier = Modifier.size(26.dp).clearAndSetSemantics {})
                else -> Icon(Icons.Outlined.Visibility, null, Modifier.size(26.dp), tint = colors.labelPrimary)
            }
        },
    )
}

@Composable
private fun WalletExternalBoundary(provider: SettingsWalletProvider, onClose: () -> Unit) {
    val colors = RemColors.current
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.85f).background(colors.backgroundPrimary)
        .testTag("wallet.external.${provider.id}")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = RemSpacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Text(provider.domain, style = RemTypography.bodyBold, color = colors.labelPrimary,
                modifier = Modifier.weight(1f).semantics { heading() })
            TextButton(onClick = onClose, modifier = Modifier.testTag("wallet.external.close")) {
                Text("Close", style = RemTypography.body, color = colors.brandBlue)
            }
        }
        Box(Modifier.fillMaxSize().semantics { contentDescription = "External provider boundary. Prototype only. No provider page is loaded." })
    }
}
