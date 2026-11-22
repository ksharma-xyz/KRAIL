package xyz.ksharma.krail.trip.planner.ui.homemap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.search.ai.ResolvedTripIntent
import xyz.ksharma.krail.trip.planner.ui.search.ai.isHandoffActionable
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * What an Ask KRAIL answer hands to Search Stop, or null when it found no stop at all. The home holds no
 * stop of its own, so the stops the answer did find open Search Stop's To and From form already filled and
 * the rider chooses the rest and goes. A half answer fills the bar it found and leaves the other to edit.
 */
internal fun handoffStopsFor(resolved: ResolvedTripIntent?): Pair<StopItem?, StopItem?>? =
    resolved?.takeIf { it.hasAnyStop }?.let { it.fromStopItem to it.toStopItem }

/**
 * Opens Search Stop with the answer once the Ask KRAIL dialog has closed onto the home after one.
 *
 * The answer is live while the dialog shows its settle beat (the phase is resolved), and the close that
 * follows steps the phase down in the same emission. So the answer is remembered while it is live, saved so
 * a rotation in the beat loses nothing, and acted on once, at the close. A dialog the rider closes without an
 * answer, or dismisses during the beat, holds nothing and opens nothing.
 */
@Composable
internal fun AskKrailHandoffEffect(
    aiState: AiSearchInputUiState,
    onHandoff: (from: StopItem?, to: StopItem?) -> Unit,
) {
    var answered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(aiState.isHandoffActionable, aiState.isInputOpen) {
        when {
            aiState.isHandoffActionable -> answered = true
            !aiState.isInputOpen && answered -> {
                answered = false
                handoffStopsFor(aiState.resolved)?.let { (from, to) -> onHandoff(from, to) }
            }
        }
    }
}
