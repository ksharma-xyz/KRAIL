# The largest bucket of failed searches was mostly people mid-typing

**2026-09-05** · Analytics / an event that counted keystrokes as intents · Cost: one wrong P1,
carried in a handover doc for two days, nearly acted on

## Symptom

Nothing on a device. This one was only ever visible in the data.

A review of shipped telemetry found that genuine zero-result searches split roughly evenly by
what `AddressSearchGate` decided, and that most of the `BELOW_THRESHOLD` half contained a digit.
Read straight, that says: riders type a house number, the length threshold suppresses the only
pipeline that could have answered, and they get an empty screen. It was filed P1 as the one
defect users were feeling, with a proposed fix to let digit-bearing queries bypass the
threshold.

The code was correct. The tests were green. The event fired exactly where it was supposed to.
The conclusion was still wrong.

## Root cause

`search_stop_query` fired **once per settled keystroke, not once per search.**

The local search debounce was 100 ms, chosen so the list on screen keeps up with typing. Anyone
typing slower than that — which is most people — produced one firing per character. A rider
typing `4 fulton place` emitted `4`, `4 f`, `4 fu`, `4 ful`, and so on.

Every one of those prefixes is short enough to be `BELOW_THRESHOLD`, contains a digit, and finds
nothing in the local stop table. So each one landed in the bucket as an independent failed
search, and the rider's search then succeeded a few hundred milliseconds later.

Two things made it invisible:

1. **`searchSessionId` is minted per settled query**, so it joins a query to its own selection —
   it does not group a typing session. There was no key that said "these eight rows are one
   person typing one thing."
2. **A prefix is indistinguishable from an abandoned search.** `4 ful` and a rider who gave up
   after typing `4 ful` produce byte-identical rows.

A second defect in the same path inflated the error side: `runCatching` around the local search
swallowed the `CancellationException` thrown when the next keystroke cancelled the job, so every
cancelled keystroke was reported as `isError = true` and also flashed the error state.

## Why it took so long

It did not take long to find. It took two days to *doubt*, which is the part worth recording.

- **The finding was internally consistent.** `queryHasDigit` exists in the codebase precisely
  because a house number is the cheapest address signal there is. So "the digit-bearing queries
  are the address queries" reads as the parameter's own documented purpose confirming the
  finding. It was written into `AnalyticsEvent.kt` as reasoning, and then used as evidence.
- **It had been checked against the source.** The handover doc says so, and it was true: the
  gate code was read and found correct. Reading the *firing* code, in a different file, was not
  part of that check, and nothing suggested it should be.
- **The number was large and stable.** A big consistent bucket reads as a real phenomenon. Its
  size was entirely an artifact of typing speed.
- **The fix was cheap.** A one-line threshold change is easy to say yes to, which is exactly
  when a wrong premise costs the most.

What actually broke it open was changing the instrumentation for an unrelated reason — the
1.27 privacy work moved the firing to once per typing burst — and only then re-measuring. Nobody
reasoned their way to it from the data, because the data could not express the distinction.

## What would have caught it sooner

**Ask what one row means before computing a rate from it.** Not "is this event correct" but
"can this event fire more than once for one thing a rider did?" If it can, and nothing in the
payload identifies the intent, then every count, rate and ranking derived from it is wrong by an
unknown factor — and it will look plausible, because the bias is smooth.

The reusable check is a question about the event's *shape*, and it applies to any event fired
from a debounced input or a polling loop:

> Does this fire once per user intent, or once per tick?

An event fired from an `onEvent` handler is intent-shaped by construction. An event fired from
inside a debounce, a `while (true)`, or a flow collector is not, unless something makes it
edge-triggered.

## Actions taken

- [x] `search_stop_query` now fires once per typing burst. It waits out a quiet period after the
  results render, and the next keystroke cancels the pending report — cancellation does the work,
  so no extra state. See `SearchStopViewModel.SEARCH_ANALYTICS_QUIET_MS`.
- [x] Pinned by a test: `SearchStopViewModelTest` asserts three keystrokes produce one reported
  search. That test is the check; without it the debounce is one refactor from being tuned back
  down to the UI's value.
- [x] Cancellation defect fixed in the same path — `suspendSafeResult` instead of `runCatching`,
  which rethrows `CancellationException` rather than reporting it as an error.
- [x] **Audited every other analytics call site reachable from a debounce or a repeating loop**
  for the same shape. Four candidates besides this one; all are already intent-shaped:
  `DeparturesViewModel` fires its status event only on the first transition into error per
  polling session, `TimeTableViewModel`'s auto-refresh loop pokes the rate limiter and tracks
  nothing, `DeviceWindowReporter` is debounced and diffed against last reported state, and
  `SavedTripsViewModel` fires from `onEvent` handlers.
- [x] The original finding corrected in place in `docs/SEARCH_AND_LABELS_TELEMETRY_FINDINGS.md`
  rather than deleted. The bucket is still in the data and still looks compelling to anyone
  querying it cold.
- [x] Recorded as `address-gate-digit-queries` in `docs/ANALYTICS_ASSUMPTIONS.md`, so the claim
  now has a scheduled re-check instead of living in a handover doc. That it never entered the
  ledger is the process half of this failure: the ledger exists for exactly this, and a
  data-derived claim went into a P1 label without passing through it.

## The rule that came out of it

An analytics event that can fire more than once per user intent must carry something that
identifies the intent, or it must be edge-triggered. Otherwise the event is honest and every
metric built on it is not.
