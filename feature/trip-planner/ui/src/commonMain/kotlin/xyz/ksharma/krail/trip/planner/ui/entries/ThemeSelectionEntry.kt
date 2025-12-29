package xyz.ksharma.krail.trip.planner.ui.entries

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.state.usualride.ThemeSelectionEvent
import xyz.ksharma.krail.trip.planner.ui.themeselection.ThemeSelectionScreen
import xyz.ksharma.krail.trip.planner.ui.themeselection.ThemeSelectionViewModel

/**
 * Theme Selection Entry - Detail Screen in List-Detail pattern
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun EntryProviderScope<NavKey>.ThemeSelectionEntry(
    tripPlannerNavigator: TripPlannerNavigator,
) {
    entry<ThemeSelectionRoute>(
        metadata = androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.detailPane(),
    ) {
        val viewModel: ThemeSelectionViewModel = koinViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        ThemeSelectionScreen(
            selectedThemeStyle = state.selectedThemeStyle ?: KrailThemeStyle.Train,
            onThemeSelected = { themeId ->
                val hexColorCode = KrailThemeStyle.entries
                    .firstOrNull { it.id == themeId }
                    ?.hexColorCode

                check(hexColorCode != null) { "hexColorCode for themeId $themeId not found" }

                // Update global theme via interface method
                tripPlannerNavigator.updateTheme(hexColorCode)

                // Save to database
                viewModel.onEvent(ThemeSelectionEvent.ThemeSelected(themeId))
            },
            onBackClick = { tripPlannerNavigator.goBack() },
            onThemeModeSelect = { code ->
                viewModel.onEvent(ThemeSelectionEvent.ThemeModeSelected(code))
            },
        )
    }
}
