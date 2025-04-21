package xyz.ksharma.krail.trip.planner.ui.state.settings.about

data class AboutUsState(
    val isLoading: Boolean = true,
    val story: String = "",
    val disclaimer: String = "",
)
