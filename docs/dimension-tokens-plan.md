# Dimension Token System — Design & Migration Plan

## Goal

Replace every hardcoded `.dp` value in the KRAIL codebase with named tokens from a
comprehensive dimension system. After migration, zero hardcoded dp values should remain
in any Composable or modifier call — only token references.

**Scope:** 619 hardcoded dp values across 47 unique values, in ~47 files.

---

## Current State

### What already exists in `taj/`

| System | Status |
|---|---|
| Colors | ✅ Complete — `KrailColors`, `LocalKrailColors`, `KrailTheme.colors` |
| Typography | ✅ Complete — `KrailTypography`, `LocalKrailTypography`, `KrailTheme.typography` |
| Dimensions | ❌ Missing — only 4 isolated token objects (`ButtonTokens`, `BadgeTokens`, `TextFieldTokens`, `ContentAlphaTokens`) |

### Top hardcoded values by frequency

| Value | Uses | Primary role |
|---|---|---|
| `16.dp` | 167 | Screen horizontal padding, card padding |
| `8.dp` | 100 | Inter-element spacing |
| `12.dp` | 89 | Component internal padding |
| `4.dp` | 67 | Fine spacing, stroke |
| `24.dp` | 59 | Section gaps, icon padding |
| `20.dp` | 31 | Medium spacing |
| `2.dp` | 26 | Hairline spacing, stroke |
| `32.dp` | 26 | Larger components |
| `48.dp` | 17 | Buttons, touch targets |
| `10.dp` | 19 | Chip padding |
| `6.dp` | 14 | Chip padding |
| `44.dp` | 8 | Icon buttons |
| `36.dp` | 7 | Medium icons |
| `56.dp` | 4 | FAB-style buttons |
| `64.dp` | 5 | Large components |

---

## Token Architecture

Follow the existing `KrailTheme.colors` / `KrailTheme.typography` pattern exactly:

```
taj/tokens/
  SpacingTokens.kt          ← raw dp constants (internal)
  RadiusTokens.kt           ← raw dp constants (internal)
  IconSizeTokens.kt         ← raw dp constants (internal)
  StrokeTokens.kt           ← raw dp constants (internal)
  ComponentTokens.kt        ← component-level named tokens (internal)

taj/theme/
  KrailDimensions.kt        ← public data class + CompositionLocal + defaults
  Theme.kt                  ← add `KrailTheme.dimensions` accessor (modify existing)
```

---

## Phase 1 — Define Token Files

### `SpacingTokens.kt` (internal)

Raw spacing scale. Names follow Material Design 3 naming conventions.

```kotlin
internal object SpacingTokens {
    val None   = 0.dp
    val XXS    = 2.dp
    val XS     = 4.dp
    val S      = 6.dp
    val M      = 8.dp
    val ML     = 10.dp
    val L      = 12.dp
    val XL     = 16.dp   // dominant — screen padding
    val XXL    = 20.dp
    val XXXL   = 24.dp
    val XXXXL  = 32.dp
}
```

### `RadiusTokens.kt` (internal)

```kotlin
internal object RadiusTokens {
    val None        = 0.dp
    val XS          = 4.dp
    val S           = 8.dp
    val M           = 12.dp
    val L           = 16.dp   // dominant — standard cards
    val XL          = 24.dp   // bottom sheets
    val XXL         = 28.dp   // large bottom sheets
    val Full        = 50.dp   // pill / circular
}
```

### `IconSizeTokens.kt` (internal)

```kotlin
internal object IconSizeTokens {
    val XS   = 16.dp
    val S    = 20.dp
    val M    = 24.dp   // standard icon
    val L    = 32.dp
    val XL   = 36.dp
    val XXL  = 44.dp
    val XXXL = 48.dp
    val FAB  = 56.dp
}
```

### `StrokeTokens.kt` (internal)

