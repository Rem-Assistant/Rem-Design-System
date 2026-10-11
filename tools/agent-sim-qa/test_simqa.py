"""Unit and fake-runner tests for the hosted visual diagnostic.

Run: python3 -m unittest discover -s tools/agent-sim-qa -p 'test_*.py'
The end-to-end cases put fake `xcodebuild`, `xcrun` and `claude` executables on PATH, so the real
controller loop (mailbox, frames, transcript parsing, acknowledgment post-check, receipt) runs on
any machine without a simulator or model.
"""
import json
import os
import subprocess
import sys
import tempfile
import time
import unittest
import zlib
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
    return {'type': 'result', 'subtype': 'success', 'is_error': is_error, 'result': text, 'num_turns': 2}


def chunk(kind, body):
    return len(body).to_bytes(4, 'big') + kind + body + zlib.crc32(kind + body).to_bytes(4, 'big')


def png(width, height):
    ihdr = width.to_bytes(4, 'big') + height.to_bytes(4, 'big') + bytes([8, 6, 0, 0, 0])
    return simqa.PNG_SIGNATURE + chunk(b'IHDR', ihdr) + chunk(b'IDAT', zlib.compress(b'x')) + chunk(b'IEND', b'')


class TranscriptTests(unittest.TestCase):
    def test_successful_image_read_counts(self):
        s = simqa.parse_transcript(stream(
            {'type': 'system', 'subtype': 'init', 'model': 'claude-opus-4-8'},
            read_use('a', '/tmp/f/frame-01.png'), read_result('a'), result('{}')), '/x/frame-01.png')
        self.assertTrue(s['expected_read_ok'])
        self.assertEqual(s['successful_png_reads'], 1)
        self.assertEqual(s['model'], 'claude-opus-4-8')
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

    def test_inspect_png(self):
        whole = png(1179, 2556)
        self.assertEqual(simqa.inspect_png(whole), ((1179, 2556), None))
        self.assertEqual(simqa.inspect_png(whole[:-1])[1], 'truncated chunk')
        self.assertEqual(simqa.inspect_png(whole + b'x')[1], 'data after IEND')
        self.assertEqual(simqa.inspect_png(b'GIF89a' + whole[6:])[1], 'no PNG signature')
        # Arbitrary interior bytes between a signature and a valid IEND are not a PNG.
        self.assertIsNone(simqa.inspect_png(simqa.PNG_SIGNATURE + b'0' * 32 + simqa.PNG_IEND)[0])
        corrupted = bytearray(whole); corrupted[40] ^= 0xFF
        self.assertIn('bad CRC', simqa.inspect_png(bytes(corrupted))[1])
        no_idat = simqa.PNG_SIGNATURE + chunk(b'IHDR', (1179).to_bytes(4, 'big') + (2556).to_bytes(4, 'big') + bytes(5)) \
            + chunk(b'IEND', b'')
        self.assertEqual(simqa.inspect_png(no_idat)[1], 'no image data')

    def test_capture_problems(self):
        self.assertEqual(simqa.capture_problems((1179, 2556), (393, 852), 100.0, 100.0), [])
        self.assertEqual(simqa.capture_problems((786, 1704), (393, 852), 100.0, 100.0), [])
        self.assertEqual(simqa.capture_problems((1179, 2556), (393, 852), 50.0, 100.0), ['not written by this capture'])
        self.assertEqual(len(simqa.capture_problems((1000, 2556), (393, 852), 100.0, 100.0)), 1)

    def test_ui_confirms(self):
        both = ('button id=catalog.addToChat.photos label="Photos" frame=1,2,3,4\n'
                'button id=catalog.addToChat.files label="Files" frame=1,2,3,4')
        self.assertTrue(simqa.ui_confirms_sheet(both))
        self.assertFalse(simqa.ui_confirms_sheet('button id=catalog.addToChat.photos label="Photos" frame=1,2,3,4'))
        self.assertFalse(simqa.ui_confirms_sheet('button id=catalog.composerAdd label="Add" frame=1,2,3,4'))
        self.assertIsNone(simqa.ui_confirms_sheet(None))


FAKE_XCODEBUILD = r'''#!/usr/bin/env python3
import json, os, sys, time
from pathlib import Path
mode = (Path(__file__).parent / 'mode').read_text().strip()
box = Path(os.environ['TEST_RUNNER_AGENT_BRIDGE_DIR'])
assert not any('TOKEN' in k for k in os.environ), 'credential leaked to the build/test process'
(box / 'ready.json').write_text(json.dumps({'ok': True, 'width': 393, 'height': 852}))
sheet = False
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
    if c['kind'] == 'observe':
        ui = 'button id=catalog.composerAdd label="Add" frame=340,800,44,44'
        if sheet: ui += ('\nbutton id=catalog.addToChat.photos label="Photos" frame=0,500,393,44'
                         '\nbutton id=catalog.addToChat.files label="Files" frame=0,544,393,44')
        res['ui'] = ui
    if c['kind'] == 'tap_id' and c['id'] == 'catalog.composerAdd':
        sheet = True
    time.sleep(0.05)
    (box / f'res-{seq:03d}.json').write_text(json.dumps(res))
    seq += 1
'''

