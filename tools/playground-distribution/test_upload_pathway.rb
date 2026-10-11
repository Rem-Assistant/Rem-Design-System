# End-to-end upload failure pathway with the real locked fastlane 2.240.1; run with `bundle exec`.
# For each sanitized, synthetic fixture (written from fastlane's and altool's own message formats):
#   stub `xcrun altool` prints the fixture and exits -> real `bundle exec fastlane distribute` (CLI, runner,
#   upload_to_testflight, pilot BuildManager#upload, ItunesTransporter, AltoolTransporterExecutor,
#   FastlanePty, UI.user_error!) -> the Fastfile upload rescue (upload_guard, build-record polling) and lane
#   rescue writing distribution.json -> `summary.py result` (public line and step summary) ->
#   `summary.py cleanup`. upload_pathway_harness.rb lists the only replaced parts; no network is reachable.
# Every fixture carries unique secret sentinels; none may reach any public output or survive cleanup.
require 'json'
require 'digest'
require 'base64'
require 'fileutils'
require 'open3'
require 'openssl'
require 'rbconfig'
require 'tmpdir'
require 'yaml'
require 'zip'

HERE = __dir__
HARNESS = File.join(HERE, 'upload_pathway_harness.rb')
SUMMARY = File.join(HERE, 'summary.py')
FASTLANE = Gem.bin_path('fastlane', 'fastlane')
SHA = 'a' * 40
GROUP = '12345678-1234-1234-1234-123456789012'
REVIEW = 'https://github.com/Rem-Assistant/Rem-Design-System/pull/87#issuecomment-123'

KEY_ID = 'Z9SENTK3Y7'
ISSUER = 'c0ffee00-5e47-4e11-9a5e-000000badd0e'
EMAIL = 'sentinel.uploader+q7@example.invalid'
JWT = 'eyJhbGciOiJFUzI1NiIsImtpZCI6IlNFTlRJTkVMIn0.eyJpc3MiOiJzZW50aW5lbC1xNyJ9.c2VudGluZWwtc2lnbmF0dXJlLXE3'
KEY_PATH = "/Users/runner/work/sentinel-q7/private_keys/AuthKey_#{KEY_ID}.p8"
TEAM = 'SENTTEAMQ7'
PROVIDER = 'Provider said SENTINEL_PROVIDER_Q7'
PEM = OpenSSL::PKey::EC.generate('prime256v1').to_pem
KEY_BASE64 = Base64.strict_encode64(PEM)
PEM_BODY = PEM.lines[1].strip # first line of the private key material
SECRET_TEXT = "#{PROVIDER} team #{TEAM} #{EMAIL} key #{KEY_ID} issuer #{ISSUER} jwt #{JWT} at #{KEY_PATH}"
SENTINELS = [KEY_ID, ISSUER, EMAIL, JWT, KEY_PATH, 'sentinel-q7', TEAM, 'SENTINEL_PROVIDER_Q7', PEM_BODY,
             KEY_BASE64[0, 40]].freeze

def altool_log(*lines)
  lines.map { |l| "2026-10-11 15:16:#{format('%02d', lines.index(l))}.123 #{l}" }.join("\n") + "\n"
end

