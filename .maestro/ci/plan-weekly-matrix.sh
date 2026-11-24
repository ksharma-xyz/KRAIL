#!/usr/bin/env bash
#
# Decides which devices the weekly run tests, and writes the two job matrices the workflow fans out over.
#
# Usage: DEVICES=<list> plan-weekly-matrix.sh
#
# DEVICES is the `devices` input of a manual run: a comma separated list of leg names, group names or `all`.
#
#   legs     duo, ipad, small-iphone (iOS), tablet, fold (Android)
#   groups   ios, android, all
#
# An empty value means all, which is what the scheduled run passes. An unknown word fails the run at once, before a macOS
# runner is paid for, because a typo that silently selected nothing would look like a green run.
#
# This file is the one place the devices are listed. To add a device, add a line to the list below, and the workflow, which
# only reads the matrices this prints, needs no edit. For an iOS leg `device_type` is a CoreSimulator device type identifier,
# and `xcrun simctl list devicetypes` shows the ones a Mac has. For an Android leg `profile` is an avdmanager device id, and
# `avdmanager list device -c` shows them.
#
# Output, as GITHUB_OUTPUT lines (or on stdout when that is not set, for a run from a terminal):
#
#   ios=<matrix json>   android=<matrix json>   ios_count=<n>   android_count=<n>
#
# A matrix is `{"include":[...]}`, which `fromJSON` reads. The counts let a job be skipped when its platform has no leg
# selected, because a matrix with no entries is an error in Actions, not an empty job.

set -euo pipefail

wanted="$(printf '%s' "${DEVICES:-all}" | tr '[:upper:]' '[:lower:]' | tr -d ' ')"
[ -n "$wanted" ] || wanted="all"

IOS_LEGS='[
  {"leg":"duo","name":"iPhone Duo outer screen","device_type":"com.apple.CoreSimulator.SimDeviceType.iPhone-Duo"},
  {"leg":"ipad","name":"iPad Pro 13-inch (M5)","device_type":"com.apple.CoreSimulator.SimDeviceType.iPad-Pro-13-inch-M5-12GB"},
  {"leg":"small-iphone","name":"iPhone SE (3rd generation)","device_type":"com.apple.CoreSimulator.SimDeviceType.iPhone-SE-3rd-generation"}
]'
ANDROID_LEGS='[
  {"leg":"tablet","name":"Pixel Tablet","profile":"pixel_tablet"},
  {"leg":"fold","name":"Pixel 9 Pro Fold","profile":"pixel_9_pro_fold"}
]'

known=" all ios android duo ipad small-iphone tablet fold "
tokens_json="[]"
IFS=',' read -r -a tokens <<< "$wanted"
for t in "${tokens[@]}"; do
  case "$known" in
    *" $t "*) tokens_json="$(jq -c --arg t "$t" '. + [$t]' <<< "$tokens_json")" ;;
    *)
      echo "::error::Unknown device '$t'. Use all, ios, android, duo, ipad, small-iphone, tablet, fold, separated by commas."
      exit 1
      ;;
  esac
done

# A leg is selected by its own name, by its platform, or by all.
select_legs() {
  jq -c --argjson t "$tokens_json" --arg platform "$2" \
    '[.[] | . as $d | select(($t | index("all")) or ($t | index($platform)) or ($t | index($d.leg)))]' <<< "$1"
}

ios="$(select_legs "$IOS_LEGS" ios)"
android="$(select_legs "$ANDROID_LEGS" android)"
ios_count="$(jq 'length' <<< "$ios")"
android_count="$(jq 'length' <<< "$android")"

if [ "$ios_count" -eq 0 ] && [ "$android_count" -eq 0 ]; then
  echo "::error::'$wanted' selects no device."
  exit 1
fi

out="${GITHUB_OUTPUT:-/dev/stdout}"
{
  echo "ios=$(jq -c '{include: .}' <<< "$ios")"
  echo "android=$(jq -c '{include: .}' <<< "$android")"
  echo "ios_count=$ios_count"
  echo "android_count=$android_count"
} >> "$out"
