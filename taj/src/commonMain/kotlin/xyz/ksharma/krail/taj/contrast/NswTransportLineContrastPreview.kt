package xyz.ksharma.krail.taj.contrast

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.core.transport.nsw.NswTransportLine
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.ensureMinimumContrast
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_surface

// ── Snapshot-test entry points ────────────────────────────────────────────────
//
// These three functions are auto-discovered by TajSnapshotTest (scans xyz.ksharma.krail.taj)
// and captured in both light and dark mode by BaseSnapshotTest's default mode iteration.
//
// When a new entry is added to NswTransportLine it automatically appears in the appropriate
// section on the next `./gradlew :taj:recordRoborazziDebug` run — no manual changes needed.
//
// Columns shown per row:
//   ● swatch  — raw brand hex colour box
//   raw key   — line key text in the *raw* brand colour (may be illegible on dark bg → shows the problem)
//   adj key   — line key text after ensureMinimumContrast(surface) → always legible
//   name      — human-readable line name
//   L: X.X✓/✗ — contrast ratio on the light surface
//   D: X.X✓/✗ — contrast ratio on the dark surface
//   badge     — PASS / LIGHT / DARK / FIX! category

/**
 * Visual WCAG AA catalog for all NSW **train** lines.
 *
 * TajSnapshotTest captures this in:
 *   • light_normal  (1.0× font scale)
 *   • light_xlarge  (2.0× font scale — accessibility)
 *   • dark_normal   (1.0× font scale)
 */
@ScreenshotTest
@Preview(name = "Trains — Light", showBackground = true, widthDp = 420)
@Preview(name = "Trains — Dark", showBackground = true, widthDp = 420, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Composable
private fun NswTransportLineTrainContrastPreview() {
    PreviewTheme {
        LineContrastSection(
            sectionTitle = "Trains",
            lines = NswTransportLine.entries.filter { it.isTrainLine() },
        )
    }
}

/**
 * Visual WCAG AA catalog for all NSW **ferry** lines.
 */
@ScreenshotTest
@Preview(name = "Ferries — Light", showBackground = true, widthDp = 420)
@Preview(name = "Ferries — Dark", showBackground = true, widthDp = 420, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Composable
private fun NswTransportLineFerryContrastPreview() {
    PreviewTheme {
        LineContrastSection(
            sectionTitle = "Ferries",
            lines = NswTransportLine.entries.filter { it.isFerryLine() },
        )
    }
}

/**
 * Visual WCAG AA catalog for all NSW **light rail** lines.
 */
@ScreenshotTest
@Preview(name = "Light Rail — Light", showBackground = true, widthDp = 420)
@Preview(name = "Light Rail — Dark", showBackground = true, widthDp = 420, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Composable
private fun NswTransportLineLightRailContrastPreview() {
    PreviewTheme {
        LineContrastSection(
            sectionTitle = "Light Rail",
            lines = NswTransportLine.entries.filter { it.isLightRailLine() },
        )
    }
}

// ── Internal composables ──────────────────────────────────────────────────────

@Composable
private fun LineContrastSection(
    sectionTitle: String,
    lines: List<NswTransportLine>,
    modifier: Modifier = Modifier,
) {
    // Surface adapts automatically: PreviewTheme → KrailTheme → light or dark based on uiMode
    val surface = KrailTheme.colors.surface

    // Compute contrast results once; both light and dark ratios are always shown side-by-side
    // so the designer can compare them at a glance regardless of which theme variant they are
    // currently looking at.
    val analyzer = remember {
        ContrastAnalyzer(
            lightSurface = md_theme_light_surface,
            darkSurface = md_theme_dark_surface,
            minContrast = ContrastAnalyzer.TEXT_CONTRAST_AA,
        )
    }
    val resultMap: Map<String, ContrastResult> = remember(lines) {
        analyzer
            .analyze(lines.map { ColorEntry(label = it.key, hexColor = it.hexColor) })
            .associateBy { it.entry.label }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Section header ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KrailTheme.colors.surface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = sectionTitle.uppercase(),
                style = KrailTheme.typography.titleSmall,
                color = KrailTheme.colors.softLabel,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "L-ratio  D-ratio  Cat",
                style = KrailTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = KrailTheme.colors.softLabel,
            )
        }

        // ── Legend row (explains the two key columns) ─────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KrailTheme.colors.surface)
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(20.dp)) // swatch placeholder
            Text(
                text = "raw",
                style = KrailTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = KrailTheme.colors.softLabel,
                modifier = Modifier.width(32.dp),
            )
            Text(
                text = "adj",
                style = KrailTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = KrailTheme.colors.softLabel,
                modifier = Modifier.width(36.dp),
            )
            Text(
                text = "name",
                style = KrailTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = KrailTheme.colors.softLabel,
                modifier = Modifier.weight(1f),
            )
        }

        // ── Line rows ─────────────────────────────────────────────────────────
        lines.forEach { line ->
            val result = resultMap[line.key]
            if (result != null) {
                LineContrastRow(line = line, result = result, surface = surface)
            }
        }
    }
}

