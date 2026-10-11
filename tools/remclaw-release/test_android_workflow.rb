# Structural confidentiality and safety contract for remclaw-android-release.yml. Dependency-free.
# Mirrors test_workflow.rb (the iOS contract).
require 'yaml'
root = File.expand_path('../..', __dir__)
text = File.read(File.join(root, '.github/workflows/remclaw-android-release.yml'), encoding: 'UTF-8')
release = YAML.safe_load(text)
def check(ok, message)
  raise message unless ok
end

events = release['on'] || release[true] # Psych YAML 1.1 parses the key "on" as true.
check(events.keys == ['workflow_dispatch'], 'Release must be manual only')
inputs = events['workflow_dispatch']['inputs']
check(inputs.keys == %w[remclaw_sha version_code track], 'Inputs are exactly remclaw_sha, version_code, track')
check(inputs['track']['type'] == 'choice' && inputs['track']['options'] == ['internal'], 'Track input offers internal only')
check(release['permissions'] == {'contents' => 'read'}, 'Workflow token must default to contents: read')
check(release.dig('concurrency', 'cancel-in-progress') == false, 'Do not cancel store uploads')
{ 'REMCLAW_SHA' => 'remclaw_sha', 'REMCLAW_VERSION_CODE' => 'version_code', 'REMCLAW_TRACK' => 'track' }.each do |name, input|
  check(release['env'][name] == "${{ inputs.#{input} }}", 'Inputs must reach scripts through env, not inline interpolation')
end
release['jobs'].each_value do |j|
  j['steps'].each { |s| check(!s['run'].to_s.include?('${{'), 'No expression interpolation inside run scripts') }
end

admission = release['jobs']['admission']
job = release['jobs']['release']
[admission, job].each do |j|
  check(j['if'].include?("github.ref == 'refs/heads/main'") && j['if'].include?("github.repository == 'Rem-Assistant/Rem-Design-System'"),
    'Missing main-only/repository guard')
  check(j['permissions'].values.all? { |v| v == 'read' }, 'Job tokens must be read-only')
  check(!j.key?('secrets'), 'No inherited secrets')
end
check(!admission.to_s.include?('secrets.') && !admission.to_s.include?('vars.'), 'Admission job must not reference secrets or variables')
check(!admission.key?('environment'), 'Admission must not enter an environment')
check(admission['steps'].any? { |s| s['run'].to_s.include?('android_release.py admit') }, 'Admission must verify environment protection')
check(job['needs'] == 'admission', 'Release waits for admission')
check(job['environment'] == 'remclaw-android-release', 'Release must use the protected remclaw-android-release environment')
check(job['runs-on'] == 'ubuntu-24.04', 'Release runs on hosted ubuntu-24.04')
check(job['env']['BUNDLE_FROZEN'] == 'true', 'Release dependency resolution must be frozen')

steps = job['steps']
names = steps.map { |s| s['name'].to_s }
runs = steps.map { |s| s['run'].to_s }
all_runs = (admission['steps'] + steps).map { |s| s['run'].to_s }.join("\n")

release['jobs'].each_value do |item|
  item['steps'].each do |step|
    action = step['uses']
    next unless action
    check(action.match?(/@[0-9a-f]{40}\z/), "Action is not pinned by SHA: #{action}")
    check(!action.match?(/upload-artifact|cache|release|download-artifact|setup-gradle|gradle-build-action/), "Do not publish or cache: #{action}")
    check(!step.fetch('with', {}).key?('cache'), "No dependency caching: #{action}")
  end
end
check(!text.match?(/set\s+-[a-wyz]*x|set\s+-o\s+xtrace|bash\s+-x|ACTIONS_STEP_DEBUG/), 'No shell tracing')
check(!all_runs.match?(/\b(cat|tail|head|less|more|tee)\b/), 'Never print files in public logs')
check(!text.include?('actions/cache') && !text.include?('upload-artifact'), 'No caches or artifacts')
checkouts = steps.select { |s| s['uses'].to_s.start_with?('actions/checkout@') }
check(checkouts.all? { |s| s.dig('with', 'persist-credentials') == false && s.dig('with', 'ref') == '${{ github.sha }}' &&
  !s['with'].key?('repository') && !s['with'].key?('ssh-key') },
  'Only the trusted control checkout may use actions/checkout; it would log RemClaw commit author/message and refs')

runs.each do |run|
  run.scan(/bundle exec fastlane [a-z_]+[^\n]*|bundle install[^\n]*/).each do |call|
    check(call.include?('> "$REMCLAW_PRIVATE/') && call.include?('2>&1'), "Unredirected tool output: #{call}")
  end
  check(!run.match?(/^\s*(\.\/gradlew|gradle |git (?!diff --quiet -- Gemfile\.lock$)|keytool|jarsigner|java |fastlane|gem |bundletool)/),
    'Tools must run through android_release.py or a redirected wrapper')
