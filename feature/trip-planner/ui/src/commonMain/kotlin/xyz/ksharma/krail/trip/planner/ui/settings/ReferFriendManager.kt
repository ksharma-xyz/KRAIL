package xyz.ksharma.krail.trip.planner.ui.settings

object ReferFriendManager {

    private const val KRAIL_GOOGLE_PLAY = "https://play.google.com/store/apps/details?id=xyz.ksharma.krail"
    private const val KRAIL_APP_STORE = "https://apps.apple.com/us/app/krail-app/id6738934832"

    fun getReferText(): String {
        val randomIndex = referTextOptions.indices.random()
        return referTextOptions[randomIndex]
    }

    // TODO - fetch these from Firebase.
    private val referTextOptions: List<String> = listOf(
        "Hey mate!\n" +
                "Try the best Sydney public transport app.\n" +
                "Download now \uD83D\uDC47 \n" +
                "Apple - $KRAIL_APP_STORE\n" +
                "Android - $KRAIL_GOOGLE_PLAY\n" +
                "KRAIL - Ride the rail without fail",

        "Yo! legend,\n" +
                "Sydney’s best public transport app is here.\n" +
                "Download now \uD83D\uDC47 \n" +
                "Apple - $KRAIL_APP_STORE\n" +
                "Android - $KRAIL_GOOGLE_PLAY\n" +
                "KRAIL - Ride the rail without fail",

        "KRAIL — Sydney trains, minus the drama \uD83E\uDEE0\n" +
                "Get the app ya should’ve had yesterday.\n" +
                "Download now \uD83D\uDC47 \n" +
                "Apple - $KRAIL_APP_STORE\n" +
                "Android - $KRAIL_GOOGLE_PLAY\n" +
                "KRAIL - Ride the rail without fail",

        "Oi where’s my train?\n" +
                "Use KRAIL — it’s like TripView but actually good.\n" +
                "Download now \uD83D\uDC47 \n" +
                "Apple - $KRAIL_APP_STORE\n" +
                "Android - $KRAIL_GOOGLE_PLAY\n" +
                "KRAIL - Ride the rail without fail",

        "Not all heroes wear capes.\n" +
                "Some just show you when your bus is actually coming.\n" +
                "Download now \uD83D\uDC47 \n" +
                "Apple - $KRAIL_APP_STORE\n" +
                "Android - $KRAIL_GOOGLE_PLAY\n" +
                "KRAIL - Ride the rail without fail",

        "Bus ghosted ya again? Rude.\n" +
                "KRAIL tells you when and where — no cap.\n" +
                "Download now \uD83D\uDC47 \n" +
                "Apple - $KRAIL_APP_STORE\n" +
                "Android - $KRAIL_GOOGLE_PLAY\n" +
                "KRAIL - Ride the rail without fail",

        "Late again? Haha, your train’s playing hide and seek.\n" +
                "Catch it before it ghosts ya.\n" +
                "KRAIL’s got your back.\n" +
                "Download now \uD83D\uDC47 \n" +
                "Apple - $KRAIL_APP_STORE\n" +
                "Android - $KRAIL_GOOGLE_PLAY\n" +
                "KRAIL - Ride the rail without fail",
    )
}
