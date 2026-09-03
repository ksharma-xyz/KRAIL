# 1.27 — search query telemetry: plan

Target release: **end of September 2026**, Android and iOS together.

Context: the privacy policy at https://krail.app/privacy-policy (updated 2026-08-28)
discloses that search query text is collected to improve search, and promises that house
and unit numbers are masked. 1.27 turns that on.

## The decision, settled

**Digits are masked on the device**, not downstream. `RealAnalytics` logs straight to
Firebase, so whatever the client sends is what a third party stores — a masking step later
in the pipeline would be masking data that had already been sent. So the 1.26 carve-out is
not replaced by "send everything" or kept as a no-digit guard; it is replaced by
`"4 fulton place"` going out as `"# fulton place"`.

The KRAIL-Analytics pull/snapshot masking **stays** as a backstop. 1.26 and earlier are in
the field for months and keep sending under the old rule, and a future call site could
bypass the client masking. Two layers, neither assuming the other ran.

Full reasoning and the rules that follow from it: `docs/SEARCH_QUERY_TELEMETRY_SPEC.md`.

---

## 1. App code — shared, so one change covers both platforms

Everything here is in `commonMain`. Android and iOS run the same masking, the same firing
rule and the same guards; there is no per-platform redaction code to keep in step.

### Done (local branch, not raised)

- [x] `SearchQueryAnalyticsRedaction.maskedQueryOrNull` — trim, drop above 40 chars, mask
      every digit to `#`. Result counts are no longer part of the decision.
- [x] **All-digit queries pass through as typed.** Route numbers and stop IDs are digits
      with no street beside them, so they identify no home, and masking them would erase
      the class of search entirely. One non-digit character anywhere masks the whole query.
- [x] `AnalyticsEvent.SearchStopQuery.zeroResultQuery` renamed `maskedQuery`. The Firebase
      property key stays `query`, so existing dashboards keep reading the same column.
- [x] Text rides the **local** firing only. The address firing carries none — same
      `searchSessionId`, so a join gets it, and duplicating it would double the egress and
      double-count the eval corpus.
- [x] **Text and event fire once per typing burst.** The firing now waits
      `SEARCH_ANALYTICS_QUIET_MS` (600 ms) after results render, and the next keystroke
      cancels it. The list on screen still refreshes at 100 ms.
- [x] Fixed on the way past: `runCatching` around the local search swallowed the
      `CancellationException` thrown when a keystroke cancelled the job, so a cancelled
      keystroke was reported as `isError = true` **and** flashed the error state. Now
      `suspendSafeResult`, which rethrows cancellation.
- [x] Guard tests re-pinned to the new invariant — "no digit ever leaves", which is a
      stronger and simpler property than the old four-condition carve-out:
      `SearchQueryAnalyticsRedactionTest`, `SearchQueryTextEgressTest`,
      `SearchQueryRedactionCallSiteTest`, plus `SearchStopViewModelTest`.
- [x] New test pinning the burst rule: three keystrokes, one reported search.
- [x] `docs/SEARCH_QUERY_TELEMETRY_SPEC.md` rewritten.
- [x] `docs/ANALYTICS_REGISTRY_HANDOFF.md` — three rows added (`Pending`).
- [x] `AnalyticsParamSanitizer` left untouched. Address-ID hashing and param truncation
      stay regardless.

### Still to build

- [ ] **Android: stop collecting the Advertising ID.** `firebase-analytics` collects AAID
      by default and there is no opt-out in `androidApp/src/main/AndroidManifest.xml`:

      ```xml
      <meta-data
          android:name="google_analytics_adid_collection_enabled"
          android:value="false" />
      ```

      This is what lets "Data Not Linked to You" stand without an asterisk, and it lets
      policy section 07 say the Ad ID is not collected rather than that it may be. iOS
      needs nothing — no `NSUserTrackingUsageDescription` in `Info.plist`, so no IDFA.
- [ ] **BigQuery partition expiry**, if the export is linked. Section 04 promises that over
      time only masked text and aggregates are kept; export tables have no expiry by
      default, so that half of the sentence currently has no mechanism behind it.

### Deliberately not in 1.27

- The address gate change from `docs/SEARCH_AND_LABELS_TELEMETRY_FINDINGS.md` item 1
  (digit-bearing queries bypassing the length threshold). The bucket it rests on is
  inflated by keystroke prefixes, and the burst fix above is what makes it measurable.
  **Re-read that bucket on 1.27 data before touching `AddressSearchEligibility`** — a
  large part of it is likely riders typing route numbers (`861`, `T80`), which an address
  geocoder cannot answer either.

---

## 2. Store declarations — the part with external timing

| | Android | iOS |
|---|---|---|
| Form | Play Console → Data safety | App Store Connect → App Privacy |
| Change | Declare Search history: collected, not shared, encrypted in transit, purpose Analytics; deletion request via email (matches policy section 08) | Search History **already ticked** — verify its three follow-up answers |
| Review | **Resubmission triggers a review of unknown length** | Publishes independently of a build, no review |
| Risk | The real gate on the rollout | Low; correctable after the fact |

iOS, the three answers to read back on the Search History type:

- Purposes: **Analytics** + **App Functionality**
- Linked to the user's identity: **No**
- Used for tracking: **No**

Same pass: confirm Usage Data, Crash Data and Performance Data all read Linked = No,
Tracking = No. One of them set to Linked = Yes drags the whole app into "Data Linked to
You" and makes the policy's "random identifier, not your name" read as a contradiction.

**Order that matters:** the policy is live and both forms describe the new behaviour
*before* the build reaches anyone. There is no kill switch, so Play's review is a hard gate
on the rollout rather than something the release can run ahead of — do not start the
rollout until the Data safety form is through.

---

## 3. QA — both platforms, on device

Static checks prove none of this. The proof is Firebase DebugView showing the parameter
arrive in the right shape.

- [ ] Android + iOS: type `4 fulton place`, confirm DebugView shows `query = "# fulton place"`.
- [ ] Both: type a stop name at normal speed, confirm **one** `search_stop_query` row for
      the finished query, not one per keystroke.
- [ ] Both: search a bus route (`861`) and a stop ID, confirm `query` arrives unmasked.
- [ ] Both: search `T80`, confirm it arrives as `T##` (known and accepted).
- [ ] Both: confirm the address firing (`resultSource = address`) carries no `query` at all.
- [ ] Both: search, get an error state, confirm the error row carries the masked query and
      that a merely cancelled keystroke produces no error row.
- [ ] Rotation on SearchStopScreen mid-typing, per the `CLAUDE.md` config-change checklist.
- [ ] `adb logcat` for `FATAL EXCEPTION` after the Android pass.

## 4. Release

- [ ] 1.27 release notes carry one line about search text collection — policy section 09
      now promises this. Rider language, per the `krail-release-notes` skill: what changed
      for them, not "telemetry parameter".
- [ ] iOS: `distribute-testflight.yml`. Android: `distribute-google-play.yml -f track=internal`.
- [ ] Watch the first days of 1.27 data for digit-bearing `query` values. Any at all means
      a call site bypassed the masking on a platform, and the flag goes back off.

## 5. Sequencing to end of September

| Week | Work |
|---|---|
| 1 | Raise the masking + burst stack. Android Ad-ID change as its own PR |
| 2 | Both store forms submitted. Play's review starts here — with no kill switch it gates the rollout |
| 3 | Device QA on both platforms against DebugView; release notes written |
| 4 | 1.27 to TestFlight and internal track; rollout only once the Play form is through |
