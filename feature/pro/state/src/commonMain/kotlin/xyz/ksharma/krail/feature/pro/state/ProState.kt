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
    val note: String,
) {
    MONTHLY(
        displayPrice = "$3.99",
        billingCycle = "/ mo",
        savingsBadge = null,
        note = "Cancel anytime",
    ),
    ANNUAL(
        displayPrice = "$35.99",
        billingCycle = "/ yr",
        savingsBadge = "BEST",
        note = "~$3.00/mo · Save 25%",
    ),
}
