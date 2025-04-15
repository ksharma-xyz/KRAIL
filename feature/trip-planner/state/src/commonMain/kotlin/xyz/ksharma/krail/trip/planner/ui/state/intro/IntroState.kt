package xyz.ksharma.krail.trip.planner.ui.state.intro

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
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
        REAL_TIME_DATA,
        ALERTS,
        PLAN_TRIP,
        SELECT_MODE,
        INVITE_FRIENDS,
    }

    companion object {
        fun default(): IntroState = IntroState(
            pages = persistentListOf(
                IntroPage(
                    id = 0,
                    colorsList = persistentListOf(
                        KrailThemeStyle.Metro.hexColorCode,
                        KrailThemeStyle.Bus.hexColorCode,
                        "#FFC800", // Yellow
                        KrailThemeStyle.Metro.hexColorCode,
                    ),
                    title = "Save your trips",
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
                        "#FFC800", // Yellow
                        TransportMode.Ferry().colorCode,
                    ),
                    title = "Real-time information",
                    tagline = "FASTEST\nROUTES\nEVERYTIME",
                    emoji = "\uD83D\uDE80",
                    ctaText = "LET'S KRAIL",
                    primaryStyle = TransportMode.Ferry().colorCode,
                    type = IntroPageType.REAL_TIME_DATA,
                    ),
                IntroPage(
                    id = 2,
                    colorsList = persistentListOf(
                        KrailThemeStyle.Bus.hexColorCode,
                        "#FFC800", // Yellow
                        KrailThemeStyle.Bus.hexColorCode,
                    ),
                    title = "Alerts",
                    tagline = "WE\nRESPECT\nYOUR TIME",
                    emoji = "⚠",
                    ctaText = "LET'S KRAIL",
                    primaryStyle =  KrailThemeStyle.Bus.hexColorCode,
                    type = IntroPageType.ALERTS,
                    ),
                IntroPage(
                    id = 3,
                    colorsList = persistentListOf(
                        KrailThemeStyle.Train.hexColorCode,
                        KrailThemeStyle.Bus.hexColorCode,
                        KrailThemeStyle.Train.hexColorCode,
                    ),
                    title = "Choose your mode",
                    tagline = "TRAIN, BUS\nOR ALL\nYOUR CHOICE",
                    emoji = "\uD83D\uDE0E",
                    ctaText = "LET'S KRAIL",
                    primaryStyle =  KrailThemeStyle.Train.hexColorCode,
                    type = IntroPageType.SELECT_MODE,
                ),
                IntroPage(
                    id = 4,
                    colorsList = persistentListOf(
                        KrailThemeStyle.PurpleDrip.hexColorCode,
                        KrailThemeStyle.Bus.hexColorCode,
                        "#FFC800", // Yellow
                        KrailThemeStyle.PurpleDrip.hexColorCode,
                    ),
                    title = "Plan your trip",
                    tagline = "WE\nCAN TELL\nTHE FUTURE",
                    emoji = "\uD83D\uDD2E",
                    ctaText = "LET'S KRAIL",
                    primaryStyle =  KrailThemeStyle.PurpleDrip.hexColorCode,
                    type = IntroPageType.PLAN_TRIP,
                    ),
                IntroPage(
                    id = 5,
                    colorsList = persistentListOf(
                        KrailThemeStyle.BarbiePink.hexColorCode,
                        "#FFC800", // Yellow
                        KrailThemeStyle.BarbiePink.hexColorCode,
                    ),
                    title = "Invite your friends",
                    tagline = "LET'S\nKRAIL\nTOGETHER",
                    emoji = "\uD83D\uDC95",
                    ctaText = "Invite your friends",
                    primaryStyle =  KrailThemeStyle.BarbiePink.hexColorCode,
                    type = IntroPageType.INVITE_FRIENDS,
                ),
            )
        )
    }
}
