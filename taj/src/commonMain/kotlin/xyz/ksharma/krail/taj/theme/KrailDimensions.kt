package xyz.ksharma.krail.taj.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import xyz.ksharma.krail.taj.tokens.ComponentTokens
import xyz.ksharma.krail.taj.tokens.IconSizeTokens
import xyz.ksharma.krail.taj.tokens.RadiusTokens
import xyz.ksharma.krail.taj.tokens.SpacingTokens
import xyz.ksharma.krail.taj.tokens.StrokeTokens

data class KrailDimensions(
    // ── Spacing ───────────────────────────────────────────────────────────────
    val spacingNone: Dp = SpacingTokens.None,
    val spacingXXS: Dp = SpacingTokens.XXS,
    val spacingXS: Dp = SpacingTokens.XS,
    val spacingS: Dp = SpacingTokens.S,
    val spacingM: Dp = SpacingTokens.M,
    val spacingML: Dp = SpacingTokens.ML,
    val spacingL: Dp = SpacingTokens.L,
    val spacingXL: Dp = SpacingTokens.XL,
    val spacingXXL: Dp = SpacingTokens.XXL,
    val spacingXXXL: Dp = SpacingTokens.XXXL,
    val spacingXXXXL: Dp = SpacingTokens.XXXXL,

    // ── Corner radius ─────────────────────────────────────────────────────────
    val radiusNone: Dp = RadiusTokens.None,
    val radiusXS: Dp = RadiusTokens.XS,
    val radiusS: Dp = RadiusTokens.S,
    val radiusM: Dp = RadiusTokens.M,
    val radiusL: Dp = RadiusTokens.L,
    val radiusXL: Dp = RadiusTokens.XL,
    val radiusXXL: Dp = RadiusTokens.XXL,
    val radiusFull: Dp = RadiusTokens.Full,

    // ── Icon sizes ────────────────────────────────────────────────────────────
    val iconXS: Dp = IconSizeTokens.XS,
    val iconS: Dp = IconSizeTokens.S,
    val iconM: Dp = IconSizeTokens.M,
    val iconL: Dp = IconSizeTokens.L,
    val iconXL: Dp = IconSizeTokens.XL,
    val iconXXL: Dp = IconSizeTokens.XXL,
    val iconXXXL: Dp = IconSizeTokens.XXXL,
    val iconFab: Dp = IconSizeTokens.FAB,

    // ── Strokes ───────────────────────────────────────────────────────────────
    val strokeHairline: Dp = StrokeTokens.Hairline,
    val strokeThin: Dp = StrokeTokens.Thin,
    val strokeRegular: Dp = StrokeTokens.Regular,
    val strokeMedium: Dp = StrokeTokens.Medium,
    val strokeThick: Dp = StrokeTokens.Thick,
    val strokeExtraThick: Dp = StrokeTokens.ExtraThick,

    // ── Page / Screen ─────────────────────────────────────────────────────────
    val pageHorizontalPadding: Dp = ComponentTokens.PageHorizontalPadding,
    val pageVerticalPadding: Dp = ComponentTokens.PageVerticalPadding,
    val pageSectionGap: Dp = ComponentTokens.PageSectionGap,

    // ── Cards ─────────────────────────────────────────────────────────────────
    val cardHorizontalPadding: Dp = ComponentTokens.CardHorizontalPadding,
    val cardVerticalPadding: Dp = ComponentTokens.CardVerticalPadding,
    val cardCornerRadius: Dp = ComponentTokens.CardCornerRadius,
    val cardInternalSpacing: Dp = ComponentTokens.CardInternalSpacing,

    // ── Journey card ──────────────────────────────────────────────────────────
    val journeyCardHorizontalPadding: Dp = ComponentTokens.JourneyCardHorizontalPadding,
    val journeyCardTopPadding: Dp = ComponentTokens.JourneyCardTopPadding,
    val journeyCardLegSpacing: Dp = ComponentTokens.JourneyCardLegSpacing,
    val journeyLegStrokeWidth: Dp = ComponentTokens.JourneyLegStrokeWidth,

    // ── Saved trip card ───────────────────────────────────────────────────────
    val savedTripCardPadding: Dp = ComponentTokens.SavedTripCardPadding,
    val savedTripCardSpacing: Dp = ComponentTokens.SavedTripCardSpacing,
    val savedTripIconButtonSize: Dp = ComponentTokens.SavedTripIconButtonSize,

    // ── Bottom sheet ──────────────────────────────────────────────────────────
    val bottomSheetCornerRadius: Dp = ComponentTokens.BottomSheetCornerRadius,
    val bottomSheetPadding: Dp = ComponentTokens.BottomSheetPadding,

    // ── Buttons ───────────────────────────────────────────────────────────────
    val buttonRoundSize: Dp = ComponentTokens.ButtonRoundSize,
    val buttonSmallHeight: Dp = ComponentTokens.ButtonSmallHeight,
    val buttonMediumHeight: Dp = ComponentTokens.ButtonMediumHeight,
    val buttonSmallHorizontalPadding: Dp = ComponentTokens.ButtonSmallHorizontalPadding,
    val buttonMediumHorizontalPadding: Dp = ComponentTokens.ButtonMediumHorizontalPadding,
    val buttonLargeHorizontalPadding: Dp = ComponentTokens.ButtonLargeHorizontalPadding,
    val buttonSmallVerticalPadding: Dp = ComponentTokens.ButtonSmallVerticalPadding,
    val buttonMediumVerticalPadding: Dp = ComponentTokens.ButtonMediumVerticalPadding,
    val buttonLargeVerticalPadding: Dp = ComponentTokens.ButtonLargeVerticalPadding,

    // ── Chips ─────────────────────────────────────────────────────────────────
    val chipHorizontalPadding: Dp = ComponentTokens.ChipHorizontalPadding,
    val chipVerticalPadding: Dp = ComponentTokens.ChipVerticalPadding,
    val chipSpacing: Dp = ComponentTokens.ChipSpacing,

    // ── Icons ─────────────────────────────────────────────────────────────────
    val iconSmall: Dp = ComponentTokens.IconSmall,
    val iconDefault: Dp = ComponentTokens.IconDefault,
    val iconLarge: Dp = ComponentTokens.IconLarge,

    // ── Map ───────────────────────────────────────────────────────────────────
    val mapButtonSize: Dp = ComponentTokens.MapButtonSize,
    val mapFabSize: Dp = ComponentTokens.MapFabSize,
    val mapStrokeWidth: Dp = ComponentTokens.MapStrokeWidth,

    // ── Timeline ──────────────────────────────────────────────────────────────
    val timelineStrokeWidth: Dp = ComponentTokens.TimelineStrokeWidth,
    val timelineDotSize: Dp = ComponentTokens.TimelineDotSize,

    // ── Text field ────────────────────────────────────────────────────────────
    val textFieldHeight: Dp = ComponentTokens.TextFieldHeight,
    val textFieldCornerRadius: Dp = ComponentTokens.TextFieldCornerRadius,
)

val LocalKrailDimensions = staticCompositionLocalOf { KrailDimensions() }

val krailDimensions = KrailDimensions()
