package xyz.ksharma.krail.feature.pro.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import xyz.ksharma.krail.feature.pro.state.ProFeature
import xyz.ksharma.krail.feature.pro.state.ProPlan
import xyz.ksharma.krail.feature.pro.state.ProState
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.antonioFontFamily
import kotlin.math.abs

private val ProAccentColor = Color(0xFFE0218A)
private val ProDarkInk = Color(0xFF1C1B1A)
private val ProLightSurface = Color(0xFFFCF6F1)

@Composable
fun ProUpgradeScreen(
    state: ProState,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onSelectPlan: (ProPlan) -> Unit = {},
    onSubscribe: () -> Unit = {},
    onRestorePurchase: () -> Unit = {},
) {
    val pagerState = rememberPagerState { state.features.size }

    val activeAccentColor by remember(pagerState.currentPage) {
        derivedStateOf {
            state.features.getOrNull(pagerState.currentPage)?.accentHex
                ?: ProFeature.defaults().first().accentHex
        }
    }
    val animatedAccent by animateColorAsState(
        targetValue = activeAccentColor.hexToComposeColor(),
        animationSpec = tween(durationMillis = 550),
        label = "ProAccentColor",
    )

    Box(modifier = modifier.fillMaxSize().background(KrailTheme.colors.surface)) {
        // Radial tint that follows the active card's mode colour
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .align(Alignment.TopCenter)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(animatedAccent.copy(alpha = 0.22f), Color.Transparent),
                    ),
                ),
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.statusBarsPadding())

            ProChromeRow(onClose = onClose, onRestore = onRestorePurchase)

            Spacer(Modifier.height(8.dp))

            ProBrandLockup(modifier = Modifier.align(Alignment.Start).padding(start = 18.dp))

            Spacer(Modifier.height(14.dp))

            ProHeroHeadline(modifier = Modifier.align(Alignment.Start).padding(horizontal = 18.dp))

            Spacer(Modifier.height(20.dp))

            if (state.features.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 44.dp),
                    pageSpacing = 14.dp,
                    modifier = Modifier.fillMaxWidth(),
                    key = { it },
                ) { page ->
                    val pageOffset =
                        (pagerState.currentPage - page).toFloat() + pagerState.currentPageOffsetFraction
                    val scale = lerp(0.84f, 1f, 1f - abs(pageOffset).coerceIn(0f, 1f))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Coloured glow behind the ticket
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(0.65f)
                                .aspectRatio(1f)
                                .blur(32.dp)
                                .clip(CircleShape)
                                .background(animatedAccent.copy(alpha = 0.40f * scale)),
                        )
                        ProFeatureTicket(
                            feature = state.features[page],
                            pageIndex = page + 1,
                            totalPages = state.features.size,
                            accentColor = animatedAccent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                ProPagerDots(
                    count = state.features.size,
                    currentPage = pagerState.currentPage,
                    accentColor = animatedAccent,
                )
            }

            Spacer(Modifier.weight(1f))
        }

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

// ── Chrome row ────────────────────────────────────────────────────────────────

@Composable
private fun ProChromeRow(
    onClose: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(KrailTheme.colors.onSurface.copy(alpha = 0.06f))
                .klickable { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "‹",
                style = KrailTheme.typography.titleLarge.copy(fontSize = 22.sp),
                color = KrailTheme.colors.onSurface,
            )
        }
        TextButton(onClick = onRestore) {
            Text(
                text = "Restore",
                style = KrailTheme.typography.labelLarge,
                color = KrailTheme.colors.softLabel,
            )
        }
    }
}

// ── Brand lockup ─────────────────────────────────────────────────────────────

@Composable
private fun ProBrandLockup(modifier: Modifier = Modifier) {
    val antonioFamily = antonioFontFamily()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "KRAIL",
            style = TextStyle(
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                letterSpacing = (-0.6).sp,
                color = KrailTheme.colors.onSurface,
            ),
        )
        // PRO badge with hard 2px offset shadow
        Box(modifier = Modifier.padding(end = 3.dp, bottom = 3.dp)) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 2.dp, y = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.18f)),
            )
            Box(
                modifier = Modifier
                    .graphicsLayer { rotationZ = -2.5f }
                    .clip(RoundedCornerShape(4.dp))
                    .background(ProAccentColor)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "PRO",
                    style = TextStyle(
                        fontFamily = antonioFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        letterSpacing = 1.6.sp,
                        color = Color.White,
                    ),
                )
            }
        }
    }
}

// ── Hero headline ─────────────────────────────────────────────────────────────

