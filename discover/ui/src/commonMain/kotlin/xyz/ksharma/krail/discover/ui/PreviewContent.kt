package xyz.ksharma.krail.discover.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle

@OptIn(ExperimentalCoilApi::class)
@Composable
fun PreviewContent(
    krailThemeStyle: KrailThemeStyle = KrailThemeStyle.Train,
    imageColor: Color = Color.Blue,
    content: @Composable () -> Unit
) {
    val previewHandler = AsyncImagePreviewHandler {
        ColorImage(color = imageColor.toArgb())
    }
    val themeColorHexCode = rememberSaveable {
        mutableStateOf(krailThemeStyle.hexColorCode)
    }
    KrailTheme {
        CompositionLocalProvider(
            LocalThemeColor provides themeColorHexCode,
            LocalAsyncImagePreviewHandler provides previewHandler,
        ) {
            content()
        }
    }
}
