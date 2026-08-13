# AI search input mode — exploration + architecture proposal

Status: **build in progress.** UX explored and real components built
(`explore/ai-search-input-mode`, `feat/ai-search-*` component branches); real capability
build started on `feat/ai-text-extract-trip-intent`, stacked on top. `AiTextService
.extractTripIntent()` is real and Android-backed (see below) — speech-to-text, OCR, and the
ViewModel/navigation wiring that actually uses it are not yet built. No PR — waiting on both
the iOS Foundation Models bridge (`docs/investigations/IOS_FOUNDATION_MODELS_BRIDGE.md`) and
enough of the vertical slice to be worth shipping.

## Problem

Today, planning a trip means: tap From, land on `SearchStopScreen`, search/pick a stop,
back out, tap To, repeat, then separately open the date/time sheet and pick
leave-by/arrive-by. It works but is entirely manual field-filling — three separate screens
for one intent ("I want to get from A to B by time T").

The idea explored here: let a rider speak, type freeform text, or paste a screenshot
(calendar invite, email, group chat message) and have KRAIL extract origin, destination,
arrival/departure constraint, transport-mode preference, and anything worth flagging (e.g.
an active alert on the route) — then either fill the existing fields or go straight to
results. This is additive, not a replacement: every precedent researched below layers
natural-language entry *on top of* the existing structured UI rather than replacing it, and
that's the shape proposed here too.

## Real precedent grounding this design

Researched instead of invented (see full findings via the session's research agent
`a64bb9ee940c1d426` if needed later):

- **Listening/thinking/responding is one shape, three motions** — ChatGPT Advanced Voice
  Mode's orb, Gemini Live's amplitude-reactive pill, Perplexity's particle sphere. None of
  them switch shapes between states; the same shape changes *how* it moves. KRAIL already
  has this exact pattern half-built: `rememberAiSpinAngle(spinning)` in
  `taj/animations/AiSpinAnimation.kt` — constant rotation while active, physical
  decelerate-to-stop when done. This proposal reuses that driver rather than inventing a
  second animation system.
- **Extraction always shows a confirm step, never silently commits** — iOS 26's
  screenshot-to-calendar flow surfaces an "Add to Calendar" affordance, then a preview of
  extracted fields, then a final confirm tap. Fantastical fills its event panel live as you
  type but leaves it editable before saving. Nothing found auto-commits extracted data
  without a look-over step — this proposal doesn't either.
- **Natural language is an alternate entry point into the same results surface, not a
  replacement UI** — Google Maps' "Ask Maps," Citymapper's AI journey layer, Hopper's chat
  assist all still land the rider in the same structured results/booking flow every other
  entry point uses. Confirms the plan below: the AI flow's only job is to populate the
  existing `Trip` + `DateTimeSelectionItem` contract and hand off to the existing
  `TimeTableScreen`, not to build a parallel results UI.
- **Chip/card field-preview is the safest confirm pattern, but is genuinely under-precedented**
  — Apple's and Fantastical's editable-field previews are the strongest real examples found;
  a dedicated "AI extracted these chips, tap one to fix it" pattern wasn't found fully
  documented anywhere. Flagged as a first-principles design area below, not a copy job.
- **AI visual identity is usually a separate, consistent gradient layered over the host
  app's own brand color, not a re-tint of it** — Google's Gemini gradient and Microsoft
  Copilot's gradient both stay fixed regardless of which Google/Microsoft product they
  appear in; ChatGPT is the outlier (monochrome, no persistent AI gradient). This directly
  matches the direction requested for KRAIL: a fixed **cool blue-to-pink gradient** as the
  "AI touched this" signal everywhere AI shows up, decoupled from whichever transport-mode
  theme (Train orange / Metro green / Bus blue) the rider has selected for the rest of the
  app. See "Color system" below — this is a proposed *addition* alongside the existing
  `AiGradientTokens` (Train/Yellow/Metro/Bus) used by the alert-summary card, not a
  replacement of it; reconciling the two is an open question, not decided here.

## Color system (proposed)

Two independent color roles, not one:

1. **Theme color** — whatever the rider has already selected (`KrailThemeStyle`: Train
   orange, Metro green, Bus blue, etc.). Used for anything that isn't specifically an "AI
   did this" signal: the mic button's resting state background, selected-chip fill,
   progress/confirmation accents. This is "use the theme color for things that need theme
   color" — most of the surface stays exactly as themed as it is today.
