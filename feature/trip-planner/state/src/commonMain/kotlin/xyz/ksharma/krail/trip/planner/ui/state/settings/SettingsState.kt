package xyz.ksharma.krail.trip.planner.ui.state.settings

data class SettingsState(val appVersion: String = "")

// TODO - move to separate module for reusability.
enum class SocialType(val displayName: String, val httpLink: String) {
    LinkedIn(displayName = "LinkedIn", httpLink = "https://www.linkedin.com/company/krail/"),

    Reddit(displayName = "Reddit", httpLink = "https://www.reddit.com/r/krailapp/"),

    Instagram(displayName = "Instagram", httpLink = "https://www.instagram.com/krailapp/"),

    Facebook(displayName = "Facebook", httpLink = "https://www.facebook.com/krailapp")
    ;
}
