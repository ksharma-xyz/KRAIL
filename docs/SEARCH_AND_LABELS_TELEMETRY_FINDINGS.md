# Search & stop-label findings from analytics — handover

Raised 2026-09-03 from a review of shipped-release telemetry in the
KRAIL-Analytics repo. Everything below was checked against this repo's source
before being written down; where the code turned out to be correct, that is
stated rather than filed as a bug.

Figures stay in KRAIL-Analytics (private). This file is qualitative on purpose —
this repo is public.

---

## 1. The address gate suppresses the queries most likely to need it — P1

**What the data shows.** On the shipped release, genuine zero-result searches —
those that returned nothing from *either* pipeline — split into two roughly
comparable groups by what `AddressSearchGate` decided:

- `BELOW_THRESHOLD` — the address call was never made.
- `ELIGIBLE` — the address call ran and genuinely found nothing.

The first group is the interesting one. **Most of its zero-result queries
contain a digit.** They are the single largest identifiable bucket of failed
searches on the shipped release.

**Why that matters.** `AnalyticsEvent.SearchStopQuery` already documents the
reasoning:

> `queryHasDigit` — Whether the typed query contains a digit - a house number is
> the cheapest address signal there is.

So the strongest available signal that a query *is* an address is present on
exactly the queries the length threshold turns away. Someone types a house
number, the gate suppresses the only pipeline that could have answered, and they
get an empty screen.

**Suggested fix.** Let a digit-bearing query bypass (or use a lower) length
threshold in `AddressSearchGate`. The telemetry to confirm the effect already
ships — `addressSearchGate` and `queryHasDigit` ride on the local firing, which
fires for every settled query, so a before/after read needs no new events.

**This interacts with `docs/SEARCH_TELEMETRY_1.27_TODO.md`.** That document's
open decision is whether to keep the client-side no-digit guard when 1.27 starts
sending query text. Note what the guard costs: digit-bearing queries are where
the failures concentrate, so keeping it means the one class of query most in
need of diagnosis is also the one class that never reports its text. That is a
real trade, not a free safety margin — worth deciding deliberately rather than
by default.

---

## 2. `stop_label_reordered` has never been observed in its current shape — P2

**The code is correct — this is not a firing-path bug.** Checked end to end:

- `ManageStopLabelsEntry.kt` dispatches both `MoveLabelToIndex` (per swap,
  mid-drag) and `LabelReorderDragCompleted` (once, on drop).
- `SearchStopViewModel` deliberately tracks only the second, and only when
  `fromIndex != toIndex`. The comment explaining why is accurate.

Since the parameter reshape, the event has still never arrived from a real user
in its current shape. A single firing exists from an old build, under the old
parameters.

**So the question is a product one, not an instrumentation one:** is the drag
affordance on Manage Labels discoverable, and does anyone have enough labels for
reordering to be worth doing? Both are answerable in a usability pass; neither is
answerable from the event stream, because the absence of the event is the whole
finding.

Worth confirming the drag handle is visible without a long-press discovery step,
and that it is reachable for keyboard and accessibility users — a gesture-only
reorder would explain the silence completely.

---

## 3. Two of four label creation surfaces have never been observed — P3

`StopLabelSurface` defines four values. Only `SEARCH_RESULT` and `RECENT` have
ever arrived. `EMPTY_STATE` and `ADDRESS_RESULT` have never been seen.

`ADDRESS_RESULT` is the notable one: address results ship on the current
release and are actively used, so a "+ New label" chip on an address row should
be producing events by now. Either the chip is not rendered on that row kind, or
it is rendered and passes a different surface.

`EMPTY_STATE` is worth the same check — an empty search is a plausible moment to
offer label creation, and if that chip exists it is not reporting.

Neither is user-visible breakage. Both mean a surface cannot be compared against
the others, so "which entry point converts best" currently has two blind spots.

Related, low priority: `labelCountBucket` on `stop_label_created` has only ever
reported the `3_5` bucket. Consistent with default labels existing before the
first custom one, so probably correct — but it means the bucket currently
carries no information at creation time, and `ONE`/`TWO` may be unreachable
there by construction.

---

## 4. Already fixed — no action, recorded so it is not re-raised

Old builds sent a free-text `labelName` on `stop_label_created` and
`stop_label_removed`, which carried user-typed content including home addresses.

**The current release does not send it** — the parameter reshape replaced it with
`labelCountBucket`, and it is absent from every recent build's events. The
remaining volume is old builds still in the field, which will age out on their
own. Nothing to change in this repo; the historical rows are handled on the
analytics side.

---

## Summary

| # | Item | Kind | Priority |
|---|---|---|---|
| 1 | Address gate turns away digit-bearing queries | Real defect, user-visible | **P1** |
| 2 | Reorder never observed | Product / discoverability question | P2 |
| 3 | `ADDRESS_RESULT` + `EMPTY_STATE` creation surfaces silent | Measurement blind spot | P3 |
| 4 | Free-text `labelName` | Already fixed | — |

Only item 1 is a defect users are feeling today.
