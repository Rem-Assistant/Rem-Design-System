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
  'HTTP 401' => 'upload_auth_failed',
  'status code: 401' => 'upload_auth_failed',
  'statusCode=401' => 'upload_auth_failed',
  '401 Unauthorized' => 'upload_auth_failed',
  # An incidental 401 (a build, asset or line number) is not an auth failure.
  'Build 401 could not be processed' => 'upload_user_error_unclassified',
  'Asset icon-401.png is invalid' => 'upload_user_error_unclassified',
  'Invalid bundle at line 401' => 'upload_user_error_unclassified',
  'Version 1.401 was rejected' => 'upload_user_error_unclassified',
  'Timed out waiting for build processing' => 'upload_processing_timeout',
  'Build processing timeout reached after 1200 seconds' => 'upload_processing_timeout',
  SECRET => 'upload_user_error_unclassified',
  '' => 'upload_user_error_unclassified',
  nil => 'upload_user_error_unclassified',
  "\xFF\xFEbinary".b => 'upload_user_error_unclassified',
  'ITMS-1234 short code is not Apple\'s 5-digit form' => 'upload_user_error_unclassified',
  # Xcode 26 altool and fastlane 2.240.1 wrappers (pilot BuildManager#upload, AltoolTransporterExecutor).
  "Error uploading ipa file: \n [Application Loader Error Output]: The file \u201CPlayground.ipa\u201D couldn\u2019t be " \
    "opened because it isn\u2019t in the correct format. (259)" => 'upload_altool_format_259',
  "The file couldn't be opened because it isn't in the correct format. " + SECRET => 'upload_altool_format_259',
  'Error Domain=NSCocoaErrorDomain Code=259 "x"' => 'upload_altool_format_259',
  'ERROR: Failed to upload package (259)' => 'upload_altool_format_259',
  'Build (259) was rejected' => 'upload_user_error_unclassified',
  'Upload failed with error code 2590' => 'upload_user_error_unclassified',
  "Error uploading ipa file: \n [Application Loader Error Output]: Validation failed (409) Unable to " \
    "process (-19209) " + SECRET => 'upload_apple_code_m19209',
  'Failed (-19241) then (-19232)' => 'upload_apple_code_m19241',
  'ITMS-90062: invalid (-19209)' => 'upload_itms_90062',
  'The bundle version must be higher than the previously uploaded version (-19232)' => 'upload_build_number_used',
  'Range (-) and minus 19209 are not codes' => 'upload_user_error_unclassified',
  'Could not download/upload from App Store Connect! ' + SECRET => 'upload_asc_transfer_failed',
  "Couldn't download/upload from App Store Connect" => 'upload_asc_transfer_failed',
  'Error uploading ipa file: Could not download/upload from App Store Connect!' => 'upload_asc_transfer_failed',
  "Error uploading ipa file: \n [Application Loader Error Output]: The call to the altool completed with a " \
    'non-zero exit status: 1. This indicates a failure.' => 'upload_altool_exit_1',
  "Error uploading ipa file: \n -1 indicates altool exited abnormally; try retrying " + SECRET =>
    'upload_ipa_error_unclassified',
  'Error uploading pkg file: x' => 'upload_user_error_unclassified'
}
# fastlane 2.240.1: pilot raises "Error uploading ipa file: \n " + one "[Application Loader Error Output]: "
# entry per altool ERROR: line, then fastlane's own exit-status line(s), which are not counted.
def pilot_message(errors, exit_status = 1)
  entries = errors.dup
  if exit_status
    entries << "The call to the altool completed with a non-zero exit status: #{exit_status}. This indicates a failure."
    entries << '-1 indicates altool exited abnormally; try retrying (see https://github.com/fastlane/fastlane/issues/21535)' if exit_status == -1
  end
  "Error uploading ipa file: \n " + entries.map { |e| "[Application Loader Error Output]: #{e}" }.join("\n")
