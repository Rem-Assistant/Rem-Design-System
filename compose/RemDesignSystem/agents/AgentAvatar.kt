package com.rem.designsystem.agents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rem.designsystem.brand.RemFaceMark
import com.rem.designsystem.brand.RemFaceMarkMode
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography

/**
 * **AgentAvatar** — the circular agent identity mark, the Compose twin of `Agents/AgentAvatar.swift`.
 * The Main agent is the Rem face on a `backgroundSecondary` disc (`2718:3396`); a side agent is its
 * chosen glyph in white on its chosen colour (`2714:3587` at 80dp, `2718:3061` at 96dp). Decorative:
 * callers pair it with the agent name.
 */
@Composable
fun AgentAvatar(identity: AgentAvatarIdentity, modifier: Modifier = Modifier, size: Dp = 96.dp) {
    val colors = RemColors.current
    when (identity) {
        AgentAvatarIdentity.Main -> Box(
            modifier
                .size(size)
                .clip(CircleShape)
                .background(colors.backgroundSecondary)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            RemFaceMark(mode = RemFaceMarkMode.Idle, tint = colors.brandBlue, size = size * 0.75f)
        }
        is AgentAvatarIdentity.Side -> {
            val glyphSp = with(LocalDensity.current) { (size * 0.45f).toSp() }
            val symbol = identity.look.glyph.symbol
            Box(
                modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(identity.look.color.swatch())
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = symbol.glyph,
                    fontFamily = RemMaterialSymbols.family(symbol),
                    fontSize = glyphSp,
                    color = Color.White,
                )
            }
        }
    }
}

/** The token each swatch resolves to. */
@Composable
fun AgentColor.swatch(): Color {
    val colors = RemColors.current
    return when (this) {
        AgentColor.Pink -> colors.systemPink
        AgentColor.Blue -> colors.systemBlue
        AgentColor.Green -> colors.systemGreen
        AgentColor.Purple -> colors.systemPurple
        AgentColor.Indigo -> colors.systemIndigo
    }
}

// ── Shared grouped-form chrome (internal) ─────────────────────────────────────────────────────────

/**
 * Figma **SectionHeader** (`2709:132175`): 17sp semibold `labelSecondary`, sentence case, 16dp inset,
 * 6dp below. Main's `RemSection(header=)` still renders the older uppercased footnote header, so these
 * agent surfaces place this header above a header-less `RemSection`. Reconcile: once
 * `RemSection(settingsHeader = true)` lands (PR #106), use it and delete this helper.
 */
@Composable
internal fun AgentSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold),
        color = RemColors.current.labelSecondary,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = RemSpacing.lg, end = RemSpacing.lg, bottom = 6.dp)
            .semantics { heading() },
    )
}

@Composable
internal fun AgentSectionFooter(text: String) {
    Text(
        text = text,
        style = RemTypography.footnote,
        color = RemColors.current.labelSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = RemSpacing.lg, end = RemSpacing.lg, top = 6.dp),
    )
}

/** A grouped block: optional header, a [RemSection] surface, optional footer. */
@Composable
internal fun AgentFormSection(
    header: String? = null,
    footer: String? = null,
    modifier: Modifier = Modifier,
    rows: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        if (header != null) AgentSectionHeader(header)
        RemSection(rows = rows)
        if (footer != null) AgentSectionFooter(footer)
    }
}

/** Inset separator between rows inside one [RemSection]. */
@Composable
internal fun AgentRowDivider(leadingInset: Dp = 60.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = leadingInset),
        thickness = 0.5.dp,
        color = RemColors.current.separator,
    )
}

/**
 * The canonical row content used inside agent sections — `[leading] · title/subtitle · [trailing]`,
 * matching the SwiftUI `ListRow` (`101:18`) on main: semibold body title, caption1 subtitle, 12dp
 * horizontal / 8dp vertical padding, 12dp gaps. Main has no Compose `ListRow` yet; reconcile with
 * `rows/ListRow.kt` from PR #106 when it lands.
 */
@Composable
internal fun AgentListRow(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    showsChevron: Boolean = onClick != null,
) {
    val colors = RemColors.current
    val description = listOfNotNull(title, subtitle).joinToString(", ")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .clearAndSetSemantics {
                contentDescription = description
                if (onClick != null) role = Role.Button
            }
            .padding(horizontal = RemSpacing.md, vertical = RemSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold), color = colors.labelPrimary)
            if (subtitle != null) {
                Text(subtitle, style = RemTypography.caption1, color = colors.labelSecondary)
            }
        }
        if (showsChevron) {
            Text(
                text = RemMaterialSymbols.DisclosureChevron.glyph,
                fontFamily = RemMaterialSymbols.family(RemMaterialSymbols.DisclosureChevron),
                fontSize = 18.sp,
                color = colors.labelTertiary,
            )
        }
    }
}

/** A connected-service row (`2718:3335`) or "Add a service" (`2714:3744`): blue tinted tile + chevron. */
@Composable
internal fun AgentServiceRow(
    title: String,
    subtitle: String?,
    glyph: AgentServiceGlyph,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    AgentListRow(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        onClick = onClick,
        leading = {
            ContainedIcon(symbol = glyph.symbol, fill = ContainedIconFill.Tint(RemColors.current.systemBlue))
        },
    )
}

/**
 * Full-width **Button · Rect · Black** (`377:8` / `1798:17209`), mirroring the SwiftUI
 * `RemButtonTokenSet` rectBlack values: medium radius, 12dp padding, bodyBold `backgroundPrimary` on
 * `labelPrimary`; disabled = `labelTertiary` @ 35%. Main has no Compose `RemButton` yet; reconcile with
 * `buttons/RemButton.kt` from PR #106 when it lands.
 */
@Composable
internal fun AgentPrimaryButton(label: String, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = RemColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RemRadius.medium))
            .background(if (enabled) colors.labelPrimary else colors.labelTertiary.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button }
            .padding(RemSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = RemTypography.bodyBold, color = colors.backgroundPrimary)
    }
}

/** A centred text action inside a [RemSection] (Pause agent / Delete agent), 44dp tall. */
@Composable
internal fun AgentTextAction(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold), color = color)
    }
}
