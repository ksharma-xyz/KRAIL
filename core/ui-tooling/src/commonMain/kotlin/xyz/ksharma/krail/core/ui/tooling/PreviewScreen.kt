package xyz.ksharma.krail.core.ui.tooling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme

@Composable
fun PreviewScreen(
    modifier: Modifier = Modifier,
    themeStyle: KrailThemeStyle = KrailThemeStyle.Train,
    // temporary measure until font scale support is added to compose multiplatform
    backgroundColor: Color? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides PreviewNavigationEventDispatcherOwner()) {
        PreviewTheme(
            themeStyle = themeStyle,
            modifier = modifier,
            backgroundColor = backgroundColor,
        ) {
            content()
        }
    }
}
