package xyz.ksharma.krail.discover.network.api

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList

@Stable
data class DiscoverCardModel(
    val title: String,
    val description: String,
    val imageUrl: String,
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

        data object Social : Button()
    }
}

data class DiscoverCardButtonRowState(
    val left: LeftButtonType?,
    val right: RightButtonType?,
) {
    sealed interface LeftButtonType {
        data class Cta(val button: DiscoverCardModel.Button.Cta) : LeftButtonType
        data class Social(val button: DiscoverCardModel.Button.Social) : LeftButtonType
        data class Feedback(val button: DiscoverCardModel.Button.Feedback) : LeftButtonType
    }
    sealed interface RightButtonType {
        data class Share(val button: DiscoverCardModel.Button.Share) : RightButtonType
    }
}

fun List<DiscoverCardModel.Button>.toButtonRowState(): DiscoverCardButtonRowState? {
    if (!isValidButtonCombo()) return null
    var left: DiscoverCardButtonRowState.LeftButtonType? = null
    var right: DiscoverCardButtonRowState.RightButtonType? = null
    for (button in this) {
        when (button) {
            is DiscoverCardModel.Button.Cta -> left = DiscoverCardButtonRowState.LeftButtonType.Cta(button)
            is DiscoverCardModel.Button.Social -> left = DiscoverCardButtonRowState.LeftButtonType.Social(button)
            is DiscoverCardModel.Button.Feedback -> left = DiscoverCardButtonRowState.LeftButtonType.Feedback(button)
            is DiscoverCardModel.Button.Share -> right = DiscoverCardButtonRowState.RightButtonType.Share(button)
        }
    }
    return DiscoverCardButtonRowState(left, right)
}

fun List<DiscoverCardModel.Button>.isValidButtonCombo(): Boolean {
    val types = this.map { it::class }

    val leftTypes = listOf(
        DiscoverCardModel.Button.Cta::class,
        DiscoverCardModel.Button.Feedback::class,
        DiscoverCardModel.Button.Social::class
    )
    if (types.count { it in leftTypes } > 1) return false

    // Only one Share allowed, can be alone or with Cta
    if (types.count { it == DiscoverCardModel.Button.Share::class } > 1) return false
    if (types.contains(DiscoverCardModel.Button.Share::class)) {
        val leftCount = types.count { it in leftTypes }
        if (leftCount > 1) return false
        if (leftCount == 1 && !types.contains(DiscoverCardModel.Button.Cta::class)) return false
    }

    // Cta cannot be combined with Social or Feedback
    if (types.contains(DiscoverCardModel.Button.Cta::class) &&
        (types.contains(DiscoverCardModel.Button.Social::class) || types.contains(
            DiscoverCardModel.Button.Feedback::class
        ))
    ) return false

    // Social cannot be combined with Feedback or Cta
    if (types.contains(DiscoverCardModel.Button.Social::class) &&
        (types.contains(DiscoverCardModel.Button.Feedback::class) || types.contains(DiscoverCardModel.Button.Cta::class))
    ) return false

    // Feedback cannot be combined with Social or Cta
    if (types.contains(DiscoverCardModel.Button.Feedback::class) &&
        (types.contains(DiscoverCardModel.Button.Social::class) || types.contains(DiscoverCardModel.Button.Cta::class))
    ) return false

    // Only one of each type allowed
    if (types.count { it == DiscoverCardModel.Button.Cta::class } > 1) return false
    if (types.count { it == DiscoverCardModel.Button.Feedback::class } > 1) return false
    if (types.count { it == DiscoverCardModel.Button.Social::class } > 1) return false

    return true
}