# name => [description, altool fixture or nil, altool exit (Integer, or :segv), harness mode, Apple record, public line,
#          optional environment overrides]
FIXTURES = {
  'a_itms' => ['Xcode 26 altool ERROR: lines with ITMS codes',
    altool_log("INFO: [ContentDelivery.Uploader.600] Upload for #{KEY_ID} on #{TEAM} started",
      "ERROR: [ContentDelivery.Uploader.600] Validation failed (409) Invalid Pre-Release Train. ITMS-90186: #{SECRET_TEXT}",
      "ERROR: [ContentDelivery.Uploader.600] ITMS-90062: This bundle is invalid. #{EMAIL}",
      'UPLOAD FAILED with 2 errors'), 1, nil, 'absent',
    'stopped in phase upload at guard upload_itms_90186_90062; HTTP status none; build record absent'],
  'b_format_259' => ['"(259)" format error',
    altool_log("ERROR: [altool.600003A1C000] The file “Playground.ipa” couldn’t be opened because it " \
      "isn’t in the correct format. (259) #{KEY_PATH}", "INFO: #{SECRET_TEXT}"), 1, nil, 'absent',
    'stopped in phase upload at guard upload_altool_format_259; HTTP status none; build record absent'],
  'c_apple_code' => ['Apple negative code (-19209)',
    altool_log("ERROR: [ContentDelivery.Uploader.600] Unable to process upload (-19209) #{SECRET_TEXT}"), 1, nil,
    'present',
    'stopped in phase upload at guard upload_apple_code_m19209; HTTP status none; build record present; ' \
    'next step: reconcile the existing build, never re-upload'],
  'd_exit_unknown_error' => ['non-zero exit, unknown ERROR: text',
    altool_log("ERROR: [altool.600] Something unexpected #{SECRET_TEXT}"), 3, nil, 'error',
    'stopped in phase upload at guard upload_altool_exit_3_errlines_1; HTTP status none; build record unknown'],
  'd_exit_no_error_lines' => ['non-zero exit, no ERROR: lines',
    altool_log("INFO: #{SECRET_TEXT}", 'NOTICE: giving up'), 70, nil, 'absent',
    'stopped in phase upload at guard upload_altool_exit_70; HTTP status none; build record absent'],
  'e_exit_m1_crash' => ['altool killed by a signal: fastlane "Process crashed", exit -1',
    altool_log("INFO: Uploading for #{EMAIL}", "ERROR: [altool.600] freed pointer was not the last allocation #{JWT}"),
    :segv, nil, 'absent',
    'stopped in phase upload at guard upload_altool_exit_m1_errlines_1; HTTP status none; build record absent'],
  'f_succeeded_then_nonzero' => ['"UPLOAD SUCCEEDED" then a non-zero exit (fastlane#30130)',
    altool_log("ERROR: [altool.10190B070] Failed to get json from defaults for #{SECRET_TEXT}",
      'UPLOAD SUCCEEDED with no errors'), 1, nil, 'absent',
    'stopped in phase processing_check at guard build_not_valid; HTTP status none'],
  'g_pre_altool_bad_ipa' => ['pre-altool pilot failure: unreadable IPA (rubyzip Zip::Error)',
    nil, nil, 'bad_ipa', 'absent',
    'stopped in phase upload at guard upload_user_error_unclassified; HTTP status none; build record absent'],
  'h_wrapped_runtime' => ['RuntimeError wrapper; pilot\'s 259 error is its cause',
    altool_log("ERROR: [altool.600] The file couldn't be opened because it isn't in the correct format. (259) " \
      "#{SECRET_TEXT}"), 1, 'wrapped_runtime', 'absent',
    'stopped in phase upload at guard upload_altool_format_259; HTTP status none; build record absent'],
  'h_wrapped_errno' => ['Errno::EPIPE wrapper; pilot\'s (-19209) error is its cause',
    altool_log("ERROR: [ContentDelivery.Uploader.600] Unable to process upload (-19209) #{SECRET_TEXT}"), 1,
    'wrapped_errno', 'absent',
    'stopped in phase upload at guard upload_apple_code_m19209; HTTP status none; build record absent'],
  'i_unknown_message' => ['completely unknown pilot message', nil, nil, 'unknown_message', 'absent',
    'stopped in phase upload at guard upload_user_error_unclassified; HTTP status none; build record absent'],
  'j_flag_rejected_error_line' => ['--use-old-altool rejected on an ERROR: line',
    altool_log("ERROR: [altool.600] Unknown option '--use-old-altool'. #{SECRET_TEXT}"), 64, nil, 'absent',
    'stopped in phase upload at guard upload_altool_flag_rejected; HTTP status none; build record absent'],
  'j_flag_rejected_plain' => ['--use-old-altool rejected without ERROR: (text stays in the private log)',
    "altool: unrecognized option '--use-old-altool' #{SECRET_TEXT}\nUsage: altool --upload-app ...\n", 64, nil,
    'absent', 'stopped in phase upload at guard upload_altool_exit_64; HTTP status none; build record absent'],
  'k_c_locale_still_classified' => ['the 259 fixture with the job in a C locale: the step and Fastfile read UTF-8',
    altool_log("ERROR: [altool.600003A1C000] The file \u201CPlayground.ipa\u201D couldn\u2019t be opened because it " \
      "isn\u2019t in the correct format. (259) #{SECRET_TEXT}"), 1, nil, 'absent',
    'stopped in phase upload at guard upload_altool_format_259; HTTP status none; build record absent',
    {'LANG' => 'C', 'LC_ALL' => 'C'}],
  'l_parser_error_if_masked' => ['the same output read as US-ASCII anyway (UTF-8 scope disabled): distinct code',
    altool_log("ERROR: [altool.600003A1C000] The file \u201CPlayground.ipa\u201D couldn\u2019t be opened because it " \
      "isn\u2019t in the correct format. (259) #{SECRET_TEXT}"), 1, 'no_utf8_scope', 'absent',
    'stopped in phase upload at guard upload_output_encoding_error; HTTP status none; build record absent',
    {'LANG' => 'C', 'LC_ALL' => 'C', 'PATHWAY_RUBYOPT' => '-EUS-ASCII'}]
}.freeze
# The job's ambient locale before the store step (hosted runners: UTF-8; macOS has no C.UTF-8).
UTF8_LOCALE = RUBY_PLATFORM.include?('darwin') ? 'en_US.UTF-8' : 'C.UTF-8'
# The workflow's own store step script (umask, task-owned TMPDIR, UTF-8 locale, private log redirect).
WORKFLOW = File.expand_path('../../.github/workflows/playground-distribute.yml', HERE)
RELEASE = YAML.safe_load(File.read(WORKFLOW))
STORE_STEP = RELEASE['jobs']['distribute']['steps'].find { |step| step['name'].to_s.start_with?('Upload directly') }
  .fetch('run')
