# In-app review: trigger placement and timing

Working notes for the in-app review request (issue #1739, built on the user-lifecycle
store from #1742). The plumbing is built and the trigger described in **FINAL DESIGN**
below is now implemented. Read this before enabling the feature or changing the trigger.

## Status

Four branches, stacked, not yet raised as PRs:

| Branch | Contains |
|---|---|
| `user-lifecycle-store` | `:sandook` `UserLifecycleStore`: install date, named counters, migration `12.sqm` |
| `app-review-wrapper` | `:core:app-review` module, `AppReviewRequester`, Android and iOS implementations, `CurrentActivityHolder` |
| `app-review-eligibility` | `RealAppReviewManager` two-ask gates, Remote Config keys, `review_prompt_requested` event |
| `app-review-trigger` | The shared delight-moment trigger wired into `SavedTripsViewModel`, `TimeTableViewModel`, and `AddParkRideViewModel` |

All four compile on Android and iOS, pass detekt, and have green tests. The feature is
inert: `in_app_review_enabled` defaults to `false`.

## FINAL DESIGN (decided 2026-07-24, supersedes the analysis below)

The trigger is settled. The analysis further down records how it was reached; this section
is the spec to build. The next session codes this fresh, reusing the store, the wrapper,
and the gate infrastructure, and deleting the logic listed under "To delete".

**Ask at most twice per user, ever. Then never again.**

Both asks fire on the **same shared set of delight moments**. They differ only by their
gates. This is deliberate: one moment-set and two gate-sets is less code than two separate
triggers, and it means no user is stranded waiting for a specific moment they never reach.

### Delight moments (any one of these)

- **A**: user opened a saved trip, viewed the timetable, and navigated **back** to the
  Saved Trips screen. Prompt fires on arrival at Saved Trips, not on the timetable.
- **B**: user saved a trip and arrived on the Saved Trips screen.
- **C**: user added a Park and Ride facility (the park-ride picker feature, now on `main`)
  and arrived on the Saved Trips screen.
- **D**: user tapped a Park and Ride card, and 3 seconds elapsed (delay so the prompt is
  not in the same frame as the tap).

All four are completed positive actions landing on a calm screen. None is a "rate" button,
none interrupts a pending task.

### Ask 1 (first prompt)

Any delight moment above, when all of these hold:

- `in_app_review_enabled` is on
- onboarding finished (`KEY_HAS_SEEN_INTRO`)
- install age greater than 3 days
- user has 2 or more saved trips
- lifetime asks so far is 0

### Ask 2 (second and final prompt)

Any delight moment above, when both hold:

- at least 5 months since ask 1
- lifetime asks so far is 1

After ask 2, never prompt again. Lifetime cap is 2.

Note the earlier "first save is a bad moment" caveat does **not** apply to moment B here:
by the time B can fire, the user already has 2+ saved trips and (for ask 2) 5+ months of
tenure, so a save is an engaged user's action, not a day-one visitor's.

### Why these numbers

- **Two asks, lifetime**: we cannot detect whether a prompt was shown, dismissed, or
  rated, so we treat every ask identically and simply stop after two well-timed ones. A
  user who has not rated after two good moments will not.
- **5 months**: spaces the two asks to well within Apple's roughly 3-per-year ceiling, so
  we never waste a request against the silent OS throttle.
- **2 saved trips + day 3**: investment plus tenure, so the asker is an engaged user, not
  a day-one visitor who cannot judge the app yet.
- **Completed moments only (moments A to D above)**: never mid-task. A transit user is
  often walking to a platform; interrupting that is how you earn a one-star.
- **Shared moment-set for both asks**: closes the gap where a user who never taps a Park
  and Ride card would otherwise never reach ask 2. Whatever positive moment they hit first
  after the 5-month gate fires it.

### Deleted in the rework (done)

- `SavedTripsViewModel.onSavedTripCardClick` no longer calls the review manager (the tap
  trigger was the wrong moment).
- `SearchStopViewModel`'s `onZeroResultSearch()` call and the whole zero-result suppression
  gate are gone. Search has nothing to do with this feature.
- The `SAVED_TRIP_OPEN` lifecycle counter is removed; ask 1 gates on saved-trip *count*, a
  `SavedTrip` table query (`savedTripCount` provider), not an open counter.
- `IN_APP_REVIEW_MIN_SAVED_TRIP_OPENS` and `IN_APP_REVIEW_COOLDOWN_DAYS` Remote Config keys
  are replaced by `IN_APP_REVIEW_MIN_SAVED_TRIPS` (default 2) and
  `IN_APP_REVIEW_MIN_DAYS_BETWEEN_ASKS` (default 150, about five months).

### Reused as-is

- `UserLifecycleStore` install date and the `REVIEW_PROMPT_REQUESTED` counter: its count is
  the lifetime cap of 2, its last-seen time is the 5-month spacing.
- `AppReviewRequester`, both platform implementations, `CurrentActivityHolder`.
- `RealAppReviewManager` structure and `review_prompt_requested` (new `source` values, one
  per [`DelightMoment`], extend the event rather than minting new names).

## Architecture

All decision logic lives in `:core:app-review`. Feature code calls two methods and knows
no rules, so the ruleset can be replaced without touching any screen.

```
:core:app-review
  AppReviewManager.kt       interface + why-no-dialog rationale
  RealAppReviewManager.kt   the gates
  AppReviewThresholds.kt    Remote Config readers and default values
  AppReviewRequester.kt     platform contract, no logic
  androidMain/iosMain       Play In-App Review, StoreKit
```

Supporting split: `:sandook` owns **state** (what happened), `:core:remote-config` owns
**policy** (the numbers), `:core:app-review` owns **the decision**. Do not add threshold
constants to the store; that is the boundary violation to reject in review.

## Compliance constraints (not negotiable)

Both stores forbid the custom pre-prompt plus sentiment gate pattern:

- **Google Play In-App Review policy**: no custom prompt or question before the API, no
  gating by sentiment, no triggering from a button.
- **Apple HIG (Ratings and reviews) and Guideline 1.1.7**: not in response to a button
  tap, no custom prompt that mimics or precedes the system alert, only after demonstrated
  engagement, never during onboarding.

Consequence: there is no KRAIL-authored dialog anywhere in this feature. The rationale is
duplicated in `AppReviewManager`'s KDoc so a later change has to read it before removing
it.

## Implemented gates

A [`DelightMoment`] arms a request; landing on Saved Trips (`onSavedTripsScreenShown`)
evaluates the gates and, if they pass, fires. The armed moment is consumed on every landing
whether or not it fires, so a not-yet-eligible user simply spends that moment and re-arms on
the next one.

Common gates (both asks):

| Gate | Default | Source |
|---|---|---|
| Feature enabled | `false` | `IN_APP_REVIEW_ENABLED` |
| Onboarding finished | n/a | `KEY_HAS_SEEN_INTRO` |

Then, by how many times the user has been asked (`REVIEW_PROMPT_REQUESTED` count):

| Prior asks | Fires when | Tunable |
|---|---|---|
| 0 (ask 1) | install age ≥ N days **and** saved-trip count ≥ M | `IN_APP_REVIEW_MIN_ACCOUNT_AGE_DAYS` (3), `IN_APP_REVIEW_MIN_SAVED_TRIPS` (2) |
| 1 (ask 2) | ≥ D days since ask 1 | `IN_APP_REVIEW_MIN_DAYS_BETWEEN_ASKS` (150) |
| 2 or more | never | lifetime cap, hardcoded |

The lifetime cap of two is deliberately not a Remote Config dial: it is the premise of the
design (the platform reports no outcome, so we stop after two well-timed asks), not a knob.

## Timing analysis

### Reframe: do not try to detect delight

Delight is not observable. Dwell time is ambiguous: thirty seconds on a timetable means
"reading calmly" or "cannot find my train", and those are indistinguishable from
instrumentation. Guessing wrong produces exactly the low rating the feature is meant to
avoid.

Frustration, by contrast, is precisely observable. So invert the design: **fire on a calm,
neutral moment and suppress hard on any recent friction.**

Friction signals available:

| Signal | Wired up? |
|---|---|
| Zero-result search | yes, gate 6 |
| API error or retry tapped | no |
| Departures failed to load | no |
| Rapid re-search (several queries in a minute) | no |
| Tracking dropped out | no |
| Empty trip results | no |

There is also a domain-specific pressure most apps do not have: transit users are
frequently walking to a platform. Being in a hurry is the default state, not the
exception.

### Candidate moments

| Moment | Intent pending | Likely rushed | Verdict |
|---|---|---|---|
| Saved-trip card tap (current) | yes | yes | worst case |
| 30s dwell on timetable | no | yes | dwell is not delight, likely mid-commute |
| Back from timetable | no | yes | better, but the user is now moving |
| Park and Ride card expand plus 5 to 10s | partly | no | discretionary so unhurried, but rare and mid-decision |
| Return to Saved Trips after a clean timetable view | no | lower | best candidate |
| Cold start, lands on home, idles, does not drill in | no | no | calmest, weakest engagement evidence |
| Just saved a new trip | no | no | investment signal, but often a first-ever action |

### Leading proposal

User opens a saved trip, the timetable loads without error, they dwell past a floor
(around 10s), they navigate back, and the request fires roughly 2s later while they are on
the Saved Trips screen.

Rationale: intent is satisfied, there is evidence the app worked, and the sheet lands on a
calm screen rather than on top of live departure data. The post-navigation delay matters as
much as the placement; never fire in the same frame as a navigation.

Park and Ride expand is a sound secondary trigger later. It is a discretionary action, so
the user is not under time pressure. It is too infrequent to be primary. If added, reuse
`review_prompt_requested` with a different `source` value rather than minting a new event
name (Firebase caps the app at 500 event names, forever).

### This cannot be A/B tested

Neither platform reports whether the sheet appeared or what was submitted. There is no
outcome signal to optimise against. What is actually available:

- Aggregate store rating volume and average from Play Console and App Store Connect, which
  is laggy, noisy, and confounded by releases.
- A proxy: whether the session continued normally after `review_prompt_requested` fired.

Practical consequence: choose placement by principle, roll out slowly behind the flag, and
read aggregate store data over weeks rather than days. Treat it as a design decision, not
an experiment.

## Where the trigger lives

The module boundary keeps each caller to one or two lines:

- `AppReviewManager.onDelightMoment(moment)` arms a request; each [`DelightMoment`] carries
  its own analytics `source`.
- `AppReviewManager.onSavedTripsScreenShown()` evaluates the gates and fires.

Callers:

- `TimeTableViewModel` arms `TIMETABLE_VIEWED` on a clean load and `TRIP_SAVED` on either
  save path (star toggle and the save-trip prompt).
- `AddParkRideViewModel` arms `PARK_RIDE_ADDED` when a facility is added.
- `SavedTripsViewModel` fires on every arrival at the screen (`onSavedTripsScreenShown`), and
  arms + fires `PARK_RIDE_CARD_TAPPED` after a 3-second delay on a Park and Ride card tap.

## Testing on device

The feature is gated, so first relax the gates in Remote Config:

```
in_app_review_enabled                true
in_app_review_min_saved_trips        1
in_app_review_min_account_age_days   0
in_app_review_min_days_between_asks  0
```

A fresh install cannot otherwise trigger it, because the default minimum install age is
three days and the default minimum saved trips is two. Also finish the intro, then hit one
of the delight moments (save a trip, view a timetable and return, add a Park and Ride
facility, or tap a Park and Ride card).

| Build | Behaviour |
|---|---|
| iOS run from Xcode (debug) | StoreKit alert shows every time, no throttle. This is the real visual test. |
| iOS TestFlight | Never shows. Apple disables it there. |
| iOS App Store | Throttled, roughly three times a year. |
| Android sideloaded (`installDebug`) | `requestReviewFlow()` fails because the install did not come from Play. Logs and returns. No card. |
| Android internal testing or internal app sharing | Real card, subject to Play quota. |

Android:

```sh
./gradlew :androidApp:installDebug
adb logcat -c && adb logcat | grep -i AppReview
```

Expect `AppReview: requesting platform review sheet`, which proves every gate passed,
followed by a `requestReviewFlow failed` line, which is normal for a sideloaded build and
is not a defect.

iOS: open `iosApp/iosApp.xcodeproj` and run from Xcode.

An unbuilt convenience: adding review overrides to the existing debug-only
`DebugConfigScreen` would replace the Remote Config round trip with an on-device toggle,
including a bypass-all-gates action.

## Migration numbering

`12.sqm` is used rather than `11.sqm` because a parallel park-ride branch already claims
`11`. The two migrations create different tables, so they are independent and safe in
either order, and `UserLifecycleStoreTest` covers the upgrade path explicitly. If merge
order changes so that this lands first, renumber to `11` rather than leaving a permanent
gap in the sequence.

## Open decisions

1. **Trigger placement is resolved** (FINAL DESIGN above) and implemented. What remains is a
   judgement call on *enabling* it: turn the flag on for a slice, then read aggregate store
   ratings over weeks, since neither platform reports whether a sheet showed.
2. **Settings rows are not built.** Issue #1739 also asks for a user-initiated "Rate KRAIL"
   store deep link and an optional "Send feedback" mailto row. These are allowed because they
   are user-initiated and open the store rather than driving the API.
3. **Debug Config overrides are not built.** On-device toggles to bypass the gates would
   replace the Remote Config round trip during QA.
