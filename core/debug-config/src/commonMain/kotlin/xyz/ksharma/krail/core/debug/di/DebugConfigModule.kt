package xyz.ksharma.krail.core.debug.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import xyz.ksharma.krail.core.debug.DebugConfigManager
import xyz.ksharma.krail.core.debug.DebugConfigViewModel
import xyz.ksharma.krail.core.debug.RealDebugConfigManager

/**
 * Debug configuration module.
 * This should only be included in debug builds.
 */
val debugConfigModule = module {
    single<DebugConfigManager> {
        RealDebugConfigManager(
            sandookPreferences = get(),
            sandook = get(),
        )
    }
    viewModel { DebugConfigViewModel(get()) }
}
