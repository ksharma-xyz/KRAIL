package xyz.ksharma.krail.feature.pro.state

data class ProState(
    val selectedPlan: ProPlan = ProPlan.ANNUAL,
    val features: List<ProFeature> = ProFeature.defaults(),
    val isProActive: Boolean = false,
)

enum class ProPlan(
    val displayPrice: String,
    val billingCycle: String,
    val savingsBadge: String?,
) {
    MONTHLY(
        displayPrice = "$3.99",
        billingCycle = "per month",
        savingsBadge = null,
    ),
    ANNUAL(
        displayPrice = "$29.99",
        billingCycle = "per year",
        savingsBadge = "Save 37%",
    ),
}
