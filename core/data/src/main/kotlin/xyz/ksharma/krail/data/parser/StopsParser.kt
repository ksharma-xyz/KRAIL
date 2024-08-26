package xyz.ksharma.krail.data.parser

import timber.log.Timber
import xyz.ksharma.krail.model.gtfs_realtime.proto.Stop
import xyz.ksharma.krail.model.gtfs_realtime.proto.TranslatedString
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.nio.file.Path

object StopsParser {

    internal fun Path.parseStops(): List<Stop> {
        //val path = context.toPath(GTFSFeedFileNames.STOPS.fileName)
        val stops = mutableListOf<Stop>()

        try {
            BufferedReader(FileReader(this.toString())).use { reader ->
                val headersList = reader.readLine().split(",").trimQuotes()
                // todo use headers instead of hard code later.
                //Log.d(TAG, "headersList: $headersList")

                while (true) {
                    val line = reader.readLine() ?: break
                    val fieldsList = line.split(",").trimQuotes()

                    stops.add(
                        Stop(
                            stop_id = fieldsList[0],
                            stop_code = fieldsList[1].translate(),
                            stop_name = fieldsList[2].translate(),
                            stop_desc = fieldsList[3].translate(),
                            stop_lat = fieldsList[4].toFloatOrNull(),
                            stop_lon = fieldsList[5].toFloatOrNull(),
                            zone_id =  fieldsList[6],
                            stop_url = fieldsList[7].translate(),
//                            location_type = fieldsList[8].toInt(),
                            parent_station = fieldsList[9],
                            stop_timezone = fieldsList[10],
                            wheelchair_boarding = fieldsList[11].toInt().toWheelchairBoarding(),
                        )
                    )
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Timber.e(e, "readStopsFromCSV: ")
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            Timber.e(e, "readStopsFromCSV: ")
        }

        return stops
    }

    private fun String.translate(): TranslatedString {
        // Create a Translation object with the text and language
        val translation = TranslatedString.Translation(
            text = this,
            language = "en"
        )

        // Create and return the TranslatedString object containing the translation
        return TranslatedString(translation = listOf(translation))
    }

    private fun List<String>.trimQuotes(): List<String> = this.map { it.trim('\"') }

    private fun Int?.toWheelchairBoarding() = when (this) {
        0 -> Stop.WheelchairBoarding.UNKNOWN
        1 -> Stop.WheelchairBoarding.AVAILABLE
        2 -> Stop.WheelchairBoarding.NOT_AVAILABLE
        else -> null
    }
}
