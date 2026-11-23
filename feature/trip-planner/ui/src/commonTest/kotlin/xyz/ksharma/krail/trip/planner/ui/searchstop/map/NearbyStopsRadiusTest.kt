package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import xyz.ksharma.krail.core.maps.state.NearbyStopsConfig
import xyz.ksharma.krail.core.remoteconfig.RemoteConfigDefaults
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import kotlin.test.Test
import kotlin.test.assertEquals

class NearbyStopsRadiusTest {

    private val default = NearbyStopsConfig.DEFAULT_RADIUS_KM

    private fun resolved(value: FlagValue?): Double {
        val flag = FakeFlag()
        value?.let { flag.setFlagValue(FlagKeys.NEARBY_STOPS_RADIUS_KM.key, it) }
        return resolveNearbyStopsRadiusKm(flag)
    }

    @Test
    fun `the default is three kilometres and the Remote Config default says the same number`() {
        val remoteDefault = RemoteConfigDefaults.getDefaults()
            .single { it.first == FlagKeys.NEARBY_STOPS_RADIUS_KM.key }
            .second

        assertEquals(3.0, default)
        assertEquals(default, remoteDefault, "The two copies of the default must be the same number.")
    }

    @Test
    fun `a whole number and a Double that arrives as text are both read`() {
        assertEquals(1.0, resolved(FlagValue.NumberValue(1)))
        assertEquals(5.0, resolved(FlagValue.NumberValue(5)))
        assertEquals(3.0, resolved(FlagValue.StringValue("3.0")))
        assertEquals(2.5, resolved(FlagValue.StringValue("2.5")))
        assertEquals(2.5, resolved(FlagValue.StringValue("  2.5 ")))
    }

    @Test
    fun `the edges of the range are in and a step past either is out`() {
        assertEquals(0.5, resolved(FlagValue.StringValue("0.5")))
        assertEquals(10.0, resolved(FlagValue.StringValue("10.0")))
        assertEquals(10.0, resolved(FlagValue.NumberValue(10)))
        assertEquals(default, resolved(FlagValue.StringValue("0.49")))
        assertEquals(default, resolved(FlagValue.StringValue("10.01")))
        assertEquals(default, resolved(FlagValue.NumberValue(11)))
        assertEquals(default, resolved(FlagValue.NumberValue(0)))
    }

    @Test
    fun `a value that is out of range is ignored as a whole, never pulled into range`() {
        assertEquals(default, resolved(FlagValue.NumberValue(300)))
        assertEquals(default, resolved(FlagValue.NumberValue(-1)))
        assertEquals(default, resolved(FlagValue.StringValue("-3.0")))
    }

    @Test
    fun `text that is no number, and every other kind of value, give the default`() {
        listOf("", "   ", "far", "3km", "NaN", "Infinity", "-Infinity").forEach { text ->
            assertEquals(default, resolved(FlagValue.StringValue(text)), "text '$text'")
        }
        assertEquals(default, resolved(FlagValue.BooleanValue(false)))
        assertEquals(default, resolved(FlagValue.BooleanValue(true)))
        assertEquals(default, resolved(FlagValue.JsonValue("{\"km\":2}")))
    }

    @Test
    fun `a client that has fetched nothing reads the default`() {
        // FakeFlag answers an unknown key with a boolean, which is the shape a real unfetched key
        // has no business being; the reader must not be fooled by it.
        assertEquals(default, resolved(null))
    }
}
