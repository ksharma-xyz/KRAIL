package xyz.ksharma.krail.feature.track.network.crowding

/**
 * One trip from the real Sydney Trains trip updates feed (`/v2/gtfs/realtime/sydneytrains`),
 * captured 2026-10-26 at 10:24pm. Raw bytes sliced from the response, never re-encoded, so
 * the decode test proves Wire reads what TfNSW actually sends.
 *
 * Trip 204R.1324.165.12.H.8.92002332, two stops, eight cars each:
 *  - 2141313: whole train FEW (6 = 2); cars 1 and 8 MANY_SEATS, cars 2 to 7 FEW_SEATS
 *  - 2135232: whole train MANY (6 = 1); all eight cars MANY_SEATS
 */
internal object SydneyTrainsTripUpdateFixture {
    const val TRIP_ID = "204R.1324.165.12.H.8.92002332"

    const val HEX =
        "0a0d0a03322e30100018f3f0fcd60612eb010a1d323034522e313332342e3136352e31322e482e382e393230" +
            "30323333321ac9010a290a1d323034522e313332342e3136352e31322e482e382e393230303233333220002a" +
            "065753545f3263124d120208001a02080022073231343133313328003002fa3e0410013801fa3e0410023802" +
            "fa3e0410033802fa3e0410043802fa3e0410053802fa3e0410063802fa3e0410073802fa3e0410083801124d" +
            "120208001a02080022073231333532333228003001fa3e0410013801fa3e0410023801fa3e0410033801fa3e" +
            "0410043801fa3e0410053801fa3e0410063801fa3e0410073801fa3e0410083801"

    fun bytes(): ByteArray = HEX.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
