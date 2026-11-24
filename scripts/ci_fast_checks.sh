#!/usr/bin/env bash
#
# The fast steps of .github/workflows/code-quality.yml, in one list.
#
# Two callers run this: scripts/pre_push_structural_checks.sh (on every push) and
# scripts/fullQualityChecks.sh (the pre-PR gate). Each used to carry its own copy of these steps, and
# the copies drifted: CI ran `python3 scripts/check_layout_invariants.py` first and the pre-push hook
# did not, so a branch that was green locally failed `code-quality / detekt` on its first step.
# Keeping the list here means the two callers cannot disagree with each other, and
# scripts/check_ci_parity.py fails when this list and the workflow disagree.
#
# Usage:
#   scripts/ci_fast_checks.sh                the script steps, then the Gradle verify tasks
#   scripts/ci_fast_checks.sh scripts        only the script steps (milliseconds, no Gradle)
#   scripts/ci_fast_checks.sh gradle [ARGS]  only the Gradle verify tasks; ARGS go to ./gradlew
#   scripts/ci_fast_checks.sh --list         print every step, one per line, for check_ci_parity.py
#
# Exit codes are CI's. The workflow runs its script steps under `bash -e`, so a script that exits
# non-zero fails the step and one that exits 0 passes it. check_stale_assumptions.py is "warning
# only" for that reason and no other: it prints the overdue rows and exits 0 unless it is given
# --strict, and it exits 1 when the ledger is missing or its table no longer parses. Here every
# non-zero exit fails, the same way, and every step still runs so one run reports all of them.

set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# One command per entry, run from the repo root. Keep these as plain words with no quoting: each
# entry is split on spaces and run directly, and check_ci_parity.py compares it with the workflow
# as text. When code-quality.yml gains a `python3 scripts/...` step, add it here in the same change.
SCRIPT_STEPS=(
  "python3 scripts/check_layout_invariants.py"
  "python3 scripts/check_stale_assumptions.py"
)

# The "Verify test wiring" step of code-quality.yml. These answer "is the repo still coherent?",
# which a targeted `./gradlew :some:module:testAndroidHostTest` can never find. See
# docs/learning/2026-09-22-the-gate-i-did-not-open.md.
VERIFY_TASKS=(
  verifyTestWiring
  verifyTestingModuleUsage
  verifyNoAdHocBoundaryFakes
  verifyIosTestClassification
)

list_steps() {
  local step task
  for step in "${SCRIPT_STEPS[@]}"; do
    echo "script $step"
  done
  for task in "${VERIFY_TASKS[@]}"; do
    echo "gradle $task"
  done
}

run_script_steps() {
  cd "$ROOT" || return 1

  if ! command -v python3 >/dev/null 2>&1; then
    echo "ci-fast-checks: python3 was not found. code-quality.yml runs these scripts with it, so they" >&2
    echo "ci-fast-checks: cannot be skipped here. Install python3, or push with SKIP_STRUCTURAL_CHECKS=1." >&2
    return 1
  fi

  local failed=0 step
  for step in "${SCRIPT_STEPS[@]}"; do
    echo "ci-fast-checks: $step"
    # Unquoted on purpose: the entry is a command line made of plain words.
    if ! $step; then
      echo "ci-fast-checks: FAILED: $step" >&2
      failed=1
    fi
  done
  return "$failed"
}

run_gradle_tasks() {
  cd "$ROOT" || return 1
  # --continue so all four report at once rather than one per run, as CI does.
  #
  # -PciQuality is deliberately NOT passed: that flag exists so CI can substitute placeholder API
  # keys, and a local run has the real ones in local.properties.
  ./gradlew "${VERIFY_TASKS[@]}" --continue "$@"
}

case "${1:-all}" in
  --list)
    list_steps
    ;;
  scripts)
    run_script_steps
    ;;
  gradle)
    shift
    run_gradle_tasks "$@"
    ;;
  all)
    run_script_steps && run_gradle_tasks
    ;;
  *)
    echo "usage: scripts/ci_fast_checks.sh [all | scripts | gradle [GRADLE_ARGS] | --list]" >&2
    exit 2
    ;;
esac
