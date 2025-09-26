package xyz.ksharma.krail.social.state

enum class KrailSocialType(val socialType: SocialType, val url: String) {
    LinkedIn(socialType = SocialType.LinkedIn, url = "https://www.linkedin.com/company/krail/"),

    Reddit(socialType = SocialType.Reddit, url = "https://www.reddit.com/r/krailapp/"),

    Instagram(socialType = SocialType.Instagram, url = "https://www.instagram.com/krailapp/"),

    Facebook(socialType = SocialType.Facebook, url = "https://www.facebook.com/krailapp")
}
