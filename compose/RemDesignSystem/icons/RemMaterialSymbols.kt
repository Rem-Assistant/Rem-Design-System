package com.rem.designsystem.icons

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.rem.designsystem.R

/**
 * One verified row from the cross-platform icon registry.
 *
 * [glyphName], [codePoint], and [fill] are deliberately inseparable: callers select a semantic
 * symbol instead of independently choosing a character and font file. That makes a filled registry
 * row such as privacy / lock-shield impossible to accidentally render from the outline asset.
 */
class RemMaterialSymbol internal constructor(
    val meaning: String,
    val glyphName: String,
    val codePoint: Int,
    val fill: Float,
) {
    init {
        require(Character.isValidCodePoint(codePoint)) { "invalid Material Symbols codepoint: $codePoint" }
        require(fill == 0f || fill == 1f) { "registry FILL must be pinned to 0 or 1" }
    }

    val glyph: String = String(Character.toChars(codePoint))
}

/**
 * **Material Symbols** — the Android half of `docs/contracts/icon-registry.md`. Icons are *font
 * glyphs* on both platforms (SF Symbols on iOS, Material Symbols here), never SVG components, so a row
 * is matched by **meaning + FILL + weight** and can never drift into "whatever Material icon looks
 * close."
 *
 * This binds the **Material Symbols** glyphs (subset from the Apache-2.0 Google font by
 * `tools/material-symbols/subset.py` to only the glyphs the registry needs) as two **static** font
 * files, one per FILL: an outline file (FILL 0 — the legal rows + chevron) and a filled file
 * (FILL 1 — the shield-lock hero + error notice). [family] routes a glyph to the file whose fill
 * matches. That FILL split is what kills the "iOS filled / Android outline" drift the visual gate
 * exists to catch: a registry row that is FILL 1 on iOS renders from the filled file here — a value,
 * not a guess.
 *
 * This is deliberately **not** `androidx.compose.material.icons.Icons.Filled.*`: the legacy Material
 * Icons set is always-filled and has no FILL notion, so it cannot honour an outline row
 * (registry rule 2).
 *
 * **Why two static files, not one variable font.** In Material Symbols the FILL axis does not
 * interpolate an outline — it *substitutes* a different glyph (`shield` → `shield.fill`) via an
 * OpenType feature. A variable font kept at an axis default would therefore render the right fill
 * only if the runtime honours variation coords AND applies that feature — which the render runner
 * (Paparazzi / LayoutLib) does not reliably do. So `subset.py` bakes the result: each file is a
 * plain static font whose **`cmap → glyph` already points at the correct fill shape** (the filled
 * file's cmap maps straight to the `.fill` glyphs). No `FontVariation` setting and no GSUB feature is
 * needed to render the correct fill on ANY renderer — the fill is a value, which is exactly what the
 * parity gate diffs. See `tools/material-symbols/subset.py`.
 *
 * Render a registry symbol as a `Text` node:
 * `Text(symbol.glyph, fontFamily = RemMaterialSymbols.family(symbol))`.
 *
 * **Proven, not asserted.** That the two files actually carry the correct baked fill — every registry
 * codepoint present (no tofu) and the filled file's fill-twin glyphs genuinely more-inked than the
 * outline file's — is verified by `RemMaterialSymbolsFillTest`. In addition,
 * `verifyMaterialSymbolResources` is a `preBuild` dependency: every Android assembly fails before
 * resource packaging unless both files are present, fully static, GSUB-free, distinct, and contain
 * every registry codepoint. A missing or drifted asset therefore cannot silently render tofu in a
 * shipped AAR. See `docs/contracts/icon-registry.md` rule 1.
 */
object RemMaterialSymbols {
    /** Which baked static font file a requested FILL routes to. Two files, one per fill bucket. */
    internal enum class SymbolFontFile { Outlined, Filled }

    /**
     * Route a requested FILL (0f outline … 1f filled) to the static font file whose baked glyphs match.
     * Threshold: `fill >= 0.5f` picks the filled file. Pure and Android-free so the fill-parity test can
     * bind to the *real* routing rather than a copy of it.
     */
    internal fun fontFileFor(fill: Float): SymbolFontFile =
        if (fill >= 0.5f) SymbolFontFile.Filled else SymbolFontFile.Outlined

    /**
     * The Material Symbols family at a given FILL (0f outline … 1f filled). Picks the static font file
     * whose baked glyphs match — outline below the axis midpoint, filled at/above it — so the fill is
     * correct on any renderer without relying on variation settings. Threshold: `fill >= 0.5f`.
     */
    fun family(fill: Float): FontFamily {
        val resId = when (fontFileFor(fill)) {
            SymbolFontFile.Filled -> R.font.material_symbols_filled
            SymbolFontFile.Outlined -> R.font.material_symbols_outlined
        }
        return FontFamily(Font(resId = resId))
    }

    /** The baked static font family pinned by a semantic registry row. */
    fun family(symbol: RemMaterialSymbol): FontFamily = family(fill = symbol.fill)

    /**
     * `shield_lock` — meaning "privacy / lock-shield", the consent hero. FILL **1** (pairs with the iOS
     * `lock.shield.fill` per the registry). NOT `security` (a shield-*check*, a different glyph the
     * registry flags as a near-miss). Codepoint U+F686 in Material Symbols.
     */
    val PrivacyLockShield = RemMaterialSymbol(
        meaning = "privacy / lock-shield",
        glyphName = "shield_lock",
        codePoint = 0xF686,
        fill = 1f,
    )

    /**
     * `shield` — meaning "privacy policy", the legal row leading glyph. FILL **0** (outline; pairs with
     * the iOS `shield`). Codepoint U+E9E0.
     */
    val PrivacyPolicy = RemMaterialSymbol(
        meaning = "privacy policy",
        glyphName = "shield",
        codePoint = 0xE9E0,
        fill = 0f,
    )

    /**
     * `description` — meaning "terms / document", the legal row leading glyph. FILL **0** (outline;
     * pairs with the iOS `doc.text`). Codepoint U+E873.
     */
    val TermsDocument = RemMaterialSymbol(
        meaning = "terms / document",
        glyphName = "description",
        codePoint = 0xE873,
        fill = 0f,
    )

    /**
     * `chevron_right` — meaning "disclosure chevron", the list-row trailing accessory. FILL **0**
     * (outline; pairs with the iOS `chevron.right`). Codepoint U+E5CC.
     */
    val DisclosureChevron = RemMaterialSymbol(
        meaning = "disclosure chevron",
        glyphName = "chevron_right",
        codePoint = 0xE5CC,
        fill = 0f,
    )

    /**
     * `error` — meaning "error / warning", the notice-card glyph. FILL **1** (pairs with the iOS
     * `exclamationmark.triangle.fill` per the registry). Codepoint U+E000 in Material Symbols.
     */
    val ErrorNotice = RemMaterialSymbol(
        meaning = "error / warning",
        glyphName = "error",
        codePoint = 0xE000,
        fill = 1f,
    )
}
