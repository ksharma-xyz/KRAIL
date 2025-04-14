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
        val ctaText: String
    )

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
                    tagline = "JUST\nONE TAP\nTHAT'S IT",
                    emoji = "\uD83C\uDF1F",
                    ctaText = "LET'S KRAIL",
                ),
                IntroPage(
                    id = 1,
                    colorsList = persistentListOf(
                        TransportMode.LightRail().colorCode,
                        "#FFC800", // Yellow
                        TransportMode.LightRail().colorCode,
                    ),
                    title = "Real-time information",
                    tagline = "FASTEST\nROUTES\nEVERYTIME",
                    emoji = "\uD83D\uDE80",
                    ctaText = "LET'S KRAIL",

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

                    ),
                IntroPage(
                    id = 3,
                    colorsList = persistentListOf(
                        KrailThemeStyle.Coach.hexColorCode,
                        KrailThemeStyle.Bus.hexColorCode,
                        "#FFC800", // Yellow
                        KrailThemeStyle.Coach.hexColorCode,
                    ),
                    title = "Plan your trip",
                    tagline = "WE\nCAN TELL\nTHE FUTURE",
                    emoji = "\uD83D\uDD2E",
                    ctaText = "LET'S KRAIL",

                    ),
                IntroPage(
                    id = 4,
                    colorsList = persistentListOf(
                        KrailThemeStyle.Train.hexColorCode,
                        KrailThemeStyle.Bus.hexColorCode,
                        KrailThemeStyle.Train.hexColorCode,
                    ),
                    title = "Select transport mode",
                    tagline = "TRAIN, BUS\nOR BOTH\nYOUR CHOICE",
                    emoji = "\uD83D\uDE0E",
                    ctaText = "LET'S KRAIL",
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
                ),
            )
        )
    }
}
