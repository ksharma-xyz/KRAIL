package xyz.ksharma.krail.park.ride.network

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.remoteconfig.JsonConfig
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.park.ride.network.model.NswParkRideFacility

internal class RealNswParkRideFacilityManager(
    private val flag: Flag,
) : NswParkRideFacilityManager {

    override fun getParkRideFacilities(): List<NswParkRideFacility> {
        val flagValue: FlagValue = flag.getFlagValue(FlagKeys.NSW_PARK_RIDE_FACILITIES.key)

        val facilities = when (flagValue) {
            is FlagValue.JsonValue -> {
                val jsonArray = JsonConfig.lenient.parseToJsonElement(flagValue.value).jsonArray
                jsonArray.map { element ->
                    val obj = element.jsonObject
                    NswParkRideFacility(
                        stopId = obj["stopId"]?.jsonPrimitive?.content ?: "",
                        parkRideFacilityId = obj["parkRideFacilityId"]?.jsonPrimitive?.content ?: "",
                        parkRideName = obj["parkRideName"]?.jsonPrimitive?.content ?: "",
                    )
                }
            }

            else -> emptyList()
        }

        // Remote Config or, failing that, the bundled default always holds the stations, so an empty list means the
        // flag was read before either was in place, or the value is malformed. Say so, loudly, and never as "none near".
        if (facilities.isEmpty()) {
            logError(
                "Park & Ride catalogue is empty. Flag ${FlagKeys.NSW_PARK_RIDE_FACILITIES.key} read as " +
                    "${flagValue::class.simpleName}, but Remote Config or the bundled default should hold stations.",
            )
        }
        return facilities
    }

    override fun getParkRideFacilityById(facilityId: String): NswParkRideFacility? {
        val facilities = getParkRideFacilities()
        return facilities.find { it.parkRideFacilityId == facilityId }
    }
}
