# Ask KRAIL analytics, the plan

What KRAIL will measure about the Ask KRAIL surface, what it will deliberately never measure,
and why each line sits where it does.

**Status: proposal. No code written, nothing instrumented.** Reviewed by the KRAIL-Analytics
side on 2026-09-22; their changes are folded in below and marked where the reasoning is theirs,
because most of it comes from a scar in their repo rather than from preference.

This file states shapes and rules only. Real counts and rates live in the private
KRAIL-Analytics repo and never appear here, in a PR, or in a commit message.

---

## 1. Where things stand

`components/ai/` (the screen), `search/ai/` (the pipeline) and `alerts/summary/` (the sibling
on-device AI card) contain no `Analytics` reference at all. Nothing in `AnalyticsEvent.kt`
matches ai, speech, mic or alert summary. The feature has never been measured.

Two facts make this urgent rather than tidy-up work:

1. **The flag is already on, and part of the measurement window has closed.**
   `AI_SEARCH_INPUT_ENABLED` defaults off in code (`DEFAULT_AI_SEARCH_INPUT_ENABLED = false`,
   matching the Remote Config default), which reads as "not rolled out". It is not:
   `docs/STORE_DATA_DECLARATIONS.md` records that `ai_search_input_enabled` **was turned on for
   1.27**, which is why the microphone moved onto the Play and Apple forms as collected.

   So 1.27 ships this feature to riders with no instrumentation at all, and that period is
   **permanently dark**. No later release recovers it. The availability signal in §7 should land
   in 1.28 to start the denominator from there, and §12's sequencing is about the remaining
   rollout rather than about a first flip.

   Recorded because the code default actively misleads: a Remote Config value in production is
   not visible from the source, and the only written record of its real state is a store-forms
   doc nobody opens when planning analytics. **Check the RC state, not the Kotlin default.**
2. **The per-attempt event already exists as a log line.** `AiSearchInputViewModel.logOutcome()`
   emits one wide row per attempt carrying phase, reason, from-resolved, to-resolved and
   spokeIt, and its KDoc records that it deliberately carries no rider text. Promoting it is
   most of the work in this plan, and the privacy thinking was done when it was written.

---

## 2. The rule: a pattern may leave, the sentence never does

**No part of what the rider wrote leaves the device as their words.** What may leave is a
*template* of the sentence with every place and time replaced by a placeholder, and only when
two independent gates both pass.

```
typed:  "get me home from 12 smith st by 6"
sent:   "get me <PLACE> from <PLACE> by <TIME>"
```

The template answers the question the raw text was wanted for. "What do riders most commonly
say" is a question about **phrasing**, and the phrasing is exactly what survives substitution.
The street name was never the part that carried the answer.

### Why the search rule could not simply be reused

`SearchQueryAnalyticsRedaction` masks every digit and drops anything over 40 characters. It
does not transfer, and the reasons are worth keeping because the surfaces look similar enough
that someone will reach for it again:

- An Ask KRAIL sentence is long by design, so the 40-character cap would drop nearly all of
  them. The cap would do no work and the masking would be carrying the whole load.
- Digit masking is not enough for a sentence. `"get me home from 12 smith st by 6"` masked is
  `"get me home from ## smith st by #"`, and a street plus a suburb identifies a home with no
  number in it at all. That is the reason the search rule has a length cap in the first place.
- A query is one place reference. A sentence is origin, destination and time together, which is
  a schedule rather than a lookup.

### The two gates

**The model is not trusted.** It is wrong often enough that masking built on its output alone
would leak whenever it erred, and it errs silently: a place it fails to report is a place that
stays in the sentence with nothing indicating anything went wrong.

| | Gate | Catches |
|---|---|---|
| 1 | Every span the model claims must appear **verbatim** in what the rider typed, matched case-insensitively and whole-word | The model rewording or inventing. "bondi junctn" typed, "Bondi Junction" returned |
| 2 | After substitution, **every remaining word must be in a small allowlist** of function words | The model missing a place entirely. Nothing failed, so gate 1 cannot see it |

**Gate 2 is the one that provides the guarantee**, because it asks the model nothing. It inverts
the trust model: rather than detecting the sensitive parts and removing them, it only permits a
sentence whose every non-placeholder word is already known to be safe.

Gate 1 has a precedent in this repo. `AiSearchInputViewModel.kt:597` already runs
`typedText.contains(place, ignoreCase = true)` before quoting a place back at the rider, for the
same reason: a reworded name in quote marks would read as something the rider said.

Failing either gate **drops the row entirely**. Never a partially substituted sentence, never a
truncation. The model being wrong must cost data and never privacy.

### Worked examples

