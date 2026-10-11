#!/usr/bin/env python3
"""PR size limit: a pull request changes at most 150 files and 750,000 bytes of diff.

Large PRs cannot be reviewed whole: the agent reviewer fetches at most 300 files and 750,000 bytes of
patch, and people review small PRs better. Split work into stacked PRs instead (see README.md).

  check_pr_size.py BASE_SHA HEAD_SHA
"""
import subprocess
import sys
from collections import Counter

MAX_FILES = 150
MAX_DIFF_BYTES = 750_000


def git(*args):
    return subprocess.run(["git", *args], check=True, capture_output=True).stdout


def measure(base, head):
    """Files and diff bytes of the PR as GitHub shows it: head against its merge base with base."""
    files = [f for f in git("diff", "--name-only", f"{base}...{head}").decode().splitlines() if f]
    diff_bytes = len(git("diff", f"{base}...{head}"))
    return files, diff_bytes


def problems(files, diff_bytes):
    found = []
    if len(files) > MAX_FILES:
        found.append(f"{len(files)} changed files (limit {MAX_FILES})")
    if diff_bytes > MAX_DIFF_BYTES:
        found.append(f"{diff_bytes:,} bytes of diff (limit {MAX_DIFF_BYTES:,})")
    return found


def largest_areas(files, depth=2, top=10):
    return Counter("/".join(f.split("/")[:depth]) for f in files).most_common(top)


def main(argv):
    if len(argv) != 2:
        print(__doc__, file=sys.stderr)
        return 2
    files, diff_bytes = measure(*argv)
    print(f"{len(files)} changed files, {diff_bytes:,} bytes of diff")
    found = problems(files, diff_bytes)
    if not found:
        return 0
    print("PR too large: " + "; ".join(found) + ". Split it into stacked PRs.", file=sys.stderr)
    print("Largest areas:", file=sys.stderr)
    for area, count in largest_areas(files):
        print(f"  {count:4d}  {area}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
