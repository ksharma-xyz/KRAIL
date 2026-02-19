package xyz.ksharma.krail.core.location.data.di
import org.koin.core.module.Module
import org.koin.dsl.module
/**
 * Android-specific location module.
 *
 * Note: LocationTracker is created via Compose rememberLocationTracker()
 * for proper FusedLocationProviderClient integration.
 */
actual val locationModule: Module = module {
    // Android-specific dependencies can go here if needed
    // LocationTracker is created via Compose
}
