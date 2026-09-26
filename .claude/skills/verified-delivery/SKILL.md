---
name: verified-delivery
description: >-
  A complete, repo-agnostic method for turning feedback into tracked, verified, shipped
  software using GitHub as the single system of record. It stands up a self-maintaining issue
  hierarchy (root → cores → area epics → issues as native GitHub sub-issues, plus a Lane field
  and a small label taxonomy), and runs a capture → triage → delegate → verify loop with a
  two-gate verification model: an AI agent drives the real running product and captures
  screenshot evidence, while the human stays a feedback + spot-check gate rather than driving
  every change. Use this whenever the user wants to set up work tracking or a "system of
  record", break a codebase out of a plateau, stop losing feedback between AI sessions, run
  GitHub Issues/Projects/PRs as a real tracker (not just a dumping ground), have agents build
  AND verify features instead of merging on green tests, scale AI-assisted development across a
  codebase or across repos, wire epics/sub-issues/lanes/automations, keep git worktrees
  disposable and swept so work never disappears between "built" and "in front of the user",
  reconcile issues that went stale as product direction changed, drive-test a change the way a
  real user would, elicit a lifecycle spec (entry/exit/every-state) and ask before building rather
  than guess, run a UX-completeness / dead-end-path check while driving, hold UI to design-system
  conformance while flagging taste calls to the human, verify a feature end-to-end instead of
  trusting that closed issues mean it shipped (PR-merged is not feature-done), fan independent work
  out across agents/worktrees or a dynamic workflow to deliver faster, or says things like "use our
  workflow", "set up the board", "capture this as an issue", "why didn't the change I asked for show up", or
  "make sure this doesn't get lost". Applies to any repository with a GitHub remote.
---

# Verified delivery

This is **one skill**: the operating discipline for an AI engineering-PM that orients a human
through coding sessions — capturing their intent, turning it into tracked work, driving it to
done, and keeping the record honest as the product moves. The tracking, the verification, the
branch hygiene, and the issue upkeep are **one system, not separate skills** — don't split them
apart. A way of working that survives session crashes, agent hand-offs, and green-tests-that-lie.

The roles are explicit. The human is the **product design director** — sets direction, judges the
output, decides what matters and whether it feels right. The AI is the **engineer, the PM, and
(inside a design system) the designer** that gets it working: it scopes, builds, tests, and
**surfaces every gap it finds** as a triage list — never silently fixing, never silently dropping.
The director decides priority and taste; the AI does everything mechanical that leads up to that
decision. Nothing that only the director can judge gets guessed; nothing the AI can do gets
handed back up.

It exists to defeat four failure modes that quietly kill AI-assisted projects:

1. **Feedback rot.** A person gives sharp, hard-won feedback in a chat; the session ends; it's
   gone. Nobody executes it. The same note gets re-typed weeks later, or never.
2. **Seeded-green lies.** An agent grinds overnight, passes its own tests, and declares victory
   on work that doesn't function when a human actually uses it. Progress looks busy but stalls.
3. **The tar pit.** An agent reports "it's built," but the human tests and doesn't see it —
   because the work sits on a branch or a worktree that never reached the trunk they're looking
   at, or a stale worktree is serving old code. Effort vanishes into the gap between "committed"
   and "in front of the user." This is the one that quietly produces the most frustration.
4. **Stale-decision drift.** Product direction changes across sessions; issues filed under an old
   plan silently stop matching where the product went, and agents pull work that shouldn't be
   built.

The cure runs through everything below: **the durable record lives in GitHub, not chat; "done"
means an agent drove the real product like a user and left evidence; work lands on the trunk the
human actually tests; and the record is reconciled to the product's *current* direction, not the
moment each note was captured.**

## The one rule that makes it work

**If it isn't a GitHub Issue, it isn't tracked.** The instant someone gives feedback, notices a
bug, or names a gap, it becomes an issue — before anything else, mid-sentence if needed. Chat
and an agent's private memory both evaporate; an issue persists across sessions, tools (Claude,
Codex, the human), and crashes. This is the single habit that ends feedback rot. When in doubt,
file it; a redundant issue costs seconds, a lost one costs a cycle.

## The structure (set up once, then it maintains itself)

Run `scripts/bootstrap.sh <owner/repo>` to stand up the pieces on any repo. It creates:

