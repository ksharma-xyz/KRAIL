package xyz.ksharma.krail.taj.theme

val DEFAULT_THEME_STYLE = KrailThemeStyle.Train

enum class KrailThemeStyle(val hexColorCode: String, val id: Int, val tagLine: String) {
    Train(
        hexColorCode = "#F6891F",
        id = 1,
        tagLine = "On the track, no lookin' back!"
    ),
    Metro(
        hexColorCode = "#009B77",
        id = 2,
        tagLine = "Surf the sub, no cap!"
    ),
    Bus(
        hexColorCode = "#00B5EF",
        id = 5,
        tagLine = "Hoppin' the concrete jungle!"
    ),
    Coach(
        hexColorCode = "#742282",
        id = 7,
        tagLine = "Purple drip, endless trip!"
    ),
    Ferry(
        hexColorCode = "#5AB031",
        id = 9,
        tagLine = "Smooth sail, no fail!"
    ),
    BarbiePink(
        hexColorCode = "#E0218A",
        id = 100,
        tagLine = "Dressed in pink, fastest link!"
    ),
}
// TODO - update transport mode badges color in the Mode selection
