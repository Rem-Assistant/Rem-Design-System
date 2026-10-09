#!/usr/bin/env python3
"""Run checksum-pinned Actionlint without a global installation or credentials."""
import hashlib
import io
from pathlib import Path
import platform
import subprocess
import tarfile
import tempfile
import urllib.request

VERSION = '1.7.12'
DIGESTS = {
    'linux_amd64': '8aca8db96f1b94770f1b0d72b6dddcb1ebb8123cb3712530b08cc387b349a3d8',
    'darwin_amd64': '5b44c3bc2255115c9b69e30efc0fecdf498fdb63c5d58e17084fd5f16324c644',
    'darwin_arm64': 'aba9ced2dee8d27fecca3dc7feb1a7f9a52caefa1eb46f3271ea66b6e0e6953f',
}
target = platform.system().lower() + '_' + {'x86_64': 'amd64', 'arm64': 'arm64'}[platform.machine()]
url = f'https://github.com/rhysd/actionlint/releases/download/v{VERSION}/actionlint_{VERSION}_{target}.tar.gz'
with urllib.request.urlopen(url, timeout=60) as response:
    data = response.read(20 * 1024 * 1024)
if hashlib.sha256(data).hexdigest() != DIGESTS[target]:
    raise SystemExit('Actionlint download checksum mismatch')
with tempfile.TemporaryDirectory(prefix='playground-actionlint-') as temp:
    binary = Path(temp) / 'actionlint'
    with tarfile.open(fileobj=io.BytesIO(data), mode='r:gz') as archive:
        member = archive.getmember('actionlint')
        if not member.isfile() or member.size > 30 * 1024 * 1024:
            raise SystemExit('Unexpected Actionlint archive')
        binary.write_bytes(archive.extractfile(member).read())
    binary.chmod(0o700)
    root = Path(__file__).resolve().parents[2]
    subprocess.run([str(binary), '-shellcheck=', '-pyflakes=',
        str(root / '.github/workflows/playground-distribute.yml'),
        str(root / '.github/workflows/playground-distribution-check.yml')], check=True, cwd=root)
print('Pinned Actionlint validated both workflow schemas and expression contexts.')