- **A label taxonomy** — three orthogonal axes so any issue has coordinates:
  - **type**: `nit` · `bug` · `gap` · `feature` · `spec-decision` · `chore`
  - **readiness (Bar 1)**: `ready` (cleared to build). Its *absence* means the issue still needs
    the split / extract-decision / scope transform — agents grab `label:ready` only.
  - **verification (Bar 2)**: `needs-drive` (built/tested but not driven from a clean slate; not
    Done until cleared) · `blocked`
  - **priority**: `prio/now` · `prio/next` · `prio/later`
  - Plus `area/*` labels you define per repo (the surfaces of *this* codebase).
- **A GitHub Project** with a **Lane** single-select field (the coarse grouping — e.g. the
  domains of the product) and **Status** columns carrying the verify gate:
  `Todo → In progress → In review → Verified → Done`.
- **Automations**: new issues auto-add to Todo; a merged PR moves the card to **In review**
  (not Done); a closed issue moves to Done. Board is **issues-only** — a PR is the
  *implementation* of an issue and rides its status; putting both on the board double-counts.

### Hierarchy: native sub-issues, not markdown checklists

Use GitHub's **native sub-issue** relationship to build the tree (see
`references/hierarchy.md` for the exact `gh api` calls):

```
root (start-here)
├── core / domain trackers
│   └── area epics
│       └── issues (nits, bugs, gaps) as sub-issues
```

The parent shows a live progress bar and its children; closing a child ticks the parent up
automatically. This is the difference between a real hierarchy and a pile of labels.

**Critical convention — issues track, epic bodies narrate.** A markdown `- [ ]` checklist in an
epic's *body* is static prose; it does NOT cross off when work lands, so it rots and confuses
("why isn't this checked?"). So: the **native sub-issues are the only live gap list**; the epic
body is a dated narrative snapshot (context: what the area is, its architecture, known
constraints). When testing finds a new gap, you **file a new sub-issue** — that's the tracker. The
body itself you don't have to freeze: as an epic evolves, either **edit it in place and re-date
it**, or **append a comment** with the update (pick one workflow and stick to it). Either way the
body carries a one-line header — *"snapshot as of &lt;date&gt;; later comments may supersede this"* —
so no one mistakes it for live state or trusts a line the comments have since overtaken. Promote a
narrative line into a real sub-issue only **when it's about to be worked** — pre-creating hundreds
of atom-issues just rebuilds the tar pit in a new place.

### Lane vs labels

The **Lane field** (a Project single-select) is the clean grouping dimension — group the board
or a Table view by it and each item sits in exactly one lane. `area/*` **labels** ride on the
issue itself (visible in the repo, set at creation, portable). Field for grouping, labels for
filtering; don't force group-by onto multi-valued labels.

## The two gates (this is the heart)

The instinct to make a human personally verify *every* change is correct in spirit and fatal in
practice — it doesn't scale, so verification gets skipped and the lies return. Split it: the AI
holds the shipping gate, the human holds an async sampling gate. Crucially, **the human is not a
per-PR merge button.** Once the AI's gate is green — an independent review *and* a clean-slate
drive with evidence — the agent ships; it does not park the PR waiting for sign-off. The human
gate runs *alongside*, not *in front of*, the merge.

### Gate 1 — the AI shipping gate (review + drive)

Two checks, both by a pass *separate from the builder*:
1. **Independent review** of the code — not the author rubber-stamping itself. Make it
   *adversarial* (a reviewer whose job is to refute, who deletes the fix and proves the test goes
   red), and verify the review itself against *current* HEAD before acting — a review is a claim
   that goes stale. See the Field lessons.
2. **A clean-slate drive** of the real, running product — the way a person would — capturing
   **screenshot / DOM evidence** of the actual behavior.

`needs-drive` is the label for the gap between these: *built, maybe green on tests, but not yet
driven from a clean slate.* An item wears it from the moment a PR opens until the drive clears it
with evidence. Driving-and-photographing reality is not the seeded-green trap (that was an agent
passing its *own tests*) — it's QA, not test-writing. **When both checks are green, the agent
clears `needs-drive` and merges** — that is the whole point of splitting the gates.

