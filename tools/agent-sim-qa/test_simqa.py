"""Unit and fake-runner tests for the hosted visual diagnostic.

Run: python3 -m unittest discover -s tools/agent-sim-qa -p 'test_*.py'
The end-to-end cases put fake `xcodebuild`, `xcrun` and `claude` executables on PATH, so the real
controller loop (mailbox, frames, transcript parsing, acknowledgment post-check, receipt) runs on
any machine without a simulator or model.
"""
import json
import os
import plistlib
import subprocess
import sys
import tempfile
import time
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import simqa  # noqa: E402

SHA = 'c23521b43fd6b0dd62e5c8207a45611ac86e4d4f'


def stream(*events):
    return [json.dumps(e) for e in events]


def read_use(use_id, path):
    return {'type': 'assistant', 'message': {'content': [
        {'type': 'tool_use', 'id': use_id, 'name': 'Read', 'input': {'file_path': path}}]}}


def read_result(use_id, image=True, error=False):
    body = [{'type': 'image', 'source': {'type': 'base64', 'media_type': 'image/png', 'data': 'AAAA'}}] \
        if image else '<tool_use_error>File does not exist.</tool_use_error>'
    block = {'type': 'tool_result', 'tool_use_id': use_id, 'content': body}
    if error:
        block['is_error'] = True
    return {'type': 'user', 'message': {'content': [block]}}


def result(text, is_error=False):
    return {'type': 'result', 'subtype': 'success', 'is_error': is_error, 'result': text, 'num_turns': 2,
            'total_cost_usd': 0.0125, 'usage': {'input_tokens': 10, 'cache_read_input_tokens': 5000, 'output_tokens': 90}}


class TranscriptTests(unittest.TestCase):
    def test_successful_image_read_counts(self):
        s = simqa.parse_transcript(stream(
            {'type': 'system', 'subtype': 'init', 'model': 'claude-opus-4-8'},
            read_use('a', '/tmp/f/frame-01.png'), read_result('a'), result('{}')), '/x/frame-01.png')
        self.assertTrue(s['expected_read_ok'])
        self.assertEqual(s['successful_png_reads'], 1)
        self.assertEqual(s['model'], 'claude-opus-4-8')
        self.assertEqual((s['cost_usd'], s['input_tokens'], s['output_tokens']), (0.0125, 5010, 90))
        self.assertEqual(s['tool_uses'], [{'name': 'Read', 'target': 'frame-01.png', 'ok': True, 'answered': True}])

    def test_issued_but_failed_read_does_not_count(self):
        s = simqa.parse_transcript(stream(read_use('a', '/f/frame-01.png'), read_result('a', image=False, error=True),
                                          result('{}')), 'frame-01.png')
        self.assertFalse(s['expected_read_ok'])
        self.assertEqual(s['successful_png_reads'], 0)

    def test_text_only_result_is_not_an_image_read(self):
        s = simqa.parse_transcript(stream(read_use('a', '/f/frame-01.png'), read_result('a', image=False),
                                          result('{}')), 'frame-01.png')
        self.assertFalse(s['expected_read_ok'])

    def test_unanswered_read_and_other_frame_do_not_count(self):
        s = simqa.parse_transcript(stream(read_use('a', '/f/frame-01.png'), read_use('b', '/f/frame-00.png'),
                                          read_result('b'), result('{}')), 'frame-01.png')
        self.assertFalse(s['expected_read_ok'])
        self.assertEqual(s['successful_png_reads'], 1)

    def test_garbage_lines_are_ignored(self):
        s = simqa.parse_transcript(['not json', '[]', '', json.dumps(result('x', True))], None)
        self.assertTrue(s['is_error'])
        self.assertFalse(s['expected_read_ok'])


class DecisionTests(unittest.TestCase):
    screen = (393, 852)

    def ok(self, **extra):
        return simqa.validate_decision({'frame': 2, 'observation': 'Chat page', **extra}, 2, self.screen)

    def test_valid_shapes(self):
        self.ok(action='tap_id', id='catalog.composerAdd', purpose='add_tap')
        self.ok(action='tap_point', x=360, y=790, purpose='add_tap')
        self.ok(action='drag', x=196, y=600, to_x=196, to_y=350, purpose='scroll')
        self.ok(action='finish', outcome='sheet_not_opened')

    def test_rejections(self):
        bad = [
            dict(action='tap_id', id='x y', purpose='add_tap'),
            dict(action='tap_id', id='a', purpose='hack'),
            dict(action='tap_id', id='a'),
            dict(action='tap_point', x=0, y=10, purpose='other'),
            dict(action='tap_point', x=10, y=900, purpose='other'),
            dict(action='tap_point', x=True, y=10, purpose='other'),
            dict(action='tap_point', x=10, y=10, purpose='other', command='rm -rf /'),
            dict(action='finish', outcome='passed'),
            dict(action='finish', outcome='sheet_opened', purpose='add_tap'),
            dict(action='shell', purpose='other'),
        ]
        for value in bad:
            with self.subTest(value=value), self.assertRaises(simqa.DecisionError):
                self.ok(**value)
        with self.assertRaises(simqa.DecisionError):
            simqa.validate_decision({'frame': 1, 'observation': 'x', 'action': 'finish', 'outcome': 'blocked'},
                                    2, self.screen)
        with self.assertRaises(simqa.DecisionError):
            simqa.validate_decision({'frame': 2, 'observation': 'x', 'action': 'tap_id', 'id': 'a',
                                     'purpose': 'other'}, 2, self.screen, finish_only=True)

    def test_extract_json(self):
        self.assertEqual(simqa.extract_json('```json\n{"a": 1}\n```'), {'a': 1})
        self.assertEqual(simqa.extract_json('Sure: {"a": 2}'), {'a': 2})
        for text in (None, 'no json', '{bad'):
            with self.assertRaises(simqa.DecisionError):
                simqa.extract_json(text)


