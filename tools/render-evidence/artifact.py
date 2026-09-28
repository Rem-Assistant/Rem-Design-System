"""Validate workflow artifacts before trusted delivery code can consume them."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re


def validate(root: Path, *, pr: int, sha: str, run_id: int, run_attempt: int,
             workflow_sha256: str, media_dirs: tuple[str, ...],
             figma_file_key: str | None = None, contracts_sha256: str | None = None,
             require_reference_export: bool = False) -> dict:
    if not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise ValueError("full lowercase head SHA required")
    if not re.fullmatch(r"[0-9a-f]{64}", workflow_sha256):
        raise ValueError("full lowercase workflow digest required")
    if not media_dirs or any(not re.fullmatch(r"[a-z0-9-]+", item) for item in media_dirs):
        raise ValueError("safe media directories required")
    root = root.resolve()
    manifest_path = (root / "delivery/manifest.json").resolve()
    if root not in manifest_path.parents or not manifest_path.is_file():
        raise ValueError("missing or unsafe evidence manifest")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if manifest.get("version", 1) != 1:
        raise ValueError("unsupported evidence manifest version")
    if manifest.get("pr") != pr or manifest.get("sha") != sha:
        raise ValueError("evidence manifest does not match the triggering PR head")
    if manifest.get("run_id") != run_id or manifest.get("run_attempt") != run_attempt:
        raise ValueError("evidence manifest does not match the triggering run attempt")
    if manifest.get("workflow_sha256") != workflow_sha256:
        raise ValueError("evidence was not produced by the trusted base-branch workflow")
    if figma_file_key is not None and manifest.get("figma_file_key") != figma_file_key:
        raise ValueError("Figma evidence names an unexpected file")
    if contracts_sha256 is not None and manifest.get("contracts_sha256") != contracts_sha256:
        raise ValueError("Figma evidence used an unexpected reference contract")
    if require_reference_export and manifest.get("reference_export_status") != "success":
        raise ValueError("Figma reference export did not succeed")

    media = manifest.get("media_sha256")
    if not isinstance(media, dict) or not media:
        raise ValueError("evidence declares no media")
    actual = {
        path.relative_to(root).as_posix()
        for directory in media_dirs
        for path in (root / directory).glob("*.png")
    }
    if actual != set(media):
        raise ValueError("artifact images do not exactly match the authenticated manifest")
    allowed = "|".join(re.escape(directory) for directory in media_dirs)
    for relative, expected_digest in media.items():
        if not isinstance(relative, str) or not re.fullmatch(
            rf"(?:{allowed})/[A-Za-z0-9._-]+\.png", relative
        ):
            raise ValueError(f"unexpected media path: {relative}")
        if not isinstance(expected_digest, str) or not re.fullmatch(r"[0-9a-f]{64}", expected_digest):
            raise ValueError(f"invalid evidence digest for {relative}")
        path = (root / relative).resolve()
        if root not in path.parents or not path.is_file():
            raise ValueError(f"missing or unsafe evidence path: {relative}")
        if hashlib.sha256(path.read_bytes()).hexdigest() != expected_digest:
            raise ValueError(f"evidence digest mismatch for {relative}")
    return manifest


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--pr", type=int, required=True)
    parser.add_argument("--sha", required=True)
    parser.add_argument("--run-id", type=int, required=True)
    parser.add_argument("--run-attempt", type=int, required=True)
    parser.add_argument("--workflow-sha256", required=True)
    parser.add_argument("--media-dir", action="append", required=True)
    parser.add_argument("--figma-file-key")
    parser.add_argument("--contracts-sha256")
    parser.add_argument("--require-reference-export", action="store_true")
    args = parser.parse_args()
    validate(
        args.root,
        pr=args.pr,
        sha=args.sha,
        run_id=args.run_id,
        run_attempt=args.run_attempt,
        workflow_sha256=args.workflow_sha256,
        media_dirs=tuple(args.media_dir),
        figma_file_key=args.figma_file_key,
        contracts_sha256=args.contracts_sha256,
        require_reference_export=args.require_reference_export,
    )


if __name__ == "__main__":
    main()