# The workflow's static environment (FASTLANE_* switches, BUNDLE_FROZEN); expressions are set per fixture.
WORKFLOW_ENV = RELEASE['env'].merge(RELEASE['jobs']['distribute']['env']).reject { |_, v| v.to_s.include?('${{') }

def write_ipa(path, valid)
  return File.binwrite(path, "not a zip #{SECRET_TEXT}") unless valid
  plist = <<~PLIST
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
    <plist version="1.0"><dict>
    <key>CFBundleIdentifier</key><string>com.rem.playground.settings</string>
    <key>CFBundleShortVersionString</key><string>0.1.0</string>
    <key>CFBundleVersion</key><string>8</string>
    <key>DTPlatformName</key><string>iphoneos</string>
    </dict></plist>
  PLIST
  Zip::OutputStream.open(path) do |zip|
    zip.put_next_entry('Payload/Playground.app/Info.plist')
    zip.write(plist)
  end
end

def files_under(dir)
  Dir.glob(File.join(dir, '**', '*'), File::FNM_DOTMATCH).reject { |f| File.basename(f) =~ /\A\.\.?\z/ }
end

def leaks(text)
  SENTINELS.select { |s| text.include?(s) || text.downcase.include?(s.downcase) }
end

def check(ok, message)
  raise message unless ok
end