class AcknowledgmentTests(unittest.TestCase):
    def frame(self, i, t, ok=True):
        return {'type': 'frame', 'index': i, 'captured_at': t, 'read_ok': ok}

    def action(self, i, basis, t, done):
        return {'type': 'action', 'index': i, 'basis_frame': basis, 'dispatched_at': t, 'done_at': done}

    def test_acknowledged_sequence(self):
        events = [self.frame(0, 1), self.action(0, 0, 2, 3), self.frame(1, 4),
                  self.action(1, 1, 5, None), self.frame(2, 6), {'type': 'finish', 'basis_frame': 2}]
        self.assertEqual(simqa.check_acknowledged(events), [])

    def test_finish_on_frame_before_action_is_rejected(self):
        events = [self.frame(0, 1), self.action(0, 0, 2, 3), {'type': 'finish', 'basis_frame': 0}]
        self.assertTrue(any('not acknowledged' in p for p in simqa.check_acknowledged(events)))

    def test_decision_on_stale_frame_is_rejected(self):
        events = [self.frame(0, 1), self.action(0, 0, 2, 3), self.frame(1, 4),
                  {'type': 'finish', 'basis_frame': 0}]
        self.assertTrue(any('latest frame' in p for p in simqa.check_acknowledged(events)))

    def test_frame_captured_before_completion_does_not_acknowledge(self):
        events = [self.frame(0, 1), self.action(0, 0, 2, 5), self.frame(1, 4),
                  {'type': 'finish', 'basis_frame': 1}]
        self.assertTrue(any('not acknowledged' in p for p in simqa.check_acknowledged(events)))

    def test_unread_frame_does_not_acknowledge(self):
        events = [self.frame(0, 1), self.action(0, 0, 2, 3), self.frame(1, 4, ok=False),
                  {'type': 'finish', 'basis_frame': 1}]
        problems = simqa.check_acknowledged(events)
        self.assertTrue(any('did not succeed' in p for p in problems))

    def test_missing_finish(self):
        self.assertIn('no finish decision', simqa.check_acknowledged([self.frame(0, 1)]))


class SourceTests(unittest.TestCase):
    def test_pristine(self):
        self.assertEqual(simqa.check_source_state(SHA, SHA, ''), [])
        self.assertTrue(simqa.check_source_state(SHA, SHA, '?? x\n'))
        self.assertTrue(simqa.check_source_state('0' * 40, SHA, ''))
        self.assertTrue(simqa.check_source_state('main', 'main', ''))

    def test_injected(self):
        line = f' M {simqa.INJECTED_FILE}\n'
        self.assertEqual(simqa.check_source_state(SHA, SHA, line, allow_injected=True), [])
        self.assertTrue(simqa.check_source_state(SHA, SHA, line + ' M Sources/X.swift\n', allow_injected=True))
        self.assertTrue(simqa.check_source_state(SHA, SHA, '', allow_injected=True))

    def test_built_sha(self):
        self.assertEqual(simqa.check_built_sha({'RemPlaygroundSourceSHA': SHA}, SHA), [])
        self.assertTrue(simqa.check_built_sha({'RemPlaygroundSourceSHA': 'unversioned'}, SHA))
        self.assertTrue(simqa.check_built_sha({}, SHA))


class BudgetAndRedactionTests(unittest.TestCase):
    def test_budget(self):
        self.assertEqual(simqa.call_budget(0, 1000, 150, 180), 180)
        self.assertEqual(simqa.call_budget(0, 300, 150, 180), 150)
        self.assertEqual(simqa.call_budget(0, 160, 150, 180), 0)

    def test_redact(self):
        text = ('token sk-ant-oat01-abcdefghijklmnop Authorization: Bearer xyz ghp_' + 'a' * 36 +
                ' CLAUDE_CODE_OAUTH_TOKEN=secretvalue ' + 'Q' * 60)
        clean = simqa.redact(text)
        for secret in ('sk-ant-oat01', 'Bearer xyz', 'ghp_', 'secretvalue', 'Q' * 60):
            self.assertNotIn(secret, clean)
        self.assertIn(SHA, simqa.redact(f'built {SHA}'))

    def test_classify(self):
        self.assertEqual(simqa.classify_failure(1, False, {'result': 'Claude AI usage limit reached'}), 'model_quota')
        self.assertEqual(simqa.classify_failure(1, False, {'result': 'Invalid API key · Please run /login'}), 'model_auth')
        self.assertEqual(simqa.classify_failure(0, True, {}), 'model_timeout')
        self.assertIsNone(simqa.classify_failure(0, False, {'result': '{}', 'is_error': False}))

    def test_complete_png(self):
        whole = simqa.PNG_SIGNATURE + b'0' * 32 + simqa.PNG_IEND
        self.assertTrue(simqa.complete_png(whole))
        self.assertFalse(simqa.complete_png(whole[:-1]))
        self.assertFalse(simqa.complete_png(simqa.PNG_SIGNATURE + b'0' * 32))
        self.assertFalse(simqa.complete_png(b'GIF89a' + whole[6:]))
        self.assertFalse(simqa.complete_png(simqa.PNG_SIGNATURE + simqa.PNG_IEND))

    def test_ui_confirms(self):
        self.assertTrue(simqa.ui_confirms_sheet('button id=catalog.addToChat.photos label="Photos" frame=1,2,3,4'))
        self.assertFalse(simqa.ui_confirms_sheet('button id=catalog.composerAdd label="Add" frame=1,2,3,4'))
        self.assertIsNone(simqa.ui_confirms_sheet(None))


