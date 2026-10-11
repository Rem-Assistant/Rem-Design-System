# Agent simulator QA (proof of concept)

`.github/workflows/agent-sim-qa.yml` builds the iOS Playground for the simulator, boots an iPhone 15
(iOS 17.5) on a `macos-14` runner, and lets a headless Claude Code agent use the app the way a person
would: take a screenshot, look at it, tap or type, look again. It writes `qa-report.md` (each step,
what it saw, PASS/FAIL per expectation with screenshot filenames) and saves its screenshots.

This is **evidence alongside the XCUITest journeys, not a gate**. The deterministic suite
(`tools/playground-ios/run-hosted-journeys.sh`, `playground-native-candidate.yml`) remains the
release record. The agent is useful for "what is actually on screen when this breaks", and for
quick visual checks of a flow nobody has written a test for yet. Its judgement can be wrong; always
open the screenshots it cites.

## Files

| File | Purpose |
|---|---|
| `prompt.md` | Agent instructions. `{{UDID}}`, `{{BUNDLE_ID}}` and `{{SCENARIO}}` are filled in by the workflow. |
| `../../.github/workflows/agent-sim-qa.yml` | Build, simulator, AXe, the agent run, report check, artifact upload. |

## Running it

Actions, then **agent-sim-qa**, then **Run workflow**. Inputs:

- `source_ref`: the branch or SHA whose Playground to build. It must contain
  `tools/playground-ios` (main currently does not; use a Playground branch or candidate SHA). The
  harness and prompt always come from the dispatch ref.
- `scenario`: plain-language steps and expected outcomes.

Output: the job summary shows the report; the `agent-sim-qa-<run id>` artifact holds the report,
screenshots, the agent transcript (`transcript.jsonl`) and the build log. The job fails when the
report is missing, contains `FAIL`, or has no `OVERALL: PASS` line.

## Tools and versions

- Simulator control: [AXe](https://github.com/cameroncooke/AXe) `1.8.0`, downloaded from its release
  and checked against the same SHA-256 as its Homebrew formula (`axe describe-ui`, `axe tap --id`,
  `axe type`, `axe swipe`), plus `xcrun simctl io booted screenshot`.
- Agent: `@anthropic-ai/claude-code@2.1.283` on Node `22.20.0`, run with
  `--tools Bash,Read,Write`, `--allowedTools "Bash(axe:*)" "Bash(xcrun simctl io:*)" Read Write`,
  `--permission-mode dontAsk` (anything else is denied), `--max-turns 40`.

## Security

- `workflow_dispatch` only; no `pull_request` trigger, so fork code never runs beside the token.
- `permissions: contents: read`; both checkouts use `persist-credentials: false`.
- The agent gets `CLAUDE_CODE_OAUTH_TOKEN` (the repository's existing secret, also used by the
  `agent-*.yml` workflows) and nothing else: no GitHub token, no general shell, no MCP servers, and it
  runs in a scratch directory outside the checkouts, so no repository `CLAUDE.md` or settings load.
- The app under test is built from `source_ref` on the same runner before the agent step. The build
  cannot write `GITHUB_ENV`/`GITHUB_PATH`, but it is still code running on the runner, so dispatch
  only refs you would trust to run in CI.

## Cost

Each run uses roughly 15 to 30 macOS runner minutes (build is most of it; macOS minutes are billed
at a multiple of Linux minutes on private repositories) plus one Claude session of up to 40 turns
with many screenshots, charged to the existing subscription token's usage limits.

## Limitations

- One device, one OS, light appearance (`--settings-light`).
- The agent reads screenshots that are downscaled; tiny text or 1 px rendering issues are not
  reliable findings.
- Results are not deterministic; a flaky tap or misread screen can produce a false FAIL or, more
  rarely, an optimistic PASS. Screenshots are the evidence, not the verdict.