Rules that keep it honest:
- **Assert the USER's outcome, not the builder's proxy.** The done-condition is the human's sentence
  ("I leave the editor and land on my *usable* files"; "the control reads as *enclosed* like the
  buttons"), NOT a mechanism you happened to wire ("URL === /projects"; "the ring class is in the
  DOM"). A green trusted-event assertion on a proxy is still *testing what you built*. If the user's
  condition can't be met (e.g. the destination is an auth wall), that's a **finding**, not a pass.
- **Evidence or it didn't happen — and you must LOOK at it.** An atom leaves `needs-drive` only with an
  attached screenshot of the real behavior, **posted to the issue/PR** (use the `gh-image` extension —
  plain `gh` can't upload images; a text-only note must never read as visual proof). Capturing a
  screenshot is not examining it: read every screenshot *against the intent* and say what a user sees.
  A screenshot that contradicts "done" blocks the merge. (Real misses: a post-navigation screenshot
  showing "Sign-in unavailable" was captured but not looked at; an "invisible-because-covered" stroke
  was called done off a class assertion.)
- **A reviewer-flagged felt-outcome defect is a completeness bug, not a deferrable "taste" nit,** when
  it defeats the feature's visible purpose (a stroke you can't see is not shipped).
- **Be adversarial.** Try to break it; author from an empty state, not a seeded fixture. Report
  what you *couldn't* make work as loudly as what you could. (Finding "the button is inert" is a
  success of the gate, not a failure.)
- **Think like a user, not a builder.** Exercise the *discoverable* path — the controls a person
  would actually click, from the state they'd actually start in — not the code path you just
  wrote. Agents test what they built; users test what they wanted, and the two diverge exactly
  where the bugs live. (Field notes on driving-like-a-user accumulate in
  `references/verification.md`; the owner adds to them.)
- **Run the UX-completeness checklist while you drive (the UX gate).** You're already walking the
  paths to check correctness — walk them for *completeness* in the same pass. For each user-facing
  flow, confirm it has: an **entry point**, an **exit / end / undo**, an **empty** state, a
  **loading** state, an **error** state, and a **post-action** state. Every "no" is a dead-end
  path — file it. This mechanizes user-advocacy: you catch UX dead-ends exactly *because* you're
  walking the paths to test.
- **Check the UI against how it's already built (the UI gate) — not against taste you don't have.**
  Conform to the existing design system / component patterns: **reuse them, don't invent.** Where
  there's no formal design system, reference an owner-maintained note describing *how the current
  design and components are built* and reuse from it. The AI checks **conformance** (does this use
  the existing tokens/patterns? is the copy clear?); a "does this feel right" judgment is a **taste
  call flagged to the human** — never pretend to a taste you don't have.
- **Guard against regressions.** Screenshot the touched surface **before and after** the change.
  An unexpected visual change — here or somewhere the change shouldn't have reached — is a
  finding, not collateral. The two common causes are an agent's change bleeding past its scope,
  and a **stale worktree serving old code** — so agents branch off *current trunk*, never a
  lingering worktree, and stale worktrees get pruned.
- **A passing test is necessary, never sufficient.** Especially when CI is unavailable or its
  billing lapsed — then drive-verification is the *only* real gate.
- See `references/verification.md` for the driving technique (including firing real pointer
  events, which many component libraries need and plain synthetic clicks miss).

### Gate 2 — the human async sampling + feedback gate

This runs *after* and *alongside* shipping, never in front of it. The human drives when they
*want* to, **spot-checks a sample** of the agents' evidence, and makes the calls only they can
(product decisions, design, anything gated on an owner). Its highest-value form is a **voice /
drive session where they test several shipped things *together*** — surfacing the interaction
bugs and direction mismatches that no per-PR gate can see, because a per-PR gate only ever looks
at one PR in isolation. Whatever they catch becomes **new issues**, not a merge they were
blocking. The guardrail against an over-optimistic agent read isn't human labor gating every
change — it's **auditable evidence + sampling + the independent review** from Gate 1.

The AI hands over a **triage list** — correctness bugs, UX-completeness dead-ends, and UI
conformance/taste flags it *surfaced but did not decide*. The director rules on **priority and
taste**. That division — the AI surfaces, the director decides — is what makes the offload safe:
the AI is relentless about finding gaps and disciplined about never silently fixing or dropping
them.

## Two readiness bars (don't conflate them)

There are two distinct gates, and quality leaks where they're blurred. One is about the *issue*
going in; the other is about the *output* coming out. An issue can be perfectly built and still
have been un-drivable to begin with — and vice versa.

