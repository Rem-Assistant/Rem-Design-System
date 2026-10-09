# Chat guidance review — 2026-10-09

Scope: reusable guidance and review gates for Samuel's Chat feedback. This is a documentation
and guidance-test change; it neither implements product behavior nor edits Figma. Review the
[current contracts](../../REGISTRY.md#chat-interaction-and-anatomy-contracts) and
[required checklist](../../.claude/skills/rem-design-system/references/chat-review.md).

## Scope and provenance

Based on `main` at `dcadc3dab5186a5f295185c8adae3dff73a08c9c`, in isolated branch
`codex/chat-guidance-review`. Inspection found no `.agents/skills` or `AGENTS.md` in this checkout;
the existing skill entrypoints are under `.claude/skills`.

Read and preserved all six uncommitted guidance diffs in the older `codex/settings-playground`
checkout. Its patch SHA-256 before and after this work is
`e3d33670d44d8fdef7a80134f9948d9587c958856440d35adc7cb49e43d17a09`.
The original checkout remains unmodified by this task, with the same six dirty paths:

- `REGISTRY.md`
- `FILE-ORG.md`
- `.claude/skills/rem-design-system/SKILL.md`
- `.claude/skills/rem-design-system/references/screen-track.md`
- `.claude/skills/design-system-delivery/SKILL.md`
- `docs/design-reconciliation/2026-10-05-topology-feedback-ledger.md`

The related registry, canvas and skill amendments were carried into this focused draft.
The connected-flow screen guidance was adapted to current main without importing unrelated
branch-only instructions. The historical topology ledger does not exist on main and remains
preserved in the old checkout; its supersession is represented by the current canvas contract,
without importing the older branch's ledger or product commits.

`skills.json` still pins `samuelalake/agent-skills` at
`8f0ae6c7a74e60353db71e75687a2a4d2c23eb1f`. Its two vendored trees,
`product-design-delivery` and `verified-delivery`, are unchanged relative to main.
`design-system-delivery` is not in that vendor manifest. No vendor bump, Factory/configuration,
CI/workflow, product-code, token, generated-manifest, or Figma changes are part of this diff.

## Authority and reference evidence

Samuel's final clarification is recorded as approved direction: the **red outline
circle-exclamation icon** is entirely outside/right of the bubble; **Not delivered text stays
below**, with no timestamp. A reaction overlaps the bubble edge; the under-bubble receipt/subtext
has a right inset or left shift to clear it. This supersedes the temporary placement hold.
The sheet-opening Button owns the credential chevron; ListRow does not. Permission requests
have a separate, explicitly chosen inline-card presentation: disclosure/header above a slotted
request body, in-place expansion, collapsed Denied receipt with inspectable history, and three
horizontal canonical ButtonGroup actions. The existing Actions slot already supports three
children; no new primitive was needed. Existing variants remain; native OS permission handoffs
stay separate. This does
not decide Always Allow scope/backend policy or authorize a redundant Rem review sheet.

Read-only pixel inspection of [Grok reference 2535:22130](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO?node-id=2535-22130)
was performed on 2026-10-09. The final capture was 2532×5364 pixels, equal to the node's reported
natural canvas size. Local evidence file: `review-evidence/grok-reference-2532x5364.png` in the
task workspace (outside this Git worktree); SHA-256
`bf928b9c3ceb918061ac0f028a954b36813fe88b626f7ddab4964170dc78fe66`.
The capture is not committed or re-published; the source node is the review link.

Visible observations: connector Authorize, Adding, Added and Retry; unanswered/selected choice;
email review controls, Sent and Unconfirmed; credential empty/filled/Saved states. These are
reference UI states, not proof of Rem implementation, storage security or backend completion.
That original screenshot does not show the long-press sheet; the completed Rem review below
now supplies the current sheet and full-screen evidence. Muse observations remain attributed
to the earlier worker readback.

### Completed Rem review reconciliation

The design worker reported the latest corrections complete. The following current Figma captures
were also inspected read-only during this documentation reconciliation:

| Evidence | Inspected result / limit |
|---|---|
| [Full Chat review `2626:20028`](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO?node-id=2626-20028) | Credential before/saved screens; inline permission expanded/denied collapsed/re-expanded states; short/wrapped messages with reaction, outline error icon and receipt together. Compact Allow once / Always allow / Deny passes at 320/330/370pt without shrinking type. Longer labels fail 320pt and are explicitly non-shipping. |
| [Grok-style long-press sheet `2603:19498`](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO?node-id=2603-19498) | Natural 402×874 capture shows a 2×6 reaction grid and grouped Reply / Mark as unread, Copy / Select Text, and Report actions. This is one inspected menu; do not infer identical actions for every role. |
| [Full Chat prototype entry `2630:20783`](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO?node-id=2630-20783) | Natural 402×874 entry capture shows the shared Chat composition and composer. A static capture does not prove destination wiring or runtime behavior. |

The full-review capture is 2680×1600 pixels around a reported 2600×1520 canvas; the two screen
captures match their 402×874 natural dimensions. Local files are retained outside the Git worktree
under `review-evidence/`; no screenshots or temporary asset URLs are published in this PR:

- `chat-review-2626-20028.png` — SHA-256 `c757ba5e78a00a80320f5cac7a2160f8fd212b86c2d8ff95cef3659fccb3066b`
- `chat-long-press-2603-19498.png` — SHA-256 `fbbea91fc1799d6e0591917136e235d208816eb90d0bfa4c3b10016dfa62c897`
- `chat-prototype-2630-20783.png` — SHA-256 `b35bff3467bedf7a27e31bc4093e174815442f2268c103028bc43fd6020a2f51`

The existing ButtonGroup slot's three-child support is verified evidence from the design worker;
this documentation task did not independently re-audit live ancestry, slots, overrides or click
prototype destinations. The reusable checklist retains those structural/interaction gates,
including one shared permission body slot, role-specific menus and destination/return checks.
The completed visual review supersedes the earlier blanket “not verified” status for the visible
specimens above, but does not assert every checklist cell passed or approve runtime behavior.
Long-label failure remains a real limitation: do not shrink type to make it pass. Always allow
scope remains proposed; reaction/error combinations are geometry stress tests, not runtime proof.

## Validation and enforcement limits

- Both changed `SKILL.md` files passed skill-creator `quick_validate.py`.
- `python3 -m unittest discover -s tools/render-evidence -p 'test_*.py'`: **48 tests passed**.
  Includes two new guidance-route tests and a negative broken-file/heading case.
- `git diff --check`: passed.
- Diff exclusions checked against base: pinned skills, vendor manifest, Factory configuration,
  workflows and product source trees have no changes.
- Original six-file patch hash remained identical after the work.

The new tests validate Markdown route integrity, not prose meaning or UI compliance. The existing
contract workflow discovers these tests when its path filters trigger (this PR adds a test under
`tools/`). This does not add a new CI trigger for future guidance-only edits. Run the documented
command for such edits; manual evidence review still owns visual and interaction acceptance.
Reactions/read/runtime remain not implemented; the broader activity model, permission policy and
exact Read backend remain proposed/unresolved. Inline-card presentation and compact-label fit
have current visual evidence; broader label fit and runtime behavior remain unresolved.

## Changed paths and publication boundary

Eight documentation files and one test file:

- `.claude/skills/design-system-delivery/SKILL.md`
- `.claude/skills/rem-design-system/SKILL.md`
- `.claude/skills/rem-design-system/references/component-track.md`
- `.claude/skills/rem-design-system/references/screen-track.md`
- `.claude/skills/rem-design-system/references/chat-review.md`
- `FILE-ORG.md`
- `REGISTRY.md`
- `docs/design-reconciliation/2026-10-09-chat-guidance-review.md`
- `tools/render-evidence/test_chat_guidance.py`

Prepared for a focused **draft PR against main**. The PR's live state is authoritative for remote
publication; this record is not a merge or approval assertion. No merge or product release is
part of this task. The completed design review is evidence for the recorded visual results;
unresolved product decisions and runtime acceptance remain separate work.
