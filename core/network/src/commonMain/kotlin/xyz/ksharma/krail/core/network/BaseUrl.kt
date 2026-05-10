package xyz.ksharma.krail.core.network

const val NSW_TRANSPORT_BASE_URL = "https://api.transport.nsw.gov.au"

/**
 * Local KRAIL-BFF base URL, sourced from `local.properties` `krail.bffBaseUrl`.
 * Empty when the developer has not opted in to local-BFF testing; services
 * fall back to NSW direct in that case.
 *
 * Intentionally not used in release builds. When release routing lands it
 * will be flagged via Firebase Remote Config, not this constant.
 */
val KRAIL_BFF_BASE_URL: String = NetworkBuildKonfig.KRAIL_BFF_BASE_URL

/** True when the developer has set `krail.bffBaseUrl` in local.properties. */
val IS_BFF_LOCAL_OVERRIDE_SET: Boolean = KRAIL_BFF_BASE_URL.isNotBlank()

/**
 * Phase C scaffolding flag — when `true` AND [IS_BFF_LOCAL_OVERRIDE_SET] is on,
 * `RealTripPlanningService.trip()` calls the BFF's `/api/v1/trip/plan-proto`
 * endpoint and decodes a `JourneyList` protobuf instead of NSW JSON.
 *
 * Hard-coded to `false` for now. The eventual debug-settings UI (Phase B prep)
 * will flip this at runtime and this constant will move into the store.
 *
 * Why a separate flag: Phase A's `IS_BFF_LOCAL_OVERRIDE_SET` is shape-identical
 * (BFF passes NSW JSON through). Phase C swaps the wire format and requires a
 * dedicated mapper, so we want to flip them independently — and ship Phase A
 * tonight without taking on Phase C's mapper risk.
 */
const val IS_BFF_PROTO_FOR_TRIP_RESULTS_ENABLED: Boolean = true