| Rider types | Outcome |
|---|---|
| `get me home by 9pm` | Sent as `get me <PLACE> by <TIME>`. Leftovers `get me by`, all allowlisted |
| `goin to bondi junctn` | **Dropped.** Model returns the corrected spelling, gate 1 fails |
| `10am Monday work` | Sent as `<TIME> <PLACE>`. The model swallowing the day into the place over-masks, which is safe |
| `meet sarah at the clinic on king st then home` | **Dropped.** Model finds only "home"; `sarah`, `clinic`, `king`, `st` are not allowlisted, gate 2 fails |
| `12 smith st to work` | **Dropped.** Model misses the address; digits and `smith` fail gate 2 |

Over-masking is always safe. Under-masking is always dropped.

### The drop rate is high, and that is the design working

Most sentences will fail gate 2. That is the correct outcome, because of **what** fails it:
common phrasings are built from common words by definition and pass, while an unusual sentence
is unusual precisely because it names a person, a clinic or a street.

### What the surviving corpus actually is, which is not "what riders say"

This has to be stated beside any frequency readout, because the drops are **biased**, not random,
and biased in the direction that flatters us.

The two failure modes split in the worst possible way:

| Rider types | Outcome |
|---|---|
| `when is the next train` | Named no place, every word allowlisted, **survives** |
| `get me to the clinic on smith st` | Model misses the place, raw text remains, **dropped** |

So the corpus systematically contains the failures where riders named nothing, and systematically
excludes the failures where they named something we did not recognise. **The second group is
where the fixes are.**

What can honestly be said of a template distribution is therefore: *these are the phrasings
riders build from vocabulary we already understand.* That is close to the opposite of "what
riders say that we cannot handle", and presenting it as coverage of rider phrasing would be
wrong.

This is not an argument for weakening gate 2. It is the reason `templateKept` exists: read
together, the shape is honest, because one says what the distribution is and the other says what
fraction it was drawn from.

**`templateKept` is a boolean that survives the drop.** The template itself is removed before it
reaches permanent storage, so from history the param is always absent and "absent because a gate
failed" cannot be told from "absent because it was dropped on purpose". Since the whole design
rests on gate 2, and a model update could shift its pass rate without anything visible changing,
the gate's own behaviour has to be measurable forever. Gate 1 already had this in
`spanMatchedVerbatim`; gate 2 is the one carrying the guarantee and had nothing.

### The allowlist

Small and boring on purpose, in `AiSentenceTemplateRedaction` beside the placeholders. Function
words, movement verbs and the connective vocabulary riders actually use: *a, at, be, by, from,
get, go, going, i, in, me, my, need, next, now, on, the, then, to, want, when*, and similar.

Rules for it:

- **Whole-word, case-insensitive, no stemming.** Same discipline as `LabelSynonyms`, which is
  exact for the same reason: a nearly-right match is worse than none.
- **No proper nouns, ever.** Not suburb names, not line names, not `Central`. A word that names
  a place does not become safe by being a common place.
- **No digits.** Any digit outside a `<TIME>` placeholder fails the gate.
- **Growing it needs a reason and a test.** Each addition widens what can be sent, so the list is
  the security boundary and changes to it are reviewed as such.

### What is still never sent, under any gate

