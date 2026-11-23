package xyz.ksharma.krail.trip.planner.ui.intro

import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Buffer
import xyz.ksharma.krail.core.remoteconfig.JsonConfig
import xyz.ksharma.krail.core.remoteconfig.RemoteConfigDefaults
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.park.ride.network.model.NswParkRideFacility
import xyz.ksharma.krail.trip.planner.ui.parkride.groupIntoStations
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.StationPosition
import java.io.File

internal class BundledStop(
    val stopId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    /** Absent from the file means a parent station, an explicit false means a platform or stand. */
    val isParent: Boolean,
)

/**
 * The real stops file and the real default Park & Ride catalogue, so the Intro's logic is run on the
 * data riders get and not on hand-made rows.
 *
 * Decoded with the wire reader and not with the generated `NswStop`, because that class cannot tell
 * an absent `isParent` from an explicit false.
 */
internal object BundledStops {

    private const val STOPS_PATH = "io/gtfs/src/commonMain/composeResources/files/NSW_STOPS.pb"
    private const val TAG_STOP_ID = 1
    private const val TAG_STOP_NAME = 2
    private const val TAG_LATITUDE = 3
    private const val TAG_LONGITUDE = 4
    private const val TAG_IS_PARENT = 6

    val byId: Map<String, BundledStop> by lazy { decode(stopsFile().readBytes()).associateBy { it.stopId } }

    fun name(stopId: String): String? = byId[stopId]?.name

    fun position(stopId: String): StationPosition? =
        byId[stopId]?.let { StationPosition(it.latitude, it.longitude) }

    /** The default catalogue grouped by the same code the app runs, with coordinates from the file. */
    fun defaultStations(): List<ParkRideStationPickerItem> = defaultFacilities().groupIntoStations(
        stopNameLookup = ::name,
        positionLookup = { stopIds -> stopIds.mapNotNull { id -> position(id)?.let { id to it } }.toMap() },
    )

    fun defaultFacilities(): List<NswParkRideFacility> {
        val json = RemoteConfigDefaults.getDefaults()
            .first { it.first == FlagKeys.NSW_PARK_RIDE_FACILITIES.key }
            .second as String
        return JsonConfig.lenient.parseToJsonElement(json).jsonArray.map { row ->
            val fields = row.jsonObject
            NswParkRideFacility(
                stopId = fields.getValue("stopId").jsonPrimitive.content,
                parkRideFacilityId = fields.getValue("parkRideFacilityId").jsonPrimitive.content,
                parkRideName = fields.getValue("parkRideName").jsonPrimitive.content,
            )
        }
    }

    // The tests run with the module directory as the working directory, so walk up to the repo root.
    private fun stopsFile(): File = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, STOPS_PATH) }
        .firstOrNull { it.exists() }
        ?: error("Cannot find $STOPS_PATH above ${File("").absolutePath}")

    private fun decode(bytes: ByteArray): List<BundledStop> {
        val reader = ProtoReader(Buffer().write(bytes))
        val stops = mutableListOf<BundledStop>()
        val listToken = reader.beginMessage()
        while (true) {
            val tag = reader.nextTag()
            if (tag == -1) break
            if (tag == 1) stops += readStop(reader) else reader.readUnknownField(tag)
        }
        reader.endMessageAndGetUnknownFields(listToken)
        return stops
    }

    private fun readStop(reader: ProtoReader): BundledStop {
        var stopId = ""
        var name = ""
        var latitude = 0.0
        var longitude = 0.0
        var isParent = true
        val token = reader.beginMessage()
        while (true) {
            when (val tag = reader.nextTag()) {
                -1 -> break
                TAG_STOP_ID -> stopId = ProtoAdapter.STRING.decode(reader)
                TAG_STOP_NAME -> name = ProtoAdapter.STRING.decode(reader)
                TAG_LATITUDE -> latitude = ProtoAdapter.DOUBLE.decode(reader)
                TAG_LONGITUDE -> longitude = ProtoAdapter.DOUBLE.decode(reader)
                TAG_IS_PARENT -> isParent = ProtoAdapter.BOOL.decode(reader)
                else -> reader.readUnknownField(tag)
            }
        }
        reader.endMessageAndGetUnknownFields(token)
        return BundledStop(stopId, name, latitude, longitude, isParent)
    }
}
