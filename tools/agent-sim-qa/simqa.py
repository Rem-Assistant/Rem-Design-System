"""Pure checks for the hosted visual diagnostic (no I/O, no subprocesses; see test_simqa.py).

Each function here is one of the harness-enforced guarantees the controller relies on:

- parse_transcript: a screenshot counts as seen only when the Read tool_result for that exact
  file came back as image content (review fix 1), not merely when a Read was issued.
- check_acknowledged: every action is followed by a newer, successfully read frame before the
  next action or the final outcome (review fix 2).
- check_source_state / check_built_sha: pristine source at the exact SHA, and the built app
  embeds that SHA (review fix 3).
- call_budget: every model call is bounded by the single job deadline (review fix 4).
"""
from __future__ import annotations

import json
import re
from pathlib import PurePath

SHA_RE = re.compile(r'^[0-9a-f]{40}$')
ID_RE = re.compile(r'^[A-Za-z0-9_.\-]{1,80}$')
ACTIONS = {
    'tap_id': {'id'},
    'tap_point': {'x', 'y'},
    'drag': {'x', 'y', 'to_x', 'to_y'},
    'long_press': {'x', 'y'},
    'finish': {'outcome'},
}
# Optional, bounded gesture parameters (seconds; speed maps to XCUIGestureVelocity in the bridge).
# The bridge's defaults are the v1 drag (press 0.1 s, default speed, no hold) and a 1 s long press.
SPEEDS = ('slow', 'default', 'fast')
OPTIONAL = {
    'drag': {'speed': SPEEDS, 'press_s': (0.05, 1.0), 'hold_s': (0.0, 2.0)},
    'long_press': {'duration_s': (0.5, 2.0)},
}
PURPOSES = {'navigate', 'model_menu', 'add_tap', 'scroll', 'dismiss', 'probe', 'other'}
OUTCOMES = {'sheet_opened', 'sheet_not_opened', 'blocked'}
FINDINGS = ('yes', 'no', 'mixed', 'unclear')
SHEET_IDS = ('catalog.addToChat.photos', 'catalog.addToChat.files')
INJECTED_FILE = 'tools/playground-ios/UITests/PlaygroundNavigationUITests.swift'

# ---- Scenario catalog ------------------------------------------------------------------------------
# Each scenario is a reviewed choice in agent-sim-qa.yml. `sources` are the exact app SHAs it may run
# on; `launch` names a launch-argument profile the Swift bridge owns ('legacy' = --settings-light,
# 'journey' = JourneyLaunch's --developer --settings-light --no-motion). Check scenarios name questions
# the model answers with FINDINGS; each answer must cite tagged attempts, and the harness compares it
# with an independent accessibility-listing oracle where one exists. `expect` is the contract answer
# (docs/contracts/chat.md) and is never shown to the model.

BUILD7 = 'c23521b43fd6b0dd62e5c8207a45611ac86e4d4f'
BUILD8 = '6fa6366928c41331615bf1eb1810b871df8f9378'
BUILD9 = '73ac81894508035730e901476f03d5b909ef758e'
BUBBLE = r'^message\..+\.bubble$'

_ADD_RULES = (
    'If the Add tap has not visibly worked, you may retry it once as a `tap_point` at the Add button\'s '
    'frame centre. `finish` with `sheet_opened` only if THIS screenshot shows the Add to Chat sheet with '
    'Photos and Files after your Add tap; `sheet_not_opened` if Add was tapped (and retried once) and the '
    'sheet is not on screen; `blocked` if you cannot reach the Add button. Do not tap Photos, Files, links, '
    'or anything outside the scenario. Use `purpose: "add_tap"` for every tap on the composer\'s Add '
    'button and `model_menu` for taps on the model menu or its Automatic item.')

_CHECK_RULES = (
    'Tag every attempt at a question with `"check": "<question key>"` and `purpose: "probe"`; untagged '
    'inputs are navigation. Judge each attempt by comparing the screenshot BEFORE it with the one AFTER it '
    '(for example, the same message\'s position). Every answer other than `unclear` needs at least one '
    'tagged attempt, and `no` needs two (a second route: another start point or speed) because a bug is '
    'reported only once it reproduces. Use `mixed` when attempts disagree. `finish` with `completed` once '
    'every question has an answer, or `blocked` (answers may be `unclear`) if you cannot reach the screen.')

