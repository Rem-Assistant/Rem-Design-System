#!/usr/bin/env python3
"""Map private build/sign/upload logs to a fixed, public failure class.

The private RemClaw logs live only under $RUNNER_TEMP and may contain source paths, symbol names,
compiler diagnostics, account details or provider payloads. This classifier reads them and prints
exactly one line built from fixed tokens and integers, for example:

    classification=COMPILE dependency=0 provisioning=0 signing=0 compile=3 auth=0 upload=0 lines=1234

It never prints, quotes, hashes or summarizes any log text, file name or path. Unreadable or missing
input counts as zero lines and classifies as UNKNOWN. Usage:

    classify_failure.py [--record CODE_FILE] LOG [LOG ...]

--record also writes the bare classification code (one of CODES) to CODE_FILE.
"""
import re
import sys

# Priority order: the first category with a match is the classification. Each log line counts towards
# at most one category (the first that matches), so counts are disjoint.
CATEGORIES = (
    ('DEPENDENCY', (
        r'Could not resolve package dependencies',
        r'xcodebuild: error: Could not resolve',
        r'Permission denied \(publickey\)',
        r'Repository not found',
        r'could not read Username',
        r'fatal: (unable to access|could not read from remote repository)',
        r'Host key verification failed',
        r'Bundler::\w+Error',
        r'Could not find gem',
        r'Gem::\w*(Error|Exception)',
        r'failed to (clone|fetch) ',
        # Android (Gradle) dependency resolution
        r'Could not resolve (all |com\.|org\.|androidx\.|io\.|net\.)',
        r"Could not (GET|HEAD|download) '?https?://",
        r'Plugin \[id: .*\] was not found',
    )),
    ('PROVISIONING', (
        r'error:.*provisioning profile',
        r'requires a provisioning profile',
        r'No profiles for ',
        r'Provisioning profile .*(doesn\'t|does not|has expired|is not)',
        r'doesn\'t (include|match) the .*entitlement',
        r'No matching provisioning profiles',
    )),
    ('SIGNING', (
        r'No signing certificate',
        r'Signing certificate .*not found',
        r'errSec[A-Za-z]+',
        r'CSSMERR_[A-Z_]+',
        r'(code ?sign|codesign)\S*:? .*(fail|error|invalid)',
        r'error:.*(code ?sign|signing identity|certificate)',
        r'SecKeychain\w+',
        # Android upload-key signing
        r'jarsigner(:| error)',
        r'keytool error',
    )),
    ('COMPILE', (
        r'\*\* (ARCHIVE|BUILD) FAILED \*\*',
        r'\.(swift|m|mm|c|cc|cpp|h|hpp|metal|xib|storyboard|xcassets|plist):\d+(:\d+)?: (fatal )?error:',
        r'Command (SwiftCompile|SwiftEmitModule|CompileSwiftSources|CompileC|Ld|CompileAssetCatalog\w*) failed',
        r'[Ll]inker command failed',
        r'^ld: ',
        r'Undefined symbols? for architecture',
        # Android (Kotlin/Gradle) build
        r'^e: (file://)?\S+\.kts?:',
        r'Compilation error\. See log',
        r'FAILURE: Build failed with an exception',
        r'Release build refused:',
    )),
    ('AUTH', (
        r'NOT_AUTHORIZED',
        r'Authentication credentials are missing or invalid',
        r'\b401\b.*(Unauthorized|NOT_AUTHORIZED)',
        r'FORBIDDEN_ERROR',
        r'invalid (API key|token|issuer)',
        r'Apple API failed \(HTTP (401|403)\)',
        # Google Play
        r'Google::Apis::AuthorizationError',
        r'The caller does not have permission',
        r'invalid_grant|unauthorized_client',
        r'Play credential is not a service-account key',
    )),
    ('UPLOAD', (
        r'ITMS-\d+',
        r'ENTITY_ERROR',
        r'(bundle version|CFBundleVersion).*(must be higher|has already been used)',
        r'Redundant Binary Upload',
        r'Error (uploading|while uploading)',
        r'(altool|iTMSTransporter|Transporter).*(error|fail)',
        r'[Uu]pload.*failed',
        r'processing (timed out|failed)',
        r'Apple API failed \(HTTP 5\d\d\)',
        # Google Play
        r'Google Api Error',
        r'Google::Apis::(ClientError|ServerError)',
        r'[Vv]ersion code \d+ has already been used',
        r'Only releases with status draft may be created on draft app',
        r'Play internal release readback missing',
    )),
)
# POLICY marks a trusted-control rejection (input, ancestry, environment, metadata) rather than a tool log.
CODES = tuple(code for code, _ in CATEGORIES) + ('POLICY', 'UNKNOWN')
COMPILED = tuple((code, re.compile('|'.join(f'(?:{p})' for p in patterns), re.IGNORECASE))
                 for code, patterns in CATEGORIES)
# Bound work on hostile or runaway logs without reading unbounded data into memory.
MAX_LINE = 64 * 1024
MAX_LINES = 5_000_000


def classify_lines(lines):
    counts = {code: 0 for code, _ in CATEGORIES}
    total = 0
    for line in lines:
        if total >= MAX_LINES:
            break
        total += 1
        line = line[:MAX_LINE]
        for code, pattern in COMPILED:
            if pattern.search(line):
                counts[code] += 1
                break
    classification = next((code for code, _ in CATEGORIES if counts[code]), 'UNKNOWN')
    return classification, counts, total


def classify_files(paths):
    def lines():
        for path in paths:
            try:
                with open(path, 'r', encoding='utf-8', errors='replace') as handle:
                    for line in handle:
                        yield line
            except OSError:
                continue
    return classify_lines(lines())


def render(classification, counts, total):
    """The only public output: fixed tokens and integers."""
    assert classification in CODES
    fields = ' '.join(f'{code.lower()}={int(counts[code])}' for code, _ in CATEGORIES)
    return f'classification={classification} {fields} lines={int(total)}'


def main(argv):
    args = list(argv[1:])
    record = None
    if args[:1] == ['--record']:
        require_record = len(args) >= 2
        record = args[1] if require_record else None
        args = args[2:]
    classification, counts, total = classify_files(args)
    print(render(classification, counts, total))
    if record:
        # The fixed code only, for the allowlisted step summary.
        with open(record, 'w') as handle:
            handle.write(classification + '\n')
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main(sys.argv))
    except Exception:
        # Even an internal error must not surface paths or content.
        print('classification=UNKNOWN')
        sys.exit(0)