B9 = simqa.BUILD9
GESTURES = 'b9-hub-chat-gestures'


def listing(*rows):
    return '\n'.join(f'{t} id={i} label="{l}" frame={x},{y},{w},{h}' for t, i, l, (x, y, w, h) in rows)


class ScenarioTests(unittest.TestCase):
    screen = (393, 852)

    def decide(self, scenario=GESTURES, **value):
        return simqa.validate_decision({'frame': 3, 'observation': 'Chat', **value}, 3, self.screen,
                                       scenario=scenario)

    def test_inputs_are_paired_to_exact_shas(self):
        self.assertEqual(simqa.check_inputs(B9, GESTURES), [])
        self.assertEqual(simqa.check_inputs(SHA, 'catalog-chat-add'), [])
        self.assertTrue(simqa.check_inputs(SHA, GESTURES))
        self.assertTrue(simqa.check_inputs(B9, 'catalog-chat-add'))
        self.assertTrue(simqa.check_inputs(B9, 'free text'))
        for spec in simqa.SCENARIOS.values():
            self.assertTrue(all(simqa.SHA_RE.match(sha) for sha in spec['sources']))
            self.assertIn(spec['launch'], ('legacy', 'journey'))

    def test_workflow_offers_exactly_the_catalog(self):
        workflow = (HERE.parents[1] / '.github/workflows/agent-sim-qa.yml').read_text()
        options = lambda block: {line.strip()[2:] for line in block.splitlines() if line.strip().startswith('- ')}
        scenarios, arms = workflow.split('      scenario:', 1)[1].split('      arm:', 1)
        self.assertEqual(options(scenarios), set(simqa.SCENARIOS))
        self.assertEqual(options(arms.split('permissions:', 1)[0]), set(simqa.ARMS))
        shas = {sha for spec in simqa.SCENARIOS.values() for sha in spec['sources']}
        source_block = workflow.split('source_ref:', 1)[1].split('scenario:', 1)[0]
        self.assertEqual({line.strip()[2:] for line in source_block.splitlines()
                          if line.strip().startswith('- ')}, shas)

    def test_gesture_inputs(self):
        self.decide(action='drag', x=120, y=430, to_x=120, to_y=730, speed='slow', press_s=0.05, hold_s=0.3,
                    purpose='probe', check='bubble_drag_scrolls')
        self.decide(action='long_press', x=120, y=430, duration_s=1.0, purpose='probe',
                    check='long_press_opens_actions')
        self.decide(action='long_press', x=120, y=430, purpose='probe')
        bad = [
            dict(action='drag', x=1, y=2, to_x=3, to_y=4, speed='warp', purpose='probe'),
            dict(action='drag', x=1, y=2, to_x=3, to_y=4, hold_s=5, purpose='probe'),
            dict(action='drag', x=1, y=2, to_x=3, to_y=4, press_s=0, purpose='probe'),
            dict(action='drag', x=1, y=2, to_x=3, to_y=4, hold_s=True, purpose='probe'),
            dict(action='long_press', x=1, y=2, duration_s=0.1, purpose='probe'),
            dict(action='long_press', x=1, y=2, duration_s=1, speed='slow', purpose='probe'),
            dict(action='tap_point', x=1, y=2, purpose='probe', check='not_a_question'),
            dict(action='tap_point', x=1, y=2, hold_s=1, purpose='probe'),
            dict(action='finish', outcome='sheet_opened', findings={}),
            dict(action='finish', outcome='completed'),
            dict(action='finish', outcome='completed', findings={'bubble_drag_scrolls': 'yes'}),
        ]
        for value in bad:
            with self.subTest(value=value), self.assertRaises(simqa.DecisionError):
                self.decide(**value)
        answers = {k: 'unclear' for k in simqa.SCENARIOS[GESTURES]['checks']}
        self.decide(action='finish', outcome='blocked', findings=answers)
        with self.assertRaises(simqa.DecisionError):
            self.decide(action='finish', outcome='completed', findings={**answers, 'bubble_drag_scrolls': 'maybe'})
        with self.assertRaises(simqa.DecisionError):  # legacy scenarios take no check tags
            self.decide('catalog-chat-add', action='tap_point', x=1, y=2, purpose='other', check='x')

    def test_listing_oracle(self):
        before = simqa.parse_listing(listing(('other', 'message.a1.bubble', 'Hi "there"', (20, 400, 200, 60)),
                                             ('other', 'message.u1.bubble', 'Yo', (180, 300, 200, 40)),
                                             ('button', 'chat.header.identity', 'Health, idle', (0, 50, 393, 44))))
        moved = simqa.parse_listing(listing(('other', 'message.a1.bubble', 'Hi', (20, 700, 200, 60)),
                                            ('other', 'message.u1.bubble', 'Yo', (180, 600, 200, 40))))
        self.assertEqual(before[0][2], 'Hi "there"')
        self.assertEqual(simqa.bubble_at(before, 100, 430), 'message.a1.bubble')
        self.assertIsNone(simqa.bubble_at(before, 8, 430))
        self.assertEqual(simqa.bubble_shift(before, moved), 300)
        self.assertEqual(simqa.bubble_shift(before, simqa.parse_listing(listing(
            ('other', 'message.h1.bubble', '', (0, 0, 1, 1))))), 'disjoint')
        self.assertEqual(simqa.oracle_verdict([{'bubble_shift': 20}], before, moved), 'yes')
        self.assertEqual(simqa.oracle_verdict([{'bubble_shift': 20}], before, before), 'no')
        self.assertEqual(simqa.oracle_verdict([{'label': ['chat.header.identity', '^Health']}], None, before), 'yes')
        self.assertEqual(simqa.oracle_verdict([{'present': r'^chat\.jumpToLatest$'}], None, before), 'no')
        self.assertEqual(simqa.oracle_verdict([{'absent': r'^chat\.jumpToLatest$'}], None, before), 'yes')
        self.assertIsNone(simqa.oracle_verdict(None, before, moved))
        self.assertIsNone(simqa.oracle_verdict([{'bubble_shift': 20}], before, None))
        self.assertIsNone(simqa.parse_listing('unavailable: stalled'))

    def test_findings_are_bound_to_attempts(self):
        at = lambda y: listing(('other', 'message.a1.bubble', '', (20, y, 200, 60)))
        listings = {0: at(400), 1: at(400), 2: at(400), 3: at(700)}
        drag = lambda frame, x, check: {'basis_frame': frame, 'action': 'drag', 'x': x, 'y': 430, 'bridge_ok': True,
                                        'check': check}
        actions = [drag(0, 100, 'bubble_drag_scrolls'), drag(1, 100, 'bubble_drag_scrolls'),
                   drag(2, 8, 'margin_drag_scrolls')]
        findings = {'bubble_drag_scrolls': 'no', 'margin_drag_scrolls': 'yes', 'swipe_left_reveals_times': 'yes',
                    'long_press_opens_actions': 'unclear'}
        report = simqa.evaluate_checks(GESTURES, actions, listings, findings)
        bubble, margin = report['bubble_drag_scrolls'], report['margin_drag_scrolls']
        self.assertEqual((bubble['finding'], bubble['listing_oracle'], bubble['agrees_with_listing']), ('no', 'no', True))
        self.assertFalse(bubble['contract_met'])
        self.assertEqual([a['start_bubble'] for a in bubble['attempts']], ['message.a1.bubble'] * 2)
        self.assertEqual((margin['finding'], margin['listing_oracle']), ('yes', 'yes'))
        self.assertIsNone(margin['attempts'][0]['start_bubble'])
        self.assertEqual(report['swipe_left_reveals_times']['finding'], 'unclear')  # no attempt
        self.assertEqual(report['swipe_left_reveals_times']['note'], 'no valid tagged attempt')
        # One failed attempt is not a reproduced bug; a drag from the wrong place does not count.
        one = simqa.evaluate_checks(GESTURES, actions[:1], listings, findings)['bubble_drag_scrolls']
        self.assertEqual((one['finding'], one['note']), ('unclear', 'a negative finding needs a second attempt'))
        wrong = [drag(0, 8, 'bubble_drag_scrolls'), drag(1, 8, 'bubble_drag_scrolls')]
        self.assertEqual(simqa.evaluate_checks(GESTURES, wrong, listings, findings)['bubble_drag_scrolls']['finding'],
                         'unclear')