@Composable
private fun LineContrastRow(
    line: NswTransportLine,
    result: ContrastResult,
    surface: Color,
    modifier: Modifier = Modifier,
) {
    val rawColor = remember(line.hexColor) { line.hexColor.hexToComposeColor() }

    // "adj" = the colour after ensureMinimumContrast — always legible on the current surface
    val adjColor = remember(rawColor, surface) { rawColor.ensureMinimumContrast(surface) }

    val lightCheck = if (result.passesLight) "✓" else "✗"
    val darkCheck = if (result.passesDark) "✓" else "✗"
    val lightIndicatorColor = if (result.passesLight) PASS_COLOR else FAIL_COLOR
    val darkIndicatorColor = if (result.passesDark) PASS_COLOR else FAIL_COLOR

    // Short category tag that fits in the narrow column
    val (categoryShort, categoryColor) = when (result.category) {
        "PASS_BOTH" -> "PASS" to PASS_COLOR
        "PREFER_LIGHT_MODE" -> "LIGHT" to WARN_COLOR
        "PREFER_DARK_MODE" -> "DARK" to WARN_COLOR
        "NEEDS_CONTRAST_FIX" -> "FIX!" to FAIL_COLOR
        else -> result.category to KrailTheme.colors.softLabel
    }

    // Friendly title-cased line name, e.g. "EASTERN_SUBURBS_ILLAWARRA" → "Eastern Suburbs Illawarra"
    val displayName = line.name
        .replace("_", " ")
        .lowercase()
        .split(" ")
        .joinToString(" ") { word -> word.replaceFirstChar { c -> c.uppercase() } }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // ── Colour swatch ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(rawColor)
                .border(0.5.dp, KrailTheme.colors.softLabel),
        )

        // ── Raw key text — intentionally shown in the *unmodified* brand colour ─
        // In dark mode this will be hard to read for low-contrast lines (T4, T8, etc.)
        // that is the whole point: it visually proves ensureMinimumContrast is needed.
        Text(
            text = line.key,
            color = rawColor,
            style = KrailTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.width(32.dp),
            maxLines = 1,
        )

        // ── Adjusted key text — always legible on the current surface ─────────
        Text(
            text = line.key,
            color = adjColor,
            style = KrailTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.width(36.dp),
            maxLines = 1,
        )

        // ── Line name ─────────────────────────────────────────────────────────
        Text(
            text = displayName,
            style = KrailTheme.typography.bodySmall,
            color = KrailTheme.colors.label,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // ── Light contrast ratio ──────────────────────────────────────────────
        Text(
            text = "L:${result.lightModeContrast.fmt1dp()}$lightCheck",
            style = KrailTheme.typography.bodySmall.copy(fontSize = 9.sp),
            color = lightIndicatorColor,
        )

        // ── Dark contrast ratio ───────────────────────────────────────────────
        Text(
            text = "D:${result.darkModeContrast.fmt1dp()}$darkCheck",
            style = KrailTheme.typography.bodySmall.copy(fontSize = 9.sp),
            color = darkIndicatorColor,
        )

        // ── Category badge ────────────────────────────────────────────────────
        Text(
            text = categoryShort,
            style = KrailTheme.typography.bodySmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = categoryColor,
        )
    }
}

// ── Formatting helper ─────────────────────────────────────────────────────────
// String.format("%.1f", value) is JVM-only; this is KMP-safe.

