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
    /** Which baked static font file a pinned registry FILL routes to. */
    internal enum class SymbolFontFile { Outlined, Filled }

    /**
     * Route an exact registry FILL to the matching static font. Only the pinned values `0f` (outline)
     * and `1f` (filled) are valid. Material Symbols may expose a continuous axis upstream, but these
     * packaged fonts are two discrete baked assets; accepting an intermediate value would silently
     * choose the wrong contract. Pure and Android-free so the fill-parity test binds to real routing.
     */
    internal fun fontFileFor(fill: Float): SymbolFontFile = when (fill) {
        0f -> SymbolFontFile.Outlined
        1f -> SymbolFontFile.Filled
        else -> throw IllegalArgumentException(
            "Material Symbol FILL must be pinned to exactly 0f or 1f; received $fill",
        )
    }

    /**
     * The Material Symbols family for an exact pinned FILL (`0f` outline or `1f` filled). Any other
     * value is rejected rather than rounded to one of the two baked assets.
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

    // Settings New: official google/material-design-icons codepoints; native semantic mapping.
    val Info = RemMaterialSymbol("Settings info", "info", 0xE88E, 1f)
    val Billing = RemMaterialSymbol("Settings billing", "credit_card", 0xE8A1, 1f)
    val Permissions = RemMaterialSymbol("Settings permissions", "pan_tool", 0xE925, 1f)
    val Share = RemMaterialSymbol("Settings share", "ios_share", 0xE6B8, 0f)
    val Devices = RemMaterialSymbol("Settings devices", "devices", 0xE326, 0f)
    val Connectors = RemMaterialSymbol("Settings connectors", "link", 0xE250, 1f)
    val Browser = RemMaterialSymbol("Settings browser", "language", 0xEA07, 0f)
    val Automations = RemMaterialSymbol("Settings automations", "notifications_active", 0xE7F7, 1f)
    val Memory = RemMaterialSymbol("Settings memory", "psychology", 0xEA4A, 0f)
    val Models = RemMaterialSymbol("Settings models", "memory", 0xE322, 0f)
    val Wallet = RemMaterialSymbol("Settings wallet", "wallet", 0xF8FF, 0f)
    val Voice = RemMaterialSymbol("Settings voice", "graphic_eq", 0xE1B8, 0f)
}