```kotlin
internal object StrokeTokens {
    val Hairline    = 0.5.dp
    val Thin        = 1.dp
    val Regular     = 2.dp
    val Medium      = 3.dp   // timeline stroke
    val Thick       = 4.dp   // leg visualization
    val ExtraThick  = 8.dp   // gradient border
}
```

### `ComponentTokens.kt` (internal)

Component-scoped named tokens for reusable patterns. These are semantic names —
callers use these instead of raw spacing values, so a "page horizontal padding"
change propagates everywhere automatically.

```kotlin
internal object ComponentTokens {

    // ── Page / Screen layout ──────────────────────────────────────────────────
    val PageHorizontalPadding   = SpacingTokens.XL      // 16.dp — all screen edges
    val PageVerticalPadding     = SpacingTokens.XL      // 16.dp
    val PageSectionGap          = SpacingTokens.XXXL    // 24.dp — between major sections

    // ── Cards ─────────────────────────────────────────────────────────────────
    val CardHorizontalPadding   = SpacingTokens.XL      // 16.dp
    val CardVerticalPadding     = SpacingTokens.XL      // 16.dp
    val CardCornerRadius        = RadiusTokens.L        // 16.dp
    val CardInternalSpacing     = SpacingTokens.L       // 12.dp

    // ── Journey / Timetable card ──────────────────────────────────────────────
    val JourneyCardHorizontalPadding = SpacingTokens.L  // 12.dp
    val JourneyCardTopPadding        = SpacingTokens.M  // 8.dp
    val JourneyCardLegSpacing        = SpacingTokens.XS // 4.dp
    val JourneyLegStrokeWidth        = StrokeTokens.Thick // 4.dp

    // ── Saved trip card ───────────────────────────────────────────────────────
    val SavedTripCardPadding         = SpacingTokens.XL  // 16.dp
    val SavedTripCardSpacing         = SpacingTokens.L   // 12.dp
    val SavedTripIconButtonSize      = IconSizeTokens.XXL // 44.dp

    // ── Bottom sheets ─────────────────────────────────────────────────────────
    val BottomSheetCornerRadius      = RadiusTokens.XL   // 24.dp
    val BottomSheetPadding           = SpacingTokens.XL  // 16.dp

    // ── Buttons ───────────────────────────────────────────────────────────────
    val ButtonSmallHeight            = 20.dp
    val ButtonMediumHeight           = 32.dp
    val ButtonLargeHeight            = 32.dp
    val ButtonSmallHorizontalPadding = SpacingTokens.ML  // 10.dp
    val ButtonMediumHorizontalPadding= SpacingTokens.L   // 12.dp
    val ButtonLargeHorizontalPadding = SpacingTokens.XL  // 16.dp
    val ButtonSmallVerticalPadding   = SpacingTokens.XS  // 4.dp
    val ButtonMediumVerticalPadding  = SpacingTokens.S   // 6.dp
    val ButtonLargeVerticalPadding   = SpacingTokens.ML  // 10.dp
    val ButtonRoundSize              = IconSizeTokens.XXXL // 48.dp — existing ButtonTokens.RoundButtonSize

    // ── Chips ─────────────────────────────────────────────────────────────────
    val ChipHorizontalPadding        = SpacingTokens.L   // 12.dp
    val ChipVerticalPadding          = SpacingTokens.M   // 8.dp
    val ChipSpacing                  = SpacingTokens.ML  // 10.dp

    // ── Icons ─────────────────────────────────────────────────────────────────
    val IconSmall                    = IconSizeTokens.S  // 20.dp
    val IconDefault                  = IconSizeTokens.M  // 24.dp
    val IconLarge                    = IconSizeTokens.L  // 32.dp

    // ── Badges ────────────────────────────────────────────────────────────────
    val BadgeSize                    = 12.dp             // existing BadgeTokens.BadgeSize
    val BadgeOffsetX                 = (-4).dp
    val BadgeOffsetY                 = 2.dp

    // ── Map / floating components ─────────────────────────────────────────────
    val MapButtonSize                = IconSizeTokens.XXXL // 48.dp
    val MapFabSize                   = IconSizeTokens.FAB  // 56.dp
    val MapStrokeWidth               = StrokeTokens.Regular // 2.dp

    // ── Timeline / TrackedLegView ─────────────────────────────────────────────
    val TimelineStrokeWidth          = StrokeTokens.Medium  // 3.dp
    val TimelineDotSize              = SpacingTokens.L      // 12.dp

    // ── Search / input ────────────────────────────────────────────────────────
    val TextFieldHeight              = IconSizeTokens.XXXL  // 48.dp — existing TextFieldTokens
    val TextFieldCornerRadius        = RadiusTokens.Full    // pill shape
}
```

