package xyz.ksharma.krail.core.navigation

import org.koin.core.qualifier.named

/**
 * Centralized definition of all entry builder qualifiers and names.
 *
 * This ensures consistency across modules and prevents typos when
 * providing and injecting entry builders.
 */
object EntryBuilderQualifiers {
    /**
     * Entry builder names - used in EntryBuilderDescriptor.name
     */
    object Names {
        const val SPLASH = "splash"
        const val APP_UPGRADE = "appUpgrade"
        const val TRIP_PLANNER = "tripPlanner"
        const val LOCATION = "location"

        // Add more names here as needed for other features
        // const val DISCOVER = "discover"
        // const val PARK_RIDE = "parkRide"
        // const val INFO_TILE = "infoTile"
    }

    /**
     * Koin qualifiers - used when providing/injecting entry builders
     */
    val SPLASH = named(Names.SPLASH)
    val APP_UPGRADE = named(Names.APP_UPGRADE)
    val TRIP_PLANNER = named(Names.TRIP_PLANNER)

    // Add more qualifiers here as needed for other features
    // val DISCOVER = named(Names.DISCOVER)
    // val PARK_RIDE = named(Names.PARK_RIDE)
    // val INFO_TILE = named(Names.INFO_TILE)
}
