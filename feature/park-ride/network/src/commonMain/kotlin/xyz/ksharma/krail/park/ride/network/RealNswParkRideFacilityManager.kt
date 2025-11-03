package xyz.ksharma.krail.park.ride.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.park.ride.network.model.NswParkRideFacility

class RealNswParkRideFacilityManager(
    private val flag: Flag,
) : NswParkRideFacilityManager {

    override fun getParkRideFacilities(): List<NswParkRideFacility> {
        val flagValue: FlagValue = flag.getFlagValue(FlagKeys.NSW_PARK_RIDE_FACILITIES.key)

        return when (flagValue) {
            is FlagValue.JsonValue -> {
                val jsonArray = Json.parseToJsonElement(flagValue.value).jsonArray
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
    }

    override fun getParkRideFacilityById(facilityId: String): NswParkRideFacility? {
        val facilities = getParkRideFacilities()
        return facilities.find { it.parkRideFacilityId == facilityId }
    }
}
