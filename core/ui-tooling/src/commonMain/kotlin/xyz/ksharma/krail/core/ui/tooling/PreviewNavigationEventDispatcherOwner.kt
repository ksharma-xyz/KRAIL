package xyz.ksharma.krail.core.ui.tooling

import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner

internal class PreviewNavigationEventDispatcherOwner : NavigationEventDispatcherOwner {
    override val navigationEventDispatcher: NavigationEventDispatcher
        get() = NavigationEventDispatcher(onBackCompletedFallback = {})
}
