# Drive-verification: producing honest visual evidence

An agent clears `needs-drive` by **driving the real running product and showing a picture of it
working** — not by pointing at a passing test. This file is how to do that without fooling
yourself.

## The stance

You are a QA engineer, not a test author. Author the behavior **from a clean slate**, the way a
first-time user would — never from a seeded fixture that skips the discoverable path. Half of
real bugs live in the gap between "the render path works when state is injected" and "a human
can actually get to that state." Your job is to find that gap, out loud.

**Evidence or it didn't happen.** The atom leaves `needs-drive` only with an attached screenshot
of the real behavior. If you can't produce one, it stays `needs-drive` and you name the wall.
Reporting "I could not make X work" is a *success* of this gate — it's exactly the signal green
tests hide.

## The loop

1. **Get it running.** Start the dev server (a launch config + the platform's preview tooling,
   or a plain `run` in the background). If the app won't boot, the first finding is *why* — a
   stale dependency install is common; reconciling it (`npm install` / reinstall) is often the
   real unblock, and getting the app runnable is itself high-value for every future verification.
2. **Reach the surface.** Navigate to the exact screen/route. For gated apps, use the
   documented dev/preview entry (a reserved local fixture route, a preview auth flag) rather
   than fighting real auth.
3. **Author from empty.** Do the real gesture — create the thing, don't inject it.
4. **Observe + capture.** Screenshot the actual result. Read the DOM / console / network to
   confirm state changed, not just that pixels moved. Try to break it (edge inputs, scrub,
   reload).
5. **Report honestly.** What worked (with the screenshot), what didn't (with the blocker), and
   whether any finding deserves its own new issue. File those immediately.

## Real pointer events (the gotcha that wastes hours)

Many component libraries (React Aria, Radix, and friends) open menus / trigger controls on
**pointer** events, and a plain synthetic `click` fires the wrong sequence — the control looks
**inert** (enabled, no error, nothing happens). Before concluding "this button is broken,"
confirm it isn't just the automation missing pointer events. Dispatch a full, well-specified
sequence:

```js
const r = el.getBoundingClientRect();
const base = { bubbles:true, cancelable:true, pointerId:1, isPrimary:true, button:0,
               pointerType:'mouse', clientX:r.x+r.width/2, clientY:r.y+r.height/2 };
el.dispatchEvent(new PointerEvent('pointerover', {...base, buttons:0}));
el.dispatchEvent(new PointerEvent('pointerdown', {...base, buttons:1}));
el.dispatchEvent(new PointerEvent('pointerup',   {...base, buttons:0}));
el.dispatchEvent(new MouseEvent('click',        {...base, buttons:0}));
```

If the real behavior appears with this but not with a plain click, it's an **automation
artifact**, not a product bug — say so, and (if a real human confirms the gesture works for
them) close any bug you filed as such. If it's still dead after real pointer events, it's a
genuine finding.

## Check the viewport is real *before* you conclude anything (the (0,0) trap)

If the headless browser opens at a **0×0 viewport**, every element has a zero-size bounding rect,
every ref-click resolves to (0,0), and nothing responds — indistinguishable from a totally broken
app. Confirm the window has size *first*: read `window.innerWidth/innerHeight`; if either is 0,
resize (e.g. 1440×900) and re-check before trusting a single inert click. Hours of "the whole
editor is dead" evaporate here. (A `read_page` showing `Viewport: 0x0` is the same tell.)

Scrubbing a value slider: pointer-drag often needs the thumb's capture and may not take. An ARIA
`role=slider` responds to keyboard — `Home`/`End` jump to min/max, arrows step — which is more
reliable for landing exact frames. Synchronous repeated key events in one JS turn may only commit
once (React reads a stale value); space them across separate calls if you need to advance.

## What counts as evidence

- A **screenshot** of the actual UI state (the diamond appeared, the value updated, the error
  showed). This is the primary artifact — attach it to the PR/issue.
- Supporting: a DOM/`localStorage`/store read proving the document actually mutated; a console
  with no errors; a network response.
