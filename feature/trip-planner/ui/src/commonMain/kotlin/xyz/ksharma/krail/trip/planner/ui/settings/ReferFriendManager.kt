package xyz.ksharma.krail.trip.planner.ui.settings

object ReferFriendManager {

    private const val KRAIL_GOOGLE_PLAY =
        "https://play.google.com/store/apps/details?id=xyz.ksharma.krail"
    private const val KRAIL_APP_STORE = "https://apps.apple.com/us/app/krail-app/id6738934832"

    fun getReferText(): String {
        val randomIndex = referTextOptions.indices.random()
        return referTextOptions[randomIndex]
    }

    private const val SUFFIX = "\n\nDownload KRAIL here \uD83D\uDC47 \n" +
        "Apple - $KRAIL_APP_STORE\n" +
        "Android - $KRAIL_GOOGLE_PLAY\n\n" +
        "KRAIL - Ride the rail without fail"

    // TODO - fetch these from Firebase.
    private val referTextOptions: List<String> = listOf(
        "Not all heroes wear capes.\n" +
            "Some just show you when your bus is actually coming." +
            SUFFIX,

        "With great power, comes great public transport.\n" +
            "Swing into KRAIL App, let’s get on the fastest route. \uD83D\uDD78\uFE0F \uD83D\uDD77\uFE0F" +
            SUFFIX,

        "These are the tracks you’re looking for. \uD83C\uDF0C\n" +
            "Use KRAIL App, mate and may the fastest routes be with us. \uD83D\uDE80\uD83D\uDE80" +
            SUFFIX,

        "I’m not the transport app Sydney deserves. \n" +
            "I’m the one it needs. \uD83D\uDE87" +
            SUFFIX,

        "\uD83D\uDCA3 Your mission, should you choose to accept it is to " +
            "invite two mates to KRAIL App and upgrade their Sydney experience.\n\n" +
            "⏱\uFE0F This message will self-destruct in 2 minutes." +
            SUFFIX,

        "\uD83E\uDDF3 Still stuck at Platform 9¾?\n" +
            "Cast the real spell, say “KRAILiosa Commuta!” \uD83E\uDE84 ✨" +
            SUFFIX,

        "\uD83E\uDDE0 “It’s a train app. Obviously, I love it.”\n" +
            "Join me on KRAIL App. I’ve already saved some trips. \uD83D\uDE86 " +
            SUFFIX,
    )
}
