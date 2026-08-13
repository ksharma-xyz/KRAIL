# On-device AI alert summary

Documents the AI summary card on `ServiceAlertScreen`: what gates it, what it renders, and
why every failure path looks identical to the feature not existing.

**One aggregate card, not one per alert.** `CollapsibleAlert.kt` is untouched — no per-alert
AI wiring. A single card summarizes every active alert for the trip together, sitting at
the top of the Service Alerts sheet, above the (unmodified) list of individual alert cards.
This shape was chosen over "card on the Timetable screen" (bigger blast radius, needs
`TimeTableViewModel` changes) and "card per alert" (2N cards on a multi-alert trip).

## Classes

| Class | Module | Responsibility |
|---|---|---|
| `AiTextService` | `:core:ai-text` (commonMain) | `checkAvailability()` / `summarize(text)`. `expect`/`actual`: Foundation Models on iOS, ML Kit GenAI (Gemini Nano) on Android. Never throws across the boundary. |
| `AlertSummaryViewModel` | `:feature:trip-planner:ui`, `alerts/summary/` | Owns the request lifecycle and the vote feedback loop for the whole alert set. Scoped to the alerts sheet only; `TimeTableViewModel` is untouched. |
| `AlertSummaryUiState` | same | Sealed: `Generating` (model running) / `Resolved(text, vote)`. `null` at the call site — not a third case — means render nothing. |
| `AlertSummaryEvent` | same | `SummaryRequested(alerts)` / `VoteClicked(vote)` — the only way `ServiceAlertScreen` talks to the ViewModel. No Koin in the composable. |
| `AiAlertSummaryCard` | same | The card itself: wheel mark + label, skeleton lines while `Generating`, summary text + vote row while `Resolved`. |
| `AiWheelMark`, `Modifier.aiGradientBorder` | `:taj` | KRAIL's on-device-AI mark (a wheel, not a sparkle) and its matching rotating gradient border. Reusable for any future AI surface, not alert-specific. |
| `AlertFeedbackVote` | `:taj` | Generic thumbs-up/down component; not alert-specific either. |

## Gating: three independent switches, one outcome

1. **Remote Config kill switch** — `FlagKeys.ALERT_SUMMARY_ENABLED`, default `false`.
   `AlertSummaryViewModel.requestSummary` checks this before ever touching
   `AiTextService`. Overridable per-build from Debug Config -> Alert Summary in debug
   builds (`DebugNetworkConfigStore.state.value.alertSummaryEnabled`), live — flipping it
   while the sheet is already open takes effect immediately.
2. **Device capability** — `AiTextService.checkAvailability()`. Hardware-gated (Gemini
   Nano tier only on Android) and model-download-gated (downloaded on demand, not
   bundled). Checked per request, not cached at app start, since a download can
   complete mid-session.
3. **Call outcome** — `summarize()` returns `String?`. A guardrail rejection, timeout,
   or SDK error all collapse to `null`.

The flag and device-capability checks collapse to the same UI outcome: **no card at all**
(`ServiceAlertScreen`'s `alertSummaryState` stays `null`). A failed call after the model
was confirmed available also resets state back to `null` — the brief `Generating` skeleton
simply disappears rather than being replaced by an error. There is nothing for a rider to
see as broken in any of these cases.

## Animation: settle, never swap

Both `AiWheelMark` and `Modifier.aiGradientBorder` share one rotation driver
(`rememberAiSpinAngle` in `:taj`, `taj/animations/AiSpinAnimation.kt`) so the wheel and the
card border always move in the same rhythm:

- **Generating** — constant slow rotation (4.5s/turn).
- **Resolved** — the *same* gradient decelerates to a full stop (linear for ~55% of the
  transition, then an ease-out curve) rather than being swapped for a different gradient
  or cut instantly. An earlier version did the latter and it read as "the color abruptly
  disappearing" — see the design notes referenced from project memory for the full
  before/after reasoning.

## Cache key

`ServiceAlert` has no server-provided id. `ImmutableSet<ServiceAlert>.summaryCacheKey()`
sorts and hashes every alert's `heading + message` in the set — stable regardless of
iteration order, adequate since alert content itself is stable once TfNSW publishes it.
Used both to dedupe repeat requests for the same alert set and as the `alertSetKey`
analytics param (never the alert text itself).

## Vote

At most one vote per summary; tapping an already-selected choice is a no-op
(`AlertSummaryViewModel.onVoteClicked`). No standalone "thumbs down" icon exists in
`material-icons-core`; `AlertFeedbackVote` mirrors the thumbs-up icon on both axes rather
than shipping a second vector asset.

## Fixed: summary quality on multi-alert trips

First on-device pass (`requestSummary` in `AlertSummaryViewModel.kt`) joined every alert's
`heading + message` with a blank line and handed the whole block to `summarize()` as one
prompt. On a 4-alert trip this produced a summary that only reflected alert 1 and dropped
the other three entirely — neither platform's summarizer is instructed to cover N
independent items in one call, so it just summarized the block it could best make sense of.

Fixed by calling `summarize()` once per alert instead of once for the whole joined block
(`AlertSummaryViewModel.summarizeAlerts`) — each call only ever has one alert to summarize,
the exact shape both platforms' single-alert instructions already handle correctly. Results
join as a bulleted list (`"• $summary"` per line) rather than prose covering all alerts; a
single-alert trip keeps the original one-paragraph shape. A partial result (some alerts
summarized, some didn't) still renders — only a total failure (every alert failed) falls
back to no card, same as any other failure mode.
