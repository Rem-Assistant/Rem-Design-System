#!/usr/bin/env python3
"""Explicit private device install; records install and launch separately. No provisioning."""
import argparse
import json
import plistlib
import stat
import subprocess
import tempfile
import zipfile
from pathlib import Path, PurePosixPath

import delivery as d


def safe_archive(path):
    with zipfile.ZipFile(path) as archive:
        names = []
        total = 0
        for item in archive.infolist():
            name = PurePosixPath(item.filename)
            d.require(not name.is_absolute() and '..' not in name.parts, 'Unsafe archive path')
            d.require(not stat.S_ISLNK(item.external_attr >> 16), 'Symlink archives are unsupported')
            names.append(name.parts[0] if name.parts else '')
            total += item.file_size
        d.require(names and set(names) <= {'RemSettingsPlayground.app', '__MACOSX'}, 'Unexpected app archive')
        d.require(total < 1024**3, 'Unexpectedly large app archive')


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--sha', required=True)
    p.add_argument('--manifest', required=True)
    p.add_argument('--binary', required=True)
    p.add_argument('--device', required=True)
    p.add_argument('--output', required=True, help='New private verification directory on the external volume')
    args = p.parse_args()
    d.source(args.sha)
    binary = Path(args.binary).resolve()
    record = json.loads(Path(args.manifest).read_text())
    d.validate_manifest(record, args.sha, 'ios', binary)
    safe_archive(binary)
    out = Path(args.output).resolve()
    out.mkdir()  # Refuse to reuse old install/launch results.
    verification = {'status': 'not_installed', 'source_sha': args.sha,
                    'binary_sha256': record['binary_sha256'], 'evidence_sha256': {}}
    record['device_verification'] = verification
    try:
        with tempfile.TemporaryDirectory(prefix='app-', dir=out) as tmp:
            subprocess.run(['ditto', '-x', '-k', str(binary), tmp], check=True)
            app = Path(tmp) / 'RemSettingsPlayground.app'
            metadata = d.inspect_artifact('ios', app, args.sha)
            d.require(metadata['version'] == record['version'] and metadata['version_code'] == record['version_code'], 'Binary version mismatch')
            subprocess.run(['codesign', '--verify', '--deep', '--strict', str(app)], check=True)
            for name, command in [
                ('install', ['install', 'app', str(app)]),
                ('installed', ['info', 'apps', '--bundle-id', d.IDS['ios']]),
                ('launch', ['process', 'launch', d.IDS['ios']]),
            ]:
                evidence = out / (name + '.json')
                subprocess.run(['xcrun', 'devicectl', 'device', *command, '--device', args.device,
                                '--timeout', '60', '--json-output', str(evidence)], check=True)
                result = json.loads(evidence.read_text())
                d.require(result.get('info', {}).get('outcome') == 'success', name + ' not confirmed')
                verification['evidence_sha256'][name] = d.digest(evidence)
                if name == 'install':
                    apps = result['result']['installedApplications']
                    d.require(any(a.get('bundleID') == d.IDS['ios'] for a in apps), 'Install returned wrong app')
                    verification['status'] = 'installed_launch_unverified'
                elif name == 'installed':
                    apps = result['result']['apps']
                    d.require(any(a.get('bundleIdentifier') == d.IDS['ios'] and a.get('version') == record['version'] and a.get('bundleVersion') == record['version_code'] for a in apps), 'Installed version mismatch')
                else:
                    verification['status'] = 'installed_launch_confirmed'
                    record['readiness'] = 'native_tested_launch_confirmed'
    except (subprocess.CalledProcessError, ValueError, OSError, KeyError) as error:
        verification['error'] = str(error)
        d.write_json(out / 'manifest.json', record)
        raise SystemExit('Device verification incomplete; inspect private evidence in ' + str(out))
    d.write_json(out / 'manifest.json', record)
    print('Install and launch confirmed. This does not claim a visual/interactive review:', out)


if __name__ == '__main__':
    main()
