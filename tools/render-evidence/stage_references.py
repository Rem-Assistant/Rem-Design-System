"""Stage only contract-declared Figma exports into the authenticated reference artifact."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
import shutil


SAFE_NAME = re.compile(r"[A-Za-z0-9][A-Za-z0-9._ -]*")


def stage(source: Path, destination: Path, contracts: dict,
          primary_contract: str | None = None) -> list[Path]:
    if primary_contract is not None:
        if primary_contract not in contracts:
            raise ValueError(f"Unknown primary delivery contract: {primary_contract}")
        selected = {primary_contract: contracts[primary_contract]}
    else:
        selected = contracts
    names: set[str] = set()
    for contract in selected.values():
        for field in ("references", "waypoints"):
            for item in contract.get(field, {}).values():
                name = item.get("name") if isinstance(item, dict) else None
                if not isinstance(name, str) or SAFE_NAME.fullmatch(name) is None:
                    raise ValueError(f"Unsafe configured reference export name: {name}")
                names.add(name)
    if not names:
        raise ValueError("Selected delivery contract declares no Figma references")
    destination.mkdir(parents=True, exist_ok=True)
    staged = []
    for name in sorted(names):
        source_path = source / f"{name}.png"
        if not source_path.is_file():
            raise ValueError(f"Missing contract reference export: {name}")
        target = destination / source_path.name
        shutil.copyfile(source_path, target)
        staged.append(target)
    actual = {path.name for path in destination.glob("*.png")}
    expected = {f"{name}.png" for name in names}
    if actual != expected:
        raise ValueError("Staged reference set contains undeclared media")
    return staged


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--destination", type=Path, required=True)
    parser.add_argument("--contracts", type=Path, required=True)
    parser.add_argument("--primary-contract")
    args = parser.parse_args()
    contracts = json.loads(args.contracts.read_text(encoding="utf-8"))
    stage(args.source, args.destination, contracts, args.primary_contract)


if __name__ == "__main__":
    main()
