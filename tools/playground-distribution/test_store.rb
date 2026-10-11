# Dependency-free tests: load helpers but never execute a lane or call a provider.
module UI
  def self.user_error!(message)
    raise message
  end
end
def skip_docs; end
def lane(_name); end
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
def page(access, next_url = nil)
  {'data' => [{'attributes' => {'hasAccessToAllBuilds' => access}}], 'links' => {'next' => next_url}}
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
  [[page(false, 'https://evil.example/v1/apps/6820738808/betaGroups?cursor=next')], {}] => 'group_pagination_host'
}.each do |(pages, target), code|
  got = guard_after(pages, target)
  raise "Expected guard #{code}, got #{got.inspect}" unless got == code
end
responses([page(false)])
$release_guard = nil
raise 'A passing check must not set a guard' unless apple_group == GROUP && $release_guard.nil?
puts '6 TestFlight privacy/scope guard scenarios and 8 guard-code cases passed; no store requests made.'
