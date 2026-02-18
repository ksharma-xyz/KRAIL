package xyz.ksharma.krail.feature.location

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.core.location.Location
import xyz.ksharma.krail.core.location.data.rememberLocationTracker
import xyz.ksharma.krail.core.permission.LocationPermissionType
import xyz.ksharma.krail.core.permission.data.rememberPermissionController
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

/**
 * Location tracking demo screen.
 *
 * Demonstrates permission handling and location tracking.
 */
@Composable
fun LocationScreen(
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel = koinViewModel()
) {
    val permissionController = rememberPermissionController()
    val locationTracker = rememberLocationTracker()
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()

    // Handle single location request
    fun handleSingleLocation() {
        scope.launch {
            viewModel.updateState(LocationUiState.Loading)

            when (val result = permissionController.requestPermission(LocationPermissionType.LOCATION_WHEN_IN_USE)) {
                is xyz.ksharma.krail.core.permission.PermissionResult.Granted -> {
                    try {
                        val location = locationTracker.getCurrentLocation(timeoutMs = 10_000L)
                        viewModel.updateState(LocationUiState.Success(location = location, isTracking = false))
                    } catch (e: xyz.ksharma.krail.core.location.LocationError) {
                        viewModel.updateState(LocationUiState.Error(e.message ?: "Unknown error"))
                    }
                }
                is xyz.ksharma.krail.core.permission.PermissionResult.Denied -> {
                    if (result.isPermanent) {
                        viewModel.updateState(LocationUiState.PermissionDeniedPermanently)
                    } else {
                        viewModel.updateState(LocationUiState.PermissionRequired)
                    }
                }
                is xyz.ksharma.krail.core.permission.PermissionResult.Cancelled -> {
                    viewModel.updateState(LocationUiState.Idle)
                }
            }
        }
    }

    // Handle continuous tracking
    fun handleStartTracking() {
        scope.launch {
            viewModel.updateState(LocationUiState.Loading)

            when (val result = permissionController.requestPermission(LocationPermissionType.LOCATION_WHEN_IN_USE)) {
                is xyz.ksharma.krail.core.permission.PermissionResult.Granted -> {
                    val job = launch {
                        viewModel.updateState(LocationUiState.Success(location = null, isTracking = true))

                        locationTracker.startTracking(
                            xyz.ksharma.krail.core.location.LocationConfig(
                                updateIntervalMs = 30_000L,
                                minDistanceMeters = 10f,
                                priority = xyz.ksharma.krail.core.location.LocationPriority.HIGH_ACCURACY
                            )
                        ).collect { location ->
                            viewModel.updateState(LocationUiState.Success(location = location, isTracking = true))
                        }
                    }
                    viewModel.setTrackingJob(job)
                }
                is xyz.ksharma.krail.core.permission.PermissionResult.Denied -> {
                    if (result.isPermanent) {
                        viewModel.updateState(LocationUiState.PermissionDeniedPermanently)
                    } else {
                        viewModel.updateState(LocationUiState.PermissionRequired)
                    }
                }
                is xyz.ksharma.krail.core.permission.PermissionResult.Cancelled -> {
                    viewModel.updateState(LocationUiState.Idle)
                }
            }
        }
    }

    fun handleStopTracking() {
        locationTracker.stopTracking()
        viewModel.stopTracking()
    }

    fun handleOpenSettings() {
        permissionController.openAppSettings()
    }

    LocationScreenContent(
        uiState = uiState,
        onRequestSingleLocation = ::handleSingleLocation,
        onStartTracking = ::handleStartTracking,
        onStopTracking = ::handleStopTracking,
        onOpenSettings = ::handleOpenSettings,
        onRetry = viewModel::retry,
        modifier = modifier
    )
}

@Composable
private fun LocationScreenContent(
    uiState: LocationUiState,
    onRequestSingleLocation: () -> Unit,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Location Tracking Demo",
            style = MaterialTheme.typography.headlineMedium
        )

        HorizontalDivider()

        when (uiState) {
            is LocationUiState.Idle -> {
                IdleState(
                    onRequestSingleLocation = onRequestSingleLocation,
                    onStartTracking = onStartTracking
                )
            }

            is LocationUiState.PermissionRequired -> {
                PermissionRequiredState(
                    onRequestAgain = onRequestSingleLocation
                )
            }

            is LocationUiState.PermissionDeniedPermanently -> {
                PermissionDeniedState(
                    onOpenSettings = onOpenSettings
                )
            }

            is LocationUiState.Loading -> {
                LoadingState()
            }

            is LocationUiState.Success -> {
                SuccessState(
                    location = uiState.location,
                    isTracking = uiState.isTracking,
                    onRequestSingleLocation = onRequestSingleLocation,
                    onStartTracking = onStartTracking,
                    onStopTracking = onStopTracking
                )
            }

            is LocationUiState.Error -> {
                ErrorState(
                    message = uiState.message,
                    onRetry = onRetry
                )
            }
        }
    }
}

