# Dependency-free tests: load the RemClaw Android Play lane helpers but never execute a lane or call Google.
module UI
  def self.user_error!(message)
    raise message
  end
end
def skip_docs; end
@lanes = []
def lane(name)
  @lanes << name
end
load File.join(__dir__, 'android/fastlane/Fastfile')
raise 'Unexpected lanes' unless @lanes == %i[internal_upload]
raise 'Package must be com.remapp.rem' unless REMCLAW_PACKAGE == 'com.remapp.rem'
raise 'Track must be internal' unless REMCLAW_TRACK == 'internal'

Track = Struct.new(:track, :releases)
Release = Struct.new(:status, :version_codes)
Bundle = Struct.new(:version_code)

def rejects(message)
  begin
    yield
  rescue RuntimeError => e
    raise "Wrong rejection: #{e.message}" unless e.message.include?(message)
    return
  end
  raise "Expected rejection: #{message}"
end

internal = Track.new('internal', [Release.new('completed', ['40'])])
production = Track.new('production', [Release.new('completed', ['12'])])
check_version_code([internal, production], [Bundle.new(41)], '42')
check_version_code([Track.new('internal', [])], [], '1') # existing internal track with no releases yet
rejects('does not exist') { check_version_code([production], [], '42') }
rejects('pending work') { check_version_code([Track.new('internal', [Release.new('draft', ['41'])])], [], '42') }
rejects('pending work') { check_version_code([Track.new('internal', [Release.new('inProgress', ['41'])])], [], '42') }
rejects('increasing Play version code') { check_version_code([internal], [], '40') }
rejects('increasing Play version code') { check_version_code([internal], [Bundle.new(50)], '42') }
rejects('increasing Play version code') { check_version_code([internal, Track.new('alpha', [Release.new('completed', ['99'])])], [], '42') }
rejects('non-integer') { check_version_code([Track.new('internal', [Release.new('completed', ['4x'])])], [], '42') }

raise 'readback' unless internal_has([Track.new('internal', [Release.new('completed', ['42'])])], '42')
raise 'readback must require completed' if internal_has([Track.new('internal', [Release.new('draft', ['42'])])], '42')
raise 'readback must require internal' if internal_has([Track.new('alpha', [Release.new('completed', ['42'])])], '42')

# First-upload (draft app) path: the status comes only from the admission record's reviewed value.
raise 'statuses' unless REMCLAW_RELEASE_STATUSES == %w[draft completed]
raise 'draft admitted' unless release_status_for('release_status' => 'draft') == 'draft'
raise 'completed admitted' unless release_status_for('release_status' => 'completed') == 'completed'
[nil, '', 'inProgress', 'halted', 'Completed', ['draft']].each do |bad|
  rejects('reviewed Play release status') { release_status_for('release_status' => bad) }
end
rejects('reviewed Play release status') { release_status_for({}) }
raise 'draft readback' unless internal_has([Track.new('internal', [Release.new('draft', ['42'])])], '42', 'draft')
raise 'draft readback must not accept completed' if internal_has([Track.new('internal', [Release.new('completed', ['42'])])], '42', 'draft')
raise 'draft readback must require internal' if internal_has([Track.new('alpha', [Release.new('draft', ['42'])])], '42', 'draft')
raise 'draft final state' unless final_distribution('draft') == 'draft_internal'
raise 'completed final state' unless final_distribution('completed') == 'published_internal'
raise 'draft-app rejection' unless draft_app_rejection?('Google Api Error: Invalid request - Only releases with status draft may be created on draft app.')
raise 'unrelated error is not a draft-app rejection' if draft_app_rejection?('APK specifies a version code that has already been used.')
# An unpromoted first draft blocks the next upload instead of being stacked or replaced.
rejects('pending work') { check_version_code([Track.new('internal', [Release.new('draft', ['1'])])], [], '2') }

ENV.delete('REMCLAW_PLAY_SERVICE_ACCOUNT_JSON')
rejects('Missing protected-environment secret') { play_json }
ENV['REMCLAW_PLAY_SERVICE_ACCOUNT_JSON'] = '{"type":"authorized_user"}'
rejects('not a service-account key') { play_json }
ENV['REMCLAW_PLAY_SERVICE_ACCOUNT_JSON'] = 'not json MARKER'
rejects('not a service-account key') { play_json }
ENV['REMCLAW_PLAY_SERVICE_ACCOUNT_JSON'] = '{"type":"service_account"}'
raise 'service account accepted' unless play_json == '{"type":"service_account"}'
ENV.delete('REMCLAW_PLAY_SERVICE_ACCOUNT_JSON')

ENV['GITHUB_ACTIONS'] = 'false'
rejects('Hosted main dispatch required') { hosted_main! }

text = File.read(File.join(__dir__, 'android/fastlane/Fastfile'))
raise 'Upload must name only the internal track' unless text.scan(/track: /).length == 1 && text.include?('track: REMCLAW_TRACK')
raise 'Upload status must come from the admission record' unless text.include?('release_status: status') &&
  text.scan(/release_status: /).length == 1
raise 'Exactly one upload call; no retry with another status' unless text.scan(/upload_to_play_store\(/).length == 1
%w[promote_track rollout: track_promote_to upload_to_play_store_internal_app_sharing validate_play_store_json_key].each do |forbidden|
  raise "Forbidden Play action: #{forbidden}" if text.include?(forbidden)
end
puts 'RemClaw Android Fastfile contracts passed: internal track only, reviewed draft/completed status, increasing version code, exact-status readback.'
