package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import xyz.ksharma.krail.core.navigation.ResultEffect
import xyz.ksharma.krail.trip.planner.ui.navigation.StopSelectedResult

/**
 * Receives the stop the rider picked in the app's own stop search and sends it on as
 * [IntroUiEvent.StopPickedFromSearch], once for each pick.
 *
 * The result comes over the shared result bus, whose channel holds a result that arrives while the
 * Intro is not composed, as it is while the search is on screen. The Intro receives it when it is
 * back, once, and a turn of the screen does not receive it again because the channel gave it up the
 * first time. Nothing reads or writes the search itself.
 */
@Composable
internal fun IntroSearchResultEffect(onEvent: (IntroUiEvent) -> Unit) {
    val send by rememberUpdatedState(onEvent)
    ResultEffect<StopSelectedResult> { result -> result.toIntroEvent()?.let { send(it) } }
}
