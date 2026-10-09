#!/usr/bin/env python3
"""Local evidence binding. No uploads, credential management, or latest pointer."""
import argparse
import hashlib
import json
import os
import plistlib
import re
import subprocess
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FEATURES = ["settings", "onboarding-voice"]
IDS = {"ios": "com.rem.playground.settings", "android": "com.rem.designsystem.demo"}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def digest(path):
    h = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def source(sha):
    require(re.fullmatch(r"[0-9a-f]{40}", sha), "Require full immutable source SHA")
    actual = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    require(actual == sha, "Source SHA mismatch")
    dirty = subprocess.check_output(["git", "status", "--porcelain", "--untracked-files=all"], cwd=ROOT, text=True)
    # Name the offending paths so a rejected run is diagnosable; the rejection itself is unchanged.
    lines = dirty.splitlines()
    listed = "\n".join(lines[:50]) + (f"\n… and {len(lines) - 50} more" if len(lines) > 50 else "")
    require(not dirty, "Require a clean isolated checkout; unexpected changes:\n" + listed)


def expected_tests(platform):
    if platform == "ios":
        files = (ROOT / "tools/playground-ios/UITests").glob("*.swift")
        pattern = r"func\s+(test\w+)\s*\("
    else:
        files = (ROOT / "compose/demo/src/androidTest").rglob("*.kt")
        pattern = r"@Test\s+fun\s+(\w+)\s*\("
    result = {f"{p.stem}/{name}" for p in files for name in re.findall(pattern, p.read_text())}
    require(result, "No native tests discovered")
    return result


def parse_ios(text):
    # XCTest's serial, unfiltered test output from run-hosted-journeys.sh.
    rows = re.findall(r"Test Case '-\[(?:\w+\.)?(\w+) (\w+)\]' (passed|failed|skipped)", text)
    require("** TEST SUCCEEDED **" in text, "No successful XCTest completion")
    require(rows, "No XCTest method results found")
    require(all(state == "passed" for _, _, state in rows), "Failed or skipped XCTest method")
    names = [f"{suite}/{name}" for suite, name, _ in rows]
    require(len(names) == len(set(names)), "Repeated XCTest methods; retries are not readiness proof")
    return set(names)


def shard_tests(platform, index, count):
    """Deterministic shard of the expected method set: sorted, then round-robin, so every method
    lands in exactly one shard and slow suites spread across shards."""
    require(1 <= count <= 8 and 0 <= index < count, "Invalid shard index or count")
    return set(sorted(expected_tests(platform))[index::count])


def parse_ios_shards(texts):
    """Every shard log must be a successful XCTest run with only passing, unrepeated methods, and no
    method may appear in two shards; the caller still requires the union to equal the source set."""
    require(texts, "No XCTest shard logs")
    methods = set()
    for text in texts:
        shard = parse_ios(text)
        require(not (shard & methods), "A method ran in more than one shard; retries are not readiness proof")
        methods |= shard
    return methods


def parse_android(files):
    names = []
    require(files, "No Android instrumentation XML reports")
    for path in files:
        root = ET.parse(path).getroot()
        require(not root.findall(".//failure") and not root.findall(".//error") and not root.findall(".//skipped"), "Failed or skipped Android test")
        for case in root.iter("testcase"):
            names.append(f"{case.attrib['classname'].rsplit('.', 1)[-1]}/{case.attrib['name']}")
    require(names and len(names) == len(set(names)), "Empty or duplicate Android test results")
    return set(names)


def validate_tests(record, sha, platform):
    require(record.get("source_sha") == sha, "Stale native test source SHA")
    require(record.get("platform") == platform, "Native test platform mismatch")
    require(record.get("status") == "passed" and record.get("unfiltered") is True, "Native tests missing, skipped, filtered, or failed")
    expected = expected_tests(platform)
    actual = record.get("passed_methods", [])
    require(len(actual) == len(set(actual)) and set(actual) == expected, "Native method set differs from current source")
    require(record.get("passed_count") == len(expected), "Native test count mismatch")
    require(record.get("features") == FEATURES, "Feature scope mismatch")
    execution = record.get("execution", {})
    require(execution.get("provider") == "github_actions" and execution.get("workflow_sha") == sha,
            "Missing exact-revision native execution provenance")
    require(str(execution.get("run_id", "")).isdigit() and str(execution.get("run_attempt", "")).isdigit(),
            "Missing native run identity")




