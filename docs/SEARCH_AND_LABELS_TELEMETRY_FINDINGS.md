# Search & stop-label findings from analytics — handover

Raised 2026-09-03 from a review of shipped-release telemetry in the
KRAIL-Analytics repo. Everything below was checked against this repo's source
before being written down; where the code turned out to be correct, that is
stated rather than filed as a bug.

Figures stay in KRAIL-Analytics (private). This file is qualitative on purpose —
this repo is public.

**Item 1 was corrected on 2026-09-05 and is no longer a defect.** It was a
measurement artifact of how `search_stop_query` used to fire. Read that section
before acting on anything about the address gate.

---

## 1. The address gate finding was a measurement artifact — corrected, no action

**Corrected 2026-09-05. It is kept here rather than deleted, because the bucket it
describes is still in the data and still looks like the largest group of failed
searches to anyone who queries it cold.** Delete the section and the same wrong
conclusion gets re-derived from the same rows next quarter.

### What was originally raised

On the shipped release, genuine zero-result searches — those returning nothing from
*either* pipeline — split into two roughly comparable groups by what
`AddressSearchGate` decided:

- `BELOW_THRESHOLD` — the address call was never made.
- `ELIGIBLE` — the address call ran and genuinely found nothing.

Most of the first group's zero-result queries contain a digit, and they looked like
the single largest identifiable bucket of failed searches. Since `queryHasDigit`
exists precisely because a house number is the cheapest address signal there is, the
reading was: the strongest signal that a query *is* an address sits on exactly the
queries the length threshold turns away. Someone types a house number, the gate
suppresses the only pipeline that could have answered, and they get an empty screen.

That was filed P1, and the suggested fix was to let a digit-bearing query bypass or
lower the threshold in `AddressSearchGate`.

### Why it was wrong

**`search_stop_query` fired once per settled keystroke, not once per search.** The
local search debounce was 100 ms, so anyone typing slower than that produced a firing
per character. A rider typing `4 fulton place` emitted `4`, `4 f`, `4 fu`, `4 ful` and
so on, and every one of those prefixes is short enough to be `BELOW_THRESHOLD`,
contains a digit, and finds nothing locally.

So the bucket was mostly people mid-typing on the way to a search that then worked.
Nothing in the payload distinguished a keystroke from a settled search, so there was
no way to tell the two apart from the data alone. Re-measured on the analytics side
after the firing rule changed: the great majority of that bucket is mid-typing.

The remainder is not obviously addresses either. A short all-digit query is far more
likely a bus or train route number, or a stop ID, which an address geocoder cannot
answer any better than the local search can.

### What changed

`search_stop_query` now fires once per typing burst: it waits for typing to stop
before reporting, and the next keystroke cancels the pending report. Prefixes no
longer appear as searches in their own right.

A second defect in the same path inflated the error side: a cancelled keystroke was
being reported as an error, because the cancellation was caught as one. Error rates
on this event before that fix are overstated.

### What to do

**Nothing to `AddressSearchEligibility`.** The threshold stays as it is.

If the gate is revisited, read the bucket on post-fix data only, and treat a
digit-bearing short query as a possible route number before assuming it is an address.
The assumption is tracked as `address-gate-digit-queries` in
`docs/ANALYTICS_ASSUMPTIONS.md`; check that row before changing the gate.

The same class of defect elsewhere was audited and found nowhere else — see
`docs/learning/2026-09-05-a-bucket-that-was-mostly-typing.md`.

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
| 1 | Address gate turns away digit-bearing queries | ~~Real defect~~ Measurement artifact, corrected | — |
| 2 | Reorder never observed | Product / discoverability question | P2 |
| 3 | `ADDRESS_RESULT` + `EMPTY_STATE` creation surfaces silent | Measurement blind spot | P3 |
| 4 | Free-text `labelName` | Already fixed | — |

Item 1 was originally filed as the one defect users were feeling. It was not: the
bucket behind it was mostly keystroke prefixes, and the reporting rule that produced
it has since changed. Nothing in this table is a user-visible defect today.
