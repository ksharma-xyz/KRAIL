# Store data declarations

What KRAIL declares on the Google Play Data safety form and the App Store Connect App
Privacy form, and **why each answer follows from the code**.

The answers themselves are public and take a minute to read off either console. The
derivation is not, and it is the half that goes stale silently. A year from now the
question is never "what does the form say", it is "why does the form say that, and is it
still true".

## How to use this file

**Any PR that changes what leaves the device updates this file in the same PR.** That means
a new SDK, a new upstream API, a new permission, a new analytics parameter carrying a new
*kind* of value, or a feature flag that unlocks a data type the app did not previously send.

Adding an event that carries an existing kind of value does not. That case is already
covered by `docs/ANALYTICS_REGISTRY_HANDOFF.md`, and the two files answer different
questions: the ledger tracks event names and parameters, this one tracks data *categories*
a store makes you name.

If a change here means a declaration is now wrong, the form has to be resubmitted **before
the build ships**, not after. Play's resubmission triggers a review of unknown length; see
"The gate" below.

## What the app sends

Every row in both tables below traces back to this list. Nothing else leaves the device.

| What | Where it goes | Notes |
|---|---|---|
| Screen views, feature interactions, stop identifiers, device and window metrics, network outcomes | Firebase Analytics | Keyed to a random per-install identifier. There is no account, so no name, email or phone number exists to collect. |
| Stop search query text | Firebase Analytics | Every digit masked to `#` on the device before the event is sent, trimmed, dropped above 40 characters. An all-digit query passes through, because a route number or stop ID identifies no home. See `docs/SEARCH_QUERY_TELEMETRY_SPEC.md`. |
| Crash traces with device model and OS version | Firebase Crashlytics | |
| App start and network timings | Firebase Performance | |
| Trip, departure, stop and Park &amp; Ride requests | NSW Transport API | Stop identifiers and times. No coordinate is ever sent: `StopType.COORD` exists in the model but is never used as a trip origin. |
| Map tile requests | `tiles.openfreemap.org` | Carries the viewport being drawn. This is the **only** thing in the app that reveals anything positional, and it is why location is declared. |
| Spoken audio, while the rider holds the mic in stop search | The platform speech recogniser, Google's on Android and Apple's on iOS | KRAIL never receives, stores or forwards the audio. It hands the microphone to the platform and gets text back. Both platforms prefer on-device transcription and fall back to the vendor's servers when no local model is installed, which is the case where the audio leaves the phone. |

Read on the device and never transmitted:

- **Location.** Used to place the rider on the map and to find nearby stops. Nearby stops
  resolve against the bundled stop database in `RealNearbyStopsRepository`, which is a local
  SQLite query, not a network call. No analytics event carries a latitude or longitude.
- **Saved trips, stop labels, Park &amp; Ride selections, recent searches, preferences.**
  Local database only.
Microphone used to sit in this list. It does not any more: `ai_search_input_enabled` was
turned on for 1.27, so voice input reaches riders and the audio can leave the device. See
judgement call 2.

Not collected at all:

- **Advertising ID.** Android removes the `AD_ID` permission with `tools:node="remove"` and
  sets `google_analytics_adid_collection_enabled=false`. iOS links
  `FirebaseAnalyticsWithoutAdIdSupport` and declares no `NSUserTrackingUsageDescription`, so
  there is no IDFA to read.

  **The Play Console declaration nonetheless answers Yes**, with purpose Analytics only and
  "turn off release errors" ticked. That question asks about imported SDKs, not about what
  the app reads, and four GMS libraries declare the permission in their own manifests:
  `play-services-measurement-api`, `play-services-measurement-impl`,
  `play-services-ads-identifier` and `play-services-measurement-sdk-api`. Answering No while
  those are on the classpath is what blocked every 1.27 upload for a day. With the permission
  absent the identifier reads back as zeroes, so the behaviour above is unchanged; the
  declaration simply over-reports, which is the safe direction.

## Google Play, Data safety

