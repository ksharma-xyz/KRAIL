package xyz.ksharma.krail.core.location.data.di
import org.koin.core.module.Module
import org.koin.dsl.module
/**
 * iOS-specific location module.
 *
 * Note: LocationTracker is created via Compose rememberLocationTracker()
 * for proper CLLocationManager integration.
 */
actual val locationModule: Module = module {
    // iOS-specific dependencies can go here if needed
    // LocationTracker is created via Compose
}
