package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ksharma.krail.trip.planner.network.api.service.TripPlanningService
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultMapper.toStopResults
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import kotlin.collections.addAll

class SearchStopViewModel(private val tripPlanningService: TripPlanningService) : ViewModel() {

    private val _uiState: MutableStateFlow<SearchStopState> = MutableStateFlow(SearchStopState())
    val uiState: StateFlow<SearchStopState> = _uiState

    private var searchJob: Job? = null

    fun onEvent(event: SearchStopUiEvent) {
        when (event) {
            is SearchStopUiEvent.SearchTextChanged -> onSearchTextChanged(event.query)
        }
    }

    private fun onSearchTextChanged(query: String) {
        // Display local results immediately
        val localResults = processLocalStopResults(query)
        updateUiState { displayData(localResults) }

        // Fetch network results and merge them with local results
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val response = tripPlanningService.stopFinder(stopSearchQuery = query)
                println("response VM: $response")

                val networkResults = response.toStopResults()
                val mergedResults = mergeResults(localResults, networkResults)
                mergedResults.forEach {
                    println("stopId: ${it.stopId}, stopName: ${it.stopName}, transportModeType: ${it.transportModeType}")
                }

                updateUiState { displayData(mergedResults) }
            }.getOrElse {
                delay(1500) // buffer for API response before displaying error.
                updateUiState { displayError() }
            }
        }
    }

    private fun processLocalStopResults(query: String): List<SearchStopState.StopResult> {
        val resultMap = LinkedHashMap<String, SearchStopState.StopResult>()

        // Filter metroStops and trainStops based on the query
        val matchingMetroStops = metroStops.filter { it.value.contains(query, ignoreCase = true) }
        val matchingTrainStops = trainStops.filter { it.value.contains(query, ignoreCase = true) }

        // Create StopResult objects for matching metro stops
        matchingMetroStops.forEach { (id, name) ->
            val existingResult = resultMap[id]
            if (existingResult != null) {
                val combinedModes = (existingResult.transportModeType + TransportMode.Metro()).toPersistentList()
                resultMap[id] = existingResult.copy(transportModeType = combinedModes)
            } else {
                resultMap[id] = SearchStopState.StopResult(
                    stopName = name,
                    stopId = id,
                    transportModeType = persistentListOf(TransportMode.Metro())
                )
            }
        }

        // Create StopResult objects for matching train stops
        matchingTrainStops.forEach { (id, name) ->
            val existingResult = resultMap[id]
            if (existingResult != null) {
                val combinedModes = (existingResult.transportModeType + TransportMode.Train()).toPersistentList()
                resultMap[id] = existingResult.copy(transportModeType = combinedModes)
            } else {
                resultMap[id] = SearchStopState.StopResult(
                    stopName = name,
                    stopId = id,
                    transportModeType = persistentListOf(TransportMode.Train())
                )
            }
        }

        return resultMap.values.toList()
    }

    private fun mergeResults(
        localResults: List<SearchStopState.StopResult>,
        networkResults: List<SearchStopState.StopResult>
    ): List<SearchStopState.StopResult> {
        val resultMap = LinkedHashMap<String, SearchStopState.StopResult>()

        (localResults + networkResults).forEach { result ->
            val existingResult = resultMap[result.stopId]
            if (existingResult != null) {
                val combinedModes = (existingResult.transportModeType + result.transportModeType).toPersistentList()
                resultMap[result.stopId] = existingResult.copy(transportModeType = combinedModes)
            } else {
                resultMap[result.stopId] = result
            }
        }

        return resultMap.values.toList()
    }

    private fun SearchStopState.displayData(stopsResult: List<SearchStopState.StopResult>) = copy(
        stops = stopsResult.toImmutableList(),
        isLoading = false,
        isError = false,
    )

    private fun SearchStopState.displayLoading() =
        copy(isLoading = true, isError = false)

    private fun SearchStopState.displayError() = copy(
        isLoading = false,
        stops = persistentListOf(),
        isError = true,
    )

    private fun updateUiState(block: SearchStopState.() -> SearchStopState) {
        _uiState.update(block)
    }
}

