package xyz.ksharma.krail

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle
import xyz.ksharma.krail.taj.theme.LocalSystemDarkThemeOverride

fun MainViewController() = ComposeUIViewController {
    // WORKAROUND: isSystemInDarkTheme() on iOS reads UIViewController.traitCollection which
    // can get stuck returning dark after a background→foreground transition. iOS temporarily
    // sets a dark trait collection on the VC when taking the app-switcher screenshot, and
    // traitCollectionDidChange may not re-fire to correct it once the app is active again.
    //
    // Fix: track the real system appearance via UIApplicationDidBecomeActiveNotification,
    // reading UIScreen.mainScreen.traitCollection which is not affected by the same stale state.
    // This is provided via LocalSystemDarkThemeOverride to bypass the broken Compose value.
    var systemIsDark by remember {
        mutableStateOf(
            UIScreen.mainScreen.traitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark,
        )
    }

    DisposableEffect(Unit) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            systemIsDark =
                UIScreen.mainScreen.traitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark
        }
        onDispose {
            observer.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        }
    }

    CompositionLocalProvider(LocalSystemDarkThemeOverride provides systemIsDark) {
        KrailApp()
    }
}
