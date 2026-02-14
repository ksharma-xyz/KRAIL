package xyz.ksharma.krail.trip.planner.ui.journeymap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapUiState

/**
 * Full-screen Journey Map Screen.
 * Shows the complete journey route on a map.
 *
 * Note: Journey data is already loaded from TimeTable screen, so no loading state needed.
 */
@Composable
fun JourneyMapScreen(
    journeyMapState: JourneyMapUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Journey data is already in memory - should always be Ready state
        when (journeyMapState) {
            is JourneyMapUiState.Ready -> {
                // Map takes full screen
                JourneyMap(
                    journeyMapState = journeyMapState,
                    modifier = Modifier.fillMaxSize(),
                )

                TitleBar(
                    title = {
                        Text("Journey Map")
                    },
                    onNavActionClick = onBackClick,
                    modifier = Modifier.background(color = KrailTheme.colors.surface),
                )
            }

            // Defensive fallbacks - should never happen in normal flow
            JourneyMapUiState.Loading -> {
                // Loading state is instant (data transform only)
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}
