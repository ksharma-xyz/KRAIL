package xyz.ksharma.krail.feature.location

/**
 * UI state for the location screen.
 */
sealed class LocationUiState {
    /**
     * Initial state, no action taken yet.
     */
    data object Idle : LocationUiState()

    /**
     * Permission needs to be requested.
     */
    data object PermissionRequired : LocationUiState()

    /**
     * Permission was permanently denied, user must go to settings.
     */
    data object PermissionDeniedPermanently : LocationUiState()

    /**
     * Loading state, waiting for location.
     */
    data object Loading : LocationUiState()

    /**
     * Successfully tracking location.
     *
     * @property location Current location data, or null if waiting for first update
     * @property isTracking Whether continuous tracking is active
     */
    data class Success(
        val location: xyz.ksharma.krail.core.location.Location?,
        val isTracking: Boolean = false
    ) : LocationUiState()

    /**
     * An error occurred.
     *
     * @property message Error message to display
     */
    data class Error(val message: String) : LocationUiState()
}

