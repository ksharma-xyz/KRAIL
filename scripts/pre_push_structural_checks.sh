#!/usr/bin/env bash
#
# The fast half of CI's `code-quality / detekt` job, run on a push. Everything here is a step that
# job runs before detekt and the tests, and each is cheap enough to run locally every time.
#
# What runs, in order, from the one list in scripts/ci_fast_checks.sh:
#
#   1. The script steps of code-quality.yml (`python3 scripts/check_layout_invariants.py`,
#      `check_stale_assumptions.py` and `check_ci_parity.py`). Milliseconds, no Gradle, no
#      local.properties needed. They run first so a violation fails before anything slow starts.
#   2. A grep for commas in backtick test names, for modules on the iOS lane (not a CI step, see below).
#   3. The four Gradle `verify*` tasks of the "Verify test wiring" step. Seconds, not milliseconds.
#
# The Gradle tasks answer "is the repo still coherent?" rather than "is my change correct?". They are
# the ones a targeted `./gradlew :some:module:testAndroidHostTest` can never find, because the defects
# they catch are in the shape of the build rather than in any one module: a module with test sources
# and no host-test task, a module with shared tests and no iOS lane classification, an ad-hoc fake at a
# boundary that has a real one.
#
# Placed on pre-push rather than pre-commit deliberately. They take seconds, not milliseconds,
# and a commit is cheap to amend while a red CI run is not.
#
# Why this exists at all: `verifyIosTestClassification` failed in CI on a change whose author
# had run a compile, detekt and the touched modules' tests, all green, and skipped the script
# that wraps these. See docs/learning/2026-09-22-the-gate-i-did-not-open.md. The script steps were
# left out of the first version of this hook, and CI's first step failed on one of them for the same
# reason: the local gates were a subset of CI's. scripts/ci_fast_checks.sh is the single list now, so
# this hook and fullQualityChecks.sh cannot disagree with each other, and scripts/check_ci_parity.py
# (one of its steps) fails when that list and code-quality.yml disagree.
#
# Bypass with `git push --no-verify`, or SKIP_STRUCTURAL_CHECKS=1 when the hook itself is in
# the way. Both are fine; the point is that skipping becomes a decision rather than an oversight.

set -euo pipefail

if [ "${SKIP_STRUCTURAL_CHECKS:-0}" = "1" ]; then
  echo "pre-push: structural checks skipped (SKIP_STRUCTURAL_CHECKS=1)"
  exit 0
fi

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

# The script steps of code-quality.yml first: milliseconds, and they need neither Gradle nor
# local.properties, so a fresh worktree gets them too. Non-zero exits fail the push, as in CI.
echo "pre-push: fast CI checks (layout invariants, analytics assumptions, CI parity)..."
if ! "$repo_root/scripts/ci_fast_checks.sh" scripts; then
  cat >&2 <<'MSG'

pre-push: fast CI checks failed. Nothing has been pushed.

These are the scripts code-quality.yml runs first, before detekt and the tests. The output above
names the file and the rule. Running them takes milliseconds and failing them in CI costs a whole
red run, which is why they sit here.

Fix it, or push with --no-verify if you know why it is failing.
MSG
  exit 1
fi

# A fresh worktree has none of the gitignored local config, and Gradle then fails with an error that
# does not say what to do. Say it here, before the slow part.
if [ ! -f "$repo_root/local.properties" ]; then
  echo "pre-push: local.properties is missing, so Gradle cannot run." >&2
  echo "pre-push: this looks like a fresh worktree. Run ./scripts/worktree_setup.sh and push again." >&2
  echo "pre-push: see docs/WORKTREES.md. A clone with no NSW key can push with SKIP_STRUCTURAL_CHECKS=1." >&2
  exit 1
fi

if [ ! -x ./gradlew ]; then
  echo "pre-push: no ./gradlew here, skipping structural checks"
  exit 0
fi

echo "pre-push: structural checks (test wiring, iOS lane, boundary fakes)..."

# Commas in backtick test names, for modules that run on the iOS lane.
#
# Kotlin/Native rejects `,` inside a backtick identifier where the JVM accepts it, so a test
# named `a thing, and another` compiles for Android and fails the iOS lane with
# "Name contains illegal characters". IosUnitTests.kt documents this and calls it cheap to fix
# per module, which it is — once you know. Finding out costs a CI round trip.
#
# Grepped rather than compiled because on a machine whose Xcode is newer than Kotlin supports,
# the iOS test compile cannot run locally at all, so this is the only local signal there is.
#
# Module list is read from IosUnitTests.kt so the two cannot drift.
ios_lane_file="gradle/build-logic/convention/src/main/kotlin/xyz/ksharma/krail/gradle/IosUnitTests.kt"
if [ -f "$ios_lane_file" ]; then
  offenders=""
  # Only the IOS_TEST_MODULES block: the exclusions do not run on Native and may keep commas.
  lane_modules=$(sed -n '/^val IOS_TEST_MODULES/,/^)/p' "$ios_lane_file" \
    | grep -oE '":[^"]+"' | tr -d '"')

  for module in $lane_modules; do
    dir=$(echo "$module" | sed 's|^:||; s|:|/|g')
    for src in "$dir/src/commonTest" "$dir/src/iosTest"; do
      [ -d "$src" ] || continue
      hits=$(grep -rn 'fun `[^`]*,[^`]*`' "$src" 2>/dev/null || true)
      [ -n "$hits" ] && offenders="$offenders$hits\n"
    done
  done

  if [ -n "$offenders" ]; then
    printf >&2 "\npre-push: comma in a backtick test name, in a module on the iOS lane.\n"
    printf >&2 "Kotlin/Native rejects these; Android does not, so local tests pass and CI fails.\n\n"
    printf >&2 "%b\n" "$offenders"
    exit 1
  fi
fi

# The "Verify test wiring" step of code-quality.yml, from the same list fullQualityChecks.sh uses
# (--continue so all four report at once, no -PciQuality, both explained in ci_fast_checks.sh).
if "$repo_root/scripts/ci_fast_checks.sh" gradle --quiet; then
  echo "pre-push: structural checks passed"
else
  cat >&2 <<'MSG'

pre-push: structural checks failed. Nothing has been pushed.

These are the checks a change-shaped test run cannot find. The usual cause is a module
that grew test sources without a classification, or a new boundary fake.

Fix it, or push with --no-verify if you know why it is failing.
MSG
  exit 1
fi
