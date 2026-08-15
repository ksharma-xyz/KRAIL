# AI search input — rules, failure modes, and what is actually tested

Covers the "Where are you going?" surface (`components/ai/`) and the pipeline behind it
(`search/ai/`). Read this before changing either.

## The one rule that must not bend

**The model is used to look stops up. Nothing it produces is ever shown to a rider.**

What reaches the screen is a stop from our own database, or words written in this repo. The
model's `originText` / `destinationText` go into `StopTextResolver` and no further.

There is exactly one place model output comes close to the screen: the failure message quotes
the place that matched no stop ("No stop called *Hogwarts*"). That quote is only rendered when
those exact characters appear in what the rider typed, so it is their words being repeated,
never the model's. A reworded or invented name inside quote marks would read as something the
rider said.

Enforced by `a place the model invented is never quoted back at the rider`. If that test is
deleted, this rule is gone with it.

## Failure modes

Every way this can fail, what the rider gets, and whether a test holds it in place.

| # | Failure | Rider sees | Test |
|---|---|---|---|
| 1 | Feature flag off | **Nothing at all.** Continue does nothing | `submit while flag is off does nothing` — the no-op is tested, the dead button is not fixed |
| 2 | Model not downloaded, or downloading | "still downloading… try again in a moment" | `model downloadable lands in DOWNLOADING`, `model downloading lands in DOWNLOADING too` |
| 3 | Device unsupported | Generic unresolved message | `unsupported device lands in UNRESOLVED, not DOWNLOADING` — reads as a parse failure rather than "not available here" |
| 4 | Model returns nothing usable | "did not come through, have another go" | `failed extraction resolves to UNRESOLVED`, `a model that gives nothing back is its own kind of failure` |
| 5 | Parsed, no place named | Example stop offered | `a sentence with no place in it says so`, `a sentence about nothing does not fill the origin with wherever the rider is` |
| 6 | Place named, no stop matches | Name quoted back when the rider wrote it | `a named place that matches no stop is quoted back` |
| 7 | Place named, matches the **wrong** stop | Field filled, silently. No failure shown | Partly: `an unlabelled word does not become a stop that merely contains it` plus `StopTextResolverTest`. Nothing can test that a confident match is *correct* |
| 8 | Only one end resolves | That field fills, the other is left | `one stop resolving is still a result` |
| 9 | Origin unstated, destination found, GPS off or denied | From left empty | `nearby-stop resolver failure leaves origin unresolved, not a crash` |
| 10 | Microphone denied, blocked, unsupported, or the recogniser errors | A different message for each; a blocked mic opens Settings | `speech unavailable surfaces the reason`, `a recogniser error is reported as itself` |
| 11 | Recogniser never reports an end | Stops itself at 20s | `listening stops itself once it hits the ceiling` |
| 12 | Time understood ("by 6pm") | **Dropped.** Parsed, then thrown away | `resolves a time intent into the confirm state` proves we parse it. Nothing covers it being lost, because it is lost outside this ViewModel |
| 13 | Dismissed mid-flight | Mic stopped, draft discarded | `closing the box while listening stops the mic`, `closing the box throws the draft away` |

Two of these are open defects rather than behaviour:

- **#1** is a dead button. The flag reaches the ViewModel but not the UI, so the entry point
  shows and does nothing when the feature is off. It should hide the wheel instead.
- **#12** is worse than not parsing at all: `navigateToTimeTable` has no date/time parameter, so
  a rider who says "by 6pm" is understood and then ignored. Fixing it means a route parameter.

## Parked, deliberately

**Offer a choice when confidence is low.** #7 is the only failure with no visible failure, and
the honest answer to a weak match is to ask rather than commit: two or three candidates with
the reason each was suggested (your label, a saved trip, a name that looks similar), and no
auto-fill below the bar. `FuzzyStopRanker` already computes the score this needs and discards
it at the interface boundary, so the groundwork is a scored result type rather than new
matching. Not being built today; this note is the record of why it should be.
