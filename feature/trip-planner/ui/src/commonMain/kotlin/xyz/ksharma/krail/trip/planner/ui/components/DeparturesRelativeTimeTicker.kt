package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import xyz.ksharma.krail.departures.ui.DeparturesViewModel

/**
 * Keeps the departure board's countdown counting down.
 *
 * [DeparturesViewModel.isActive] recomputes every row's "in X mins" on a 10 second beat, and
 * like the rest of the polling in this app it is a `SharingStarted.WhileSubscribed` flow — with
 * no collector it never runs at all, so the board silently shows whatever the last network
 * fetch computed until the next fetch replaces it. Every surface that hosts a departure board
 * has to call this.
 *
 * `repeatOnLifecycle(STARTED)` rather than a plain `LaunchedEffect`, per
 * `docs/POLLING_LIFECYCLE.md`: composition scope would hold the subscriber open through
 * background and lock-screen, which is exactly what the `WhileSubscribed` gate exists to
 * prevent.
 */
@Composable
internal fun DeparturesRelativeTimeTicker(viewModel: DeparturesViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.isActive.collect {}
        }
    }
}
