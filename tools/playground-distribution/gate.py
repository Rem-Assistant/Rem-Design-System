#!/usr/bin/env python3
"""Read-only, secret-free admission for private Playground distribution."""
import argparse
import ast
import hashlib
import io
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

REPO = 'Rem-Assistant/Rem-Design-System'
WORKFLOW = '.github/workflows/playground-native-candidate.yml'
PLATFORMS = ('ios', 'android')
# Existing repository administrators verified through GitHub's collaborator API.
# Supporting an identity here does not assign it or grant repository access.
REVIEWERS = {58840187: 'samuelalake', 62378296: 'oledibefrancis', 74985099: 'davidolaniran'}
OWNER_ID = 58840187


def same_repository(value):
    # GitHub repository slugs are case-insensitive; branch refs are not.
    return isinstance(value, str) and value.casefold() == REPO.casefold()


def require(ok, message):
    if not ok:
        raise ValueError(message)


def arguments(sha, run_id, build):
    require(re.fullmatch(r'[0-9a-f]{40}', sha or ''), 'Full lowercase source SHA required')
    require(re.fullmatch(r'[1-9][0-9]*', str(run_id)), 'Numeric native run ID required')
    require(re.fullmatch(r'[1-9][0-9]{0,8}', str(build)), 'Build number must be a positive integer below 1 billion')


def visual_review(url):
    # A locator for human visual acceptance, never a claim that tests imply visual approval.
    require(re.fullmatch(r'https://(?i:github\.com/Rem-Assistant/Rem-Design-System)/pull/[1-9][0-9]*'
                         r'#(?:pullrequestreview|issuecomment)-[1-9][0-9]*', url or '', flags=re.ASCII),
            'Provide a specific review/comment URL in this repository')
    return url


def validate_run(run, jobs, sha, run_id):
    require(str(run['id']) == str(run_id), 'Run identity mismatch')
    require(same_repository(run['repository']['full_name']) and same_repository(run['head_repository']['full_name']),
            'Foreign repository or fork')
    require(run['head_sha'] == sha, 'Native run source mismatch')
    require(run['path'] == WORKFLOW, 'Wrong native workflow')
    require(run['event'] in ('workflow_dispatch', 'pull_request'), 'Unexpected native event')
    require(run['status'] == 'completed' and run['conclusion'] == 'success', 'Native run is not successful')
    def passed(job):
        return job['status'] == 'completed' and job['conclusion'] == 'success'
    # The native workflow runs iOS either as one `ios` job or as `ios (N)` shards whose method sets
    # `ios-evidence` merges into the single exact receipt. Accept exactly one form, never a mix.
    shards = sorted(j['name'] for j in jobs if re.fullmatch(r'ios \([0-9]+\)', j['name']))
    ios = ('ios-evidence',) if shards else ('ios',)
    require(shards == sorted(f'ios ({i})' for i in range(len(shards))) and
            all(passed(j) for j in jobs if j['name'] in shards),
            'Missing, duplicate or unsuccessful native iOS shard')
    require(not (shards and any(j['name'] == 'ios' for j in jobs)), 'Mixed sharded and unsharded iOS jobs')
    for name in ('preflight', *ios, 'android', 'native-evidence'):
        matches = [j for j in jobs if j['name'] == name]
        require(len(matches) == 1 and passed(matches[0]), 'Missing, duplicate or unsuccessful native job: ' + name)


def expected_methods(root, platform):
    if platform == 'ios':
        files = (root / 'tools/playground-ios/UITests').glob('*.swift')
        pattern = r'func\s+(test\w+)\s*\('
    else:
        files = (root / 'compose/demo/src/androidTest').rglob('*.kt')
        pattern = r'@Test\s+fun\s+(\w+)\s*\('
    methods = [f'{p.stem}/{n}' for p in files for n in re.findall(pattern, p.read_text())]
    require(methods and len(set(methods)) == len(methods), 'Missing or ambiguous source test inventory')
    return set(methods)


