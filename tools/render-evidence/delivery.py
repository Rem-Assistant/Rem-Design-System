"""Build the canonical paired delivery using trusted publisher-side code."""
import argparse
import fnmatch
import json
from pathlib import Path
import re


def _evidence(root: Path, platform: str) -> dict[str, Path]:
    evidence: dict[str, Path] = {}
    for path in sorted((root / platform).glob("*.png")):
        key = path.stem.lower()
        if platform == "compose":
            key = re.sub(r"^com\.rem\.designsystem_evidencesnapshots_[^_]+_", "", key)
        if not re.fullmatch(r"[a-z0-9-]+", key):
            raise ValueError(f"Invalid evidence name: {path.name}")
        if key in evidence:
            raise ValueError(f"Duplicate evidence for {platform} {key}")
        evidence[key] = path.resolve()
    return evidence


def prepare(root: Path, head: str, conclusion: str, changed: list[str], contracts: dict,
            primary_contract: str | None = None) -> tuple[str, str, list[Path]]:
    if not re.fullmatch(r"[0-9a-f]{40}", head):
        raise ValueError("Full head SHA required")
    pairs: dict[str, dict[str, Path]] = {}
    for platform in ("swiftui", "compose"):
        for key, path in _evidence(root, platform).items():
            pairs.setdefault(key, {})[platform] = path
    references = _evidence(root, "reference")
    if not pairs:
        raise ValueError("No rendered evidence")
    required = set()
    # When a PR edits a named screen contract, that declaration is the delivery scope.
    # Collateral implementation changes may match another screen's broad path rules, but
    # they should remain regression coverage rather than expanding the review table.
    if primary_contract is not None:
        if primary_contract not in contracts:
            raise ValueError(f"Unknown primary delivery contract: {primary_contract}")
        selected_contracts = {primary_contract}
    else:
        selected_contracts = {
            name for name in contracts
            if f"docs/contracts/{name}.md" in changed
        }
    if selected_contracts:
        for name in selected_contracts:
            required.update(contracts[name]["states"])
    else:
        selected_contracts = {
            name for name, contract in contracts.items()
            if any(fnmatch.fnmatchcase(path, pattern) for path in changed for pattern in contract["paths"])
        }
        for name in selected_contracts:
            contract = contracts[name]
            if any(fnmatch.fnmatchcase(path, pattern) for path in changed for pattern in contract["paths"]):
                required.update(contract["states"])
    missing = [f"{key} / {platform}" for key in sorted(required)
               for platform in ("swiftui", "compose") if platform not in pairs.get(key, {})]
    required_references = {
        key for name in selected_contracts
        for key in contracts[name].get("references", {})
    }
    undeclared_references = required_references - required
    if undeclared_references:
        raise ValueError(
            "Contract references are not declared states: " + ", ".join(sorted(undeclared_references))
        )
    missing += [f"{key} / figma-reference" for key in sorted(required_references)
                if key not in references]
    waypoint_keys = {
        key for name in selected_contracts
        for key in contracts[name].get("waypoints", {})
    }
    missing += [f"{key} / figma-reference" for key in sorted(waypoint_keys)
                if key not in references]
    prefixes = {
        prefix for name in selected_contracts
        for prefix in contracts[name].get("exclusive_output_prefixes", [])
    }
    if any(not re.fullmatch(r"[a-z0-9-]+", prefix) for prefix in prefixes):
        raise ValueError("Exclusive output prefixes must be lowercase kebab-case")
    observed_keys = set(pairs) | set(references)
    unexpected = sorted(
        key for key in observed_keys
        if any(key.startswith(prefix) for prefix in prefixes) and key not in required and key not in waypoint_keys
    )
    status = "ready" if conclusion == "success" and not missing and not unexpected else "failed"
    lines = [f"Current head: `{head}`", "",
             "Paired runner-produced evidence: iOS Simulator (SwiftUI) | Android Paparazzi (Compose).",
             "Rendering and coverage checks do not establish visual parity; Reviewer must compare the pixels against the approved contracts.", ""]
    if missing:
        lines += ["**Required evidence missing — delivery blocked:**", "", *[f"- {item}" for item in missing], ""]
    if unexpected:
        lines += ["**Unexpected contract-prefixed output — delivery blocked:**", "",
                  *[f"- {item}" for item in unexpected], ""]
    if conclusion != "success":
        lines += ["**Render workflow did not pass.** Builder must repair its failing checks.", ""]
    include_reference_column = bool(references or required_references)
    header = "| Screen / component · state | iOS · SwiftUI Render | Android · Compose Render"
    divider = "|---|:---:|:---:"
    if include_reference_column:
        header += " | Figma Reference"
        divider += "|:---:"
    lines += [header + " |", divider + " |"]
    attachments = []
    # A contract-scoped delivery is deliberately narrow: Reviewer sees exactly the required states
    # for the changed screen, while the full renderer may still exercise regression/component
    # snapshots in CI. Unscoped changes retain the complete gallery as a useful fallback.
    delivery_keys = sorted(required) if required else sorted(pairs)
    for key in delivery_keys:
        cells = []
        for platform in ("swiftui", "compose"):
            path = pairs.get(key, {}).get(platform)
            if path:
                cells.append(f"![{platform} Render {key}]({path})")
                attachments.append(path)
            else:
                cells.append("**Not rendered**")
        if include_reference_column:
            path = references.get(key)
            if path:
                cells.append(f"![Figma Reference {key}]({path})")
                attachments.append(path)
            else:
                cells.append("**Not exported**")
        lines.append(f"| `{key}` | {' | '.join(cells)} |")
    if waypoint_keys:
        lines += ["", "Authenticated Figma structure waypoints:", "",
                  "| Waypoint | Figma Reference |", "|---|:---:|"]
        for key in sorted(waypoint_keys):
            path = references.get(key)
            if path:
                lines.append(f"| `{key}` | ![Figma Reference {key}]({path}) |")
                attachments.append(path)
            else:
                lines.append(f"| `{key}` | **Not exported** |")
    lines += ["", "Rows marked **Not rendered** are not evidence of cross-platform parity.",
              "", "_Generated by Agent Factory. Delivery workflow authored by Codex._"]
    return status, "\n".join(lines) + "\n", attachments


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--evidence-root", type=Path, required=True)
    parser.add_argument("--head", required=True)
    parser.add_argument("--conclusion", required=True)
    parser.add_argument("--changed-paths", type=Path, required=True)
    parser.add_argument("--primary-contract")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    contracts = json.loads(Path(__file__).with_name("contracts.json").read_text())
    status, body, attachments = prepare(args.evidence_root, args.head, args.conclusion,
                                        args.changed_paths.read_text().splitlines(), contracts,
                                        args.primary_contract)
    args.output.mkdir(parents=True, exist_ok=True)
    (args.output / "body.md").write_text(body)
    (args.output / "status").write_text(status)
    (args.output / "attachments.json").write_text(json.dumps([str(p) for p in attachments]))


if __name__ == "__main__":
    main()
