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
        "Hey mate,\n" +
                "Sydney’s best public transport app is here. Like TripView but ad-free\n\n" +
                "Download KRAIL here \uD83D\uDC47 \n" +
                "Apple - $KRAIL_APP_STORE\n" +
                "Android - $KRAIL_GOOGLE_PLAY\n" +
                "KRAIL - Ride the rail without fail",

        "Not all heroes wear capes.\n" +
                "Some just show you when your bus is actually coming.\n\n" +
                "Download KRAIL here \uD83D\uDC47 \n" +
                "Apple - $KRAIL_APP_STORE\n" +
                "Android - $KRAIL_GOOGLE_PLAY\n" +
                "KRAIL - Ride the rail without fail",

        "Bus ghosted ya again? Rude.\n" +
                "KRAIL tells you when and where - no cap.\n\n" +
                "Download KRAIL here \uD83D\uDC47 \n" +
                "Apple - $KRAIL_APP_STORE\n" +
                "Android - $KRAIL_GOOGLE_PLAY\n" +
                "KRAIL - Ride the rail without fail",

        "Your train was late again?.\n" +
                "KRAIL App’s got your back.\n\n" +
                "Download KRAIL here \uD83D\uDC47 \n" +
                "Apple - $KRAIL_APP_STORE\n" +
                "Android - $KRAIL_GOOGLE_PLAY\n\n" +
                "KRAIL - Ride the rail without fail",
    )
}
