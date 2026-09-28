@file:OptIn(ExperimentalTextApi::class)

package com.rem.designsystem.icons

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.rem.designsystem.R

/**
 * **Material Symbols** — the Android half of `docs/contracts/icon-registry.md`. Icons are *font
 * glyphs* on both platforms (SF Symbols on iOS, Material Symbols here), never SVG components, so a row
 * is matched by **meaning + FILL + weight** and can never drift into "whatever Material icon looks
 * close."
 *
 * This binds the **Material Symbols variable font** (subset by `tools/material-symbols/subset.py` from
 * the Apache-2.0 Google variable font to only the glyphs the registry needs) with its **FILL axis**
 * (0 = outline, 1 = filled). That FILL axis is what kills the "iOS filled / Android outline" drift the
 * visual gate exists to catch: a registry row that is FILL 1 on iOS renders at FILL 1 here — a value,
 * not a guess. This is deliberately **not** `androidx.compose.material.icons.Icons.Filled.*`: the
 * legacy Material Icons set is always-filled and has no FILL axis, so it cannot honour an outline row
 * (registry rule 2).
 *
 * Render a glyph as a `Text` node:
 * `Text(RemMaterialSymbols.ShieldLock, fontFamily = RemMaterialSymbols.family(fill = 1f))`.
 *
 * **Two font files, one per baked FILL default.** The FILL axis is retained in both, so
 * `FontVariation.Setting("FILL", …)` still applies where the renderer honours it (real Android). But
 * the render runner (Paparazzi / LayoutLib) does not reliably honour font variation settings, so
 * [family] routes to the file whose *baked default* already matches the requested fill: outline rows
 * come from a FILL-0 file, filled rows (the shield-lock hero, the error notice) from a FILL-1 file.
 * That makes the fill correct on ANY renderer — the fill is a value, not a hope the renderer varies —
 * which is exactly what the parity gate diffs. See `tools/material-symbols/subset.py`.
 *
 * `FontVariation.Setting(name, value)` (the custom-axis constructor) is `@ExperimentalTextApi`, so this
 * file opts in at the top; the FILL axis it drives is a stable OpenType variation.
 */
object RemMaterialSymbols {
    /**
     * The Material Symbols family at a given FILL (0f outline … 1f filled). Picks the font file whose
     * baked default matches (so the fill is correct even on a renderer that ignores variation), and
     * still applies the FILL variation so a renderer that honours it lands on the exact axis value.
     * Threshold at the axis midpoint: `fill >= 0.5f` → the filled subset, else the outline subset.
     */
    fun family(fill: Float): FontFamily {
        val resId = if (fill >= 0.5f) R.font.material_symbols_filled else R.font.material_symbols_outlined
        return FontFamily(
            Font(
                resId = resId,
                variationSettings = FontVariation.Settings(FontVariation.Setting("FILL", fill)),
            ),
        )
    }

    /**
     * `shield_lock` — meaning "privacy / lock-shield", the consent hero. FILL **1** (pairs with the iOS
     * `lock.shield.fill` per the registry). NOT `security` (a shield-*check*, a different glyph the
     * registry flags as a near-miss). Codepoint U+F686 in Material Symbols.
     */
    const val ShieldLock: String = ""

    /**
     * `shield` — meaning "privacy policy", the legal row leading glyph. FILL **0** (outline; pairs with
     * the iOS `shield`). Codepoint U+E9E0.
     */
    const val Shield: String = ""

    /**
     * `description` — meaning "terms / document", the legal row leading glyph. FILL **0** (outline;
     * pairs with the iOS `doc.text`). Codepoint U+E873.
     */
    const val Description: String = ""

    /**
     * `chevron_right` — meaning "disclosure chevron", the list-row trailing accessory. FILL **0**
     * (outline; pairs with the iOS `chevron.right`). Codepoint U+E5CC.
     */
    const val ChevronRight: String = ""

    /**
     * `error` — meaning "error / warning", the notice-card glyph. FILL **1** (pairs with the iOS
     * `exclamationmark.triangle.fill` per the registry). Codepoint U+E000 in Material Symbols.
     */
    const val Error: String = ""
}
