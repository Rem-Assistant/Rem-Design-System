# Archived Settings source evidence

[source-evidence.zip](source-evidence.zip) preserves all 55 generated text contexts and 53 Figma target screenshots previously stored under this directory's `raw/` and `screenshots/` subdirectories. Each entry retains its full original repository-relative path and exact bytes. The [manifest](source-evidence-manifest.json) records every original path, byte size and SHA-256, plus the archive's size and SHA-256 and the source commit.

These captures are design reference evidence, not application assets or executable code. Source code, contracts, metadata, provider-brand exports and runtime resources remain ordinary files. CI exports its canonical Figma references separately through `tools/design-sync` and `tools/render-evidence`; it does not consume this archive. Archiving reduces the number of individual generated files in the PR diff without changing reviewer configuration, path coverage or the review byte limit. It does not mean the archive contents have been reviewed by the automated code reviewer.

## Verify and extract

Run this from the repository root with Python 3. It verifies the archive and every entry before writing anything, then extracts to a new temporary directory. It never overwrites files in the checkout. Open the printed directory and follow the original `docs/playground/settings-design/raw/` and `screenshots/` paths. IDs use hyphens in filenames, for example `1833-5048.txt` and `1833-5048.png`.

```sh
python3 - <<'PY'
import hashlib, json, stat, tempfile, zipfile
from pathlib import Path, PurePosixPath

manifest = json.loads(Path('docs/playground/settings-design/source-evidence-manifest.json').read_text())
archive = Path(manifest['archive']['path'])
assert archive == Path('docs/playground/settings-design/source-evidence.zip')
data = archive.read_bytes()
assert len(data) == manifest['archive']['size_bytes']
assert hashlib.sha256(data).hexdigest() == manifest['archive']['sha256']
expected = {item['path']: item for item in manifest['files']}
assert len(expected) == len(manifest['files']) == manifest['file_count']
prefix = PurePosixPath('docs/playground/settings-design')
verified = {}
with zipfile.ZipFile(archive) as bundle:
    assert bundle.namelist() == sorted(expected)
    for entry in bundle.infolist():
        path = PurePosixPath(entry.filename)
        assert path.parts[:3] == prefix.parts and len(path.parts) == 5
        assert '..' not in path.parts and not path.is_absolute()
        assert (path.parts[3], path.suffix) in {('raw', '.txt'), ('screenshots', '.png')}
        assert stat.S_ISREG(entry.external_attr >> 16)
        content = bundle.read(entry)
        item = expected[entry.filename]
        assert len(content) == item['size_bytes']
        assert hashlib.sha256(content).hexdigest() == item['sha256']
        verified[path] = content
assert sum(map(len, verified.values())) == manifest['total_uncompressed_bytes']
destination = Path(tempfile.mkdtemp(prefix='rem-settings-source-evidence-'))
for path, content in verified.items():
    output = destination / path
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open('xb') as stream:
        stream.write(content)
print(f'Verified and extracted {len(verified)} files to {destination}')
PY
```

## Deterministic packaging

Entries are sorted by full repository-relative path and use a fixed DOS timestamp of `1980-01-01 00:00:00`, Unix regular-file mode `100644`, and no extra fields, per-entry comments or archive comment. `ZIP_STORED` deliberately avoids compression-library version differences. The archive is about 3.4 MB; the purpose is a single lossless reference bundle rather than compression. The manifest stays outside the ZIP so its archive checksum is not self-referential.

To independently reproduce the archive from the original Git blobs, run the following from a checkout containing the manifest's source commit. It creates no files and compares the rebuilt bytes with the archive checksum:

```sh
python3 - <<'PY'
import hashlib, io, json, stat, subprocess, zipfile
from pathlib import Path

manifest = json.loads(Path('docs/playground/settings-design/source-evidence-manifest.json').read_text())
buffer = io.BytesIO()
with zipfile.ZipFile(buffer, 'w', compression=zipfile.ZIP_STORED) as bundle:
    for item in sorted(manifest['files'], key=lambda item: item['path']):
        content = subprocess.check_output(['git', 'show', manifest['source_commit'] + ':' + item['path']])
        assert len(content) == item['size_bytes']
        assert hashlib.sha256(content).hexdigest() == item['sha256']
        entry = zipfile.ZipInfo(item['path'], date_time=(1980, 1, 1, 0, 0, 0))
        entry.create_system = 3
        entry.external_attr = (stat.S_IFREG | 0o644) << 16
        entry.compress_type = zipfile.ZIP_STORED
        bundle.writestr(entry, content)
assert len(buffer.getvalue()) == manifest['archive']['size_bytes']
assert hashlib.sha256(buffer.getvalue()).hexdigest() == manifest['archive']['sha256']
print('Archive reproduced exactly from the original Git blobs.')
PY
```
