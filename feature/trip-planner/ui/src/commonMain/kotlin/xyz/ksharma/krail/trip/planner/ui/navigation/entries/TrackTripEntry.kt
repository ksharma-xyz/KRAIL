package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.feature.track.ui.navigation.TrackTripRoute
import xyz.ksharma.krail.trip.planner.ui.tracktrip.TrackTripScreen

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun EntryProviderScope<NavKey>.TrackTripEntry(
    onBack: () -> Unit,
) {
    entry<TrackTripRoute>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) { route ->
        LaunchedEffect(route) {
            log(
                "[DEEPLINK] TrackTripEntry composed — " +
                    "encodedData=${route.encodedData?.take(20)?.plus("…") ?: "null"}, " +
                    "routeHash=${route.hashCode()}",
            )
        }
        TrackTripScreen(
            encodedData = route.encodedData,
            onBack = onBack,
        )
    }
}
