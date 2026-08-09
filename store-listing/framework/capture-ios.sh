#!/bin/zsh
set -euo pipefail

if (( $# != 5 )); then
  print -u2 "usage: $0 <udid> <flow.yaml> <output.png> <width> <height>"
  exit 2
fi

udid="$1"
flow="$2"
output="$3"
expected_width="$4"
expected_height="$5"

for tool in xcrun maestro sips; do
  command -v "$tool" >/dev/null || {
    print -u2 "missing required command: $tool"
    exit 1
  }
done

[[ -f "$flow" ]] || { print -u2 "flow not found: $flow"; exit 1; }

# Maestro can report one UDID while reading or driving another booted simulator.
# Make the target exclusive before any interaction.
booted_ids=(${(f)"$(xcrun simctl list devices | sed -n 's/.*(\([0-9A-F-]\{36\}\)) (Booted).*/\1/p')"})
for booted in "${booted_ids[@]}"; do
  [[ "$booted" == "$udid" ]] || xcrun simctl shutdown "$booted"
done

xcrun simctl boot "$udid" 2>/dev/null || true
xcrun simctl bootstatus "$udid" -b
maestro --device "$udid" test "$flow"

mkdir -p "${output:h}"
xcrun simctl io "$udid" screenshot "$output"

actual_width="$(sips -g pixelWidth "$output" | awk '/pixelWidth/ {print $2}')"
actual_height="$(sips -g pixelHeight "$output" | awk '/pixelHeight/ {print $2}')"
if [[ "$actual_width" != "$expected_width" || "$actual_height" != "$expected_height" ]]; then
  print -u2 "wrong device capture: ${actual_width}x${actual_height}; expected ${expected_width}x${expected_height}"
  exit 1
fi

print "captured $output at ${actual_width}x${actual_height}"
