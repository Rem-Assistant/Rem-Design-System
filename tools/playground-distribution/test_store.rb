# Dependency-free tests: load helpers but never execute a lane or call a provider.
module UI
  def self.user_error!(message)
    raise message
  end
end
def skip_docs; end
def lane(name, &block)
  (@lanes ||= {})[name] = block
end
load File.join(__dir__, 'fastlane/Fastfile')

GROUP = '12345678-1234-1234-1234-123456789012'
ENV['PLAYGROUND_TESTFLIGHT_GROUP_ID'] = GROUP
def responses(pages)
  @responses = [
    {'data' => {'attributes' => {'isInternalGroup' => true, 'publicLinkEnabled' => false, 'hasAccessToAllBuilds' => false}}},
    {'data' => {'id' => '6820738808'}}
  ] + pages
end
def apple_request(method, path, payload = nil)
  raise 'Unexpected mutation' unless method == :get && payload.nil?
  raise 'Unexpected extra provider call' if @responses.empty?
  @responses.shift
end
def page(access, next_url = nil, internal: true)
  attributes = {'hasAccessToAllBuilds' => access}
  attributes['isInternalGroup'] = internal unless internal == :absent
  {'data' => [{'attributes' => attributes}], 'links' => {'next' => next_url}}
end
def rejects(message)
  begin
    apple_group
  rescue RuntimeError => e
    raise "Wrong rejection: #{e.message}" unless e.message.include?(message)
    return
  end
  raise 'Expected rejection did not occur'
end

responses([page(false)])
raise 'Wrong group' unless apple_group == GROUP && @responses.empty?
responses([page(true)])
rejects('automatic or unknown')
responses([page(nil)])
rejects('automatic or unknown')
responses([page(false, 'https://api.appstoreconnect.apple.com/v1/apps/6820738808/betaGroups?cursor=next'), page(true)])
rejects('automatic or unknown')
responses([page(false, 'https://evil.example/v1/apps/6820738808/betaGroups?cursor=next')])
rejects('pagination host/path')
responses([page(false)])
@responses[0]['data']['attributes']['publicLinkEnabled'] = true
rejects('private, manual')
# Each rejection names the deciding condition in a fixed guard code (published by summary.py).
def guard_after(pages, target = {})
  responses(pages)
  @responses[0]['data']['attributes'].merge!(target)
  $release_guard = nil
  begin
    apple_group
  rescue RuntimeError
  end
  $release_guard
end
{
  [[page(true)], {}] => 'group_other_all_builds',
  [[page(nil)], {}] => 'group_other_access_flag_unknown',
  [[page(false)], {'publicLinkEnabled' => true}] => 'group_public_link',
  [[page(false)], {'isInternalGroup' => false}] => 'group_not_internal',
  [[page(false)], {'hasAccessToAllBuilds' => true}] => 'group_target_all_builds',
  [[page(false)], {'hasAccessToAllBuilds' => nil}] => 'group_target_access_flag_unknown',
  [[page(false, 'https://evil.example/v1/apps/6820738808/betaGroups?cursor=next')], {}] => 'group_pagination_host',
  [[page(nil, internal: false)], {}] => nil,
  [[page(false, internal: false)], {}] => nil,
  [[page(true, internal: false)], {}] => 'group_other_all_builds',
  [[page('yes', internal: false)], {}] => 'group_other_access_flag_unknown',
  [[page(nil, internal: :absent)], {}] => 'group_other_type_unknown',
  [[page(false, internal: nil)], {}] => 'group_other_type_unknown',
  [[page(false, internal: 'false')], {}] => 'group_other_type_unknown'
}.each do |(pages, target), code|
  got = guard_after(pages, target)
  raise "Expected guard #{code}, got #{got.inspect}" unless got == code
end
responses([page(false)])
$release_guard = nil
raise 'A passing check must not set a guard' unless apple_group == GROUP && $release_guard.nil?
# An external group's null flag (Apple's documented response) passes, so the run reaches upload scope.
responses([page(nil, internal: false)])
$release_guard = nil
raise 'External group with a null flag must pass' unless apple_group == GROUP && $release_guard.nil?

# TestFlight upload failures: pilot's message maps to a fixed code by allowlisted pattern; text never kept.
SECRET = 'Provider said: team 9ZZ secret@example.com token=abc'
upload_cases = {
  'ERROR ITMS-90062: This bundle is invalid. The value for key CFBundleShortVersionString' => 'upload_itms_90062',
  'ITMS-90186: Invalid Pre-Release Train. ITMS-90062: x. ITMS-90186: again. ITMS-04238: y. ITMS-90000: z' =>
    'upload_itms_90186_90062_04238',
  'Transporter not found at path /Applications/Transporter.app' => 'upload_transporter_missing',
  'iTMSTransporter is not executable' => 'upload_transporter_missing',
  'altool: No such file or directory' => 'upload_transporter_missing',
  'Could not find altool. Please install Xcode' => 'upload_transporter_missing',
  "Could not find app with app identifier 'com.rem.playground.settings' on account" => 'upload_app_not_found',
  'App not found on App Store Connect' => 'upload_app_not_found',
  'The bundle version 8 has already been used' => 'upload_build_number_used',
  'Build number must be higher than the previously uploaded build' => 'upload_build_number_used',
  'CFBundleVersion must be greater than 7' => 'upload_build_number_used',
  'Authentication credentials are missing or invalid.' => 'upload_auth_failed',
  'Server responded with 401' => 'upload_auth_failed',
  'Unauthorized access' => 'upload_auth_failed',
  'Timed out waiting for build processing' => 'upload_processing_timeout',
  'Build processing timeout reached after 1200 seconds' => 'upload_processing_timeout',
  SECRET => 'upload_user_error_unclassified',
  '' => 'upload_user_error_unclassified',
  nil => 'upload_user_error_unclassified',
  "\xFF\xFEbinary".b => 'upload_user_error_unclassified',
  'ITMS-1234 short code is not Apple\'s 5-digit form' => 'upload_user_error_unclassified'
}
upload_cases.each do |message, code|
  got = upload_guard(message)
  raise "Expected upload guard #{code} for #{message.inspect}, got #{got.inspect}" unless got == code
  raise "Upload guard not allowlist-shaped: #{got}" unless got.match?(/\A[a-z0-9_]{1,64}\z/)
