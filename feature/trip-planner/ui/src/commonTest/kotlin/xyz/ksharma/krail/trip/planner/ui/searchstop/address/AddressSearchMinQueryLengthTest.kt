package xyz.ksharma.krail.trip.planner.ui.searchstop.address

import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import kotlin.test.Test
import kotlin.test.assertEquals

class AddressSearchMinQueryLengthTest {

    private val fakeFlag = FakeFlag()

    @Test
    fun `GIVEN no remote value WHEN resolve THEN default six`() {
        assertEquals(6, resolveAddressSearchMinQueryLength(fakeFlag))
    }

    @Test
    fun `GIVEN in-range remote value WHEN resolve THEN that value`() {
        fakeFlag.setFlagValue(
            FlagKeys.SEARCH_STOP_ADDRESS_MIN_QUERY_LENGTH.key,
            FlagValue.NumberValue(8),
        )
        assertEquals(8, resolveAddressSearchMinQueryLength(fakeFlag))
    }

    @Test
    fun `GIVEN lower bound remote value WHEN resolve THEN that value`() {
        fakeFlag.setFlagValue(
            FlagKeys.SEARCH_STOP_ADDRESS_MIN_QUERY_LENGTH.key,
            FlagValue.NumberValue(2),
        )
        assertEquals(2, resolveAddressSearchMinQueryLength(fakeFlag))
    }

    @Test
    fun `GIVEN upper bound remote value WHEN resolve THEN that value`() {
        fakeFlag.setFlagValue(
            FlagKeys.SEARCH_STOP_ADDRESS_MIN_QUERY_LENGTH.key,
            FlagValue.NumberValue(12),
        )
        assertEquals(12, resolveAddressSearchMinQueryLength(fakeFlag))
    }

    @Test
    fun `GIVEN below-range remote value WHEN resolve THEN fallback to default`() {
        fakeFlag.setFlagValue(
            FlagKeys.SEARCH_STOP_ADDRESS_MIN_QUERY_LENGTH.key,
            FlagValue.NumberValue(1),
        )
        assertEquals(6, resolveAddressSearchMinQueryLength(fakeFlag))
    }

    @Test
    fun `GIVEN above-range remote value WHEN resolve THEN fallback to default`() {
        fakeFlag.setFlagValue(
            FlagKeys.SEARCH_STOP_ADDRESS_MIN_QUERY_LENGTH.key,
            FlagValue.NumberValue(13),
        )
        assertEquals(6, resolveAddressSearchMinQueryLength(fakeFlag))
    }

    @Test
    fun `GIVEN wildly oversized remote value WHEN resolve THEN fallback to default not a wrapped Int`() {
        fakeFlag.setFlagValue(
            FlagKeys.SEARCH_STOP_ADDRESS_MIN_QUERY_LENGTH.key,
            FlagValue.NumberValue(Long.MAX_VALUE),
        )
        assertEquals(6, resolveAddressSearchMinQueryLength(fakeFlag))
    }

    @Test
    fun `GIVEN malformed non-numeric remote value WHEN resolve THEN fallback to default`() {
        fakeFlag.setFlagValue(
            FlagKeys.SEARCH_STOP_ADDRESS_MIN_QUERY_LENGTH.key,
            FlagValue.StringValue("six"),
        )
        assertEquals(6, resolveAddressSearchMinQueryLength(fakeFlag))
    }
}
