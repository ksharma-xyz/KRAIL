#!/usr/bin/env python3
"""Report analytics assumptions that are due for a re-check.

Reads the table in docs/ANALYTICS_ASSUMPTIONS.md and compares each row's "Review by" date
against today. See that file for why the ledger exists and what a row means.

Warns by default and exits 0, because a date passing is not a reason to block somebody's
unrelated build. Pass --strict to exit 1 instead, which is what a scheduled job should use to
raise an issue.

Usage:
    python3 scripts/check_stale_assumptions.py
    python3 scripts/check_stale_assumptions.py --strict
    python3 scripts/check_stale_assumptions.py --today 2027-01-01   # for testing
"""

from __future__ import annotations

import argparse
import re
import sys
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LEDGER = ROOT / "docs/ANALYTICS_ASSUMPTIONS.md"

# Rows look like: | `id` | assumption | relies | how | falsify | 2026-08-16 | 2026-11-16 |
ROW = re.compile(
    r"^\|\s*`(?P<id>[^`]+)`\s*\|.*\|\s*(?P<checked>\d{4}-\d{2}-\d{2}|—|-)\s*\|"
    r"\s*(?P<review>\d{4}-\d{2}-\d{2})\s*\|\s*$"
)


def parse_rows(text: str) -> list[dict[str, str]]:
    rows = []
    for line in text.splitlines():
        match = ROW.match(line)
        if match:
            rows.append(match.groupdict())
    return rows


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--strict", action="store_true", help="exit 1 when anything is due")
    parser.add_argument("--today", help="override today's date (YYYY-MM-DD), for testing")
    args = parser.parse_args()

    if not LEDGER.exists():
        print(f"{LEDGER} not found.")
        return 1

    today = date.fromisoformat(args.today) if args.today else date.today()
    rows = parse_rows(LEDGER.read_text())

    if not rows:
        print(f"{LEDGER}: no assumption rows parsed. Has the table shape changed?")
        return 1

    due = [row for row in rows if date.fromisoformat(row["review"]) <= today]
    never = [row for row in due if row["checked"] in {"—", "-"}]

    if not due:
        nearest = min(date.fromisoformat(row["review"]) for row in rows)
        print(f"Analytics assumptions OK. {len(rows)} tracked, next due {nearest}.")
        return 0

    print(f"{len(due)} analytics assumption(s) due for a re-check as of {today}:\n")
    for row in due:
        checked = "never checked" if row in never else f"last checked {row['checked']}"
        print(f"  - {row['id']}: due {row['review']}, {checked}")
    print(
        f"\nRe-check them, then update the dates in {LEDGER.relative_to(ROOT)}. If a finding "
        "moved, change the code in the same PR rather than leaving the row and the behaviour "
        "disagreeing."
    )

    return 1 if args.strict else 0


if __name__ == "__main__":
    sys.exit(main())
