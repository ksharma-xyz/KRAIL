package xyz.ksharma.krail.taj.theme

import xyz.ksharma.krail.taj.toHex

val DEFAULT_THEME_STYLE = KrailThemeStyle.Train

enum class KrailThemeStyle(val hexColorCode: String, val id: Int, val tagLine: String) {
    Train(
        hexColorCode = train_theme.toHex(),
        id = 1,
        tagLine = "On the track, no lookin' back!"
    ),
    Metro(
        hexColorCode = metro_theme.toHex(),
        id = 2,
        tagLine = "Surf the sub, no cap!"
    ),
    Bus(
        hexColorCode = bus_theme.toHex(),
        id = 5,
        tagLine = "Hoppin' the concrete jungle!"
    ),
    Coach(
        hexColorCode = coach_theme.toHex(),
        id = 7,
        tagLine = "Purple drip, endless trip!"
    ),
    Ferry(
        hexColorCode = ferry_theme.toHex(),
        id = 9,
        tagLine = "Smooth sail, no fail!"
    ),
    BarbiePink(
        hexColorCode = barbie_pink_theme.toHex(),
        id = 100,
        tagLine = "Dressed in pink, fastest link!"
    ),
}
