package xyz.ksharma.krail.feature.debug.settings.ui.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.feature.debug.settings.ui.DebugConfigScreen
import xyz.ksharma.krail.feature.debug.settings.ui.DebugSettingsViewModel
import xyz.ksharma.krail.feature.debug.settings.ui.NetworkConfigScreen

/**
 * Navigation entries for the Debug Config feature. Wired into the parent
 * nav graph via `EntryProviderScope` from the host module (currently
 * trip-planner UI's `TripPlannerEntries`).
 *
 * @param onBack invoked when any of the screens fires a back press.
 *   The host's navigator does the actual `pop` since this module is
 *   navigator-agnostic.
 * @param onNavigateToNetwork open the Network sub-screen.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun EntryProviderScope<NavKey>.DebugConfigEntries(
    onBack: () -> Unit,
    onNavigateToNetwork: () -> Unit,
) {
    entry<DebugConfigHomeRoute>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) {
        val viewModel: DebugSettingsViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        DebugConfigScreen(
            tripTrackingEnabled = state.tripTrackingEnabled,
            onTripTrackingToggle = viewModel::setTripTrackingEnabled,
            addressSearchEnabled = state.addressSearchEnabled,
            onAddressSearchToggle = viewModel::setAddressSearchEnabled,
            onBackClick = onBack,
            onNetworkClick = onNavigateToNetwork,
            onResetReviewClick = viewModel::resetInAppReviewAsks,
        )
    }

    entry<DebugConfigNetworkRoute>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) {
        val viewModel: DebugSettingsViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val bffEnabled by viewModel.bffEnabled.collectAsStateWithLifecycle()
        NetworkConfigScreen(
            selected = state.source,
            liveBffEnabled = bffEnabled,
            onBackClick = onBack,
            onSelect = viewModel::selectSource,
        )
    }
}