end
raise 'Three ITMS codes must fit the 64-char guard' unless upload_guard('ITMS-11111 ITMS-22222 ITMS-33333 ITMS-44444').length <= 64

# Build record lookup: one read-only GET, its own rescue, never replaces the upload's guard or HTTP status.
def apple_key; end
def record_after(response)
  @responses = [response]
  $release_guard, $release_http = 'upload_auth_failed', nil
  got = upload_build_record('8')
  raise 'Build record lookup replaced the upload guard' unless $release_guard == 'upload_auth_failed' && $release_http.nil?
  raise 'Build record lookup made extra requests' unless @responses.empty?
  got
end
{
  {'data' => [{'id' => 'b', 'attributes' => {'version' => '8'}}]} => 'present',
  {'data' => []} => 'absent',
  {'data' => nil} => 'unknown',
  {} => 'unknown'
}.each do |response, value|
  got = record_after(response)
  raise "Expected build record #{value}, got #{got.inspect}" unless got == value
end
def apple_request(method, path, payload = nil)
  raise 'Unexpected mutation' unless method == :get && payload.nil?
  raise "Unexpected path #{path}" unless path == '/v1/builds?filter%5Bapp%5D=6820738808&filter%5Bversion%5D=8'
  $release_http = 500
  require_release(false, 'Apple API failed', 'apple_api_http')
end
$release_guard, $release_http = 'upload_auth_failed', nil
raise 'Failed lookup must be unknown' unless upload_build_record('8') == 'unknown'
raise 'Failed lookup leaked its guard/status' unless $release_guard == 'upload_auth_failed' && $release_http.nil?

# Whole lane, iOS upload failure: distribution.json carries only fixed codes, nothing is printed.
require 'tmpdir'
Dir.mktmpdir do |dir|
  number = '8'
  sha = 'a' * 40
  File.write(File.join(dir, 'Playground.ipa'), 'ipa')
  digest = Digest::SHA256.file(File.join(dir, 'Playground.ipa')).hexdigest
  File.write(File.join(dir, 'packaging.json'), JSON.generate('platform' => 'ios', 'signing' => 'verified',
    'source_sha' => sha, 'build_number' => number, 'binary_sha256' => digest))
  File.write(File.join(dir, 'admission.json'), JSON.generate('source_sha' => sha, 'build_number' => number,
    'native_run_id' => 1, 'native_run_attempt' => 1))
  ENV.update('GITHUB_ACTIONS' => 'true', 'GITHUB_REF' => 'refs/heads/main', 'PLAYGROUND_OUTPUT' => dir,
    'PLAYGROUND_ADMISSION' => File.join(dir, 'admission.json'), 'PLAYGROUND_PLATFORM' => 'ios',
    'PLAYGROUND_BUILD_NUMBER' => number, 'PLAYGROUND_SOURCE_SHA' => sha)
  [['ITMS-90062: ' + SECRET, 'upload_itms_90062', {'data' => [{'attributes' => {'version' => '8'}}]}, 'present'],
   [SECRET, 'upload_user_error_unclassified', {'data' => []}, 'absent'],
   ['Authentication credentials are missing or invalid. ' + SECRET, 'upload_auth_failed', nil, 'unknown']].each do |message, code, lookup, record|
    calls = []
    define_method(:apple_group) { GROUP }
    define_method(:upload_to_testflight) { |**_| raise message }
    define_method(:apple_request) do |method, path, payload = nil|
      raise 'Unexpected mutation' unless method == :get && payload.nil?
      calls << path
      next {'data' => [{'attributes' => {'version' => '7'}}], 'links' => {}} if calls.length == 1
      raise 'Apple lookup failed' if lookup.nil?
      lookup
    end
    $release_phase, $release_guard, $release_http, $release_upload_build_record = 'inputs', nil, nil, nil
    out = StringIO.new
    stdout, stderr, $stdout, $stderr = $stdout, $stderr, out, out
    begin
      @lanes[:distribute].call
      raise 'Upload failure was swallowed'
    rescue RuntimeError => e
      raise 'Upload exception was not re-raised unchanged' unless e.message == message
    ensure
      $stdout, $stderr = stdout, stderr
    end
    text = File.read(File.join(dir, 'distribution.json'))
    failure = JSON.parse(text)['failure']
    expected = {'phase' => 'upload', 'guard' => code, 'http_status' => nil, 'upload_build_record' => record}
    raise "Wrong failure record #{failure.inspect}" unless failure == expected
    raise 'Build record lookup must be the second and last request' unless calls.length == 2 &&
      calls[1] == '/v1/builds?filter%5Bapp%5D=6820738808&filter%5Bversion%5D=8'
    ['Provider said', 'secret@example.com', 'token=abc', '9ZZ', 'missing or invalid'].each do |leak|
      raise "Provider text leaked into distribution.json: #{leak}" if text.include?(leak)
      raise "Provider text printed: #{leak}" if out.string.include?(leak)
    end
  end
end
puts "6 TestFlight privacy/scope guard scenarios, 15 guard-code cases, #{upload_cases.length} upload-guard cases, " \
  '6 build-record cases and 3 upload-failure lane runs passed; no store requests made.'