@Composable
private fun IdleState(
    onRequestSingleLocation: () -> Unit,
    onStartTracking: () -> Unit
) {
    Text(
        text = "This demo shows location tracking capabilities.",
        style = MaterialTheme.typography.bodyMedium
    )

    Button(
        onClick = onRequestSingleLocation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Get Current Location")
    }

    Button(
        onClick = onStartTracking,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Start Continuous Tracking")
    }
}

@Composable
private fun PermissionRequiredState(
    onRequestAgain: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Location Permission Required",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This app needs location permission to show your current location.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }

    Button(onClick = onRequestAgain, modifier = Modifier.fillMaxWidth()) {
        Text("Grant Permission")
    }
}

@Composable
private fun PermissionDeniedState(
    onOpenSettings: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Permission Permanently Denied",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Location permission was permanently denied. Please enable it in app settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }

    Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
        Text("Open Settings")
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Getting location...")
        }
    }
}

@Composable
private fun SuccessState(
    location: Location?,
    isTracking: Boolean,
    onRequestSingleLocation: () -> Unit,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit
) {
    if (location != null) {
        LocationDataCard(location = location)
    } else {
        Text(
            text = if (isTracking) "Waiting for first location update..." else "No location data yet",
            style = MaterialTheme.typography.bodyMedium
        )
    }

    if (isTracking) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Tracking active (updates every 30s or 10m movement)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Button(
            onClick = onStopTracking,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Stop Tracking")
        }
    } else {
        Button(onClick = onRequestSingleLocation, modifier = Modifier.fillMaxWidth()) {
            Text("Get Current Location")
        }

        Button(onClick = onStartTracking, modifier = Modifier.fillMaxWidth()) {
            Text("Start Continuous Tracking")
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }

    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
        Text("Try Again")
    }
}

@Composable
private fun LocationDataCard(location: Location) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Location Data",
                style = MaterialTheme.typography.titleMedium
            )

            HorizontalDivider()

            LocationDataRow(label = "Latitude", value = "${location.latitude.round(6)}°")
            LocationDataRow(label = "Longitude", value = "${location.longitude.round(6)}°")

            location.accuracy?.let {
                LocationDataRow(label = "Accuracy", value = "${it.round(2)} m")
            }

            location.altitude?.let {
                LocationDataRow(label = "Altitude", value = "${it.round(2)} m")
            }

            location.altitudeAccuracy?.let {
                LocationDataRow(label = "Altitude Accuracy", value = "${it.round(2)} m")
            }

            location.speed?.let {
                LocationDataRow(
                    label = "Speed",
                    value = "${it.round(2)} m/s (${(it * 3.6).round(1)} km/h)"
                )
            }

            location.speedAccuracy?.let {
                LocationDataRow(label = "Speed Accuracy", value = "${it.round(2)} m/s")
            }

            location.bearing?.let {
                val direction = when {
                    it < 22.5 || it >= 337.5 -> "N"
                    it < 67.5 -> "NE"
                    it < 112.5 -> "E"
                    it < 157.5 -> "SE"
                    it < 202.5 -> "S"
                    it < 247.5 -> "SW"
                    it < 292.5 -> "W"
                    else -> "NW"
                }
                LocationDataRow(label = "Bearing", value = "${it.round(1)}° ($direction)")
            }

            location.bearingAccuracy?.let {
                LocationDataRow(label = "Bearing Accuracy", value = "${it.round(1)}°")
            }

            LocationDataRow(
                label = "Timestamp",
                value = formatTimestamp(location.timestamp)
            )
        }
    }
}

@Composable
private fun LocationDataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * Round double to specified number of decimal places.
 * Multiplatform-safe alternative to String.format.
 */
private fun Double.round(decimals: Int): String {
    val multiplier = when (decimals) {
        0 -> 1.0
        1 -> 10.0
        2 -> 100.0
        3 -> 1000.0
        4 -> 10000.0
        5 -> 100000.0
        6 -> 1000000.0
        else -> 1.0
    }
    val rounded = kotlin.math.round(this * multiplier) / multiplier

    // Convert to string and ensure we have the right number of decimals
    val str = rounded.toString()
    val dotIndex = str.indexOf('.')

    return if (decimals == 0) {
        if (dotIndex >= 0) str.substring(0, dotIndex) else str
    } else {
        if (dotIndex < 0) {
            // No decimal point, add it
            str + "." + "0".repeat(decimals)
        } else {
            val currentDecimals = str.length - dotIndex - 1
            when {
                currentDecimals < decimals -> str + "0".repeat(decimals - currentDecimals)
                currentDecimals > decimals -> str.substring(0, dotIndex + decimals + 1)
                else -> str
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun formatTimestamp(timestamp: Long): String {
    // Calculate time difference from now using kotlin.time
    val now = Clock.System.now()
    val locationTime = timestamp.milliseconds
    val currentTime = now.toEpochMilliseconds().milliseconds
    val diff = currentTime - locationTime

    val seconds = diff.inWholeSeconds
    return when {
        seconds < 5 -> "Just now"
        seconds < 60 -> "${seconds}s ago"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86400 -> "${seconds / 3600}h ago"
        else -> "${seconds / 86400}d ago"
    }
}

