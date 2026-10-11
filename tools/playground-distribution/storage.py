#!/usr/bin/env python3
"""Runner storage guard: numeric free-space checks, output budgets and bounded cleanup.

Standard hosted runners only. When capacity is insufficient the job stops before the stage starts;
it never erases system SDKs, simulators or tool caches and never falls back to a larger paid runner.
Cleanup only removes named intermediates strictly inside a given base directory.
"""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
from gate import require

MiB = 1024 ** 2
GiB = 1024 ** 3

STAGES = ('dependencies', 'build', 'sign', 'upload')

# Minimum free bytes on the output volume before each stage starts. Conservative for a small
# Playground app; documented standard macOS hosted runners list 14 GB SSD, so iOS stays well below it.
MIN_FREE = {
    'ios': {'dependencies': 2 * GiB, 'build': 6 * GiB, 'sign': 2 * GiB, 'upload': 1 * GiB},
    'android': {'dependencies': 2 * GiB, 'build': 5 * GiB, 'sign': 1 * GiB, 'upload': 1 * GiB},
}

# Maximum bytes kept in the package output after each stage's cleanup. After signing only
# packaging.json and the single signed binary remain.
OUTPUT_BUDGET = {
    'ios': {'build': 2 * GiB, 'sign': 1 * GiB},
    'android': {'build': 512 * MiB, 'sign': 512 * MiB},
}


def gib(n):
    return f'{n / GiB:.1f} GiB'


def require_free(path, platform, stage, usage=None):
    require(platform in MIN_FREE and stage in MIN_FREE[platform], 'Unknown storage stage')
    free, needed = (usage or shutil.disk_usage)(path).free, MIN_FREE[platform][stage]
    require(free >= needed, f'Insufficient runner storage before {stage}: {gib(free)} free, '
            f'{gib(needed)} required. Stopped safely; nothing was deleted and no larger runner was used.')
    return free


def tree_size(path):
    """Bytes in regular files under path, counting symlinks themselves and never following them."""
    path = Path(path)
    if path.is_symlink() or path.is_file():
        return path.lstat().st_size
    total = 0
    for top, dirs, files in os.walk(path, followlinks=False):
        for name in files + [d for d in dirs if (Path(top) / d).is_symlink()]:
            total += (Path(top) / name).lstat().st_size
    return total


def require_budget(out, platform, stage):
    require(platform in OUTPUT_BUDGET and stage in OUTPUT_BUDGET[platform], 'Unknown output budget')
    size, budget = tree_size(out), OUTPUT_BUDGET[platform][stage]
    require(size <= budget, f'Package output after {stage} is {gib(size)}, over its {gib(budget)} budget')
    return size


def remove_within(base, target):
    """Remove one intermediate strictly inside base. Refuses base itself, escapes and symlinked parents."""
    base = Path(base).resolve(strict=True)
    target = Path(target)
    require(target.name not in ('', '.') and '..' not in target.parts, 'Invalid cleanup target')
    # The parent is resolved (symlinks followed); the target itself is not, so a symlink is unlinked
    # rather than followed.
    try:
        parent = target.parent.resolve(strict=True)
    except OSError:
        raise ValueError('Cleanup target parent does not exist')
    require(parent == base or base in parent.parents, 'Cleanup target escapes its base')
    if target.is_symlink():
        target.unlink()
    elif target.is_dir():
        shutil.rmtree(target)
    elif target.exists():
        target.unlink()


def remove_ignored_build_output(root, relative):
    """Remove a candidate build directory only if git confirms it is untracked, ignored output."""
    target = Path(root) / relative
    if not target.exists():
        return
    ignored = subprocess.run(['git', '-C', str(root), 'check-ignore', '-q', '--', relative], capture_output=True)
    require(ignored.returncode == 0, 'Refuse to remove a build path that git does not ignore')
    tracked = subprocess.run(['git', '-C', str(root), 'ls-files', '--', relative], capture_output=True)
    require(tracked.returncode == 0 and not tracked.stdout.strip(), 'Refuse to remove tracked files')
    remove_within(root, target)


def main():
    p = argparse.ArgumentParser()
    p.add_argument('mode', choices=['check'])
    p.add_argument('--path', type=Path, required=True)
    p.add_argument('--platform', choices=sorted(MIN_FREE), required=True)
    p.add_argument('--stage', choices=STAGES, required=True)
    a = p.parse_args()
    free = require_free(a.path, a.platform, a.stage)
    print(f'Storage check passed before {a.stage}: {gib(free)} free, '
          f'{gib(MIN_FREE[a.platform][a.stage])} required.')


if __name__ == '__main__':
    try:
        main()
    except ValueError as error:
        raise SystemExit('Storage guard stopped the release: ' + str(error))
