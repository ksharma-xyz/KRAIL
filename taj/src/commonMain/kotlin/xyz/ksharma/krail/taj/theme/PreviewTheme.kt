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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import xyz.ksharma.krail.taj.LocalThemeColor

@Composable
fun PreviewTheme(
    themeStyle: KrailThemeStyle = KrailThemeStyle.Train,
    modifier: Modifier = Modifier,
    darkTheme: Boolean? = null,
    // temporary measure until font scale support is added to compose multiplatform
    fontScale: Float = 1.0f,
    backgroundColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val isDarkTheme = darkTheme ?: isSystemInDarkTheme()

    KrailTheme(
        themeController = ThemeController(
            currentMode = if (isDarkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
            setThemeMode = {},
        ),
    ) {
        val bgColor = backgroundColor ?: KrailTheme.colors.surface
        val color = remember { mutableStateOf(themeStyle.hexColorCode) }
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalThemeColor provides color,
            LocalDensity provides Density(density = density.density, fontScale = fontScale),
        ) {
            Column(modifier = modifier.systemBarsPadding().background(bgColor)) {
                content()
            }
        }
    }
}
