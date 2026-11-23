package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.runtime.Composable

private val neverChanging: () -> Boolean = { false }

/** iOS does not rebuild the screen for a rotation, so a stop is always a real one. */
@Composable
internal actual fun rememberIsChangingConfigurations(): () -> Boolean = neverChanging