### `KrailDimensions.kt` (public, in `taj/theme/`)

Public data class, CompositionLocal, and `KrailTheme.dimensions` accessor — mirrors
the exact pattern of `KrailColors`.

```kotlin
data class KrailDimensions(
    // Spacing
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

    // Corner radius
    val radiusNone: Dp = RadiusTokens.None,
    val radiusXS: Dp = RadiusTokens.XS,
    val radiusS: Dp = RadiusTokens.S,
    val radiusM: Dp = RadiusTokens.M,
    val radiusL: Dp = RadiusTokens.L,
    val radiusXL: Dp = RadiusTokens.XL,
    val radiusXXL: Dp = RadiusTokens.XXL,
    val radiusFull: Dp = RadiusTokens.Full,

    // Icon sizes
    val iconXS: Dp = IconSizeTokens.XS,
    val iconS: Dp = IconSizeTokens.S,
    val iconM: Dp = IconSizeTokens.M,
    val iconL: Dp = IconSizeTokens.L,
    val iconXL: Dp = IconSizeTokens.XL,
    val iconXXL: Dp = IconSizeTokens.XXL,
    val iconXXXL: Dp = IconSizeTokens.XXXL,
    val iconFab: Dp = IconSizeTokens.FAB,

    // Strokes
    val strokeHairline: Dp = StrokeTokens.Hairline,
    val strokeThin: Dp = StrokeTokens.Thin,
    val strokeRegular: Dp = StrokeTokens.Regular,
    val strokeMedium: Dp = StrokeTokens.Medium,
    val strokeThick: Dp = StrokeTokens.Thick,

    // Component — page
    val pageHorizontalPadding: Dp = ComponentTokens.PageHorizontalPadding,
    val pageVerticalPadding: Dp = ComponentTokens.PageVerticalPadding,
    val pageSectionGap: Dp = ComponentTokens.PageSectionGap,

    // Component — cards
    val cardHorizontalPadding: Dp = ComponentTokens.CardHorizontalPadding,
    val cardVerticalPadding: Dp = ComponentTokens.CardVerticalPadding,
    val cardCornerRadius: Dp = ComponentTokens.CardCornerRadius,
    val cardInternalSpacing: Dp = ComponentTokens.CardInternalSpacing,

    // Component — journey card
    val journeyCardHorizontalPadding: Dp = ComponentTokens.JourneyCardHorizontalPadding,
    val journeyCardTopPadding: Dp = ComponentTokens.JourneyCardTopPadding,
    val journeyLegStrokeWidth: Dp = ComponentTokens.JourneyLegStrokeWidth,

    // Component — saved trip
    val savedTripCardPadding: Dp = ComponentTokens.SavedTripCardPadding,
    val savedTripIconButtonSize: Dp = ComponentTokens.SavedTripIconButtonSize,

    // Component — bottom sheet
    val bottomSheetCornerRadius: Dp = ComponentTokens.BottomSheetCornerRadius,

    // Component — map
    val mapButtonSize: Dp = ComponentTokens.MapButtonSize,
    val mapFabSize: Dp = ComponentTokens.MapFabSize,
    val mapStrokeWidth: Dp = ComponentTokens.MapStrokeWidth,

    // Component — timeline
    val timelineStrokeWidth: Dp = ComponentTokens.TimelineStrokeWidth,
    val timelineDotSize: Dp = ComponentTokens.TimelineDotSize,
)

val LocalKrailDimensions = staticCompositionLocalOf { KrailDimensions() }

val krailDimensions = KrailDimensions()
```

