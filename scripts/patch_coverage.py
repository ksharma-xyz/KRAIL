#!/usr/bin/env python3
"""Patch coverage of a change, measured locally from Kover, before the PR is opened.

Codecov reports this on the PR but does not block it, so the number only matters if someone looks
before opening the PR. Every PR needs at least 80 percent of its new and changed lines covered, and
90 percent is the aim. This prints the number and the uncovered lines, and exits 1 below the floor.

  ./gradlew :feature:trip-planner:ui:koverXmlReportAndroid     # one task per touched module
  ./scripts/patch_coverage.py                                   # this branch against origin/main
  ./scripts/patch_coverage.py --base <ref> --head <ref>         # any two commits
  ./scripts/patch_coverage.py --min 90                          # a stricter floor

It reads every `build/reports/kover/reportAndroid.xml` it finds, so run the Kover task for each
module the change touches first. Only production Kotlin counts; test files are not instrumented.
A line is uncovered when it ran zero times, and partial when a branch on it never ran, which is how
Codecov counts too. Put the result in the PR's Testing section.
"""
import argparse
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def sh(*args: str) -> str:
    return subprocess.run(args, capture_output=True, text=True, check=True).stdout


def changed_lines(base: str, head: str) -> dict[str, set[int]]:
    """Added or changed line numbers per production Kotlin file, from the diff."""
    diff = sh("git", "diff", "-U0", "--diff-filter=AM", f"{base}...{head}", "--", "*.kt")
    out: dict[str, set[int]] = {}
    current = None
    for line in diff.splitlines():
        if line.startswith("+++ b/"):
            current = line[6:]
            if "/src/commonTest/" in current or "/src/androidHostTest/" in current or "/src/test/" in current:
                current = None
            else:
                out.setdefault(current, set())
        m = re.match(r"@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@", line)
        if m and current:
            start, count = int(m.group(1)), int(m.group(2) or 1)
            out[current].update(range(start, start + count))
    return {k: v for k, v in out.items() if v}


def kover_lines(root: Path) -> dict[str, dict[int, tuple[int, int]]]:
    """Per source file (package path plus name): line number to (missed instructions, missed branches)."""
    found: dict[str, dict[int, tuple[int, int]]] = {}
    for report in root.glob("**/build/reports/kover/reportAndroid.xml"):
        for package in ET.parse(report).getroot().iter("package"):
            for source in package.findall("sourcefile"):
                key = f"{package.get('name')}/{source.get('name')}"
                found[key] = {
                    int(ln.get("nr")): (int(ln.get("mi")), int(ln.get("mb"))) for ln in source.findall("line")
                }
    return found


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--base", default="origin/main")
    parser.add_argument("--head", default="HEAD")
    parser.add_argument("--min", type=float, default=80.0, help="floor in percent, default 80")
    args = parser.parse_args()

    root = Path(sh("git", "rev-parse", "--show-toplevel").strip())
    changed = changed_lines(args.base, args.head)
    coverage = kover_lines(root)
    if not coverage:
        print("patch_coverage: no Kover report found. Run :<module>:koverXmlReportAndroid first.", file=sys.stderr)
        return 2

    total = covered = 0
    for path, lines in sorted(changed.items()):
        match = next((k for k in coverage if path.endswith(k)), None)
        if match is None:
            continue  # not instrumented: a resource, an expect declaration, or a module with no report
        measured = [n for n in sorted(lines) if n in coverage[match]]
        if not measured:
            continue
        uncovered = [n for n in measured if coverage[match][n][0] > 0 or coverage[match][n][1] > 0]
        total += len(measured)
        covered += len(measured) - len(uncovered)
        status = "ok " if not uncovered else "GAP"
        print(f"{status} {path.split('/')[-1]}: {len(measured) - len(uncovered)}/{len(measured)}"
              + (f", uncovered or partial lines {uncovered}" if uncovered else ""))

    if total == 0:
        print("patch_coverage: the change has no instrumented production lines.")
        return 0
    percent = 100 * covered / total
    verdict = "pass" if percent >= args.min else "FAIL"
    print(f"patch coverage {percent:.1f}% ({covered}/{total} lines), floor {args.min:.0f}%, aim 90%: {verdict}")
    return 0 if percent >= args.min else 1


if __name__ == "__main__":
    sys.exit(main())
