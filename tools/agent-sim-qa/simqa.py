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
    'finish': {'outcome'},
}
PURPOSES = {'navigate', 'model_menu', 'add_tap', 'scroll', 'dismiss', 'other'}
OUTCOMES = {'sheet_opened', 'sheet_not_opened', 'blocked'}
SHEET_IDS = ('catalog.addToChat.photos', 'catalog.addToChat.files')
INJECTED_FILE = 'tools/playground-ios/UITests/PlaygroundNavigationUITests.swift'


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


def validate_decision(value, frame, screen, finish_only=False):
    """Accept exactly one typed action for `frame`. Coordinates are points inside `screen`."""
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
    allowed = required | ({'purpose'} if kind != 'finish' else set())
    if kind != 'finish':
        required = required | {'purpose'}
    if not required <= set(value) or not set(value) <= allowed:
        raise DecisionError('unexpected or missing decision fields')
    observation = value['observation']
    if not isinstance(observation, str) or not observation.strip() or len(observation) > 600:
        raise DecisionError('observation must be 1..600 characters')
    if kind == 'finish':
        if value['outcome'] not in OUTCOMES:
            raise DecisionError('unknown outcome')
        return value
    if value['purpose'] not in PURPOSES:
        raise DecisionError('unknown purpose')
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