### `Theme.kt` — add `dimensions` accessor

```kotlin
object KrailTheme {
    val colors: KrailColors
        @Composable get() = LocalKrailColors.current

    val typography: KrailTypography
        @Composable get() = LocalKrailTypography.current

    val dimensions: KrailDimensions           // ← add this
        @Composable get() = LocalKrailDimensions.current
}
```

And provide it in `KrailTheme {}` composable:
```kotlin
CompositionLocalProvider(
    LocalKrailColors provides animatedColors,
    LocalKrailTypography provides krailTypography,
    LocalKrailDimensions provides krailDimensions,  // ← add this
    LocalThemeController provides themeController,
    content = content,
)
```

---

## Phase 2 — Migration Order

Migrate module by module, largest dp-count first. Run `detekt + testAndroidHostTest`
after each module to catch regressions.

| Priority | Module | Key files | Notes |
|---|---|---|---|
| 1 | `taj/` | `components/Button.kt`, `components/InfoTile.kt`, all token files | Foundation — tokens live here; fix tokens referencing themselves |
| 2 | `feature/trip-planner/ui` | `SavedTripsScreen`, `SavedTripCard`, `JourneyCard`, `TimeTableScreen`, `TrackTripScreen`, `TrackedLegView` | Highest dp count |
| 3 | `feature/trip-planner/ui/components/` | `ErrorMessage`, `JourneyCard`, reusable components | Shared components used everywhere |
| 4 | `discover/ui/` | `DiscoverScreen`, `DiscoverCard` | Chip and card patterns |
| 5 | `feature/track/ui/` | `TrackTripViewModel`, tracking UI components | Newer module |
| 6 | `feature/departures/ui/` | Departure board cards | |
| 7 | `feature/park-ride/ui/` | Park & ride components | May have intentional divergent sizing |
| 8 | `core/maps/ui/` | Map overlays, floating buttons | Outlier values (200.dp+) stay as constants |
| 9 | `composeApp/` | Nav host, settings screens | |

---

## Phase 3 — Handling Outlier Values

Some dp values should **not** become base tokens — they are layout-calculated or
one-off component sizes with no semantic reuse:

| Value | Location | Treatment |
|---|---|---|
| `200.dp`, `300.dp`, `320.dp` | Discover section max widths | `private val MaxDiscoverCardWidth = 300.dp` local constant |
| `360.dp`, `400.dp`, `500.dp` | Search bar widths | `private val SearchBarMaxWidth = 360.dp` |
| `420.dp`, `525.dp`, `550.dp` | Park & ride sheet heights | `private val ParkRideSheetMaxHeight = 550.dp` |
| `0.5.dp` | Dividers | `StrokeTokens.Hairline` |
| `3.dp` | Some timeline dots | `ComponentTokens.TimelineDotSize` variant |
| `100.dp`, `104.dp`, `108.dp`, `120.dp` | One-off image heights | Local `private val` constants |
| `180.dp` | Journey map card | Local `private val` |

Rule: **any value used ≥ 2 times across ≥ 2 files → becomes a token**. Single-use
layout values → `private val` local constant in the file, still no hardcoded literal.

---

## Phase 4 — Validation

After each module migration:

```bash
# No hardcoded .dp should remain (except the token definition files themselves)
grep -r "\.dp" --include="*.kt" \
  --exclude-dir="tokens" \
  feature/trip-planner/ui/src/commonMain/ | grep -v "//.*\.dp"
```

