package xyz.ksharma.krail.trip.planner.ui.searchstop.address

import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import kotlin.test.Test
import kotlin.test.assertEquals

class AddressSearchMaxLocalStopsTest {

    private val fakeFlag = FakeFlag()

    private fun setRemote(value: FlagValue) {
        fakeFlag.setFlagValue(FlagKeys.SEARCH_STOP_ADDRESS_MAX_LOCAL_STOPS.key, value)
    }

    @Test
    fun `GIVEN no remote value WHEN resolve THEN default ten`() {
        assertEquals(10, resolveAddressSearchMaxLocalStops(fakeFlag))
    }

    @Test
    fun `GIVEN in-range remote value WHEN resolve THEN that value`() {
        setRemote(FlagValue.NumberValue(3))
        assertEquals(3, resolveAddressSearchMaxLocalStops(fakeFlag))
    }

    @Test
    fun `GIVEN lower bound remote value WHEN resolve THEN that value`() {
        setRemote(FlagValue.NumberValue(0))
        assertEquals(0, resolveAddressSearchMaxLocalStops(fakeFlag))
    }

    @Test
    fun `GIVEN upper bound remote value WHEN resolve THEN that value`() {
        setRemote(FlagValue.NumberValue(50))
        assertEquals(50, resolveAddressSearchMaxLocalStops(fakeFlag))
    }

    @Test
    fun `GIVEN below-range remote value WHEN resolve THEN fallback to default`() {
        setRemote(FlagValue.NumberValue(-1))
        assertEquals(10, resolveAddressSearchMaxLocalStops(fakeFlag))
    }

    @Test
    fun `GIVEN above-range remote value WHEN resolve THEN fallback to default`() {
        setRemote(FlagValue.NumberValue(51))
        assertEquals(10, resolveAddressSearchMaxLocalStops(fakeFlag))
    }

    @Test
    fun `GIVEN wildly oversized remote value WHEN resolve THEN fallback to default not a wrapped Int`() {
        setRemote(FlagValue.NumberValue(Long.MAX_VALUE))
        assertEquals(10, resolveAddressSearchMaxLocalStops(fakeFlag))
    }

    @Test
    fun `GIVEN malformed non-numeric remote value WHEN resolve THEN fallback to default`() {
        setRemote(FlagValue.StringValue("ten"))
        assertEquals(10, resolveAddressSearchMaxLocalStops(fakeFlag))
    }
}
