package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.runtime.Composable

/**
 * Whether the screen is being torn down only to be built again, as Android does to the Activity for a rotation, a
 * fold or a font change. It answers true from the moment the old screen stops until it is gone, and false at every
 * other time, including a real stop (the app going to the background) and a real leave.
 *
 * The map's analytics need it because a stop and a start also come with that rebuild, and a rotation must not end a
 * map open and start another. iOS never rebuilds the screen, so it is always false there.
 */
@Composable
internal expect fun rememberIsChangingConfigurations(): () -> Boolean
