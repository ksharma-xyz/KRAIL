package xyz.ksharma.krail.core.permission.data.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Common permission module that includes platform-specific implementations.
 * PermissionStateTracker is an internal impl detail of AndroidPermissionController;
 * it is not a shared Koin singleton.
 */
val permissionModuleCommon = module {
    includes(permissionModule)
}

/**
 * Platform-specific permission module.
 * Each platform provides its own implementation.
 */
expect val permissionModule: Module

