package xyz.ksharma.krail.feature.track.network.crowding

/**
 * One real stop for each crowding scenario in docs/testing/CROWDING_SCENARIOS.md, from the Sydney
 * Trains trip updates feed (`/v2/gtfs/realtime/sydneytrains`) captured 2026-10-27 at 8:40am.
 *
 * Each stop_time_update is copied byte for byte from the response; only the enclosing entity and
 * trip update lengths are rebuilt, because each kept trip carries only its scenario stops. Two
 * scenarios share a trip, so their stops stay under that trip's one entity, as in the feed.
 * Found with `scripts/crowding_scenarios.py`. "Seats in the middle" and "few seats at both ends"
 * were not on the network at capture time; `CrowdingWordingTest` holds those.
 */
internal object CrowdingScenarioFeedFixture {

    /** One captured stop: the trip, the stop, the cars in feed order (S, F, X) and what the rider sees. */
    data class Scenario(
        val name: String,
        val tripId: String,
        val stopId: String,
        val cars: String,
        val tag: String,
        val sentence: String?,
    )

    val scenarios = listOf(
        Scenario(
            name = "Seats all along",
            tripId = "564F.1324.165.124.M.8.92004199",
            stopId = "2166271",
            cars = "SSSSSSSS",
            tag = "Seats available",
            sentence = "Seats expected all along the train.",
        ),
        Scenario(
            name = "Seats at both ends",
            tripId = "564F.1324.165.124.M.8.92004199",
            stopId = "2150404",
            cars = "SSSFFSSS",
            tag = "Seats at both ends",
            sentence = "Seats expected at both ends.",
        ),
        Scenario(
            name = "Few seats all along",
            tripId = "172G.1324.165.12.A.8.92000951",
            stopId = "2145641",
            cars = "FFFFFFFF",
            tag = "Few seats",
            sentence = "A few seats expected along the train.",
        ),
        Scenario(
            name = "Standing all along",
            tripId = "29-G.1324.165.8.B.8.91999433",
            stopId = "204261",
            cars = "XXXXXXXX",
            tag = "Standing only",
            sentence = "Standing only expected all along the train.",
        ),
        // Only the end car has a few seats; without front and back there is no sentence, and the
        // tag gives the level most cars share while the bars show the better car.
        Scenario(
            name = "Seats at one end only",
            tripId = "29-G.1324.165.8.B.8.91999433",
            stopId = "204991",
            cars = "FXXXXXXX",
            tag = "Standing only",
            sentence = null,
        ),
        // One middle car has a few seats, but each zone counts as its busiest car, so the train
        // reads as standing only all along.
        Scenario(
            name = "One better car inside a full zone",
            tripId = "22-C.1324.165.108.B.8.92003405",
            stopId = "2015111",
            cars = "XXXXXFXX",
            tag = "Standing only",
            sentence = "Standing only expected all along the train.",
        ),
    )

    const val HEX =
        "0a0d0a03322e30100018dd8effd60612ed010a1e353634462e313332342e3136352e3132342e4d2e382e3932" +
            "3030343139391aca010a2a0a1e353634462e313332342e3136352e3132342e4d2e382e393230303431393920" +
            "002a06434d425f3262124d120208001a02080022073231363632373128003001fa3e0410013801fa3e041002" +
            "3801fa3e0410033801fa3e0410043801fa3e0410053801fa3e0410063801fa3e0410073801fa3e0410083801" +
            "124d120208001a02080022073231353034303428003001fa3e0410013801fa3e0410023801fa3e0410033801" +
            "fa3e0410043802fa3e0410053802fa3e0410063801fa3e0410073801fa3e041008380112e7010a1c32392d47" +
            "2e313332342e3136352e382e422e382e39313939393433331ac6010a280a1c32392d472e313332342e313635" +
            "2e382e422e382e393139393934333320002a0649574c5f3261124c120208001a02082a220632303439393128" +
            "003003fa3e0410013802fa3e0410023803fa3e0410033803fa3e0410043803fa3e0410053803fa3e04100638" +
            "03fa3e0410073803fa3e0410083803124c1202082a1a02082a220632303432363128003003fa3e0410013803" +
            "fa3e0410023803fa3e0410033803fa3e0410043803fa3e0410053803fa3e0410063803fa3e0410073803fa3e" +
            "0410083803129b010a1d313732472e313332342e3136352e31322e412e382e39323030303935311a7a0a290a" +
            "1d313732472e313332342e3136352e31322e412e382e393230303039353120002a065753545f3264124d1202" +
            "08001a02080022073231343536343128003002fa3e0410013802fa3e0410023802fa3e0410033802fa3e0410" +
            "043802fa3e0410053802fa3e0410063802fa3e0410073802fa3e0410083802129d010a1e32322d432e313332" +
            "342e3136352e3130382e422e382e39323030333430351a7b0a2a0a1e32322d432e313332342e3136352e3130" +
            "382e422e382e393230303334303520002a064150535f3261124d120208001a02080022073230313531313128" +
            "003003fa3e0410013803fa3e0410023803fa3e0410033803fa3e0410043803fa3e0410053803fa3e04100638" +
            "02fa3e0410073803fa3e0410083803"

    fun bytes(): ByteArray = HEX.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
