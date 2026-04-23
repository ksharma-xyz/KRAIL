#!/usr/bin/env bash
# Checks out each PR branch and verifies: detekt, Android compile, iOS compile, Android tests.
# Run from the repo root: bash check_branches.sh

set -euo pipefail

BRANCHES=(pr/track-foundation pr/track-ui pr/track-quality)
PASS=()
FAIL=()

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

run_step() {
    local label="$1"
    shift
    echo -e "  ${YELLOW}▶ $label${NC}"
    if "$@" > /tmp/krail_check_out.txt 2>&1; then
        echo -e "  ${GREEN}✓ $label${NC}"
        return 0
    else
        echo -e "  ${RED}✗ $label${NC}"
        tail -20 /tmp/krail_check_out.txt
        return 1
    fi
}

for branch in "${BRANCHES[@]}"; do
    echo ""
    echo -e "${YELLOW}══════════════════════════════════════${NC}"
    echo -e "${YELLOW} Branch: $branch${NC}"
    echo -e "${YELLOW}══════════════════════════════════════${NC}"

    git checkout "$branch"

    branch_ok=true

    run_step "detekt" ./gradlew detekt --continue || branch_ok=false
    run_step "Android compile (assembleDebug)" ./gradlew assembleDebug || branch_ok=false
    run_step "iOS compile (linkDebugFrameworkIosSimulatorArm64)" \
        ./gradlew linkDebugFrameworkIosSimulatorArm64 || branch_ok=false
    run_step "Android tests (testAndroidHostTest)" \
        ./gradlew testAndroidHostTest --continue || branch_ok=false

    if $branch_ok; then
        PASS+=("$branch")
        echo -e "${GREEN}✓ $branch — all checks passed${NC}"
    else
        FAIL+=("$branch")
        echo -e "${RED}✗ $branch — one or more checks failed${NC}"
    fi
done

echo ""
echo -e "${YELLOW}══════════════════════════════════════${NC}"
echo -e "${YELLOW} Summary${NC}"
echo -e "${YELLOW}══════════════════════════════════════${NC}"
for b in "${PASS[@]+"${PASS[@]}"}"; do echo -e "${GREEN}  ✓ $b${NC}"; done
for b in "${FAIL[@]+"${FAIL[@]}"}"; do echo -e "${RED}  ✗ $b${NC}"; done

if [ ${#FAIL[@]} -gt 0 ]; then exit 1; fi
