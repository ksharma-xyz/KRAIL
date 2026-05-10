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
 * Production KRAIL-BFF base URL, sourced from `local.properties`
 * `krail.bffProdBaseUrl`. Empty represents "BFF prod not yet deployed";
 * services must treat that case as "fall back to NSW direct" rather than
 * letting an empty URL through.
 *
 * Like [KRAIL_BFF_BASE_URL] this is intentionally not used in release
 * builds yet. The runtime debug-settings selector (Phase B) reads it when a
 * scope is set to `BFF_PROD`. Production rollout will eventually resolve via
 * Firebase Remote Config rather than this constant.
 */
val KRAIL_BFF_PROD_BASE_URL: String = NetworkBuildKonfig.KRAIL_BFF_PROD_BASE_URL

/**
 * True when [KRAIL_BFF_PROD_BASE_URL] has been wired up. Empty default until
 * the BFF actually deploys; the debug-settings UI can use this flag to
 * disable the `BFF_PROD` row with a `<not deployed>` hint.
 */
val IS_BFF_PROD_DEPLOYED: Boolean = KRAIL_BFF_PROD_BASE_URL.isNotBlank()

/**
 * Phase C scaffolding flag — when `true` AND [IS_BFF_LOCAL_OVERRIDE_SET] is on,
 * the proto-flagged branch is taken on every consumer that has one wired up:
 *
 *  - `RealTripPlanningService.trip()` hits `/api/v1/trip/plan-proto` and
 *    decodes a `JourneyList`.
 *  - `RealDeparturesService.departures()` hits
 *    `/api/v1/stops/{id}/departures-proto` and decodes a
 *    `DepartureBoardResponse`.
 *  - `RealParkRideService.fetchAvailabilityForStops()` hits
 *    `/api/v1/parking/availability-proto` and decodes a
 *    `ParkingAvailabilityResponse`.
 *
 * Each consumer maps its proto response back to the existing JSON-shape model
 * so downstream UI code keeps working unchanged.
 *
 * Hard-coded to `true` for this branch. Phase B's production wiring flips this
 * at runtime via Firebase Remote Config (`enable_proto_bff`). The future
 * debug-settings UI (Branch 2) will let developers force a local value via
 * `NetworkSource.FOLLOW_RC` etc.
 *
 * Why a separate flag from [IS_BFF_LOCAL_OVERRIDE_SET]: Phase A's override is
 * shape-identical (BFF passes NSW JSON through). Phase C swaps the wire format
 * and requires dedicated mappers, so the two flags must move independently.
 */
const val IS_BFF_PROTO_ENABLED: Boolean = true