// Metro
val metroStops = mapOf(
    "200030" to "Martin Place Station",
    "200046" to "Barangaroo Station",
    "200060" to "Central Station",
    "200066" to "Gadigal Station",
    "201721" to "Waterloo Station",
    "204420" to "Sydenham Station",
    "206044" to "Victoria Cross Station",
    "206516" to "Crows Nest Station",
    "206710" to "Chatswood Station",
    "211310" to "Macquarie University Station",
    "211320" to "North Ryde Station",
    "211340" to "Macquarie Park Station",
    "212110" to "Epping Station",
    "2126158" to "Cherrybrook Station",
    "2153477" to "Norwest Station",
    "2153478" to "Bella Vista Station",
    "2154391" to "Castle Hill Station",
    "2154392" to "Hills Showground Station",
    "2155382" to "Kellyville Station",
    "2155383" to "Rouse Hill Station",
    "2155384" to "Tallawong Station"
)

// Train Station
val trainStops = mapOf(
    "26401" to "Albury Station",
    "233610" to "Aberdeen Station",
    "222020" to "Allawah Station",
    "252710" to "Albion Park Station",
    "23501" to "Armidale Station",
    "220520" to "Arncliffe Station",
    "206410" to "Artarmon Station",
    "213110" to "Ashfield Station",
    "207730" to "Asquith Station",
    "228920" to "Adamstown Station",
    "214410" to "Auburn Station",
    "251540" to "Austinmer Station",
    "228310" to "Awaba Station",
    "211910" to "Beecroft Station",
    "220710" to "Bardwell Park Station",
    "26211" to "Bungendore Station",
    "208110" to "Berowra Station",
    "257420" to "Bargo Station",
    "278510" to "Blackheath Station",
    "214320" to "Birrong Station",
    "202210" to "Bondi Junction Station",
    "28801" to "Broken Hill Station",
    "221610" to "Banksia Station",
    "27991" to "Blayney Station",
    "278410" to "Bullaburra Station",
    "251610" to "Bulli Station",
    "278620" to "Bell Station",
    "23971" to "Bellata Station",
    "253320" to "Bombo Station",
    "229210" to "Broadmeadow Station",
    "251810" to "Bellambi Station",
    "219210" to "Belmore Station",
    "40001" to "Brisbane Station",
    "220010" to "Bankstown Station",
    "233510" to "Branxton Station",
    "23821" to "Boggabri Station",
    "257620" to "Burradoo Station",
    "228410" to "Booragul Station",
    "257610" to "Bowral Station",
    "214120" to "Berala Station",
    "253510" to "Berry Station",
    "232230" to "Beresfield Station",
    "279538" to "Bathurst Station",
    "214810" to "Blacktown Station",
    "257810" to "Bundanoon Station",
    "257129" to "Buxton Bus",
    "V20030" to "Broadmeadows Station",
    "220910" to "Beverly Hills Station",
    "36721" to "Benalla Station",
    "213410" to "Burwood Station",
    "277410" to "Blaxland Station",
    "220720" to "Bexley North Station",
    "216620" to "Cabramatta Station",
    "217030" to "Casula Station",
    "219320" to "Canterbury Station",
    "250850" to "Coalcliff Station",
    "228430" to "Cockle Creek Station",
    "200020" to "Circular Quay Station",
    "228520" to "Cardiff Station",
    "275630" to "Clarendon Station",
    "200060" to "Central Station",
    "24501" to "Coffs Harbour Station",
    "216220" to "Chester Hill Station",
    "211920" to "Cheltenham Station",
    "251530" to "Coledale Station",
    "214230" to "Clyde Station",
    "221810" to "Carlton Station",
    "251820" to "Corrimal Station",
    "27011" to "Coolamon Station",
    "222610" to "Como Station",
    "219410" to "Campsie Station",
    "26041" to "Canberra Station",
    "222910" to "Caringbah Station",
    "223020" to "Cronulla Station",
    "250030" to "Coniston Station",
    "216610" to "Canley Vale Station",
    "28772" to "Condobolin Station",
    "213810" to "Concord West Station",
    "250520" to "Cringila Station",
    "257165" to "Couridjah Bus",
    "216330" to "Carramar Station",
    "24701" to "Casino Station",
    "25901" to "Cootamundra Station",
    "256020" to "Campbelltown Station",
    "26601" to "Culcairn Station",
    "206710" to "Chatswood Station",
    "208120" to "Cowan Station",
    "213210" to "Croydon Station",
    "253020" to "Dapto Station",
    "28301" to "Dubbo Station",
    "226420" to "Dora Creek Station",
    "220310" to "Dulwich Hill Station",
    "242010" to "Dungog Station",
    "28781" to "Darnick Station",
    "211410" to "Denistone Station",
    "276710" to "Doonside Station",
    "256910" to "Douglas Park Station",
    "202710" to "Edgecliff Station",
    "217426" to "Edmondson Park Station",
    "223310" to "Engadine Station",
    "221320" to "East Hills Station",
    "232330" to "East Maitland Station",
    "24411" to "Eungai Station",
    "212110" to "Epping Station",
    "275020" to "Emu Plains Station",
    "275310" to "East Richmond Station",
    "204310" to "Erskineville Station",
    "28771" to "Euabalong West Station",
    "212210" to "Eastwood Station",
    "257930" to "Exeter Station",
    "277610" to "Faulconbridge Station",
    "216510" to "Fairfield Station",
    "214020" to "Flemington Station",
    "251910" to "Fairy Meadow Station",
    "228330" to "Fassifern Station",
    "277310" to "Glenbrook Station",
    "24221" to "Gloucester Station",
    "23801" to "Gunnedah Station",
    "216710" to "Glenfield Station",
    "258010" to "Goulburn Station",
    "216110" to "Guildford Station",
    "25811" to "Gunning Station",
    "253420" to "Gerringong Station",
    "225040" to "Gosford Station",
    "201710" to "Green Square Station",
    "207210" to "Gordon Station",
    "28311" to "Geurie Station",
    "26801" to "Griffith Station",
    "233410" to "Greta Station",
    "214240" to "Granville Station",
    "222710" to "Gymea Station",
    "230310" to "Hamilton Station",
    "25871" to "Harden Station",
    "207720" to "Hornsby Station",
    "223320" to "Heathcote Station",
    "242040" to "Hilldale Station",
    "250810" to "Helensburgh Station",
    "217310" to "Holsworthy Station",
    "214010" to "Homebush Station",
    "215010" to "Harris Park Station",
    "219310" to "Hurlstone Park Station",
    "208310" to "Hawkesbury River Station",
    "232010" to "High Street Station",
    "26581" to "Henty Station",
    "222010" to "Hurstville Station",
    "232210" to "Hexham Station",
    "277910" to "Hazelbrook Station",
    "256510" to "Ingleburn Station",
    "28782" to "Ivanhoe Station",
    "222620" to "Jannali Station",
    "26631" to "Junee Station",
    "278020" to "Katoomba Station",
    "24391" to "Kendall Station",
    "24401" to "Kempsey Station",
    "220810" to "Kingsgrove Station",
    "201110" to "Kings Cross Station",
    "253330" to "Kiama Station",
    "207110" to "Killara Station",
    "253010" to "Kembla Grange Station",
    "221710" to "Kogarah Station",
    "23521" to "Kootingal Station",
    "223230" to "Kirrawee Station",
    "228910" to "Kotara Station",
    "274720" to "Kingswood Station",
    "225620" to "Koolewong Station",
    "24741" to "Kyogle Station",
    "219510" to "Lakemba Station",
    "275030" to "Lapstone Station",
    "278310" to "Lawson Station",
    "214110" to "Lidcombe Station",
    "207010" to "Lindfield Station",
    "217933" to "Leppington Station",
    "27051" to "Leeton Station",
    "204920" to "Lewisham Station",
    "223220" to "Loftus Station",
    "277820" to "Linden Station",
    "217020" to "Liverpool Station",
    "232110" to "Lochinvar Station",
    "278010" to "Leura Station",
    "225070" to "Lisarow Station",
    "250510" to "Lysaghts Station",
    "279010" to "Lithgow Station",
    "216310" to "Leightonfield Station",
    "256010" to "Leumeah Station",
    "257940" to "Marulan Station",
    "256030" to "Macarthur Station",
    "202010" to "Mascot Station",
    "201520" to "Macdonaldtown Station",
    "211430" to "Meadowbank Station",
    "23381" to "Murrurundi Station",
    "232050" to "Mindaribba Station",
    "277010" to "Mount Druitt Station",
    "278030" to "Medlow Bath Station",
    "232310" to "Metford Station",
    "256810" to "Menangle Station",
    "256310" to "Menangle Park Station",
    "256610" to "Minto Station",
    "279821" to "Millthorpe Station",
    "257510" to "Mittagong Station",
    "208010" to "Mount Kuring-Gai Station",
    "24471" to "Macksville Station",
    "220410" to "Marrickville Station",
    "232020" to "Maitland Station",
    "28791" to "Menindee Station",
    "200030" to "Martin Place Station",
    "206110" to "Milsons Point Station",
    "256410" to "Macquarie Fields Station",
    "222810" to "Miranda Station",
    "24001" to "Moree Station",
    "242050" to "Martins Creek Station",
    "222310" to "Mortdale Station",
    "200040" to "Museum Station",
    "226410" to "Morisset Station",
    "207910" to "Mount Colah Station",
    "278610" to "Mount Victoria Station",
    "275610" to "Mulgrave Station",
    "253310" to "Minnamurra Station",
    "233310" to "MuswellBrook Station",
    "257710" to "Moss Vale Station",
    "214820" to "Marayong Station",
    "216010" to "Merrylands Station",
    "24481" to "Nambucca Heads Station",
    "23901" to "Narrabri Station",
    "27001" to "Narrandera Station",
    "225060" to "Niagara Park Station",
    "207620" to "Normanhurst Station",
    "254110" to "Bomaderry (Nowra) Station",
    "2250772" to "Narara Station",
    "220920" to "Narwee Station",
    "213710" to "North Strathfield Station",
    "206010" to "North Sydney Station",
    "204210" to "Newtown Station",
    "250010" to "North Wollongong Station",
    "229310" to "Newcastle Interchange Station",
    "252920" to "Oak Flats Station",
    "222320" to "Oatley Station",
    "212710" to "Olympic Park Station",
    "28001" to "Orange Station",
    "250830" to "Otford Station",
    "225810" to "Ourimbah Station",
    "221110" to "Padstow Station",
    "221310" to "Panania Station",
    "225030" to "Point Clare Station",
    "60041" to "Perth Station",
    "214530" to "Pendle Hill Station",
    "275010" to "Penrith Station",
    "204910" to "Petersham Station",
    "257110" to "Picton Station",
    "250540" to "Port Kembla Station",
    "250530" to "Port Kembla North Station",
    "28701" to "Parkes Station",
    "222210" to "Penshurst Station",
    "212020" to "Pennant Hills Station",
    "257920" to "Penrose Station",
    "215020" to "Parramatta Station",
    "242110" to "Paterson Station",
    "219610" to "Punchbowl Station",
    "207310" to "Pymble Station",
    "23431" to "Quirindi Station",
    "276310" to "Quakers Hill Station",
    "26201" to "Queanbeyan Station",
    "2795999" to "Raglan Bus",
    "201510" to "Redfern Station",
    "2790128" to "Rydal Station",
    "213820" to "Rhodes Station",
    "275320" to "Richmond Station",
    "276510" to "Riverstone Station",
    "221620" to "Rockdale Station",
    "214310" to "Regents Park Station",
    "206910" to "Roseville Station",
    "2577146" to "Robertson Bus",
    "276610" to "Rooty Hill Station",
    "221210" to "Revesby Station",
    "221010" to "Riverwood Station",
    "230430" to "Sandgate Station",
    "2577209" to "Ranelagh House Bus",
    "233710" to "Scone Station",
    "276220" to "Schofields Station",
    "251510" to "Scarborough Station",
    "202020" to "Domestic Airport Station",
    "V17191" to "Seymour Station",
    "216210" to "Sefton Station",
    "233010" to "Singleton Station",
    "2529218" to "Shellharbour Junction Station",
    "252910" to "Dunmore Station",
    "V22180" to "Southern Cross Station",
    "202030" to "International Airport Station",
    "213010" to "Summer Hill Station",
    "204810" to "Stanmore Station",
    "24601" to "Grafton Station",
    "277720" to "Springwood Station",
    "223210" to "Sutherland Station",
    "200050" to "St James Station",
    "206520" to "St Leonards Station",
    "276010" to "St Marys Station",
    "204410" to "St Peters Station",
    "213510" to "Strathfield Station",
    "28201" to "Stuart Town Station",
    "214710" to "Seven Hills Station",
    "250840" to "Stanwell Park Station",
    "24521" to "Sawtell Station",
    "204420" to "Sydenham Station",
    "257310" to "Tahmoor Station",
    "257950" to "Tallong Station",
    "234012" to "Tamworth Station",
    "27871" to "Tarana Station",
    "225020" to "Tascott Station",
    "228420" to "Teralba Station",
    "214610" to "Toongabbie Station",
    "258011" to "Tarago Station",
    "251550" to "Thirroul Station",
    "200070" to "Town Hall Station",
    "257225" to "Thirlmere Bus",
    "212010" to "Thornleigh Station",
    "232030" to "Telarah Station",
    "204430" to "Tempe Station",
    "232240" to "Thornton Station",
    "24301" to "Taree Station",
    "26551" to "The Rock Station",
    "220530" to "Turrella Station",
    "232220" to "Tarro Station",
    "225910" to "Tuggerah Station",
    "207410" to "Turramurra Station",
    "251830" to "Towradgi Station",
    "252610" to "Unanderra Station",
    "24551" to "Urunga Station",
    "235813" to "Uralla Station",
    "277710" to "Valley Heights Station",
    "216320" to "Villawood Station",
    "276520" to "Vineyard Station",
    "232320" to "Victoria Street Station",
    "207610" to "Wahroonga Station",
    "207710" to "Waitara Station",
    "206020" to "Waverton Station",
    "251520" to "Wombarra Station",
    "220510" to "Wolli Creek Station",
    "23411" to "Werris Creek Station",
    "225010" to "Wondabyne Station",
    "277810" to "Woodford Station",
    "223330" to "Waterfall Station",
    "278210" to "Wentworth Falls Station",
    "26501" to "Wagga Wagga Station",
    "24291" to "Wingham Station",
    "257910" to "Wingello Station",
    "275620" to "Windsor Station",
    "242030" to "Wirragulla Station",
    "217010" to "Warwick Farm Station",
    "242020" to "Wallarobba Station",
    "23541" to "Walcha Road Station",
    "206510" to "Wollstonecraft Station",
    "223010" to "Woolooware Station",
    "219520" to "Wiley Park Station",
    "214510" to "Westmead Station",
    "2845999" to "Wallerawang Bus",
    "250020" to "Wollongong Station",
    "251710" to "Woonona Station",
    "225610" to "Woy Woy Station",
    "211420" to "West Ryde Station",
    "225930" to "Warnervale Station",
    "274710" to "Werrington Station",
    "230420" to "Warabrook Station",
    "277420" to "Warrimoo Station",
    "207420" to "Warrawee Station",
    "23391" to "Willow Tree Station",
    "230410" to "Waratah Station",
    "28202" to "Wellington Station",
    "24461" to "Wauchope Station",
    "214520" to "Wentworthville Station",
    "36771" to "Wangaratta Station",
    "225940" to "Wyee Station",
    "225920" to "Wyong Station",
    "200080" to "Wynyard Station",
    "219910" to "Yagoona Station",
    "25821" to "Yass Junction Station",
    "216120" to "Yennora Station",
    "257540" to "Yerrinbool Station",
    "278630" to "Zig Zag Station"
)