def run_fixture(name, (_, output, exit_code, mode, record, expected, overrides))
  overrides = (overrides || {}).dup
  extra_rubyopt = overrides.delete('PATHWAY_RUBYOPT')
  Dir.mktmpdir('pathway') do |root|
    # system-tmp is the job's ordinary TMPDIR/TMP/TEMP; home is HOME. Neither is a cleaned path.
    runner_temp, system_tmp, home, harness, bin =
      %w[runner-temp system-tmp home harness harness/bin].map { |d| File.join(root, d) }
    [runner_temp, system_tmp, home, bin].each { |d| FileUtils.mkdir_p(d) }
    out = File.join(runner_temp, 'playground-package')
    FileUtils.mkdir_p(out)
    ephemeral = File.join(runner_temp, 'playground-ephemeral')
    Dir.mkdir(ephemeral, 0o700)
    ipa = File.join(out, 'Playground.ipa')
    write_ipa(ipa, mode != 'bad_ipa')
    File.write(File.join(out, 'packaging.json'), JSON.generate('platform' => 'ios', 'signing' => 'verified',
      'source_sha' => SHA, 'build_number' => '8', 'binary_sha256' => Digest::SHA256.file(ipa).hexdigest))
    admission = File.join(runner_temp, 'admission.json')
    File.write(admission, JSON.generate('source_sha' => SHA, 'build_number' => '8', 'native_run_id' => 1,
      'native_run_attempt' => 1, 'visual_review_url' => REVIEW))
    fixture, args, calls = %w[altool-output.txt altool-args.txt apple-calls.txt].map { |f| File.join(harness, f) }
    File.write(fixture, output.to_s)
    xcrun = File.join(bin, 'xcrun')
    File.write(xcrun, <<~SH)
      #!/bin/sh
      printf '%s\\n' "$@" > "$PATHWAY_ARGS"
      [ "$1" = altool ] || exit 69
      cat "$PATHWAY_ALTOOL_OUTPUT"
      if [ "$PATHWAY_ALTOOL_EXIT" = segv ]; then
        # Crash as fastlane sees it: the process it spawned dies by a signal. dash keeps its `sh -c` parent
        # (signal that one); bash execs into this script (then this process is the one fastlane waits for).
        case "$(ps -o comm= -p "$PPID" 2>/dev/null)" in *sh) kill -SEGV "$PPID" ;; esac
        kill -SEGV $$
      fi
      exit "$PATHWAY_ALTOOL_EXIT"
    SH
    File.chmod(0o755, xcrun)
    log = File.join(runner_temp, 'playground-store-private.log')
    store_tmp = File.join(runner_temp, 'playground-store-tmp')
    report_in_checkout = File.join(HERE, 'fastlane', 'report.xml')
    FileUtils.rm_f(report_in_checkout)
    env = {
      'PATH' => "#{bin}:#{ENV.fetch('PATH')}", 'HOME' => home, 'RUNNER_TEMP' => runner_temp,
      'TMPDIR' => system_tmp, 'TMP' => system_tmp, 'TEMP' => system_tmp,
      'RUBYOPT' => [ENV['RUBYOPT'], "-r#{HARNESS}", extra_rubyopt].compact.join(' ').strip,
      'LANG' => UTF8_LOCALE, 'LC_ALL' => UTF8_LOCALE,
      'HTTP_PROXY' => nil, 'HTTPS_PROXY' => nil, 'http_proxy' => nil, 'https_proxy' => nil,
      'FASTLANE_DISABLE_COLORS' => '1', 'CI' => 'true', 'FASTLANE_HIDE_GITHUB_ISSUES' => nil,
      'DELIVER_ALTOOL_ADDITIONAL_UPLOAD_PARAMETERS' => nil, 'FL_REPORT_PATH' => nil,
      'GITHUB_ACTIONS' => 'true', 'GITHUB_REF' => 'refs/heads/main', 'PLAYGROUND_OUTPUT' => out,
      'PLAYGROUND_ADMISSION' => admission, 'PLAYGROUND_EPHEMERAL' => ephemeral, 'PLAYGROUND_PLATFORM' => 'ios',
      'PLAYGROUND_BUILD_NUMBER' => '8', 'PLAYGROUND_SOURCE_SHA' => SHA, 'PLAYGROUND_ASC_KEY_ID' => KEY_ID,
      'PLAYGROUND_ASC_ISSUER_ID' => ISSUER, 'PLAYGROUND_ASC_KEY_BASE64' => KEY_BASE64,
      'PLAYGROUND_TESTFLIGHT_GROUP_ID' => GROUP,
      'PATHWAY_ALTOOL_OUTPUT' => fixture, 'PATHWAY_ALTOOL_EXIT' => exit_code.to_s, 'PATHWAY_ARGS' => args,
      'PATHWAY_CALLS' => calls, 'PATHWAY_RECORD' => record, 'PATHWAY_MODE' => mode.to_s,
      'PATHWAY_SENTINEL_TEXT' => SECRET_TEXT
    }.merge(WORKFLOW_ENV).merge(overrides)
    # The workflow's store step itself, in the shell GitHub uses for `run:`; stdout/stderr here is public.
    step_out, step_status = Open3.capture2e(env, 'bash', '--noprofile', '--norc', '-eo', 'pipefail', '-c', STORE_STEP,
      chdir: HERE)
    check(!step_status.success?, "#{name}: the store step must fail")
    # A host without en_US.UTF-8 makes bash warn on `export LC_ALL`; that diagnostic carries no provider data.
    public_lines = step_out.lines.reject { |l| l.match?(/\Abash: line \d+: warning: setlocale: LC_ALL: cannot change locale/) }
    check(public_lines.join.strip == 'Store step failed or is unresolved. Inspect provider state before rerunning; do not ' \
      'blindly re-upload.', "#{name}: unexpected public step output #{step_out.inspect}")
    private_log = File.read(log)
    check(!private_log.include?('pathway harness: network blocked'), "#{name}: a provider call tried the network")
    check(!private_log.include?('Looking for related GitHub issues'), "#{name}: fastlane searched GitHub with the error")
    distribution = File.read(File.join(out, 'distribution.json'))
    staged = files_under(store_tmp)
    report = File.join(ephemeral, 'report.xml')
    check(File.exist?(report) && !File.exist?(report_in_checkout), "#{name}: fastlane report.xml not in the ephemeral dir")
    report_text = File.read(report)
    # summary.py result: the one public log line and the step summary.
    step = File.join(root, 'step-summary.md')
    public_out, public_err, = Open3.capture3({'GITHUB_STEP_SUMMARY' => step}, 'python3', SUMMARY, 'result',
      File.join(out, 'distribution.json'))
    line = public_out.strip
    step_text = File.read(step)
    # summary.py cleanup: only the task-owned runner paths are removed.
    _, cleanup_err, cleanup = Open3.capture3({'RUNNER_TEMP' => runner_temp}, 'python3', SUMMARY, 'cleanup', runner_temp)
    check(cleanup.success?, "#{name}: cleanup failed #{cleanup_err}")
    check(!File.exist?(store_tmp), "#{name}: the store temporary directory survived cleanup")
    check(files_under(runner_temp).empty?, "#{name}: files survived cleanup in RUNNER_TEMP: #{files_under(runner_temp)}")
    # Everything outside the cleaned paths that the store step could have written to.
    left = files_under(system_tmp) + files_under(home)

    check(line == "Distribution state: upload_started; #{expected}", "#{name}: public line was #{line.inspect}")
    check(step_text.include?('"distribution": "upload_started"'), "#{name}: step summary lacks the record")
    if output
      argv = File.read(args).split("\n")
      check(argv.first(2) == %w[altool --upload-app] && argv.count('--use-old-altool') == 1,
        "#{name}: altool did not get the scoped workaround: #{argv.inspect}")
      # Positive control: fastlane staged its IPA copies in the task-owned directory.
      check(staged.any? { |f| f.end_with?('.ipa') }, "#{name}: fastlane did not stage the IPA in the store tmp dir")
    else
      check(!File.exist?(args), "#{name}: altool must not run")
    end
    lookups = File.readlines(calls, chomp: true)
    check(lookups.first == '/v1/builds?filter%5Bapp%5D=6820738808&limit=200', "#{name}: build list not first")
    check(!File.exist?(log), "#{name}: private log survived cleanup")
    check(left.none? { |f| f.end_with?('.ipa', '.itmsp', 'metadata.xml', '.p8') || File.basename(f).start_with?('AuthKey_') },
      "#{name}: fastlane staging survived outside the cleaned paths: #{left}")
    { 'distribution.json' => distribution, 'public line' => line + public_err, 'public step output' => step_out,
      'step summary' => step_text, 'leftover names' => left.join("\n"),
      'leftover contents' => left.select { |f| File.file?(f) }.map { |f| File.binread(f).force_encoding('UTF-8').scrub }.join("\n") }
      .each { |where, text| check(leaks(text).empty?, "#{name}: #{leaks(text).inspect} leaked into #{where}") }
    # Positive control: the sentinels did flow through the real path into the (now deleted) private log
    # (f: fastlane prints nothing of an upload it judged successful; g fails before any provider text exists),
    # and fastlane's report.xml carries pilot's failure message (provider text), so it must stay in a cleaned
    # path: shown on fixture a, whose pilot message holds the sentinels.
    check(!leaks(private_log).empty?, "#{name}: fixture never reached fastlane output") unless name =~ /\A[fg]_/
    check(!leaks(report_text).empty?, "#{name}: report.xml did not carry the action message") if name == 'a_itms'
    [line, lookups.length, staged.count { |f| f.end_with?('.ipa') }]
  end
end

only = ARGV.empty? ? FIXTURES.keys : ARGV
rows = only.map { |name| [name, *run_fixture(name, FIXTURES.fetch(name))] }
puts '| Fixture | Public line | Apple GETs | IPA copies staged (then removed) |'
puts '|---|---|---|---|'
rows.each { |name, line, gets, left| puts "| #{name}: #{FIXTURES[name][0]} | #{line} | #{gets} | #{left} |" }
puts "#{rows.length} upload pathway fixtures passed through real fastlane 2.240.1; no sentinel reached " \
  'distribution.json, the public line or step output, the step summary or any file left after cleanup; ' \
  'no IPA, package or key survived outside the cleaned paths; no network.'
