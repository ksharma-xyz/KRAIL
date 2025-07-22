package xyz.ksharma.krail.discover.network.api

data class DiscoverCardModel(
    val title: String,
    val description: String,
    val imageUrl: String,
    val buttons: List<Button>? = null,
) {
    data class Button(
        val type: ButtonType,
        val label: String? = null,
        val url: String? = null,
        val shareUrl: String? = null,
    )

    enum class ButtonType(val type: String) {
        CTA("CTA"),
        SHARE("SHARE"),
        FEEDBACK("FEEDBACK"),
        SOCIAL("SOCIAL"),
    }
}
