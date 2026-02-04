package xyz.ksharma.krail.taj.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.LocalThemeColor

@Composable
fun PreviewTheme(
    themeStyle: KrailThemeStyle = KrailThemeStyle.Train,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val darkMode = isSystemInDarkTheme()
    KrailTheme(
        themeController = ThemeController(
            currentMode = if (darkMode) ThemeMode.DARK else ThemeMode.LIGHT,
            setThemeMode = {},
        ),
    ) {
        val bgColor = backgroundColor ?: KrailTheme.colors.surface
        val color = remember { mutableStateOf(themeStyle.hexColorCode) }
        CompositionLocalProvider(
            LocalThemeColor provides color,
        ) {
            Column(modifier = modifier.systemBarsPadding().background(bgColor)) {
                content()
            }
        }
    }
}