2. **AI cool gradient** (new, proposed) — a fixed blue-to-pink gradient, always the same
   regardless of selected theme, reserved for the moment-to-moment "this is generative AI
   working" signal: the listening/thinking orb motion, the extraction-in-progress border,
   the "AI Summary"-style label treatment. Candidate stops (to validate against KRAIL's
   actual surface tokens before building, not final): a blue in the `#3B6FF0`–`#4A7FFF`
   range through a violet midpoint `#8B5CF6`–`#9B5CF6` to a pink `#EC4899`–`#FF4FA3`. This
   sits alongside the existing `AiGradientTokens` (Train/Yellow/Metro/Bus) used by the
   shipped alert-summary card — whether the two unify into one AI identity or stay separate
   (alert-summary keeps the transport-gradient, this new surface gets the cool gradient) is
   an open question for the UX review, not resolved here.

## Proposed scope: what gets extracted

| Field | Source | Existing contract it maps to |
|---|---|---|
| Origin | speech / text / screenshot | `Trip.fromStopId` / `fromStopName` (needs resolving free text -> a real `StopItem`, see below) |
| Destination | speech / text / screenshot | `Trip.toStopId` / `toStopName` |
| Time constraint (leave-by / arrive-by) | speech / text / screenshot | `DateTimeSelectionItem(option: JourneyTimeOptions, hour, minute, date)` |
| Transport mode preference | speech / text | not currently modeled at the search-entry level — open question, see below |
| Things to watch for (alerts) | cross-referenced against live alert data, not extracted from user input | reuses the `AiTextService`/`AlertSummaryViewModel` pattern already shipped for the Service Alerts sheet |

**Origin/destination is the hard part.** Free text like "Central" or "home" isn't a
`StopItem` — it's a search query. The extraction step can only ever produce candidate
*text*, which then has to run through the exact same stop/address search
`SearchStopViewModel` already does (including the address-search eligibility gate documented
in `feature/trip-planner/ui/ADDRESS_SEARCH_ELIGIBILITY.md`). This proposal does **not** invent
a second stop-resolution path — extracted origin/destination text is handed to the existing
search pipeline and the top result becomes the candidate, shown to the rider as an editable
chip precisely because it's a guess, not a confirmed pick.

## Proposed architecture

### Structured extraction — built, on branch `feat/ai-text-extract-trip-intent`

**Status update:** the paragraph originally here assumed `extractTripIntent` could reuse
`genai-summarization` with "a structured-output style instruction." That assumption was
wrong, confirmed by decompiling the actual jar: `SummarizationRequest.Builder` only accepts
raw text, and `SummarizerOptions.InputType` is a fixed `ARTICLE`/`CONVERSATION` enum — there
is no prompt/instruction parameter anywhere in that module. Real free-form prompting on
Android turned out to live in a *different*, separate ML Kit GenAI module —
`com.google.mlkit:genai-prompt:1.0.0-beta2` (public beta, not allowlisted) — exposing a
genuine `GenerativeModel.generateContent(prompt): GenerateContentResponse` as a plain Kotlin
`suspend fun` (no Guava future, unlike Summarization). No structural guarantee on the
output though — it returns free text the model *should* format as JSON per the prompt's
instructions, but can still wrap in prose or a markdown fence, so the caller defensively
extracts the `{...}` substring and decodes leniently, collapsing any failure to `null`.

`AiTextService` (`core/ai-text/`) now exposes a second method, built and real:

```kotlin
interface AiTextService {
    suspend fun checkAvailability(): AiAvailability
    suspend fun summarize(text: String): String?
    suspend fun extractTripIntent(text: String): TripIntentExtraction?
}

@Serializable
data class TripIntentExtraction(
    val originText: String? = null,
    val destinationText: String? = null,
    val timeIntent: TimeIntent? = null,
    val modeHints: List<String> = emptyList(),
)

@Serializable
data class TimeIntent(val isArrival: Boolean, val timeText: String)
```

