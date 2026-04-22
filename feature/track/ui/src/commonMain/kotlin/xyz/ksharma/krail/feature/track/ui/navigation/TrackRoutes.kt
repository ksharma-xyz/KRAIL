package xyz.ksharma.krail.feature.track.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface TrackRoute : NavKey

/**
 * @param encodedData base64url-encoded [TripDeepLink] from the deep link `?d=` query param.
 *   Null when navigating from the SavedTrips screen (TrackingManager already has the journey).
 */
@Serializable
data class TrackTripRoute(val encodedData: String? = null) : TrackRoute