_SHELL_LONG = (
    'The app opens on the Developer hub. Set the Chat conversation picker (identifier chatFixturePicker, '
    'a segmented control; tap its "Long" segment) to Long, then tap Close (developer.close). You are now in '
    'the full-screen Chat of the app shell (header button chat.header.menu), showing the newest message. ')
_HUB_LONG = (
    'The app opens on the Developer hub. Set the Chat conversation picker (identifier chatFixturePicker, '
    'a segmented control; tap its "Long" segment) to Long, then tap openChatScreen. You are now on the Chat '
    'screen pushed from the hub (header Back, chat.header.back), showing the newest message. ')
_GESTURES = (
    'Then, as a person would, answer: '
    '(bubble_drag_scrolls) Press on the CENTRE of a message bubble (a listed message.<id>.bubble frame) and '
    'drag straight DOWN about 300 points, which should reveal older messages. Make one attempt with '
    '`speed` "slow", `press_s` 0.05, `hold_s` 0.3 and one with `speed` "default", `press_s` 0.05, `hold_s` 0. '
    '(margin_drag_scrolls) The same two drags, but starting in the empty side margin of the transcript, '
    'level with a message, outside every bubble frame (for example x = 8). '
    '(swipe_left_reveals_times) Drag a message bubble LEFT about 120 points, keeping y constant, with '
    '`hold_s` 1.5; the harness also captures frames while the finger is still down (listed as '
    'during_frames in the history; Read them; the first may precede the touch). Do message times appear at the trailing edge while held, '
    'and do the rows return to rest afterwards? '
    '(long_press_opens_actions) `long_press` the centre of a message bubble with `duration_s` 1.0. Does a '
    'message actions sheet (reactions and actions such as Copy) appear? Then dismiss it (drag the sheet '
    'down or tap above it). ')
_UI_CHECK = {'aggregate': 'final'}

SCENARIOS = {
    'catalog-chat-add': {
        'sources': (BUILD7, BUILD8), 'launch': 'legacy', 'max_decisions': 14,
        'outcomes': OUTCOMES, 'rules': _ADD_RULES, 'checks': {},
        'brief': (
            'From the Playground root, open the Component catalog (identifier openComponents), go to its '
            'Chat page, bring the composer into view, then tap the composer Add button (identifier '
            'catalog.composerAdd) and check whether the Add to Chat sheet appears with Photos '
            '(catalog.addToChat.photos) and Files (catalog.addToChat.files). Then finish.'),
    },
    'catalog-chat-model-menu-add': {
        'sources': (BUILD7, BUILD8), 'launch': 'legacy', 'max_decisions': 14,
        'outcomes': OUTCOMES, 'rules': _ADD_RULES, 'checks': {},
        'brief': (
            'From the Playground root, open the Component catalog (identifier openComponents) and go to '
            'its Chat page. Open the model menu (catalog.modelMenu), choose Automatic, and confirm the menu '
            'closed. Then bring the composer into view, tap the composer Add button (identifier '
            'catalog.composerAdd) and check whether the Add to Chat sheet appears with Photos '
            '(catalog.addToChat.photos) and Files (catalog.addToChat.files). Then finish.'),
    },
}

_GESTURE_CHECKS = {
    'bubble_drag_scrolls': {
        'question': 'Did a vertical drag that started ON a message bubble scroll the conversation?',
        'expect': 'yes', 'start': 'bubble', 'aggregate': 'each', 'oracle': [{'bubble_shift': 20}]},
    'margin_drag_scrolls': {
        'question': 'Did a vertical drag that started in the side margin (beside, not on, a bubble) scroll it?',
        'expect': 'yes', 'start': 'beside', 'aggregate': 'each', 'oracle': [{'bubble_shift': 20}]},
    'swipe_left_reveals_times': {
        'question': 'While a leftward drag on a bubble was held, did message times appear, and did the rows '
                    'return to rest after release?',
        'expect': 'yes', 'start': 'bubble', 'aggregate': 'each', 'oracle': None},  # times are hidden from AX
    'long_press_opens_actions': {
        'question': 'Did a long press on a message bubble open the message actions sheet?',
        'expect': 'yes', 'start': 'bubble', 'aggregate': 'each',
        'oracle': [{'present': r'^message\..+\.actions\.'}]},
}
for _name, _route in (('b9-shell-chat-gestures', _SHELL_LONG), ('b9-hub-chat-gestures', _HUB_LONG)):
    SCENARIOS[_name] = {
        'sources': (BUILD9,), 'launch': 'journey', 'max_decisions': 22, 'outcomes': {'completed', 'blocked'},
        'rules': _CHECK_RULES, 'brief': _route + _GESTURES, 'checks': _GESTURE_CHECKS}

