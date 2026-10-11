# Structural confidentiality and safety contract for remclaw-ios-release.yml. Dependency-free.
require 'yaml'
root = File.expand_path('../..', __dir__)
text = File.read(File.join(root, '.github/workflows/remclaw-ios-release.yml'), encoding: 'UTF-8')
release = YAML.safe_load(text)
checks = YAML.safe_load(File.read(File.join(root, '.github/workflows/remclaw-release-check.yml')))
def check(ok, message)
  raise message unless ok
end

events = release['on'] || release[true] # Psych YAML 1.1 parses the key "on" as true.
check(events.keys == ['workflow_dispatch'], 'Release must be manual only')
check(events['workflow_dispatch']['inputs'].keys == ['remclaw_sha'], 'Exactly one input: remclaw_sha')
check(release['permissions'] == {'contents' => 'read'}, 'Workflow token must default to contents: read')
check(release.dig('concurrency', 'cancel-in-progress') == false, 'Do not cancel store uploads')
check(release['env']['REMCLAW_SHA'] == '${{ inputs.remclaw_sha }}', 'SHA must reach scripts through env, not inline interpolation')

admission = release['jobs']['admission']
job = release['jobs']['release']
[admission, job].each do |j|
  check(j['if'].include?("github.ref == 'refs/heads/main'") && j['if'].include?("github.repository == 'Rem-Assistant/Rem-Design-System'"),
    'Missing main-only/repository guard')
  check(j['permissions'].values.all? { |v| v == 'read' }, 'Job tokens must be read-only')
end
check(!admission.to_s.include?('secrets.'), 'Admission job must not reference secrets')
check(!admission.key?('environment'), 'Admission must not enter an environment')
check(admission['steps'].any? { |s| s['run'].to_s.include?('release.py admit') }, 'Admission must verify environment protection')
check(job['needs'] == 'admission', 'Release waits for admission')
check(job['environment'] == 'remclaw-ios-release', 'Release must use the protected remclaw-ios-release environment')
check(job['runs-on'] == 'macos-15', 'Release runs on hosted macos-15')
check(job['env']['BUNDLE_FROZEN'] == 'true', 'Release dependency resolution must be frozen')

steps = job['steps']
names = steps.map { |s| s['name'].to_s }
runs = steps.map { |s| s['run'].to_s }
all_runs = (admission['steps'] + steps).map { |s| s['run'].to_s }.join("\n")

# No publication, caching or tracing anywhere.
[release, checks].each do |workflow|
  workflow['jobs'].each_value do |item|
    check(!item.key?('secrets'), 'No inherited secrets')
    item['steps'].each do |step|
      action = step['uses']
      next unless action
      check(action.match?(/@[0-9a-f]{40}\z/), "Action is not pinned by SHA: #{action}")
      check(!action.match?(/upload-artifact|cache|release|download-artifact/), "Do not publish or cache: #{action}")
    end
  end
end
check(!text.match?(/set\s+-[a-wyz]*x|set\s+-o\s+xtrace|bash\s+-x|ACTIONS_STEP_DEBUG/), 'No shell tracing')
check(!all_runs.match?(/\b(cat|tail|head|less|more|tee)\b/), 'Never print files in public logs')
check(!text.include?('actions/cache') && !text.include?('upload-artifact'), 'No caches or artifacts')
checkouts = steps.select { |s| s['uses'].to_s.start_with?('actions/checkout@') }
check(checkouts.all? { |s| s.dig('with', 'persist-credentials') == false && s.dig('with', 'ref') == '${{ github.sha }}' &&
  !s['with'].key?('repository') && !s['with'].key?('ssh-key') },
  'Only the trusted control checkout may use actions/checkout; it would log RemClaw commit author/message and refs')

# Every tool that can see RemClaw source or provider output writes only to private files.
runs.each do |run|
  run.scan(/bundle exec fastlane [a-z_]+[^\n]*|bundle install[^\n]*/).each do |call|
    check(call.include?('> "$REMCLAW_PRIVATE/') && call.include?('2>&1'), "Unredirected tool output: #{call}")
  end
  # The only direct git call checks the public lockfile of the control checkout.
  check(!run.match?(/^\s*(xcodebuild|git (?!diff --quiet -- Gemfile\.lock$)|security |codesign|fastlane|gem )/), 'Tools must run through release.py or a redirected wrapper')
