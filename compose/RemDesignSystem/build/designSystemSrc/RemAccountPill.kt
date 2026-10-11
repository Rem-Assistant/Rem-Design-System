package com.rem.designsystem.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography

/**
 * **RemAccountPill** — the signed-in account at the bottom of the sidebar (Compose twin of
 * `Navigation/RemAccountPill.swift`): initial disc + name + `unfold_more`, on a floating capsule. It
 * **is** the Settings entry — no separate gear — so TalkBack reads "<name>, Opens Settings".
 * Figma: **Account · opens Settings** (`2709:2648`).
 */
@Composable
fun RemAccountPill(account: RemSidebarAccount, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RemColors.current
    Row(
        modifier
            .height(44.dp)
            .shadow(20.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.12f))
            .clip(CircleShape)
            .background(colors.backgroundPrimary.copy(alpha = 0.85f))
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                role = Role.Button
                contentDescription = "${account.displayName}, ${RemSidebarDisplay.AccountHint}"
                onClick(label = RemSidebarDisplay.AccountHint) { onClick(); true }
            }
            .testTag("sidebar.account")
            .padding(start = RemSpacing.xs, end = RemSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(29.dp).clip(CircleShape).background(colors.backgroundSecondary),
            contentAlignment = Alignment.Center,
        ) {
            Text(account.initial, style = RemTypography.subheadline.copy(fontWeight = FontWeight.SemiBold), color = colors.labelPrimary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                account.displayName,
                style = RemTypography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = colors.labelPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SymbolGlyph(RemMaterialSymbols.AccountMenu, 16, colors.labelSecondary)
        }
    }
}
