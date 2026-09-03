# Search query telemetry — the spec

What KRAIL may learn about what a rider types into search, why the line sits where it does, and
how what we keep is used. Read this before changing `SearchQueryAnalytics`,
`SearchQueryAnalyticsRedaction`, or any `search_stop_query` parameter.

This file states rules and shapes only. Real queries, counts and rates live in the private
KRAIL-Analytics repo and never appear here, in a PR, or in a commit message.

## The rule

**The typed query is collected, with every digit masked out of it before it leaves the
device.** The privacy policy (updated 2026-08-28) discloses this: search text is kept to
improve search, and it promises that house and unit numbers are masked.

```
"4 fulton place"   ->  "# fulton place"
"12/345 smith st"  ->  "##/### smith st"
"T80"              ->  "T##"
"861"              ->  "861"             // all digits - see below
"wynyard"          ->  "wynyard"
```

A house or unit number is the part of an address that identifies a home. The street is the part
the fuzzy stop matcher has to be fixed against. Masking destroys the first and keeps the second.

### The all-digit exception

**A query that is nothing but digits is sent as typed.** Riders search bus and train route
numbers and stop IDs, and those are digits with nothing else in them.

A number on its own is not an address. A house number identifies a home only in combination with
the street beside it, and in an all-digit query there is no street to combine with. Masking these
would erase a whole class of search while protecting nothing: `861` and `200060` would both
arrive as a run of `#`, indistinguishable from each other and from every other number typed.

The moment a query carries a single character that is not a digit, it can carry a street, so
every digit in it is masked. `T80` becomes `T##` - the cost of a rule with no soft edge, and the
right side to err on.

### Why masking happens on the device and nowhere else

`RealAnalytics` calls `firebaseAnalytics.logEvent` directly. There is no server of ours between
the app and Google, so **whatever the client sends is what a third party stores**. A masking step
further down the pipeline would be masking data that had already been sent. The client is the
only place the policy's promise can actually be kept.

`SearchQueryAnalyticsRedaction.maskedQueryOrNull` is the only place this happens.

### The pipeline masking script is still required

KRAIL-Analytics keeps its own digit-masking step on every pull and snapshot write. It is no
longer the guarantee, but it is not redundant either:

- 1.26 and earlier are in the field for months and keep sending under the rule they shipped with.
- It is the backstop if a future call site ever bypasses the client masking.

**Do not delete it because the client masks.** Two layers, and neither one is allowed to assume
the other ran.

## What else is sent

Everything here describes the *shape* of the query rather than its content, and predates 1.27:

| Signal | What it is | Why it is safe |
|---|---|---|
| `queryLength` | Character count of the typed query | A number cannot be geocoded |
| `queryHasDigit` | Bool | Kept even though the mask now shows digit positions: dashboards read it directly |
| `resultsCount`, `localResultsCount` | How many stops or addresses came back | Says nothing about what was asked |
| `searchSessionId` | Random per settled query | Joins a query to its selection without identifying a person |
| `addressSearchGate` | Which branch the address pipeline took | The only record of address calls *not* made |
| `resultIndex` | Which row the rider picked | A row number describes the ranking, not the rider |

## The two limits that remain

1. **Length cap.** A query longer than `MAX_QUERY_LENGTH` (40) is dropped, not truncated. Digits
   are not the only identifying thing in a query: street plus suburb together identify a home
   even with no number in them, and that pair only fits in a long query. Dropping rather than
   truncating keeps a silently shortened query from entering the eval corpus as a real one. The
   cap has to clear the stop names riders actually type - "north sydney interchange stand c" is
   32 characters - or the longest queries, which are the ones most likely to be failing, would be
   exactly the ones never reported.
2. **One firing carries it.** Only the local firing carries the text. It happens for every
   settled query, so the address firing would only ever duplicate it — same `searchSessionId`,
   twice the egress, double-counted eval cases.

## Text is attached once per typing burst, not once per keystroke

The local search debounce is 100 ms because the list on screen has to keep up with typing. The
analytics firing deliberately waits `SEARCH_ANALYTICS_QUIET_MS` **after** the results are on
screen, and the next keystroke cancels the job while it waits. So `4`, `4 f`, `4 fu` never fire —
only the query the rider stopped typing does.

This is not only an egress rule. Before it, a prefix of a search that then succeeded was counted
as a failed search in its own right, which inflated every read of the address gate: a
`BELOW_THRESHOLD` zero-result row was as likely to be a keystroke as an abandoned search, and
nothing in the data separated them.

## What the data is for

One thing: making the fuzzy stop search find what riders actually type.

`FuzzyStopSearchEvalTest` (in `:feature:trip-planner:ui` androidHostTest) scores the ranker
against every real NSW stop, and its case list is built from these queries plus false-positive
guards from manual QA. The loop is: riders type something that finds nothing, that query becomes
an eval case, the ranker is changed until it finds the right stop, and the case stays as a
regression test.

Before 1.27 only queries that found nothing anywhere were available for this, which meant the
ranker could never be scored on the near-misses — the queries that returned *something*, just
not the right thing. Those are the interesting ones.

`resultIndex` on `stop_selected` is the other half, and it needs no text at all: a rider who
scrolls past eight results to reach the one they meant has told you the ranking is wrong for that
query without telling you what the query was. Read it against `displayedLocalCount`, and remember
that zero is the common case and a real value, not a missing one.

## Rules for anyone changing this

- **A digit only ever leaves as part of a query that is nothing but digits.** That is the whole
  line, and it is the one the privacy policy states out loud. `SearchQueryTextEgressTest` and
  `SearchQueryRedactionCallSiteTest` hold it; neither may be weakened.
- **Never add a parameter carrying query text under another name.** A "first token", a
  "normalised query" or a hash of the text are all the text, and none of them is masked.
- **Never attach the text to a second event.** One firing per settled query, joined on
  `searchSessionId`.
- Any change to what a `search_stop_query` parameter means needs a row in
  `docs/ANALYTICS_REGISTRY_HANDOFF.md` in the same PR (see `CLAUDE.md`).
- Old app versions keep sending the format they shipped with. Anything that looks like a rule
  being broken in the data should be checked against the app version on the row before it is
  called a defect: the fix can only ever apply to builds that have it. Queries with real digits
  in them can still arrive from 1.26 and earlier — that is what the pipeline backstop is for.
- The AI trip-search path (`search/ai/`) resolves stops through the same stop search, so its
  matching problems show up in the same eval corpus. It sends no query text of its own, and its
  outcome logging is local only, never analytics. See `feature/trip-planner/ui/AI_SEARCH_UX.md`.

### Known gaps

- Masking covers digits. It does not cover a number written as a word — "unit four" survives
  whole. The mask is a cheap and complete filter for the way house numbers are actually typed,
  not a proof that no number can ever be expressed.
- A route number with a letter in it (`T1`, `M20`, `861X`) is masked like any other mixed query,
  so those searches arrive as `T#`, `M##`, `###X`. The letter still separates them from a street
  query, but which line was searched for is lost. Reconsider only with a rule that cannot also
  admit a house number.
