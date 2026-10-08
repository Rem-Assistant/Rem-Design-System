"""Explicit Settings base/head pairs admitted by the trusted manual evidence lane."""
import re


def validate_settings_pair(base_ref: str, head_ref: str) -> None:
    if (base_ref == "codex/settings-integration" and isinstance(head_ref, str)
            and re.fullmatch(r"agent-factory/settings-issue-[0-9]+", head_ref)):
        return
    if (base_ref, head_ref) in {
        ("codex/settings-review-contracts", "codex/settings-review-native"),
        ("codex/settings-review-native", "codex/settings-review-evidence"),
    }:
        return
    raise ValueError("unsupported Settings base/head pair")
