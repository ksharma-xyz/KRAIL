#!/usr/bin/env bash
#
# Runs the weekly flows on one iOS simulator that is already booted, with the app already installed, and keeps what
# Maestro wrote in a directory of its own.
#
# Usage: run-weekly-ios.sh <simulator-udid> <app-id> <flows-path> <output-dir>
#
# It never creates, boots or installs anything. The weekly workflow does that for the device type it is testing, and
# scripts/run_device_matrix_locally.sh checks that the simulator the operator named is booted and has the app. Keeping
# this one script for both means the flows run the same way in CI and on a desk.
#
# MAESTRO_RETRIES (default 0) retries the whole suite that many extra times, the same rule as the nightly iOS leg: this run
# reports and does not gate, so one retry buys signal without hiding a real break, because a genuine regression fails
# twice. The CI workflow sets it to 1 and a local run leaves it at 0.
#
# RESTART_SIMULATOR_ON_RETRY=1 shuts the simulator down and boots it again between attempts, which is what the nightly does
# and why: a run on 2026-08-24 hit "iOS driver not ready in time" on the first attempt, and terminating only the app left
# the retry's XCTest driver session as wedged as the first. Only CI sets it. A local run does not boot simulators, so it
# never sets it, and a retry there only stops the app.
#
# The output goes to <output-dir> by `--test-output-dir`, one timestamped folder for each attempt, so nothing has to be
# copied out of `~/.maestro/tests` afterwards and a retry does not overwrite the attempt before it.
#
# Exit status is that of the last attempt.

set -u

UDID="${1:?usage: run-weekly-ios.sh <simulator-udid> <app-id> <flows-path> <output-dir>}"
APP_ID="${2:?missing app id}"
FLOWS="${3:?missing flows path}"
OUT="${4:?missing output dir}"
RETRIES="${MAESTRO_RETRIES:-0}"
RESTART="${RESTART_SIMULATOR_ON_RETRY:-0}"

mkdir -p "$OUT"

attempt=0
while true; do
  maestro --device "$UDID" test --test-output-dir "$OUT" -e APP_ID="$APP_ID" "$FLOWS"
  code=$?

  if [ "$code" -eq 0 ] || [ "$attempt" -ge "$RETRIES" ]; then
    break
  fi
  attempt=$((attempt + 1))
  echo "::warning::Maestro suite failed on $UDID; retry $attempt of $RETRIES."
  # So the retry starts from a launch and not from wherever the failed flow left the app.
  xcrun simctl terminate "$UDID" "$APP_ID" >/dev/null 2>&1 || true
  if [ "$RESTART" = "1" ]; then
    xcrun simctl shutdown "$UDID" >/dev/null 2>&1 || true
    xcrun simctl boot "$UDID"
    xcrun simctl bootstatus "$UDID" -b
  fi
done

exit "$code"
