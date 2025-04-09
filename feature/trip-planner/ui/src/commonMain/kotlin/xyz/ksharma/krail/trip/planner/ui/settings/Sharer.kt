package xyz.ksharma.krail.trip.planner.ui.settings

interface Sharer {

    fun shareText()

    companion object {
        const val REFER_FRIEND_TEXT = "KRAIL - Ride the rail without fail\n" +
                "Hey mate!\n" +
                "Try the best Sydney public transport app.\n" +
                "Download now - https://krail.app"

        val options: List<String> = listOf(
            "Yo! legend,\n" +
                    "Sydney’s best public transport app is here.\n" +
                    "Download it now 👉 https://krail.app\n" +
                    "Let's KRAIL - Ride the rail without fail",

            "KRAIL — Sydney trains, minus the drama \uD83E\uDEE0\n" +
                    "No more guessing. No more stressin’.\n" +
                    "Get the app ya should’ve had yesterday.\n" +
                    "\uD83D\uDC49 https://krail.app\n" +
                    "Let's KRAIL - Ride the rail without fail",

            "Oi where’s my train?\n" +
                    "KRAIL: knows before ya even ask \uD83D\uDC40\n" +
                    "The app that actually gets it.\n" +
                    "\uD83D\uDE89 https://krail.app\n" +
                    "Let's KRAIL - Ride the rail without fail",

            "Not all heroes wear capes.\n" +
                    "Some just show you when your bus is actually coming.\n" +
                    "\uD83D\uDC85 https://krail.app\n" +
                    "Let's KRAIL - Ride the rail without fail",

            "Oi mate, tryna catch a bull, are ya?\n" +
                    "Don’t get stitched up by late trains.\n" +
                    "Use KRAIL — it’s like Google Maps but actually good.\n" +
                    "\uD83D\uDE89 https://krail.app\n" +
                    "Let's KRAIL - Ride the rail without fail",

            "Bus ghosted ya again? Rude.\n" +
                    "KRAIL tells you when and where — no cap.\n" +
                    "Real-time vibes mate!" +
                    "\uD83D\uDE89 https://krail.app\n" +
                    "Let's KRAIL - Ride the rail without fail",

            "Late again? Haha, your train’s playing hide and seek.\n" +
                    "Catch it before it ghosts ya.\n" +
                    "KRAIL’s got your back.\n" +
                    "\uD83D\uDE89 https://krail.app\n" +
                    "Let's KRAIL - Ride the rail without fail",
        )
    }
}
