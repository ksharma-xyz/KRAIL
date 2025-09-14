package xyz.ksharma.krail

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.core.appversion.AppUpgradeScreen
import xyz.ksharma.krail.core.deeplink.DeepLinkManager
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.deeplink.DeepLinkNavigationHandler
import xyz.ksharma.krail.splash.SplashScreen
import xyz.ksharma.krail.splash.SplashUiEvent
import xyz.ksharma.krail.splash.SplashViewModel
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.LocalThemeContentColor
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.toHex
import xyz.ksharma.krail.taj.unspecifiedColor
import xyz.ksharma.krail.trip.planner.ui.navigation.AppUpgradeRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.tripPlannerDestinations

@Composable
fun KrailNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val deepLinkManager: DeepLinkManager = koinInject()

    val themeColorHexCode = rememberSaveable { mutableStateOf(unspecifiedColor) }
    var themeId: Int? by rememberSaveable { mutableStateOf(null) }
    val themeContentColorHexCode = rememberSaveable { mutableStateOf(unspecifiedColor) }

    themeContentColorHexCode.value =
        getForegroundColor(
            backgroundColor = themeColorHexCode.value.hexToComposeColor(),
        ).toHex()

    // Handle deep link navigation events
    LaunchedEffect(navController) {
        val navigationHandler = DeepLinkNavigationHandler(navController)

        // Notify that navigation is ready to handle events
        deepLinkManager.onNavigationReady()
        log("KrailNavHost: Navigation system initialized and ready")

        deepLinkManager.deepLinkEvents.collectLatest { event ->
            log("KrailNavHost: Received deep link navigation event: $event")
            navigationHandler.handleNavigationEvent(event)
        }
    }

    CompositionLocalProvider(
        LocalThemeColor provides themeColorHexCode,
        LocalThemeContentColor provides themeContentColorHexCode,
        LocalTextColor provides KrailTheme.colors.onSurface,
    ) {
        NavHost(
            navController = navController,
            startDestination = SplashScreen,
            modifier = modifier.fillMaxSize(),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
        ) {
            tripPlannerDestinations(navController = navController)

            composable<SplashScreen> {
                val viewModel: SplashViewModel = koinViewModel<SplashViewModel>()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // Set theme in CompositionLocal
                themeId = uiState.themeStyle.id
                themeColorHexCode.value = uiState.themeStyle.hexColorCode

                LaunchedEffect(uiState.navigationDestination) {
                    uiState.navigationDestination?.let { destination ->
                        log("KrailNavHost: Splash completed, navigating to: $destination")

                        // Navigate to the destination and remove splash from back stack
                        // This ensures SavedTripsRoute becomes the effective root
                        navController.navigate(destination) {
                            // Remove splash from back stack completely
                            popUpTo<SplashScreen> {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                }

                SplashScreen(
                    logoColor = if (themeId != null && themeColorHexCode.value != unspecifiedColor) {
                        themeColorHexCode.value.hexToComposeColor()
                    } else {
                        KrailTheme.colors.onSurface
                    },
                    backgroundColor = KrailTheme.colors.surface,
                    onSplashAnimationComplete = {
                        log("KrailNavHost: Splash animation completed")
                        viewModel.onUiEvent(SplashUiEvent.SplashAnimationComplete)
                    },
                )
            }

            composable<AppUpgradeRoute> {
                AppUpgradeScreen()
            }
        }
    }
}

@Serializable
private data object SplashScreen