- **Not** evidence: "the test passes", "the code looks right", a screenshot of a *seeded*
  fixture rather than an authored-from-scratch result.

### Getting the picture *into* GitHub

Plain `gh issue comment` / `gh pr comment` **cannot upload an image** — there is no attach flag,
and the web drag-and-drop path isn't available to an agent. So a real screenshot won't reach the
issue on its own. Use a `gh` image-upload extension — **`gh-image`** is the recommended one — to
post the screenshot onto the issue/PR so the evidence lives with the work. If you genuinely have
no upload path this run, post the evidence as a **detailed written observation** (exact states,
values, before/after) **and say plainly that the image could not be attached** — never let a
text-only note quietly read as full visual proof (that's how the drive gate silently degrades).

A **screen recording** is a nice bonus but is *not* the bar — recording is often unavailable to
an agent, and gating `needs-drive` on video would just block the gate. Evidence is granular and
takes several forms: **stills + a DOM/state read** are the reliable baseline; a short clip, a
network trace, or a console capture are welcome additions, not requirements.

## When you can't drive

- **App won't run / dependency wall you can't clear** → leave `needs-drive`, name the wall, and
  hand it to the human (a real human pointer / their environment may succeed where automation
  can't).
- **CI is the only other check, and it's down** (disabled, or Actions billing lapsed) → then
  drive-verify is the *only* real gate; do not merge on a green that never ran.
- **The behavior needs an owner decision or design** → not a verification problem; it's a
  `spec-decision` for the human.

## Field notes — thinking like a user (owner-maintained, growing)

Agents test what they *built*; users test what they *wanted*. The bugs live in that gap, so a
drive that only re-walks the code path just re-confirms the author's assumptions. This section
collects concrete "test it like a person would" heuristics — **the owner adds to it** as they
notice the specific ways an agent's drive missed what a real session would catch. Seed rules:

- **Start where a user starts, not where the code starts.** Empty document, default settings,
  first-run state — not a fixture pre-loaded with the exact shape your feature expects.
- **Use the visible affordance.** If a person would reach it by clicking a button in a panel,
  click *that* — don't call the underlying action directly. "Reachable in code" ≠ "reachable by a
  human"; the discoverability gap is a real finding.
- **Do the whole small task, end to end.** Not "the opacity prop changed" but "I added a fade,
  scrubbed, and watched it fade" — the way the feature is actually used, in one sitting.
- **Then try to break it like a distracted human would** — double-click, reload mid-action, hit
  the wrong order, leave a field empty, undo. Steady-state happy paths hide the real gaps.
- **Drive by the DOM, not by the screenshot (Composa, 2026-07).** The in-app browser pane returned
  screenshots at *varying* sizes (800, 757, 603 px wide) for the *same* 1440×900 viewport, so
  screenshot-pixel coordinates — and `ref`-clicks, which report CSS coords — landed off-target and
  controls looked inert. Reliable recipe: (1) **verify state via `javascript_tool` DOM/store reads**
  (`aria-selected`, visible section text, timeline layer/keyframe counts) — never trust a downscaled
  screenshot to tell you a tab didn't switch or a row is empty; it lied twice in one session. (2)
  **Click by dispatching real pointer events on the element by selector** (coordinate-free; the
  buttons:1-on-pointerdown sequence above). (3) Take a screenshot only as the *final human-facing
  evidence artifact*, not to make driving decisions.
- **The different-origin localStorage confound (Composa, 2026-07).** Comparing trunk (`localhost:5173`)
  vs a worktree branch (`127.0.0.1:5174`) is *not* apples-to-apples: they are different origins with
  **separate localStorage**, so autosaved editor state (keyframes, docs) on one is absent on the other.
  A branch row that looks "stripped" may just be the fresh `initialState` with no autosave. Confirm by
  reading `localStorage` on each origin before calling a difference a regression — a code reviewer
  saying "the data path is untouched" against your drive saying "it's gone" is the tell to check this.
- *(Owner: append your own here as they come up — this list is the point of the file.)*