### Bar 1 — ready to build (is the issue a drivable unit?)

Capture is instant and rough; *execution* needs a **scoped, decided, verifiable** unit or the
agent guesses and the error margin balloons. Readiness here is not pass/fail — it's a
**transform**: you split, extract, and scope a raw issue until it clears the bar. An issue is
ready to hand an agent only when:

- **A lifecycle spec — and the duty to ask (the spec gate).** *Highest-leverage of all, and
  upstream of the rest.* Before building, enumerate the flow's every **entry point**, **exit /
  end / undo**, and **state** (empty, loading, error, post-action). Where the spec is silent, the
  agent **asks — it does not guess.** Almost every "user-advocacy" gap is a spec gap caught late:
  *(real case: a browser session's End button + ended-state card were retrofitted after the fact
  because the original spec never answered "how does a session end, and what does the user see
  after?")*. This reframes "communication" from prompt-polish into an **obligation to ask** before
  the first line of code. *For UI specifically:* when the visual isn't pinned by a Figma/screenshot
  and the detail isn't obvious, **ask — don't invent a treatment.** "Reuse the existing component" is
  not license to add un-specified decoration. *(Real case: told to reuse the menu component with no
  visual; an agent added a leading arrow icon the owner never asked for — a guess that should have
  been a question. Also: the state-spec is often already owned by the design-system component, so the
  gate for design-system UI is the **feature spec** — what each interaction does — not a re-enumeration
  of empty/loading/error states the component already handles.)*
- **One concern, one PR.** If the title needs multiple "and"s (a contract *and* a migration *and*
  UI), it isn't one issue — split it (e.g. 2a/2b/2c). One agent, one PR, one blast radius.
  *(Real case: an issue bundling contract + backend retirement + data migration + UI had to be
  split three ways before any of it could be handed to a single agent.)*
- **No embedded undecided product question.** A build issue with a hidden decision inside stalls
  the builder mid-PR. **Extract the decision into its own `spec-decision` issue** and make the
  build issue `blocked-by` it — so code never waits on an owner call it can't make. *(Real case:
  a design question buried in a build issue became its own owner-decision issue instead of
  blocking the code.)*
- **A stated verify step** — the clean-slate action + expected result + that a screenshot proves
  it. This is the atom's definition of *done*, written before it starts.
- **Explicit dependencies** — "depends on #X" as a `blocked-by` link, so nothing starts on sand.
- **The execution basics** — behavior in product terms, area + likely files, a **do-not-touch
  scope** (the regression guard, written in), and a **design reference** where fidelity matters.

A rich capture session (an owner driving the app, dumping everything) is how issues *earn* this
bar: the dump is raw; each item is then split / decided / scoped and, once it clears, tagged
**`ready`** — the queue agents pull from (`label:ready`). An un-transformed issue is visibly
not-yet-drivable. This transform is the highest-leverage work — creating issues is the easy 80%;
making them *drivable* is the value.

### Bar 2 — ready to ship (is the output verified?)

This is the **two gates** above: an agent **drove the real product and produced screenshot
evidence** (Gate 1), and the human **sampled** it (Gate 2). A green test is necessary, never
sufficient. An output that only passed tests — or bled past its do-not-touch scope — is
`needs-drive`, not done.

## Done means the feature, not the PR

A merged PR and a closed issue are *steps*, not the finish line — and conflating them is how gaps
ship. It happens two ways: the spec was **shallow**, so "build it" was underspecified and the
build faithfully implemented an incomplete picture; or the AI treated **its bar met** (tests pass,
issue closed) as **feature delivered**. Either way the director trusts the closed issues,
overlooks the gap, and rediscovers it as a bug two sessions later — the most expensive place to
find it. So the **feature**, not the PR, is the unit of "done":

- **The epic carries the feature's lifecycle spec** (from the spec gate): every entry point, exit,
  and state. That enumerated spec *is* the definition of done, written before the work — so
  "delivered" has a fixed meaning no one can quietly shrink.
- **Closing sub-issues never auto-completes the feature.** A checkbox rollup means the pieces
  landed, not that the whole works. Before calling a feature delivered, an agent **re-drives it
  end-to-end against the spec** and reports what's still a dead-end — **proactively, the moment the
  last piece merges, not when asked.** `PR merged` ≠ `feature done`; say which one you mean.