FAKE_XCRUN = r'''#!/usr/bin/env python3
import sys, zlib
from pathlib import Path
assert sys.argv[1:4] == ['simctl', 'io', 'UDID'] and sys.argv[4] == 'screenshot', sys.argv
mode = (Path(__file__).parent / 'mode').read_text().strip()
def chunk(kind, body):
    return len(body).to_bytes(4, 'big') + kind + body + zlib.crc32(kind + body).to_bytes(4, 'big')
width, height = (1000, 2556) if mode == 'shotwrongsize' else (1179, 2556)
ihdr = width.to_bytes(4, 'big') + height.to_bytes(4, 'big') + bytes([8, 6, 0, 0, 0])
whole = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', zlib.compress(b'x')) + chunk(b'IEND', b'')
if mode == 'shotnone':
    sys.exit(1)
open(sys.argv[5], 'wb').write(whole[:30] if mode == 'shottruncated' else whole)
if mode in ('shotexit', 'shottruncated', 'shotwrongsize'):
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
if finish_only:
    d = {'action': 'finish', 'outcome': 'sheet_not_opened'}
elif frame == 0:
    d = {'action': 'tap_id', 'id': 'openComponents', 'purpose': 'navigate'}
elif frame == 1:
    d = {'action': 'tap_id', 'id': 'catalog.composerAdd', 'purpose': 'add_tap'}
else:
    d = {'action': 'finish', 'outcome': 'sheet_opened'}
d.update(frame=frame, observation='fake observation sk-ant-oat01-SECRETSECRET')
emit({'type': 'result', 'is_error': False, 'num_turns': 2, 'result': json.dumps(d)})
'''


class FakeRunnerTests(unittest.TestCase):
    def run_controller(self, mode, deadline_s=900, stall_window=1.0):
        temp = Path(tempfile.mkdtemp(prefix='simqa-test-'))
        bin_dir = temp / 'bin'
        bin_dir.mkdir()
        for name, body in (('xcodebuild', FAKE_XCODEBUILD), ('xcrun', FAKE_XCRUN), ('claude', FAKE_CLAUDE)):
            (bin_dir / name).write_text(body)
            (bin_dir / name).chmod(0o755)
        (bin_dir / 'mode').write_text(mode)
        out = temp / 'out'
        out.mkdir()
        env = {'PATH': f'{bin_dir}:{os.environ["PATH"]}', 'HOME': str(temp), 'CLAUDE_CODE_OAUTH_TOKEN': 'fake-oauth',
               'GITHUB_TOKEN': 'must-not-pass'}
        args = [sys.executable, str(HERE / 'controller.py'), 'run', '--src', str(temp), '--sha', SHA,
                '--udid', 'UDID', '--derived-data', str(temp / 'dd'), '--out', str(out), '--raw', str(temp / 'raw'),
                '--mailbox', str(temp / 'box'), '--prompt', str(HERE / 'prompt.md'),
                '--deadline', str(time.time() + deadline_s), '--scenario', 'catalog-chat-add',
                '--stall-window', str(stall_window)]
        process = subprocess.run(args, env=env, capture_output=True, text=True, timeout=120)
        receipt = json.loads((out / 'receipt.json').read_text())
        return process, receipt, out, temp

    def test_add_opens_sheet(self):
        process, receipt, out, _ = self.run_controller('ok')
        self.assertEqual(process.returncode, 0, process.stdout + process.stderr)
        self.assertEqual(receipt['status'], 'sheet_opened')
        self.assertEqual(receipt['successful_png_reads'], 3)
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

    def test_every_capture_is_recorded_and_clean_runs_say_so(self):
        process, receipt, out, _ = self.run_controller('ok')
        self.assertTrue(receipt['captures'])
        self.assertTrue(all(c['simctl'] == 'clean' and c['kept'] and c['pixels'] == [1179, 2556]
                            for c in receipt['captures']))
        self.assertIn('- Capture: all ', (out / 'summary.md').read_text())

    def test_fresh_whole_frame_kept_when_simctl_exits_non_zero_but_reported(self):
        process, receipt, out, _ = self.run_controller('shotexit')
        self.assertEqual(process.returncode, 0, process.stdout + process.stderr)
        self.assertEqual(receipt['status'], 'sheet_opened')  # the observed UI result, unchanged
        self.assertTrue(all(c['simctl'] == 'exit 1' and c['kept'] for c in receipt['captures']))
        summary = (out / 'summary.md').read_text()
        self.assertIn('**sheet_opened**', summary)
        self.assertIn('- Capture: **non-clean**', summary)

    def test_truncated_frame_is_rejected(self):
        process, receipt, out, _ = self.run_controller('shottruncated')
        self.assertEqual(receipt['status'], 'screenshot_failed')
        self.assertEqual(receipt['captures'][0]['simctl'], 'exit 1')
        self.assertFalse(receipt['captures'][0]['kept'])

    def test_wrong_size_frame_is_rejected(self):
        process, receipt, out, _ = self.run_controller('shotwrongsize')
        self.assertEqual(receipt['status'], 'screenshot_failed')
        self.assertIn('not 393x852 points', receipt['captures'][0]['problems'][0])

    def test_no_file_is_rejected(self):
        process, receipt, out, _ = self.run_controller('shotnone')
        self.assertEqual(receipt['status'], 'screenshot_failed')
        self.assertEqual(receipt['captures'][0]['problems'], ['no file written'])

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


if __name__ == '__main__':
    unittest.main()