SCENARIOS['b9-hub-chat-jump-to-latest'] = {
    'sources': (BUILD9,), 'launch': 'journey', 'max_decisions': 14, 'outcomes': {'completed', 'blocked'},
    'rules': _CHECK_RULES,
    'brief': _HUB_LONG + (
        'Then answer: (jump_appears_after_scroll_up) Scroll up toward older messages the way a person would. '
        'Does a "Scroll to latest message" button appear above the composer? Tag the scroll that made it '
        'appear (or your last scroll attempt). (jump_returns_to_latest) Tap that button. Does the '
        'conversation return to the newest message and the button disappear?'),
    'checks': {
        'jump_appears_after_scroll_up': {
            'question': 'After scrolling up, did the jump to latest button appear?', 'expect': 'yes',
            'oracle': [{'present': r'^chat\.jumpToLatest$'}], **_UI_CHECK},
        'jump_returns_to_latest': {
            'question': 'Did tapping it return to the newest message and hide the button?', 'expect': 'yes',
            'oracle': [{'absent': r'^chat\.jumpToLatest$'}, {'present': r'^message\.a2\.bubble$'}], **_UI_CHECK},
    },
}
SCENARIOS['b9-shell-sidebar-agent'] = {
    'sources': (BUILD9,), 'launch': 'journey', 'max_decisions': 14, 'outcomes': {'completed', 'blocked'},
    'rules': _CHECK_RULES,
    'brief': (
        'The app opens on the Developer hub; tap Close (developer.close) to reach the full-screen Chat of the '
        'app shell. Then answer: (sidebar_opens) Tap the header\'s Open sidebar button. Does a sidebar listing '
        'agents open? (agent_selected_closes_sidebar) Choose the Health agent in it. Does the sidebar close '
        'and the chat show Health\'s conversation?'),
    'checks': {
        'sidebar_opens': {
            'question': 'Did the header button open the sidebar?', 'expect': 'yes',
            'oracle': [{'present': r'^sidebar\.close$'}], **_UI_CHECK},
        'agent_selected_closes_sidebar': {
            'question': 'Did choosing Health close the sidebar and show Health\'s chat?', 'expect': 'yes',
            'oracle': [{'absent': r'^sidebar\.close$'}, {'label': ['chat.header.identity', '^Health']}],
            **_UI_CHECK},
    },
}
SCENARIOS['b9-shell-settings-from-account'] = {
    'sources': (BUILD9,), 'launch': 'journey', 'max_decisions': 16, 'outcomes': {'completed', 'blocked'},
    'rules': _CHECK_RULES,
    'brief': (
        'The app opens on the Developer hub; tap Close (developer.close) to reach the full-screen Chat of the '
        'app shell. Open the sidebar from the header and choose the Money agent. Then answer: '
        '(account_pill_opens_settings) Open the sidebar again and tap the account pill (the person\'s name '
        'at the bottom). Does Settings open? (done_returns_to_same_chat) Tap Done. Does Settings close and '
        'the chat still show Money?'),
    'checks': {
        'account_pill_opens_settings': {
            'question': 'Did the account pill open Settings?', 'expect': 'yes',
            'oracle': [{'present': r'^settings\.done$'}], **_UI_CHECK},
        'done_returns_to_same_chat': {
            'question': 'Did Done close Settings and return to Money\'s chat?', 'expect': 'yes',
            'oracle': [{'absent': r'^settings\.done$'}, {'label': ['chat.header.identity', '^Money']}],
            **_UI_CHECK},
    },
}