- **A shallow spec is a finding, not a license to guess.** When the enumeration is missing depth,
  the AI **asks the director**, or files the hole as a `spec-decision` — it does not ship an
  assumption that reads as done. Most late-discovered "bugs" are just spec gaps that were never
  surfaced up front.

This is where tracking and verification marry: the **hierarchy** tracks the *pieces*, the **spec +
end-to-end drive** verify the *feature*, and "done" needs both. An epic whose children are all
closed but which was never driven end-to-end is `needs-drive` at the feature level.

## The operating loop

1. **Capture** — feedback → a labeled issue, instantly, linked into the hierarchy.
2. **Triage** — set type/area labels + Lane so it lands in the right group. New auto-added
   issues arrive unlabeled; labeling on arrival is the triage step.
3. **Delegate** — hand well-scoped, *decided* issues to sub-agents. Each works in a disposable
   worktree, builds, adds tests, **pushes on the first commit**, opens a PR that says `Closes #N`
   (or `Part of #N` for a slice), and leaves it `needs-drive`. Sequence agents that touch the
   same hot files (a monolithic component, a shared adapter) — concurrent edits there manufacture
   merge collisions.
4. **Review + drive-verify, then ship (AI gate)** — an *independent* pass reviews the code; an
   agent drives the PR's flow from a clean slate and screenshots it. Both green → clear
   `needs-drive` with evidence and **merge** (the automation walks the card toward Done), then
   retire the worktree. Not green → name the wall; it stays `needs-drive`, unmerged.
5. **Human sampling (async)** — the owner spot-checks evidence when they want and, in voice/drive
   sessions, tests several shipped things *together* — the check per-PR gates can't do. What they
   find becomes new issues, not a merge they were blocking.

Feedback compounds: one shipped feature typically surfaces two or three *new* real gaps while
being driven — each becomes a sub-issue, so the backlog fills itself with true work.

## Deliver in parallel by default

One agent, one issue, one worktree — but **many at once**. Speed comes from parallelism, and the
AI is expected to reach for it *without being told*: a queue of independent `ready` issues run one
at a time is a self-inflicted plateau.

- **Default to parallel.** Fan every independent `ready` issue out to its own agent in its own
  disposable worktree, running concurrently. Use a dynamic workflow / orchestration to spawn the
  fleet, collect results, and drive-verify — don't hand-serialize what could be in flight together.
- **Sequence only around real contention.** Issues that touch the same hot files (a monolithic
  component, a shared adapter) or the same lock (e.g. a single repin/version bump) must serialize;
  everything else parallelizes. Identify the seams up front so *most* work is independent by
  construction — clean seams are what make parallelism possible, so design for them.
- **Parallelize the verify, too.** Drive-verifying N merged PRs, or re-driving several features
  end-to-end, is itself fan-out-able.

The constraint is contention, not caution: if two slices won't collide, they should be moving at
the same time. When in doubt about the fastest path, scope the independent set and fan it out.

## Orchestration hygiene (make parallel safe)

Fanning work across agents multiplies throughput *and* multiplies the ways git state corrupts
silently. Parallelism is only safe on three disciplines — **isolate every agent, pin the canonical
truth, and measure the artifact instead of trusting a claim.** This is the concurrency-safety layer
under "Deliver in parallel"; the worktree lifecycle contract below handles disposal. Each rule cost
real time in a long multi-agent session.

- **One agent, one isolated checkout — never a shared working directory.** Every agent gets its own
  `git worktree add` (or a fresh clone) branched off the canonical ref; two agents never edit the
  same directory. Concurrent edits in one dir silently overwrite each other, and a *shared* scratch
  checkout is a single point that can drift out from under you. *(Cost this session: concurrent
  agents clobbering each other's edits in one dir; and a shared clone whose `origin` was actually
  pointing at a **different private repo** than assumed, so "edits to the public repo" landed
  nowhere the session expected.)*
- **Pin one canonical source-of-truth per repo, and verify it before trusting any checkout.** Don't
  assume a directory points where you think — *measure* it: `git remote get-url origin` and
  `git rev-parse HEAD`. Read every baseline from `origin/<canonical-branch>` (fetch first), never
  from a stale or detached checkout. *(Cost this session: a detached HEAD hundreds of commits behind
  trunk read as current and produced a confident, wrong diagnosis of a live bug. The checkout is a
  claim too — same evidence gate as "Verify before you build.")*
