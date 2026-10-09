#!/usr/bin/env python3
"""Only allowlisted public metadata; no binaries, URLs to private downloads or provider logs."""
import json
import os
from pathlib import Path
import shutil
import sys
from gate import arguments, visual_review, require


def public_record(data):
    arguments(data.get('source_sha'), data.get('native_run_id'), data.get('build_number'))
    require(isinstance(data.get('native_run_attempt'), int) and data['native_run_attempt'] > 0, 'Invalid attempt')
    require(data.get('physical_device') == 'unverified', 'Unmeasured physical-device claim')
    if 'platform' in data:
        require(data['platform'] in ('ios', 'android'), 'Invalid platform')
    if 'binary_sha256' in data:
        import re
        require(re.fullmatch('[0-9a-f]{64}', data['binary_sha256']), 'Invalid binary digest')
    require(data.get('distribution') in ('not_started', 'upload_started', 'processed',
            'assigned_existing_internal_group', 'published_existing_internal_track'), 'Unknown state')
    visual_review(data.get('visual_review_url'))
    allowed = ('source_sha', 'native_run_id', 'native_run_attempt', 'build_number', 'platform',
               'binary_sha256', 'distribution', 'physical_device', 'visual_review_url')
    return {k: data[k] for k in allowed if k in data}


def main():
    mode, path = sys.argv[1], Path(sys.argv[2])
    if mode == 'cleanup':
        require(path.resolve() == Path(os.environ['RUNNER_TEMP']).resolve(), 'Only clean runner temporary output')
        for name in ('playground-package', 'playground-store-private.log', 'admission.json'):
            target = path / name
            if target.is_symlink():
                target.unlink()
            elif target.is_dir():
                shutil.rmtree(target)
            elif target.exists():
                target.unlink()
    else:
        text = 'No verified distribution result. Check failed stage; an upload may require store reconciliation.'
        if path.exists():
            try:
                text = '```json\n' + json.dumps(public_record(json.loads(path.read_text())), indent=2) + '\n```'
            except (ValueError, TypeError, KeyError):
                text = 'Invalid distribution metadata suppressed. Reconcile provider state before retrying.'
        if mode == 'admission':
            text += '\nEnvironment approver: verify independent review and paired visual acceptance at this exact SHA, committed store metadata, existing tester scope, and build number before approving. Native tests alone do not establish visual readiness.\n'
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as f:
            f.write(text + '\n')


if __name__ == '__main__':
    main()