def check_inputs(sha, scenario):
    """The dispatch pair is allowed: a known scenario on one of its exact source SHAs."""
    spec = SCENARIOS.get(scenario)
    if spec is None:
        return [f'unknown scenario {scenario!r}']
    if sha not in spec['sources']:
        return [f'scenario {scenario} does not run on {sha}; allowed: {", ".join(spec["sources"])}']
    return []


class DecisionError(ValueError):
    pass


# ---- Review fix 1: a Read must have succeeded with image content -------------------------------

def parse_transcript(lines, expected_png=None):
    """Summarise one stream-json transcript without keeping any content.

    Returns tool uses (name, target basename, ok), the count of successful PNG reads, whether
    `expected_png` (a path or basename) was successfully read as an image, and the final result
    text. A Read counts as successful only when its tool_result is not an error and contains an
    image block.
    """
    uses = {}
    order = []
    results = {}
    final = {'result': None, 'is_error': None, 'num_turns': None, 'model': None, 'subtype': None}
    for raw in lines:
        raw = raw.strip()
        if not raw:
            continue
        try:
            event = json.loads(raw)
        except ValueError:
            continue
        if not isinstance(event, dict):
            continue
        kind = event.get('type')
        if kind == 'system' and event.get('subtype') == 'init':
            final['model'] = event.get('model') if isinstance(event.get('model'), str) else None
        message = event.get('message') if isinstance(event.get('message'), dict) else {}
        content = message.get('content') if isinstance(message.get('content'), list) else []
        if kind == 'assistant':
            for block in content:
                if isinstance(block, dict) and block.get('type') == 'tool_use' and block.get('id'):
                    tool_input = block.get('input') if isinstance(block.get('input'), dict) else {}
                    target = tool_input.get('file_path')
                    uses[block['id']] = {
                        'name': str(block.get('name')),
                        'target': PurePath(target).name if isinstance(target, str) else None,
                        'target_path': target if isinstance(target, str) else None,
                    }
                    order.append(block['id'])
        elif kind == 'user':
            for block in content:
                if isinstance(block, dict) and block.get('type') == 'tool_result':
                    body = block.get('content')
                    has_image = isinstance(body, list) and any(
                        isinstance(part, dict) and part.get('type') == 'image' for part in body)
                    results[block.get('tool_use_id')] = (block.get('is_error') is not True) and has_image
        elif kind == 'result':
            final['result'] = event.get('result') if isinstance(event.get('result'), str) else None
            final['is_error'] = event.get('is_error') is True
            final['num_turns'] = event.get('num_turns')
            final['subtype'] = event.get('subtype') if isinstance(event.get('subtype'), str) else None
    tool_uses = []
    for use_id in order:
        use = uses[use_id]
        tool_uses.append({
            'name': use['name'],
            'target': use['target'],
            'ok': bool(results.get(use_id, False)),
            'answered': use_id in results,
        })
    expected = PurePath(expected_png).name if expected_png else None
    png_reads = [u for u in tool_uses if u['name'] == 'Read' and u['ok'] and (u['target'] or '').endswith('.png')]
    return {
        'tool_uses': tool_uses,
        'successful_png_reads': len(png_reads),
        'expected_read_ok': bool(expected) and any(u['target'] == expected for u in png_reads),
        'result': final['result'],
        'is_error': final['is_error'],
        'num_turns': final['num_turns'] if isinstance(final['num_turns'], int) else None,
        'model': final['model'],
        'subtype': final['subtype'],
    }


# ---- Decisions -----------------------------------------------------------------------------------

def extract_json(text):
    """The model's final message: one JSON object, optionally fenced."""
    if not isinstance(text, str):
        raise DecisionError('no decision text')
    stripped = text.strip()
    fence = re.search(r'```(?:json)?\s*(\{.*\})\s*```', stripped, re.S)
    if fence:
        stripped = fence.group(1)
    start, end = stripped.find('{'), stripped.rfind('}')
    if start < 0 or end <= start:
        raise DecisionError('no JSON object in decision')
    try:
        value = json.loads(stripped[start:end + 1])
    except ValueError as error:
        raise DecisionError('decision is not valid JSON') from error
    if not isinstance(value, dict):
        raise DecisionError('decision is not an object')
    return value


