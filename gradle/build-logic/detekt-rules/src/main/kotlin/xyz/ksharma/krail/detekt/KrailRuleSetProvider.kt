package xyz.ksharma.krail.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class KrailRuleSetProvider : RuleSetProvider {

    override val ruleSetId: String = "krail"

    override fun instance(config: Config): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            CollectAsStateBan(config),
            CyclomaticSuppressBan(config),
            LazyItemKeyRule(config),
            PublicImplementationClass(config),
            ScreenshotPreviewAnnotation(config),
            SnackbarBan(config),
        ),
    )
}
