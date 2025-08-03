package xyz.ksharma.krail.discover.state

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.social.state.SocialType

@Stable
data class DiscoverState(
    val discoverCardsList: ImmutableList<DiscoverUiModel> = persistentListOf(),
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

enum class DiscoverCardType(val displayName: String) {
    Krail("KRAIL"), // general Krail related content

    Travel("Travel"), // places to visit, travel tips etc.

    Events("Events"), // concerts, festivals etc.

    Food("Food"), // restaurants, cafes etc.

    Sports("Sports"), // football, cricket etc.

    Unknown("Unknown"); // fallback for unknown types
    ;
}

sealed class Button {
    data class Cta(
        val label: String,
        val url: String,
    ) : Button()

    data class Share(
        val shareUrl: String,
    ) : Button()

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
    if (!isValidButtonCombo()) return null
    var left: DiscoverCardButtonRowState.LeftButtonType? = null
    var right: DiscoverCardButtonRowState.RightButtonType? = null
    for (button in this) {
        when (button) {
            is Button.Cta -> left =
                DiscoverCardButtonRowState.LeftButtonType.Cta(button)

            is Button.Social -> left =
                DiscoverCardButtonRowState.LeftButtonType.Social(button)

            is Button.Share -> right =
                DiscoverCardButtonRowState.RightButtonType.Share(button)
        }
    }
    return DiscoverCardButtonRowState(left, right)
}

fun List<Button>.isValidButtonCombo(): Boolean {
    val types = this.map { it::class }

    val leftTypes = listOf(
        Button.Cta::class,
        Button.Social::class
    )
    if (types.count { it in leftTypes } > 1) return false

    // Only one Share allowed, can be alone or with Cta
    if (types.count { it == Button.Share::class } > 1) return false
    if (types.contains(Button.Share::class)) {
        val leftCount = types.count { it in leftTypes }
        if (leftCount > 1) return false
        if (leftCount == 1 && !types.contains(Button.Cta::class)) return false
    }

    // Social cannot be combined with Cta
    if (types.contains(Button.Social::class) && types.contains(Button.Cta::class)) return false

    // Only one of each type allowed
    if (types.count { it == Button.Cta::class } > 1) return false
    if (types.count { it == Button.Social::class } > 1) return false

    return true
}
