package xyz.ksharma.krail.core.location.data.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Common location module that includes platform-specific implementations.
 */
val locationModuleCommon = module {
    includes(locationModule)
}

/**
 * Platform-specific location module.
 * Each platform provides its own implementation.
 */
expect val locationModule: Module