class IdbHelperTests(unittest.TestCase):
    def test_launch_profiles_match_the_bridge(self):
        swift = (HERE / 'AgentVisualBridge.swift').read_text()
        for args in simqa.LAUNCH_ARGS.values():
            self.assertIn(json.dumps(args), swift)

    def test_listing_and_center(self):
        elements = simqa.parse_idb_elements('noise\n' + json.dumps([
            {'type': 'Application', 'AXUniqueId': None, 'AXLabel': 'Rem', 'frame': {'x': 0, 'y': 0, 'width': 393, 'height': 852}},
            {'type': 'Button', 'AXUniqueId': 'chat.header.menu', 'AXLabel': 'Open\nsidebar',
             'frame': {'x': 10, 'y': 50, 'width': 44, 'height': 44}},
            {'type': 'Other', 'AXUniqueId': 'bad id;rm', 'AXLabel': 'x', 'frame': {'x': 1, 'y': 2, 'width': 3, 'height': 4},
             'enabled': False},
            {'type': 'Other', 'AXUniqueId': 'noframe'}]))
        listing = simqa.idb_listing(elements)
        self.assertEqual(listing.splitlines(), [
            'application id=- label="Rem" frame=0,0,393,852',
            'button id=chat.header.menu label="Open sidebar" frame=10,50,44,44',
            'other id=- label="x" frame=1,2,3,4 disabled'])
        self.assertEqual(simqa.parse_listing(listing)[1][1], 'chat.header.menu')
        self.assertEqual(simqa.idb_center(elements, 'chat.header.menu')[:2], (32.0, 72.0))
        self.assertIsNone(simqa.idb_center(elements, 'noframe'))
        for bad in (None, 'no json', '{"a": 1}', '[1, 2'):
            self.assertIsNone(simqa.parse_idb_elements(bad))

    def test_swipe_duration(self):
        drag = {'x': 100, 'y': 100, 'to_x': 100, 'to_y': 400}
        self.assertEqual(simqa.idb_swipe_duration(drag), 0.5)
        self.assertEqual(simqa.idb_swipe_duration({**drag, 'speed': 'slow', 'hold_s': 0.3}), 1.3)
        self.assertEqual(simqa.idb_swipe_duration({**drag, 'speed': 'fast'}), 0.15)
        self.assertEqual(simqa.idb_swipe_duration({**drag, 'speed': 'slow', 'hold_s': 2, 'to_y': 851}), 4.0)  # capped