def declared_features(root):
    # Read the candidate contract as data. Never import/execute candidate Python.
    tree = ast.parse((root / 'tools/playground-delivery/delivery.py').read_text())
    values = [ast.literal_eval(n.value) for n in tree.body if isinstance(n, ast.Assign)
              and any(isinstance(t, ast.Name) and t.id == 'FEATURES' for t in n.targets)]
    require(len(values) == 1 and isinstance(values[0], list) and values[0] and
            all(isinstance(x, str) for x in values[0]), 'Missing declared feature contract')
    return values[0]


def validate_receipt(record, run, sha, platform, methods, features):
    require(record.get('source_sha') == sha and record.get('platform') == platform, 'Receipt source/platform mismatch')
    require(record.get('status') == 'passed' and record.get('unfiltered') is True, 'Filtered or failed native receipt')
    actual = record.get('passed_methods', [])
    require(len(actual) == len(set(actual)) and set(actual) == methods and
            record.get('passed_count') == len(methods), 'Incomplete or duplicate native method set')
    require(record.get('features') == features, 'Receipt feature scope mismatch')
    e = record.get('execution', {})
    require(e.get('provider') == 'github_actions' and e.get('workflow_sha') == sha and
            e.get('event_name') == run['event'], 'Receipt execution mismatch')
    require(str(e.get('run_id')) == str(run['id']) and
            str(e.get('run_attempt')) == str(run['run_attempt']), 'Stale run/attempt receipt')
    if run['event'] == 'workflow_dispatch':
        require(e.get('event_sha') == sha, 'Dispatch event SHA mismatch')
    else:
        require(re.fullmatch(r'[0-9a-f]{40}', e.get('event_sha', '')), 'Missing PR event SHA')
        require(any(p['head']['sha'] == sha for p in run.get('pull_requests', [])), 'PR head mismatch')


def validate_environment(environment, policies, initiators):
    reviews = [r for r in environment.get('protection_rules', []) if r['type'] == 'required_reviewers']
    require(len(reviews) == 1 and len(reviews[0].get('reviewers', [])) == 1,
            'Existing environment must require exactly one selected reviewer')
    entry = reviews[0]['reviewers'][0]
    user = entry.get('reviewer', {})
    reviewer = REVIEWERS.get(user.get('id'))
    require(entry.get('type') == 'User' and reviewer and user.get('login', '').casefold() == reviewer,
            'Reviewer must match a verified existing repository identity')
    prevent_self = reviews[0].get('prevent_self_review')
    require(isinstance(prevent_self, bool), 'Explicit self-review policy required')
    if prevent_self:
        require(initiators and all(isinstance(x, str) and x for x in initiators), 'Missing dispatch identity')
        require(reviewer not in {x.casefold() for x in initiators},
                'Selected reviewer initiated this run; choose a reachable reviewer or approved owner self-review')
        model = 'independent'
    else:
        require(user['id'] == OWNER_ID, 'Self-review exception is limited to Samuel as sole reviewer')
        model = 'owner-approval'
    require(environment.get('can_admins_bypass') is False, 'Environment must disable administrator bypass')
    require(environment.get('deployment_branch_policy') == {'protected_branches': False, 'custom_branch_policies': True},
            'Environment must use a selected-branch policy')
    require(policies == [{'name': 'main', 'type': 'branch'}], 'Environment must allow only main (no tags or wildcards)')
    return {'model': model, 'reviewer': reviewer}


def read_receipt_archive(data, artifact, sha, platform, run):
    require(artifact.get('expired') is False, 'Expired receipt artifact')
    require(artifact.get('workflow_run', {}).get('head_sha') == sha and
            str(artifact['workflow_run']['id']) == str(run['id']), 'Artifact run mismatch')
    require(artifact.get('digest') == 'sha256:' + hashlib.sha256(data).hexdigest(), 'Artifact digest missing or incorrect')
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        names = archive.namelist()
        # Receipts only. No generic archive extraction and no candidate-provided paths.
        require(names == [platform + '-native.json'], 'Unexpected receipt archive contents')
        require(archive.getinfo(names[0]).file_size <= 1024 * 1024, 'Receipt too large')
        return json.loads(archive.read(names[0]))


def gh(path, raw=False):
    p = subprocess.run(['gh', 'api', path], capture_output=True)
    require(p.returncode == 0, 'GitHub read failed; check Actions/environment read access')
    return p.stdout if raw else json.loads(p.stdout)


