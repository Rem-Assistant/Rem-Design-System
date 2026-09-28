package com.rem.designsystem.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.primitives.ContainedIconSize
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography

/**
 * The **shared onboarding chrome** — the reusable layout every sequencer step renders into. It is the
 * Compose sibling of the SwiftUI onboarding shell (`OnboardingFlow.swift` step scaffold): a back
 * affordance + optional progress + a centered hero/title/subtitle over a scrollable content slot,
 * with a bottom-pinned CTA bar (primary + optional secondary) and an optional legal footer.
 *
 * It is **presentational and state-driven** — it owns no navigation or auth logic; the
 * [OnboardingSequencer] drives it and the host wires real behaviour through the [OnboardingAction]
 * callbacks. This is the boundary that keeps sign-in "wired to real auth, no mock": the scaffold only
 * renders the state it is handed.
 *
 * Every visual binds to the generated `RemTokens` (colors/spacing/radius/typography); the form is
 * native Material (ImageVector glyphs, not SF Symbols), per the SPEC cross-platform contract.
 */
@Composable
fun OnboardingScaffold(
    primary: OnboardingAction,
    modifier: Modifier = Modifier,
    hero: OnboardingHero? = null,
    title: String? = null,
    subtitle: String? = null,
    secondary: OnboardingAction? = null,
    legalFooter: String? = null,
    /** An error notice card rendered directly **above** the bottom-pinned CTA (e.g. consent's error). */
    notice: String? = null,
    background: OnboardingBackground = OnboardingBackground.Primary,
    progress: OnboardingProgress? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val colors = RemColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background.color())
            .padding(horizontal = RemSpacing.xl),
    ) {
        // Top bar — back chevron (leading) mirrors the reference frames' top-left back button. When
        // there is nowhere to go back to, the sequencer passes onBack = null and we keep the same
        // vertical rhythm with an empty spacer so the hero doesn't jump between steps.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickableRole(onClick = onBack, label = "Back"),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = colors.labelPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            } else {
                Spacer(Modifier.size(44.dp))
            }
            Spacer(Modifier.width(RemSpacing.sm))
            if (progress != null) {
                OnboardingProgressIndicator(
                    progress = progress,
                    modifier = Modifier.padding(start = RemSpacing.xs),
                )
            }
        }

        // Scrollable body: hero → title → subtitle → step content. Scrolls so tall steps (voice
        // sliders, long consent copy) never clip on small devices; the CTA bar stays pinned below.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(RemSpacing.xxl))
            if (hero != null) {
                val heroTint = ContainedIconFill.Tint(hero.tint ?: colors.brandBlue)
                // The onboarding hero is the large brand squircle in every reference frame. It reuses
                // the canonical ContainedIcon (anti-drift Rule 0) at its Large size (64dp). The
                // reference hero reads slightly larger (~88pt); logged as a metric-reconcile item (a
                // dedicated `hero` size token) rather than forking a bespoke tile here.
                //
                // A glyph hero (consent's shield-lock) renders the Material Symbols font glyph at its
                // registry FILL — never a legacy `Icons.Filled.*` — so its FILL matches the iOS SF
                // Symbol. A vector hero (other steps) still uses the ImageVector overload.
                if (hero.glyph != null) {
                    ContainedIcon(
                        glyph = hero.glyph,
                        glyphFill = hero.glyphFill,
                        fill = heroTint,
                        size = ContainedIconSize.Large,
                        contentDescription = hero.contentDescription,
                    )
                } else if (hero.icon != null) {
                    ContainedIcon(
                        icon = hero.icon,
                        fill = heroTint,
                        size = ContainedIconSize.Large,
                        contentDescription = hero.contentDescription,
                    )
                }
                Spacer(Modifier.height(RemSpacing.lg))
            }
            if (title != null) {
                Text(
                    text = title,
                    style = RemTypography.largeTitle.copy(fontWeight = RemTypography.title1Bold.fontWeight),
                    color = colors.labelPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(RemSpacing.sm))
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = RemTypography.body,
                    color = colors.labelSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(RemSpacing.xl))
            }
            content()
            Spacer(Modifier.height(RemSpacing.xl))
        }

        // Bottom CTA region — pinned; does not scroll. Order top→bottom matches the contract: the
        // error notice (when present) sits directly ABOVE the CTA; the legal footnote sits BELOW it
        // (contrast the earlier footer-above-button arrangement — the footnote reads as consent's
        // legal fine print under the button, matching iOS + `docs/contracts/onboarding-consent.md`).
        //
        // Spacing mirrors the iOS `OnboardingConsentTemplate` bottom bar EXACTLY so the paired render
        // has the same density in every state: top `sm`, `md` between each element (notice→CTA and
        // CTA→footnote), and `md` below the footnote. The gaps are only inserted between the elements
        // that are actually present, so the no-error / no-footer states stay compact — there is no
        // reserved empty notice slot and no extra bottom padding beyond the iOS `md`.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = RemSpacing.sm, bottom = RemSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (notice != null) {
                OnboardingNotice(notice)
                Spacer(Modifier.height(RemSpacing.md))
            }
            OnboardingActionButton(action = primary, modifier = Modifier.fillMaxWidth())
            if (secondary != null) {
                Spacer(Modifier.height(RemSpacing.sm))
                OnboardingActionButton(action = secondary, modifier = Modifier.fillMaxWidth())
            }
            if (legalFooter != null) {
                Spacer(Modifier.height(RemSpacing.md))
                // caption1 + labelSecondary, matching iOS + the contract's footnote role.
                Text(
                    text = legalFooter,
                    style = RemTypography.caption1,
                    color = colors.labelSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RemSpacing.sm),
                )
            }
        }
    }
}

