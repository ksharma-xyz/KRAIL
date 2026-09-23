#!/usr/bin/env bash
# Every place this repo pins the iOS toolchain, in one report, plus a warning when the local
# Xcode and CI disagree on the major version.
#
# Run it first whenever a new Xcode lands, locally or on the runners.
# See docs/ci_cd/XCODE_AND_IOS_TOOLCHAIN.md for what to do with the answer.
set -uo pipefail
cd "$(dirname "$0")/.."

toml=gradle/libs.versions.toml
version_of() { grep -E "^$1 *= *\"" "$toml" | head -1 | sed -E 's/.*"(.*)".*/\1/'; }

echo "== Local"
if command -v xcodebuild >/dev/null 2>&1; then
    local_xcode=$(xcodebuild -version 2>/dev/null | awk '/^Xcode/ {print $2; exit}')
    echo "Xcode            ${local_xcode:-unknown} ($(xcode-select -p 2>/dev/null))"
else
    local_xcode=""
    echo "Xcode            not installed"
fi
echo "macOS            $(sw_vers -productVersion 2>/dev/null || echo n/a)"

echo
echo "== Gradle ($toml)"
echo "kotlin           $(version_of kotlin)"
echo "spmForKmp        $(version_of spmForKmp)   (builds the Swift bridges; check its releases for 'xcode N support')"
echo "compose-mp       $(version_of compose-multiplatform)"

echo
echo "== App target"
grep -h "IPHONEOS_DEPLOYMENT_TARGET" iosApp/iosApp.xcodeproj/project.pbxproj | sort -u | sed 's/^[[:space:]]*/deployment       /'

echo
echo "== CI (.github/workflows)"
ci_majors=""
for wf in .github/workflows/*.yml; do
    pins=$(grep -E "xcode-version:" "$wf" | sed -E "s/.*xcode-version: *'?([^' #]*)'?.*/\1/" | tr '\n' ' ')
    [ -z "$pins" ] && continue
    runner=$(grep -E "runs-on: *(macos|xcode)" "$wf" | head -1 | sed -E 's/.*runs-on: *//')
    printf "%-32s runs-on %-14s xcode %s\n" "$(basename "$wf")" "$runner" "$pins"
    ci_majors="$ci_majors $(echo "$pins" | grep -oE '[0-9]+' | head -1)"
done

local_major=${local_xcode%%.*}
mismatch=0
for major in $ci_majors; do
    if [ -n "$local_major" ] && [ "$major" != "$local_major" ]; then mismatch=1; fi
done
echo
if [ "$mismatch" = 1 ]; then
    echo "WARNING: local Xcode $local_xcode but CI pins major(s):$ci_majors."
    echo "A build green on one can fail on the other. See docs/ci_cd/XCODE_AND_IOS_TOOLCHAIN.md."
else
    echo "Local Xcode and CI pins agree on the major version."
fi
