package xyz.ksharma.krail.core.permission.data.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android-specific permission module.
 *
 * Note: PermissionController cannot be provided via Koin because it requires
 * ActivityResultLauncher which is only available at Compose runtime.
 * Use rememberPermissionController() in Compose code.
 */
actual val permissionModule: Module = module {
    // Android-specific dependencies can go here if needed
    // PermissionController is created via Compose
}