**Nothing is shared.** Firebase is a processor acting for us, not a recipient. Nothing is
sold, and there is no ad network or data broker in the app. Voice audio is declared collected
but not shared; judgement call 2 has the reasoning, because it is the one row where that
distinction took an argument.

| Category and type | Purpose | Required | Why it is declared |
|---|---|---|---|
| App activity / In-app search history | Analytics | Required | The masked query text and whether it found results. |
| App activity / App interactions | Analytics | Required | Screen views and feature taps. |
| App info and performance / Crash logs | Analytics | Required | Crashlytics. |
| App info and performance / Diagnostics | Analytics | Required | Firebase Performance. |
| Device or other IDs | Analytics | Required | The Firebase app-instance ID. Random, and we never read it, but it is an identifier and it leaves the device. Under-declaring identifiers is the most common cause of a Data safety rejection. |
| Location / Approximate | App functionality | Optional | The map tile request. See judgement call 1. |
| Location / Precise | App functionality | Optional | Same. At close zoom the viewport is a small box, so claiming only Approximate would be the under-report. |
| Audio / Voice or sound recordings | App functionality | Optional | Only while the rider is speaking into stop search, and only reaching the platform recogniser. Answered **processed ephemerally**: the audio is streamed, transcribed and discarded, and nothing is written anywhere. Ephemeral rows are disclosed but do not appear on the public store listing. See judgement call 2. |

Everything else is answered No, and the reasoning is worth keeping because the form asks
about all of it every time:

| Not declared | Because |
|---|---|
| Personal info | No account. No name, email, phone number or address exists anywhere in the app. |
| Financial info | No payments. |
| Health and fitness | Nothing read. |
| Messages, Photos and videos, Files and docs, Calendar, Contacts | No permission requested, nothing read. |
| App activity / Installed apps | Not queried. |
| Web browsing history | Stop search is App activity. There is no browser in the app. |

Security practices:

| Question | Answer | Why |
|---|---|---|
| Encrypted in transit | Yes | Firebase, the NSW API and the tile host are all HTTPS. |
| Users can request deletion | No | See judgement call 3. |
| Play Families policy | No | KRAIL is not in the Families programme. |

## App Store Connect, App Privacy

All nine types land under **Data Not Linked to You**, and there is no *Data Used to Track
You* section. That grouping is the fastest proof that every "linked to identity" answer is
No and every "used for tracking" answer is No.

| Data type | Purposes |
|---|---|
| Search History | App Functionality, Analytics |
| Usage Data / Product Interaction | Analytics, App Functionality |
| Diagnostics / Crash Data | App Functionality |
| Diagnostics / Performance Data | Analytics, App Functionality |
| Diagnostics / Other Diagnostic Data | Analytics, App Functionality |
| Identifiers / Device ID | Analytics, App Functionality |
| Location / Precise | App Functionality |
| Location / Coarse | App Functionality |
| User Content / Audio Data | App Functionality |

**User Privacy Choices URL is deliberately blank.** It is Apple's field for a page where
people manage or delete their data, and it is the counterpart of answering No to deletion on
Play. Filling one in without the other would make the two stores disagree.

Apple lists a purpose or two more than Play does on some rows. That direction is harmless: an
extra purpose over-reports rather than under-reports. Do not close the gap by *removing* a
purpose from Apple.

## Judgement calls

These are the three answers that are not lookups. Each one has a trigger that should send
someone back to the form.

### 1. Location is declared because of map tiles, and nothing else

No analytics event carries a coordinate, nearby stops are a local query, and no coordinate
reaches the NSW API. The only positional thing that leaves is the MapLibre tile request,
which names the tiles being drawn. When the rider has centred the map on themselves, that is
a small box around them.

Both stores define collection as transmission off the device, so this is a genuine
collection and both Approximate and Precise are declared. Un-declaring would be the riskier
direction to move in: a declaration that under-reports is a policy violation, one that
over-reports is only a scarier label.

