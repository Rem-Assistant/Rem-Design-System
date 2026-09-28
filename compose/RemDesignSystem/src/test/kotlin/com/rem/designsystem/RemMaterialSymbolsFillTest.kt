package com.rem.designsystem

import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.icons.RemMaterialSymbols.SymbolFontFile
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Executable proof that the Android icon fill is a **value, not a claim** (`docs/contracts/icon-registry.md`
 * rule 1). The consent parity fix rests entirely on the two baked static Material Symbols files rendering
 * each registry glyph at the pinned FILL from `cmap → glyph` alone — no `FontVariation`, no GSUB — so the
 * fill is correct on *any* renderer. A comment asserting that is not enough (reviewer P1); this test
 * rasterizes each registry glyph from **both** font files with pure JDK AWT (renderer-independent — it
 * reads the font bytes directly, not through Compose/LayoutLib) and fails when the rendered bitmap does
 * not match the expected fill:
 *
 * 1. **No tofu / no fallback** — every registry codepoint resolves to a real glyph (not `.notdef`) in
 *    both files. A missing glyph on any renderer would reintroduce the drift.
 * 2. **Fill is baked** — for every row whose glyph has a `.fill` twin, the filled file renders strictly
 *    more ink than the outline file for the *same* codepoint, proving the filled file's cmap points at the
 *    filled shape (the substitution `subset.py` bakes) rather than the outline one.
 * 3. **Outline stays outline** — the row with no `.fill` twin (`chevron_right`) renders identically from
 *    both files, proving the baking left non-fillable glyphs untouched.
 * 4. **Routing matches the registry** — `RemMaterialSymbols.fontFileFor(fill)` (the real routing behind
 *    `family(fill)`) sends each registry row's FILL to the file that renders that fill.
 *
 * If the fonts are re-subset and a glyph regresses to outline (the exact drift from #23), this goes red.
 */
class RemMaterialSymbolsFillTest {

    /** One row of `docs/contracts/icon-registry.md`, as the Android side consumes it. */
    private data class Row(
        val name: String,
        val glyph: String,
        val fill: Float,
        /** Material Symbols names a filled variant `<glyph>.fill`; a few glyphs have none. */
        val hasFillTwin: Boolean,
    )

    private val registry = listOf(
        Row("shield_lock (consent hero)", RemMaterialSymbols.ShieldLock, fill = 1f, hasFillTwin = true),
        Row("error (notice card)", RemMaterialSymbols.Error, fill = 1f, hasFillTwin = true),
        Row("shield (privacy row)", RemMaterialSymbols.Shield, fill = 0f, hasFillTwin = true),
        Row("description (terms row)", RemMaterialSymbols.Description, fill = 0f, hasFillTwin = true),
        Row("chevron_right (disclosure)", RemMaterialSymbols.ChevronRight, fill = 0f, hasFillTwin = false),
    )

    private val outlineFont by lazy { loadFont("material_symbols_outlined.ttf") }
    private val filledFont by lazy { loadFont("material_symbols_filled.ttf") }

    @Test
    fun everyRegistryGlyphRendersAtItsPinnedFill() {
        for (row in registry) {
            val cp = row.glyph.codePointAt(0)
            val outlineInk = ink(outlineFont, cp)
            val filledInk = ink(filledFont, cp)

            // (1) No tofu in either file — a fallback/.notdef would silently drift on some renderer.
            assertTrue(
                "${row.name}: missing glyph (tofu) in material_symbols_outlined.ttf",
                outlineInk > 0,
            )
            assertTrue(
                "${row.name}: missing glyph (tofu) in material_symbols_filled.ttf",
                filledInk > 0,
            )

            if (row.hasFillTwin) {
                // (2) The filled file genuinely draws the FILLED shape: strictly more ink than the
                // outline file for the same codepoint. This is what proves the `.fill` glyph is reachable
                // via cmap alone — the whole point of the static-bake.
                assertTrue(
                    "${row.name}: filled file is not more-inked than outline file " +
                        "(outline=$outlineInk filled=$filledInk) — fill is NOT baked; the FILL-drift is back",
                    filledInk > outlineInk * 1.15,
                )
            } else {
                // (3) No `.fill` twin → both files must render the identical outline glyph.
                val ratio = filledInk.toDouble() / outlineInk
                assertEquals(
                    "${row.name}: renders differently across files but has no .fill twin " +
                        "(outline=$outlineInk filled=$filledInk)",
                    1.0, ratio, 0.02,
                )
            }

            // (4) The real routing sends this row's FILL to the file that renders that fill.
            val routed = RemMaterialSymbols.fontFileFor(row.fill)
            val expected = if (row.fill >= 0.5f) SymbolFontFile.Filled else SymbolFontFile.Outlined
            assertEquals("${row.name}: routed to the wrong font file", expected, routed)
            if (row.hasFillTwin) {
                val routedInk = if (routed == SymbolFontFile.Filled) filledInk else outlineInk
                val otherInk = if (routed == SymbolFontFile.Filled) outlineInk else filledInk
                val routedIsMoreInked = routedInk > otherInk
                assertEquals(
                    "${row.name}: FILL ${row.fill} routes to a file whose rendered fill is wrong",
                    row.fill >= 0.5f, routedIsMoreInked,
                )
            }
        }
    }

    /** Ink pixels of a single codepoint, or 0 if the font has no glyph for it (`.notdef`). */
    private fun ink(base: Font, codePoint: Int): Long {
        val font = base.deriveFont(200f)
        val size = 256
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val frc: FontRenderContext = g.fontRenderContext
        val s = String(Character.toChars(codePoint))
        // Missing glyph → .notdef (glyph code 0): treat as tofu, ink 0.
        if (font.createGlyphVector(frc, s).getGlyphCode(0) == 0) {
            g.dispose()
            return 0
        }
        g.color = Color.BLACK
        TextLayout(s, font, frc).draw(g, 28f, 210f)
        g.dispose()
        var ink = 0L
        for (y in 0 until size) {
            for (x in 0 until size) {
                if ((img.getRGB(x, y) ushr 24 and 0xFF) > 32) ink++
            }
        }
        return ink
    }

    /** Resolve a `res/font` asset by walking up from the working dir (module or repo root). */
    private fun loadFont(name: String): Font {
        val relative = "src/main/res/font/$name"
        var dir: File? = File(".").absoluteFile
        while (dir != null) {
            for (candidate in listOf(File(dir, relative), File(dir, "compose/RemDesignSystem/$relative"))) {
                if (candidate.isFile) return Font.createFont(Font.TRUETYPE_FONT, candidate)
            }
            dir = dir.parentFile
        }
        throw AssertionError("could not locate $relative from ${File(".").absolutePath}")
    }
}
