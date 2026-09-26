# Hierarchy: native sub-issues + the Lane field

GitHub's native **sub-issue** relationship is what makes the tree self-maintain — the parent
shows a progress bar and its children, and closing a child ticks the parent up. Markdown
checklists do none of that. Build the tree with these calls.

## Add a sub-issue (parent ← child)

The API wants the child's **numeric database `id`** (NOT its issue number). Fetch it, then POST.

```bash
R=owner/repo
child_id=$(gh api "repos/$R/issues/<CHILD_NUMBER>" --jq .id)
gh api --method POST "repos/$R/issues/<PARENT_NUMBER>/sub_issues" -F sub_issue_id="$child_id"
```

Batch helper:

```bash
R=owner/repo
link(){ # parent child
  local cid; cid=$(gh api "repos/$R/issues/$2" --jq .id)
  gh api --method POST "repos/$R/issues/$1/sub_issues" -F sub_issue_id="$cid" >/dev/null \
    && echo "  #$1 ← #$2"
}
# root ← cores
for c in <CORE_A> <CORE_B> <CORE_C>; do link <ROOT> $c; done
# a core ← its area epics
for c in <EPIC_1> <EPIC_2>; do link <CORE_A> $c; done
# an area epic ← its issues
for c in <ISSUE_1> <ISSUE_2>; do link <EPIC_1> $c; done
```

An issue has **one parent**. To move a child, remove it from the old parent first. The remove
endpoint can be finicky across API versions — if `DELETE .../issues/<PARENT>/sub_issue` 404s,
just re-parent in the Project/issue UI (drag the sub-issue); it's cosmetic, not worth fighting.

## Inspect / verify

```bash
gh api "repos/$R/issues/<PARENT>/sub_issues" --jq 'length'          # child count
gh api "repos/$R/issues/<PARENT>/sub_issues" --jq '.[].number'      # the children
```

## Set the Lane field on a Project item

Grouping the board/Table by Lane needs the field set per item. You need three ids: the
project id, the Lane field id, and the option id.

```bash
OWNER=<org-or-user>; PROJ=<number>
# field id + option ids
gh project field-list "$PROJ" --owner "$OWNER" --format json \
  | python3 -c 'import sys,json;[print(f["id"],[ (o["name"],o["id"]) for o in f.get("options",[])]) for f in json.load(sys.stdin)["fields"] if f["name"]=="Lane"]'
# item ids (per issue)
gh project item-list "$PROJ" --owner "$OWNER" --format json \
  | python3 -c 'import sys,json;[print(i["content"].get("number"),i["id"]) for i in json.load(sys.stdin)["items"]]'

# set Lane on one item
gh project item-edit --project-id <PROJECT_NODE_ID> --id <ITEM_ID> \
  --field-id <LANE_FIELD_ID> --single-select-option-id <OPTION_ID>
```

## Structure conventions

- **root** issue = "start here": the vision + links to the core trackers. One per product.
- **core / domain trackers** = the top groupings (map to Lane values). Sub-issues of root.
- **area epics** = one per surface of the product. Sub-issues of a core.
- **issues** (nit/bug/gap/feature/spec-decision) = sub-issues of an area epic.
- Cross-area work (spans two epics) can sit at the core level with links into both.
- Epic bodies are **narrative snapshots**, not trackers — head each with a line like:
  `> Live gaps = the sub-issues below (they roll up here). The text is a dated snapshot.`
