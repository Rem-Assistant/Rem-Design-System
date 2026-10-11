# Hosted visual proof

An isolated diagnostic for Build 8 candidate `6fa6366928c41331615bf1eb1810b871df8f9378`.
UNFINISHED HANDOFF: dispatch hook is not implemented. This main-based checkout does
not contain playground-native-candidate.yml; reconcile the registered workflow and
its branch before adding a narrow diagnostic dispatch entry. The separate reusable
macOS workflow has no publication or release steps, but has never run.

Independent review requires successful PNG Read-result verification, action-to-next-
frame acknowledgments, pristine-source/compiled-SHA verification, and a whole-session
time budget. The current draft does not yet satisfy those four requirements. Two
command-policy unit tests passed; no native or model/input proof ran.

Apple XCTest supplies the input adapter already installed with hosted Xcode. This is
not the scripted Add assertion test: a colocated model must issue Read for every PNG
before returning a typed coordinate action. The loopback-only bridge executes that
decision and captures the next image. There is no public port or remote desktop.
The 12-frame budget and timeout bound operation. App sources remain exactly the pinned
candidate; only the disposable UI-test target receives the driver. The original tests
are not edited, selected as proof, or reported passed.

The proof starts at the app root and visits Component catalog → Chat → Add. It does
not reproduce the preceding reactions, retry, and model-menu sequence of the failing
journey. A successful direct Add tap cannot establish that the original failure is fixed.

The existing Factory subscription secret is referenced only in the model controller
step. The model uses Factory's current CLI pin 2.1.236 and configured claude-opus-4-8,
Read only, no session persistence, no provider fallback, and no repository-change
requirement. Quota/auth/tool errors produce a blocked receipt and retained evidence.
No Builder publish entrypoint, bot private key, Figma token, or signing secret is used.
Candidate build/test processes receive an environment stripped of credential variables.

Artifacts retain PNGs, the action/Read trace, toolchain/runtime, exact app/control SHAs,
Xcode log, and xcresult for three days even on failure. Raw provider output is withheld.
These are diagnostics, never a native receipt, visual release acceptance, or waiver.

## Attribution and staged routing

The existing Factory Builder uses its App token to publish and configures commits as
`Agent Factory Builder <agent-factory-builder[bot]@users.noreply.github.com>`.
Existing PR 106 is authored by `samuelalake`; the diagnostic does not relabel it.
The new proof records workflow actor/run/control SHA/model separately. Future narrow
coordinator routing should link an assigned Builder run and exact candidate evidence;
the coordinator sets scope and consumes results, and the Builder owns implementation.
No automatic routing, normal Builder enablement, Figma writes, or release-policy changes
are introduced until the proof actually demonstrates the input loop.
