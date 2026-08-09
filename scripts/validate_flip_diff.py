#!/usr/bin/env python3
"""Guard run before any push/PR/auto-merge in analytics-registry-sync.yml.

flip_registry_status.py has already rewritten docs/ANALYTICS_REGISTRY_HANDOFF.md on
disk (uncommitted) by the time this runs. This script independently re-checks the
actual `git diff` rather than trusting that script's own bookkeeping, so a future bug
in either script (or a malformed dispatch payload) gets caught here instead of
reaching a PR that then auto-merges unreviewed.

Passes only if ALL of:
  - Exactly one file changed: docs/ANALYTICS_REGISTRY_HANDOFF.md
  - No lines added or removed as whole lines (line count unchanged)
  - Every changed line differs from its old version by nothing except the literal
    substring "| Pending |" -> "| Registered |"

Anything else (a deleted row, a rewritten row, an edited unrelated section, a second
file touched) fails loudly and non-zero. The caller must not proceed to git add/commit/
push on a non-zero exit.
"""

import subprocess
import sys

LEDGER_PATH = "docs/ANALYTICS_REGISTRY_HANDOFF.md"
OLD_MARKER = "| Pending |"
NEW_MARKER = "| Registered |"


def fail(message: str) -> None:
    print(f"::error::{message}")
    sys.exit(1)


def main() -> None:
    changed_files = subprocess.run(
        ["git", "diff", "--name-only"], capture_output=True, text=True, check=True
    ).stdout.split()

    if changed_files != [LEDGER_PATH]:
        fail(f"Expected only {LEDGER_PATH} changed, got: {changed_files or '(nothing)'}")

    diff = subprocess.run(
        ["git", "diff", "-U0", "--", LEDGER_PATH], capture_output=True, text=True, check=True
    ).stdout

    removed, added = [], []
    for line in diff.splitlines():
        if line.startswith("+++") or line.startswith("---"):
            continue
        if line.startswith("-"):
            removed.append(line[1:])
        elif line.startswith("+"):
            added.append(line[1:])

    if len(removed) != len(added):
        fail(f"Line count changed: {len(removed)} removed vs {len(added)} added (rows must only be edited, never added/removed)")

    if not removed:
        fail("No changed lines found, but a diff was expected")

    for old, new in zip(removed, added):
        if old.replace(OLD_MARKER, NEW_MARKER) != new:
            fail(f"Row changed by more than the status flip:\n  old: {old}\n  new: {new}")
        if OLD_MARKER not in old or NEW_MARKER not in new:
            fail(f"Row does not contain the expected Pending/Registered markers:\n  old: {old}\n  new: {new}")

    print(f"✓ Diff validated: {len(removed)} row(s) changed, each only Pending -> Registered")


if __name__ == "__main__":
    main()
