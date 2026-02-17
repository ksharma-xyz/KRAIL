package xyz.ksharma.krail.core.permission.data.di
import org.koin.core.module.Module
import org.koin.dsl.module
/**
 * iOS-specific permission module.
 * 
 * Note: PermissionController is created via Compose rememberPermissionController()
 * for proper lifecycle management.
 */
actual val permissionModule: Module = module {
    // iOS-specific dependencies can go here if needed
    // PermissionController is created via Compose
}
