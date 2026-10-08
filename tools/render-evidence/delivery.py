"""Build the canonical paired delivery using trusted publisher-side code."""
import argparse
import fnmatch
import json
from pathlib import Path
import re


FIGMA_NODE_ID = re.compile(r"^(?:I)?\d+:\d+(?:;\d+:\d+)*$")


def _markdown_cell(value: object) -> str:
    return str(value).replace("\n", " ").replace("\r", " ").replace("|", "\\|").replace("`", "'")


def _authenticated_structure_proof(root: Path, head: str, contract: dict) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    report_path = root / "structure/report.json"
    if not report_path.is_file():
        return [], ["Authenticated Figma structure report is missing"]
    try:
        report = json.loads(report_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        return [], [f"Authenticated Figma structure report is invalid: {error}"]
    if not isinstance(report, dict) or report.get("status") != "completed" or report.get("head") != head:
        return [], ["Authenticated Figma structure report does not match the completed exact-head verification"]
    structure = report.get("structure")
    if not isinstance(structure, dict):
        return [], ["Authenticated Figma structure report has no verified structure"]

    canonical = structure.get("canonicalScreens")
    documented = structure.get("screens")
    if not isinstance(canonical, list) or not isinstance(documented, list):
        return [], ["Authenticated Figma structure report has no canonical/documentation screen proof"]
    reference_rows = []
    for key, expected in contract.get("references", {}).items():
        expected_node = expected.get("node")
        canonical_matches = [entry for entry in canonical
                             if isinstance(entry, dict) and entry.get("id") == expected_node]
        if len(canonical_matches) != 1 or canonical_matches[0].get("status") != "conformant":
            errors.append(f"{key} canonical component proof is missing or nonconformant")
            continue
        if not expected.get("requireDocumentation", True):
            reference_rows.append((key, expected_node, canonical_matches[0]["status"], "—", "not required"))
            continue
        documentation_matches = [entry for entry in documented
                                 if isinstance(entry, dict) and str(entry.get("name", "")).lower() == key]
        if len(documentation_matches) != 1:
            errors.append(f"{key} must have exactly one documentation instance proof")
            continue
        documentation = documentation_matches[0]
        resolved_node = documentation.get("resolvedNode")
        if (documentation.get("status") != "conformant" or
                documentation.get("componentId") != expected_node or
                not isinstance(resolved_node, str) or not FIGMA_NODE_ID.fullmatch(resolved_node)):
            errors.append(f"{key} documentation instance proof is missing or mismatched")
            continue
        reference_rows.append((key, expected_node, canonical_matches[0]["status"],
                               resolved_node, documentation["status"]))

    require_prototype = contract.get("requirePrototype", True)
    prototype = structure.get("prototype")
    if not require_prototype:
        prototype = {}
    elif not isinstance(prototype, dict):
        errors.append("Authenticated Figma prototype proof is missing")
        prototype = {}
    starts = prototype.get("verifiedFlowStartingPoints")
    flow_start_verification = prototype.get("flowStartVerification")
    frames = prototype.get("frames")
    if require_prototype and (not isinstance(starts, list) or not starts):
        errors.append("Authenticated Figma prototype flow-start proof is missing")
        starts = []
    if require_prototype and flow_start_verification not in {"figma-rest", "interaction-graph"}:
        errors.append("Authenticated Figma prototype flow-start verification source is missing")
    if require_prototype and (not isinstance(frames, list) or not frames):
        errors.append("Authenticated Figma prototype frame proof is missing")
        frames = []

    start_by_node: dict[str, list[str]] = {}
    for start in starts if require_prototype else []:
        if not isinstance(start, dict) or not isinstance(start.get("nodeId"), str) or not FIGMA_NODE_ID.fullmatch(start["nodeId"]):
            errors.append("Authenticated Figma prototype has an invalid flow start")
            continue
        start_by_node.setdefault(start["nodeId"], []).append(
            f"{_markdown_cell(start.get('name', 'Unnamed flow'))} ({flow_start_verification})"
        )
    prototype_rows = []
    for frame in frames if require_prototype else []:
        if not isinstance(frame, dict):
            errors.append("Authenticated Figma prototype has an invalid frame proof")
            continue
        node = frame.get("node")
        destinations = frame.get("destinations")
        back_count = frame.get("backCount")
        navigations = frame.get("navigations")
        back_actions = frame.get("backActions")
        if (not isinstance(node, str) or not FIGMA_NODE_ID.fullmatch(node) or
                frame.get("status") != "conformant" or
                not isinstance(destinations, list) or
                any(not isinstance(item, str) or not FIGMA_NODE_ID.fullmatch(item) for item in destinations) or
                isinstance(back_count, bool) or not isinstance(back_count, int) or back_count < 0 or
                not isinstance(navigations, list) or not isinstance(back_actions, list)):
            errors.append("Authenticated Figma prototype frame proof is missing or nonconformant")
            continue
        interaction_entries = [*navigations, *back_actions]
        if any(
            not isinstance(action, dict) or
            not isinstance(action.get("sourceNode"), str) or
            not FIGMA_NODE_ID.fullmatch(action["sourceNode"]) or
            not isinstance(action.get("sourceName"), str) or
            not isinstance(action.get("trigger"), str)
            for action in interaction_entries
        ):
            errors.append("Authenticated Figma prototype interaction proof is malformed")
            continue
        raw_destinations = [action.get("destinationId") for action in navigations]
        if (any(not isinstance(item, str) or not FIGMA_NODE_ID.fullmatch(item) for item in raw_destinations) or
                sorted(raw_destinations) != sorted(destinations) or len(back_actions) != back_count):
            errors.append("Authenticated Figma prototype interactions do not match their observed summary")
            continue
        navigation_text = ", ".join(
            f"{_markdown_cell(action['sourceName'])} · `{action['sourceNode']}` → `{action['destinationId']}`"
            for action in navigations
        ) if navigations else "—"
        back_text = ", ".join(
            f"{_markdown_cell(action['sourceName'])} · `{action['sourceNode']}` · {_markdown_cell(action['trigger'])}"
            for action in back_actions
        ) if back_actions else "—"
        prototype_rows.append((
            _markdown_cell(frame.get("name", node)), node,
            start_by_node.get(node, []), navigation_text, back_text,
        ))
    if errors:
        return [], errors

    lines = [
        "Authenticated editable Figma proof:", "",
        "| Required reference | Canonical component | Documentation instance |",
        "|---|---|---|",
    ]
    for key, canonical_node, canonical_status, instance_node, instance_status in reference_rows:
        lines.append(
            f"| `{key}` | `{canonical_node}` · {canonical_status} | `{instance_node}` · {instance_status} |"
        )
    amendments = structure.get("sourceAmendments", [])
    if amendments:
        if not isinstance(amendments, list) or any(not isinstance(item, str) for item in amendments):
            return [], ["Authenticated source amendments are malformed"]
        lines += ["", "Approved implementation amendments to the read-only source:", "",
                  *[f"- {_markdown_cell(item)}" for item in amendments]]
    if require_prototype:
        lines += [
            "", "Authenticated prototype proof:", "",
            "| Prototype frame | Flow start | Navigation destinations | Back actions |",
            "|---|---|---|---:|",
        ]
        for name, node, flow_names, navigation_text, back_text in prototype_rows:
            flow_start = ", ".join(f"{item} → `{node}`" for item in flow_names) if flow_names else "—"
            lines.append(f"| {name} · `{node}` | {flow_start} | {navigation_text} | {back_text} |")
    return lines, []


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
    # Figma is an explicit contract-level capability. Path inference still determines the required
    # SwiftUI/Compose states, but it must not make an unscoped delivery depend on a Figma artifact.
    reference_contracts = selected_contracts if primary_contract is not None else set()
    references = _evidence(root, "reference") if reference_contracts else {}
    required_references = {
        key for name in reference_contracts
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
        key for name in reference_contracts
        for key in contracts[name].get("waypoints", {})
    }
    missing += [f"{key} / figma-reference" for key in sorted(waypoint_keys)
                if key not in references]
    prefixes = {
        prefix for name in reference_contracts
        for prefix in contracts[name].get("exclusive_output_prefixes", [])
    }
    if any(not re.fullmatch(r"[a-z0-9-]+", prefix) for prefix in prefixes):
        raise ValueError("Exclusive output prefixes must be lowercase kebab-case")
    observed_keys = set(pairs) | set(references)
    unexpected = sorted(
        key for key in observed_keys
        if any(key.startswith(prefix) for prefix in prefixes) and key not in required and key not in waypoint_keys
    )
    structure_lines: list[str] = []
    structure_errors: list[str] = []
    if reference_contracts:
        if len(reference_contracts) != 1:
            structure_errors.append("Exactly one scoped contract is required for authenticated Figma structure proof")
        else:
            contract_name = next(iter(reference_contracts))
            structure_lines, structure_errors = _authenticated_structure_proof(
                root, head, contracts[contract_name]
            )
    status = "ready" if conclusion == "success" and not missing and not unexpected and not structure_errors else "failed"
    lines = [f"Current head: `{head}`", "",
             "Paired runner-produced evidence: iOS Simulator (SwiftUI) | Android Paparazzi (Compose).",
             "Comparison basis: iOS uses a 393×852-point viewport at 2×; Android uses the full "
             "Paparazzi `DeviceConfig.PIXEL_6` viewport; Figma references use the canonical 402×874 "
             "frame at export scale. Normalize each image to its full logical viewport before "
             "comparing layout; raw PNG dimensions are not layout differences.",
             "Rendering and coverage checks do not establish visual parity; Reviewer must compare the pixels against the approved contracts.", ""]
    if primary_contract == "settings-foundation":
        lines = [f"Current head: `{head}`", "",
                 "Paired actual Settings playground captures: iOS Simulator UI tests | Android emulator instrumentation tests.",
                 "Comparison basis: each app capture includes its native device chrome; Figma references use the approved source frame. "
                 "Normalize logical viewports before comparing. Device density, native chrome and the listed approved amendments are not pixel-parity failures.",
                 "Rendering and coverage checks do not establish visual parity; Reviewer must compare the pixels against the approved contracts.", ""]
    if missing:
        lines += ["**Required evidence missing — delivery blocked:**", "", *[f"- {item}" for item in missing], ""]
    if unexpected:
        lines += ["**Unexpected contract-prefixed output — delivery blocked:**", "",
                  *[f"- {item}" for item in unexpected], ""]
    if structure_errors:
        lines += ["**Authenticated editable Figma proof invalid — delivery blocked:**", "",
                  *[f"- {item}" for item in structure_errors], ""]
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
    if structure_lines:
        lines += ["", *structure_lines]
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
