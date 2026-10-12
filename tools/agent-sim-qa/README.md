# Hosted visual diagnostic (agent-driven simulator check)

`.github/workflows/agent-sim-qa.yml` is a bounded, `workflow_dispatch`-only diagnostic. It builds the
iOS Playground unsigned for the simulator at one exact SHA, and lets a model drive one flow the way a
person would: it Reads a PNG of the screen, chooses one typed input, a runner-local XCTest bridge
executes it, and a new PNG is captured and Read. Every frame, UI listing and the receipt are kept
even when the run fails.

This is **diagnostic evidence, never a native receipt, release gate, approval or waiver.** The
deterministic journeys (`tools/playground-ios/run-hosted-journeys.sh` and the native/release
workflows) are unchanged and remain the record.

## Scenarios

The scenario catalog is `simqa.SCENARIOS`; each entry names the exact SHAs it may run on, a launch
profile, a decision budget, the brief the model reads, and (for check scenarios) the questions it must
answer. `controller.py check-inputs` refuses a scenario/SHA pair the catalog does not list, before the
build. Inputs are choices, not free text; adding a SHA or scenario is a reviewed edit to the workflow
and the catalog (`test_workflow_offers_exactly_the_catalog` keeps them identical).

| Scenario | Sources | What the model does |
|---|---|---|
| `catalog-chat-add`, `catalog-chat-model-menu-add` | Build 7 `c23521b`, Build 8 `6fa6366` | Catalog → Chat → (model menu → Automatic) → composer Add; outcome `sheet_opened` / `sheet_not_opened` / `blocked`, cross-checked against `catalog.addToChat.*` in the listing. |
| `b9-shell-chat-gestures` | Build 9 `73ac818` | Long conversation in the shell's full-screen Chat: vertical drags that start on a bubble and in the side margin (one at the journeys' slow-with-hold timing, one at default speed), a held left swipe (time peek), a long press. |
| `b9-hub-chat-gestures` | Build 9 | The same questions on the Developer hub's pushed Chat screen, the route the bubble-drag journeys use. |
| `b9-hub-chat-jump-to-latest` | Build 9 | Scroll up, jump to latest appears; tap it, it returns (twin of `testChatJumpToLatestAppearsWhenScrolledUpAndReturns`). |
| `b9-shell-sidebar-agent` | Build 9 | Sidebar opens; choosing Health closes it (twin of `testShellSidebarSelectsAnAgentAndClosesIt`). |
| `b9-shell-settings-from-account` | Build 9 | Money agent; account pill opens Settings; Done returns to Money (twin of `testShellSettingsFromAccountReturnsToTheSelectedChat`). |

