# PR size

`check_pr_size.py` fails a pull request that changes more than **150 files** or **750,000 bytes** of
diff, measured like GitHub: the head against its merge base with the base branch. The
`PR size` workflow (`.github/workflows/pr-size.yml`) runs it on every pull request.

Why: the agent reviewer reads at most 300 files and 750,000 bytes of patch, and people review small
PRs better. Limits set by Samuel on 2026-10-11.

Over the limit? Split the work into stacked PRs: each PR targets the previous one's branch, builds and
passes its tests on its own, and lands in order. Keep binary assets in their own small PR.

Tests: `python3 -m unittest discover -s tools/pr-size -p 'test_*.py'`.