def paged(path, key):
    rows = []
    for page in range(1, 101):
        data = gh(path + ('&' if '?' in path else '?') + f'per_page=100&page={page}')
        batch = data[key]
        rows += batch
        if len(batch) < 100:
            return rows
    raise ValueError('Pagination limit exceeded')


def verify(root, sha, run_id, build, platforms, check_environment=True):
    arguments(sha, run_id, build)
    require(subprocess.check_output(['git', '-C', str(root), 'rev-parse', 'HEAD'], text=True).strip() == sha,
            'Checkout differs from candidate SHA')
    require(not subprocess.check_output(['git', '-C', str(root), 'status', '--porcelain', '--untracked-files=all']),
            'Candidate checkout must be clean')
    run = gh(f'repos/{REPO}/actions/runs/{run_id}')
    jobs = paged(f'repos/{REPO}/actions/runs/{run_id}/attempts/{run["run_attempt"]}/jobs', 'jobs')
    validate_run(run, jobs, sha, run_id)
    artifacts = paged(f'repos/{REPO}/actions/runs/{run_id}/artifacts', 'artifacts')
    features = declared_features(root)
    receipts = {}
    for platform in PLATFORMS:
        matches = [a for a in artifacts if a['name'] == f'{platform}-native-{sha}']
        require(len(matches) == 1, 'Missing or ambiguous receipt artifact: ' + platform)
        a = matches[0]
        record = read_receipt_archive(gh(f'repos/{REPO}/actions/artifacts/{a["id"]}/zip', raw=True), a, sha, platform, run)
        validate_receipt(record, run, sha, platform, expected_methods(root, platform), features)
        receipts[platform] = {'artifact_id': a['id'], 'digest': a['digest'], 'passed_count': record['passed_count']}
    approvals = {}
    if check_environment:
        for platform in platforms:
            base = f'repos/{REPO}/environments/playground-{platform}'
            env = gh(base)
            policies = [{'name': p['name'], 'type': p['type']} for p in paged(base + '/deployment-branch-policies', 'branch_policies')]
            approvals[platform] = validate_environment(env, policies,
                (os.environ.get('GITHUB_ACTOR'), os.environ.get('GITHUB_TRIGGERING_ACTOR')))
    return {'source_sha': sha, 'native_run_id': int(run_id), 'native_run_attempt': run['run_attempt'],
            'build_number': str(build), 'features': features, 'receipts': receipts, 'approvals': approvals,
            'distribution': 'not_started', 'physical_device': 'unverified'}


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--source', type=Path, required=True)
    p.add_argument('--sha', required=True)
    p.add_argument('--run', required=True)
    p.add_argument('--build', required=True)
    p.add_argument('--platform', choices=['both', *PLATFORMS], required=True)
    p.add_argument('--out', type=Path, required=True)
    a = p.parse_args()
    require(same_repository(os.environ.get('GITHUB_REPOSITORY')) and os.environ.get('GITHUB_REF') == 'refs/heads/main'
            and os.environ.get('GITHUB_EVENT_NAME') == 'workflow_dispatch', 'Release control must run by manual dispatch from main')
    platforms = list(PLATFORMS) if a.platform == 'both' else [a.platform]
    result = verify(a.source.resolve(), a.sha, a.run, a.build, platforms)
    result['visual_review_url'] = visual_review(os.environ.get('PLAYGROUND_VISUAL_REVIEW_URL'))
    a.out.parent.mkdir(parents=True, exist_ok=True)
    a.out.write_text(json.dumps(result, indent=2) + '\n')
    if os.environ.get('GITHUB_OUTPUT'):
        matrix = {'include': [{'platform': x, 'runner': 'macos-15' if x == 'ios' else 'ubuntu-24.04'} for x in platforms]}
        with open(os.environ['GITHUB_OUTPUT'], 'a') as f:
            f.write('matrix=' + json.dumps(matrix) + '\n')
    print('Exact-commit native receipts and existing environment protections verified.')


if __name__ == '__main__':
    try:
        main()
    except (ValueError, KeyError, OSError, subprocess.SubprocessError, zipfile.BadZipFile) as error:
        raise SystemExit('Distribution admission rejected: ' + str(error))