end
exit_cases = {
  pilot_message([]) => 'upload_altool_exit_1',
  pilot_message([], 176) => 'upload_altool_exit_176',
  pilot_message(['Failed to upload package. ' + SECRET]) => 'upload_altool_exit_1_errlines_1',
  pilot_message(['Failed to get json from defaults ' + SECRET] * 2) => 'upload_altool_exit_1_errlines_2to5',
  pilot_message(['x'] * 5, 9) => 'upload_altool_exit_9_errlines_2to5',
  pilot_message(['Unexpected ' + SECRET] * 6, -1) => 'upload_altool_exit_m1_errlines_6plus',
  pilot_message(['x'] * 40, -1) => 'upload_altool_exit_m1_errlines_6plus',
  pilot_message([], 999_999) => 'upload_altool_exit_999999',
  pilot_message([], -12_345) => 'upload_altool_exit_m12345',
  # Longer than 6 characters (with the m): no exit code; the ipa fallback, still with its count.
  pilot_message([], 1_234_567) => 'upload_ipa_error_unclassified',
  pilot_message(['y'], -123_456) => 'upload_ipa_error_unclassified_errlines_1',
  # Precedence: ITMS, semantic patterns (259 last), then the Apple code all beat the exit status.
  pilot_message(['ITMS-90062: bad ' + SECRET]) => 'upload_itms_90062',
  pilot_message(['The bundle version must be higher than the previously uploaded version']) => 'upload_build_number_used',
  pilot_message(["The file couldn't be opened because it isn't in the correct format. (259)"]) => 'upload_altool_format_259',
  pilot_message(['Validation failed (409) Unable to process (-19209) ' + SECRET] * 3) => 'upload_apple_code_m19209',
  # The exit status beats the transfer-failed and ipa-error fallbacks.
  pilot_message(['Could not download/upload from App Store Connect!']) => 'upload_altool_exit_1_errlines_1',
  # No exit status: fallbacks carry the count bucket; counts only, never text.
  pilot_message(['z ' + SECRET], nil) => 'upload_ipa_error_unclassified_errlines_1',
  "Could not download/upload from App Store Connect!\n[Application Loader Error Output]: a\n" \
    '[Application Loader Error Output]: b' => 'upload_asc_transfer_failed_errlines_2to5',
  "ERROR: one #{SECRET}\n*** Error: two\n[altool] ERROR: three" => 'upload_user_error_unclassified_errlines_2to5',
  "Something\nERROR: only #{SECRET}" => 'upload_user_error_unclassified_errlines_1',
  # Empty entries, fastlane's own status lines and mid-line mentions are not altool error lines.
  pilot_message(['', ' ']) => 'upload_altool_exit_1',
  'Error uploading ipa file: saw ERROR: inline' => 'upload_ipa_error_unclassified',
  'exit status: 1 but not the altool line' => 'upload_user_error_unclassified',
  # fastlane's own PTY lines are not altool error lines.
  pilot_message(['Process crashed'], -1) => 'upload_altool_exit_m1',
  pilot_message(['Process crashed', 'freed pointer was not the last allocation'], -1) => 'upload_altool_exit_m1_errlines_1',
  # The workaround flag refused by altool, only when the flag is named.
  pilot_message(["[altool.600] Unknown option '--use-old-altool'. " + SECRET], 64) => 'upload_altool_flag_rejected',
  "altool: unrecognized option `--use-old-altool'" => 'upload_altool_flag_rejected',
  'Invalid argument: --use-old-altool' => 'upload_altool_flag_rejected',
  '--use-old-altool is not supported by this version of altool' => 'upload_altool_flag_rejected',
  '--use-old-altool is deprecated' => 'upload_user_error_unclassified',
  'Unknown option --verbose' => 'upload_user_error_unclassified',
  pilot_message([], 64) => 'upload_altool_exit_64',
  # fastlane's altool reader failed on non-ASCII output under a non-UTF-8 locale.
  pilot_message(['invalid byte sequence in US-ASCII'], -1) => 'upload_output_encoding_error',
  'invalid byte sequence in UTF-8' => 'upload_output_encoding_error'
}
upload_cases.merge!(exit_cases)
upload_cases.each do |message, code|
  got = upload_guard(message)
  raise "Expected upload guard #{code} for #{message.inspect}, got #{got.inspect}" unless got == code
  raise "Upload guard not allowlist-shaped: #{got}" unless got.match?(/\A[a-z0-9_]{1,64}\z/)
