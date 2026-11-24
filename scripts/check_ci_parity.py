#!/usr/bin/env python3
"""Fail when the fast CI steps and the local copy of them disagree.

.github/workflows/code-quality.yml runs a few cheap steps before detekt and the tests: some
`python3 scripts/...` commands and the `verify*` Gradle tasks. scripts/ci_fast_checks.sh is the one
list of those steps that both the pre-push hook and scripts/fullQualityChecks.sh run. A step in the
workflow that is missing from that list is a check CI runs and nobody runs locally, which is how a
branch went red on CI's first step while every local gate was green.

This script compares the two and reports every difference, in both directions:

- the workflow runs a step the list does not run (the gap that caused the incident), and
- the list runs a step the workflow does not (a stale entry: the list stops describing CI).

It also fails when it finds no steps on either side, so a refactor of the workflow or of the list
that defeats the matching cannot make it pass by comparing two empty sets.

What it reads: non-comment lines of the workflow, for `python3 scripts/<name>.py [args]` and for
`verifyXxx` task names. What it cannot see: a step in a different workflow file, a shell script
that CI calls, or a Gradle task other than `verify*`. Add those here when one of them becomes a
fast CI step.

Run standalone or via scripts/ci_fast_checks.sh. Exit code 1 on any difference.

    python3 scripts/check_ci_parity.py
    python3 scripts/check_ci_parity.py --workflow path/to/copy.yml   # for testing the check itself
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WORKFLOW = ROOT / ".github/workflows/code-quality.yml"
FAST_CHECKS = ROOT / "scripts/ci_fast_checks.sh"

# `python3 scripts/x.py` or `python scripts/x.py`, then whatever follows on the line up to a shell
# operator. Arguments matter: `--strict` turns a warning into a failure, so a step that gains one
# is a different step.
SCRIPT_STEP = re.compile(r"(?<![\w/.-])python3?\s+(?:\./)?(scripts/[\w./-]+\.py)([^|&;<>]*)")

# A Gradle verification task name: `verifyTestWiring`. The workflow's step id `verify-test-wiring`
# and its name `Verify test wiring` do not match, because the next character is not upper case.
VERIFY_TASK = re.compile(r"\b(verify[A-Z][A-Za-z]*)\b")


def normalise_script(path: str, args: str) -> str:
    args = args.strip().rstrip("\\").strip()
    return f"python3 {path}" + (f" {args}" if args else "")


def code_lines(text: str) -> list[str]:
    """Workflow lines with comments removed, so prose that names a script is not a step."""
    lines = []
    for line in text.splitlines():
        if line.lstrip().startswith("#"):
            continue
        lines.append(re.sub(r"\s#.*$", "", line))
    return lines


def workflow_steps(workflow: Path) -> tuple[set[str], set[str]]:
    scripts: set[str] = set()
    tasks: set[str] = set()
    for line in code_lines(workflow.read_text()):
        for match in SCRIPT_STEP.finditer(line):
            scripts.add(normalise_script(match.group(1), match.group(2)))
        tasks.update(VERIFY_TASK.findall(line))
    return scripts, tasks


def local_steps() -> tuple[set[str], set[str]]:
    """Asks the shell script for its list rather than parsing bash, so the two cannot disagree."""
    result = subprocess.run(
        ["bash", str(FAST_CHECKS), "--list"],
        capture_output=True,
        text=True,
        check=False,
    )
    if result.returncode != 0:
        print(f"{FAST_CHECKS.relative_to(ROOT)} --list failed:\n{result.stderr}")
        sys.exit(1)

    scripts: set[str] = set()
    tasks: set[str] = set()
    for line in result.stdout.splitlines():
        kind, _, rest = line.partition(" ")
        if kind == "script":
            match = SCRIPT_STEP.search(rest)
            if match:
                scripts.add(normalise_script(match.group(1), match.group(2)))
        elif kind == "gradle":
            tasks.add(rest.strip())
    return scripts, tasks


def script_problems(workflow_scripts: set[str], local_scripts: set[str], where: str) -> list[str]:
    problems: list[str] = []

    def path_of(step: str) -> str:
        return step.split()[1]

    for step in sorted(workflow_scripts - local_scripts):
        twin = [other for other in local_scripts if path_of(other) == path_of(step)]
        if twin:
            problems.append(
                f"{where} runs `{step}` but scripts/ci_fast_checks.sh runs `{twin[0]}`. The "
                "arguments differ, so the local step is not the CI step: `--strict` alone turns a "
                "warning into a failure. Make SCRIPT_STEPS match the workflow."
            )
        else:
            problems.append(
                f"{where} runs `{step}` but scripts/ci_fast_checks.sh does not list it. The "
                "pre-push hook and fullQualityChecks.sh run only what that script lists, so a "
                "violation this step catches passes both and fails CI on its first step. Add it "
                "to SCRIPT_STEPS in scripts/ci_fast_checks.sh."
            )
    for step in sorted(local_scripts - workflow_scripts):
        if any(path_of(other) == path_of(step) for other in workflow_scripts):
            continue  # already reported above as an argument mismatch
        problems.append(
            f"scripts/ci_fast_checks.sh lists `{step}` but {where} does not run it, so the local "
            "list no longer describes CI. Remove it from SCRIPT_STEPS, or add the step to the "
            "workflow."
        )
    return problems


def task_problems(workflow_tasks: set[str], local_tasks: set[str], where: str) -> list[str]:
    problems: list[str] = []
    for task in sorted(workflow_tasks - local_tasks):
        problems.append(
            f"{where} runs the Gradle task `{task}` but scripts/ci_fast_checks.sh does not list "
            "it, so the pre-push hook and fullQualityChecks.sh never run it. Add it to "
            "VERIFY_TASKS in scripts/ci_fast_checks.sh."
        )
    for task in sorted(local_tasks - workflow_tasks):
        problems.append(
            f"scripts/ci_fast_checks.sh lists the Gradle task `{task}` but {where} does not run "
            "it, so the local list no longer describes CI. Remove it from VERIFY_TASKS, or add "
            "it to the workflow."
        )
    return problems


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--workflow",
        type=Path,
        default=WORKFLOW,
        help="workflow file to compare against (default: code-quality.yml); for testing this check",
    )
    args = parser.parse_args()
    workflow = args.workflow.resolve()

    if not workflow.exists():
        print(f"{workflow}: workflow not found")
        return 1
    where = ".github/workflows/code-quality.yml" if workflow == WORKFLOW else str(workflow)

    workflow_scripts, workflow_tasks = workflow_steps(workflow)
    local_scripts, local_tasks = local_steps()

    # Scan-broke detectors: an empty side means the matching stopped working, not that all is well.
    problems: list[str] = []
    for label, found in (
        (f"python3 scripts/... steps in {where}", workflow_scripts),
        (f"verify* Gradle tasks in {where}", workflow_tasks),
        ("script steps from scripts/ci_fast_checks.sh --list", local_scripts),
        ("Gradle tasks from scripts/ci_fast_checks.sh --list", local_tasks),
    ):
        if not found:
            problems.append(
                f"Found no {label}. Either the file changed shape and this check needs updating "
                "(scripts/check_ci_parity.py), or it was emptied by mistake. Refusing to pass "
                "by comparing nothing."
            )

    if not problems:
        problems = script_problems(workflow_scripts, local_scripts, where) + task_problems(
            workflow_tasks, local_tasks, where
        )

    if problems:
        print("CI parity check FAILED:\n")
        for problem in problems:
            print(f"  - {problem}\n")
        print(
            "The fast steps of code-quality.yml and scripts/ci_fast_checks.sh must name the same "
            "checks. See docs/testing/GUARDS.md, section scripts/check_ci_parity.py."
        )
        return 1

    print(
        f"CI parity OK. {len(workflow_scripts)} script step(s) and {len(workflow_tasks)} verify "
        "task(s) match scripts/ci_fast_checks.sh."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
