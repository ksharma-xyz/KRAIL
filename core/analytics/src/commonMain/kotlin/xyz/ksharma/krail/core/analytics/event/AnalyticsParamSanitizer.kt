package xyz.ksharma.krail.core.analytics.event

/**
 * Last line of defence applied to every [AnalyticsEvent]'s properties before they leave
 * the app. Two failure modes it exists to stop:
 *
 * 1. **Firebase silently drops any String parameter longer than 100 characters.** The
 *    event still lands, the parameter simply is not there, so a dashboard reads
 *    "this feature is unused" instead of "this parameter was rejected". Address/POI
 *    selections hit exactly this: an EFA `streetID:...` id is 120+ characters, so
 *    `stop_selected` rows for addresses arrived with no `stopId` at all.
 * 2. **Address ids and address display names are personal data.** An EFA address id
 *    embeds the street, suburb and postcode as plain text
 *    (`streetID:1500000015:123:...:Example St:Parramatta:...:2150:...`), and the matching
 *    display name is the address itself. Sending either would undo the search-query
 *    redaction the app already does - a home address is exactly what must never reach
 *    analytics.
 *
 * Transit stop ids are unaffected: they are short and non-identifying, so they pass
 * through verbatim and every existing stop dashboard keeps working. An id is treated as
 * identifying when it is namespaced with a colon (`streetID:`, `poiID:`, `coord:`) or
 * exceeds the parameter limit - structural rather than prefix matching, so an unknown
 * future EFA namespace is redacted too rather than leaking until someone notices.
 */
internal object AnalyticsParamSanitizer {

    /** Firebase's hard limit for a String parameter value; longer values are dropped. */
    const val MAX_PARAM_VALUE_LENGTH = 100

    /**
     * Substituted for a stop name that belongs to an address/POI, so the row stays
     * countable without carrying the address text.
     */
    const val REDACTED_LOCATION_NAME = "address"

    /** Prefix marking a value as a hashed location id rather than a real stop id. */
    const val HASHED_ID_PREFIX = "addr_"

    private const val NAMESPACE_SEPARATOR = ':'

    private val LOCATION_ID_KEYS = setOf("stopId", "fromStopId", "toStopId")

    private val LOCATION_NAME_KEYS = setOf("stopName")

    private const val FNV_OFFSET_BASIS = -3_750_763_034_362_895_579L // 0xcbf29ce484222325
    private const val FNV_PRIME = 1_099_511_628_211L
    private const val HEX_RADIX = 16
    private const val HASH_HEX_LENGTH = 16

    fun sanitize(properties: Map<String, Any>): Map<String, Any> {
        val hasAddressId = properties.any { (key, value) ->
            key in LOCATION_ID_KEYS && value is String && value.isAddressId()
        }
        return properties.mapValues { (key, value) ->
            when {
                value !is String -> value
                key in LOCATION_ID_KEYS -> locationId(value)
                key in LOCATION_NAME_KEYS && hasAddressId -> REDACTED_LOCATION_NAME
                value.length > MAX_PARAM_VALUE_LENGTH -> value.take(MAX_PARAM_VALUE_LENGTH)
                else -> value
            }
        }
    }

    /**
     * Returns [rawId] unchanged for transit stops, or a stable short hash for an
     * address/POI id. The hash is deterministic across platforms and app launches, so
     * "which addresses are picked most" stays answerable while the address text does not.
     */
    fun locationId(rawId: String): String =
        if (rawId.isAddressId()) HASHED_ID_PREFIX + hash(rawId) else rawId

    private fun String.isAddressId(): Boolean =
        contains(NAMESPACE_SEPARATOR) || length > MAX_PARAM_VALUE_LENGTH

    /** FNV-1a 64-bit - no crypto dependency needed, and identical on Android and iOS. */
    private fun hash(value: String): String {
        var hash = FNV_OFFSET_BASIS
        for (char in value) {
            hash = hash xor char.code.toLong()
            hash *= FNV_PRIME
        }
        return hash.toULong().toString(HEX_RADIX).padStart(HASH_HEX_LENGTH, '0')
    }
}
