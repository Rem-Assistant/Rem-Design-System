#!/usr/bin/env python3
"""Instance Inter's variable font into the static faces bundled in compose/RemDesignSystem/src/main/res/font.

Android Compose binds the Rem type ramp to Inter (`Inter` in tokens/generated/RemTokens.kt). We ship static
instances, one file per weight, rather than InterVariable.ttf with `FontVariation.Settings`:
  * minSdk is 24 and variation settings need API 26, so on API 24-25 every weight would draw Regular;
  * the render runner (Paparazzi / layoutlib) does not reliably honour variation coordinates, the same reason
    tools/material-symbols/subset.py bakes static files;
  * four statics (~1.37 MB) are larger than the variable font (~0.88 MB), an accepted cost for exact weights on
    every API level and deterministic renders.

Source: InterVariable.ttf from the Inter 4.1 release (https://github.com/rsms/inter/releases/tag/v4.1,
Inter-4.1.zip, SHA-256 9883fdd4a49d4fb66bd8177ba6625ef9a64aa45899767dde3d36aa425756b11e; InterVariable.ttf SHA-256
4989b125924991b90d05b2d16e0e388c48f7d5bb8b30539bbf9c755278d0ccaf), font version "4.001;git-9221beed3". SIL Open Font License 1.1, see tools/inter/OFL.txt; each
instance keeps the font's own copyright and licence name records (IDs 0, 13, 14).

Process: fontTools `instancer.instantiateVariableFont(font, {"opsz": 14, "wght": W})` (the Text optical size,
the variable font's default), no renaming. The bundled Regular (400) and Bold (700) were made this way: re-running
it reproduces them table for table (only head timestamps differ). Head timestamps are copied from the source so a
rerun is byte-stable.

Usage: python3 tools/inter/instance.py <path/to/InterVariable.ttf> [weight ...]   (default: 500 600)
"""
import sys
from pathlib import Path

from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

ROOT = Path(__file__).resolve().parents[2]
FONT_DIR = ROOT / "compose" / "RemDesignSystem" / "src" / "main" / "res" / "font"
EXPECTED_VERSION = "4.001;git-9221beed3;RSMS;InterVariable-Regular"
FILES = {400: "inter_regular.ttf", 500: "inter_medium.ttf", 600: "inter_semibold.ttf", 700: "inter_bold.ttf"}


def main() -> None:
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    source = Path(sys.argv[1])
    weights = [int(w) for w in sys.argv[2:]] or [500, 600]
    probe = TTFont(source)
    version = probe["name"].getDebugName(3)
    if version != EXPECTED_VERSION:
        sys.exit(f"expected InterVariable {EXPECTED_VERSION!r}, got {version!r}")
    created, modified = probe["head"].created, probe["head"].modified
    for weight in weights:
        if weight not in FILES:
            sys.exit(f"no file name for weight {weight}; known: {sorted(FILES)}")
        font = instancer.instantiateVariableFont(TTFont(source, recalcTimestamp=False), {"opsz": 14, "wght": weight})
        assert "fvar" not in font and font["OS/2"].usWeightClass == weight
        font["head"].created, font["head"].modified = created, modified
        out = FONT_DIR / FILES[weight]
        font.save(out, reorderTables=True)
        font.close()
        print(f"wrote {out.relative_to(ROOT)} ({out.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
