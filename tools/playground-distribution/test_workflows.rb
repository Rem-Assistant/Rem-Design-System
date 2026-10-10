require 'yaml'
root = File.expand_path('../..', __dir__)
release = YAML.safe_load(File.read(File.join(root, '.github/workflows/playground-distribute.yml')))
checks = YAML.safe_load(File.read(File.join(root, '.github/workflows/playground-distribution-check.yml')))
def check(ok, message)
  raise message unless ok
end
events = release['on'] || release[true] # Psych YAML 1.1 parses the key "on" as true.
check(events.keys == ['workflow_dispatch'], 'Release must be manual only')
check(release['permissions'] == {'contents' => 'read', 'actions' => 'read', 'deployments' => 'read'}, 'Release token permissions changed')
check(release.dig('concurrency', 'cancel-in-progress') == false, 'Do not cancel store uploads')
check(release.dig('jobs', 'admission', 'if').include?("github.ref == 'refs/heads/main'"), 'Missing main-only guard')
job = release['jobs']['distribute']
check(job['environment'] == 'playground-${{ matrix.platform }}', 'Missing platform environment')
check(job['env']['BUNDLE_FROZEN'] == 'true', 'Release dependency resolution must be frozen')
check(checks['permissions'] == {'contents' => 'read'}, 'Checks must remain read-only')
[release, checks].each do |workflow|
  workflow['jobs'].each_value do |item|
    check(!item.key?('secrets'), 'No inherited secrets')
    check(!item.fetch('env', {}).values.any? { |v| v.to_s.include?('runner.') },
      'Runner context is unavailable in job-level env; initialize paths in a step')
    item['steps'].each do |step|
      action = step['uses']
      if action
        check(action.match?(/@[0-9a-f]{40}\z/), 'Action is not pinned by SHA')
        check(!action.match?(/upload-artifact|cache|release/), 'Do not publish/cache packages')
      end
      if workflow.equal?(checks)
        check(!step.to_s.include?('secrets.'), 'Secret-free checks cannot access release credentials')
      end
    end
  end
end
names = job['steps'].map { |s| s['name'].to_s }
# Numeric storage checks run immediately before each heavy or irreversible stage.
{ 'dependencies' => 'Install frozen checksummed dependencies', 'build' => 'Build and inspect',
  'sign' => 'Sign iOS', 'upload' => 'Upload directly' }.each do |stage, prefix|
  index = names.index { |n| n.start_with?(prefix) }
  check(index && index > 0 && names[index - 1] == "Check runner storage before #{stage}", "Missing storage check before #{stage}")
  check(job['steps'][index - 1]['run'].include?("storage.py check --path \"$RUNNER_TEMP\"") &&
        job['steps'][index - 1]['run'].include?("--stage #{stage}"), "Storage check for #{stage} is malformed")
end
init = job['steps'].first.fetch('run', '')
check(init.include?('PLAYGROUND_EPHEMERAL=$RUNNER_TEMP/playground-ephemeral') &&
      init.include?('GRADLE_USER_HOME=$RUNNER_TEMP/playground-ephemeral/gradle-home'),
      'Build state must use ephemeral runner paths')
steps = job['steps'].map { |s| s.fetch('run', '') }.join("\n")
check(steps.include?('bundle exec fastlane distribute'), 'Store action must use the locked bundle')
check(!steps.include?('gem install fastlane'), 'Unfrozen dependency install')
puts 'Workflow safety contracts passed; manual release and secret-free checks remain separate.'
