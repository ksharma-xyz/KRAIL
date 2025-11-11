package xyz.ksharma.krail.di

import org.koin.core.module.Module
import org.koin.dsl.module
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.debug.DebugConfigManager
import xyz.ksharma.krail.core.debug.NoOpDebugConfigManager
import xyz.ksharma.krail.core.debug.di.debugConfigModule

/**
 * Returns the appropriate debug module based on whether this is a debug build.
 *
 * Uses appInfoProvider.getAppInfo().isDebug to determine build type:
 * - DEBUG builds: Full debug UI with environment switcher
 * - RELEASE builds: No-op implementation (always production)
 */
fun getDebugModule(appInfoProvider: AppInfoProvider): Module {
    return if (appInfoProvider.getAppInfo().isDebug) {
        debugConfigModule // Full debug implementation with UI
    } else {
        module {
            // No-op implementation for release
            single<DebugConfigManager> { NoOpDebugConfigManager() }
        }
    }
}

