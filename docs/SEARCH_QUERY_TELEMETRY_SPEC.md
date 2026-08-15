# Search query telemetry — the spec

What KRAIL may learn about what a rider types into search, why the line sits where it does, and
how the little we keep is used. Read this before changing `SearchQueryAnalytics`,
`SearchQueryAnalyticsRedaction`, or any `search_stop_query` parameter.

This file states rules and shapes only. Real queries, counts and rates live in the private
KRAIL-Analytics repo and never appear here, in a PR, or in a commit message.

## The rule

**A typed search query is treated as personal data.** It can be a street address, and a street
address is a home or a workplace. The privacy policy promises analytics carry nothing
personally identifying, so the default for the text itself is: never send it.

What we send instead describes the *shape* of the query, never its content:

| Signal | What it is | Why it is safe |
|---|---|---|
| `queryLength` | Character count | A number cannot be geocoded |
| `queryHasDigit` | Bool | A house number is the cheapest address signal there is, and a bool carries no address |
| `resultsCount`, `localResultsCount` | How many stops or addresses came back | Says nothing about what was asked |
| `searchSessionId` | Random per settled query | Joins a query to its selection without identifying a person |
| `addressSearchGate` | Which branch the address pipeline took | The only record of address calls *not* made |

## The one carve-out

Raw text is sent for a query that found **nothing anywhere**. All four conditions, together:

1. zero local stop results, and
2. zero address results, or the address pipeline never ran, and
3. no digits in the query, and
4. no longer than `MAX_ZERO_RESULT_QUERY_LENGTH` characters.

`SearchQueryAnalyticsRedaction.zeroResultQueryOrNull` is the only place this decision is made.

The reasoning: a query that matched nothing is, by definition, not a place we know — so it is
far more likely to be a misspelling of a stop than a real address. The condition that makes
this safe is the *conjunction*: something that finds nothing, has no house number in it, and is
short is a fuzzy-matcher diagnostic, not a location. A query that finds something is a
different thing entirely, and is never sent whatever else is true of it.

**Which pipeline owns the decision.** When the address pipeline is eligible for a query, the
local site must not decide, because the address API may well recognise as a real address what
the local stop search did not. The decision moves to the address completion site, which is the
only place that knows both counts. `resolveLocalZeroResultQuery` returns null in that case,
deliberately.

### Known gap

`#` is not a digit. A rider writing a unit or house number as "# 12" style text can pass
condition 3 while having written exactly the thing the condition exists to exclude. The digit
test is a cheap filter, not a proof, and this is where it is thin.

## What the data is for

One thing: making the fuzzy stop search find what riders actually type.

`FuzzyStopSearchEvalTest` (in `:feature:trip-planner:ui` androidHostTest) scores the ranker
against every real NSW stop, and its case list is built from these zero-result queries plus
false-positive guards from manual QA. The loop is: riders type something that finds nothing,
that query becomes an eval case, the ranker is changed until it finds the right stop, and the
case stays as a regression test.

This is why the carve-out earns its place. Without it the ranker can only be tuned by guessing
at what people type.

## Rules for anyone changing this

- **Never widen the carve-out to queries that returned results.** That is the whole line.
- **Never add a parameter carrying query text under another name.** A "first token", a
  "normalised query" or a hash of the text are all the text.
- Any change to what a `search_stop_query` parameter means needs a row in
  `docs/ANALYTICS_REGISTRY_HANDOFF.md` in the same PR (see `CLAUDE.md`).
- Old app versions keep sending the format they shipped with. Anything that looks like a rule
  being broken in the data should be checked against the app version on the row before it is
  called a defect: the fix can only ever apply to builds that have it.
- The AI trip-search path (`search/ai/`) resolves stops through the same stop search, so its
  matching problems show up in the same eval corpus. It sends no query text of its own, and
  its outcome logging is local only, never analytics. See `feature/trip-planner/ui/AI_SEARCH_UX.md`.
