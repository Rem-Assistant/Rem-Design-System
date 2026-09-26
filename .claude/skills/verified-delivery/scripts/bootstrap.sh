#!/usr/bin/env bash
# verified-delivery bootstrap — stand up the tracking system on any GitHub repo.
# Usage:  scripts/bootstrap.sh <owner/repo> [owner-for-project]
# Idempotent: labels use --force; re-running is safe. Requires `gh` authed with `repo` + `project` scopes.
set -euo pipefail

REPO="${1:?usage: bootstrap.sh <owner/repo> [project-owner]}"
OWNER="${2:-${REPO%%/*}}"   # project owner (org or user); defaults to the repo owner

echo "== labels (type · verification · priority) =="
mklabel(){ gh label create "$1" --color "$2" --description "$3" --force -R "$REPO" >/dev/null && echo "  $1"; }
# type
mklabel nit           FBCA04 "Small fix / polish from feedback"
mklabel bug           D73A4A "Broken when you drive the real product"
mklabel gap           D93F0B "Spec'd but not built / unwired"
mklabel feature       0E8A16 "New capability"
mklabel spec-decision 5319E7 "Product intent needs an owner decision"
mklabel chore         6E7781 "Maintenance / infra"
# readiness (Bar 1) — an issue is safe for an agent to grab only once it's `ready`
mklabel ready         0E8A16 "Cleared Bar 1 (ready to build): one concern, no undecided question, verify step, deps linked"
# verification (Bar 2)
mklabel needs-drive   FBCA04 "Built/tested but NOT driven from a clean slate; not Done until cleared"
mklabel blocked       000000 "Waiting on a decision or another issue"
# priority
mklabel "prio/now"    B60205 "Working it now"
mklabel "prio/next"   FBCA04 "Up next"
mklabel "prio/later"  C5DEF5 "Backlog"

echo "== area labels =="
echo "  Define these per repo — one per surface of THIS codebase, e.g.:"
echo "    gh label create area/<name> --color 1D76DB -R $REPO"
echo "  (skipped: they're repo-specific by design)"

echo "== Project 'Delivery' =="
PROJ_JSON="$(gh project create --owner "$OWNER" --title "Delivery" --format json)"
PROJ_NUM="$(printf '%s' "$PROJ_JSON" | python3 -c 'import sys,json;print(json.load(sys.stdin)["number"])')"
echo "  created project #$PROJ_NUM  ($(printf '%s' "$PROJ_JSON" | python3 -c 'import sys,json;print(json.load(sys.stdin)["url"])'))"

echo "== Status field: extend to the verify-gate columns =="
echo "  The built-in Status starts Todo/In Progress/Done. Add 'In review' and 'Verified' and"
echo "  order them Todo → In Progress → In review → Verified → Done."
echo "  (Adding options + reordering is cleanest in the Project UI: Settings → Status field.)"

echo "== Lane field (coarse grouping — one value per item) =="
echo "  Set the options to your product's domains, e.g. Editor / Product / Seam / System:"
echo "  gh project field-create $PROJ_NUM --owner $OWNER --name Lane --data-type SINGLE_SELECT \\"
echo "    --single-select-options \"<Domain1>,<Domain2>,<Domain3>,System\""

echo "== Automations (Project → ⋯ → Workflows) =="
cat <<'EOF'
  Enable / set these built-in workflows (they act on the Status field):
    - Auto-add to project:    filter  is:issue is:open   (ISSUES ONLY — not PRs)
    - Item added to project → Status: Todo
    - Pull request merged   → Status: In review     (NOT Done — that's the verify gate)
    - Item closed           → Status: Done
  These toggles are configured in the Project's Workflows UI (not scriptable reliably via gh).
EOF

echo
echo "Done. Next: define your area/* labels + Lane options, then build the hierarchy"
echo "(root → cores → area epics) with the calls in references/hierarchy.md."