FAKE_IDB = r'''#!/usr/bin/env python3
import json, os, sys, time
from pathlib import Path
here = Path(__file__).parent
mode = (here / 'mode').read_text().strip()
assert not any('TOKEN' in k for k in os.environ), 'credential leaked to idb'
state_file = here / 'idb-state.json'
state = json.loads(state_file.read_text()) if state_file.exists() else {'offset': 0, 'actions': False}
args = sys.argv[1:]
if args[0] == 'connect':
    sys.exit(0)
assert args[2:4] == ['--udid', 'UDID'], args
launch = json.loads((here / 'launched.json').read_text())
assert launch == ['--developer', '--settings-light', '--no-motion'], launch
on_bubble = lambda x, y: 20 <= x <= 220 and 400 + state['offset'] <= y <= 460 + state['offset']
if args[:2] == ['ui', 'describe-all']:
    elements = [{'type': 'Application', 'AXLabel': 'Playground', 'frame': {'x': 0, 'y': 0, 'width': 393, 'height': 852}},
                {'type': 'Other', 'AXUniqueId': 'message.a1.bubble', 'AXLabel': 'Hello',
                 'frame': {'x': 20, 'y': 400 + state['offset'], 'width': 200, 'height': 60}}]
    if state['actions']:
        elements.append({'type': 'Button', 'AXUniqueId': 'message.a1.actions.copy', 'AXLabel': 'Copy',
                         'frame': {'x': 0, 'y': 600, 'width': 393, 'height': 60}})
    print(json.dumps(elements))
elif args[:2] == ['ui', 'tap']:
    rest = args[4:]
    duration = float(rest[rest.index('--duration') + 1]) if '--duration' in rest else 0
    x, y = int(rest[-2]), int(rest[-1])
    if duration >= 0.5:
        state['actions'] = on_bubble(x, y)
elif args[:2] == ['ui', 'swipe']:
    rest = args[4:]
    duration = float(rest[rest.index('--duration') + 1])
    x, y, tx, ty = (int(v) for v in rest[-4:])
    time.sleep(min(duration, 2))
    if abs(ty - y) > abs(tx - x) and not (mode.startswith('stuck') and on_bubble(x, y)):
        state['offset'] += (ty - y) // 10
else:
    sys.exit(64)
state_file.write_text(json.dumps(state))
'''

FAKE_COMPANION = r'''#!/usr/bin/env python3
import os, sys, time
assert not any('TOKEN' in k for k in os.environ), 'credential leaked to idb_companion'
assert sys.argv[1:] == ['--udid', 'UDID', '--grpc-port', '10882'], sys.argv
time.sleep(600)
'''


FAKE_XCODEBUILD = r'''#!/usr/bin/env python3
import json, os, sys, time
from pathlib import Path
mode = (Path(__file__).parent / 'mode').read_text().strip()
box = Path(os.environ['TEST_RUNNER_AGENT_BRIDGE_DIR'])
assert not any('TOKEN' in k for k in os.environ), 'credential leaked to the build/test process'
profile = os.environ['TEST_RUNNER_AGENT_LAUNCH_PROFILE']
(box / 'ready.json').write_text(json.dumps({'ok': True, 'width': 393, 'height': 852, 'profile': profile}))
sheet = False
offset = 0
actions_open = False
def on_bubble(x, y):
    return 20 <= x <= 220 and 400 + offset <= y <= 460 + offset
seq = 0
while True:
    cmd = box / f'cmd-{seq:03d}.json'
    if not cmd.exists():
        time.sleep(0.05); continue
    c = json.loads(cmd.read_text())
    (box / f'ack-{seq:03d}.json').write_text('{}')
    if c['kind'] == 'end':
        (box / f'res-{seq:03d}.json').write_text(json.dumps({'ok': True})); break
    if c['kind'] == 'tap_id' and c['id'] == 'catalog.composerAdd' and mode == 'stall':
        time.sleep(5); break
    res = {'seq': seq, 'ok': True}
    if c['kind'] == 'observe' and profile == 'journey':
        ui = f'other id=message.a1.bubble label="Hello" frame=20,{400 + offset},200,60'
        ui += f'\nother id=message.u1.bubble label="Hi" frame=180,{300 + offset},200,40'
        if actions_open: ui += '\nbutton id=message.a1.actions.copy label="Copy" frame=0,600,393,60'
        res['ui'] = ui
    elif c['kind'] == 'observe':
        ui = 'button id=catalog.composerAdd label="Add" frame=340,800,44,44'
        if sheet: ui += '\nbutton id=catalog.addToChat.photos label="Photos" frame=0,500,393,44'
        res['ui'] = ui
    if c['kind'] == 'drag':
        time.sleep(c.get('hold_s', 0))
        dy = c['to_y'] - c['y']
        if abs(dy) > abs(c['to_x'] - c['x']) and not (mode.startswith('stuck') and on_bubble(c['x'], c['y'])):
            offset += int(dy // 10)
    if c['kind'] == 'long_press':
        actions_open = on_bubble(c['x'], c['y'])
    if c['kind'] == 'tap_id' and c['id'] == 'catalog.composerAdd':
        sheet = True
    time.sleep(0.05)
    (box / f'res-{seq:03d}.json').write_text(json.dumps(res))
    seq += 1
'''

