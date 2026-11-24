#!/usr/bin/env bash
#
# Runs the weekly flows on the emulator the weekly workflow started, in every posture the device has, and keeps what each
# run wrote in a folder of its own, while the emulator is still alive.
#
# Usage: run-weekly-android.sh <app-id> <flows-path> <leg>
#
# <leg> is `tablet` or `fold`. A tablet has one posture. A foldable has two displays, and the point of the fold leg is that
# the same flows run on the cover display, a phone sized one, and on the inner display, an almost square one, because a
# layout can be right on one and cut off on the other. Postures are switched with `adb shell cmd device_state state <id>`,
# whose ids this script reads from the device with `cmd device_state print-states` and does not assume, matching them by name
# (CLOSED for the cover, OPENED for the inner display).
#
# When the device does not report both, which is the case for a system image that has no foldable state configuration, the
# flows run once in whatever posture the emulator booted in, and the run says so in `postures.txt` and on the log, so a fold
# leg that tested one display is never mistaken for one that tested two. The decision is the device's, not this script's.
#
# Each posture runs through `run-flows.sh`, unchanged, so the settle pre-flight, the retry (MAESTRO_RETRIES, set by the
# workflow as a same-line prefix on this script's own invocation, which the child inherits) and the FATAL EXCEPTION scan
# are exactly the nightly's. It is one line in the workflow and one shell in here for the reason `run-flows.sh` explains:
# the emulator action runs its `script:` a line at a time.
#
# Output, in the workspace, for the artifact upload and for weekly-summary.sh:
#
#   maestro-output/<leg>-<posture>/tests/<timestamp>/<flow>/...   what Maestro wrote for that posture only
#   maestro-output/postures.txt                                    one line per posture: name, display id, exit status
#   logcat-android-<leg>-<posture>.txt                             that posture's logcat
#
# Exit status is non-zero if any posture failed. A failed posture does not stop the next one.

set -u

APP_ID="${1:?usage: run-weekly-android.sh <app-id> <flows-path> <leg>}"
FLOWS="${2:?missing flows path}"
LEG="${3:?missing leg}"
HERE="$(cd "$(dirname "$0")" && pwd)"
TESTS="$HOME/.maestro/tests"

mkdir -p maestro-output
: > maestro-output/postures.txt

# `cmd device_state print-states` prints one DeviceState{identifier=N, name='NAME', ...} for each state the device supports.
state_id() {
  adb shell cmd device_state print-states 2>/dev/null \
    | tr -d '\r' \
    | sed -n "s/.*identifier=\([0-9][0-9]*\), name='$1'.*/\1/p" \
    | head -1
}

# A fold raises the keyguard on whichever display is now active, even with the lock screen turned off, so each posture change
# is followed by waking the device and swiping it away. The swipe is sized from the display that is current, because the
# cover and the inner display are different sizes.
clear_keyguard() {
  adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
  adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
  local size w h
  size="$(adb shell wm size 2>/dev/null | tr -d '\r' | sed -n 's/.*: \([0-9]*x[0-9]*\).*/\1/p' | tail -1)"
  w="${size%x*}"
  h="${size#*x}"
  if [ -n "$w" ] && [ -n "$h" ] && [ "$w" -gt 0 ] 2>/dev/null; then
    adb shell input swipe $((w / 2)) $((h * 8 / 10)) $((w / 2)) $((h * 2 / 10)) 300 >/dev/null 2>&1 || true
  fi
}

# The run folders Maestro has written so far, one name per line, oldest first.
list_runs() {
  [ -d "$TESTS" ] || return 0
  find "$TESTS" -mindepth 1 -maxdepth 1 -type d -exec basename {} \; | sort
}

# Runs the flows once and files the output under <leg>-<posture>. $1 is the posture name, $2 a note for postures.txt.
run_posture() {
  local posture="$1" note="$2" before after dir logcat code
  logcat="logcat-android-$LEG-$posture.txt"
  dir="maestro-output/$LEG-$posture"
  before="$(list_runs)"

  echo "== $LEG, posture $posture =="
  bash "$HERE/run-flows.sh" "$APP_ID" "$FLOWS" "$logcat"
  code=$?

  # run-flows.sh copies all of ~/.maestro/tests into maestro-output/tests on every call. Keep only the folders this posture
  # made, and drop the combined copy, so the artifact is one set of files for each posture and not the sum so far.
  after="$(list_runs)"
  mkdir -p "$dir/tests"
  comm -13 <(printf '%s\n' "$before") <(printf '%s\n' "$after") | while IFS= read -r run; do
    [ -n "$run" ] && cp -R "$TESTS/$run" "$dir/tests/$run"
  done
  rm -rf maestro-output/tests

  echo "$posture exit=$code $note" >> maestro-output/postures.txt
  return "$code"
}

overall=0

if [ "$LEG" = "fold" ]; then
  closed="$(state_id CLOSED)"
  opened="$(state_id OPENED)"
  echo "device_state ids: CLOSED=${closed:-none} OPENED=${opened:-none}"
  adb shell cmd device_state print-states 2>&1 | tr -d '\r' || true

  if [ -n "$closed" ] && [ -n "$opened" ]; then
    # The inner display first: it is the posture a foldable emulator boots in, so the first run needs no switch.
    adb shell cmd device_state state "$opened" >/dev/null 2>&1 || true
    sleep 5
    clear_keyguard
    run_posture inner "state=$opened" || overall=1

    adb shell cmd device_state state "$closed" >/dev/null 2>&1 || true
    sleep 5
    clear_keyguard
    run_posture cover "state=$closed" || overall=1

    adb shell cmd device_state state reset >/dev/null 2>&1 || true
  else
    echo "::warning::This device does not report both a CLOSED and an OPENED state, so only one display is tested."
    run_posture default "single posture, device_state did not report CLOSED and OPENED" || overall=1
  fi
else
  run_posture default "single posture" || overall=1
fi

echo "== postures =="
cat maestro-output/postures.txt
exit "$overall"
