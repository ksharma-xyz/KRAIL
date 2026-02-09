package xyz.ksharma.krail.trip.planner.ui.journeymap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapUiState

/**
 * Full-screen Journey Map Screen.
 * Shows the complete journey route on a map with better gesture support.
 */
@Composable
fun JourneyMapScreen(
    journeyMapState: JourneyMapUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (journeyMapState) {
            JourneyMapUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            is JourneyMapUiState.Ready -> {
                // Map takes full screen
                JourneyMap(
                    journeyMapState = journeyMapState,
                    modifier = Modifier.fillMaxSize(),
                )

                // Title bar overlay at top
                TitleBar(
                    title = {
                        Text(
                            text = "Journey Map",
                            style = KrailTheme.typography.headlineMedium,
                        )
                    },
                    onNavActionClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .systemBarsPadding(),
                )
            }

            is JourneyMapUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = journeyMapState.message,
                        style = KrailTheme.typography.bodyLarge,
                        color = KrailTheme.colors.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                // Title bar for navigation
                TitleBar(
                    title = {
                        Text(
                            text = "Journey Map",
                            style = KrailTheme.typography.headlineMedium,
                        )
                    },
                    onNavActionClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .systemBarsPadding(),
                )
            }
        }
    }
}