`unmatchedPlace` (the rider's own word for a place we could not find), label words, `modeHints`
as strings, `fromText` / `toText`, and any transcript.

The `STOP_NOT_FOUND` case deserves naming, because it is the highest-value question about this
feature and someone will ask: *which place did we fail to find?* The answer stays no. §5.3 gets
at it another way, by joining to the stop the rider settled on afterwards, which gives the
answer without the question.

---

## 3. Two new event names

Folded hard. The app's binding constraint is not the 500 event-name budget (~437 free) but the
GA4 event-scoped custom dimension cap. See §9, which largely dissolves that fight.

### `ask_krail_attempt`

One row per submit, fired where `logOutcome()` already fires.

| Param | Values | Notes |
|---|---|---|
| `phase` | `RESOLVED` / `UNRESOLVED` / `DOWNLOADING` | |
| `reason` | `UnresolvedReason` name, or `flag_off`, `empty_text`, `model_unavailable_*` | Also absorbs `dismissedAtPhase`, see below |
| `endsResolved` | `none` / `from_only` / `to_only` / `both` | One enum, not two booleans |
| `originSource` | `said` / `labelled_nearby` / `nearest` / `home` / `standing_at_dest` / `blank` | Currently `[AI_ORIGIN]` logcat only |
| `inputMode` | `typed` / `spoken` / `mixed` | |
| `timeShape` | `none` / `absolute` / `relative` / `day_only` | Replaces a `hadTime` bool. Which *kind* of time the grammar reads is the actionable half |
| `extractedEnds` | `none` / `origin_only` / `destination_only` / `both` | What the **model** found, before resolution. The gap against `endsResolved` is the diagnostic, see §5 |
| `hadLabelWord` | bool | The sentence contained a label word (`work`, `office`, `uni`). Never which one |
| `unmatchedKind` | `single_word` / `multi_word` / `has_digit` / `label_word` | Shape of the place that matched no stop. Only on `STOP_NOT_FOUND` |
| `attemptIndex` | 1, 2, 3... within one dialog session | Semantics pinned in §4 |
| `extractMs` | **raw milliseconds** | Not bucketed, see §4 |
| `askSessionId` | random hex per dialog open | Join key only |

Thirteen param names against a cap of 25, though a given row carries fewer: `unmatchedKind`
appears only on a `STOP_NOT_FOUND` row. That makes this the densest event in the app, ahead of
`app_start`.

`candidatesRejected` is **not** in that list and was removed before the event shipped. It needs
the word-boundary guard to report what it discarded, which it does not, so a declared param
would have been one that never arrived. A param that is declared and absent reads on a dashboard
exactly like a param that is being dropped, and telling those apart costs someone an afternoon.
It is described in §5.2 as work still to do rather than as a field. Deliberate, and §5 is the argument for it: these params are what
replaces collecting the rider's sentence, so the density is bought with a privacy decision
rather than spent carelessly.

`app_start` is the densest, and counting it is a trap twice over.

**It declares nine constructor params and emits eleven.** `timeStamp` is computed inline in
`rawProperties` and is not a constructor param, and `AnalyticsPaneTracker.decorate` adds `pane`
centrally in `RealAnalytics.track`. Counting declarations undercounts what consumes dimension
budget. Count what is emitted.

**And counting from the BigQuery export overcounts, because a param can outlive its code by
years.** `app_start` appears in the export carrying a twelfth param, `batteryLevel`, which is in
no source file in this repo in any language. It is not a defect and not a second central rider:
it was added by #574 on 2025-01-28, removed by #1402 on 2026-02-22, and is still arriving from
devices running builds from that window. `v1.16.0` contains it, `v1.17.0` does not.

Those rows are also **Android-only in practice**, which nothing on them says. The iOS
implementation read `UIDevice.currentDevice.batteryLevel` without ever setting
`isBatteryMonitoringEnabled`, so iOS returned `-1.0`, and `(level * 100).toInt().absoluteValue`
turned that into a constant `100`. Every iOS row reported a full battery. A broken reading that
lands inside the plausible range is worse than a missing one, and it is the same failure shape
as the `~trunc` suffix in `AnalyticsParamSanitizer`: a value that looks real is not checked.

**The source tree says what ships. The export says what is running**, which includes every build
still installed on a device. Both are correct and they answer different questions, so budget
against what current builds emit, and use the version range to tell a ghost from a bug before
losing an afternoon to it.

One wrinkle that makes the version range less decisive than it looks: `1.17.0` appears on
**both** sides of that boundary. The removal landed seven hours before the `v1.17.0` tag was
cut, and the branch cut bumps `main` to the next minor at the *start* of a cycle, so builds made
during the 1.17 cycle report `1.17.0` while containing code removed before the tag. This is the
same rule `docs/SEARCH_QUERY_TELEMETRY_SPEC.md` already states for `search_stop_query`: **a
version number does not identify the code.**

**`endsResolved` as one enum is deliberate.** Two booleans get summed independently by every
aggregation, and the from-only versus to-only distinction disappears into two marginal rates
that do not reconstruct the joint. Four genuinely exclusive states, one dimension.

**`originSource` is the most product-loaded param here**, not engineer trivia. The ladder rung
IS a product decision: if `nearest` dominates `labelled_nearby`, the labelled-stop preference in
`RiderOriginLocator` is not earning its place in this flow. If `blank` is common, the two
deliberately-blank cases (standing at the destination, known location with no stop near it) are
more common than anyone expects. Chart it in week one.

**`dismissedAtPhase` is folded into `reason`** rather than taking its own dimension. Dismissed
while `EXTRACTING` is a latency complaint, dismissed at `UNRESOLVED` is a message that did not
help, dismissed at `IDLE` with text typed is cold feet. Three different products of the same
gesture, and `reason` already has the shape to carry them.

### `ask_krail_status(action)`

Everything that is not an attempt, folded into the app's existing `{feature}_status(action)`
pattern rather than seven event names.

`opened`, `dismissed`, `start_over`, `mic_start`, `mic_stop`, `mic_denied`, `mic_blocked`,
`speech_unsupported`, `speech_error`, `listen_timeout`, `handoff_settled`,
`suggestion_rendered`.

Extra params: `reason` (speech errors and dismiss phase, shared vocabulary with the attempt
event). `handoff_settled` carries **no** outcome param: it is the denominator and nothing more,
which is what lets it fire without waiting on a rider decision. See §4.

**`suggestion_rendered`, not `suggestion_shown`.** It is an app action, not a rider one, and the
name has to say so. The analytics side has the scar: `review_prompt_requested` was labelled
"shown" in their registry, and that one word is how it got read as an impression count for
months when it is an upper bound on asks, which is itself an upper bound on shows. A value
sitting in an enum next to `mic_start` and `dismissed` will be read as something the rider did
unless the name refuses that reading.

**No `view_screen` for the dialog.** It is a dialog, not a nav route. `action = opened` covers
it and firing both double-counts, which is the check `docs/ANALYTICS_EVENTS.md` asks for.

---

## 4. Four shapes that have to be right the first time

### `extractMs` ships raw, not bucketed

This is the change the analytics side pushed hardest on and it is right.

**Bucketing at collection is irreversible.** The boundaries encode today's guess about which
latencies matter, and every row collected under them is permanently stuck with that guess.
KRAIL-Analytics deliberately does the opposite: bands are computed downstream from raw
values, so re-banding across all history costs nothing and needs no app change.

If this ships bucketed and someone asks for p95 in month two, and they will, because that is the
question a latency number exists to answer, it is unanswerable for every row already collected
and the clock restarts at the next release.

Raw milliseconds costs the same single dimension. Its cardinality is not a problem because it is
BigQuery-only (§9).

### `attemptIndex` semantics, pinned here before any code

The analytics side's worst scar in their repo is exactly this class of bug. `searchSessionId` is
minted per settled keystroke rather than per search; on 2026-09-03 that invalidated a P1 finding
outright, because grouping by it counted keystrokes and much of a zero-result bucket turned out
to be riders mid-typing rather than failed searches. The metric was wrong for months and read as
authoritative the whole time.

"Attempt 2 succeeded where attempt 1 failed" is the best extraction-quality signal in this plan,
and the reset rule **is** that metric's definition. So:

| Question | Decision |
|---|---|
| Resubmitting identical text | Increments. It is a second attempt from the rider's point of view |
| Edit then resubmit | Increments |
| Reset on dialog close | Yes. `OpenInput` already resets the whole state; the counter goes with it |
| Reset on successful resolve | Yes, on handoff settle. Stated as its own rule, not as a consequence of the dialog closing |
| Reset on app background with the dialog open | No, and the reason is the argument: see below |
| Reset on `StartOver` | Yes. `StartOver` is the rider saying this is a new question |

**The invariant, stated directly so it survives the UI changing: one `askSessionId` is one
question, and both it and `attemptIndex` reset on handoff settle and on dialog close.**

Coupling the reset to "the dialog auto-closes after the settle beat" would tie the counter's
lifetime to a UI behaviour. If that behaviour ever moves for product reasons and nobody connects
it to analytics, the reset silently stops happening and `attemptIndex` starts spanning several
questions with no signal at all. The analytics side has that exact scar:
`save_trip_prompt_shown` changed meaning on 2026-08-07 because a display detail moved underneath
it, and every rate built on it jumped at the boundary and looked like the feature suddenly
working.

**Why backgrounding must not reset.** A rider who switches out to look up an address and comes
back would restart at 1, so their return attempt books as a first attempt. That inflates
first-attempt success, the headline quality number, specifically for the riders who had to go
and check something, which is to say the ones who struggled. The bias runs in the flattering
direction, which is the worst kind.

### `askSessionId` stays a separate param from `searchSessionId`

No shared namespace with a `kind` param. `searchSessionId` first appears on
`load_timetable_click` on 2026-08-09, so every row before that has no join. If Ask KRAIL reused
the name, a `load_timetable_click` carrying a `searchSessionId` with no matching
`search_stop_query` would be ambiguous three ways: a pre-join-era row, an Ask KRAIL row, or a
genuine orphan. That ambiguity cannot be resolved retroactively. Two distinct names disambiguate
for free and permanently, and a `kind` param would spend a dimension to recreate a problem that
otherwise does not exist.

### The handoff needs a denominator, not only a numerator

The original plan fired `handoff_edited` when the rider corrected a field the AI had written.
That is the right instinct and it is the only obtainable signal for `AI_SEARCH_UX.md` failure
mode 7 (the model resolves confidently to the wrong stop, the field fills silently, and nothing
in the app can tell it is wrong).

As specified it was a lower bound with noise on both sides: a rider who gets a wrong stop and
just closes the dialog is invisible, and a rider who edits may be refining a match that was
already correct. It would have been read as "the wrong-match rate" within a month of existing.

**Abandonment is not an observable event.** The rider does not do anything, they stop. Any
moment picked to fire an abandonment row is a guess about intent, so the plan stops trying to
fire one and derives it from absence instead. Three roles, three places:

| Role | Where it comes from |
|---|---|
| Denominator | `ask_krail_status(handoff_settled)`, fired when the AI writes the fields. Every handoff, no rider decision needed. Carries no outcome param |
| Numerator and detail | `handoffKept` on `load_timetable_click`, which is already gaining `askSessionId`. Fires only for riders who pressed Search, which is exactly who it describes |
| Abandoned | A `handoff_settled` with no `load_timetable_click` sharing its `askSessionId`. Falls out of the join |

`handoffKept`: `kept_both` / `changed_from` / `changed_to` / `changed_both`.

Verb-first because `both` sitting next to `both_changed` has to mean both-kept but reads as
both-something, and an enum value is what appears on a chart axis with no docs attached.

The param count is unchanged: `handoffKept` **moves** rather than adds. Putting it on
`load_timetable_click` also puts the evaluation where the comparison is natural, because at
Search time both what the AI wrote and what is actually loading are in hand.

**An earlier draft had `handoffKept` on `handoff_settled` but evaluated at Search press**, which
is two different moments wearing one event name. That shape decays: someone later moves the fire
point to match the name, or the name to match the fire point, and either way the metric changes
meaning with no signal that it did.

### The wrong-match rate is a bracket, not a number

Someone will want a single figure. There is not one, and the honest shape is:

- **Lower bound**: changed among converters.
- **Upper bound**: changed among converters, plus every abandoner.

The truth is inside. Neither endpoint is defensible alone, because abandonment has innocent
causes and an edit can be a refinement of a match that was already correct. The bracket is
defensible, and it is the strongest claim this instrumentation can support about failure mode 7.

---

## 5. Learning from a failure without collecting the sentence

The hard question this plan has to answer, because refusing the text is only defensible if the
thing the text would have bought is obtained another way. Five substitutes, cheapest first.
Together they cover most of what a corpus of failed sentences would have given.

### 1. The extraction gap says which system failed

`extractedEnds` (what the model found) against `endsResolved` (what matched a stop) splits
failures that currently look identical:

| `extractedEnds` | `endsResolved` | What broke |
|---|---|---|
| `none` | `none` | The **model**. The sentence was not parsed |
| `both` | `none` or `from_only` | The **stop search**. The parse was fine, the lookup failed |
| `destination_only` | `to_only` | Working as designed, origin came off the ladder |

One is prompt and model work, the other is ranker work, and they go to different fixes. Today
both arrive as `UNRESOLVED` with nothing to tell them apart.

### 2. The resolver already knows why it failed, and throws it away

**Not built yet, and worth building.** A `candidatesRejected` count would report the stop
candidates that `StopSearchTextResolver`'s word-boundary guard discarded. Zero rejected means nothing looked
close and the ranker never saw a plausible match. Twelve rejected means the guard killed matches
a rider would have accepted, which is the guard being too strict on real input.

Those are opposite bugs producing the same rider experience and the same event today. Neither
number is the rider's words; both are facts about our own ranker.

This is **not** the discarded `FuzzyStopRanker` score, which `AI_SEARCH_UX.md` argues at length
is the wrong signal for *disambiguation*. It is a count, used for *diagnosis*, which is a
different job and does not reintroduce the bug that guard exists to prevent.

### 3. The recovery join: get the answer without the question

**The strongest one, and it needs no app change at all.**

A rider whose sentence fails usually goes on to find the stop the ordinary way, in the same
sitting. So:

```
ask_krail_attempt (reason = STOP_NOT_FOUND)
   then, same ga_session_id, seconds later
stop_selected (stopId = ...)
```

The second row names the stop the rider wanted. **That is the eval case.** We never learn what
they typed, and we do not need to: knowing that Ask KRAIL fails for riders who end up at a
particular stop is enough to go and type sentences about that stop by hand and fix it.

`ga_session_id` and `event_timestamp` are on every row Firebase exports already, so this is a
BigQuery query pattern rather than instrumentation. Cost: writing the query down once.

It also measures something no corpus would: **whether riders recover at all.** A failure they
route around is a different problem from one that ends the session.

### 4. On-device capture for the cases that still need words

Some failures will resist all of the above. Those are worth reading as text, and the right place
is the device they happened on, not an export: a "recent Ask KRAIL failures" list behind debug
settings, read by hand, promoted to `FuzzyStopSearchEvalTest` cases one at a time.

Small sample, high quality, no egress, and it is the same loop that already builds the eval
corpus. It works because QA here is done by a person who can read their own device.

### 5. Opt-in, if 1 to 4 prove insufficient

On the roadmap, not in this plan. Scoped in §6.

### What stays unknowable, stated honestly

`NO_PLACE_MENTIONED` is the weakest case for these substitutes. A rider typing "when is the next
train" named no place, and the shape params say only that. What phrasings riders expect this
surface to understand is genuinely lost without text, and §5.3 does not help because there is no
recovery stop to join to.

That is the real cost of the rule, and it is worth naming rather than claiming full coverage.
The mitigation is §5.4, and the judgement is that a category of sentence being under-measured is
a smaller harm than a corpus of riders' journeys sitting in a third party's warehouse.

---

## 6. Roadmap: opt-in sentence capture

Not in this plan. Recorded so the shape is decided before anyone builds it under pressure, and
because §5 only justifies refusing the text if there is a route to it when the substitutes run
out.

**Trigger for building it:** §5.1 to §5.4 shipped, and a failure class still unexplained after a
full release of data. Most likely `NO_PLACE_MENTIONED`, which §5 names as the weakest covered
case.

### The shape, if built

**Per-failure, with the text on screen.** After an attempt fails, the rider is offered "send
this sentence so we can fix it?" with the exact characters they typed visible and a send button.
Not a settings toggle.

The difference is not legal formality. A toggle is given once and applied to thousands of later
sentences the rider never sees, which is consent in name and not in substance. Showing the text
at the moment of sending means the rider reads it first and declines the ones that name their
mother's house. **The rider is a better redactor than any rule we can write**, and this is the
only design that lets them do that job.

### What it costs, so the trade is visible

| Area | Change |
|---|---|
| Store declarations | KRAIL starts collecting **User Content**, a data type it does not collect today. Both forms change, and the Play listing shows it |
| Privacy policy | A new disclosure. Today's policy covers masked search text, not whole sentences |
| Retention | Needs an answer, and "forever in BigQuery" is not one |
| Destination | The sentence would sit in the same third-party warehouse as everything else. A first-party endpoint is a bigger change and a better one |
| Reversibility | None. A sentence sent cannot be recalled, and §2's "no server of ours in between" applies here too |

### The cheaper version worth trying first

Ship the send button in **debug builds and TestFlight only**. Beta riders are self-selected, the
volume is small, and the eval corpus needs quality rather than quantity. If that produces enough
cases, the public version never has to be built, which is the best outcome available.

---

## 7. No third event. Extend what exists

### Availability, and why it needs both halves

- **User property `ai_way_in_available`.** Three of 25 user-scoped dimensions are in use.
  Attaches to every subsequent event, which is what makes segmentation cheap.
- **`aiCapability` param on `app_start`.** `flag on/off`, `device capable/not`, and the
  unavailability reason (downloadable, downloading, unsupported, off in settings).

Both, and **the `app_start` param is the load-bearing one**. A GA4 user property is
current-state and overwrites: when a device becomes capable later, the property flips and the
fact that it was unavailable at the time of an earlier attempt is gone. The per-`app_start`
param preserves it as history. If something has to give, protect the param.

### Conversion

Add `askSessionId` **and** `handoffKept` to the existing `load_timetable_click`. No new event
name. `handoffKept`'s reasoning is in §4; it lives here because Search press is the moment where
what the AI wrote and what is actually loading are both in hand.

---

## 8. The question the feature's existence rests on

Nobody has asked it yet, and it should be designed for now rather than discovered later: **did a
sentence actually save anyone anything against the ordinary From/To row?**

Both halves are already available if this plan ships:

| Path | Funnel |
|---|---|
| Ask KRAIL | `ask_krail_status(opened)`, `ask_krail_status(handoff_settled)`, `load_timetable_click` joined on `askSessionId` |
| Ordinary search | `search_stop_query`, `stop_selected`, `load_timetable_click` joined on `searchSessionId`, live since 2026-08-09 |

Time to timetable, compared. Written down here because if it is not, the two halves get built
and never joined.

---

## 9. GA4 registration, and why the dimension cap is smaller than it looks

The app emits more distinct param names than a GA4 property can register, and has for a while.
The important correction, from the analytics side:

**Unregistered does not mean unavailable.** An unregistered param lands in the BigQuery export
in full. KRAIL-Analytics reads the BigQuery export, not GA4 dimensions. Registration buys
visibility in
the GA4 console and nothing else, so the cap constrains what the console can chart, not what can
be answered.

**Unregistered is not the same as rejected, and rejection is the one that hurts.** Firebase has
its own hard limits, separate from the GA4 dimension cap: a per-event parameter ceiling, and
name and value length limits. A param that breaches one of those is dropped *before* it reaches
BigQuery, so it is available nowhere, silently, while the event keeps firing and looks healthy.
That is what the incident below was, and it is why "BigQuery has everything" must not be quoted
as cover for adding a twelfth, thirteenth and twentieth param to an event. At eleven,
`ask_krail_attempt` is comfortably clear of the ceiling. That is a fact about this plan, not a
general licence.

**Register four params**, the minimum for someone to answer "is it working" without BigQuery:

`reason`, `endsResolved`, `inputMode`, `action`

**BigQuery-only, unregistered:** `askSessionId`, `extractMs`, `attemptIndex`, `originSource`,
`hadTime`, `handoffKept`, `field`, `phase`.

### The precedent this is budgeted against is not hypothetical

`fromStopId` and `toStopId` were rejected by Firebase on `load_timetable_click` for roughly a
month between mid-July and mid-August (`firebase_error=4`, `error_value` naming each param).
The params silently did not arrive while the event kept firing and looked healthy. It is clean
now. It happened on the exact event this plan proposes to extend, which is the reason for
keeping `ask_krail_attempt` at eleven params rather than letting it grow.

---

## 10. The alert summary surface, and the free win

`alerts/summary/` has the same hole: the on-device AI summary card and its vote UI have no
events at all, and the votes currently go nowhere measurable.

**Separate event names** (`ask_krail_*` and `alert_summary_*`), not a shared
`ai_<surface>_status(source, action)`. A `source` param would spend a dimension on every row to
distinguish two surfaces that will almost never be queried together, and grouping is the
analytics side's job anyway: their registry groups by metric rather than by name prefix, so
rolling both into one "on-device AI" metric later needs no app change.

**Shared param names and enum values wherever they mean the same thing.** GA4 registers an
event-scoped dimension per *parameter name*, and one registration covers that name across every
event that sends it, so reusing `reason`, `action` and `inputMode` on the alert summary surface
costs zero additional dimensions. Shared vocabulary is free; divergent vocabulary is what costs,
and it costs permanently: two names for one concept means two code paths in every derivation,
forever, and the divergence is invisible until someone writes the second one.

---

## 11. Cut, and why

**`lengthBucket`.** Removed from the plan.

KRAIL-Analytics already has the analogue on the search side (`queryLength`, banded downstream)
and across everything built on it, it earns its place in exactly one derivation, where it stands
in for API cost on the address pipeline. Ask KRAIL has no per-character cost, so the band has no
decision attached to it. It also correlates with `reason` and `inputMode`, which means it would
look explanatory while adding nothing.

The test it fails: *what would be done differently if long sentences failed more often?* The
answer is the same as if they failed at the same rate, so it is not a measurement.

---

## 12. Order of work

1. Availability first: the user property and the `app_start` param, in **1.28**. The flag went
   on in 1.27 (§1), so this is no longer "before the flip". It is the earliest point a
   denominator can start at all, and every release it slips is another dark period.

   What that buys, stated precisely, because "it cannot be backfilled" is stronger than the
   truth and the imprecision matters if anyone is ever under pressure to flip early:

   - **Device capability is partly reconstructible after the fact.** `deviceModel`, `osVersion`
     and `platformType` are already on every event. To whatever extent "can this device run the
     model" is a function of those, it can be rebuilt retroactively across all history.
   - **Rider state never is.** Whether the setting was switched off, whether the model was still
     downloading, what the flag was: none of it derives from anything already collected, and no
     later release recovers it for earlier rows.

   So what is actually lost if this step slips is the ability to tell **"the device could not"**
   from **"the rider switched it off"**. That is the distinction deciding whether the response is
   engineering work (support more devices) or product work (find out why people turn it off),
   and losing it makes the first month's most actionable question permanently unanswerable for
   that period.
2. `ask_krail_attempt`, promoting `logOutcome()`.
3. `ask_krail_status`, including `handoff_settled` as a bare denominator.
4. `askSessionId` and `handoffKept` on `load_timetable_click`. Steps 3 and 4 are one metric
   split across two events and are worth shipping together; either alone measures nothing.
5. Alert summary, reusing the vocabulary from step 2 and 3.

---

## 13. The registry handoff, settled

`docs/ANALYTICS_REGISTRY_HANDOFF.md` said one thing and `docs/ANALYTICS_EVENTS.md` said the
opposite. Settled with the KRAIL-Analytics side on 2026-09-22, because they own the far end of
the handshake and only they can see what it actually consumes.

**Both docs are wrong, each in half.**

### What actually gates a release, and it is not the ledger

KRAIL-Analytics reads `AnalyticsEvent.kt` **at the latest published release tag** and builds its
own registry. A check on that side compares those event names against that registry and fails
its build when a shipped event has no label. That is the gate, and a ledger row does not satisfy
it: **a shipped event needs a label on the analytics side, not a row here.**

A separate check is the only thing that reads this ledger, and it is report-only, never fatal.
So the ledger is not enforcement. It is a **notification channel**: the way KRAIL tells the analytics
side what is staged before it reaches a release, and that check turns it into a working queue.

### The auto-flip bot never ran

CLAUDE.md says new-event rows "flip to `Registered` automatically". They never have.

The credential the dispatching side needed was never created, and its script treats a missing
one as a dry run and exits 0, so the workflow went green while sending nothing. Verified from this side: `analytics-registry-sync.yml` has exactly **one** run ever,
`2026-08-09T04:29:11Z`, the day it was built, and **no** PR labelled `analytics-sync` has ever
existed. Every row in the ledger was flipped by hand.

Recorded because the reasoning that got this wrong was otherwise sound. The 2026-08-09 commits
building the bot, with an auto-merge exception and a `validate_flip_diff.py` guard, look exactly
like a handshake being revived two weeks after `ANALYTICS_EVENTS.md` declared it dead. Nobody
builds that for a dead file. They built it, smoke-tested it once, and it never fired. **Intent
in the git history is not evidence of a working mechanism**; the run history is.

The bot is being retired rather than fixed. It needed a fine-grained PAT with `Contents: write`
on public KRAIL, held in another repo's secrets, because GitHub has no dispatch-only permission,
and all it bought was one status cell updating itself. Check 6 already surfaces the chore.

### What the Ask KRAIL PRs do

Rows for all of it, in the same PR, `Status = Pending`. None of it is a gate.

| Change | Row | Why |
|---|---|---|
| `ask_krail_attempt`, `ask_krail_status` | Yes | Check 6 puts a new name in front of the analytics side as "needs a label here" before it reaches a release |
| `aiCapability` on `app_start`, `askSessionId` on `load_timetable_click` | Yes, and these matter **more** | A new event announces itself by arriving. A changed shape on an existing event does not, which is what bit `search_stop_query` twice |
| `ai_way_in_available` user property | Yes, marked `Documented` | No per-item registry surface on the analytics side |

Then ping the analytics side when the release is close, so the labels exist before it publishes.
That, not the rows, is what keeps their build green.

**The user property needs no registration to be usable.** `AnalyticsUserProperty.kt` says user
properties must be registered as a custom dimension before they appear in reports. True of the
GA4 console, false of BigQuery, exactly the same split as event params in §9:
`device_form_factor`, `pane_mode` and `window_width_class` are arriving in the export and being
read unregistered today. Register `ai_way_in_available` only if a human needs to segment on it
in the console.

---

## 14. Docs to change in the same PR

- **`docs/SEARCH_QUERY_TELEMETRY_SPEC.md`** currently states that the AI path's "outcome logging
  is local only, never analytics". Half of that stops being true. **Mark the line superseded
  with a date rather than deleting it**: someone who remembers the old stance needs to be able
  to tell whether the privacy position changed or was always this, and a deleted line leaves
  them guessing. The no-text half is unchanged and should be restated pointing here.
- **`feature/trip-planner/ui/AI_SEARCH_UX.md`**, failure mode 7, gains the `handoffKept`
  denominator as the closest thing to a test that exists for it.
- **`docs/STORE_DATA_DECLARATIONS.md`**: the microphone answer expires the day
  `ai_search_input_enabled` is turned on, which step 1 of §12 now brings forward.
- **`docs/ANALYTICS_EVENTS.md`**: event count in the budget section, and the "historical audit
  trail" claim, which §13 corrects. What it says about KRAIL having no contract file, no per-PR
  analytics test and no registration step is still true and should stay.
- **`CLAUDE.md`**: the "flip to `Registered` automatically" claim, which §13 corrects. Its
  instruction to add a row is right.

Agreed wording for both corrections, to be adjusted to each doc's voice:

> `docs/ANALYTICS_REGISTRY_HANDOFF.md` is how KRAIL tells KRAIL-Analytics what is coming. Add a
> row with Status `Pending` in the same PR for a new event name, a new or changed param on an
> existing event, or a new user property. It is **not a gate**: nothing in KRAIL blocks on it,
> and nothing in KRAIL-Analytics fails because of it. Enforcement lives on the analytics side,
> in a check that compares `AnalyticsEvent.kt` **at the latest published release tag** against
> that repo's own registry, so what actually matters is that a shipped event has a label there,
> not that a row exists here. Statuses are maintained by hand: new-event rows
> become `Registered` once the analytics side labels the event; param and user-property rows are
> marked `Documented` once their shape is final.

---

## 15. Open

- Making the suggestion line tappable is a product change, not an analytics one, so it is not in
  this plan. Worth noting that while it stays display-only, the situation table in
  `AiSuggestionSituations.kt` (the hour bands, the weekend rule, the Sunday 15:00 exception)
  stays unfalsifiable: nothing a rider does can tell us whether any of it is right.
