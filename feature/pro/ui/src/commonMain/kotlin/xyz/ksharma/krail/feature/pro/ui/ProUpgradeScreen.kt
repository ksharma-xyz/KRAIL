package xyz.ksharma.krail.feature.pro.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.feature.pro.state.ProFeature
import xyz.ksharma.krail.feature.pro.state.ProPlan
import xyz.ksharma.krail.feature.pro.state.ProState
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun ProUpgradeScreen(
    state: ProState,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onSelectPlan: (ProPlan) -> Unit = {},
    onSubscribe: () -> Unit = {},
    onRestorePurchase: () -> Unit = {},
) {
    val dim = KrailTheme.dimensions
    val pagerState = rememberPagerState { state.features.size }

    val activeAccentColor by remember(pagerState.currentPage) {
        derivedStateOf {
            state.features.getOrNull(pagerState.currentPage)?.accentHex
                ?: ProFeature.defaults().first().accentHex
        }
    }
    val animatedAccent by animateColorAsState(
        targetValue = activeAccentColor.hexToComposeColor(),
        animationSpec = tween(durationMillis = 400),
        label = "ProAccentColor",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KrailTheme.colors.surface),
    ) {
        // Ambient glow that follows the active feature's accent colour
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .align(Alignment.TopCenter)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedAccent.copy(alpha = 0.25f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.height(dim.spacingXXL))

            // Hero title
            Text(
                text = "KRAIL Pro",
                style = KrailTheme.typography.displayLarge,
                modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(dim.spacingS))
            Text(
                text = "Everything KRAIL. Then some.",
                style = KrailTheme.typography.bodyLarge,
                color = KrailTheme.colors.softLabel,
                modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(dim.spacingXXXXL))

            // Feature ticket deck
            if (state.features.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 48.dp),
                    pageSpacing = dim.spacingL,
                    modifier = Modifier.fillMaxWidth(),
                ) { page ->
                    ProFeatureTicket(
                        feature = state.features[page],
                        accentColor = animatedAccent,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(dim.spacingXL))

                // Page indicator dots
                PagerDots(
                    count = state.features.size,
                    currentPage = pagerState.currentPage,
                    accentColor = animatedAccent,
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // Sticky pricing dock
        ProPricingDock(
            selectedPlan = state.selectedPlan,
            isProActive = state.isProActive,
            accentColor = animatedAccent,
            onSelectPlan = onSelectPlan,
            onSubscribe = onSubscribe,
            onRestorePurchase = onRestorePurchase,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ProFeatureTicket(
    feature: ProFeature,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(dim.radiusXL))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.12f),
                        KrailTheme.colors.surface,
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(dim.radiusXL),
            )
            .padding(horizontal = dim.cardHorizontalPadding, vertical = dim.cardVerticalPadding),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dim.spacingM),
        ) {
            // Accent pip
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accentColor),
            )
            Text(
                text = feature.title,
                style = KrailTheme.typography.titleLarge,
            )
            Text(
                text = feature.subtitle,
                style = KrailTheme.typography.bodyMedium,
                color = KrailTheme.colors.softLabel,
            )
        }
    }
}

@Composable
private fun PagerDots(
    count: Int,
    currentPage: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count) { index ->
            val isActive = index == currentPage
            val dotColor by animateColorAsState(
                targetValue = if (isActive) accentColor else KrailTheme.colors.outlineSubtle,
                animationSpec = tween(300),
                label = "DotColor$index",
            )
            Box(
                modifier = Modifier
                    .width(if (isActive) 16.dp else 6.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(dotColor),
            )
        }
    }
}

@Composable
private fun ProPricingDock(
    selectedPlan: ProPlan,
    accentColor: Color,
    onSelectPlan: (ProPlan) -> Unit,
    onSubscribe: () -> Unit,
    onRestorePurchase: () -> Unit,
    modifier: Modifier = Modifier,
    isProActive: Boolean = false,
) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, KrailTheme.colors.surface),
                    startY = 0f,
                    endY = 80f,
                ),
            )
            .background(KrailTheme.colors.surface)
            .navigationBarsPadding()
            .padding(horizontal = dim.pageHorizontalPadding)
            .padding(bottom = dim.spacingML, top = dim.spacingXXXL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        // Plan toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        ) {
            ProPlan.entries.forEach { plan ->
                PlanOption(
                    plan = plan,
                    isSelected = plan == selectedPlan,
                    accentColor = accentColor,
                    onClick = { onSelectPlan(plan) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(dim.spacingXS))

        // CTA
        Button(
            onClick = onSubscribe,
            colors = ButtonDefaults.buttonColors(
                customContainerColor = accentColor,
                customContentColor = KrailTheme.colors.surface,
            ),
        ) {
            Text(text = if (isProActive) "Pro Active (Debug Override)" else "Start free 14-day trial")
        }

        Text(
            text = if (isProActive) "Debug override active. No real IAP."
            else "then ${selectedPlan.displayPrice} ${selectedPlan.billingCycle} · cancel anytime",
            style = KrailTheme.typography.labelSmall,
            color = KrailTheme.colors.softLabel,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(dim.spacingXS))

        // Apple-required links
        Row(
            horizontalArrangement = Arrangement.spacedBy(dim.spacingL),
        ) {
            TextButton(onClick = onRestorePurchase) {
                Text(text = "Restore purchase")
            }
            TextButton(onClick = {}) {
                Text(text = "Terms")
            }
            TextButton(onClick = {}) {
                Text(text = "Privacy")
            }
        }
    }
}

@Composable
private fun PlanOption(
    plan: ProPlan,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else KrailTheme.colors.outlineSubtle,
        animationSpec = tween(300),
        label = "PlanBorder",
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(dim.radiusL))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.08f)
                else Color.Transparent,
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(dim.radiusL),
            )
            .klickable { onClick() }
            .padding(horizontal = dim.spacingXL, vertical = dim.spacingL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dim.spacingXS),
    ) {
        plan.savingsBadge?.let { badge ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accentColor)
                    .padding(horizontal = dim.spacingM, vertical = dim.spacingXXS),
            ) {
                Text(
                    text = badge,
                    style = KrailTheme.typography.labelSmall,
                    color = KrailTheme.colors.surface,
                )
            }
        }
        Text(
            text = plan.displayPrice,
            style = KrailTheme.typography.titleLarge,
        )
        Text(
            text = plan.billingCycle,
            style = KrailTheme.typography.labelSmall,
            color = KrailTheme.colors.softLabel,
        )
    }
}
