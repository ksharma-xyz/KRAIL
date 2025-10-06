package xyz.ksharma.krail.discover.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme

@OptIn(ExperimentalCoilApi::class)
@Composable
fun PreviewDiscoverContent(
    krailThemeStyle: KrailThemeStyle = KrailThemeStyle.Train,
    imageColor: Color = Color.Blue,
    content: @Composable () -> Unit,
) {
    val previewHandler = AsyncImagePreviewHandler {
        ColorImage(color = imageColor.toArgb())
    }
    PreviewTheme(themeStyle = krailThemeStyle) {
        CompositionLocalProvider(
            LocalAsyncImagePreviewHandler provides previewHandler,
        ) {
            content()
        }
    }
}