/**
 * The onboarding page background. Steps built from grouped-list controls (Connectors, Check-in,
 * Voice) sit on `Secondary` (the iOS grouped background); plain steps (Sign-in) use `Primary`.
 */
enum class OnboardingBackground {
    Primary,
    Secondary,
    ;

    @Composable
    fun color(): Color = when (this) {
        Primary -> RemColors.current.backgroundPrimary
        Secondary -> RemColors.current.backgroundSecondary
    }
}

/**
 * The brand squircle hero: a glyph on the brand-blue (or overridden) [ContainedIcon] tile.
 *
 * Two glyph sources: [glyph] is a **Material Symbols** font glyph (from `RemMaterialSymbols`) rendered
 * at [glyphFill] — the registry-faithful path (consent's shield-lock at FILL 1), which is how a hero's
 * FILL matches its iOS SF Symbol. [icon] is a Material [ImageVector] for steps that don't (yet) have a
 * registry row. Prefer [glyph]; when both are set [glyph] wins.
 */
data class OnboardingHero(
    val icon: ImageVector? = null,
    val glyph: String? = null,
    val glyphFill: Float = 1f,
    val tint: Color? = null,
    val contentDescription: String? = null,
)

/**
 * A bottom-bar action. [style] picks the reproduced treatment (filled black CTA, blue text link,
 * subtle text link, neutral secondary). [loading] renders the disabled "Saving…" spinner state seen
 * in the Check-in reference.
 */
data class OnboardingAction(
    val label: String,
    val onClick: () -> Unit,
    val style: OnboardingActionStyle = OnboardingActionStyle.Primary,
    val enabled: Boolean = true,
    val loading: Boolean = false,
    val leadingIcon: ImageVector? = null,
    /**
     * Whether the [leadingIcon] is recolored to the label color. Monochrome provider marks (Apple)
     * tint to the inverted label; a multicolor mark (the Google "G") must stay `false` so it renders
     * at its own four colors.
     */
    val tintLeadingIcon: Boolean = true,
)

enum class OnboardingActionStyle {
    /** Full-width filled button on `buttonBackground` with an inverted label — the primary CTA. */
    Primary,

    /** Full-width filled neutral button (e.g. the second provider button in the new-user state). */
    Secondary,

    /** Centered blue text link — e.g. "Skip". */
    TextAccent,

    /** Centered subtle (labelSecondary) text link — e.g. "Sign in with a different account". */
    TextSubtle,
}

@Composable
private fun OnboardingActionButton(action: OnboardingAction, modifier: Modifier = Modifier) {
    val colors = RemColors.current
    val interactive = action.enabled && !action.loading
    when (action.style) {
        OnboardingActionStyle.Primary, OnboardingActionStyle.Secondary -> {
            val filled = action.style == OnboardingActionStyle.Primary
            // Primary = buttonBackground (black in light / white in dark) with the inverted label
            // (backgroundPrimary). Secondary = the neutral fill used for a second provider button.
            val containerColor = if (filled) colors.buttonBackground else colors.fillTertiary
            val labelColor = if (filled) colors.backgroundPrimary else colors.labelPrimary
            Box(
                modifier = modifier
                    .heightIn(min = 50.dp)
                    // `medium` radius — matches the iOS primary `RemButton` (rectBlack) + sign-in's
                    // `SignInButton`, so the paired CTA reads as the same treatment on both platforms.
                    .clip(RoundedCornerShape(RemRadius.medium))
                    .background(containerColor)
                    .alpha(if (interactive) 1f else RemOnboardingMetrics.disabledAlpha)
                    .then(if (interactive) Modifier.clickableRole(action.onClick, action.label) else Modifier)
                    .padding(vertical = RemSpacing.md, horizontal = RemSpacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    if (action.loading) {
                        CircularProgressIndicator(
                            color = labelColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(RemSpacing.sm))
                    } else if (action.leadingIcon != null) {
                        Icon(
                            imageVector = action.leadingIcon,
                            contentDescription = null,
                            // Color.Unspecified keeps a multicolor mark (Google "G") at its own colors.
                            tint = if (action.tintLeadingIcon) labelColor else Color.Unspecified,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(RemSpacing.sm))
                    }
                    Text(text = action.label, style = RemTypography.bodyBold, color = labelColor)
                }
            }
        }

        OnboardingActionStyle.TextAccent, OnboardingActionStyle.TextSubtle -> {
            val labelColor = if (action.style == OnboardingActionStyle.TextAccent) {
                colors.systemBlue
            } else {
                colors.labelSecondary
            }
            Box(
                modifier = modifier
                    .heightIn(min = 44.dp)
                    .alpha(if (interactive) 1f else RemOnboardingMetrics.disabledAlpha)
                    .then(if (interactive) Modifier.clickableRole(action.onClick, action.label) else Modifier)
                    .padding(vertical = RemSpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = action.label, style = RemTypography.bodyBold, color = labelColor)
            }
        }
    }
}

/** Onboarding metrics that are not (yet) token-backed. Centralized so no view holds a literal. */
internal object RemOnboardingMetrics {
    /** Dimming applied to a disabled/loading CTA (matches the greyed "Saving…" reference button). */
    const val disabledAlpha: Float = RemDisabledAlpha
}

// The disabled dimming has no token on tokens.json yet; kept as one named constant to reconcile
// alongside the other flagged onboarding metrics rather than sprinkled as a call-site literal.
private const val RemDisabledAlpha: Float = 0.4f
