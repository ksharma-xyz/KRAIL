package xyz.ksharma.krail.discover.state

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.social.state.SocialType

@Stable
data class DiscoverState(
    val discoverCardsList: ImmutableList<DiscoverUiModel> = persistentListOf(),
    val sortedDiscoverCardTypes: ImmutableList<DiscoverCardType> = persistentListOf(),
    val selectedType: DiscoverCardType = DiscoverCardType.Unknown,
) {

    @Stable
    data class DiscoverUiModel(
        val title: String,

        val description: String,

        // ISO 8601 date format
        val startDate: String? = null,

        // ISO 8601 date format
        val endDate: String? = null,

        // image credits etc.
        val disclaimer: String? = null,

        /**
         * List of image URLs to be displayed in the card.
         */
        val imageList: ImmutableList<String>,

        val buttons: ImmutableList<Button>? = null,

        val type: DiscoverCardType,

        val cardId: String,
    )
}

enum class DiscoverCardType(val displayName: String, val sortOrder: Int) {
    Travel("Travel", 1), // places to visit, travel tips etc.
    Events("Events", 2), // concerts, festivals etc.
    Food("Food", 3), // restaurants, cafes etc.
    Sports("Sports", 4), // football, cricket etc.
    Unknown("Unknown", 999); // fallback for unknown types - always last
}

sealed class Button {
    data class Cta(
        val label: String,
        val url: String,
    ) : Button()

    data object Share : Button()

    sealed class Social : Button() {

        data object AppSocial : Social()

        data class PartnerSocial(
            val socialPartnerName: String,
            val links: List<PartnerSocialLink>
        ) : Social() {

            init {
                require(socialPartnerName.isNotBlank()) { "socialPartnerName must not be empty or blank" }
                require(links.isNotEmpty()) { "links list must not be empty" }
            }

            data class PartnerSocialLink(
                val type: SocialType,
                val url: String,
            )
        }
    }
}

data class DiscoverCardButtonRowState(
    val left: LeftButtonType?,
    val right: RightButtonType?,
) {
    sealed interface LeftButtonType {
        data class Cta(val button: Button.Cta) : LeftButtonType
        data class Social(val button: Button.Social) : LeftButtonType
    }

    sealed interface RightButtonType {
        data class Share(val button: Button.Share) : RightButtonType
    }
}

fun List<Button>.toButtonRowState(): DiscoverCardButtonRowState? {
    println("toButtonRowState - Input: ${this.map { it::class.simpleName }}")

    if (!isValidButtonCombo()) {
        println("toButtonRowState - FAILED validation")
        return null
    }

    var left: DiscoverCardButtonRowState.LeftButtonType? = null
    var right: DiscoverCardButtonRowState.RightButtonType? = null

    for (button in this) {
        when (button) {
            is Button.Cta -> {
                left = DiscoverCardButtonRowState.LeftButtonType.Cta(button)
            }
            is Button.Social -> {
                left = DiscoverCardButtonRowState.LeftButtonType.Social(button)
            }
            is Button.Share -> {
                right = DiscoverCardButtonRowState.RightButtonType.Share(button)
            }
        }
    }

    println("toButtonRowState - Result: left=${left}, right=${right}")
    return DiscoverCardButtonRowState(left, right)
}

fun List<Button>.isValidButtonCombo(): Boolean {
    val types = this.map { it::class }

    // This logs: [PartnerSocial, Share]
    println("Button validation - Input buttons: ${this.map { it::class.simpleName }}")

    val leftTypes = listOf(
        Button.Cta::class,
        Button.Social::class  // PartnerSocial is a subclass of Button.Social
    )

    // Share button validation
    if (types.contains(Button.Share::class)) {
        val leftCount = types.count { it in leftTypes }  // This is 1 (PartnerSocial)
        if (leftCount == 1) {
            val hasCta = types.contains(Button.Cta::class)  // false
            val hasPartnerSocial = this.any { it is Button.Social.PartnerSocial }  // true
            val hasAppSocial = this.any { it is Button.Social.AppSocial }  // false

            // Allow Cta + Share OR PartnerSocial + Share, but not AppSocial + Share
            if (!hasCta && !hasPartnerSocial) {  // false && false = false
                return false  // This line won't execute
            }
            if (hasAppSocial) {  // false
                return false  // This line won't execute
            }
        }
    }

    // The validation should pass and return true
    println("Button validation - PASSED: Valid button combination")
    return true
}
