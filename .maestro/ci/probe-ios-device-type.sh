#!/usr/bin/env bash
#
# Finds out whether this macOS runner can create one iOS simulator device type, and says so plainly when it cannot.
#
# Usage: probe-ios-device-type.sh <device-type-identifier> <leg-name>
#
# A runner image is whatever GitHub last published, and the simulator device types on it are whatever that image's Xcode ships
# with a runtime that supports them. The iPhone Duo is the case that matters: it is a recent device type with a runtime of its
# own (`iOS-27-2-iPhone19-4`, which supports that one type and no other), and an image that has an older Xcode or no such runtime
# cannot make it. A weekly job that failed on that every week would be ignored, and one that quietly dropped the device would
# hide that the device was never tested. So this prints what the runner has, and when the device cannot be made it records
# a "skipped" line in the job summary and a warning annotation and exits 0, and the job's remaining steps are skipped by the
# workflow on `available=false`. The skip is then visible on the run page and costs no more than the probe.
#
# What it looks for is two separate things, and says which is missing:
#   1. the device type, in `xcrun simctl list devicetypes`
#   2. an available iOS runtime whose `supportedDeviceTypes` includes it, in `xcrun simctl list runtimes`
# When both exist it picks the newest such runtime, so the simulator is created on exactly that one and never on a runtime
# `simctl create` happened to choose.
#
# Output, as GITHUB_OUTPUT lines (stdout when that is not set): available=true|false, and runtime=<identifier> when true.
# It writes to GITHUB_STEP_SUMMARY only when that is set. It lists, creates and boots nothing.

set -u

TYPE_ID="${1:?usage: probe-ios-device-type.sh <device-type-identifier> <leg-name>}"
NAME="${2:?missing leg name}"
OUT="${GITHUB_OUTPUT:-/dev/stdout}"

echo "== Xcode =="
xcodebuild -version || true
echo
echo "== Simulator device types for the Duo, iPad and SE families =="
xcrun simctl list devicetypes | grep -E 'Duo|iPad|SE' || true
echo
echo "== Simulator runtimes =="
xcrun simctl list runtimes || true
echo

have_type="$(xcrun simctl list devicetypes -j | jq -r --arg id "$TYPE_ID" '[.devicetypes[] | select(.identifier == $id)] | length')"
runtime="$(xcrun simctl list runtimes -j | jq -r --arg id "$TYPE_ID" '
  [ .runtimes[]
    | select(.isAvailable == true and .platform == "iOS"
             and ((.supportedDeviceTypes // []) | map(.identifier) | index($id))) ]
  | sort_by(.version | split(".") | map(tonumber))
  | last
  | .identifier // empty')"

skip() {
  echo "::warning::$NAME skipped: $1"
  if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
    {
      echo "### $NAME"
      echo
      echo "**skipped: $1**"
      echo
      echo "Device type \`$TYPE_ID\`. Nothing was run for it. Run it on a Mac that has the type with" \
           "\`scripts/run_device_matrix_locally.sh\`, see \`.maestro/README.md\`."
    } >> "$GITHUB_STEP_SUMMARY"
  fi
  echo "available=false" >> "$OUT"
  exit 0
}

if [ "${have_type:-0}" -eq 0 ]; then
  skip "device type not on this runner image"
fi
if [ -z "$runtime" ]; then
  skip "device type is on this runner image but no available iOS runtime supports it"
fi

echo "$NAME: device type $TYPE_ID, runtime $runtime"
{
  echo "available=true"
  echo "runtime=$runtime"
} >> "$OUT"
