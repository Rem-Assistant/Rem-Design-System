#!/usr/bin/env python3
"""Only allowlisted public metadata; no binaries, URLs to private downloads or provider logs."""
import json
import os
import re
from pathlib import Path
import shutil
import sys
from gate import arguments, visual_review, require, REVIEWERS


# Where fastlane distribute stopped (Fastfile release_phase); fixed values only.
FAILURE_PHASES = ('inputs', 'toolchain', 'apple_key', 'group_check', 'build_numbers', 'upload', 'group_recheck',
                  'processing_check', 'assign', 'play_auth', 'play_precheck', 'play_readback')
# After a TestFlight upload failure only: did Apple create a build record for this number (one read-only GET).
UPLOAD_BUILD_RECORDS = ('present', 'absent', 'unknown')
# iOS only: the Xcode that built the IPA (package.py xcode / Fastfile ios_toolchain), in fixed shapes.
TOOLCHAIN_SHAPES = {'xcode': r'\d{2}\.\d{1,2}(?:\.\d{1,2})?', 'xcode_build': r'\d{2}[A-Z]\d{1,5}[a-z]?',
                    'sdk': r'iphoneos\d{2}\.\d{1,2}(?:\.\d{1,2})?'}
# Credential-free `xcrun altool --help` probe: was --use-old-altool passed to the upload?
ALTOOL_OLD_FLAG = ('supported', 'unsupported', 'unknown')
# The only states a successful store step may end in.
COMPLETED_STATES = ('assigned_existing_internal_group', 'published_existing_internal_track')


def failure_line(record):
    """One allowlisted line for the public job log: state, and where and why the store step stopped."""
    f = record.get('failure')
    line = f"Distribution state: {record.get('distribution')}"
    t = record.get('toolchain')
    if t:
        line += f"; Xcode {t['xcode']} ({t['xcode_build']}), SDK {t['sdk']}"
    if 'altool_old_flag' in record:
        line += f"; old altool flag {record['altool_old_flag']}"
    if f:
        line += f"; stopped in phase {f['phase']} at guard {f['guard']}; HTTP status {f['http_status'] or 'none'}"
        if 'upload_build_record' in f:
            line += f"; build record {f['upload_build_record']}"
            if f['upload_build_record'] == 'present':
                # altool can report a failure for an upload Apple accepted; the number is now used.
                line += '; next step: reconcile the existing build, never re-upload'
    return line


def public_record(data):
    arguments(data.get('source_sha'), data.get('native_run_id'), data.get('build_number'))
    require(isinstance(data.get('native_run_attempt'), int) and data['native_run_attempt'] > 0, 'Invalid attempt')
    require(data.get('physical_device') == 'unverified', 'Unmeasured physical-device claim')
    if 'platform' in data:
        require(data['platform'] in ('ios', 'android'), 'Invalid platform')
    if 'binary_sha256' in data:
        require(re.fullmatch('[0-9a-f]{64}', data['binary_sha256']), 'Invalid binary digest')
    require(data.get('distribution') in ('not_started', 'upload_started', 'processed',
            'assigned_existing_internal_group', 'published_existing_internal_track'), 'Unknown state')
    visual_review(data.get('visual_review_url'))
    if 'approvals' in data:
        require(isinstance(data['approvals'], dict) and data['approvals'], 'Missing approval model')
        for platform, approval in data['approvals'].items():
            require(platform in ('ios', 'android') and isinstance(approval, dict) and
                    set(approval) == {'model', 'reviewer'}, 'Invalid approval metadata')
            require(approval['model'] in ('owner-approval', 'independent') and
                    approval['reviewer'] in REVIEWERS.values(), 'Unknown approval model or reviewer')
            require(approval['model'] != 'owner-approval' or approval['reviewer'] == 'samuelalake',
                    'Invalid owner approval identity')
    if 'toolchain' in data:
        t = data['toolchain']
        require(data.get('platform') == 'ios' and isinstance(t, dict) and set(t) == set(TOOLCHAIN_SHAPES) and
                all(isinstance(t[k], str) and re.fullmatch(v, t[k], flags=re.ASCII) for k, v in TOOLCHAIN_SHAPES.items()),
                'Invalid toolchain metadata')
    if 'altool_old_flag' in data:
        require(data.get('platform') == 'ios' and data['altool_old_flag'] in ALTOOL_OLD_FLAG, 'Invalid altool probe state')
    if 'failure' in data:
        f = data['failure']
        require(isinstance(f, dict) and set(f) in ({'phase', 'guard', 'http_status'},
                {'phase', 'guard', 'http_status', 'upload_build_record'}), 'Invalid failure metadata')
        require(f['phase'] in FAILURE_PHASES, 'Unknown failure phase')
        require('upload_build_record' not in f or (f['phase'] == 'upload' and
                f['upload_build_record'] in UPLOAD_BUILD_RECORDS), 'Invalid upload build record')
        require(isinstance(f['guard'], str) and re.fullmatch('[a-z0-9_]{1,64}', f['guard']), 'Invalid failure guard')
        require(f['http_status'] is None or (isinstance(f['http_status'], int) and 100 <= f['http_status'] <= 599),
                'Invalid failure HTTP status')
    allowed = ('source_sha', 'native_run_id', 'native_run_attempt', 'build_number', 'platform',
               'binary_sha256', 'toolchain', 'altool_old_flag', 'distribution', 'physical_device', 'visual_review_url',
               'approvals', 'failure')
    return {k: data[k] for k in allowed if k in data}


def main():
    mode, path = sys.argv[1], Path(sys.argv[2])
    if mode == 'cleanup':
        require(path.resolve() == Path(os.environ['RUNNER_TEMP']).resolve(), 'Only clean runner temporary output')
        for name in ('playground-package', 'playground-ephemeral', 'playground-store-private.log', 'admission.json',
                     'playground-store-tmp'):
            target = path / name
            if target.is_symlink():
                target.unlink()
            elif target.is_dir():
                shutil.rmtree(target)
            elif target.exists():
                target.unlink()
    elif mode == 'completed':
        # Exit status only, nothing printed: a final store state with no failure record.
        try:
            record = public_record(json.loads(path.read_text()))
            done = 'failure' not in record and record.get('distribution') in COMPLETED_STATES
        except (OSError, ValueError, TypeError, KeyError):
            done = False
        sys.exit(0 if done else 1)
    else:
        text = 'No verified distribution result. Check failed stage; an upload may require store reconciliation.'
        if path.exists():
            try:
                record = public_record(json.loads(path.read_text()))
                text = '```json\n' + json.dumps(record, indent=2) + '\n```'
                if mode == 'result':
                    print(failure_line(record))
            except (ValueError, TypeError, KeyError):
                text = 'Invalid distribution metadata suppressed. Reconcile provider state before retrying.'
        if mode == 'admission':
            text += '\nEnvironment approver: verify independent review and paired visual acceptance at this exact SHA, committed store metadata, existing tester scope, and build number before approving. Native tests alone do not establish visual readiness.\n'
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as f:
            f.write(text + '\n')


if __name__ == '__main__':
    main()