def _number(value, key, low, high):
    if isinstance(value, bool) or not isinstance(value, (int, float)) or value != value:
        raise DecisionError(f'{key} is not a number')
    if not low <= value <= high:
        raise DecisionError(f'{key} is outside {low}..{high}')


def validate_decision(value, frame, screen, finish_only=False, scenario='catalog-chat-add'):
    """Accept exactly one typed action for `frame`. Coordinates are points inside `screen`."""
    spec = SCENARIOS[scenario]
    checks = spec['checks']
    if not isinstance(value, dict):
        raise DecisionError('decision is not an object')
    if value.get('frame') != frame or isinstance(value.get('frame'), bool):
        raise DecisionError('decision names a different frame')
    kind = value.get('action')
    if kind not in ACTIONS:
        raise DecisionError('unknown action')
    if finish_only and kind != 'finish':
        raise DecisionError('only finish is allowed after an input stall')
    required = {'frame', 'action', 'observation'} | ACTIONS[kind]
    if kind == 'finish':
        if checks:
            required = required | {'findings'}
        allowed = required
    else:
        required = required | {'purpose'}
        allowed = required | set(OPTIONAL.get(kind, ())) | ({'check'} if checks else set())
    if not required <= set(value) or not set(value) <= allowed:
        raise DecisionError('unexpected or missing decision fields')
    observation = value['observation']
    if not isinstance(observation, str) or not observation.strip() or len(observation) > 600:
        raise DecisionError('observation must be 1..600 characters')
    if kind == 'finish':
        if value['outcome'] not in spec['outcomes']:
            raise DecisionError('unknown outcome')
        if checks:
            findings = value['findings']
            if not isinstance(findings, dict) or set(findings) != set(checks) or \
                    any(v not in FINDINGS for v in findings.values()):
                raise DecisionError('findings must answer every question with ' + '|'.join(FINDINGS))
        return value
    if value['purpose'] not in PURPOSES:
        raise DecisionError('unknown purpose')
    if 'check' in value and value['check'] not in checks:
        raise DecisionError('unknown check')
    if kind == 'tap_id':
        if not isinstance(value['id'], str) or not ID_RE.match(value['id']):
            raise DecisionError('invalid accessibility identifier')
        return value
    width, height = screen
    for key in ACTIONS[kind]:
        number = value[key]
        if isinstance(number, bool) or not isinstance(number, (int, float)) or number != number:
            raise DecisionError(f'{key} is not a number')
        limit = width if key in ('x', 'to_x') else height
        if not 1 <= number <= limit - 1:
            raise DecisionError(f'{key} is outside the screen')
    for key, bound in OPTIONAL.get(kind, {}).items():
        if key not in value:
            continue
        if bound is SPEEDS:
            if value[key] not in SPEEDS:
                raise DecisionError(f'{key} must be one of {", ".join(SPEEDS)}')
        else:
            _number(value[key], key, *bound)
    return value


# ---- Review fix 2: every action is acknowledged by a later, successfully read frame -------------

def check_acknowledged(events):
    """Return violations (empty when acknowledged).

    `events` is the controller ledger in order: frame {index, captured_at, read_ok}, action
    {index, basis_frame, dispatched_at, done_at|None}, finish {basis_frame}. Times share one clock.
    An action is acknowledged by the first frame captured strictly after it completed (or, when it
    stalled, after it was dispatched) whose PNG Read succeeded, before the next action or finish.
    Every decision must be based on the latest frame, which must itself have been read.
    """
    violations = []
    frames = {}
    pending = None
    latest_frame = None
    finished = False
    for event in events:
        kind = event.get('type')
        if kind == 'frame':
            frames[event['index']] = event
            latest_frame = event
            if pending is not None and event.get('read_ok'):
                anchor = pending.get('done_at') if pending.get('done_at') is not None else pending['dispatched_at']
                if event['captured_at'] > anchor:
                    pending = None
        elif kind in ('action', 'finish'):
            if finished:
                violations.append('event after finish')
            basis = frames.get(event.get('basis_frame'))
            if basis is None or basis is not latest_frame:
                violations.append(f'{kind} not based on the latest frame')
            elif not basis.get('read_ok'):
                violations.append(f'{kind} based on frame {basis["index"]} whose PNG Read did not succeed')
            if pending is not None:
                violations.append(f'action {pending["index"]} was not acknowledged by a later read frame '
                                  f'before the next {kind}')
                pending = None
            if kind == 'action':
                pending = event
            else:
                finished = True
    if pending is not None:
        violations.append(f'action {pending["index"]} was never acknowledged by a later read frame')
    if not finished:
        violations.append('no finish decision')
    return violations