Final check across entire codebase:
```bash
grep -rn "= [0-9]\+\.dp\|([0-9]\+)\.dp\|[0-9]\+\.dp)" \
  --include="*.kt" \
  --exclude-dir="tokens" \
  feature/ taj/components/ discover/ composeApp/
```

Should return zero results (only token definition files contain raw `.dp` values).

---

## File Creation Checklist

### New files to create
- [ ] `taj/src/.../taj/tokens/SpacingTokens.kt`
- [ ] `taj/src/.../taj/tokens/RadiusTokens.kt`
- [ ] `taj/src/.../taj/tokens/IconSizeTokens.kt`
- [ ] `taj/src/.../taj/tokens/StrokeTokens.kt`
- [ ] `taj/src/.../taj/tokens/ComponentTokens.kt`
- [ ] `taj/src/.../taj/theme/KrailDimensions.kt`

### Files to modify
- [ ] `taj/src/.../taj/theme/Theme.kt` — add `dimensions` to `KrailTheme` object and `CompositionLocalProvider`
- [ ] `taj/src/.../taj/tokens/ButtonTokens.kt` — replace raw `48.dp` with `IconSizeTokens.XXXL`
- [ ] `taj/src/.../taj/tokens/BadgeTokens.kt` — replace raw values with spacing/component tokens
- [ ] `taj/src/.../taj/tokens/TextFieldTokens.kt` — replace raw values

### Migrate (dp → token) — module order
- [ ] `taj/components/` — Button, InfoTile, RoundButton, etc.
- [ ] `feature/trip-planner/ui/savedtrips/` — SavedTripsScreen, SavedTripCard
- [ ] `feature/trip-planner/ui/timetable/` — TimeTableScreen, JourneyCard
- [ ] `feature/trip-planner/ui/tracktrip/` — TrackTripScreen
- [ ] `feature/trip-planner/ui/components/` — TrackedLegView, ErrorMessage, etc.
- [ ] `feature/trip-planner/ui/journeymap/` — JourneyMap, LiveVehicleLayer
- [ ] `discover/ui/` — DiscoverScreen, DiscoverCard
- [ ] `feature/track/ui/` — TrackingCard, etc.
- [ ] `feature/departures/ui/`
- [ ] `feature/park-ride/ui/`
- [ ] `core/maps/ui/`
- [ ] `composeApp/`

---

## Usage Examples (after migration)

```kotlin
// Before
Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
RoundedCornerShape(16.dp)
Modifier.size(24.dp)

// After
val dim = KrailTheme.dimensions
Modifier.padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingM)
RoundedCornerShape(dim.cardCornerRadius)
Modifier.size(dim.iconM)

// Or for non-composable contexts (tokens directly):
Modifier.padding(SpacingTokens.XL)
```

---

## Open Questions / Decisions Needed

1. **`staticCompositionLocalOf` vs `compositionLocalOf`** — dimensions don't change at
   runtime (no dark/light variant), so `staticCompositionLocalOf` is correct and avoids
   recomposition cost. Confirm this is intentional (no tablet/compact size class variant planned).

2. **Adaptive sizing** — `DpExt.kt` already has `toAdaptiveSize()` for font-scale. Should
   page padding and icon sizes also scale with font size, or remain fixed? Recommend:
   spacing stays fixed, icon sizes use `toAdaptiveDecorativeIconSize()` where currently used.

3. **Tablet/large screen breakpoints** — `KrailDimensions` is a data class, so a
   `KrailTabletDimensions` variant with wider page padding could be provided via the
   CompositionLocal without changing any callsite. Worth planning for even if not implemented now.

4. **Park & Ride outlier values** — some unique sizing may be intentional design divergence.
   Review with design before replacing with standard tokens.

---

## TODO

- [ ] Write `kmp-dimension-migration` skill in `Utility/claude-skills/` based on learnings
      from this task (audit approach, token hierarchy, migration order, validation grep).
