package xyz.ksharma.krail.trip.planner.network.api.mapper

import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse

/**
 * Stub for the Phase C `JourneyList` (Wire-generated proto) to [TripResponse]
 * mapper. The proto contract is in `krail-api-proto/proto/api/trip.proto`,
 * pinned at `v0.1.0` in this repo's `krail-api-proto/` git submodule.
 *
 * Wiring this up is a follow-up step. See `docs/BFF_PHASE_A_MORNING.md` §5.
 *
 * To implement:
 *   1. Add `implementation(projects.io.bffApi)` to this module's
 *      `build.gradle.kts` (commonMain dependencies).
 *   2. Replace the parameter type below with `JourneyList`
 *      (import `app.krail.bff.proto.JourneyList`).
 *   3. Map every `// contract: required` proto field non-null per the
 *      contract convention at <https://ksharma-xyz.github.io/KRAIL-API-PROTO/contract>.
 *   4. Map `// contract: optional` proto fields to nullable domain fields
 *      and let UI handle absence.
 *   5. Replace the `RealTripPlanningService.trip()` proto-branch scaffold
 *      to call this mapper.
 *
 * Today the function signature exists so the call site in
 * `RealTripPlanningService` compiles, but invoking it always errors. That's
 * intentional because the proto branch is gated on
 * `IS_BFF_PROTO_FOR_TRIP_RESULTS_ENABLED` (hard-coded `false`).
 *
 * @param journeyListBytes The raw `JourneyList` protobuf bytes from the
 *   `/api/v1/trip/plan-proto` BFF endpoint. Once the proto module is wired,
 *   this should be replaced by a real `JourneyList` parameter.
 */
@Suppress("UnusedParameter")
internal fun journeyListBytesToTripResponse(journeyListBytes: ByteArray): TripResponse {
    error(
        "JourneyList to TripResponse mapping pending. " +
            "See docs/BFF_PHASE_A_MORNING.md §5 for the wiring steps.",
    )
}
