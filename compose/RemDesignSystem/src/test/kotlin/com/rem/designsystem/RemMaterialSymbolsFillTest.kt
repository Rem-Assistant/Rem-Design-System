package com.rem.designsystem

import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.icons.RemMaterialSymbols.SymbolFontFile
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Executable proof that the Android icon fill is baked into static font files.
 *
 * This deliberately parses TrueType bytes instead of using `java.awt`: Android
 * unit-test compilation does not expose the desktop AWT module. The paired
 * Paparazzi evidence proves the rendered pixels; this test proves the mechanism
 * underneath those pixels without depending on a desktop renderer:
 *
 * 1. both assets are static (`fvar` and `GSUB` are absent);
 * 2. every registry codepoint maps to a real glyph in both files;
 * 3. fill-capable rows map to different baked glyph bytes in the two files;
 * 4. `chevron_right`, which has no fill twin, stays byte-identical; and
 * 5. production routing selects the correct asset for each pinned FILL value.
 */
class RemMaterialSymbolsFillTest {

    private data class Row(
        val name: String,
        val glyph: String,
        val fill: Float,
        val hasFillTwin: Boolean,
    )

    private val registry = listOf(
        Row("shield_lock (consent hero)", RemMaterialSymbols.ShieldLock, 1f, true),
        Row("error (notice card)", RemMaterialSymbols.Error, 1f, true),
        Row("shield (privacy row)", RemMaterialSymbols.Shield, 0f, true),
        Row("description (terms row)", RemMaterialSymbols.Description, 0f, true),
        Row("chevron_right (disclosure)", RemMaterialSymbols.ChevronRight, 0f, false),
    )

    private val outline by lazy { TrueTypeFont(loadFontBytes("material_symbols_outlined.ttf")) }
    private val filled by lazy { TrueTypeFont(loadFontBytes("material_symbols_filled.ttf")) }

    @Test
    fun everyRegistryGlyphHasItsPinnedStaticFill() {
        for ((label, font) in listOf("outline" to outline, "filled" to filled)) {
            assertFalse("$label font must not remain variable", font.hasTable("fvar"))
            assertFalse("$label font must not require glyph substitution", font.hasTable("GSUB"))
        }

        for (row in registry) {
            val codePoint = row.glyph.codePointAt(0)
            val outlineGlyph = outline.glyphBytes(codePoint)
            val filledGlyph = filled.glyphBytes(codePoint)

            assertTrue("${row.name}: missing outline glyph", outlineGlyph.isNotEmpty())
            assertTrue("${row.name}: missing filled glyph", filledGlyph.isNotEmpty())
            if (row.hasFillTwin) {
                assertFalse(
                    "${row.name}: outline and filled assets point to the same shape",
                    outlineGlyph.contentEquals(filledGlyph),
                )
            } else {
                assertArrayEquals(
                    "${row.name}: non-fillable glyph changed between assets",
                    outlineGlyph,
                    filledGlyph,
                )
            }

            val expected = if (row.fill >= 0.5f) SymbolFontFile.Filled else SymbolFontFile.Outlined
            assertEquals("${row.name}: routed to the wrong font", expected, RemMaterialSymbols.fontFileFor(row.fill))
        }

        assertNotEquals(
            "filled and outline fonts must be distinct assets",
            outline.bytes.contentHashCode(),
            filled.bytes.contentHashCode(),
        )
    }

    private class TrueTypeFont(val bytes: ByteArray) {
        private data class Table(val offset: Int, val length: Int)

        private val data = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        private val tables: Map<String, Table> = buildMap {
            val count = u16(4)
            repeat(count) { index ->
                val record = 12 + index * 16
                val tag = String(bytes, record, 4, Charsets.US_ASCII)
                put(tag, Table(u32(record + 8), u32(record + 12)))
            }
        }

        init {
            assertTrue("invalid sfnt header", bytes.size >= 12)
            for ((tag, table) in tables) {
                assertTrue("$tag table is outside the font", table.offset >= 0 && table.length >= 0)
                assertTrue("$tag table is truncated", table.offset + table.length <= bytes.size)
            }
        }

        fun hasTable(tag: String): Boolean = tag in tables

        fun glyphBytes(codePoint: Int): ByteArray {
            val glyph = glyphIndex(codePoint)
            assertTrue("U+${codePoint.toString(16)} maps to .notdef", glyph > 0)
            val head = table("head")
            val loca = table("loca")
            val glyf = table("glyf")
            val longOffsets = i16(head.offset + 50) == 1
            fun glyphOffset(index: Int): Int = if (longOffsets) {
                u32(loca.offset + index * 4)
            } else {
                u16(loca.offset + index * 2) * 2
            }
            val start = glyphOffset(glyph)
            val end = glyphOffset(glyph + 1)
            assertTrue("glyph data is empty", end > start)
            assertTrue("glyph data is outside glyf", end <= glyf.length)
            return bytes.copyOfRange(glyf.offset + start, glyf.offset + end)
        }

        private fun glyphIndex(codePoint: Int): Int {
            val cmap = table("cmap")
            val records = u16(cmap.offset + 2)
            var format4 = -1
            repeat(records) { index ->
                val record = cmap.offset + 4 + index * 8
                val subtable = cmap.offset + u32(record + 4)
                if (u16(subtable) == 4) format4 = subtable
            }
            assertTrue("font has no format-4 cmap", format4 >= 0)
            val segCount = u16(format4 + 6) / 2
            val endCodes = format4 + 14
            val startCodes = endCodes + segCount * 2 + 2
            val deltas = startCodes + segCount * 2
            val rangeOffsets = deltas + segCount * 2
            repeat(segCount) { index ->
                val end = u16(endCodes + index * 2)
                val start = u16(startCodes + index * 2)
                if (codePoint !in start..end) return@repeat
                val delta = i16(deltas + index * 2)
                val rangeOffsetAddress = rangeOffsets + index * 2
                val rangeOffset = u16(rangeOffsetAddress)
                if (rangeOffset == 0) return (codePoint + delta) and 0xFFFF
                val glyph = u16(rangeOffsetAddress + rangeOffset + (codePoint - start) * 2)
                return if (glyph == 0) 0 else (glyph + delta) and 0xFFFF
            }
            return 0
        }

        private fun table(tag: String): Table = requireNotNull(tables[tag]) { "missing $tag table" }
        private fun u16(offset: Int): Int = data.getShort(offset).toInt() and 0xFFFF
        private fun i16(offset: Int): Int = data.getShort(offset).toInt()
        private fun u32(offset: Int): Int = data.getInt(offset).toLong().and(0xFFFF_FFFFL).toInt()
    }

    private fun loadFontBytes(name: String): ByteArray {
        val relative = "src/main/res/font/$name"
        var directory: File? = File(".").absoluteFile
        while (directory != null) {
            for (candidate in listOf(File(directory, relative), File(directory, "compose/RemDesignSystem/$relative"))) {
                if (candidate.isFile) return candidate.readBytes()
            }
            directory = directory.parentFile
        }
        throw AssertionError("could not locate $relative from ${File(".").absolutePath}")
    }
}
