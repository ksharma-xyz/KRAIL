package xyz.ksharma.krail.taj.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.taj.LocalThemeColor

@Composable
fun PreviewTheme(
    themeStyle: KrailThemeStyle,
    darkTheme: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    KrailTheme(darkTheme = darkTheme) {
        val color = remember { mutableStateOf(themeStyle.hexColorCode) }
        CompositionLocalProvider(LocalThemeColor provides color) {
            Column(modifier = modifier.systemBarsPadding().background(KrailTheme.colors.surface)) {
                content()
            }
        }
    }
}