- **Measure the actual artifact, not a proxy or a subagent's word.**
  - **`grep` / `git grep` silently skip files Git flags binary** — a test fixture with raw
    control-byte data, a minified blob. A leak/secret sweep that trusts a clean `grep` is measuring
    nothing: run **`grep -a`** over `git ls-files`. *(Cost this session: a real secret survived two
    scrubs because the file it lived in was binary-flagged and every `grep` skipped it.)*
  - **Don't trust a subagent's "BUILD SUCCEEDED" / "clean" — re-check the log.** `grep` the build log
    for the literal `** BUILD SUCCEEDED **`. Watch the shell traps: `xcodebuild … | tail` returns
    *tail's* exit code, not the build's; and `nohup … &` inside a backgrounded shell orphans the
    process, so the harness "completed" event fires on the wrapper, not the build.
  - **Before concluding work is lost or a push failed, measure the real remote** — `git ls-remote`,
    or a throwaway clone — rather than reasoning from memory about what you think you pushed.
  - Agreement among N agents is one reading replicated, not corroboration — count measurements, not
    concurring agents (see the "agreement is not evidence" field lesson).
- **Push discipline: fast-forward or rebase, never clobber.** Before pushing, confirm the branch
  descends from the *current* remote tip — `git fetch`, then
  `git merge-base --is-ancestor origin/<trunk> HEAD`. When several agents produced commits for one
  branch, **serialize and rebase** onto current trunk instead of racing pushes — and **re-run the
  material verification (the `grep -a` sweep, the build check) after each rebase**, because a rebase
  can reintroduce exactly what a scrub removed.
- **Never clobber the human's uncommitted work.** A shared or long-lived checkout may hold work the
  human is editing live (a README, a config) while agents run. Before any `checkout`, `reset`,
  `stash`, or branch switch that could discard it, **locate the work first**: `git status`,
  `git diff`, `git stash show -u`. Losing the human's in-flight edits is worse than any delay —
  inspect and preserve before you touch the tree. *(Real case: agents nearly reset a tree while the
  human was mid-edit on a README.)*

## Land the work where the human looks (kill the tar pit)

The tar pit opens when "done" and "in front of the user" drift apart: an agent commits to a
branch or a worktree, the human tests the trunk, the two never meet — and the feedback loop dies
while everyone believes it shipped. This is the failure behind *"I gave feedback, was told it's
built, and then I didn't see it when I tested."* Close the gap mechanically:

- **Push on the first commit.** Origin is the source of truth. A branch that lives only on a
  local worktree is one disk-cleanup away from gone.
- **Land small slices onto the trunk fast.** A capability-gated slice merged today beats a
  perfect branch merged "soon." Long-lived divergent branches are how a whole workstream silently
  plateaus (the orphaned-island anti-pattern below).
- **Drive the trunk the human tests — not your branch** — or state *explicitly* which branch your
  evidence came from, so branch-only work is never mistaken for shipped.

### Worktree lifecycle contract

A worktree's existence is a **liability, not an asset** — create it late, push early, and delete
it the moment its PR merges.

- **One per unit of work** (issue/PR), created off the *current* base. Never reuse a worktree
  across unrelated slices.
- **Push to origin on the first commit.** A worktree can vanish at any time — user cleanup, disk
  pressure, the harness — and the work must survive. If one goes missing, re-create it from the
  branch on origin.
- **Retire on merge or abandon:** delete the remote branch, `git worktree remove` the directory,
  `git worktree prune`. A left-behind directory is a defect — dead weight on the user's disk.
- **Sweep before ending a session:** run `git worktree list`; remove any whose branch is merged
  into the trunk or gone from origin. End state = **no orphaned worktrees.**
- **Never treat a stale worktree as truth.** Don't infer built/reviewed behavior from another
  dirty worktree — that misread is the exact failure the sweep exists to prevent.

## Keep issues honest as direction changes (issue rot)

Capture is instant, but product direction *moves* across sessions — and issues filed under an old
plan quietly become wrong. A backlog full of stale issues is its own tar pit: agents pull work
that shouldn't be built anymore. The record needs **upkeep, not just appends**:

