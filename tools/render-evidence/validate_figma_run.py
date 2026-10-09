"""Validate GitHub metadata for an exact-head pull_request_target Figma artifact."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
from settings_routing import EXPANSION_SCOPES, validate_manual_pair


def _positive_int(value: object, label: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or value <= 0:
        raise ValueError(f"{label} must be a positive integer")
    return value


def _mapping(value: object, label: str) -> dict:
    if not isinstance(value, dict):
        raise ValueError(f"{label} must be an object")
    return value


def validate(run: dict, artifact: dict, *, repository: str, pr: int,
             head_sha: str, base_sha: str, base_ref: str, allow_settings_manual: bool = False,
             head_ref: str | None = None, allow_playground_manual: bool = False,
             primary_contract: str = "settings-foundation") -> dict:
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository):
        raise ValueError("full repository name required")
    _positive_int(pr, "PR number")
    for value, label in ((head_sha, "head SHA"), (base_sha, "base SHA")):
        if not isinstance(value, str) or not re.fullmatch(r"[0-9a-f]{40}", value):
            raise ValueError(f"full lowercase {label} required")
    if not isinstance(base_ref, str) or not re.fullmatch(r"[A-Za-z0-9._/-]+", base_ref):
        raise ValueError("safe base ref required")

    run_id = _positive_int(run.get("id"), "run id")
    if run.get("path") != ".github/workflows/design-drift.yml":
        raise ValueError("unexpected Figma workflow path")
    manual = run.get("event") == "workflow_dispatch"
    expansion = primary_contract in EXPANSION_SCOPES
    manual_allowed = allow_playground_manual if expansion else allow_settings_manual
    if run.get("event") != "pull_request_target" and not (manual and manual_allowed):
        raise ValueError("Figma evidence was not produced by an authorized event")
    if manual or expansion or base_ref == "codex/playground-expansion":
        validate_manual_pair(base_ref, head_ref, primary_contract)
    if run.get("status") != "completed" or run.get("conclusion") != "success":
        raise ValueError("Figma evidence run did not complete successfully")
    expected_run_sha = base_sha if manual else head_sha
    if run.get("head_sha") != expected_run_sha:
        raise ValueError("Figma run does not match the trusted base or exact feature head")

    head_repository = _mapping(run.get("head_repository"), "run head repository")
    repository_id = _positive_int(head_repository.get("id"), "repository id")
    if head_repository.get("full_name") != repository:
        raise ValueError("Figma run came from an unexpected repository")
    head_branch = run.get("head_branch")
    if not isinstance(head_branch, str) or not head_branch:
        raise ValueError("Figma run has no head branch")

    if manual:
        if head_branch != base_ref:
            raise ValueError("manual Figma run does not originate from the admitted integration branch")
        # The trusted manual workflow resolves a constrained PR before secrets,
        # then binds its candidate in the authenticated delivery manifest. Its
        # GitHub run/artifact SHA is the trusted workflow base, not the PR head.
    else:
        pull_requests = run.get("pull_requests")
        if not isinstance(pull_requests, list):
            raise ValueError("Figma run has no pull request binding")
        matches = [item for item in pull_requests
                   if isinstance(item, dict) and item.get("number") == pr]
        if len(matches) != 1:
            raise ValueError("Figma run must contain exactly one binding for the expected PR")
        binding = matches[0]
        head = _mapping(binding.get("head"), "PR head binding")
        base = _mapping(binding.get("base"), "PR base binding")
        if head.get("sha") != head_sha or head.get("ref") != head_branch:
            raise ValueError("Figma run PR binding does not match the feature head")
        if head_ref is not None and head.get("ref") != head_ref:
            raise ValueError("Figma run PR binding does not match the expected head ref")
        if base.get("sha") != base_sha or base.get("ref") != base_ref:
            raise ValueError("Figma run PR binding does not match the expected base")
        for side, value in (("head", head), ("base", base)):
            repo = _mapping(value.get("repo"), f"PR {side} repository")
            if repo.get("id") != repository_id:
                raise ValueError(f"Figma run PR {side} repository does not match the trusted repository")

    expected_name = f"figma-reference-evidence-{pr}-{head_sha}"
    if artifact.get("name") != expected_name:
        raise ValueError("unexpected Figma artifact name")
    artifact_run = _mapping(artifact.get("workflow_run"), "artifact workflow run")
    if artifact_run.get("id") != run_id or artifact_run.get("head_sha") != expected_run_sha:
        raise ValueError("Figma artifact does not match the exact producing run and feature head")
    if artifact_run.get("head_branch") != head_branch:
        raise ValueError("Figma artifact does not match the producing branch")
    if artifact_run.get("head_repository_id") != repository_id:
        raise ValueError("Figma artifact head repository does not match the trusted repository")
    if artifact_run.get("repository_id") != repository_id:
        raise ValueError("Figma artifact repository does not match the trusted repository")
    return {"run_id": run_id, "repository_id": repository_id, "head_branch": head_branch}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--run-meta", type=Path, required=True)
    parser.add_argument("--artifact", type=Path, required=True)
    parser.add_argument("--repository", required=True)
    parser.add_argument("--pr", type=int, required=True)
    parser.add_argument("--head-sha", required=True)
    parser.add_argument("--head-ref")
    parser.add_argument("--base-sha", required=True)
    parser.add_argument("--base-ref", required=True)
    parser.add_argument("--allow-settings-manual", action="store_true")
    parser.add_argument("--allow-playground-manual", action="store_true")
    parser.add_argument("--primary-contract", default="settings-foundation")
    args = parser.parse_args()
    validate(
        json.loads(args.run_meta.read_text(encoding="utf-8")),
        json.loads(args.artifact.read_text(encoding="utf-8")),
        repository=args.repository,
        pr=args.pr,
        head_sha=args.head_sha,
        base_sha=args.base_sha,
        base_ref=args.base_ref,
        allow_settings_manual=args.allow_settings_manual,
        head_ref=args.head_ref,
        allow_playground_manual=args.allow_playground_manual,
        primary_contract=args.primary_contract,
    )


if __name__ == "__main__":
    main()