end
check(all_runs.scan(/classify_failure\.py/).length == 2, 'Every redirected shell tool must classify its failure')
check(text.scan(/bundle exec fastlane internal_upload/).length == 1, 'Exactly one Play lane invocation')

# Secrets and variables: each only in the step that needs it.
expect = {
  'Fetch exact RemClaw commit' => [%w[REMCLAW_DEPLOY_KEY], []],
  'Build unsigned release bundle' => [[], %w[REMCLAW_ANDROID_GOOGLE_SERVER_CLIENT_ID]],
  'Sign com.remapp.rem' => [%w[REMCLAW_ANDROID_UPLOAD_KEYSTORE_BASE64 REMCLAW_ANDROID_UPLOAD_KEYSTORE_PASSWORD],
                            %w[REMCLAW_ANDROID_UPLOAD_KEY_ALIAS REMCLAW_ANDROID_UPLOAD_CERT_SHA256]],
  'Upload to the Play internal track' => [%w[REMCLAW_PLAY_SERVICE_ACCOUNT_JSON], []]
}
steps.each_with_index do |s, i|
  values = s.fetch('env', {}).values.join(' ')
  used = [values.scan(/secrets\.([A-Z0-9_]+)/).flatten.sort, values.scan(/vars\.([A-Z0-9_]+)/).flatten.sort]
  prefix = expect.keys.find { |k| names[i].start_with?(k) }
  want = prefix ? expect[prefix].map(&:sort) : [[], []]
  check(used == want, "Unexpected secrets or variables in step: #{names[i]}")
  s.fetch('env', {}).each do |k, v|
    next unless v.to_s.match?(/secrets\.|vars\./)
    check(["${{ secrets.#{k} }}", "${{ vars.#{k} }}"].include?(v), "Env name must equal its secret or variable name: #{k}")
  end
  check(!s['run'].to_s.include?('secrets.'), 'Secrets only through step env')
end
check(expect.keys.all? { |k| names.any? { |n| n.start_with?(k) } }, 'Missing expected secret-bearing step')
check(!text.include?('REMCLAW_ASC_') && !text.include?('REMCLAW_IOS_') && !text.include?('PLAYGROUND_GOOGLE_PLAY_JSON'),
  'Android release must not touch iOS or Playground credentials')

recheck = names.index('Recheck admission after environment approval')
fetch = names.index { |n| n.start_with?('Fetch exact RemClaw commit') }
check(recheck && fetch && recheck < fetch, 'Environment protection is re-verified before RemClaw is fetched')
check(runs[recheck].include?('android_release.py admit'), 'Recheck must use the gate')
{ 'dependencies' => 'Install frozen checksummed dependencies', 'build' => 'Build unsigned release bundle',
  'sign' => 'Sign com.remapp.rem', 'upload' => 'Upload to the Play internal track' }.each do |stage, prefix|
  index = names.index { |n| n.start_with?(prefix) }
  check(index && names[index - 1] == "Check runner storage before #{stage}", "Missing storage check before #{stage}")
  check(runs[index - 1].include?('storage.py check --path "$RUNNER_TEMP" --platform android') && runs[index - 1].include?("--stage #{stage}"),
    "Storage check for #{stage} is malformed")
end
check(names[-2] == 'Publish only allowlisted release metadata' && steps[-2]['if'] == 'always()' && runs[-2].include?('android_release.py summary'),
  'Allowlisted summary must always run')
check(names[-1].start_with?('Remove RemClaw source') && steps[-1]['if'] == 'always()' && runs[-1].include?('android_release.py cleanup "$RUNNER_TEMP"'),
  'Cleanup must always run last')
check(!all_runs.include?('GITHUB_STEP_SUMMARY'), 'Step summary only through the allowlisted summary writer')
init = runs.first
check(init.include?('REMCLAW_PRIVATE=$RUNNER_TEMP/remclaw-private') && init.include?('REMCLAW_SOURCE=$RUNNER_TEMP/remclaw-source') &&
  init.include?('REMCLAW_EPHEMERAL=$RUNNER_TEMP/remclaw-ephemeral'), 'RemClaw state must live in ephemeral runner paths')
check(!all_runs.include?('gem install'), 'Unfrozen dependency install')
puts 'RemClaw Android release workflow contracts passed: manual main-only, internal track only, env-scoped secrets, private logs, storage guards, cleanup.'
