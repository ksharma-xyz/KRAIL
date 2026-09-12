#!/usr/bin/env python3
"""Fail when an open PR still carries the unfilled pull-request template.

`gt submit` has no `--body` flag. It seeds a new PR from
`.github/PULL_REQUEST_TEMPLATE.md` and never replaces it, so a PR raised with
Graphite ships the template verbatim, comment placeholders and all, unless
somebody follows up with `gh pr edit --body-file`.

Nothing caught that. A PR with an empty description looks exactly like a PR
whose description is still loading, and by the time anyone notices, the branch
has usually been merged and the reasoning is gone.

Usage:
    python3 scripts/check_pr_descriptions.py              # your open PRs
    python3 scripts/check_pr_descriptions.py 1999         # one PR
    python3 scripts/check_pr_descriptions.py --all-authors

Exits non-zero when any checked PR is unfilled, so it can gate a submit flow.
"""

from __future__ import annotations

import json
import re
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
TEMPLATE = REPO_ROOT / ".github" / "PULL_REQUEST_TEMPLATE.md"

# Sections the template asks for. "Snapshots" is deliberately absent: the skill
# says to delete it on a non-UI change, so requiring it would push people to
# leave an empty section rather than remove it.
REQUIRED_SECTIONS = ("## What", "## Changes", "## Testing")

# A body this short cannot be saying anything, whatever it contains.
MIN_BODY_CHARS = 200


def run(args: list[str]) -> str:
    result = subprocess.run(args, capture_output=True, text=True, check=False)
    if result.returncode != 0:
        sys.exit(f"command failed: {' '.join(args)}\n{result.stderr.strip()}")
    return result.stdout


def template_placeholders() -> list[str]:
    """Every HTML comment in the template.

    Read from the file rather than hard-coded, so editing the template cannot
    silently stop this check from working.
    """
    if not TEMPLATE.is_file():
        sys.exit(f"{TEMPLATE.relative_to(REPO_ROOT)} is missing; this check reads it.")
    return [c.strip() for c in re.findall(r"<!--.*?-->", TEMPLATE.read_text(), re.S)]


def open_prs(explicit: list[str], all_authors: bool) -> list[dict]:
    if explicit:
        return [
            json.loads(run(["gh", "pr", "view", n, "--json", "number,title,body,url"]))
            for n in explicit
        ]
    args = ["gh", "pr", "list", "--state", "open", "--limit", "50",
            "--json", "number,title,body,url"]
    if not all_authors:
        args += ["--author", "@me"]
    return json.loads(run(args))


def problems_with(pr: dict, placeholders: list[str]) -> list[str]:
    body = (pr.get("body") or "").strip()
    found: list[str] = []

    if not body:
        return ["description is empty"]

    left = [p for p in placeholders if p in body]
    if left:
        found.append(
            f"{len(left)} unfilled template placeholder(s) still present, first: "
            f"{left[0][:60]}..."
        )

    missing = [s for s in REQUIRED_SECTIONS if s not in body]
    if missing:
        found.append(f"missing section(s): {', '.join(missing)}")

    if len(body) < MIN_BODY_CHARS:
        found.append(f"description is {len(body)} chars; under the {MIN_BODY_CHARS} minimum")

    return found


def main() -> int:
    argv = [a for a in sys.argv[1:] if a != "--all-authors"]
    all_authors = "--all-authors" in sys.argv[1:]

    placeholders = template_placeholders()
    prs = open_prs(argv, all_authors)

    if not prs:
        print("No open PRs to check.")
        return 0

    failures = []
    for pr in prs:
        issues = problems_with(pr, placeholders)
        mark = "FAIL" if issues else "ok  "
        print(f"{mark} #{pr['number']}  {pr['title'][:60]}")
        for issue in issues:
            print(f"       {issue}")
        if issues:
            failures.append(pr)

    if failures:
        print()
        print("Fix with:")
        for pr in failures:
            print(f"  gh pr edit {pr['number']} --body-file <path>")
        print()
        print("`gt submit` cannot set a body; it only seeds the template. Writing the")
        print("description is always a second step. See .claude/skills/pr-desc/SKILL.md.")
        return 1

    print(f"\nAll {len(prs)} open PR description(s) filled in.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
