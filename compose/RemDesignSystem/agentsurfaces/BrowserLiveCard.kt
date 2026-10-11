package com.rem.designsystem.agentsurfaces

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * **BrowserLiveCard** — Compose sibling of the SwiftUI [BrowserLiveCard]. The in-chat cloud-browser
 * card: one backgroundSecondary rounded card (radius.large) with a leading 56×40 preview thumbnail and
 * a title + status stack. The whole card reads its layout from a single [BrowserLiveCardState] — a
 * loading globe while Opening, a brand-blue mini-browser while Active, a greyed mini-browser once Ended —
 * the same card transforming in place rather than three separate widgets.
 *
 * Cross-platform contract (SPEC): intent + tokens are shared, form is native — the globe is Material
 * [Icons.Filled.Public] here, `globe` (SF Symbol) on iOS. Figma canonical: **BrowserLiveCard** `524:31`
 * (variant set). Source lineage: `BrowserLiveView.swift`.
 *
 * This is the DS *display* form — it renders a state, not the interactive session controls.
 */
enum class BrowserLiveCardState {
    /** The session is spinning up — thumbnail shows a loading globe. */
    Opening,
    /** The session is live — thumbnail shows a brand-blue mini-browser. */
    Active,
    /** The session has finished — thumbnail greys out. */
    Ended,
}

@Composable
fun BrowserLiveCard(
    state: BrowserLiveCardState,
    modifier: Modifier = Modifier,
    title: String = "Rem's browser session",
    status: String? = null,
) {
    val colors = RemColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(RemRadius.large))
            .background(colors.backgroundSecondary)
            .padding(RemSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        Thumbnail(state)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = RemTypography.subheadline,
                fontWeight = FontWeight.SemiBold,
                color = colors.labelPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = status ?: defaultStatus(state),
                style = RemTypography.footnote,
                color = colors.labelSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val ThumbWidth = 56.dp
private val ThumbHeight = 40.dp
private val ThumbRadius = 10.dp

@Composable
private fun Thumbnail(state: BrowserLiveCardState) {
    val colors = RemColors.current
    Box(
        modifier = Modifier
            .size(width = ThumbWidth, height = ThumbHeight)
            .clip(RoundedCornerShape(ThumbRadius))
            .background(colors.fillTertiary),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            BrowserLiveCardState.Opening -> Icon(
                imageVector = Icons.Filled.Public,
                contentDescription = null,
                tint = colors.labelSecondary,
                modifier = Modifier.size(20.dp),
            )
            BrowserLiveCardState.Active -> MiniBrowser(chrome = colors.brandBlue)
            BrowserLiveCardState.Ended -> MiniBrowser(chrome = colors.labelTertiary)
        }
    }
}

/** A tiny browser window: a chrome bar across the top + two short content lines. */
@Composable
private fun MiniBrowser(chrome: Color) {
    val colors = RemColors.current
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(chrome),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(Modifier.width(28.dp).height(5.dp).clip(RoundedCornerShape(2.dp)).background(colors.labelTertiary))
            Box(Modifier.width(18.dp).height(5.dp).clip(RoundedCornerShape(2.dp)).background(colors.labelTertiary))
        }
    }
}

private fun defaultStatus(state: BrowserLiveCardState): String = when (state) {
    BrowserLiveCardState.Opening -> "Opening browser…"
    BrowserLiveCardState.Active -> "Session active · tap to watch or take over"
    BrowserLiveCardState.Ended -> "Session ended · tap to review"
}

@Preview(name = "BrowserLiveCard — all states", showBackground = true, widthDp = 362)
@Composable
private fun BrowserLiveCardPreview() {
    RemTheme {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BrowserLiveCardState.values().forEach { state ->
                BrowserLiveCard(state)
            }
        }
    }
}
