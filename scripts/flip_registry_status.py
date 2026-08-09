#!/usr/bin/env python3
"""Flip docs/ANALYTICS_REGISTRY_HANDOFF.md rows from Pending to Registered for event
names KRAIL-Analytics has just labelled.

Reads the newly-labelled event names from the EVENTS_JSON env var (a JSON array of
strings, forwarded from a repository_dispatch client_payload). Only touches rows for a
brand new event name (Event column has "(NEW EVENT)", or Param(s) has "NEW event") --
param and user-property rows have no per-item registry surface on the analytics side
and are never flipped by this script.

Writes the flipped event names to $GITHUB_OUTPUT as `flipped` (comma-separated) when
run in CI, or just prints them otherwise. Writes nothing and exits quietly if no row
matched -- this is the common case, since most dispatches won't name a new event.
"""

import json
import os
import re

LEDGER_PATH = "docs/ANALYTICS_REGISTRY_HANDOFF.md"


def is_new_event_row(event_cell: str, param_cell: str) -> bool:
    return "(NEW EVENT)" in event_cell.upper() or "NEW EVENT" in param_cell.upper()


def event_name_from_cell(event_cell: str) -> str:
    name = event_cell.replace("`", "")
    name = re.sub(r"\(NEW EVENT\)", "", name, flags=re.IGNORECASE)
    return name.strip()


def main() -> None:
    events = set(json.loads(os.environ.get("EVENTS_JSON", "[]")))
    if not events:
        print("No event names in EVENTS_JSON; nothing to do.")
        return

    with open(LEDGER_PATH, encoding="utf-8") as f:
        lines = f.readlines()

    flipped = []
    for i, line in enumerate(lines):
        if "| Pending |" not in line:
            continue
        cells = [c.strip() for c in line.split("|")]
        if len(cells) < 4:
            continue
        event_cell, param_cell = cells[2], cells[3]
        if not is_new_event_row(event_cell, param_cell):
            continue
        event_name = event_name_from_cell(event_cell)
        if event_name not in events:
            continue
        lines[i] = line.replace("| Pending |", "| Registered |")
        flipped.append(event_name)

    if not flipped:
        print("No Pending row matched a dispatched event name.")
        return

    with open(LEDGER_PATH, "w", encoding="utf-8") as f:
        f.writelines(lines)

    print(f"Flipped: {', '.join(flipped)}")

    gh_output = os.environ.get("GITHUB_OUTPUT")
    if gh_output:
        with open(gh_output, "a", encoding="utf-8") as f:
            f.write(f"flipped={','.join(flipped)}\n")


if __name__ == "__main__":
    main()