def hosted_execution(sha):
    require(os.environ.get("GITHUB_ACTIONS") == "true", "Require hosted native execution")
    event_name = os.environ.get("GITHUB_EVENT_NAME")
    event_sha = os.environ.get("GITHUB_SHA", "")
    require(re.fullmatch(r"[0-9a-f]{40}", event_sha), "Missing workflow event SHA")
    if event_name == "pull_request":
        event = json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text())
        pr = event["pull_request"]
        require(pr["head"]["sha"] == sha, "Native source differs from immutable PR event head")
        require(pr["head"]["repo"]["full_name"] == os.environ.get("GITHUB_REPOSITORY"),
                "Require same-repository candidate")
        require(os.environ.get("GITHUB_REF") == f"refs/pull/{event['number']}/merge",
                "Unexpected PR workflow ref")
    else:
        require(event_name == "workflow_dispatch" and event_sha == sha,
                "Record native results only in the exact-revision hosted workflow")
    execution = {"provider": "github_actions", "workflow_sha": sha,
                 "event_name": event_name, "event_sha": event_sha,
                 "run_id": os.environ.get("GITHUB_RUN_ID", ""),
                 "run_attempt": os.environ.get("GITHUB_RUN_ATTEMPT", "")}
    require(execution["run_id"].isdigit() and execution["run_attempt"].isdigit(), "Missing native run identity")
    return execution


def inspect_artifact(platform, artifact, sha):
    artifact = Path(artifact)
    if platform == "ios":
        info = plistlib.loads((artifact / "Info.plist").read_bytes())
        metadata = {"application_id": info.get("CFBundleIdentifier"),
                    "version": info.get("CFBundleShortVersionString"), "version_code": info.get("CFBundleVersion"),
                    "source_sha": info.get("RemPlaygroundSourceSHA")}
    else:
        sdk = Path(os.environ["ANDROID_HOME"])
        analyzer = sdk / "cmdline-tools/latest/bin/apkanalyzer"
        xml = subprocess.check_output([str(analyzer), "manifest", "print", str(artifact)], text=True)
        root = ET.fromstring(xml)
        ns = "{http://schemas.android.com/apk/res/android}"
        stamps = [n.get(ns + "value") for n in root.findall("application/meta-data") if n.get(ns + "name") == "RemPlaygroundSourceSHA"]
        require(len(stamps) == 1, "Missing or duplicate APK source stamp")
        require(stamps[0] is not None and stamps[0].startswith("git:"), "Invalid APK source stamp encoding")
        metadata = {"application_id": root.get("package"), "version": root.get(ns + "versionName"),
                    "version_code": root.get(ns + "versionCode"), "source_sha": stamps[0][4:]}
    require(metadata["application_id"] == IDS[platform], "Unexpected binary application ID")
    require(metadata["source_sha"] == sha, "Binary source stamp mismatch")
    require(metadata["version"] and metadata["version_code"], "Missing binary version")
    return metadata


def validate_manifest(record, sha, platform, binary):
    require(record.get("source_sha") == sha and record.get("build_sha") == sha and record.get("platform") == platform,
            "Stale or wrong-platform artifact")
    require(record.get("application_id") == IDS[platform] and record.get("features") == FEATURES, "App or scope mismatch")
    validate_tests(record["native_tests"], sha, platform)
    require(record.get("binary_sha256") == digest(binary), "Binary hash mismatch")