end
raise 'Three ITMS codes must fit the 64-char guard' unless upload_guard('ITMS-11111 ITMS-22222 ITMS-33333 ITMS-44444').length <= 64
# Every code is a fixed word, a fixed word plus digits, or one of three fixed count buckets.
UPLOAD_CODE_GRAMMAR = /\Aupload_(itms_\d{5}(_\d{5}){0,2}|apple_code_m\d{1,6}|altool_format_259|transporter_missing|
  altool_flag_rejected|output_encoding_error|
  app_not_found|build_number_used|auth_failed|processing_timeout|((altool_exit_m?\d{1,6}|asc_transfer_failed|
  ipa_error_unclassified|user_error_unclassified)(_errlines_(1|2to5|6plus))?))\z/x
upload_cases.each_key do |message|
  code = upload_guard(message)
  raise "Upload guard outside the code grammar: #{code}" unless code.match?(UPLOAD_CODE_GRAMMAR) && code.length <= 64
end
raise 'Apple code must fit the 64-char guard' unless upload_guard('(-999999)') == 'upload_apple_code_m999999'
# Codes are fixed or digit-only: no message text survives classification.
upload_cases.each_key do |message|
  next unless message.is_a?(String) && message.include?(SECRET)
  code = upload_guard(message)
  ['Provider', 'secret', 'example', 'token', 'abc', '9zz'].each do |leak|
    raise "Upload guard leaked message text #{leak}: #{code}" if code.include?(leak.downcase)
  end
end

# A wrapper keeps the tool's category: the cause chain (three levels) is classified, never kept.
def chain(*messages)
  messages.reverse.reduce(nil) do |cause, message|
    begin
      cause ? (begin; raise cause; rescue StandardError; raise message; end) : raise(message)
    rescue StandardError => e
      e
    end
  end
end
{
  chain('Upload step failed', pilot_message(["isn't in the correct format (259) " + SECRET])) => 'upload_altool_format_259',
  chain('Upload step failed', pilot_message(['Unable to process (-19209)'])) => 'upload_apple_code_m19209',
  chain('outer', 'middle', 'ITMS-90062: deepest') => 'upload_itms_90062',
  chain('outer', 'middle', 'inner', 'ITMS-90062: too deep') => 'upload_user_error_unclassified',
  chain('Upload step failed') => 'upload_user_error_unclassified',
  chain('Upload step failed ' + SECRET, 'unknown inner ' + SECRET) => 'upload_user_error_unclassified'
}.each do |error, code|
  got = upload_guard(error)
  raise "Expected cause-chain guard #{code}, got #{got.inspect}" unless got == code
  raise "Cause-chain guard outside the grammar: #{got}" unless got.match?(UPLOAD_CODE_GRAMMAR)
end
errno = begin
  begin
    raise pilot_message(['(-19209) ' + SECRET])
  rescue RuntimeError
    raise Errno::EPIPE, 'upload stream'
  end
rescue Errno::EPIPE => e
  e
end
raise 'Errno wrapper must keep the inner Apple code' unless upload_guard(errno) == 'upload_apple_code_m19209'
raise 'A string has no cause chain' unless upload_guard(pilot_message([], 3)) == 'upload_altool_exit_3'

# The upload reads altool output as UTF-8 whatever the locale, and the previous encoding comes back.
previous_encoding = Encoding.default_external
Encoding.default_external = Encoding::US_ASCII
seen = nil
with_upload_utf8 { seen = Encoding.default_external }
raise "Upload did not read UTF-8: #{seen}" unless seen == Encoding::UTF_8
raise 'Encoding not restored' unless Encoding.default_external == Encoding::US_ASCII
begin
  with_upload_utf8 { raise 'Upload failed' }
rescue RuntimeError
end
raise 'Encoding not restored after a failed upload' unless Encoding.default_external == Encoding::US_ASCII
Encoding.default_external = previous_encoding

# The Xcode 26 altool workaround is set only around the upload call and restored afterwards.
ALTOOL_ENV = 'DELIVER_ALTOOL_ADDITIONAL_UPLOAD_PARAMETERS'
raise 'Wrong altool workaround' unless UPLOAD_ALTOOL_PARAMETERS == '--use-old-altool'
ENV.delete(ALTOOL_ENV)
seen = nil
with_upload_altool_parameters { seen = ENV[ALTOOL_ENV] }
raise "Workaround not set during upload: #{seen.inspect}" unless seen == '--use-old-altool'
raise 'Workaround leaked past upload' if ENV.key?(ALTOOL_ENV)
begin
  with_upload_altool_parameters { raise 'Upload failed' }
