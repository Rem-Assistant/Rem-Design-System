# Loads the actual locked gems and checks called interfaces; never contacts a store.
require 'fastlane'
require 'fastlane/actions/upload_to_testflight'
require 'fastlane/actions/upload_to_play_store'
require 'fastlane/actions/app_store_connect_api_key'
require 'google/apis/androidpublisher_v3'
require 'googleauth'

raise 'Unexpected Fastlane version' unless Fastlane::VERSION == '2.240.1'
contracts = {
  Fastlane::Actions::UploadToTestflightAction => %i[api_key apple_id app_identifier ipa skip_submission
    skip_waiting_for_build_processing wait_processing_timeout_duration distribute_external
    notify_external_testers expire_previous_builds app_version build_number],
  Fastlane::Actions::UploadToPlayStoreAction => %i[package_name json_key_data aab track release_status
    version_name skip_upload_apk skip_upload_metadata skip_upload_changelogs skip_upload_images
    skip_upload_screenshots rescue_changes_not_sent_for_review],
  Fastlane::Actions::AppStoreConnectApiKeyAction => %i[key_id issuer_id key_content is_key_content_base64 duration]
}
contracts.each do |action, required|
  missing = required - action.available_options.map(&:key)
  raise "Unsupported action options: #{missing}" unless missing.empty?
end
publisher = Google::Apis::AndroidpublisherV3::AndroidPublisherService.new
%i[insert_edit list_edit_tracks list_edit_bundles delete_edit].each do |method|
  raise "Missing publisher API #{method}" unless publisher.respond_to?(method)
end
raise 'Missing credential parser' unless Google::Auth::ServiceAccountCredentials.respond_to?(:make_creds)
puts 'Locked Fastlane actions and Google provider interfaces loaded; no store calls made.'
