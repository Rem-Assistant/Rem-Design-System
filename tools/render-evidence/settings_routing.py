"""Explicit Settings base/head pairs admitted by the trusted manual evidence lane."""
import re

EXPANSION_SCOPES = frozenset({"agenda-suggestions", "onboarding-voice"})


def validate_manual_pair(base_ref: str, head_ref: str, primary_contract: str) -> None:
    """Scope and branch pair are one permission; neither independently grants access."""
    if primary_contract in EXPANSION_SCOPES:
        if (base_ref == "codex/playground-expansion" and isinstance(head_ref, str)
                and re.fullmatch(r"agent-factory/playground-issue-[0-9]+", head_ref)):
            return
        raise ValueError("unsupported playground scope/base/head pair")
    if primary_contract != "settings-foundation":
        raise ValueError("unsupported manual delivery scope")
    validate_settings_pair(base_ref, head_ref)


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
