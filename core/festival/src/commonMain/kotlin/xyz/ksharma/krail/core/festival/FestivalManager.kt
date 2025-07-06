package xyz.ksharma.krail.core.festival

import xyz.ksharma.krail.core.festival.model.Festival
import kotlin.time.ExperimentalTime

interface FestivalManager {

    /**
     * Returns a list of festivals from the remote config.
     *
     * If the remote config is not available or the value is not a valid JSON, it will return null.
     *
     * @return List of [Festival] objects or null if no festivals are available.
     */
    fun getFestivals(): List<Festival>?

    /**
     * Checks if there is a festival today and returns it if available.
     */
    @OptIn(ExperimentalTime::class)
    fun festivalToday(): Festival?
}