- **Reconcile, don't just accrue.** Periodically — and whenever a decision changes direction —
  sweep open issues for ones that now contradict where the product went. An AI can and should do
  this: read the current specs/decisions, scan open issues, and surface the conflicts to the
  owner.
- **When a decision supersedes an issue,** close it with a one-line reason pointing at the new
  decision (or edit its scope to match). A closed-with-reason issue is a decision record; a
  silently stale one is a trap the next agent falls into.
- **The record reflects *now*, not the moment of capture.** The same discipline that stops
  feedback rot on the way *in* stops decision drift *over time*.

## Verify before you build

Before building or re-proving anything, **investigate whether it's actually needed and how it
really behaves** — read the code, drive the running product. Distrust every summary, including
your own prior claims and the issue's own prose; state moves, docs and evidence-cells rot. This
prevents the two worst wastes: an overnight agent re-proving already-done work, and building
against a stale mental model. Classify each behavior honestly: `proven` (driven + guarded),
`test-only` (a — often seeded — test passes but the human path wasn't driven), `built-unproven`,
`partial`, `missing`, or `blocked`.

## Anti-patterns to refuse

- **Merging on green when CI didn't run.** If Actions is disabled/unbilled, "CI green" is
  meaningless. Say so; lean entirely on drive-verify until it's restored.
- **Long-lived divergent branches and orphaned worktrees.** Work that forks and never merges
  becomes an island the trunk passes by (the classic silent plateau); a worktree left behind
  after its PR merges is dead weight that later gets misread as truth. Keep branches short-lived,
  land small gated slices, push early, and sweep worktrees on merge.
- **Sharing one working directory across agents, or trusting a checkout you didn't measure.**
  Concurrent agents in one dir overwrite each other, and an unverified checkout can point at the
  wrong repo or a stale/detached HEAD. Isolate every agent, and confirm `origin` + HEAD before
  trusting any tree (see Orchestration hygiene).
- **A green report over an unmeasured artifact** — a clean `grep` that skipped binary-flagged files,
  a subagent's "BUILD SUCCEEDED" no one re-read in the log, a "push failed" concluded from memory
  instead of `git ls-remote`. Measure the artifact, not the claim.
- **Two trackers for one thing** — the epic-body checklist beside the sub-issues. Pick sub-issues.
- **Pre-creating hundreds of atom-issues.** Promote to an issue when worked, not before.
- **A backlog left to rot.** Issues that no longer match product direction, never reconciled.
  Sweep and close-with-reason as decisions move; an agent pulling superseded work is the drift
  failure in action.
- **Self-certification — not merging.** The trap was never an agent *merging*; under this method
  it should, once its gate is green. The trap is an agent certifying its own build with its own
  tests and calling that proof. Legitimate shipping needs an *independent* review pass and a
  clean-slate *drive* of the real product with evidence; a builder green-lighting its own seeded
  tests, or skipping the drive, is how the lies creep back. Keep reviewer/driver a different pass
  than the builder.

## Field lessons (this skill self-learns)

The method improves by feeding real misses back in. Abstracted, repo-agnostic lessons land **here**;
anything codebase-specific goes to that repo's memory/README/issues instead (don't bloat the skill
with one project's facts). Add a lesson the moment a session teaches it — that IS the discipline.

- **"Merged + compiling + SDK-verified" is not "working."** An integration/auth/tool feature is
  done only when you drive the *actual user path* in the running product and watch it succeed —
  connect → consent → **use it** end-to-end. A green build and a code-read of the vendor SDK will
  happily hide a server-deprecated endpoint, an un-wired MCP, or a runtime crash that only appears
  live. If you declared it done without driving it, you didn't verify it. (Cost this session:
  declaring Composio "done" three times before a single connect actually worked.)
- **Trace the *whole* path; distinct wires fail independently.** "Connected ✅" in a settings
  screen does not mean the agent can use the thing — authorization and tool-access (e.g. MCP into
  the runtime) are separate wires. Enumerate every hop from the user's intent to the effect and
  verify each; a feature that's 90% wired is 0% usable.
- **Before any deploy or side-effectful op: confirm the target, baseline it, and know what it
  triggers.** Which service/env is actually linked? Health-check it *before* you touch it so you
  can tell if you broke it. What hooks does the deploy run (migrations, repoints)? Guessing the
  target is how you 502 a service you didn't mean to touch and fire a side-effect you didn't expect.