def write_json(path, obj):
    # Never silently overwrite an older manifest or report.
    with Path(path).open("x") as stream:
        json.dump(obj, stream, indent=2)
        stream.write("\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    pre = sub.add_parser("preflight")
    pre.add_argument("--sha", required=True)
    for name in ("record-tests", "check-tests", "manifest", "verify", "inspect", "shard", "check-shard"):
        p = sub.add_parser(name)
        p.add_argument("--sha", required=True)
        p.add_argument("--platform", choices=IDS, required=True)
        if name == "inspect":
            p.add_argument("--artifact", required=True)
            p.add_argument("--output", required=True)
        elif name in ("shard", "check-shard"):
            p.add_argument("--index", type=int, required=True)
            p.add_argument("--count", type=int, required=True)
            if name == "check-shard":
                p.add_argument("--report", required=True)
        elif name == "record-tests":
            # iOS may pass one log per shard; Android passes its report directory.
            p.add_argument("--report", required=True, nargs="+")
            p.add_argument("--toolchain", required=True)
            p.add_argument("--output", required=True)
        elif name == "check-tests":
            p.add_argument("--tests", required=True)
        elif name == "manifest":
            p.add_argument("--metadata", required=True)
            p.add_argument("--tests", required=True)
            p.add_argument("--binary", required=True)
            p.add_argument("--toolchain", required=True)
            p.add_argument("--output", required=True)
        else:
            p.add_argument("--manifest", required=True)
            p.add_argument("--binary", required=True)
    args = parser.parse_args()
    source(args.sha)
    if args.command == "preflight":
        return
    if args.command == "inspect":
        write_json(args.output, inspect_artifact(args.platform, args.artifact, args.sha))
    elif args.command == "shard":
        print(",".join(sorted(shard_tests(args.platform, args.index, args.count))))
    elif args.command == "check-shard":
        require(args.platform == "ios", "Only iOS runs in shards")
        require(parse_ios(Path(args.report).read_text()) == shard_tests(args.platform, args.index, args.count),
                "Shard ran an incomplete or unexpected method set")
    elif args.command == "record-tests":
        execution = hosted_execution(args.sha)
        if args.platform == "ios":
            files = [Path(r) for r in args.report]
            methods = parse_ios_shards([f.read_text() for f in files])
        else:
            require(len(args.report) == 1, "Android records one report directory")
            files = sorted(Path(args.report[0]).rglob("TEST-*.xml"))
            methods = parse_android(files)
        require(methods == expected_tests(args.platform), "Incomplete or unexpected native method set")
        record = {"source_sha": args.sha, "platform": args.platform, "features": FEATURES,
                  "status": "passed", "unfiltered": True, "execution": execution, "passed_count": len(methods),
                  "passed_methods": sorted(methods), "recorded_at": datetime.now(timezone.utc).isoformat(),
                  "report_sha256": [digest(p) for p in files], "toolchain": Path(args.toolchain).read_text()}
        write_json(args.output, record)
    elif args.command in ("check-tests", "manifest"):
        tests = json.loads(Path(args.tests).read_text())
        validate_tests(tests, args.sha, args.platform)
        if args.command == "manifest":
            binary = Path(args.binary)
            require(binary.is_file(), "Binary missing")
            metadata = json.loads(Path(args.metadata).read_text())
            require(metadata["source_sha"] == args.sha and metadata["application_id"] == IDS[args.platform], "Wrong binary metadata")
            write_json(args.output, {"schema_version": 1, "source_sha": args.sha,
                "platform": args.platform, "application_id": IDS[args.platform], "features": FEATURES,
                "version": metadata["version"], "version_code": metadata["version_code"], "build_sha": args.sha,
                "binary_name": binary.name, "binary_sha256": digest(binary),
                "toolchain": Path(args.toolchain).read_text(), "native_tests": tests,
                "device_verification": {"status": "not_verified"},
                "readiness": "native_tested_device_unverified", "distribution": "private_local_only"})
    else:
        record = json.loads(Path(args.manifest).read_text())
        validate_manifest(record, args.sha, args.platform, args.binary)
        print("Artifact and native tests match exact source; device verification:", record.get("device_verification"))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, OSError) as error:
        raise SystemExit(str(error))
