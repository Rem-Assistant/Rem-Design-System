#!/usr/bin/env python3
"""Subset the Material Symbols Outlined *variable* font to the glyphs the icon
registry (`docs/contracts/onboarding-consent.md` + `docs/contracts/icon-registry.md`)
needs on Android, producing the two **static** `res/font/` assets
`RemMaterialSymbols` binds to.

Two files, one per FILL: `material_symbols_outlined.ttf` (FILL 0, the outline
rows + chevron) and `material_symbols_filled.ttf` (FILL 1, the shield-lock hero +
error notice). `RemMaterialSymbols.family(fill)` routes a glyph to the file whose
fill matches.

**Why fully static, cmap-baked (not a variable font kept at an axis default).**
In Material Symbols the FILL axis does not interpolate a glyph's outline — it
*substitutes* a different glyph (`shield` → `shield.fill`) through a GSUB feature.
So a font that "keeps the FILL axis with a baked default" still renders the fill
correctly ONLY if the runtime honours the font's variation coords AND applies the
substitution feature. The render runner (Paparazzi / LayoutLib) does neither
reliably, and even on real Android it is a dependency, not a value. Instead this
tool bakes the result: it fully instances at the target FILL (dropping the axis)
and then, for the filled file, **rewrites the cmap so each codepoint maps directly
to its `.fill` glyph and drops GSUB entirely**. The outputs are plain static fonts
whose `cmap → glyph` needs no variation setting and no OpenType feature to render
the correct fill — so the fill is a value on ANY renderer, which is exactly what
the visual-parity gate diffs.

Source: google/material-design-icons variablefont (Apache-2.0). Run:

    pip install fonttools
    python3 tools/material-symbols/subset.py [path-to-full-variable-ttf]

If no path is given it downloads the upstream variable font. The upstream
codepoints (verified against the repo's `.codepoints` file):
    error e000 (legacy PUA, also f8b6) · shield_lock f686 · shield e9e0
    description e873 · chevron_right e5cc
"""
import sys
import urllib.request
from pathlib import Path

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

UPSTREAM = (
    "https://raw.githubusercontent.com/google/material-design-icons/master/"
    "variablefont/MaterialSymbolsOutlined%5BFILL%2CGRAD%2Copsz%2Cwght%5D.ttf"
)

# Codepoint per registry row. `error` keeps the legacy U+E000 the prior subset
# used (present in the upstream font alongside U+F8B6) so `RemMaterialSymbols.Error`
# is unchanged.
GLYPHS = {
    "error": 0xE000,
    "shield_lock": 0xF686,
    "shield": 0xE9E0,
    "description": 0xE873,
    "chevron_right": 0xE5CC,
}

# Both outputs carry ALL registry codepoints so an off-fill request still renders the
# glyph (at the file's fill) instead of tofu — a safe fallback, future-proof as rows grow.
OUTLINED = {"name": "material_symbols_outlined.ttf", "fill": 0.0}
FILLED = {"name": "material_symbols_filled.ttf", "fill": 1.0}

OUT_DIR = Path(__file__).resolve().parents[2] / "compose/RemDesignSystem/src/main/res/font"


def load_full(path: str | None) -> str:
    if path:
        return path
    dest = "/tmp/MaterialSymbolsOutlined-variable.ttf"
    if not Path(dest).exists():
        print(f"downloading {UPSTREAM}")
        urllib.request.urlretrieve(UPSTREAM, dest)
    return dest


def bake_fill_into_cmap(font: TTFont) -> None:
    """Rewrite every cmap subtable so a codepoint maps directly to its `.fill` glyph
    (Material Symbols names the filled variant `<glyph>.fill`), then drop GSUB. After
    this the filled shape is reached by `cmap → glyph` alone — no feature, no variation
    — so any renderer draws the fill. Codepoints without a `.fill` twin (chevron_right)
    are left untouched."""
    glyph_set = set(font.getGlyphOrder())
    for table in font["cmap"].tables:
        remapped = {}
        for cp, name in table.cmap.items():
            fill_name = f"{name}.fill"
            remapped[cp] = fill_name if fill_name in glyph_set else name
        table.cmap = remapped
    if "GSUB" in font:
        del font["GSUB"]


def build(full_path: str, spec: dict) -> None:
    unicodes = list(GLYPHS.values())
    font = TTFont(full_path)
    # Subset to the wanted codepoints first (small file). The subsetter follows the
    # GSUB closure, so the `.fill` substitution targets are retained too.
    ss = subset.Subsetter(subset.Options(
        recalc_timestamp=False,
        name_IDs=["*"],
        name_legacy=True,
        name_languages=["*"],
        glyph_names=True,
    ))
    ss.populate(unicodes=unicodes)
    ss.subset(font)
    # Pin every axis (FILL included) to produce a STATIC font — no fvar, no variation
    # dependency at runtime.
    instancer.instantiateVariableFont(
        font,
        {"opsz": 24, "wght": 400, "GRAD": 0, "FILL": spec["fill"]},
        inplace=True,
        updateFontNames=False,
    )
    if spec["fill"] >= 0.5:
        # Filled file: bake the fill glyphs into the cmap so no GSUB/variation is needed.
        bake_fill_into_cmap(font)
    elif "GSUB" in font:
        # Outline file needs no substitution; drop any residual GSUB for a clean static font.
        del font["GSUB"]
    out = OUT_DIR / spec["name"]
    font.save(out)
    cmap = font.getBestCmap()
    print(f"wrote {out} ({out.stat().st_size} bytes) fill={spec['fill']} "
          f"variable={'fvar' in font} gsub={'GSUB' in font} "
          f"cmap={ {hex(c): cmap[c] for c in sorted(cmap)} }")


def main() -> None:
    full_path = load_full(sys.argv[1] if len(sys.argv) > 1 else None)
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    build(full_path, OUTLINED)
    build(full_path, FILLED)


if __name__ == "__main__":
    main()
