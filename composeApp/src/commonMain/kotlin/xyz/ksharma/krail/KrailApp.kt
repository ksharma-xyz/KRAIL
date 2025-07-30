package xyz.ksharma.krail

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun KrailApp() {
    KrailTheme {
        SetupCoilImageLoader()
        KrailNavHost()
    }
}

@Composable
private fun SetupCoilImageLoader() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
}
