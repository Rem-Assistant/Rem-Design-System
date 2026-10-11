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
%w[promote_track rollout: track_promote_to upload_to_play_store_internal_app_sharing validate_play_store_json_key].each do |forbidden|
  raise "Forbidden Play action: #{forbidden}" if text.include?(forbidden)
end
puts 'RemClaw Android Fastfile contracts passed: internal track only, increasing version code, completed readback.'