# ---- Review fix 3: pristine source at the exact SHA, and the built app embeds it ------------------

def check_source_state(head, expected_sha, porcelain, allow_injected=False):
    """`porcelain` is `git status --porcelain --untracked-files=all` output."""
    problems = []
    if not SHA_RE.match(expected_sha or ''):
        problems.append('source_ref is not a full 40-character SHA')
    if head != expected_sha:
        problems.append(f'checkout HEAD {head} is not {expected_sha}')
    lines = [line for line in porcelain.splitlines() if line.strip()]
    if allow_injected:
        if lines != [f' M {INJECTED_FILE}']:
            problems.append('working tree differs other than the injected UI-test bridge: '
                            + '; '.join(lines[:10]))
    elif lines:
        problems.append('source checkout is not pristine: ' + '; '.join(lines[:10]))
    return problems


def check_built_sha(plist, expected_sha, key='RemPlaygroundSourceSHA'):
    value = plist.get(key) if isinstance(plist, dict) else None
    if value != expected_sha:
        return [f'built app {key} is {value!r}, expected {expected_sha}']
    return []


# ---- Review fix 4: one deadline bounds every model call -------------------------------------------

def call_budget(now, deadline, reserve, cap):
    """Seconds the next model call may take, or 0 when the job deadline leaves no room."""
    remaining = deadline - now - reserve
    if remaining < 20:
        return 0
    return int(min(cap, remaining))


# ---- Outcome checks and sanitisation ---------------------------------------------------------------

def ui_confirms_sheet(ui_text):
    if not isinstance(ui_text, str):
        return None
    return any(f'id={sheet_id} ' in ui_text + ' ' for sheet_id in SHEET_IDS)


_SECRET_PATTERNS = [
    re.compile(r'sk-ant-[A-Za-z0-9_\-]+'),
    re.compile(r'gh[pousr]_[A-Za-z0-9]{20,}'),
    re.compile(r'github_pat_[A-Za-z0-9_]{20,}'),
    re.compile(r'(?i)\b(bearer|authorization|x-api-key|cookie)\b\s*[:=]?\s*\S+'),
    re.compile(r'(?i)\b[A-Z0-9_]*(TOKEN|SECRET|PASSWORD|API_KEY|PRIVATE_KEY)[A-Z0-9_]*\s*[=:]\s*\S+'),
    re.compile(r'eyJ[A-Za-z0-9_\-]{10,}\.[A-Za-z0-9_\-]{10,}\.[A-Za-z0-9_\-]+'),
    re.compile(r'(?<![0-9a-f])[A-Za-z0-9+/_\-]{48,}={0,2}'),
]


def redact(text, limit=600):
    if not isinstance(text, str):
        return text
    for pattern in _SECRET_PATTERNS:
        text = pattern.sub('[redacted]', text)
    return text[:limit]


PNG_SIGNATURE = b'\x89PNG\r\n\x1a\n'
PNG_IEND = b'\x00\x00\x00\x00IEND\xaeB`\x82'


def complete_png(data):
    """A whole PNG file: the signature, at least one chunk, and the final IEND chunk.

    `simctl io screenshot` can write a complete frame and then exit non-zero or not exit at all
    (runs 38118674016, 38118681715), so the file, not the process status, decides."""
    return len(data) > len(PNG_SIGNATURE) + len(PNG_IEND) and data.startswith(PNG_SIGNATURE) \
        and data.endswith(PNG_IEND)


def classify_failure(exit_code, timed_out, summary):
    """A category only; never the provider's text."""
    if timed_out:
        return 'model_timeout'
    result = (summary.get('result') or '').lower() if summary else ''
    if any(word in result for word in ('usage limit', 'rate limit', 'quota', 'overloaded')):
        return 'model_quota'
    if any(word in result for word in ('auth', 'login', 'credential', 'oauth', 'unauthorized', '401', '403')):
        return 'model_auth'
    if summary and summary.get('is_error'):
        return 'model_error'
    if exit_code:
        return 'model_cli_exit'
    return None


