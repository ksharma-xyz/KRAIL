package xyz.ksharma.krail.trip.planner.ui.journeymap.business

import org.maplibre.compose.expressions.ast.Expression
import org.maplibre.compose.expressions.dsl.Feature.get
import org.maplibre.compose.expressions.dsl.and
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.eq
import org.maplibre.compose.expressions.dsl.or
import org.maplibre.compose.expressions.value.BooleanValue
import xyz.ksharma.krail.core.maps.state.GeoJsonFeatureTypes
import xyz.ksharma.krail.core.maps.state.GeoJsonPropertyKeys
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.StopType

/**
 * Helper functions to create MapLibre filter expressions for journey map layers.
 * Simplifies complex filter syntax and removes hardcoded strings.
 */
object JourneyMapFilters {

    /**
     * Filter for journey leg features (route lines).
     */
    fun isJourneyLeg(): Expression<BooleanValue> =
        get(GeoJsonPropertyKeys.TYPE).asString() eq const(GeoJsonFeatureTypes.JOURNEY_LEG)

    /**
     * Filter for journey stop features (stop markers).
     */
    fun isJourneyStop(): Expression<BooleanValue> =
        get(GeoJsonPropertyKeys.TYPE).asString() eq const(GeoJsonFeatureTypes.JOURNEY_STOP)

    /**
     * Filter for specific stop type.
     */
    fun isStopType(stopType: StopType): Expression<BooleanValue> =
        isJourneyStop() and (get(GeoJsonPropertyKeys.STOP_TYPE).asString() eq const(stopType.name))

    /**
     * Filter for multiple stop types (OR condition).
     */
    fun isStopType(vararg stopTypes: StopType): Expression<BooleanValue> {
        require(stopTypes.isNotEmpty()) { "At least one stop type must be provided" }

        val typeFilters = stopTypes.map { stopType ->
            get(GeoJsonPropertyKeys.STOP_TYPE).asString() eq const(stopType.name)
        }

        val combinedTypeFilter = typeFilters.reduce { acc, filter -> acc or filter }
        return isJourneyStop() and combinedTypeFilter
    }
}