**Revisit when:** anything sends a coordinate upstream. A "stops near me" search hitting
`stop_finder` with `type_sf=coord`, or the BFF live-tracking path carrying a position, would
each turn this from a tile-drawing artefact into a real location feature, and the
"App functionality" purpose would stop covering it on its own.

### 2. Voice input is declared because the audio can leave the phone

`ai_search_input_enabled` was turned on for 1.27, so this stopped being hypothetical. The
microphone answer that used to read "not collected" was true only while the flag was off.

**KRAIL itself never receives the audio.** It hands the microphone to the platform
recogniser and gets text back, and the transcribed words land in the search field where the
existing masked `search_stop_query` rules apply. Nothing records, stores or uploads a
recording to us.

That is still **collection** under both stores' definitions, because the audio leaves the
device. Play's own wording is explicit that ephemeral processing counts.

**It is declared collected but not shared,** and that answer took an argument. Sharing covers
a transfer to a third party, on the device or off it, and read literally the platform
recogniser is a third party. Two things pull the other way and decided it: the transfer only
happens because the rider pressed the mic, which is the user-initiated action Play's own
exemptions describe, and on Android the handoff is to a system speech service on the same
device rather than to a recipient we chose. It is also answered **processed ephemerally**,
since the audio is streamed, transcribed and discarded with nothing written anywhere.

This is the one row in this file where the safer-looking answer was not taken, so it is worth
knowing it was a decision rather than an oversight.

**Both platforms prefer on-device transcription and fall back.** Android passes
`EXTRA_PREFER_OFFLINE`, which is a preference the system may ignore silently. iOS sets
`requiresOnDeviceRecognition = true` only when `recognizer.supportsOnDeviceRecognition()`
reports a local model, because that flag is a hard requirement there and would fail the
session outright on a device without one. So on a device with no local model, audio goes to
the vendor's servers on either platform.

**The open decision:** requiring on-device recognition instead of preferring it would keep
every recording on the phone, which would retire the collected answer entirely rather than
merely justify it. The cost is that voice search stops working on devices with no local model
instead of degrading. That trade has not been made; it is a product call, not a documentation
one.

**Revisit when:** the flag is turned off again, in which case both audio declarations can come
back out; or the app starts sending audio anywhere of its own accord, such as a server-side
transcriber. That last one would make the audio ours and change the recipient from a system
service the rider invoked to one we chose, which is exactly what the not-shared answer rests
on.

### 3. Deletion is answered No because there is nothing to delete against

There is no account, no name, no email. Analytics is keyed to a random per-install
identifier that the app never reads and never stores, and that nothing can map back to a
person. A request to delete names nothing that could be looked up.

This is permitted rather than merely honest: the mandatory deletion route attaches to apps
that let people create an account, and KRAIL has none, so the deletion URL is optional.

**Revisit when:** the app gains any account, sign-in, sync or device-pairing feature. The
answer flips the moment a stored row can be traced to a person. A "reset analytics
identifier" control in Settings, calling Firebase's own reset, would also let the answer
become Yes honestly without an account, since it severs the future stream from the past one.

## The gate

Play's Data safety resubmission triggers a review of unknown length, and search query
collection has no remote kill switch. A release that changes a declaration therefore cannot
roll out until that review clears, so the form goes in **before the release branch is cut**,
not after it. Apple's App Privacy answers publish without a review and independently of a
build, so iOS is correctable after the fact.

## Related

- `docs/SEARCH_QUERY_TELEMETRY_SPEC.md` &mdash; what the app may learn from typed search text, and the masking rule.
- `docs/ANALYTICS_EVENTS.md` &mdash; the event-versus-parameter decision checklist.
- `docs/ANALYTICS_REGISTRY_HANDOFF.md` &mdash; the per-event ledger handed to KRAIL-Analytics.
- The privacy policy at `krail.app/privacy-policy` is the rider-facing counterpart of this
  file. The two describe the same behaviour for different readers and are expected to agree;
  when a declaration here changes, check the policy in the same pass.
