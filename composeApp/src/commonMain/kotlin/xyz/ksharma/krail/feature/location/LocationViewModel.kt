package xyz.ksharma.krail.feature.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for location tracking feature.
 *
 * Note: This ViewModel receives controller/tracker instances via the UI layer
 * since they must be created through Compose for proper lifecycle integration.
 */
class LocationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LocationUiState>(LocationUiState.Idle)
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    private var trackingJob: Job? = null
    private var permissionControllerRef: Any? = null
    private var locationTrackerRef: Any? = null

    /**
     * Initialize with controllers.
     * Must be called before any operations.
     */
    fun init(permissionController: Any, locationTracker: Any) {
        this.permissionControllerRef = permissionController
        this.locationTrackerRef = locationTracker
    }

    /**
     * Request permission and get a single location.
     */
    fun requestSingleLocation() {
        _uiState.value = LocationUiState.Loading
        // Implementation will be called from UI layer with proper controllers
    }

    /**
     * Start continuous location tracking.
     */
    fun startTracking() {
        _uiState.value = LocationUiState.Loading
        // Implementation will be called from UI layer with proper controllers
    }

    /**
     * Stop location tracking.
     */
    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        _uiState.value = LocationUiState.Idle
    }

    /**
     * Open app settings.
     */
    fun openSettings() {
        // Will be called from UI layer
    }

    /**
     * Retry after error.
     */
    fun retry() {
        _uiState.value = LocationUiState.Idle
    }

    /**
     * Update UI state.
     */
    fun updateState(state: LocationUiState) {
        _uiState.value = state
    }

    /**
     * Set tracking job.
     */
    fun setTrackingJob(job: Job) {
        trackingJob = job
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}

