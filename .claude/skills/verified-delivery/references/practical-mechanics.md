# Practical mechanics — gh-image, GitHub Projects, the ordered queue

The nitty-gritty an AI OM actually needs to *operate* GitHub as the system of record.
The main skill says *what* to do; this says *how*, with copy-paste commands. Learned
by doing (Composa, 2026-07). `R=owner/repo`, `PROJ=<number>`, `OWNER=<org-or-user>`.

## gh-image — posting screenshot evidence to issues/PRs

Plain `gh` **cannot upload images**. A text-only "screenshot attached" note is not
visual proof and must never read as one (the skill's evidence rule). Use the
**`gh-image` extension** so evidence lives on the issue/PR:

```bash
gh extension install <gh-image-ext>      # once
gh image upload ./shot.png               # → returns a URL / markdown
# then embed the returned markdown in the issue/PR body or a comment:
gh issue comment <N> --repo $R --body "Drive-verify: ![state](<uploaded-url>)"
```

If `gh-image` isn't available, capture via the Browser pane and attach through the
UI — but never claim visual proof you didn't upload. **Capturing ≠ examining:** read
every screenshot against the intent and say what a user sees; a shot that contradicts
"done" blocks the merge.

## Projects: fields are better than labels for anything single-valued

Labels are flat strings you can filter by. **Single-select / number FIELDS** can be
**grouped, sorted, and are single-valued** — so priority, lane, and rank belong in
**fields**, not labels. Keeping *both* a `prio/*` label and a Priority field is the
dual-source-of-truth trap again — pick the field.

### Create fields
```bash
gh project field-create $PROJ --owner $OWNER --name "Lane" \
  --data-type SINGLE_SELECT --single-select-options "Editor,Product,Seam,System"
gh project field-create $PROJ --owner $OWNER --name "Priority" \
  --data-type SINGLE_SELECT --single-select-options "Now,Next,Later"
gh project field-create $PROJ --owner $OWNER --name "Queue Rank" --data-type NUMBER
```

### Get the ids you need (field id + option ids + per-item ids)
```bash
# field ids + option ids
gh project field-list $PROJ --owner $OWNER --format json \
  | python3 -c 'import sys,json;[print(f["name"],f["id"],[(o["name"],o["id"]) for o in f.get("options",[])]) for f in json.load(sys.stdin)["fields"]]'
# project node id (for item-edit --project-id) — from `gh project list --owner $OWNER`
# per-item ids (content.number → item id), plus current field values:
gh project item-list $PROJ --owner $OWNER --format json --limit 400 \
  | python3 -c 'import sys,json;[print((i.get("content") or {}).get("number"), i["id"], i.get("status"), i.get("lane")) for i in json.load(sys.stdin)["items"] if (i.get("content") or {}).get("type")=="Issue"]'
```

### Set a field on an item
```bash
gh project item-edit --project-id <PROJECT_NODE_ID> --id <ITEM_ID> \
  --field-id <FIELD_ID> --single-select-option-id <OPTION_ID>   # single-select
gh project item-edit --project-id <PROJECT_NODE_ID> --id <ITEM_ID> \
  --field-id <QUEUE_RANK_FIELD_ID> --number 10                  # number
```

**Bulk operations are slow** (one API call per item — ~250 items blows a 2-min
foreground budget). Run them as a **background script**, make them **idempotent**
(skip items already set so a re-run resumes), and use **gaps** in Queue Rank
(10,20,30…) so you can insert without re-indexing everyone below.

### The ordered queue (making "what's next" real)
GitHub has **no native 1-N rank**; Priority is a bucket. Two ways to get a real
sequence — use both:
- **Queue Rank number field** → machine-readable order. Autonomy picks the
  **lowest-rank UNBLOCKED item**. Sort the board by Queue Rank asc for the human view.
- **Manual drag** in a Table/Board **grouped by Priority with NO sort applied**
  (sorting breaks manual drag order). Top of a group = highest.
The view's *group-by / sort* is a UI toggle (not reliably scriptable) — you set the
field *values*; the human flips "Group by → Lane/Priority" once.

## Honest-board audits (the OM's self-check)

The board's shape *is* a diagnostic. Two cheap queries surface most rot:

**Status desync** — closed issues stuck in Todo/In-Progress inflate the backlog:
```bash
gh issue list --repo $R --state closed --limit 500 --json number -q '.[].number' | sort -n > /tmp/closed.txt
# then in item-list JSON: flag items whose number ∈ closed but status != Done (→ set Done),
# and open items whose status == Done (→ back to Todo).
```
**No-lane / orphans** — items with no `area/*`/`core/*` label get no lane; derive the
lane from the label and backfill:
```bash
# map: area/(engine|canvas*|timeline|creation-inspector|resolver|export|media|shell)|core/editor → Editor;
#      area/(home|landing|routing|auth)|core/product → Product; area/seam|core/seam → Seam; core/system → System.
```
Items with *neither* label are the real gap → require `area/*` at creation (a Bar-1
readiness check), then backfill.

**Delivered-but-open** is the costliest: an issue whose work shipped but was never
closed (early PRs said "Codex #N" not "Closes #N"). Cross-ref merged PRs; going
forward, PR bodies must say **`Closes #N`** so the loop auto-closes. Never bulk-close
umbrella/multi-item issues without verifying each sub-item.

## Native sub-issues (recap; full calls in `hierarchy.md`)
```bash
cid=$(gh api "repos/$R/issues/<CHILD>" --jq .id)
gh api --method POST "repos/$R/issues/<PARENT>/sub_issues" -F sub_issue_id="$cid"
```
Parent shows a live progress bar; closing a child ticks it up. One parent per child.

## Merge queue (GitHub native) — when, not now
A merge queue auto-orders PR merges and runs CI on the **combined** result before
merging (catches "green alone, broken together"). It **requires branch protection +
required status checks** — so if CI is disabled/unbilled, enabling it **blocks every
merge**. Right tool once CI is healthy and PR volume is high (many agents landing
concurrently); until then, serial admin-merge gated by drive-verify is correct.
Distinct from the *product* queue (Queue Rank) above.

## Worktree & preview hygiene — defeating the "phantom regression"

The costliest tar-pit variant in practice: **the human drives a preview and sees
"regressions" of things already fixed** — because the dev server is serving a
*stale checkout*, not current trunk. The code is fine; the window is old. Two root
causes, both preventable:

1. **A preview checkout stuck behind trunk.** (Real case: the `composa`/`composa-preview`
   launch configs served a sibling checkout `app/` that was **313 commits behind main**
   on an abandoned feature branch. Everything shipped for weeks was invisible there.)
2. **A preview started from a lingering feature worktree** that never advanced.

### The rules
- **One canonical preview checkout, pinned to trunk.** Previews serve from a checkout
  kept on `main` (or the intended branch) with **current deps**. Working checkouts
  (where you switch branches) are separate.
- **Verify the serving HEAD before trusting any UI** — yours or the user's. A phantom
  regression is a stale-HEAD symptom until proven otherwise:
  ```bash
  git -C <preview-checkout> rev-parse --short HEAD
  git -C <preview-checkout> rev-list --count HEAD..origin/main   # >0 ⇒ STALE, fix before believing the UI
  ```
- **After a big sync, refresh deps too** (`npm install`) — source-on-main with a stale
  `node_modules`/DS pin still renders old design-system code.
- If the preview checkout can't hold the branch (another worktree already has `main`
  checked out — worktrees of one repo can't both check out a branch), **detach it at
  the tip**: `git -C <checkout> fetch && git -C <checkout> checkout --detach origin/main`.
  Re-run after each ship to keep it current.

### Worktree lifecycle (delete on merge; audit periodically)
- **Delete a worktree the moment its PR merges.** Sprawl is disk *and* a footgun (any
  of them can be the stale source a preview starts from).
- **Removing a *clean* worktree is safe and reversible** — the branch ref and its
  commits survive; only the working directory goes (re-add with `git worktree add`).
  So bulk-pruning clean worktrees needs no merge-detection.
- **Audit + prune** (run as a **bash** script file — piped `while`/`for` in the shell
  can hit a broken-PATH subshell, and zsh doesn't word-split unquoted vars):
  ```bash
  # SAFE to remove = clean working tree. `git worktree remove` (no --force) refuses if
  # dirty, so it self-guards. Branch is preserved either way.
  git worktree list --porcelain | grep '^worktree ' | sed 's/^worktree //' | while IFS= read -r p; do
    [ -n "$(git -C "$p" status --porcelain 2>/dev/null)" ] || git worktree remove "$p" 2>/dev/null && echo "removed $p"
  done
  git worktree prune
  ```
  (Real case: **45 → 5** in one pass; 40 clean ones removed, branches all recoverable.)

## Don't triage "DS vs app" by grep alone

When deciding whether a UI bug is fixable in the app or belongs in the design-system
package, **grep lies**: it matches the app *passing props/values* to a DS component,
which reads as "the fix lives in the app." **Confirm where the control is actually
*rendered*** (open the DS component source) before scoping. Tells that it's a DS fix:
- the label/button/hover/outline is emitted by the DS component, and the app only
  passes data + handlers;
- **a control has no callback prop at all** in the DS (e.g. an inert button hardcoded
  in the component) — then it's a **coordinated fix**: DS adds the prop, app wires it,
  two PRs + a repin, not an app-only change.
(Real case: four "app-side" timeline/inspector bugs were all DS renders; two collapse
buttons had *no onClick prop* in the DS — unfixable from the app by construction.)

## CI-dark caveat: run the FULL unit suite locally before merge
When CI is disabled/unbilled, a red unit test can ride in on a merge unnoticed
(real case: a source-grep guard went red on a legitimate `measureText` added for text
wrapping, and merged because nothing ran it). With CI dark, run the **whole** unit
suite locally before admin-merge — not just the touched tests — since local gates are
the only arbiter.