rescue RuntimeError
end
raise 'Workaround leaked past a failed upload' if ENV.key?(ALTOOL_ENV)
ENV[ALTOOL_ENV] = '--previous'
with_upload_altool_parameters { seen = ENV[ALTOOL_ENV] }
raise 'Workaround must replace, not append' unless seen == '--use-old-altool'
raise 'Previous value not restored' unless ENV[ALTOOL_ENV] == '--previous'
ENV.delete(ALTOOL_ENV)

# Build record lookup: up to 6 read-only GETs ~30s apart, stopping at 'present'; the last observation
# otherwise wins. Its own rescue; never replaces the upload's guard or HTTP status. Sleeps are stubbed.
def apple_key; end
@waits = []
def upload_record_wait(seconds)
  @waits << seconds
end
BUILD_PATH = '/v1/builds?filter%5Bapp%5D=6820738808&filter%5Bversion%5D=8'
def apple_request(method, path, payload = nil)
  raise 'Unexpected mutation' unless method == :get && payload.nil?
  raise "Unexpected path #{path}" unless path == BUILD_PATH
  raise 'Unexpected extra provider call' if @responses.empty?
  response = @responses.shift
  if response == :http_error
    $release_http = 500
    require_release(false, 'Apple API failed', 'apple_api_http')
  end
  response
end
present = {'data' => [{'id' => 'b', 'attributes' => {'version' => '8'}}]}
absent = {'data' => []}
build_record_cases = [
  [[present], 'present', 1],
  [[absent] * 6, 'absent', 6],
  [[absent, absent, present], 'present', 3],
  [[absent] * 5 + [present], 'present', 6],
  [[{'data' => nil}] * 6, 'unknown', 6],
  [[{}] * 6, 'unknown', 6],
  [[absent] * 5 + [{}], 'unknown', 6],
  [[:http_error] * 6, 'unknown', 6],
  [[:http_error, :http_error, present], 'present', 3],
  [[:http_error] * 5 + [absent], 'absent', 6],
  [[absent] * 7, 'absent', 6]
]
build_record_cases.each do |responses, value, gets|
  @responses, @waits = responses.dup, []
  $release_guard, $release_http = 'upload_auth_failed', nil
  got = upload_build_record('8')
  raise "Expected build record #{value}, got #{got.inspect}" unless got == value
  raise 'Build record lookup replaced the upload guard' unless $release_guard == 'upload_auth_failed' && $release_http.nil?
  raise "Expected #{gets} GETs, made #{responses.length - @responses.length}" unless responses.length - @responses.length == gets
  raise "Expected #{gets - 1} waits of 30s, got #{@waits.inspect}" unless @waits == [30] * (gets - 1)
end
# An interrupt during a wait is not swallowed, and still restores the upload's guard and status.
@responses = [absent] * 6
$release_guard, $release_http = 'upload_auth_failed', nil
define_method(:upload_record_wait) { |_| $release_http = 503; raise Interrupt }
begin
  upload_build_record('8')
  raise 'Interrupt was swallowed by the build record lookup'