FAKE_XCRUN = r'''#!/usr/bin/env python3
import json, sys
from pathlib import Path
mode = (Path(__file__).parent / 'mode').read_text().strip()
if sys.argv[1:3] == ['simctl', 'install']:
    assert sys.argv[3] == 'UDID' and sys.argv[4].endswith('.app'), sys.argv
    sys.exit(1 if mode == 'installfail' else 0)
if sys.argv[1:4] == ['simctl', 'launch', '--terminate-running-process']:
    assert sys.argv[4:6] == ['UDID', 'site.userem.playground'], sys.argv
    (Path(__file__).parent / 'launched.json').write_text(json.dumps(sys.argv[6:]))
    sys.exit(0)
assert sys.argv[1:4] == ['simctl', 'io', 'UDID'] and sys.argv[4] == 'screenshot', sys.argv
count = Path(__file__).parent / 'shots'
n = int(count.read_text()) if count.exists() else 0
count.write_text(str(n + 1))
whole = b'\x89PNG\r\n\x1a\n' + b'0' * 32 + b'\x00\x00\x00\x00IEND\xaeB`\x82'
flaky = mode == 'shotflaky' and n == 2  # the third capture fails once, then the retry succeeds
open(sys.argv[5], 'wb').write(whole[:20] if mode == 'shottruncated' or flaky else whole)
if mode in ('shotexit', 'shottruncated') or flaky:
    sys.exit(1)
'''

FAKE_CLAUDE = r'''#!/usr/bin/env python3
import json, os, re, sys
from pathlib import Path
mode = (Path(__file__).parent / 'mode').read_text().strip()
assert 'GITHUB_TOKEN' not in os.environ and os.environ.get('CLAUDE_CODE_OAUTH_TOKEN') == 'fake-oauth'
assert sys.argv[sys.argv.index('--tools') + 1] == 'Read'
prompt = sys.stdin.read()
path = re.search(r'Screenshot: `([^`]+)` \(frame (\d+)\)', prompt)
png, frame = path.group(1), int(path.group(2))
finish_only = 'Only `finish` is accepted now' in prompt
emit = lambda e: print(json.dumps(e), flush=True)
emit({'type': 'system', 'subtype': 'init', 'model': 'claude-opus-4-8', 'apiKeySource': 'none'})
emit({'type': 'assistant', 'message': {'content': [{'type': 'tool_use', 'id': 't1', 'name': 'Read', 'input': {'file_path': png}}]}})
if mode == 'readfail':
    emit({'type': 'user', 'message': {'content': [{'type': 'tool_result', 'tool_use_id': 't1', 'is_error': True, 'content': 'error'}]}})
else:
    emit({'type': 'user', 'message': {'content': [{'type': 'tool_result', 'tool_use_id': 't1', 'content': [{'type': 'image', 'source': {'data': 'AAAA'}}]}]}})
if mode == 'quota':
    emit({'type': 'result', 'is_error': True, 'result': 'Claude AI usage limit reached'}); sys.exit(1)
bubble = re.search(r'id=message.a1.bubble label="[^"]*" frame=(\d+),(\d+(?:\.\d+)?),(\d+),(\d+)', prompt)
if bubble:
    bx, by = 120, float(bubble.group(2)) + 30
    plan = [
        dict(action='drag', x=bx, y=by, to_x=bx, to_y=by + 300, speed='slow', press_s=0.05, hold_s=0.3,
             purpose='probe', check='bubble_drag_scrolls'),
        dict(action='drag', x=bx, y=by, to_x=bx, to_y=by + 300, speed='default', press_s=0.05, hold_s=0,
             purpose='probe', check='bubble_drag_scrolls'),
        dict(action='drag', x=8, y=by, to_x=8, to_y=by + 300, purpose='probe', check='margin_drag_scrolls'),
        dict(action='drag', x=bx, y=by, to_x=bx - 100, to_y=by, hold_s=1.5, purpose='probe',
             check='swipe_left_reveals_times'),
        dict(action='long_press', x=bx, y=by, duration_s=1.0, purpose='probe', check='long_press_opens_actions'),
    ]
    stuck = 'yes' if mode in ('ok', 'stucklie') else 'no'
    d = plan[frame] if frame < len(plan) else {'action': 'finish', 'outcome': 'completed', 'findings': {
        'bubble_drag_scrolls': stuck, 'margin_drag_scrolls': 'yes', 'swipe_left_reveals_times': 'yes',
        'long_press_opens_actions': 'yes'}}
    if frame == 4:
        assert 'during-' in prompt, 'held drag frames are offered to the model'
elif finish_only:
    d = {'action': 'finish', 'outcome': 'sheet_not_opened'}
elif frame == 0:
    d = {'action': 'tap_id', 'id': 'openComponents', 'purpose': 'navigate'}
elif frame == 1:
    d = {'action': 'tap_id', 'id': 'catalog.composerAdd', 'purpose': 'add_tap'}
else:
    d = {'action': 'finish', 'outcome': 'sheet_opened'}
d.update(frame=frame, observation='fake observation sk-ant-oat01-SECRETSECRET')
emit({'type': 'result', 'is_error': False, 'num_turns': 2, 'result': json.dumps(d), 'total_cost_usd': 0.01,
      'usage': {'input_tokens': 100, 'output_tokens': 20}})
'''