Build 9 scenarios launch with the journeys' `JourneyLaunch` set (`--developer --settings-light
--no-motion`, profile `journey`), so they start on the Developer hub like the scripted journeys.

**Check scenarios.** The model tags each attempt with `check` and finishes with `findings`
(`yes`/`no`/`mixed`/`unclear`/`unconfirmed`) for every question; the contract's expected answer is never
shown to it. `unconfirmed` means "the Simulator did not show it, but a device might" — the honest answer
for a known Simulator-only artifact (a drag that starts on a message bubble does not scroll on the
Simulator for either arm, though it does on device; #129). It is evidence-bound like a positive finding,
is never a reproduced defect or a listing disagreement, and does not meet or fail the device contract:
it is surfaced as `needs_device_check` and does not fail the job. The harness binds each finding to
evidence: an answer other than `unclear` needs a tagged attempt that
started where the question says (on a listed `message.*.bubble` frame, or outside every bubble), and
`no` needs two such attempts (a second route) before it counts. Independently, it computes a listing
oracle per attempt from the accessibility listings before and after (median vertical shift of shared
bubbles, ≥ 20 pt counts as scrolled; presence/absence of identifiers; a header label) and records
agreement. On the `idb` arm the oracle is marked **unavailable** for a check that reads a modal sheet
(`sheet: True`, e.g. the account→Settings sheet): `idb ui describe-all` does not reliably list a
presented sheet's elements, so the harness withholds the oracle instead of contradicting a correct
answer (run 38187766878); the XCTest snapshot reads the sheet, so its oracle stays active. The time
peek has no oracle (times are hidden from accessibility), so the controller keeps
up to four `during-SEQ-K.png` frames while a drag with `hold_s` ≥ 0.5 is still held and offers them to
the model. The job succeeds only for `completed` with every counted finding equal to the contract and no
listing disagreement.

## Arms (touch injection)

Input `arm` chooses who turns the model's typed input into touches; scenarios, prompt, decision schema,
frames, checks and the trust boundary are identical.

- `xctest` (default): the XCTest bridge below. Its input is XCTest-synthesized, the same mechanism as the
  scripted journeys, so it inherits any synthesis artifact they have.
- `idb`: Meta's idb (`idb-companion` 1.1.8 from Meta's sha256-pinned release archive, `fb-idb` 1.1.7 in a venv;
  installed only for this arm, before the token step, and the job fails if other versions resolve). The
  controller installs the verified build with `simctl install`, launches it with `simctl launch` and the
  profile's arguments (`simqa.LAUNCH_ARGS`, kept equal to the bridge's), starts `idb_companion` on port
  10882 and sends `idb ui tap` (long press: `--duration`) and `idb ui swipe` (HID-level). The listing comes
  from `idb ui describe-all`, printed in the bridge's format so the oracle is shared; the raw
  `describe-all` output of every call is kept in the artifact (`idb/describe-all-*.json`) so a real
  listing sample — including a modal sheet's tree — is available to diagnose oracle disagreements.
  Mapping: idb swipes
  have a duration, not a velocity or a hold, so `speed` maps to 1.0 / 0.5 / 0.15 s per 300 pt and
  `hold_s` lengthens the swipe (cap 4 s); `press_s` is not representable and is ignored. The receipt
  records `idb_swipe_s` per drag and the install/launch/connect exits.

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
5. **A transient model failure is retried once, then given up.** If a model call fails with a transient
   category (`model_overloaded` — overloaded / 5xx / "unavailable" — or a bare `model_error` /
   `model_cli_exit`), the controller makes exactly one more call for the same frame, logged in the
   receipt's `model_retries`, and the retry is itself bounded by the same deadline. A usage/rate limit
   (`model_quota`) or auth failure (`model_auth`) is never retried. If the retry also fails the run ends
   `model_unavailable` (or `deadline`); an answer is never fabricated. This recovers runs that completed
   their probes and then hit a transient failure (38187758983, 38187760920).

## Bridge choice

AXe 1.8.0 was dropped: it may not support the `macos-14` Xcode, and it is an extra download. The
bridge instead uses XCTest, which ships with the same Xcode and simulator (iPhone 15, iOS 17.5) as
the native journeys. Like #109's loopback idea, but with no network at all: the controller writes
`cmd-NNN.json` into a runner-local directory passed as `TEST_RUNNER_AGENT_BRIDGE_DIR`; the bridge
writes `ack-NNN.json` when it starts a command and `res-NNN.json` when done. Commands are typed
(`observe`, `tap_id`, `tap_point`, `drag`, `long_press`, `end`); there is no port, tunnel or shell.
`drag` takes optional bounded timing (`speed` slow/default/fast, `press_s` 0.05–1, `hold_s` 0–2); without
them it is v1's drag exactly. `long_press` takes `duration_s` 0.5–2 (default 1). The launch profile
arrives as `TEST_RUNNER_AGENT_LAUNCH_PROFILE` and selects one of two fixed argument lists in Swift. Screenshots are
taken by `xcrun simctl io <udid> screenshot`, outside XCTest, so a stalled input still produces an
after-frame.

**Stalls.** XCTest input can wait on app idle. A command with no result after 45 s is recorded as a
stall (with a `stall-NNN.png`), the controller waits one more window (the single retry; input is
never resent), and if it is still stuck the model may only `finish` on a fresh frame. The status is
then `input_stalled` with the observed outcome.

## Outputs

Artifact `agent-sim-qa-<scenario>-<sha>-<run id>` (7 days): `frames/frame-NN.png` and `frame-NN.ui.txt`
(the frame before each input is the previous input's after-frame), any `during-SEQ-K.png` and
`stall-NNN.png`, `receipt.json` (with `checks` for check scenarios),
`transcript-summary.json`, `source-verification.json`, `summary.md`, `build.log`,
`xcodebuild-test.log`, and (idb arm) `idb/describe-all-*.json` raw listings and `idb-companion.log`.
The job summary shows `summary.md`. A catalog run succeeds only for
`sheet_opened` with no acknowledgment violation and no contradicting UI listing; a check run as above.

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
- idb arm: the pinned idb-companion archive (sha256-checked) and pip code (pinned versions) run
  as the same runner user, and `idb_companion` listens on a TCP port on the disposable runner for the
  run's duration. It and every `idb` call get the credential-free environment; the model cannot reach
  it (Read tool only).

## Cost

About 15 to 30 `macos-14` minutes per run (build is most of it; macOS minutes bill at 10x Linux on
private repositories), plus up to 14 (16 for the account-settings scenario, 22 for the gesture scenarios) short Read-only model calls charged to the existing subscription
token's usage limits. No new paid service.

## PR compile-only check

`agent-sim-qa-compile.yml` runs on updates to the `claude/agent-sim-qa` and
`claude/agent-tester-pilot` PR branches. It compiles the exact PR-head bridge against every
dispatchable source (Build 7 `c23521b`, Build 8 `6fa6366`, Build 9 `73ac818` — the full
`simqa.SCENARIOS` source set, kept in step by `test_compile_matrix_covers_every_dispatchable_source`)
in a disposable checkout, using Xcode 15.4 on macOS 14 and unsigned iOS Simulator `build-for-testing`. The existing
source and built-SHA checks and Python tests run too. No simulator boots, model calls, signing,
store uploads or secrets are involved. The result proves compilation only; mailbox/environment
propagation and screenshot/action proof still require the separately approved diagnostic. This
check does not register, dispatch or change the diagnostic workflow or its source allowlist.