@Composable
private fun ProHeroHeadline(modifier: Modifier = Modifier) {
    val squiggleProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        squiggleProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, delayMillis = 200, easing = FastOutSlowInEasing),
        )
    }

    val leadStyle = TextStyle(
        fontWeight = FontWeight.Medium,
        fontStyle = FontStyle.Italic,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text = "End to end.",
            style = leadStyle,
            color = KrailTheme.colors.onSurface,
        )
        Text(
            text = "Tap to tap.",
            style = leadStyle,
            color = KrailTheme.colors.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Sydney, unlocked.",
            style = leadStyle.copy(
                fontWeight = FontWeight.Black,
            ),
            color = ProAccentColor,
        )
        Spacer(Modifier.height(4.dp))
        val progress = squiggleProgress.value
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
        ) {
            val w = size.width
            val h = size.height
            val cy = h * 8f / 14f
            val path = Path().apply {
                moveTo(w * 2f / 280f, cy)
                quadraticTo(w * 35f / 280f, h * (-2f) / 14f, w * 70f / 280f, cy)
                quadraticTo(w * 105f / 280f, h * 18f / 14f, w * 140f / 280f, cy)
                quadraticTo(w * 175f / 280f, h * (-2f) / 14f, w * 210f / 280f, cy)
                quadraticTo(w * 245f / 280f, h * 18f / 14f, w * 278f / 280f, cy)
            }
            val totalLength = w * 1.15f
            val dashEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(totalLength * progress, totalLength * (2f - progress) + 1f),
                phase = 0f,
            )
            drawPath(
                path = path,
                color = ProAccentColor,
                style = Stroke(
                    width = 3f,
                    cap = StrokeCap.Round,
                    pathEffect = dashEffect,
                ),
            )
        }
    }
}

// ── Ticket card ───────────────────────────────────────────────────────────────

private class ProTicketShape(
    private val cornerRadiusDp: Dp,
    private val notchRadiusDp: Dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cr = with(density) { cornerRadiusDp.toPx() }
        val nr = with(density) { notchRadiusDp.toPx() }
        val w = size.width
        val h = size.height
        val perfY = h * (176f / 304f)

        val path = Path().apply {
            moveTo(cr, 0f)
            lineTo(w - cr, 0f)
            arcTo(Rect(w - 2 * cr, 0f, w, 2 * cr), -90f, 90f, false)
            lineTo(w, perfY - nr)
            // Right notch: CCW semicircle cuts inward from right edge
            arcTo(Rect(w - nr, perfY - nr, w + nr, perfY + nr), -90f, -180f, false)
            lineTo(w, h - cr)
            arcTo(Rect(w - 2 * cr, h - 2 * cr, w, h), 0f, 90f, false)
            lineTo(cr, h)
            arcTo(Rect(0f, h - 2 * cr, 2 * cr, h), 90f, 90f, false)
            lineTo(0f, perfY + nr)
            // Left notch: CCW semicircle cuts inward from left edge
            arcTo(Rect(-nr, perfY - nr, nr, perfY + nr), 90f, -180f, false)
            lineTo(0f, cr)
            arcTo(Rect(0f, 0f, 2 * cr, 2 * cr), 180f, 90f, false)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
private fun ProFeatureTicket(
    feature: ProFeature,
    pageIndex: Int,
    totalPages: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val antonioFamily = antonioFontFamily()
    val ticketShape = remember { ProTicketShape(cornerRadiusDp = 20.dp, notchRadiusDp = 13.dp) }
    val textSoft = Color(0x8C010101)
    val perfDotColor = Color(0x38010101)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(260f / 304f)
            .clip(ticketShape)
            .background(Color.White),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header band (weight 50 of 304)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(50f)
                    .background(accentColor)
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = feature.modeLabel.uppercase(),
                    style = TextStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.6.sp,
                        color = Color.White,
                    ),
                )
                Text(
                    text = "${pageIndex.toString().padStart(2, '0')} / ${totalPages.toString().padStart(2, '0')}",
                    style = TextStyle(
                        fontFamily = antonioFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        letterSpacing = 1.4.sp,
                        color = Color.White,
                    ),
                )
            }

            // Body (weight 126 of 304) — includes perforation dots at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(126f)
                    .background(Color.White),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = "FEATURE",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                            color = textSoft,
                        ),
                    )
                    Text(
                        text = feature.title.uppercase(),
                        style = TextStyle(
                            fontFamily = antonioFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 26.sp,
                            lineHeight = 28.sp,
                            letterSpacing = (-0.5).sp,
                            color = ProDarkInk,
                        ),
                        maxLines = 2,
                    )
                    Text(
                        text = feature.subtitle,
                        style = TextStyle(
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = textSoft,
                        ),
                        maxLines = 2,
                    )
                }
                // Perforation dots drawn at the bottom boundary of body
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(8.dp),
                ) {
                    val scale = size.width / 260f
                    val dotRadius = 1.4f * scale
                    val dotStep = 7f * scale
                    var x = 20f * scale
                    while (x <= size.width - 20f * scale) {
                        drawCircle(perfDotColor, dotRadius, Offset(x, size.height / 2f))
                        x += dotStep
                    }
                }
            }

            // Stub (weight 128 of 304)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(128f)
                    .background(Color.White)
                    .padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (feature.detail1Label.isNotEmpty()) {
                    ProStubRow(
                        label = feature.detail1Label,
                        value = feature.detail1Value,
                        textSoft = textSoft,
                    )
                }
                if (feature.detail2Label.isNotEmpty()) {
                    ProStubRow(
                        label = feature.detail2Label,
                        value = feature.detail2Value,
                        textSoft = textSoft,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProStubRow(
    label: String,
    value: String,
    textSoft: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                color = textSoft,
            ),
        )
        Text(
            text = value,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.1.sp,
                lineHeight = 16.sp,
                color = ProDarkInk,
            ),
        )
    }
}

