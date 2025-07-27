package xyz.ksharma.krail.discover.network.api

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import xyz.ksharma.krail.social.network.api.model.SocialType

@Stable
data class DiscoverModel(
    val title: String,
    val description: String,

    /**
     * List of image URLs to be displayed in the card.
     */
    val imageList: ImmutableList<String>,
    val buttons: ImmutableList<Button>? = null,
) {
    sealed class Button {
        data class Cta(
            val label: String,
            val url: String,
        ) : Button()

        data class Share(
            val shareUrl: String,
        ) : Button()

        data class Feedback(
            val label: String? = null,
            val url: String? = null,
        ) : Button()

        sealed class Social : Button() {

            data object AppSocial : Social()

            data class PartnerSocial(
                val socialPartnerName: String,
                val links: List<PartnerSocialType>
            ) : Social() {

                init {
                    require(socialPartnerName.isNotBlank()) { "socialPartnerName must not be empty or blank" }
                    require(links.isNotEmpty()) { "links list must not be empty" }
                }

                data class PartnerSocialType(
                    val type: SocialType,
                    val url: String,
                )
            }
        }
    }
}

data class DiscoverCardButtonRowState(
    val left: LeftButtonType?,
    val right: RightButtonType?,
) {
    sealed interface LeftButtonType {
        data class Cta(val button: DiscoverModel.Button.Cta) : LeftButtonType
        data class Social(val button: DiscoverModel.Button.Social) : LeftButtonType
        data class Feedback(val button: DiscoverModel.Button.Feedback) : LeftButtonType
    }

    sealed interface RightButtonType {
        data class Share(val button: DiscoverModel.Button.Share) : RightButtonType
    }
}

fun List<DiscoverModel.Button>.toButtonRowState(): DiscoverCardButtonRowState? {
    if (!isValidButtonCombo()) return null
    var left: DiscoverCardButtonRowState.LeftButtonType? = null
    var right: DiscoverCardButtonRowState.RightButtonType? = null
    for (button in this) {
        when (button) {
            is DiscoverModel.Button.Cta -> left =
                DiscoverCardButtonRowState.LeftButtonType.Cta(button)

            is DiscoverModel.Button.Social -> left =
                DiscoverCardButtonRowState.LeftButtonType.Social(button)

            is DiscoverModel.Button.Feedback -> left =
                DiscoverCardButtonRowState.LeftButtonType.Feedback(button)

            is DiscoverModel.Button.Share -> right =
                DiscoverCardButtonRowState.RightButtonType.Share(button)
        }
    }
    return DiscoverCardButtonRowState(left, right)
}

fun List<DiscoverModel.Button>.isValidButtonCombo(): Boolean {
    val types = this.map { it::class }

    val leftTypes = listOf(
        DiscoverModel.Button.Cta::class,
        DiscoverModel.Button.Feedback::class,
        DiscoverModel.Button.Social::class
    )
    if (types.count { it in leftTypes } > 1) return false

    // Only one Share allowed, can be alone or with Cta
    if (types.count { it == DiscoverModel.Button.Share::class } > 1) return false
    if (types.contains(DiscoverModel.Button.Share::class)) {
        val leftCount = types.count { it in leftTypes }
        if (leftCount > 1) return false
        if (leftCount == 1 && !types.contains(DiscoverModel.Button.Cta::class)) return false
    }

    // Cta cannot be combined with Social or Feedback
    if (types.contains(DiscoverModel.Button.Cta::class) &&
        (types.contains(DiscoverModel.Button.Social::class) || types.contains(
            DiscoverModel.Button.Feedback::class
        ))
    ) return false

    // Social cannot be combined with Feedback or Cta
    if (types.contains(DiscoverModel.Button.Social::class) &&
        (types.contains(DiscoverModel.Button.Feedback::class) || types.contains(DiscoverModel.Button.Cta::class))
    ) return false

    // Feedback cannot be combined with Social or Cta
    if (types.contains(DiscoverModel.Button.Feedback::class) &&
        (types.contains(DiscoverModel.Button.Social::class) || types.contains(DiscoverModel.Button.Cta::class))
    ) return false

    // Only one of each type allowed
    if (types.count { it == DiscoverModel.Button.Cta::class } > 1) return false
    if (types.count { it == DiscoverModel.Button.Feedback::class } > 1) return false
    if (types.count { it == DiscoverModel.Button.Social::class } > 1) return false

    return true
}
