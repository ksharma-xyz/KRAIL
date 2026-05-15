package xyz.ksharma.krail.feature.pro.ui.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.feature.pro.state.ProEvent
import xyz.ksharma.krail.feature.pro.ui.ProUpgradeScreen
import xyz.ksharma.krail.feature.pro.ui.ProViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun EntryProviderScope<NavKey>.ProEntries(
    onBack: () -> Unit,
) {
    entry<ProUpgradeRoute>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) {
        val viewModel: ProViewModel = koinViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        ProUpgradeScreen(
            state = state,
            onClose = onBack,
            onSelectPlan = { viewModel.onEvent(ProEvent.SelectPlan(it)) },
            onSubscribe = { viewModel.onEvent(ProEvent.SubscribeTapped) },
            onRestorePurchase = { viewModel.onEvent(ProEvent.RestorePurchaseTapped) },
        )
    }
}
