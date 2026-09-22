# Analytics registry sync: retired 2026-09-23

This doc used to describe a bot that flipped new-event rows in
`docs/ANALYTICS_REGISTRY_HANDOFF.md` from `Pending` to `Registered` once KRAIL-Analytics had
labelled the event. It was removed on 2026-09-23 together with
`.github/workflows/analytics-registry-sync.yml`, `scripts/flip_registry_status.py` and
`scripts/validate_flip_diff.py`.

**It never ran.** The dispatching side needed a credential that was never created, and its
script treated a missing one as a dry run and exited 0, so its workflow went green while sending
nothing. The KRAIL workflow ran once, the day it was built, and no `analytics-sync` PR ever
existed. Every row in the ledger was flipped by hand.

It was retired rather than fixed. The fix would have meant a token with write access to this
repo held in another repo's secrets, and an auto-merge exception, to update one status cell that
the analytics side's drift lint already surfaces as a chore.

Kept as a stub rather than deleted so that anyone who remembers the bot can tell it was removed
on purpose. The ledger's own "How to use this file" section is the current process: statuses
are maintained by hand, and the ledger is a notification channel, not a gate.

The lesson worth carrying: the commits that built the bot looked exactly like a working
mechanism. Intent in the git history is not evidence that something runs; the run history is.
