package xyz.ksharma.krail.trip.planner.ui.searchstop

import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.sandook.NswBusRouteVariants
import xyz.ksharma.krail.sandook.NswBusRoutesSandook
import xyz.ksharma.krail.sandook.NswBusTripOptions
import xyz.ksharma.krail.sandook.SelectStopsByTripId

/** Map-backed [Flag] for tests. Throws on unregistered keys so missing setup fails loudly. */
internal class FakeFlag(private val values: Map<String, FlagValue>) : Flag {
    override fun getFlagValue(key: String): FlagValue =
        values[key] ?: error("FakeFlag: no value registered for key '$key'")
}

/** Bus-routes sandook that errors on every call. Use when the test path doesn't touch routes. */
internal object NoOpBusRoutes : NswBusRoutesSandook {
    override fun selectRouteByShortName(routeShortName: String): String? = error("not used")
    override fun selectRouteVariantsByShortName(routeShortName: String): List<NswBusRouteVariants> = error("not used")
    override fun selectTripsByRouteId(routeId: String): List<NswBusTripOptions> = error("not used")
    override fun selectTripsByRouteIds(routeIds: List<String>): List<NswBusTripOptions> = error("not used")
    override fun selectStopsByTripId(tripId: String): List<SelectStopsByTripId> = error("not used")
    override fun busRouteGroupsCount(): Int = error("not used")
    override fun insertBusRouteGroup(routeShortName: String) = error("not used")
    override fun insertBusRouteVariant(routeId: String, routeShortName: String, routeName: String) = error("not used")
    override fun insertBusTripOption(tripId: String, routeId: String, headsign: String) = error("not used")
    override fun insertBusTripStop(tripId: String, stopId: String, stopSequence: Int) = error("not used")
    override fun clearNswBusRoutesData() = error("not used")
    override fun insertTransaction(body: () -> Unit) = error("not used")
}
