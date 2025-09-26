package xyz.ksharma.krail.trip.planner.ui.state.intro

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.magic_yellow
import xyz.ksharma.krail.taj.toHex
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

data class IntroState(
    val pages: ImmutableList<IntroPage>,
) {
    @Stable
    data class IntroPage(
        val id: Int,
        val colorsList: ImmutableList<String>,
        val title: String,
        val tagline: String,
        val emoji: String,
        val ctaText: String,
        val primaryStyle: String, // hexCode
        val type: IntroPageType,
    )

    enum class IntroPageType {
        SAVE_TRIPS,
        REAL_TIME_ROUTES,
        ALERTS,
        PLAN_TRIP,
        SELECT_MODE,
        INVITE_FRIENDS,

        PARK_RIDE,
    }

    companion object {

        fun default(): IntroState = IntroState(
            pages = persistentListOf(
                IntroPage(
                    id = 0,
                    colorsList = persistentListOf(
                        KrailThemeStyle.Metro.hexColorCode,
                        KrailThemeStyle.Bus.hexColorCode,
                        magic_yellow.toHex(),
                        KrailThemeStyle.Metro.hexColorCode,
                    ),
                    title = "Save Your Trips",
                    tagline = "JUST\nTAP STAR\nTHAT'S IT",
                    emoji = "\uD83C\uDF1F",
                    ctaText = "LET'S KRAIL",
                    primaryStyle = KrailThemeStyle.Metro.hexColorCode,
                    type = IntroPageType.SAVE_TRIPS,
                ),
                IntroPage(
                    id = 1,
                    colorsList = persistentListOf(
                        TransportMode.Ferry().colorCode,
                        magic_yellow.toHex(),
                        TransportMode.Ferry().colorCode,
                    ),
                    title = "Live Updates",
                    tagline = "FASTEST\nROUTES\nEVERY TIME",
                    emoji = "\uD83D\uDE80",
                    ctaText = "LET'S KRAIL",
                    primaryStyle = TransportMode.Ferry().colorCode,
                    type = IntroPageType.REAL_TIME_ROUTES,
                ),
                IntroPage(
                    id = 2,
                    colorsList = persistentListOf(
                        KrailThemeStyle.Bus.hexColorCode,
                        magic_yellow.toHex(),
                        KrailThemeStyle.Bus.hexColorCode,
                    ),
                    title = "Park & Ride",
                    tagline = "PARKING?\nTOTAL PAIN\nWE GOT YOU",
                    emoji = "\uD83C\uDD7F\uFE0F",
                    ctaText = "LET'S KRAIL",
                    primaryStyle = KrailThemeStyle.Bus.hexColorCode,
                    type = IntroPageType.PARK_RIDE,
                ),
                IntroPage(
                    id = 3,
                    colorsList = persistentListOf(
                        KrailThemeStyle.Train.hexColorCode,
                        KrailThemeStyle.Bus.hexColorCode,
                        KrailThemeStyle.PurpleDrip.hexColorCode,
                        KrailThemeStyle.Train.hexColorCode,
                    ),
                    title = "Pick Your Ride",
                    tagline = "RIDE\nYOUR WAY\nEVERY DAY",
                    emoji = "\uD83D\uDE0E",
                    ctaText = "LET'S KRAIL",
                    primaryStyle = KrailThemeStyle.Train.hexColorCode,
                    type = IntroPageType.SELECT_MODE,
                ),
                IntroPage(
                    id = 4,
                    colorsList = persistentListOf(
                        KrailThemeStyle.Ferry.hexColorCode,
                        TransportMode.LightRail().colorCode,
                        KrailThemeStyle.Ferry.hexColorCode,
                    ),
                    title = "Service Alerts",
                    tagline = "DELAYS?\nDETOURS?\nWE'LL TELL YOU.",
                    emoji = "⚠",
                    ctaText = "LET'S KRAIL",
                    primaryStyle = TransportMode.LightRail().colorCode,
                    type = IntroPageType.ALERTS,
                ),
                IntroPage(
                    id = 5,
                    colorsList = persistentListOf(
                        KrailThemeStyle.PurpleDrip.hexColorCode,
                        magic_yellow.toHex(),
                        KrailThemeStyle.PurpleDrip.hexColorCode,
                    ),
                    title = "Plan Your Trip",
                    tagline = "WE\nCAN TELL\nTHE FUTURE",
                    emoji = "\uD83D\uDD2E",
                    ctaText = "LET'S KRAIL",
                    primaryStyle = KrailThemeStyle.PurpleDrip.hexColorCode,
                    type = IntroPageType.PLAN_TRIP,
                ),
                IntroPage(
                    id = 6,
                    colorsList = persistentListOf(
                        KrailThemeStyle.BarbiePink.hexColorCode,
                        magic_yellow.toHex(),
                        KrailThemeStyle.BarbiePink.hexColorCode,
                    ),
                    title = "Invite Your Friends",
                    tagline = "LET'S\nKRAIL\nTOGETHER",
                    emoji = "\uD83D\uDC95",
                    ctaText = "Invite friends",
                    primaryStyle = KrailThemeStyle.BarbiePink.hexColorCode,
                    type = IntroPageType.INVITE_FRIENDS,
                ),
            ),
        )
    }
}
