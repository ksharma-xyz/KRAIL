package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop

// Real stops with their real ids and names from the bundled stops data, so the lists are held to what riders get
// and not to invented rows. A name with a comma is how the data names a stand or a platform of a station.
internal val CENTRAL_STOP = IntroStop("200060", "Central Station")
internal val TOWN_HALL_STOP = IntroStop("200070", "Town Hall Station")
internal val WYNYARD_STOP = IntroStop("200080", "Wynyard Station")
internal val MARTIN_PLACE_STOP = IntroStop("200030", "Martin Place Station")
internal val PARRAMATTA_STOP = IntroStop("215020", "Parramatta Station")
internal val REDFERN_STOP = IntroStop("201510", "Redfern Station")
internal val MUSEUM_STOP = IntroStop("200040", "Museum Station")
internal val ST_JAMES_STOP = IntroStop("200050", "St James Station")
internal val SYDENHAM_STOP = IntroStop("204420", "Sydenham Station")
internal val TALLAWONG_STOP = IntroStop("2155384", "Tallawong Station")
internal val SCHOFIELDS_STOP = IntroStop("276220", "Schofields Station")
internal val ROUSE_HILL_STOP = IntroStop("2155383", "Rouse Hill Station")
internal val KELLYVILLE_STOP = IntroStop("2155382", "Kellyville Station")
internal val CHATSWOOD_STOP = IntroStop("206710", "Chatswood Station")
internal val CHATSWOOD_STAND_A = IntroStop("206720", "Chatswood Station, Stand A")
internal val CHATSWOOD_STAND_B = IntroStop("206718", "Chatswood Station, Stand B")
internal val JUNIORS_KINGSFORD = IntroStop("203294", "Juniors Kingsford Light Rail")
internal val JUNIORS_KINGSFORD_STAND_A = IntroStop("203290", "Juniors Kingsford Light Rail, Stand A")
internal val JUNIORS_KINGSFORD_STAND_B = IntroStop("203292", "Juniors Kingsford Light Rail, Stand B")

internal fun IntroStop.asSuggested() = SuggestedStop(stopId = stopId, name = name)
