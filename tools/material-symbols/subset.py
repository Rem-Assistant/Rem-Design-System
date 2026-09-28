#!/usr/bin/env python3
"""Subset the Material Symbols Outlined *variable* font to the glyphs the icon
registry (`docs/contracts/icon-registry.md`) needs on Android, producing the
`res/font/` assets `RemMaterialSymbols` binds to.

Why two files instead of one variable font: the FILL axis is retained in each
output (so `FontVariation.Setting("FILL", …)` still applies on a renderer that
honours it), but the *default* is baked per file — outline (0) and filled (1).
The render runner (Paparazzi / LayoutLib) does not reliably honour font
variation settings, so a single default-0 file would render the filled hero and
notice as outlines, and a single default-1 file would fill the outline rows —
either way the visual-parity gate would see an iOS/Android FILL drift. Baking
the default per file makes the fill correct on ANY renderer while keeping the
axis, so real Android (which does honour the axis) is unaffected.

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

# Both outputs carry ALL registry glyphs; they differ only by the baked FILL default. A glyph is
# normally requested at its registry fill (so `family()` routes it to the file whose default matches),
# but carrying every glyph in both files means an off-fill request renders the glyph (at the file's
# fill) instead of tofu — a safe fallback, and future-proof as rows are added.
ALL_GLYPHS = list(GLYPHS.keys())
OUTLINED = {"name": "material_symbols_outlined.ttf", "fill": 0.0, "glyphs": ALL_GLYPHS}
FILLED = {"name": "material_symbols_filled.ttf", "fill": 1.0, "glyphs": ALL_GLYPHS}

OUT_DIR = Path(__file__).resolve().parents[2] / "compose/RemDesignSystem/src/main/res/font"


def load_full(path: str | None) -> str:
    if path:
        return path
    dest = "/tmp/MaterialSymbolsOutlined-variable.ttf"
    if not Path(dest).exists():
        print(f"downloading {UPSTREAM}")
        urllib.request.urlretrieve(UPSTREAM, dest)
    return dest


def build(full_path: str, spec: dict) -> None:
    unicodes = [GLYPHS[g] for g in spec["glyphs"]]
    # Subset to the wanted codepoints first (small file), then partial-instance:
    # pin the non-FILL axes to their defaults and keep FILL as an axis whose
    # default is baked to this file's value (min/max stay 0..1).
    font = TTFont(full_path)
    ss = subset.Subsetter(subset.Options(
        recalc_timestamp=False,
        name_IDs=["*"],
        name_legacy=True,
        name_languages=["*"],
        glyph_names=True,
    ))
    ss.populate(unicodes=unicodes)
    ss.subset(font)
    instancer.instantiateVariableFont(
        font,
        {"opsz": 24, "wght": 400, "GRAD": 0, "FILL": (0.0, spec["fill"], 1.0)},
        inplace=True,
        updateFontNames=False,
    )
    out = OUT_DIR / spec["name"]
    font.save(out)
    cmap = font.getBestCmap()
    print(f"wrote {out} ({out.stat().st_size} bytes) fill={spec['fill']} "
          f"glyphs={[hex(c) for c in sorted(cmap)]}")


def main() -> None:
    full_path = load_full(sys.argv[1] if len(sys.argv) > 1 else None)
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    build(full_path, OUTLINED)
    build(full_path, FILLED)


if __name__ == "__main__":
    main()
