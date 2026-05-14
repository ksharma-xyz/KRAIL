package xyz.ksharma.krail.park.ride.network.model

import kotlinx.serialization.Serializable

/**
 * Response shape for the BFF batch endpoint
 * `GET /v1/parking/availability?stopIds=...` (Mode 2 in
 * `KRAIL-BFF/docs/handover/PARK_RIDE_BATCH_HANDOVER.md` §2).
 *
 * The BFF resolves each saved-trip stop ID to one or more NSW
 * park-and-ride facilities, fans out the per-facility NSW calls
 * concurrently, and returns the merged result keyed by the same
 * stop ID the caller sent.
 *
 * Per the handover doc, JSON keys are already camelCase on the wire,
 * so plain `@Serializable` (no `@SerialName`) is correct here.
 *
 * @property stops Map of caller-sent stop ID to its facility availability
 *   payload. Stops that resolved successfully appear here even if
 *   individual NSW calls failed (their failures live in
 *   [StopFacilities.errors]).
 * @property unknownStops Stop IDs that the BFF could not map to any
 *   facility (typo, decommissioned stop, or simply no park-and-ride
 *   at that stop). Treated as soft failures, not a hard 400.
 * @property correlationId Server-side request ID for cross-referencing
 *   logs when triaging incidents.
 */
@Serializable
data class ParkingStopBatchResponse(
    val stops: Map<String, StopFacilities> = emptyMap(),
    val unknownStops: List<String> = emptyList(),
    val correlationId: String? = null,
)

/**
 * Per-stop slice of [ParkingStopBatchResponse.stops]. Splits successful
 * facility payloads from per-facility upstream errors so the caller can
 * surface partial-success states without re-parsing.
 *
 * @property facilities Successful facility payloads keyed by NSW
 *   facility ID. Each value matches the single-facility shape returned
 *   by `/v1/parking/facilities/{id}/availability`.
 * @property errors Failures keyed by NSW facility ID. Empty when every
 *   facility under this stop succeeded.
 */
@Serializable
data class StopFacilities(
    val facilities: Map<String, CarParkFacilityDetailResponse> = emptyMap(),
    val errors: Map<String, BatchError> = emptyMap(),
)

/**
 * A single per-facility upstream failure inside a batch response.
 *
 * Codes documented in the BFF handover §2 include
 * `invalid_facility_id`, `upstream_404`, `upstream_400`, `upstream_429`,
 * `upstream_error`, and `daily_budget_exceeded`. Treat unknown codes
 * as generic upstream failures.
 */
@Serializable
data class BatchError(
    val code: String,
    val message: String,
)