private fun Float.fmt1dp(): String {
    val tenths = (this * 10).toInt()
    return "${tenths / 10}.${tenths % 10}"
}

// ── "Fixes required" preview entry points ────────────────────────────────────
//
// Shows ONLY the lines whose raw brand colour fails WCAG AA on ≥ 1 surface.
// Each card has two text samples side-by-side:
//   • "as-is"  — text rendered in the *raw* brand colour → problem is visible in dark mode
//   • "fixed"  — text rendered after ensureMinimumContrast(surface) → always legible
//
// Both the Light and Dark variants are shown so you can compare:
//   Dark preview:  PREFER_LIGHT_MODE lines → as-is text is barely readable; fixed text is bright
//   Light preview: PREFER_DARK_MODE lines → as-is text is barely readable; fixed text is darker

/**
 * Trains that need contrast fixing — shown in both light and dark surfaces.
 */
@ScreenshotTest
@Preview(name = "Train Fixes — Light", showBackground = true, widthDp = 420)
@Preview(name = "Train Fixes — Dark", showBackground = true, widthDp = 420, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Composable
private fun NswTransportLineTrainFixesPreview() {
    PreviewTheme {
        FixesRequiredSection(
            sectionTitle = "Train lines needing contrast fix",
            lines = NswTransportLine.entries.filter { it.isTrainLine() },
        )
    }
}

/**
 * Ferries that need contrast fixing — shown in both light and dark surfaces.
 */
@ScreenshotTest
@Preview(name = "Ferry Fixes — Light", showBackground = true, widthDp = 420)
@Preview(name = "Ferry Fixes — Dark", showBackground = true, widthDp = 420, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Composable
private fun NswTransportLineFerryFixesPreview() {
    PreviewTheme {
        FixesRequiredSection(
            sectionTitle = "Ferry lines needing contrast fix",
            lines = NswTransportLine.entries.filter { it.isFerryLine() },
        )
    }
}

/**
 * Light rail lines that need contrast fixing — shown in both light and dark surfaces.
 */
@ScreenshotTest
@Preview(name = "Light Rail Fixes — Light", showBackground = true, widthDp = 420)
@Preview(name = "Light Rail Fixes — Dark", showBackground = true, widthDp = 420, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Composable
private fun NswTransportLineLightRailFixesPreview() {
    PreviewTheme {
        FixesRequiredSection(
            sectionTitle = "Light rail lines needing contrast fix",
            lines = NswTransportLine.entries.filter { it.isLightRailLine() },
        )
    }
}

// ── Fixes-required composables ────────────────────────────────────────────────

@Composable
private fun FixesRequiredSection(
    sectionTitle: String,
    lines: List<NswTransportLine>,
    modifier: Modifier = Modifier,
) {
    val surface = KrailTheme.colors.surface
    val analyzer = remember {
        ContrastAnalyzer(
            lightSurface = md_theme_light_surface,
            darkSurface = md_theme_dark_surface,
            minContrast = ContrastAnalyzer.TEXT_CONTRAST_AA,
        )
    }
    val problemResults = remember(lines) {
        analyzer
            .analyze(lines.map { ColorEntry(label = it.key, hexColor = it.hexColor) })
            .filter { it.category != "PASS_BOTH" }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Section header ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KrailTheme.colors.surface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = sectionTitle.uppercase(),
                style = KrailTheme.typography.titleSmall,
                color = KrailTheme.colors.softLabel,
                modifier = Modifier.weight(1f),
            )
            val count = problemResults.size
            Text(
                text = if (count == 0) "ALL PASS ✓" else "$count NEED FIX",
                style = KrailTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = if (count == 0) PASS_COLOR else FAIL_COLOR,
            )
        }

        if (problemResults.isEmpty()) {
            Text(
                text = "✓ Every line passes WCAG AA on both surfaces",
                style = KrailTheme.typography.bodySmall,
                color = PASS_COLOR,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        } else {
            problemResults.forEach { result ->
                val line = lines.first { it.key == result.entry.label }
                FixedColorRow(line = line, result = result, surface = surface)
            }
        }
    }
}

/**
 * One card per problematic line showing:
 *   [●] key  Name                       L:x.x  D:x.x  CAT
 *       as-is: [raw text]  →  fixed: [adjusted text]
 *
 * In the DARK preview, "as-is" text for PREFER_LIGHT_MODE lines will be barely legible
 * while the "fixed" text is always readable — the contrast issue is immediately obvious.
 */
@Composable
private fun FixedColorRow(
    line: NswTransportLine,
    result: ContrastResult,
    surface: Color,
    modifier: Modifier = Modifier,
) {
    val rawColor = remember(line.hexColor) { line.hexColor.hexToComposeColor() }
    val adjColor = remember(rawColor, surface) { rawColor.ensureMinimumContrast(surface) }

    val lightCheck = if (result.passesLight) "✓" else "✗"
    val darkCheck = if (result.passesDark) "✓" else "✗"
    val lightIndicatorColor = if (result.passesLight) PASS_COLOR else FAIL_COLOR
    val darkIndicatorColor = if (result.passesDark) PASS_COLOR else FAIL_COLOR

    val (categoryShort, categoryColor) = when (result.category) {
        "PASS_BOTH" -> "PASS" to PASS_COLOR
        "PREFER_LIGHT_MODE" -> "LIGHT" to WARN_COLOR
        "PREFER_DARK_MODE" -> "DARK" to WARN_COLOR
        "NEEDS_CONTRAST_FIX" -> "FIX!" to FAIL_COLOR
        else -> result.category to KrailTheme.colors.softLabel
    }

    val displayName = line.name
        .replace("_", " ")
        .lowercase()
        .split(" ")
        .joinToString(" ") { word -> word.replaceFirstChar { c -> c.uppercase() } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        // ── Row 1: swatch · key · name · ratios · badge ───────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(rawColor)
                    .border(0.5.dp, KrailTheme.colors.softLabel),
            )
            Text(
                text = line.key,
                style = KrailTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = KrailTheme.colors.label,
                modifier = Modifier.width(28.dp),
            )
            Text(
                text = displayName,
                style = KrailTheme.typography.bodySmall,
                color = KrailTheme.colors.label,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "L:${result.lightModeContrast.fmt1dp()}$lightCheck",
                style = KrailTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = lightIndicatorColor,
            )
            Text(
                text = "D:${result.darkModeContrast.fmt1dp()}$darkCheck",
                style = KrailTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = darkIndicatorColor,
            )
            Text(
                text = categoryShort,
                style = KrailTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = categoryColor,
            )
        }

        // ── Row 2: as-is color sample  →  fixed color sample ─────────────────
        // The visual contrast between "as-is" and "fixed" is the whole point:
        //   Dark preview   → PREFER_LIGHT_MODE lines: "as-is" nearly invisible, "fixed" readable
        //   Light preview  → PREFER_DARK_MODE  lines: "as-is" nearly invisible, "fixed" readable
        Row(
            modifier = Modifier.padding(start = 22.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "as-is:",
                style = KrailTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = KrailTheme.colors.softLabel,
            )
            // Raw brand colour — may be low-contrast on this surface
            Text(
                text = "「${line.key}」",
                style = KrailTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = rawColor,
            )
            Text(
                text = "→  fixed:",
                style = KrailTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = KrailTheme.colors.softLabel,
            )
            // ensureMinimumContrast result — always legible on this surface
            Text(
                text = "「${line.key}」",
                style = KrailTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = adjColor,
            )
        }
    }
}

// ── Line-group classification helpers ────────────────────────────────────────
// Based on the key naming convention used in NswTransportLine.
// Stable because the key format (T/F/L prefixes) is part of the official TfNSW naming scheme.

private fun NswTransportLine.isTrainLine(): Boolean =
    !isFerryLine() && !isLightRailLine()

private fun NswTransportLine.isFerryLine(): Boolean =
    key.startsWith("F") || key == "Stkn"

private fun NswTransportLine.isLightRailLine(): Boolean =
    key.startsWith("L") || key == "NLR"

// ── Static colour constants for pass/fail indicators ─────────────────────────
// Using literal values (not KrailTheme tokens) so the indicators are always the same
// regardless of light/dark mode — green = good, red = needs work.

private val PASS_COLOR = Color(0xFF2E7D32) // material green 800
private val FAIL_COLOR = Color(0xFFC62828) // material red 800
private val WARN_COLOR = Color(0xFFE65100) // material deep-orange 900


