package xyz.ksharma.krail.core.transport.nsw

/**
 * NSW Transport line catalogue with official brand colours.
 *
 * References:
 *   https://en.wikipedia.org/wiki/Module:Adjacent_stations/Sydney_Trains
 *   https://transportnsw.info/routes/details/newcastle-light-rail/nlr/
 *   https://opendata.transport.nsw.gov.au/resources (colour palette)
 */
enum class NswTransportLine(val key: String, val hexColor: String) {
    // Trains
    NORTH_SHORE_WESTERN("T1", "#F99D1C"),
    LEPPINGTON_INNER_WEST("T2", "#0098CD"),
    LIVERPOOL_INNER_WEST("T3", "#F37021"),
    EASTERN_SUBURBS_ILLAWARRA("T4", "#005AA3"),
    CUMBERLAND("T5", "#C4258F"),
    LIDCOMBE_BANKSTOWN("T6", "#7D3F21"), // future
    OLYMPIC_PARK("T7", "#6F818E"),
    AIRPORT_SOUTH("T8", "#00954C"),
    NORTHERN("T9", "#D11F2F"),
    BLUE_MOUNTAINS("BMT", "#F99D1C"),
    CENTRAL_COAST_NEWCASTLE("CCN", "#D11F2F"),
    HUNTER("HUN", "#833134"),
    SOUTH_COAST("SCO", "#005AA3"),
    SOUTHERN_HIGHLANDS("SHL", "#00954C"),

    // Ferries
    F1_MANLY("F1", "#00774B"),
    F2_TARONGA_ZOO("F2", "#144734"),
    F3_PARRAMATTA_RIVER("F3", "#648C3C"),
    F4_PYRMONT_BAY("F4", "#BFD730"),
    F5_NEUTRAL_BAY("F5", "#286142"),
    F6_MOSMAN_BAY("F6", "#00AB51"),
    F7_DOUBLE_BAY("F7", "#00B189"),
    F8_COCKATOO_ISLAND("F8", "#55622B"),
    F9_WATSONS_BAY("F9", "#65B32E"),
    F10_BLACKWATTLE_BAY("F10", "#5AB031"),
    STKN_STOCKTON("Stkn", "#5AB031"),

    // Light Rail
    L1_DULWICH_HILL_LINE("L1", "#BE1622"),
    L2_RANDWICK_LINE("L2", "#DD1E25"),
    L3_KINGSFORD_LINE("L3", "#781140"),
    NLR_NEWCASTLE_LIGHT_RAIL("NLR", "#EE343F"),
}