Deliberately **not** `JourneyTimeOptions`/`hour`/`minute`/`LocalDate` as first drafted —
`timeText` stays a raw verbatim phrase ("6:30pm", "in twenty minutes"). The model shouldn't
be trusted with date arithmetic (today's date, timezone, "tomorrow" vs "next Tuesday"); that
resolution is deterministic app code the caller (`feature/trip-planner`) already owns via
`DateTimeSelectionItem`, kept out of `core/ai-text` to avoid a core→feature dependency.

**iOS will end up qualitatively *better* than Android here, not just at parity**, once the
Foundation Models bridge exists: `@Generable` + `LanguageModelSession.respond(to:generating:)`
gets constrained decoding — the model literally cannot emit a token outside the declared
schema — where Android's `genai-prompt` only returns free text with no structural guarantee.
Confirmed the bridge design implication too: `@Generable` structs aren't representable across
the `@objc` boundary cinterop needs, so the Swift package must perform the extraction call
*and* flatten the result to plain strings/primitives internally, never expose raw generation
across the boundary — noted in `IosAiTextService.extractTripIntent`'s doc comment.

### New: platform speech-to-text (expect/actual, new module or `core/ai-text` extension)

No speech code exists anywhere in the repo today (confirmed via grep — this is greenfield).
Proposed shape, mirroring `AiTextService`'s pattern:

```kotlin
interface SpeechToTextService {
    suspend fun checkAvailability(): SpeechAvailability
    fun startListening(): Flow<SpeechResult> // partial + final transcripts
    fun stopListening()
}
```

- Android: `SpeechRecognizer` (on-device recognition where available via
  `RecognizerIntent.EXTRA_PREFER_OFFLINE`, falls back to network).
- iOS: `SFSpeechRecognizer` + `AVAudioEngine` for the mic tap.
- Both need a runtime permission flow — `RECORD_AUDIO` on Android,
  `NSSpeechRecognitionUsageDescription` + `NSMicrophoneUsageDescription` on iOS. Given the
  existing `ios_permission_must_request.md` project lesson (KMP permission wrappers must
  call `requestXxxAuthorization()` on `notDetermined`, not just inspect status), this needs
  the same care on the iOS actual.

### New: screenshot/paste-to-text (OCR)

Also greenfield. Proposed: ML Kit Text Recognition v2 on Android (on-device, no extra
download beyond the existing ML Kit GenAI dependency footprint), `VNRecognizeTextRequest`
(Vision framework) on iOS. Output is raw OCR text, which then flows into the same
`extractTripIntent(text)` call as speech/typed input — OCR is just another way to produce
the `String` that extraction consumes, not a separate extraction path.

### UI shape (subject to the HTML mockup / UX review before anything is final)

- Entry point: a themed (not AI-gradient) mic/AI affordance on `SearchStopRow` /
  `SavedTripsBottomSearchRow`, reusing `taj`'s existing button primitives.
- Expands into a sheet (reusing the `ModalBottomSheet` pattern already standard in this
  codebase per `feedback_no_snackbars.md`) with a combined text/voice/paste input.
- Listening/thinking state reuses `rememberAiSpinAngle` — no new animation primitive needed
  for that part.
- Result state: editable chips or a recap-sentence-with-inline-edit (open UX question,
  demoed both ways in the HTML mockup) driving the same `SearchStopUiEvent`/
  `DateTimeSelectionChanged` events the manual flow already fires — this is the part that
  keeps the AI flow from becoming a second, parallel state machine. It just pre-fills the
  existing one.
- Confirm action navigates via the existing `TripPlannerNavigator.navigateToTimeTable(...)`
  + `DateTimeSelectionChanged` pair, exactly as `SavedTripsEntry.triggerTripSearch` does
  today for the manual flow.

## Open questions (deliberately unresolved here)

1. **Chip vs. recap-sentence confirmation** — least-precedented part of the whole flow.
   Needs a UX call, not an engineering one.
2. **One AI gradient or two** — does this new cool blue-pink identity replace the
   transport-color gradient on the already-shipped alert-summary card, or do the two
   coexist (cool gradient for "generative/conversational" surfaces, transport gradient for
   "informational" AI surfaces like the alert summary)?
3. **Mode-preference resolution** — there's no existing "preferred transport modes" concept
   at the search-entry level to map extracted mode hints onto. Either a new lightweight
   filter gets built, or mode preference is dropped from v1 scope.
4. **Where alerts-to-watch-for comes from** — proposed as a live cross-reference against
   already-fetched alert data for the resolved route (reusing the alert-summary pipeline),
   not something extracted from the rider's own input. Needs confirming this is even
   sensible before the origin/destination are resolved (chicken-and-egg: alerts require a
   route, the route is the thing being determined).
5. **Share-sheet / share-target integration** — a rider forwarding a calendar invite or
   email straight to KRAIL from another app is a natural extension of the paste-screenshot
   flow, not scoped here, but flagged as a strong v2 candidate.
6. **Offline/availability gating** — same three-way collapse as the alert-summary feature
   (flag off / device unavailable / call failure all render nothing) presumably applies
   here too, but a failed extraction needs a fallback that isn't "silently do nothing" the
   way alert-summary can get away with — if a rider explicitly opened an AI input sheet and
   spoke, showing nothing back is a much worse experience than a card that just never
   appears. This needs an explicit "couldn't understand that, try the normal search" state
   the alert-summary feature never needed.

## Non-goals for this exploration

- No production code. This branch (`explore/ai-search-input-mode`) holds only this doc and
  the accompanying HTML flow/animation mockup, committed locally.
- No decision yet on which of speech / paste / type ships first, or whether all three ship
  together — that's a scope call for after UX sign-off.