end
check(all_runs.scan(/classify_failure\.py/).length == 3, 'Every redirected shell tool must classify its failure')

# Secrets: each one only in the step that needs it.
secret_steps = steps.each_with_index.map { |s, i| [i, s.fetch('env', {}).values.join(' ').scan(/secrets\.([A-Z0-9_]+)/).flatten] }
expect = {
  'Fetch exact RemClaw commit' => %w[REMCLAW_DEPLOY_KEY],
  'Reserve next TestFlight build number' => %w[REMCLAW_ASC_KEY_ID REMCLAW_ASC_ISSUER_ID REMCLAW_ASC_KEY_BASE64],
  'Sign com.remapp.rem' => %w[REMCLAW_IOS_CERTIFICATE_BASE64 REMCLAW_IOS_CERTIFICATE_PASSWORD REMCLAW_IOS_PROFILE_BASE64 REMCLAW_IOS_WIDGET_PROFILE_BASE64],
  'Upload directly to TestFlight' => %w[REMCLAW_ASC_KEY_ID REMCLAW_ASC_ISSUER_ID REMCLAW_ASC_KEY_BASE64]
}
secret_steps.each do |i, used|
  prefix = expect.keys.find { |k| names[i].start_with?(k) }
  check(used.sort == (prefix ? expect[prefix].sort : []), "Unexpected secrets in step: #{names[i]}")
  check(!steps[i]['run'].to_s.include?('secrets.'), 'Secrets only through step env')
end
check(expect.keys.all? { |k| names.any? { |n| n.start_with?(k) } }, 'Missing expected secret-bearing step')
build_index = names.index { |n| n.start_with?('Build unsigned archive') }
check(steps[build_index].fetch('env', {}).empty?, 'Build runs without credentials')

# Order: admission recheck, then fetch, then storage-guarded stages, then always summary and cleanup.
recheck = names.index('Recheck admission after environment approval')
fetch = names.index { |n| n.start_with?('Fetch exact RemClaw commit') }
check(recheck && fetch && recheck < fetch, 'Environment protection is re-verified before RemClaw is fetched')
check(runs[recheck].include?('release.py admit'), 'Recheck must use the gate')
{ 'dependencies' => 'Install frozen checksummed dependencies', 'build' => 'Build unsigned archive',
  'sign' => 'Sign com.remapp.rem', 'upload' => 'Upload directly to TestFlight' }.each do |stage, prefix|
  index = names.index { |n| n.start_with?(prefix) }
  check(index && names[index - 1] == "Check runner storage before #{stage}", "Missing storage check before #{stage}")
  check(runs[index - 1].include?('storage.py check --path "$RUNNER_TEMP" --platform ios') && runs[index - 1].include?("--stage #{stage}"),
    "Storage check for #{stage} is malformed")
end
check(names[-2] == 'Publish only allowlisted release metadata' && steps[-2]['if'] == 'always()' && runs[-2].include?('release.py summary'),
  'Allowlisted summary must always run')
check(names[-1].start_with?('Remove RemClaw source') && steps[-1]['if'] == 'always()' && runs[-1].include?('release.py cleanup "$RUNNER_TEMP"'),
  'Cleanup must always run last')
check(!all_runs.include?('GITHUB_STEP_SUMMARY'), 'Step summary only through the allowlisted summary writer')
init = runs.first
check(init.include?('REMCLAW_PRIVATE=$RUNNER_TEMP/remclaw-private') && init.include?('REMCLAW_SOURCE=$RUNNER_TEMP/remclaw-source') &&
  init.include?('REMCLAW_EPHEMERAL=$RUNNER_TEMP/remclaw-ephemeral'), 'RemClaw state must live in ephemeral runner paths')
check(!all_runs.include?('gem install'), 'Unfrozen dependency install')

# The secret-free check workflow.
check(checks['permissions'] == {'contents' => 'read'}, 'Checks must remain read-only')
check(!checks.to_s.include?('secrets.') && !checks.to_s.include?('environment'), 'Checks cannot reach release credentials')
puts 'RemClaw release workflow contracts passed: manual main-only, env-scoped secrets, private logs, storage guards, cleanup.'
