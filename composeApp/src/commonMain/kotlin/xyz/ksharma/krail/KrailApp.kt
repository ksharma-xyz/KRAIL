package xyz.ksharma.krail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import org.koin.compose.koinInject
import xyz.ksharma.krail.core.appinfo.AppInfo
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.appinfo.LocalAppInfo
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.LocalThemeController
import xyz.ksharma.krail.theme.rememberAppThemeController
import xyz.ksharma.krail.trip.planner.ui.intro.finale.IntroFinaleHost
import xyz.ksharma.krail.trip.planner.ui.intro.finale.IntroFinaleSignal

@Composable
fun KrailApp() {
    val appInfo = rememberAppInfo()
    val themeController = rememberAppThemeController()
    val introFinale: IntroFinaleSignal = koinInject()

    CompositionLocalProvider(
        LocalAppInfo provides appInfo,
        LocalThemeController provides themeController,
    ) {
        SetupCoilImageLoader()

        KrailTheme(themeController = themeController) {
            // The finale after the Intro is a sibling above the nav host, never a layer that holds it, so a map
            // under it is never composited offscreen. Idle, it is one box and draws nothing.
            IntroFinaleHost(signal = introFinale) {
                KrailNavHost(modifier = Modifier.exposeTestTagsToUiAutomation())
            }
        }
    }
}

@Composable
private fun rememberAppInfo(): AppInfo? {
    val appInfoProvider: AppInfoProvider = koinInject()
    val appInfoState = produceState<AppInfo?>(initialValue = null, appInfoProvider) {
        value = appInfoProvider.getAppInfo()
    }
    return appInfoState.value
}

@Composable
private fun SetupCoilImageLoader() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
}
