package xyz.ksharma.krail.trip.planner.ui.parkride.suggestion

import xyz.ksharma.krail.core.remoteconfig.RemoteConfigDefaults
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import kotlin.test.Test
import kotlin.test.assertEquals

class ParkRideSuggestionRadiusTest {

    private val default = PARK_RIDE_SUGGESTION_DEFAULT_RADIUS_KM

    private fun resolved(value: FlagValue?): Double {
        val flag = FakeFlag()
        value?.let { flag.setFlagValue(FlagKeys.PARK_RIDE_SUGGESTION_RADIUS_KM.key, it) }
        return resolveParkRideSuggestionRadiusKm(flag)
    }

    @Test
    fun `the default is five kilometres and the Remote Config default says the same number`() {
        val remoteDefault = RemoteConfigDefaults.getDefaults()
            .single { it.first == FlagKeys.PARK_RIDE_SUGGESTION_RADIUS_KM.key }
            .second

        assertEquals(5.0, default)
        assertEquals(default, remoteDefault, "The two copies of the default must be the same number.")
    }

    @Test
    fun `the range is half a kilometre to ten`() {
        assertEquals(0.5, PARK_RIDE_SUGGESTION_MIN_RADIUS_KM)
        assertEquals(10.0, PARK_RIDE_SUGGESTION_MAX_RADIUS_KM)
    }

    @Test
    fun `a whole number and a Double that arrives as text are both read`() {
        assertEquals(1.0, resolved(FlagValue.NumberValue(1)))
        assertEquals(7.0, resolved(FlagValue.NumberValue(7)))
        assertEquals(5.0, resolved(FlagValue.StringValue("5.0")))
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
        assertEquals(default, resolved(FlagValue.NumberValue(500)))
        assertEquals(default, resolved(FlagValue.NumberValue(-1)))
        assertEquals(default, resolved(FlagValue.StringValue("-5.0")))
    }

    @Test
    fun `text that is no number, and every other kind of value, give the default`() {
        listOf("", "   ", "near", "5km", "NaN", "Infinity", "-Infinity").forEach { text ->
            assertEquals(default, resolved(FlagValue.StringValue(text)), "text '$text'")
        }
        assertEquals(default, resolved(FlagValue.BooleanValue(false)))
        assertEquals(default, resolved(FlagValue.BooleanValue(true)))
        assertEquals(default, resolved(FlagValue.JsonValue("{\"km\":2}")))
    }

    @Test
    fun `a client that has fetched nothing reads the default`() {
        // FakeFlag answers an unknown key with a boolean, which is not the shape a real key has.
        assertEquals(default, resolved(null))
    }
}
