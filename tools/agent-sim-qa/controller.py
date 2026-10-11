#!/usr/bin/env python3
"""Hosted visual diagnostic controller (runner-local; see README.md).

Subcommands:
  verify-source  assert the source checkout is pristine (or differs only by the injected bridge)
                 and HEAD is the exact SHA
  verify-built   assert the built app's Info.plist embeds that SHA
  run            drive the loop: XCTest bridge observes -> simctl frame -> model Reads the PNG and
                 chooses one typed input -> bridge executes it -> a newer frame is captured and Read

The model gets the Read tool only, no shell and no GitHub token. Raw model transcripts stay in
--raw (never uploaded); --out receives frames, UI listings, receipt.json, a sanitized transcript
summary and summary.md, rewritten after every step so a failure keeps the partial record.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import plistlib
import re
import signal
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import simqa  # noqa: E402

CLAUDE_ENV = ('PATH', 'HOME', 'TMPDIR', 'LANG', 'LC_ALL', 'USER', 'CI', 'GITHUB_ACTIONS')
SCENARIOS = {
    'catalog-chat-add': (
        'From the Playground root, open the Component catalog (identifier openComponents), go to its '
        'Chat page, bring the composer into view, then tap the composer Add button (identifier '
        'catalog.composerAdd) and check whether the Add to Chat sheet appears with Photos '
        '(catalog.addToChat.photos) and Files (catalog.addToChat.files). Then finish.'),
    'catalog-chat-model-menu-add': (
        'From the Playground root, open the Component catalog (identifier openComponents) and go to '
        'its Chat page. Open the model menu (catalog.modelMenu), choose Automatic, and confirm the menu '
        'closed. Then bring the composer into view, tap the composer Add button (identifier '
        'catalog.composerAdd) and check whether the Add to Chat sheet appears with Photos '
        '(catalog.addToChat.photos) and Files (catalog.addToChat.files). Then finish.'),
}
STALL_WINDOW = 45          # seconds to wait for one bridge command before calling it stalled
MODEL_CALL_CAP = 180       # seconds per model decision, further bounded by the job deadline
RESERVE = 150              # seconds kept for teardown, summary and artifact upload
SECRET_NAME = re.compile(r'TOKEN|SECRET|KEY|PASSWORD|CREDENTIAL|AUTH', re.I)


def git(src, *args):
    return subprocess.run(['git', '-C', str(src), *args], check=True, capture_output=True, text=True).stdout


def record_verification(out, key, value):
    path = Path(out) / 'source-verification.json'
    data = json.loads(path.read_text()) if path.exists() else {}
    data[key] = value
    path.write_text(json.dumps(data, indent=2) + '\n')


def cmd_verify_source(a):
    head = git(a.src, 'rev-parse', 'HEAD').strip()
    porcelain = git(a.src, 'status', '--porcelain', '--untracked-files=all')
    problems = simqa.check_source_state(head, a.sha, porcelain, allow_injected=a.injected)
    entry = {'head': head, 'expected': a.sha, 'ok': not problems, 'problems': problems}
    if a.injected:
        diff = git(a.src, 'diff', '--', simqa.INJECTED_FILE)
        entry['injected_file'] = simqa.INJECTED_FILE
        entry['injected_diff_sha256'] = hashlib.sha256(diff.encode()).hexdigest()
    record_verification(a.out, 'after_injection' if a.injected else 'pristine_before_build', entry)
    for problem in problems:
        print(f'::error::{problem}')
    return 1 if problems else 0


def cmd_verify_built(a):
    with open(Path(a.app) / 'Info.plist', 'rb') as handle:
        plist = plistlib.load(handle)
    problems = simqa.check_built_sha(plist, a.sha)
    record_verification(a.out, 'built_app', {
        'key': 'RemPlaygroundSourceSHA', 'value': plist.get('RemPlaygroundSourceSHA'),
        'expected': a.sha, 'ok': not problems})
    for problem in problems:
        print(f'::error::{problem}')
    return 1 if problems else 0


class Run:
    def __init__(self, a):
        self.a = a
        self.out = Path(a.out).resolve()
        self.frames = self.out / 'frames'
        self.raw = Path(a.raw).resolve()
        self.mailbox = Path(a.mailbox).resolve()
        for path in (self.frames, self.raw, self.mailbox):
            path.mkdir(parents=True, exist_ok=True)
        self.template = Path(a.prompt).read_text()
        self.deadline = float(a.deadline)
        self.seq = 0
        self.process = None
        self.screen = (393.0, 852.0)
        self.ledger = []
        self.calls = []
        self.history = []
        self.receipt = {
            'kind': 'hosted visual diagnostic (not native evidence, not release approval)',
            'source_sha': a.sha,
            'control_sha': os.environ.get('GITHUB_SHA'),
            'run_id': os.environ.get('GITHUB_RUN_ID'),
            'scenario': a.scenario,
            'model': a.model,
            'cli_version': os.environ.get('CLAUDE_CODE_VERSION'),
            'bridge': 'xctest-file-mailbox-v1 (local; no port)',
            'status': 'starting',
            'job_deadline_epoch': int(self.deadline),
            'started_at': time.time(),
            'stalls': [],
            'actions': [],
            'frames': [],
        }
        verification = self.out / 'source-verification.json'
        if verification.exists():
            self.receipt['source_verification'] = json.loads(verification.read_text())

    # -- persistence ---------------------------------------------------------------------------
    def save(self):
        self.receipt['ledger'] = self.ledger
        self.receipt['successful_png_reads'] = sum(c.get('successful_png_reads', 0) for c in self.calls)
        self.receipt['model_calls'] = len(self.calls)
        (self.out / 'receipt.json').write_text(json.dumps(self.receipt, indent=2) + '\n')
        (self.out / 'transcript-summary.json').write_text(json.dumps(self.calls, indent=2) + '\n')

    # -- bridge --------------------------------------------------------------------------------
    def bridge_alive(self):
        return self.process is not None and self.process.poll() is None

    def start_bridge(self):
        env = {k: v for k, v in os.environ.items() if not SECRET_NAME.search(k)}
        env['TEST_RUNNER_AGENT_BRIDGE_DIR'] = str(self.mailbox)
        allowance = max(120, int(self.deadline - time.time() - RESERVE))
        cmd = ['xcodebuild', 'test-without-building',
               '-project', str(Path(self.a.src) / 'tools/playground-ios/RemSettingsPlayground.xcodeproj'),
               '-scheme', 'RemSettingsPlayground',
               '-destination', f'platform=iOS Simulator,id={self.a.udid}',
               '-derivedDataPath', self.a.derived_data,
               '-only-testing:RemSettingsPlaygroundUITests/AgentVisualBridgeUITests/testAgentVisualBridge',
               '-parallel-testing-enabled', 'NO',
               '-test-timeouts-enabled', 'YES',
               '-maximum-test-execution-time-allowance', str(allowance),
               '-resultBundlePath', str(self.raw / 'bridge.xcresult'),
               'CODE_SIGNING_ALLOWED=NO']
        log = open(self.out / 'xcodebuild-test.log', 'w')
        self.process = subprocess.Popen(cmd, stdout=log, stderr=subprocess.STDOUT, env=env,
                                        start_new_session=True)

    def wait_file(self, name, window):
        end = min(time.time() + window, self.deadline - RESERVE)
        path = self.mailbox / name
        while time.time() < end:
            if path.exists():
                try:
                    return json.loads(path.read_text())
                except ValueError:
                    pass
            if not self.bridge_alive():
                return None
            time.sleep(0.25)
        return None

    def send(self, kind, **fields):
        seq = self.seq
        self.seq += 1
        temp = self.mailbox / f'.cmd-{seq:03d}.json.tmp'
        temp.write_text(json.dumps({'seq': seq, 'kind': kind, **fields}))
        temp.rename(self.mailbox / f'cmd-{seq:03d}.json')
        return seq

    def command(self, kind, **fields):
        """Send one command; wait one stall window, then at most one more. Never resend input."""
        dispatched = time.time()
        seq = self.send(kind, **fields)
        result = self.wait_file(f'res-{seq:03d}.json', self.a.stall_window)
        stalled = False
        if result is None and self.bridge_alive():
            stall = {'seq': seq, 'kind': kind, 'after_s': self.a.stall_window,
                     'acknowledged_by_bridge': (self.mailbox / f'ack-{seq:03d}.json').exists()}
            evidence = self.frames / f'stall-{seq:03d}.png'
            stall['evidence_png'] = evidence.name if self.screenshot(evidence) else None
            self.receipt['stalls'].append(stall)
            self.save()
            result = self.wait_file(f'res-{seq:03d}.json', self.a.stall_window)  # the single retry: wait once more
            stalled = result is None
            stall['recovered'] = not stalled
        return seq, dispatched, (time.time() if result is not None else None), result, stalled

    def screenshot(self, path):
        # The file decides, not simctl's status: it can write a complete frame and then exit
        # non-zero or hang past the timeout. A frame kept that way is noted in the receipt.
        outcome = 'ok'
        try:
            subprocess.run(['xcrun', 'simctl', 'io', self.a.udid, 'screenshot', str(path)],
                           check=True, capture_output=True, timeout=30)
        except subprocess.TimeoutExpired:
            outcome = 'timeout'
        except subprocess.CalledProcessError as error:
            outcome = f'exit {error.returncode}'
        except (subprocess.SubprocessError, OSError):
            outcome = 'error'
        try:
            ok = path.exists() and simqa.complete_png(path.read_bytes())
        except OSError:
            ok = False
        if outcome != 'ok':
            self.receipt.setdefault('screenshot_notes', []).append(
                {'png': path.name, 'simctl': outcome, 'kept': ok})
        return ok

    # -- model ---------------------------------------------------------------------------------
    def ask(self, frame, png, ui, finish_only, budget_left):
        budget = simqa.call_budget(time.time(), self.deadline, RESERVE, MODEL_CALL_CAP)
        call = {'frame': frame, 'png': png.name, 'budget_s': budget}
        self.calls.append(call)
        if budget == 0:
            call['error_category'] = 'deadline'
            return None, call
        values = {
            'FRAME_FILE': str(png), 'FRAME': str(frame), 'WIDTH': f'{self.screen[0]:g}',
            'HEIGHT': f'{self.screen[1]:g}', 'BUDGET': str(budget_left), 'UI': ui,
            'SCENARIO': SCENARIOS[self.a.scenario],
            'HISTORY': json.dumps(self.history[-10:], indent=1),
            'FINISH_ONLY': ('6. The input bridge STALLED: it did not report completing the last command. '
                            'Only `finish` is accepted now. Report what THIS screenshot shows.'
                            if finish_only else ''),
        }
        prompt = re.sub(r'\{\{([A-Z_]+)\}\}', lambda m: values.get(m.group(1), m.group(0)), self.template)
        token = os.environ.get('CLAUDE_CODE_OAUTH_TOKEN', '')
        if not token:
            call['error_category'] = 'model_auth'
            return None, call
        env = {k: os.environ[k] for k in CLAUDE_ENV if os.environ.get(k)}
        env['CLAUDE_CODE_OAUTH_TOKEN'] = token
        cmd = ['claude', '-p', '--model', self.a.model,
               '--tools', 'Read', '--allowedTools', 'Read', '--add-dir', str(self.frames),
               '--strict-mcp-config', '--safe-mode', '--no-session-persistence',
               '--prompt-suggestions', 'false', '--max-turns', '4',
               '--output-format', 'stream-json', '--verbose']
        raw_out = self.raw / f'call-{frame:02d}.jsonl'
        started = time.time()
        timed_out = False
        with open(raw_out, 'w') as stdout, open(self.raw / f'call-{frame:02d}.stderr', 'w') as stderr:
            process = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=stdout, stderr=stderr, text=True,
                                       env=env, cwd=self.frames, start_new_session=True)
            try:
                process.communicate(prompt, timeout=budget)
            except subprocess.TimeoutExpired:
                timed_out = True
                kill_group(process)
        summary = simqa.parse_transcript(raw_out.read_text(errors='replace').splitlines(), png)
        call.update({
            'duration_s': round(time.time() - started, 1), 'exit': process.returncode, 'timed_out': timed_out,
            'cli_model': summary['model'], 'num_turns': summary['num_turns'], 'is_error': summary['is_error'],
            'tool_uses': summary['tool_uses'], 'successful_png_reads': summary['successful_png_reads'],
            'frame_read_ok': summary['expected_read_ok'],
        })
        category = simqa.classify_failure(process.returncode, timed_out, summary)
        if category:
            call['error_category'] = category
            return None, call
        return summary['result'], call

    # -- loop ----------------------------------------------------------------------------------
    def finish(self, status, **extra):
        self.receipt['status'] = status
        self.receipt.update(extra)
        self.save()

    def loop(self):
        self.start_bridge()
        ready = self.wait_file('ready.json', 600)
        if not ready:
            self.screenshot(self.frames / 'bridge-not-ready.png')
            return self.finish('bridge_not_ready')
        self.screen = (float(ready.get('width') or 393), float(ready.get('height') or 852))
        self.receipt['screen_points'] = list(self.screen)
        stalled = False
        for frame in range(self.a.max_decisions):
            if not stalled and not self.bridge_alive():
                stalled = True  # the bridge exited; nothing more can be executed, only observed
                self.receipt['bridge_exited_early'] = True
            ui = 'unavailable: the XCTest bridge stalled or exited'
            if not stalled:
                _, _, _, result, stalled = self.command('observe')
                if result and result.get('ok'):
                    ui = result.get('ui') or '(empty)'
                elif result:
                    ui = 'unavailable: ' + simqa.redact(str(result.get('error')), 200)
            png = self.frames / f'frame-{frame:02d}.png'
            captured_at = time.time()
            if not self.screenshot(png):
                return self.finish('screenshot_failed', failed_frame=frame)
            (self.frames / f'frame-{frame:02d}.ui.txt').write_text(ui + '\n')
            frame_event = {'type': 'frame', 'index': frame, 'captured_at': captured_at, 'read_ok': False,
                           'png': png.name, 'png_sha256': hashlib.sha256(png.read_bytes()).hexdigest(),
                           'ui_listing': 'present' if not ui.startswith('unavailable') else 'unavailable'}
            self.ledger.append(frame_event)
            self.receipt['frames'].append(png.name)
            self.save()
            text, call = self.ask(frame, png, ui, stalled, self.a.max_decisions - frame)
            frame_event['read_ok'] = bool(call.get('frame_read_ok'))
            if call.get('error_category'):
                status = 'deadline' if call['error_category'] in ('deadline', 'model_timeout') else 'model_unavailable'
                return self.finish(status, error_category=call['error_category'])
            if not frame_event['read_ok']:
                return self.finish('png_read_failed', failed_frame=frame)
            try:
                decision = simqa.validate_decision(simqa.extract_json(text), frame, self.screen, stalled)
            except simqa.DecisionError as error:
                return self.finish('invalid_decision', failed_frame=frame, detail=str(error))
            decision['observation'] = simqa.redact(decision['observation'], 400)
            call['decision'] = {k: v for k, v in decision.items() if k != 'observation'}
            if decision['action'] == 'finish':
                self.ledger.append({'type': 'finish', 'basis_frame': frame, 'at': time.time()})
                return self.conclude(decision, ui, stalled)
            fields = {k: decision[k] for k in simqa.ACTIONS[decision['action']]}
            seq, dispatched, done, result, stalled = self.command(decision['action'], **fields)
            action = {'type': 'action', 'index': len(self.receipt['actions']), 'basis_frame': frame,
                      'seq': seq, 'dispatched_at': dispatched, 'done_at': done}
            self.ledger.append(action)
            record = {'basis_frame': frame, 'action': decision['action'], 'purpose': decision['purpose'],
                      **fields, 'observation': decision['observation'],
                      'bridge_ok': bool(result and result.get('ok')), 'stalled': stalled,
                      'bridge_error': simqa.redact(str(result.get('error')), 200) if result and result.get('error') else None,
                      'element_frame': result.get('element_frame') if result else None,
                      'duration_s': round(done - dispatched, 2) if done else None}
            self.receipt['actions'].append(record)
            self.history.append({k: record[k] for k in ('basis_frame', 'action', 'purpose', 'bridge_ok', 'stalled',
                                                        'bridge_error', 'observation')} | fields)
            self.save()
        return self.finish('budget_exhausted', violations=simqa.check_acknowledged(self.ledger))

    def conclude(self, decision, ui, stalled):
        violations = simqa.check_acknowledged(self.ledger)
        add_taps = [a for a in self.receipt['actions'] if a['purpose'] == 'add_tap']
        if decision['outcome'] == 'sheet_opened' and not add_taps:
            violations.append('sheet_opened reported without any add_tap action')
        confirms = simqa.ui_confirms_sheet(None if ui.startswith('unavailable') else ui)
        observed = decision['outcome']
        if violations:
            status = 'unacknowledged'
        elif stalled:
            status = 'input_stalled'
        else:
            status = observed
        return self.finish(status, observed_outcome=observed, final_observation=decision['observation'],
                           ui_listing_confirms_sheet=confirms, chosen_add_taps=add_taps, violations=violations)

    def close(self):
        if self.bridge_alive():
            self.send('end')
            try:
                self.process.wait(timeout=30)
            except subprocess.TimeoutExpired:
                kill_group(self.process)
        if self.process is not None:
            self.receipt['bridge_exit'] = self.process.returncode
        self.receipt['finished_at'] = time.time()
        self.save()
        write_summary(self.receipt, self.calls, self.out / 'summary.md')


def kill_group(process):
    for sig in (signal.SIGTERM, signal.SIGKILL):
        try:
            os.killpg(process.pid, sig)
        except (ProcessLookupError, PermissionError):
            return
        try:
            process.wait(timeout=10)
            return
        except subprocess.TimeoutExpired:
            continue


def write_summary(receipt, calls, path):
    v = receipt.get('source_verification', {})
    built = v.get('built_app', {})
    lines = [
        '## Hosted visual diagnostic (not native evidence, not release approval)', '',
        f"- Status: **{receipt['status']}**",
        f"- Observed outcome: {receipt.get('observed_outcome', 'none')}",
        f"- UI listing confirms sheet: {receipt.get('ui_listing_confirms_sheet')}",
        f"- Source SHA (checkout): `{receipt['source_sha']}`",
        f"- Built app RemPlaygroundSourceSHA: `{built.get('value')}` (ok: {built.get('ok')})",
        f"- Pristine before build: {v.get('pristine_before_build', {}).get('ok')}; "
        f"only bridge injected: {v.get('after_injection', {}).get('ok')}",
        f"- Model: {receipt['model']} via CLI {receipt.get('cli_version')}; calls: {len(calls)}; "
        f"successful PNG reads: {receipt.get('successful_png_reads')}",
        f"- Stalls: {len(receipt.get('stalls', []))}; violations: {receipt.get('violations') or 'none'}",
        '', '| Frame | Action | Purpose | Target | Bridge | Seconds |', '|---|---|---|---|---|---|',
    ]
    for action in receipt.get('actions', []):
        target = action.get('id') or ', '.join(str(action[k]) for k in ('x', 'y', 'to_x', 'to_y') if k in action)
        bridge = 'stalled' if action.get('stalled') else ('ok' if action.get('bridge_ok') else 'error')
        lines.append(f"| {action['basis_frame']} | {action['action']} | {action['purpose']} | `{target}` | "
                     f"{bridge} | {action.get('duration_s')} |")
    if receipt.get('final_observation'):
        lines += ['', f"Final observation: {receipt['final_observation']}"]
    path.write_text('\n'.join(lines) + '\n')


def cmd_run(a):
    run = Run(a)
    try:
        run.loop()
    except Exception as error:  # keep partial evidence; report only the error type
        run.finish('harness_error', error=type(error).__name__)
    finally:
        run.close()
    print(json.dumps({k: run.receipt.get(k) for k in ('status', 'observed_outcome', 'successful_png_reads')}))
    ok = run.receipt['status'] == 'sheet_opened' and run.receipt.get('ui_listing_confirms_sheet') is not False
    return 0 if ok else 1


def main(argv=None):
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest='command', required=True)
    p = sub.add_parser('verify-source')
    p.add_argument('--src', required=True); p.add_argument('--sha', required=True)
    p.add_argument('--out', required=True); p.add_argument('--injected', action='store_true')
    p.set_defaults(func=cmd_verify_source)
    p = sub.add_parser('verify-built')
    p.add_argument('--app', required=True); p.add_argument('--sha', required=True); p.add_argument('--out', required=True)
    p.set_defaults(func=cmd_verify_built)
    p = sub.add_parser('run')
    for name in ('--src', '--sha', '--udid', '--derived-data', '--out', '--raw', '--mailbox', '--prompt', '--deadline'):
        p.add_argument(name, required=True)
    p.add_argument('--scenario', required=True, choices=sorted(SCENARIOS))
    p.add_argument('--model', default='claude-opus-4-8')
    p.add_argument('--max-decisions', type=int, default=14)
    p.add_argument('--stall-window', type=float, default=STALL_WINDOW, help=argparse.SUPPRESS)
    p.set_defaults(func=cmd_run)
    a = parser.parse_args(argv)
    return a.func(a)


if __name__ == '__main__':
    raise SystemExit(main())
