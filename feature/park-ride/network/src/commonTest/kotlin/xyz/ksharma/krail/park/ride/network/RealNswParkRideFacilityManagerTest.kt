package xyz.ksharma.krail.park.ride.network

import xyz.ksharma.krail.core.remoteconfig.RemoteConfigDefaults
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RealNswParkRideFacilityManagerTest {

    private fun managerReading(value: FlagValue) = RealNswParkRideFacilityManager(
        flag = object : Flag {
            override fun getFlagValue(key: String): FlagValue = value
        },
    )

    @Test
    fun theBundledDefaultIsAUsableCatalogue() {
        val bundled = RemoteConfigDefaults.getDefaults()
            .first { it.first == FlagKeys.NSW_PARK_RIDE_FACILITIES.key }
            .second as String

        val facilities = managerReading(FlagValue.JsonValue(bundled)).getParkRideFacilities()

        assertTrue(facilities.isNotEmpty(), "The bundled default must hold stations")
        assertTrue(facilities.all { it.stopId.isNotBlank() && it.parkRideFacilityId.isNotBlank() })
    }

    @Test
    fun aRemoteValueIsReadAsItIs() {
        val json = """[{"stopId":"1","parkRideFacilityId":"2","parkRideName":"Somewhere"}]"""

        val facilities = managerReading(FlagValue.JsonValue(json)).getParkRideFacilities()

        assertEquals(listOf("Somewhere"), facilities.map { it.parkRideName })
    }

    @Test
    fun anEmptyValueGivesAnEmptyListAndDoesNotThrow() {
        assertEquals(emptyList(), managerReading(FlagValue.StringValue("")).getParkRideFacilities())
        assertEquals(emptyList(), managerReading(FlagValue.JsonValue("[]")).getParkRideFacilities())
    }
}