rescue Interrupt
end
raise 'Interrupted lookup leaked its status' unless $release_guard == 'upload_auth_failed' && $release_http.nil?
define_method(:upload_record_wait) { |seconds| @waits << seconds }

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
    'PLAYGROUND_BUILD_NUMBER' => number, 'PLAYGROUND_SOURCE_SHA' => sha,
    'PLAYGROUND_EPHEMERAL' => File.join(dir, 'ephemeral'))
  Dir.mkdir(File.join(dir, 'ephemeral'))
  # fastlane's report.xml (it carries action error messages) must go to the ephemeral directory.
  ENV.delete('FL_REPORT_PATH')
  ENV['PLAYGROUND_EPHEMERAL'] = File.join(dir, 'missing')
  $release_guard = nil
  begin
    @lanes[:distribute].call
    raise 'Missing ephemeral directory was accepted'
  rescue RuntimeError => e
    raise "Wrong ephemeral rejection: #{e.message}" unless e.message == 'Ephemeral directory required'
  end
  raise 'Missing ephemeral directory needs its own guard' unless $release_guard == 'ephemeral_missing'
  raise 'Report path set without an ephemeral directory' if ENV.key?('FL_REPORT_PATH')
  ENV['PLAYGROUND_EPHEMERAL'] = File.join(dir, 'ephemeral')
  [['ITMS-90062: ' + SECRET, 'upload_itms_90062', {'data' => [{'attributes' => {'version' => '8'}}]}, 'present', 1],
   [SECRET, 'upload_user_error_unclassified', {'data' => []}, 'absent', 6],
   ['Authentication credentials are missing or invalid. ' + SECRET, 'upload_auth_failed', nil, 'unknown', 6],
   ["Error uploading ipa file: \n [Application Loader Error Output]: isn't in the correct format (259) " + SECRET,
    'upload_altool_format_259', {'data' => []}, 'absent', 6],
   ['Could not download/upload from App Store Connect! ' + SECRET, 'upload_asc_transfer_failed',
    {'data' => [{'attributes' => {'version' => '8'}}]}, 'present', 1],
   [pilot_message(['Failed to get json from defaults ' + SECRET] * 2, -1), 'upload_altool_exit_m1_errlines_2to5',
    {'data' => []}, 'absent', 6]].each do |message, code, lookup, record, gets|
    calls = []
    @waits = []
    altool = :unset
    define_method(:apple_group) { GROUP }
    define_method(:upload_to_testflight) { |**_| altool = ENV[ALTOOL_ENV]; raise message }
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
    raise 'fastlane report.xml not routed to the ephemeral directory' unless ENV['FL_REPORT_PATH'] == File.join(dir, 'ephemeral')
    raise "Build record lookups must follow the upload: #{calls.inspect}" unless calls.length == 1 + gets &&
      calls[1..].all? { |path| path == BUILD_PATH }
    raise "Wrong waits #{@waits.inspect}" unless @waits == [30] * (gets - 1)
    raise "Upload ran without the altool workaround: #{altool.inspect}" unless altool == '--use-old-altool'
    raise 'Altool workaround leaked past the upload' if ENV.key?(ALTOOL_ENV)
    ['Provider said', 'secret@example.com', 'token=abc', '9ZZ', 'missing or invalid', 'correct format',
     '(259)', 'download/upload', 'Error uploading', 'Application Loader', 'exit status', 'json from defaults',
     'exited abnormally'].each do |leak|
      raise "Provider text leaked into distribution.json: #{leak}" if text.include?(leak)
      raise "Provider text printed: #{leak}" if out.string.include?(leak)
    end
  end
  # An interrupt is not an upload failure: no classification and no build-record lookup.
  calls = []
  define_method(:upload_to_testflight) { |**_| raise Interrupt }
  define_method(:apple_request) do |method, path, payload = nil|
    raise 'Unexpected mutation' unless method == :get && payload.nil?
    calls << path
    {'data' => [{'attributes' => {'version' => '7'}}], 'links' => {}}
  end
  $release_phase, $release_guard, $release_http, $release_upload_build_record = 'inputs', nil, nil, nil
  out = StringIO.new
  stdout, stderr, $stdout, $stderr = $stdout, $stderr, out, out
  begin
    @lanes[:distribute].call
    raise 'Interrupt was swallowed'
  rescue Interrupt
  ensure
    $stdout, $stderr = stdout, stderr
  end
  failure = JSON.parse(File.read(File.join(dir, 'distribution.json')))['failure']
  raise "Interrupt was classified: #{failure.inspect}" unless failure == {'phase' => 'upload', 'guard' => 'exception_interrupt', 'http_status' => nil}
  raise 'Interrupt triggered a build-record lookup' unless calls.length == 1
  raise 'Altool workaround leaked past an interrupted upload' if ENV.key?(ALTOOL_ENV)
end
puts "6 TestFlight privacy/scope guard scenarios, 15 guard-code cases, #{upload_cases.length} upload-guard cases, " \
  "8 cause-chain cases, 3 UTF-8-scope checks, 4 altool-scope checks, " \
  "#{build_record_cases.length + 1} build-record polling cases, 6 upload-failure lane runs, " \
  '1 interrupt lane run and 1 ephemeral-directory guard passed; no store requests made.'
