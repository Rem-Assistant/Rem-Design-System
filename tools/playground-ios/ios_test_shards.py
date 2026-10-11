#!/usr/bin/env python3
"""iOS journey shards: one inventory, disjoint and exhaustive partitions, and fail-closed result checks.

The inventory is every `func testX()` of every `final class XUITests: XCTestCase` in UITests/. Each hosted
shard checks it against what Xcode actually discovered (`xcodebuild -enumerate-tests`), runs only its own
partition, and checks that every selected test ran to a pass or a failure (never skipped, never missing).
The aggregate job checks that the shards together ran the whole inventory, each test exactly once.

  ios_test_shards.py inventory
  ios_test_shards.py shard --count N --index I
  ios_test_shards.py check-enumeration ENUMERATION.json
  ios_test_shards.py check-results BUILD.log --count N --index I
  ios_test_shards.py check-aggregate --count N BUILD.log...
"""
import argparse
import json
import re
import sys
from pathlib import Path

TARGET = "RemSettingsPlaygroundUITests"
UITESTS = Path(__file__).resolve().parent / "UITests"
CLASS = re.compile(r"^final class ([A-Za-z]+UITests): XCTestCase \{", re.M)
METHOD = re.compile(r"^    func (test[A-Za-z0-9_]+)\(\)", re.M)
RESULT = re.compile(r"Test Case '-\[" + TARGET + r"\.([A-Za-z]+UITests) (test[A-Za-z0-9_]+)\]' (started|passed|failed|skipped)")
EXECUTED = re.compile(r"Test Suite 'Selected tests' (?:passed|failed) at .*\n\s+Executed (\d+) tests?,")


def inventory(root=UITESTS):
    """Class/method for every test in UITests/, in a stable order. One XCTestCase class per file, and only
    its top-level instance methods (4-space indent, no access modifier) count: that is what XCTest runs."""
    tests = []
    for path in sorted(root.glob("*.swift")):
        source = path.read_text(encoding="utf-8")
        classes = CLASS.findall(source)
        if not classes:
            continue
        if len(classes) != 1:
            raise SystemExit(f"{path.name}: expected one XCTestCase class, found {classes}")
        tests += [f"{classes[0]}/{method}" for method in METHOD.findall(source)]
    if len(tests) != len(set(tests)):
        raise SystemExit("duplicate test identifiers in the inventory")
    return sorted(tests)


def shard(tests, count, index):
    """Round-robin over the sorted inventory: disjoint, exhaustive and balanced by test count."""
    if count < 1 or not 0 <= index < count:
        raise SystemExit(f"invalid shard {index} of {count}")
    return tests[index::count]


def enumerated(document):
    """Enabled and disabled test identifiers from `xcodebuild -enumerate-tests` JSON, whatever its nesting."""
    enabled, disabled, ungrouped = set(), set(), set()

    def walk(node, bucket):
        if isinstance(node, dict):
            for key, value in node.items():
                lowered = key.lower()
                walk(value, disabled if "disabled" in lowered else enabled if "enabled" in lowered else bucket)
        elif isinstance(node, list):
            for value in node:
                walk(value, bucket)
        elif isinstance(node, str):
            match = re.fullmatch(TARGET + r"/([A-Za-z]+UITests)/(test[A-Za-z0-9_]+)(?:\(\))?", node)
            if match:
                bucket.add(f"{match.group(1)}/{match.group(2)}")

    walk(document, ungrouped)
    # A format without enabled/disabled grouping lists only runnable tests.
    if not enabled and not disabled:
        enabled = ungrouped
    return enabled, disabled


def check_enumeration(document, tests):
    enabled, disabled = enumerated(document)
    problems = []
    if disabled:
        problems.append(f"disabled tests: {sorted(disabled)}")
    if not enabled:
        problems.append("Xcode enumerated no enabled tests")
    if enabled - set(tests):
        problems.append(f"discovered by Xcode but missing from the inventory: {sorted(enabled - set(tests))}")
    if set(tests) - enabled:
        problems.append(f"in the inventory but not discovered by Xcode: {sorted(set(tests) - enabled)}")
    return problems


def outcomes(log):
    """{Class/method: [states]} from an xcodebuild test log."""
    seen = {}
    for cls, method, state in RESULT.findall(log):
        seen.setdefault(f"{cls}/{method}", []).append(state)
    return seen


def check_results(log, selected):
    seen = outcomes(log)
    problems = []
    for test in selected:
        states = seen.get(test, [])
        finished = [s for s in states if s in ("passed", "failed")]
        if "started" not in states:
            problems.append(f"{test}: did not start")
        elif "skipped" in states:
            problems.append(f"{test}: skipped")
        elif len(finished) != 1:
            problems.append(f"{test}: no single pass/fail result ({states})")
    unexpected = sorted(set(seen) - set(selected))
    if unexpected:
        problems.append(f"ran tests outside this shard: {unexpected}")
    executed = [int(n) for n in EXECUTED.findall(log)]
    if executed and executed[-1] != len(selected):
        problems.append(f"Xcode executed {executed[-1]} tests; the shard selected {len(selected)}")
    return problems


def check_aggregate(logs, tests):
    counts = {}
    for log in logs:
        for test, states in outcomes(log).items():
            counts[test] = counts.get(test, 0) + states.count("started")
    problems = [f"{test}: started {n} times across shards" for test, n in sorted(counts.items()) if n != 1]
    missing = sorted(set(tests) - set(counts))
    extra = sorted(set(counts) - set(tests))
    if missing:
        problems.append(f"never ran: {missing}")
    if extra:
        problems.append(f"ran but not in the inventory: {extra}")
    return problems


def main(argv=None):
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("inventory")
    for name in ("shard", "check-results"):
        p = sub.add_parser(name)
        if name == "check-results":
            p.add_argument("log", type=Path)
        p.add_argument("--count", type=int, required=True)
        p.add_argument("--index", type=int, required=True)
    p = sub.add_parser("check-enumeration")
    p.add_argument("enumeration", type=Path)
    p = sub.add_parser("check-aggregate")
    p.add_argument("--count", type=int, required=True)
    p.add_argument("logs", type=Path, nargs="+")
    args = parser.parse_args(argv)

    tests = inventory()
    if args.command == "inventory":
        print("\n".join(tests))
        return 0
    if args.command == "shard":
        print(",".join(shard(tests, args.count, args.index)))
        return 0
    if args.command == "check-enumeration":
        problems = check_enumeration(json.loads(args.enumeration.read_text(encoding="utf-8")), tests)
    elif args.command == "check-results":
        selected = shard(tests, args.count, args.index)
        problems = check_results(args.log.read_text(encoding="utf-8", errors="replace"), selected)
        if not problems:
            print(f"shard {args.index + 1} of {args.count}: {len(selected)} selected, all ran to a result")
    else:
        if len(args.logs) != args.count:
            problems = [f"expected {args.count} shard logs, found {len(args.logs)}"]
        else:
            problems = check_aggregate([p.read_text(encoding="utf-8", errors="replace") for p in args.logs], tests)
        if not problems:
            print(f"{len(tests)} inventory tests ran exactly once across {args.count} shards")
    for problem in problems:
        print(problem, file=sys.stderr)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
