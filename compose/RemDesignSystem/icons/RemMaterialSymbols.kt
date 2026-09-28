package com.rem.designsystem.icons

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
 * This binds the **Material Symbols variable font** (`res/font/material_symbols_outlined.ttf`, the
 * Apache-2.0 Google font subset to the glyphs the registry needs) with its **FILL axis** (0 = outline,
 * 1 = filled). That FILL axis is what kills the "iOS filled / Android outline" drift the visual gate
 * exists to catch: a registry row that is FILL 1 on iOS renders at FILL 1 here — a value, not a guess.
 * This is deliberately **not** `androidx.compose.material.icons.Icons.Filled.*`: the legacy Material
 * Icons set is always-filled and has no FILL axis, so it cannot honour an outline row (registry rule 2).
 *
 * Render a glyph as a `Text` node: `Text(RemMaterialSymbols.Error, fontFamily = RemMaterialSymbols.family(fill = 1f))`.
 */
object RemMaterialSymbols {
    /**
     * The Material Symbols family at a given FILL (0f outline … 1f filled). The FILL axis is applied
     * via [FontVariation]; it is honoured by the variable font on API 26+ (and the render runner),
     * matching the fill pinned per registry row. The bundled subset's FILL axis defaults to **1**
     * (its only glyph, `error`, is a FILL-1 row), so the notice glyph renders filled even where a
     * renderer ignores variation settings — the axis is retained, so outline (0f) is still reachable.
     */
    fun family(fill: Float): FontFamily = FontFamily(
        Font(
            resId = R.font.material_symbols_outlined,
            variationSettings = FontVariation.Settings(FontVariation.Setting("FILL", fill)),
        ),
    )

    /**
     * `error` — meaning "error / warning", the notice-card glyph. FILL **1** (pairs with the iOS
     * `exclamationmark.triangle.fill` per the registry). Codepoint U+E000 in Material Symbols.
     */
    const val Error: String = "\uE000"
}
