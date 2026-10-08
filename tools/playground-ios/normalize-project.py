"""Retain the Xcode 14-compatible format for this ordinary-group project."""
from pathlib import Path
import re

project = Path(__file__).with_name("RemSettingsPlayground.xcodeproj") / "project.pbxproj"
source = project.read_text()
if "PBXFileSystemSynchronized" in source:
    raise SystemExit("Synchronized groups require a newer Xcode; do not downgrade this project.")
source = re.sub(r"objectVersion = \d+;", "objectVersion = 56;", source, count=1)
source = re.sub(r"^\s*preferredProjectObjectVersion = \d+;\n", "", source, flags=re.MULTILINE)
project.write_text(source)