# ---- Check scenarios: evidence-bound findings and the accessibility-listing oracle ---------------------

_LISTING_LINE = re.compile(r'^(\S+) id=(\S+) label="(.*)" frame=(-?\d+),(-?\d+),(\d+),(\d+)(?: disabled)?$')


def parse_listing(ui_text):
    """The bridge's flat listing as [(type, id, label, (x, y, w, h))]; None when unavailable."""
    if not isinstance(ui_text, str) or ui_text.startswith('unavailable'):
        return None
    rows = []
    for line in ui_text.splitlines():
        match = _LISTING_LINE.match(line.strip())
        if match:
            kind, ident, label, *frame = match.groups()
            rows.append((kind, ident, label, tuple(int(n) for n in frame)))
    return rows


def bubble_at(rows, x, y):
    """The message bubble whose listed frame contains the point, else None."""
    for _, ident, _, (fx, fy, fw, fh) in rows or ():
        if re.match(BUBBLE, ident) and fx <= x <= fx + fw and fy <= y <= fy + fh:
            return ident
    return None


def bubble_shift(before, after):
    """Median vertical move of bubbles listed in both; 'disjoint' when none is shared."""
    first = {i: f for _, i, _, f in before if re.match(BUBBLE, i)}
    second = {i: f for _, i, _, f in after if re.match(BUBBLE, i)}
    if not first or not second:
        return None
    shared = sorted(second[i][1] - first[i][1] for i in set(first) & set(second))
    if not shared:
        return 'disjoint'
    return shared[len(shared) // 2]


def oracle_verdict(conditions, before, after):
    """'yes'/'no' from the listings around one attempt, or None when it cannot tell."""
    if conditions is None or after is None:
        return None
    for condition in conditions:
        if 'bubble_shift' in condition:
            if before is None:
                return None
            shift = bubble_shift(before, after)
            if shift is None:
                return None
            if shift != 'disjoint' and abs(shift) < condition['bubble_shift']:
                return 'no'
        elif 'present' in condition or 'absent' in condition:
            pattern = condition.get('present') or condition.get('absent')
            found = any(re.search(pattern, ident) for _, ident, _, _ in after)
            if found != ('present' in condition):
                return 'no'
        elif 'label' in condition:
            ident, pattern = condition['label']
            labels = [label for _, i, label, _ in after if i == ident]
            if not labels or not re.search(pattern, labels[0]):
                return 'no'
    return 'yes'


def _combine(verdicts):
    known = [v for v in verdicts if v is not None]
    if not known:
        return None
    return known[0] if len(set(known)) == 1 else 'mixed'


def evaluate_checks(scenario, actions, listings, findings):
    """Per question: tagged attempts, the oracle's verdict, the model's (evidence-bound) finding.

    `actions` are receipt action records (basis_frame, action, check, x, y); the frame after an action
    is basis_frame + 1. `listings` maps frame index to listing text. A finding other than 'unclear'
    needs a tagged attempt (that started where the question says); 'no' needs two."""
    report = {}
    for key, check in SCENARIOS[scenario]['checks'].items():
        attempts = []
        for action in actions:
            if action.get('check') != key:
                continue
            before = parse_listing(listings.get(action['basis_frame']))
            after = parse_listing(listings.get(action['basis_frame'] + 1))
            attempt = {'basis_frame': action['basis_frame'], 'after_frame': action['basis_frame'] + 1,
                       'action': action['action'],
                       **{k: action[k] for k in ('speed', 'press_s', 'hold_s', 'duration_s') if k in action},
                       'bridge_ok': action.get('bridge_ok'), 'during_frames': action.get('during_frames', [])}
            if 'x' in action and before is not None:
                attempt['start_bubble'] = bubble_at(before, action['x'], action['y'])
            want = check.get('start')
            attempt['start_ok'] = None if want is None or 'start_bubble' not in attempt else \
                (attempt['start_bubble'] is not None) == (want == 'bubble')
            attempt['oracle'] = oracle_verdict(check['oracle'], before, after)
            if action['action'] == 'drag' and before is not None and after is not None:
                attempt['bubble_shift_pt'] = bubble_shift(before, after)
            attempts.append(attempt)
        valid = [a for a in attempts if a['start_ok'] is not False and a.get('bridge_ok') is not False]
        if check.get('aggregate') == 'final':
            oracle = valid[-1]['oracle'] if valid else None
        else:
            oracle = _combine([a['oracle'] for a in valid])
        claimed = findings.get(key, 'unclear')
        effective, note = claimed, None
        if claimed != 'unclear' and not valid:
            effective, note = 'unclear', 'no valid tagged attempt'
        elif claimed == 'no' and len(valid) < 2:
            effective, note = 'unclear', 'a negative finding needs a second attempt'
        report[key] = {
            'question': check['question'], 'model_finding': claimed, 'finding': effective, 'note': note,
            'listing_oracle': oracle,
            'agrees_with_listing': None if oracle is None or effective == 'unclear' else effective == oracle,
            'contract_expectation': check['expect'], 'contract_met': effective == check['expect'],
            'attempts': attempts,
        }
    return report


# ---- Touch-injection arm: idb (HID-level) instead of XCTest-synthesized events --------------------------
# Same scenarios, prompt, typed decisions, frames and checks; only the executor differs. Launch arguments
# must equal the Swift bridge's profiles (test_launch_profiles_match_the_bridge keeps them identical).

ARMS = ('xctest', 'idb')
LAUNCH_ARGS = {
    'legacy': ['--settings-light'],
    'journey': ['--developer', '--settings-light', '--no-motion'],
}
# idb swipes have a duration, not a velocity and no press-then-hold. Per 300 pt of travel; the hold is
# folded into a longer, slower swipe so the finger is still down while during-frames are captured.
IDB_SWIPE_S = {'slow': 1.0, 'default': 0.5, 'fast': 0.15}


def idb_swipe_duration(fields):
    """Seconds for one `idb ui swipe`, from the typed drag fields (bounded by validate_decision)."""
    travel = ((fields['to_x'] - fields['x']) ** 2 + (fields['to_y'] - fields['y']) ** 2) ** 0.5
    base = IDB_SWIPE_S[fields.get('speed', 'default')] * max(travel, 1) / 300
    return round(min(max(base, 0.05) + fields.get('hold_s', 0), 4.0), 2)


def parse_idb_elements(text):
    """`idb ui describe-all` output (a JSON array of AX elements) as a list, or None."""
    if not isinstance(text, str) or '[' not in text:
        return None
    try:
        value = json.loads(text[text.index('['):text.rindex(']') + 1])
    except ValueError:
        return None
    return [e for e in value if isinstance(e, dict)] if isinstance(value, list) else None


def _idb_frame(element):
    frame = element.get('frame') if isinstance(element.get('frame'), dict) else {}
    try:
        return tuple(float(frame[k]) for k in ('x', 'y', 'width', 'height'))
    except (KeyError, TypeError, ValueError):
        return None


def idb_listing(elements, limit=300):
    """The same flat listing format the XCTest bridge prints, so prompts and oracles are shared."""
    lines = []
    for element in elements[:limit]:
        frame = _idb_frame(element)
        if frame is None:
            continue
        ident = element.get('AXUniqueId')
        ident = ident if isinstance(ident, str) and ID_RE.match(ident) else '-'
        label = element.get('AXLabel') if isinstance(element.get('AXLabel'), str) else ''
        kind = element.get('type') if isinstance(element.get('type'), str) and element.get('type') else 'Other'
        lines.append(f'{kind[0].lower() + kind[1:]} id={ident} label="{label.replace(chr(10), " ")[:80]}" '
                     f'frame={",".join(str(int(n)) for n in frame)}'
                     + ('' if element.get('enabled', True) else ' disabled'))
    return '\n'.join(lines)


def idb_center(elements, ident):
    """(x, y, frame) of the first element with this identifier, or None."""
    for element in elements or ():
        frame = _idb_frame(element)
        if element.get('AXUniqueId') == ident and frame is not None:
            return frame[0] + frame[2] / 2, frame[1] + frame[3] / 2, list(frame)
    return None
