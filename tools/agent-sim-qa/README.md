# Hosted visual diagnostic (agent-driven simulator check)

`.github/workflows/agent-sim-qa.yml` is a bounded, `workflow_dispatch`-only diagnostic. It builds the
iOS Playground unsigned for the simulator at one exact SHA, and lets a model drive one flow the way a
person would: it Reads a PNG of the screen, chooses one typed input, a runner-local XCTest bridge
executes it, and a new PNG is captured and Read. Every frame, UI listing and the receipt are kept
even when the run fails.

This is **diagnostic evidence, never a native receipt, release gate, approval or waiver.** The
deterministic journeys (`tools/playground-ios/run-hosted-journeys.sh` and the native/release
workflows) are unchanged and remain the record.

## Scenario

Playground root → Component catalog → Chat page → (optionally: model menu → Automatic) → composer
Add (`catalog.composerAdd`) → is the Add to Chat sheet (`catalog.addToChat.photos` / `.files`) on
screen? The model reports each chosen tap (identifier or point coordinates), and the receipt records
what the bridge reported, the after-frame, and whether the final UI listing contains the sheet
identifiers (an independent check on the model's reading of the PNG).

Inputs are choices, not free text:

- `source_ref`: `c23521b43fd6b0dd62e5c8207a45611ac86e4d4f` (Build 7 baseline, Add works; run first) or
  `6fa6366928c41331615bf1eb1810b871df8f9378` (Build 8 candidate, Add fails in XCUITest). Adding a SHA
  is a reviewed edit to the workflow.
- `scenario`: `catalog-chat-add` or `catalog-chat-model-menu-add`.

## Files

| File | Purpose |
|---|---|
| `controller.py` | Runner-local loop: source/SHA verification, mailbox to the bridge, `simctl` frames, model calls, receipt, summary. |
| `simqa.py` | Pure checks the controller relies on (transcript parsing, decision schema, acknowledgment, source state, deadline, redaction). |
| `AgentVisualBridge.swift` | XCTest input executor appended to the disposable checkout's UI-test file; reads typed commands from a local directory. |
| `prompt.md` | Per-frame instructions; `{{...}}` fields are filled by the controller. |
| `test_simqa.py` | Unit tests plus fake-runner end-to-end tests (fake `xcodebuild`, `xcrun`, `claude`). Run: `python3 -m unittest discover -s tools/agent-sim-qa -p 'test_*.py'`. |

## Review fixes and where they are enforced

1. **A screenshot counts only if its Read succeeded.** `simqa.parse_transcript` matches each Read
   `tool_use` to its `tool_result` and counts it only when it is not an error and contains an image
   block. The controller rejects a decision unless the current frame's Read succeeded
   (`png_read_failed`), and reports `successful_png_reads`.
2. **Every input is acknowledged by a later frame.** The controller numbers frames and timestamps
   every command's dispatch and completion on one clock. `simqa.check_acknowledged` requires that each
   action is followed by a frame captured after it completed (or, if it stalled, after dispatch) whose
   Read succeeded, before the next action or the finish, and that every decision is based on the
   latest frame. Any violation makes the status `unacknowledged`; the model's outcome is not accepted.
3. **Pristine source, exact SHA, built SHA.** `controller.py verify-source` asserts an empty
   `git status --porcelain --untracked-files=all` and `HEAD` equal to the input SHA before building,
   then (after the bridge is appended) that the only change is the UI-test file. `verify-built` reads
   `RemPlaygroundSourceSHA` from the built app's `Info.plist` (stamped from `REM_PLAYGROUND_SOURCE_SHA`,
   the key `run-hosted-journeys.sh` uses) and asserts it equals the SHA. All results land in
   `source-verification.json` and the receipt.
4. **Model time is bounded by one job deadline.** The first step fixes `JOB_DEADLINE`; the run step
   derives `min(JOB_DEADLINE, step start + 28 min)` (its own `timeout-minutes` is 30). Each model call
   runs in its own process group with a timeout of `min(180 s, time left − 150 s reserve)`, and is
   killed at that limit; when no budget remains the run stops as `deadline`. The receipt and frames are
   rewritten after every step, so a stop keeps the partial record.

## Bridge choice

AXe 1.8.0 was dropped: it may not support the `macos-14` Xcode, and it is an extra download. The
bridge instead uses XCTest, which ships with the same Xcode and simulator (iPhone 15, iOS 17.5) as
the native journeys. Like #109's loopback idea, but with no network at all: the controller writes
`cmd-NNN.json` into a runner-local directory passed as `TEST_RUNNER_AGENT_BRIDGE_DIR`; the bridge
writes `ack-NNN.json` when it starts a command and `res-NNN.json` when done. Commands are typed
(`observe`, `tap_id`, `tap_point`, `drag`, `end`); there is no port, tunnel or shell. Screenshots are
taken by `xcrun simctl io <udid> screenshot`, outside XCTest, so a stalled input still produces an
after-frame.

**Stalls.** XCTest input can wait on app idle. A command with no result after 45 s is recorded as a
stall (with a `stall-NNN.png`), the controller waits one more window (the single retry; input is
never resent), and if it is still stuck the model may only `finish` on a fresh frame. The status is
then `input_stalled` with the observed outcome.

## Outputs

Artifact `agent-sim-qa-<sha>-<run id>` (7 days): `frames/frame-NN.png` and `frame-NN.ui.txt` (the
frame before each input is the previous input's after-frame), any `stall-NNN.png`, `receipt.json`,
`transcript-summary.json`, `source-verification.json`, `summary.md`, `build.log`,
`xcodebuild-test.log`. The job summary shows `summary.md`. The job succeeds only for
`sheet_opened` with no acknowledgment violation and no contradicting UI listing.

## Security and privacy

- `workflow_dispatch` only; GitHub only runs it once this file is on the default branch.
- `permissions: contents: read`; both checkouts use `persist-credentials: false`; the app SHA is an
  allow-listed choice.
- The only secret is `CLAUDE_CODE_OAUTH_TOKEN` (the existing Factory subscription token used by
  `agent-*.yml`), referenced only in the run step. The controller passes it only to the model process,
  with an allow-listed environment (no `GITHUB_TOKEN`), and strips credential-named variables from
  the `xcodebuild`/XCTest process. It is never printed.
- The model has the `Read` tool only (`--tools Read`, `--strict-mcp-config`, `--safe-mode`,
  `--no-session-persistence`), runs in the frames folder, and its output must parse as one typed
  decision; free text is limited to a redacted observation.
- Raw stream-json transcripts stay in `$RUNNER_TEMP/agent-sim-qa/raw`, are never uploaded, and are
  deleted at the end. Only the sanitized summary is uploaded (tool names, Read targets, success,
  decisions without free text, timings). Quota or auth failures are reported as a category
  (`model_quota`, `model_auth`) and never retried with other credentials.
- Residual: the candidate app and its UI tests run on the same runner user as the controller while the
  token is in the controller's environment. That is why the SHA is an allow-listed choice.

## Cost

About 15 to 30 `macos-14` minutes per run (build is most of it; macOS minutes bill at 10x Linux on
private repositories), plus up to 14 short Read-only model calls charged to the existing subscription
token's usage limits. No new paid service.