// ── Pager dots ────────────────────────────────────────────────────────────────

@Composable
private fun ProPagerDots(
    count: Int,
    currentPage: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val isActive = index == currentPage
            val dotColor by animateColorAsState(
                targetValue = if (isActive) accentColor else KrailTheme.colors.outlineSubtle,
                animationSpec = tween(350),
                label = "DotColor$index",
            )
            val dotWidth by animateDpAsState(
                targetValue = if (isActive) 22.dp else 6.dp,
                animationSpec = tween(350),
                label = "DotWidth$index",
            )
            Box(
                modifier = Modifier
                    .width(dotWidth)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(dotColor),
            )
        }
    }
}

// ── Pricing dock ──────────────────────────────────────────────────────────────

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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, KrailTheme.colors.surface),
                    startY = 0f,
                    endY = 60f,
                ),
            )
            .background(KrailTheme.colors.surface)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 14.dp, top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Plan picker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProPlan.entries.forEach { plan ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp),
                ) {
                    ProPlanCard(
                        plan = plan,
                        isSelected = plan == selectedPlan,
                        accentColor = accentColor,
                        onClick = { onSelectPlan(plan) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val savingsBadge = plan.savingsBadge
                    if (savingsBadge != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-10).dp, y = (-8).dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ProAccentColor)
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = savingsBadge,
                                style = TextStyle(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.2.sp,
                                    color = Color.White,
                                ),
                            )
                        }
                    }
                }
            }
        }

        // CTA with hard offset accent shadow
        val ctaHeight = 54.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp, bottom = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 4.dp, y = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ctaHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ProDarkInk)
                    .klickable { onSubscribe() },
                contentAlignment = Alignment.Center,
            ) {
                val ctaText = if (isProActive) {
                    "Pro Active (Debug Override)"
                } else {
                    "Start 14-day trial · then ${selectedPlan.displayPrice}${selectedPlan.billingCycle}"
                }
                Text(
                    text = ctaText,
                    style = TextStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        letterSpacing = 0.3.sp,
                        color = ProLightSurface,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        Text(
            text = if (isProActive) {
                "Debug override active. No real IAP."
            } else {
                "then ${selectedPlan.displayPrice}${selectedPlan.billingCycle} · cancel anytime"
            },
            style = KrailTheme.typography.labelSmall,
            color = KrailTheme.colors.softLabel,
            textAlign = TextAlign.Center,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = onRestorePurchase) { Text(text = "Restore") }
            TextButton(onClick = {}) { Text(text = "Terms") }
            TextButton(onClick = {}) { Text(text = "Privacy") }
        }
    }
}

@Composable
private fun ProPlanCard(
    plan: ProPlan,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val antonioFamily = antonioFontFamily()
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else KrailTheme.colors.outlineSubtle,
        animationSpec = tween(300),
        label = "PlanBorder",
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.06f) else Color.Transparent)
            .border(
                width = if (isSelected) 2.dp else 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp),
            )
            .klickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        // Label row with radio button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = plan.name,
                style = TextStyle(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.6.sp,
                    color = if (isSelected) accentColor else KrailTheme.colors.softLabel,
                ),
            )
            // Radio circle
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .then(
                        if (isSelected) {
                            Modifier.background(accentColor)
                        } else {
                            Modifier
                                .background(Color.Transparent)
                                .border(1.5.dp, KrailTheme.colors.outlineSubtle, CircleShape)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                }
            }
        }
        // Price in Antonio
        Text(
            text = "${plan.displayPrice}${plan.billingCycle}",
            style = TextStyle(
                fontFamily = antonioFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                letterSpacing = (-0.5).sp,
                color = ProDarkInk,
            ),
        )
        // Note
        Text(
            text = plan.note,
            style = TextStyle(
                fontSize = 10.sp,
                color = KrailTheme.colors.softLabel,
            ),
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@PreviewScreen
@Composable
private fun PreviewProUpgradeScreen() {
    PreviewTheme {
        ProUpgradeScreen(state = ProState())
    }
}

@PreviewComponent
@Composable
private fun PreviewProFeatureTicket() {
    PreviewTheme {
        ProFeatureTicket(
            feature = ProFeature.defaults().first(),
            pageIndex = 1,
            totalPages = 8,
            accentColor = ProFeature.defaults().first().accentHex.hexToComposeColor(),
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewProPricingDock() {
    PreviewTheme {
        ProPricingDock(
            selectedPlan = ProPlan.ANNUAL,
            accentColor = ProAccentColor,
            onSelectPlan = {},
            onSubscribe = {},
            onRestorePurchase = {},
        )
    }
}
