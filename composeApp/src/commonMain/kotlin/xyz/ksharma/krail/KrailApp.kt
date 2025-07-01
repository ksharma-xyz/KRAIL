package xyz.ksharma.krail

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun KrailApp() {
    KrailTheme {
        KrailNavHost()
    }
}
