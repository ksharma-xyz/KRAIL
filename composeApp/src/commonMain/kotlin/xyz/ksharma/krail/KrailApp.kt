package xyz.ksharma.krail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.produceState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import org.koin.compose.koinInject
import xyz.ksharma.krail.core.appinfo.AppInfo
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.appinfo.LocalAppInfo
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun KrailApp() {
    val appInfo = rememberAppInfo()

    CompositionLocalProvider(LocalAppInfo provides appInfo) {
        SetupCoilImageLoader()

        KrailTheme {
            KrailNavHost()
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
