# Test-only preload for test_upload_pathway.rb: `RUBYOPT=-r<this file> bundle exec fastlane distribute`.
# Everything after it is the real locked fastlane 2.240.1 (CLI, runner, upload_to_testflight, pilot
# BuildManager#upload, ItunesTransporter, AltoolTransporterExecutor, FastlanePty) and the real Fastfile.
# It replaces only what cannot exist offline on a Linux check runner:
#   - macOS/Xcode detection: macOS is reported, and the Xcode version is the fixture's (PATHWAY_XCODE_VERSION,
#     the one the workflow's package.py xcode step selected; macos-15's default is 16.4, not 26);
#   - all TCP connections (blocked: any provider call fails instead of leaving the machine);
#   - the Fastfile's own App Store Connect helpers (apple_request/apple_group) and the polling wait;
#   - pilot's post-upload Apple API steps, reached only when fastlane decides the upload succeeded;
#   - for fixture l only, the Fastfile's UTF-8 read scope (disabled).
# `xcrun` is a stub executable on PATH (written by the test) that prints fixture output and exits.
require 'socket'

module PathwayNoNetwork
  def self.refuse(*)
    raise Errno::ENETUNREACH, 'pathway harness: network blocked'
  end
end
TCPSocket.singleton_class.prepend(Module.new do
  def new(*) = PathwayNoNetwork.refuse
  def open(*) = PathwayNoNetwork.refuse
end)
Socket.singleton_class.prepend(Module.new do
  def tcp(*) = PathwayNoNetwork.refuse
end)

require 'bundler/setup'
require 'fastlane'
require 'pilot'

FastlaneCore::Helper.singleton_class.prepend(Module.new do
  def mac? = true
  def is_mac? = true
  def xcode_version = ENV.fetch('PATHWAY_XCODE_VERSION')
  def xcode_path = '/Applications/Xcode.app/Contents/Developer/'
end)

# Read-only Apple API answers for the lane; anything else fails closed.
module PathwayAppleApi
  BUILDS = '/v1/builds?filter%5Bapp%5D=6820738808&limit=200'
  RECORD = '/v1/builds?filter%5Bapp%5D=6820738808&filter%5Bversion%5D=8'

  def apple_group
    ENV.fetch('PLAYGROUND_TESTFLIGHT_GROUP_ID')
  end

  def apple_request(method, path, payload = nil, timeouts: {})
    raise 'pathway harness: unexpected Apple mutation' unless method == :get && payload.nil?
    File.open(ENV.fetch('PATHWAY_CALLS'), 'a') { |f| f.puts("#{path} #{timeouts.values.join('/')}".strip) }
    case path
    when BUILDS then {'data' => [{'attributes' => {'version' => '7'}}], 'links' => {}}
    when RECORD
      record = ENV.fetch('PATHWAY_RECORD')
      require_release(false, 'Apple API failed', 'apple_api_http') if record == 'error'
      {'data' => record == 'present' ? [{'id' => 'b', 'attributes' => {'version' => '8'}}] : []}
    else raise 'pathway harness: unexpected Apple path'
    end
  end

  def upload_record_wait(_seconds); end

  # Records whether the workaround variable is gone once the upload call returns or raises.
  def with_upload_altool_parameters(*args, &block)
    super
  ensure
    name = 'DELIVER_ALTOOL_ADDITIONAL_UPLOAD_PARAMETERS'
    File.write(ENV.fetch('PATHWAY_ENV_AFTER'), ENV.key?(name) ? "set:#{ENV[name]}" : 'unset')
  end

  # Fixture l only: disable the Fastfile's UTF-8 scope to show the masked case still has a distinct code.
  def with_upload_utf8(&block)
    ENV['PATHWAY_MODE'] == 'no_utf8_scope' ? block.call : super
  end
end
Fastlane::FastFile.prepend(PathwayAppleApi)

# Fixture modes that need an outer layer: an unknown pilot message (i) and a generic wrapper (h).
Pilot::BuildManager.prepend(Module.new do
  def upload(options)
    case ENV['PATHWAY_MODE']
    when 'unknown_message'
      FastlaneCore::UI.user_error!("Pilot stopped: #{ENV.fetch('PATHWAY_SENTINEL_TEXT')}")
    when 'wrapped_runtime'
      begin
        super
      rescue StandardError
        raise 'Upload step failed' # RuntimeError; Ruby sets the real pilot error as its cause
      end
    when 'wrapped_errno'
      begin
        super
      rescue StandardError
        raise Errno::EPIPE, 'upload stream'
      end
    else
      super
    end
  end

  # Reached only after fastlane accepted the altool result; Apple processing is out of scope here.
  def wait_for_build_processing_to_be_complete(*)
    Struct.new(:app_version, :version).new('0.1.0', '8')
  end

  def distribute(*); end
end)
