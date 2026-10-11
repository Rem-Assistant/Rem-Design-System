# Dependency-free tests: load the RemClaw lane helpers but never execute a lane or call Apple.
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
load File.join(__dir__, 'fastlane/Fastfile')
raise 'Unexpected lanes' unless @lanes == %i[reserve_build_number upload]

def apple_request(method, path, payload = nil)
  raise 'Unexpected mutation' unless method == :get && payload.nil?
  raise 'Unexpected extra provider call' if @responses.empty?
  @paths << path
  @responses.shift
end
def respond(*pages)
  @responses = pages
  @paths = []
end
def rejects(message)
  begin
    yield
  rescue RuntimeError => e
    raise "Wrong rejection: #{e.message}" unless e.message.include?(message)
    return
  end
  raise 'Expected rejection did not occur'
end
def app(id, bundle) = { 'id' => id, 'attributes' => { 'bundleId' => bundle } }
def builds(versions, next_url = nil) = { 'data' => versions.map { |v| { 'attributes' => { 'version' => v } } }, 'links' => { 'next' => next_url } }

# App lookup is by exact bundle identifier, never a guessed numeric ID.
respond({ 'data' => [app('123', 'com.remapp.rem')] })
raise 'Wrong app' unless remclaw_app_id == '123' && @paths == ['/v1/apps?filter%5BbundleId%5D=com.remapp.rem&limit=200']
respond({ 'data' => [app('123', 'com.remapp.rem.widget')] })
rejects('Exactly one') { remclaw_app_id }
respond({ 'data' => [app('1', 'com.remapp.rem'), app('2', 'com.remapp.rem')] })
rejects('Exactly one') { remclaw_app_id }
respond({ 'data' => [] })
rejects('Exactly one') { remclaw_app_id }

# Every page of builds is read; next build number is above all of them.
respond(builds(%w[3 41], 'https://api.appstoreconnect.apple.com/v1/builds?cursor=2'), builds(%w[40]))
numbers = existing_build_numbers('123')
raise 'Wrong numbers' unless numbers.sort == [3, 40, 41] && next_build_number(numbers) == '42'
raise 'Wrong first build' unless next_build_number([]) == '1'
respond(builds(%w[1], 'https://evil.example/v1/builds?cursor=2'))
rejects('pagination host') { existing_build_numbers('123') }
respond(builds(%w[1.0.3]))
rejects('non-integer') { existing_build_numbers('123') }
rejects('exhausted') { next_build_number([999_999_999]) }
puts '10 RemClaw App Store Connect lookup/numbering scenarios passed; no store requests made.'