- **External SDKs drift from the vendor's live API, and runtimes lack globals you assume.** Verify
  against the *installed SDK source* (not just docs), test against the *live API*, and expect to
  polyfill (e.g. a Web Crypto global missing on the deploy runtime). "Latest published SDK" can
  still call a deprecated server endpoint.
- **Drive it the way the user does, not the way that's convenient.** The bug the user hits is in
  the path you skipped. When a user says "I tried X, it's broken," reproduce X live before
  theorizing — the running product is the source of truth, code-reading is the hypothesis.
- **Diagnose from evidence, not successive guesses — a root-cause claim needs the same evidence
  gate as "done."** When something fails, read the *direct* signal — the log line, the entitlements,
  the actual machine/config state — *before* asserting why. Each confident cause stated as fact and
  then proven wrong burns the user's time and trust as fast as a seeded-green lie; "I think it's X"
  said three times is worse than "let me check" said once. If you can't see the evidence yet, say
  that — don't fill the gap with a plausible story. (Cost this session: three wrong sign-in
  diagnoses in a row — "the reinstall preserved your session," "the sim isn't signed into iCloud,"
  "the keychain is corrupted" — each asserted before looking; the real root, an unsigned build with
  no keychain-group entitlement, took one `codesign -d --entitlements` to see. The tell I ignored:
  I was proposing *fixes* for a cause I hadn't confirmed.)
- **An independent review is a second opinion; an *adversarial* one is evidence.** A reviewer who
  re-reads the diff and agrees has added a second reading, not a test. The review that catches a
  green-but-dead fix **deletes the fix, keeps the test, and runs it** — if it still passes, the test
  measures nothing and the "fix" is unverified. Give at least one reviewer the job of *refuting*, not
  blessing. (This session: an adversarial verifier caught a "fix" that was a runtime no-op — it
  swapped a string constant but the code path it claimed to change never ran — and a date label that
  printed the wrong weekday for anything ~a week out; both had passed independent readings.)
- **A review is itself a claim, and it goes stale.** Verify a review comment against *current* HEAD
  before acting on it. Reviewers pin to a commit and the host re-anchors their notes onto surviving
  lines, so a fixed issue reads as live; agents sometimes review the wrong SHA entirely. "The
  reviewer said X" gets the same evidence gate as "the code does X." (This session: repeated churn
  from stale bot comments, and an agent that reviewed an earlier commit than the one that merged.)
- **Prioritize; do not chase every fix.** A thorough review returns more than you should act on now.
  Severity-rank it: fix the P0/P1s that break the user's outcome, **file the P2/P3s as their own
  issues** rather than fixing them inline. Chasing every nit inflates one PR into a stall and buries
  the shipping decision — the failure mode of "fix everything the review found" is slower delivery,
  not higher quality. (A single review returned 11 items; the right move was 1 P1 + 2 P2s fixed and
  shipped, the remaining 8 chipped — not all 11 blocking the merge.)
- **In multi-agent work, agreement is not evidence.** N agents reading the same file and concurring
  is *one reading replicated* — confidence rises while the measurement count stays at zero, and it
  hardens into settled fact by repetition. Count measurements, not concurring voices; when agents
  converge without anyone having *run* something, that convergence is the signal to stop analyzing
  and go measure. Name the unmeasured hop in every handoff or it silently becomes assumed-true.
  (This session: three agents agreed a security finding was "plausible, runtime unverified" across
  several rounds; the question was settled in one minute by actually running the parser — it fired.)

## Files in this skill

- `scripts/bootstrap.sh <owner/repo>` — creates the labels, Project, Lane field, and
  automations. Idempotent; safe to re-run.
- `references/hierarchy.md` — the exact `gh api` calls for native sub-issues (add/remove/list)
  and setting the Lane field per item.
- `references/verification.md` — how an agent drives a running app to produce honest visual
  evidence (browser tooling, real pointer events, clean-slate authoring, what "evidence" means).
- `references/practical-mechanics.md` — the operating nitty-gritty: `gh-image` for evidence,
  GitHub Projects fields vs labels (Lane/Priority/Queue-Rank), setting fields per item, the
  ordered-queue (rank) pattern, honest-board audits (status desync / no-lane / delivered-but-open),
  and the native merge queue (when, not now).
