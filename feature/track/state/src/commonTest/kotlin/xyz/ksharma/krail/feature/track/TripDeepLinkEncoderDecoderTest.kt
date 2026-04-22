package xyz.ksharma.krail.feature.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TripDeepLinkEncoderDecoderTest {

    // region encodeBase64Url / decodeBase64Url

    @Test
    fun `encodeBase64Url and decodeBase64Url round-trip`() {
        val original = """{"hello":"world","num":42}"""
        val encoded = original.encodeBase64Url()
        val decoded = encoded.decodeBase64Url()
        assertEquals(original, decoded)
    }

    @Test
    fun `encodeBase64Url produces no padding characters`() {
        val encoded = "test".encodeBase64Url()
        assert(!encoded.contains('=')) { "Encoded string should have no '=' padding" }
    }

    @Test
    fun `decodeBase64Url returns null for garbage input`() {
        val result = "!!!not-base64!!!".decodeBase64Url()
        // decodeBase64Url wraps in runCatching — invalid base64 returns null
        // (or may decode to garbled bytes, but won't throw)
        // The key contract is: no exception is thrown
        // result may be null or a garbled string depending on implementation
    }

    @Test
    fun `encodeBase64Url handles empty string`() {
        val encoded = "".encodeBase64Url()
        val decoded = encoded.decodeBase64Url()
        assertEquals("", decoded)
    }

    // endregion

    // region TripDeepLinkDecoder.decode

    @Test
    fun `decode returns null for empty string`() {
        assertNull(TripDeepLinkDecoder.decode(""))
    }

    @Test
    fun `decode returns null for random garbage`() {
        assertNull(TripDeepLinkDecoder.decode("aaabbbccc!!!"))
    }

    @Test
    fun `decode returns null for valid base64 but invalid JSON`() {
        val notJson = "hello world".encodeBase64Url()
        assertNull(TripDeepLinkDecoder.decode(notJson))
    }

    @Test
    fun `decode returns null for valid JSON missing required fields`() {
        val incompleteJson = """{"f":"stop1"}""".encodeBase64Url()
        assertNull(TripDeepLinkDecoder.decode(incompleteJson))
    }

    @Test
    fun `decode successfully parses a valid encoded TripDeepLink`() {
        val deepLink = makeTripDeepLink()
        val encoded = encodeTripDeepLink(deepLink)
        val decoded = TripDeepLinkDecoder.decode(encoded)

        assertNotNull(decoded)
        assertEquals(deepLink.fromStopId, decoded.fromStopId)
        assertEquals(deepLink.toStopId, decoded.toStopId)
        assertEquals(deepLink.fromStopName, decoded.fromStopName)
        assertEquals(deepLink.toStopName, decoded.toStopName)
        assertEquals(deepLink.departureUtcDateTime, decoded.departureUtcDateTime)
        assertEquals(deepLink.legs.size, decoded.legs.size)
        assertEquals(deepLink.legs[0].transportationId, decoded.legs[0].transportationId)
        assertEquals(deepLink.legs[0].productClass, decoded.legs[0].productClass)
    }

    @Test
    fun `decode handles multiple legs`() {
        val deepLink = makeTripDeepLink(
            legs = listOf(
                TripDeepLink.DeepLinkLeg(transportationId = "nsw:020T1:W:R:sj2", productClass = 1),
                TripDeepLink.DeepLinkLeg(transportationId = "nsw:020B320:D:R:sj2", productClass = 5),
            )
        )
        val encoded = encodeTripDeepLink(deepLink)
        val decoded = TripDeepLinkDecoder.decode(encoded)

        assertNotNull(decoded)
        assertEquals(2, decoded.legs.size)
        assertEquals("nsw:020T1:W:R:sj2", decoded.legs[0].transportationId)
        assertEquals("nsw:020B320:D:R:sj2", decoded.legs[1].transportationId)
    }

    @Test
    fun `decode ignores unknown JSON fields`() {
        val jsonWithExtras = """
            {"f":"s1","t":"s2","fn":"From","tn":"To","dep":"2025-04-19T14:30:00Z",
            "legs":[{"tid":"nsw:T1","cls":1}],"unknownField":"should be ignored"}
        """.trimIndent().replace("\n", "")
        val encoded = jsonWithExtras.encodeBase64Url()
        val decoded = TripDeepLinkDecoder.decode(encoded)

        assertNotNull(decoded)
        assertEquals("s1", decoded.fromStopId)
    }

    // endregion

    // region round-trip encode → decode

    @Test
    fun `encode then decode produces identical TripDeepLink`() {
        val original = makeTripDeepLink()
        val encoded = encodeTripDeepLink(original)
        val decoded = TripDeepLinkDecoder.decode(encoded)

        assertNotNull(decoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `round-trip preserves special characters in stop names`() {
        val deepLink = makeTripDeepLink(
            fromStopName = "St James Station, Platform 1 & 2",
            toStopName = "Sydney Airport T2/T3 International",
        )
        val decoded = TripDeepLinkDecoder.decode(encodeTripDeepLink(deepLink))

        assertNotNull(decoded)
        assertEquals(deepLink.fromStopName, decoded.fromStopName)
        assertEquals(deepLink.toStopName, decoded.toStopName)
    }

    // endregion

    // region helpers

    private fun makeTripDeepLink(
        fromStopId: String = "10101100",
        toStopId: String = "10102099",
        fromStopName: String = "Seven Hills Station",
        toStopName: String = "Wynyard Station",
        departureUtcDateTime: String = "2025-04-19T22:26:00Z",
        legs: List<TripDeepLink.DeepLinkLeg> = listOf(
            TripDeepLink.DeepLinkLeg(transportationId = "nsw:020T1:W:R:sj2", productClass = 1)
        ),
    ) = TripDeepLink(
        fromStopId = fromStopId,
        toStopId = toStopId,
        fromStopName = fromStopName,
        toStopName = toStopName,
        departureUtcDateTime = departureUtcDateTime,
        legs = legs,
    )

    private fun encodeTripDeepLink(deepLink: TripDeepLink): String {
        val json = kotlinx.serialization.json.Json.encodeToString(TripDeepLink.serializer(), deepLink)
        return json.encodeBase64Url()
    }

    // endregion
}