class FakeRunnerTests(unittest.TestCase):
    def run_controller(self, mode, deadline_s=900, stall_window=1.0, sha=SHA, scenario='catalog-chat-add',
                       arm='xctest'):
        temp = Path(tempfile.mkdtemp(prefix='simqa-test-'))
        bin_dir = temp / 'bin'
        bin_dir.mkdir()
        for name, body in (('xcodebuild', FAKE_XCODEBUILD), ('xcrun', FAKE_XCRUN), ('claude', FAKE_CLAUDE),
                           ('idb', FAKE_IDB), ('idb_companion', FAKE_COMPANION)):
            (bin_dir / name).write_text(body)
            (bin_dir / name).chmod(0o755)
        (bin_dir / 'mode').write_text(mode)
        out = temp / 'out'
        out.mkdir()
        env = {'PATH': f'{bin_dir}:{os.environ["PATH"]}', 'HOME': str(temp), 'CLAUDE_CODE_OAUTH_TOKEN': 'fake-oauth',
               'GITHUB_TOKEN': 'must-not-pass'}
        args = [sys.executable, str(HERE / 'controller.py'), 'run', '--src', str(temp), '--sha', sha,
                '--udid', 'UDID', '--derived-data', str(temp / 'dd'), '--out', str(out), '--raw', str(temp / 'raw'),
                '--mailbox', str(temp / 'box'), '--prompt', str(HERE / 'prompt.md'),
                '--deadline', str(time.time() + deadline_s), '--scenario', scenario,
                '--stall-window', str(stall_window), '--arm', arm]
        if arm == 'idb':
            app = temp / 'RemSettingsPlayground.app'
            app.mkdir()
            (app / 'Info.plist').write_bytes(plistlib.dumps({'CFBundleIdentifier': 'site.userem.playground'}))
            args += ['--app', str(app)]
        process = subprocess.run(args, env=env, capture_output=True, text=True, timeout=120)
        receipt = json.loads((out / 'receipt.json').read_text())
        return process, receipt, out, temp

    def test_add_opens_sheet(self):
        process, receipt, out, _ = self.run_controller('ok')
        self.assertEqual(process.returncode, 0, process.stdout + process.stderr)
        self.assertEqual(receipt['status'], 'sheet_opened')
        self.assertEqual(receipt['successful_png_reads'], 3)
        self.assertEqual((receipt['model_cost_usd'], receipt['model_tokens']['input_tokens']), (0.03, 300))
        self.assertTrue(receipt['ui_listing_confirms_sheet'])
        self.assertEqual(receipt['violations'], [])
        self.assertEqual([a['id'] for a in receipt['chosen_add_taps']], ['catalog.composerAdd'])
        self.assertTrue((out / 'frames' / 'frame-02.png').exists())
        self.assertTrue((out / 'frames' / 'frame-02.ui.txt').exists())
        uploaded = ''.join(p.read_text(errors='replace') for p in out.rglob('*') if p.is_file())
        self.assertNotIn('sk-ant-oat01', uploaded)
        self.assertNotIn('fake-oauth', uploaded)
        self.assertNotIn('"data"', uploaded)
        self.assertFalse(list(out.rglob('*.jsonl')), 'raw transcripts must stay out of the upload folder')

    def test_complete_frame_kept_when_simctl_exits_non_zero(self):
        process, receipt, out, _ = self.run_controller('shotexit')
        self.assertEqual(process.returncode, 0, process.stdout + process.stderr)
        self.assertEqual(receipt['status'], 'sheet_opened')
        self.assertTrue(receipt['screenshot_notes'])
        self.assertTrue(all(n['simctl'] == 'exit 1' and n['kept'] for n in receipt['screenshot_notes']))

    def test_truncated_frame_still_fails(self):
        process, receipt, out, _ = self.run_controller('shottruncated')
        self.assertEqual(receipt['status'], 'screenshot_failed')
        self.assertEqual(receipt['screenshot_notes'], [
            {'png': 'frame-00.png', 'attempt': 1, 'simctl': 'exit 1', 'kept': False},
            {'png': 'frame-00.png', 'attempt': 2, 'simctl': 'exit 1', 'kept': False}])

    def test_one_incomplete_frame_is_retried(self):
        process, receipt, out, _ = self.run_controller('shotflaky')
        self.assertEqual(process.returncode, 0, process.stdout + process.stderr)
        self.assertEqual(receipt['status'], 'sheet_opened')
        self.assertEqual([(n['attempt'], n['kept']) for n in receipt['screenshot_notes']], [(1, False), (2, True)])

    def test_failed_read_blocks_decision(self):
        process, receipt, out, _ = self.run_controller('readfail')
        self.assertEqual(process.returncode, 1)
        self.assertEqual(receipt['status'], 'png_read_failed')
        self.assertEqual(receipt['successful_png_reads'], 0)
        self.assertTrue((out / 'frames' / 'frame-00.png').exists())

    def test_quota_is_reported_not_retried(self):
        process, receipt, _, _ = self.run_controller('quota')
        self.assertEqual(receipt['status'], 'model_unavailable')
        self.assertEqual(receipt['error_category'], 'model_quota')
        self.assertEqual(receipt['model_calls'], 1)

    def test_stalled_add_tap_is_reported_with_after_frame(self):
        process, receipt, out, _ = self.run_controller('stall')
        self.assertEqual(process.returncode, 1)
        self.assertEqual(receipt['status'], 'input_stalled')
        self.assertEqual(receipt['observed_outcome'], 'sheet_not_opened')
        self.assertEqual(len(receipt['stalls']), 1)
        self.assertFalse(receipt['stalls'][0]['recovered'])
        self.assertEqual(receipt['violations'], [])
        self.assertTrue(list((out / 'frames').glob('stall-*.png')))
        self.assertTrue(receipt['actions'][-1]['stalled'])

    def test_deadline_stops_cleanly_with_partial_record(self):
        process, receipt, out, _ = self.run_controller('ok', deadline_s=165)
        self.assertEqual(receipt['status'], 'deadline')
        self.assertTrue((out / 'summary.md').exists())
        self.assertTrue((out / 'frames' / 'frame-00.png').exists())

    def test_gesture_checks_pass_when_the_transcript_scrolls(self):
        process, receipt, out, _ = self.run_controller('ok', sha=B9, scenario=GESTURES)
        self.assertEqual(process.returncode, 0, process.stdout + process.stderr + json.dumps(receipt)[:2000])
        self.assertEqual(receipt['status'], 'completed')
        self.assertEqual(receipt['launch_profile'], 'journey')
        self.assertTrue(receipt['contract_met'])
        self.assertEqual(receipt['listing_disagreements'], [])
        checks = receipt['checks']
        self.assertEqual(checks['bubble_drag_scrolls']['listing_oracle'], 'yes')
        self.assertEqual(checks['long_press_opens_actions']['listing_oracle'], 'yes')
        self.assertIsNone(checks['swipe_left_reveals_times']['listing_oracle'])
        swipe = checks['swipe_left_reveals_times']['attempts'][0]
        self.assertTrue(swipe['during_frames'])
        self.assertTrue(all((out / 'frames' / name).exists() for name in swipe['during_frames']))
        self.assertEqual(checks['bubble_drag_scrolls']['attempts'][0]['speed'], 'slow')
        self.assertIn('bubble_drag_scrolls', (out / 'summary.md').read_text())

    def test_reproduced_drag_failure_is_reported_and_fails_the_job(self):
        process, receipt, _, _ = self.run_controller('stuck', sha=B9, scenario=GESTURES)
        self.assertEqual(process.returncode, 1)
        self.assertEqual(receipt['status'], 'completed')
        bubble = receipt['checks']['bubble_drag_scrolls']
        self.assertEqual((bubble['finding'], bubble['listing_oracle'], bubble['agrees_with_listing']), ('no', 'no', True))
        self.assertFalse(receipt['contract_met'])
        self.assertEqual(receipt['checks']['margin_drag_scrolls']['finding'], 'yes')

    def test_model_claim_contradicted_by_listing_is_flagged(self):
        process, receipt, _, _ = self.run_controller('stucklie', sha=B9, scenario=GESTURES)
        self.assertEqual(process.returncode, 1)
        self.assertEqual(receipt['listing_disagreements'], ['bubble_drag_scrolls'])

    def test_scenario_on_wrong_sha_is_rejected_before_the_bridge(self):
        process, receipt, _, _ = self.run_controller('ok', sha=SHA, scenario=GESTURES)
        self.assertEqual(process.returncode, 1)
        self.assertEqual(receipt['status'], 'input_rejected')
        self.assertEqual(receipt['model_calls'], 0)

    def test_idb_arm_drives_the_same_gesture_checks(self):
        process, receipt, out, temp = self.run_controller('ok', sha=B9, scenario=GESTURES, arm='idb')
        self.assertEqual(process.returncode, 0, process.stdout + process.stderr + json.dumps(receipt)[:2000])
        self.assertEqual((receipt['arm'], receipt['status']), ('idb', 'completed'))
        self.assertEqual(receipt['idb']['launch_args'], ['--developer', '--settings-light', '--no-motion'])
        self.assertTrue(receipt['contract_met'])
        checks = receipt['checks']
        self.assertEqual(checks['bubble_drag_scrolls']['listing_oracle'], 'yes')
        self.assertEqual(checks['long_press_opens_actions']['listing_oracle'], 'yes')
        self.assertTrue(checks['swipe_left_reveals_times']['attempts'][0]['during_frames'])
        self.assertEqual(receipt['actions'][0]['action'], 'drag')
        self.assertFalse((temp / 'box' / 'cmd-000.json').exists(), 'the idb arm does not use the XCTest mailbox')

    def test_idb_arm_reports_a_reproduced_failure(self):
        process, receipt, _, _ = self.run_controller('stuck', sha=B9, scenario=GESTURES, arm='idb')
        self.assertEqual(process.returncode, 1)
        self.assertEqual(receipt['checks']['bubble_drag_scrolls']['finding'], 'no')

    def test_idb_install_failure_stops_before_the_model(self):
        process, receipt, _, _ = self.run_controller('installfail', sha=B9, scenario=GESTURES, arm='idb')
        self.assertEqual(receipt['status'], 'bridge_not_ready')
        self.assertEqual(receipt['idb']['install'], 1)
        self.assertEqual(receipt['model_calls'], 0)


if __name__ == '__main__':
    unittest.main()
