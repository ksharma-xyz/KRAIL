package xyz.ksharma.krail.taj.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import xyz.ksharma.krail.taj.LocalThemeColor

@Composable
fun PreviewTheme(
    themeStyle: KrailThemeStyle,
    content: @Composable () -> Unit
) {
    KrailTheme {
        val color = remember { mutableStateOf(themeStyle.hexColorCode) }
        CompositionLocalProvider(LocalThemeColor provides color) {
            content()
        }
    }
}
