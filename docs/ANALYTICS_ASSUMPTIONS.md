# Analytics assumptions

Design decisions in this app that were made from KRAIL-Analytics data, with the date each was
last checked and the date it falls due.

A decision justified by data is only as good as the last time anybody looked. These get written
into code comments as settled fact ("about a third of trips are loaded at a weekend, so nothing
presumes a commute"), and six months later nobody knows whether that is still true or whether it
was ever measured. This file is where that gets recorded, and
`scripts/check_stale_assumptions.py` is what stops it becoming a promise nobody keeps.

## Content rule: qualitative only

**KRAIL is a public repository.** Never write a number in this file. No user counts, no
percentages, no rates, no ratios, no absolute event volumes. Write the shape of the finding
instead:

- Yes: "weekend openings are a substantial share of all openings"
- No: "34% of openings are at weekends"

Keep the real figures in local files. The same rule governs commit messages and PR bodies; see
`.claude/skills/pr-desc/SKILL.md`.

## How to use this file

Each row is one assumption that some piece of code depends on.

- **Assumption** — the claim, qualitatively. If a code comment states it, quote it the same way.
- **Relies on it** — what breaks or becomes wrong if the claim stops holding.
- **How to check** — the query or dashboard that answers it. Enough that a re-check is ten
  minutes rather than an afternoon of rediscovering which event to look at.
- **Would falsify it** — what a result would have to look like for the decision to be wrong.
  The column people skip; it is the one that makes the re-check quick and honest.
- **Last checked** / **Review by** — ISO dates. Review by is normally last checked plus three
  months: these are travel-behaviour patterns and they move slowly.

The script parses the two date columns and the id, so keep the table shape. Ids are stable
across rewordings.

When you re-check one: update **Last checked**, push **Review by** out, and if the finding moved,
change the code in the same PR rather than leaving the row and the behaviour disagreeing.

## Assumptions

| Id | Assumption | Relies on it | How to check | Would falsify it | Last checked | Review by |
|---|---|---|---|---|---|---|
| `open-time-shape` | App opens peak in the morning and again in the late afternoon, with a substantial midday that is not far behind either, and a real tail into the evening. | The situation bands in `AiSuggestionSituations.kt`: where morning ends, when the journey turns around, how late the last band runs. | `load_timetable_click` grouped by hour, Sydney time, over 60 days. | A single peak, or an evening that falls away sharply enough that the late bands are dead weight. | 2026-08-16 | 2026-11-16 |
| `weekend-share` | Weekend openings are a substantial share of all openings, not a rounding error. | The whole weekend half of the situations table, and the rule that nothing presumes a commute. If weekends were rare, six commute-shaped rows would be the better design. | `load_timetable_click` grouped by day of week, over 60 days. | Weekend openings becoming a small minority. | 2026-08-16 | 2026-11-16 |
| `address-gate-digit-queries` | Short digit-bearing queries turned away by the address length threshold are NOT predominantly failed address searches. Most of that bucket was riders mid-typing, and much of the rest is plausibly route numbers and stop IDs, which an address lookup answers no better than the local search does. | The address length threshold in `AddressSearchEligibility` staying where it is. A lower or digit-bypassing threshold was proposed on the earlier reading and rejected on this one. | `search_stop_query` grouped by `addressSearchGate` and `queryHasDigit`, restricted to rows whose firing regime is `per_burst_masked`. KRAIL-Analytics derives the regime per app version from the data (`search_firing_regime`, read with `getAggQuery` and joined on `app_version`); the other values are `per_keystroke_no_session`, `per_keystroke_carve_out`, and `pending` for a version with too little traffic to classify yet. Treat `pending` as undecided, not as pre-change. **Do not gate on the version string**: 1.27.0 exists in the field in both forms, so a version comparison reintroduces the mixing this row exists to prevent. | A digit-bearing below-threshold bucket that stays large once keystroke prefixes can no longer inflate it, and whose queries read as street addresses rather than route numbers. | 2026-09-05 | 2026-12-05 |
| `label-adoption` | Riders who use the app regularly tend to have Home set, and often a second label. | The suggestion ladder trying labels first, and the claim that the label rung is what most riders actually see. | Not yet measured. No event reports whether a rider has labels set. | If most riders reached the saved-trip or generic rung instead, the ladder's order would be teaching the wrong thing first. | — | 2026-11-16 |

## Open gap

`label-adoption` has never been checked, because nothing measures it. It is listed rather than
left implicit: the suggestion ladder is built on it, and an assumption holding up a design is
worth naming even when the data to test it does not exist yet. Measuring it would mean a
parameter on an existing event rather than a new event name, given the 500-event cap. See
`docs/ANALYTICS_EVENTS.md` before adding anything.
