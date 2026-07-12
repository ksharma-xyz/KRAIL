package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.ui.graphics.Color
import app.krail.taj.resources.Res
import app.krail.taj.resources.ic_stop_label_beach
import app.krail.taj.resources.ic_stop_label_cafe
import app.krail.taj.resources.ic_stop_label_gym
import app.krail.taj.resources.ic_stop_label_home
import app.krail.taj.resources.ic_stop_label_hospital
import app.krail.taj.resources.ic_stop_label_library
import app.krail.taj.resources.ic_stop_label_university
import app.krail.taj.resources.ic_stop_label_work
import org.jetbrains.compose.resources.DrawableResource

/**
 * Suggested label names and their corresponding icons. Order is the order shown
 * in the suggestion chip row. "Uni" is listed separately from "University" — same
 * icon, since that's the name most people actually type — but only one shows as a
 * suggestion chip; the other still resolves via [stopLabelIcon]'s lookup.
 */
internal val stopLabelSuggestions: List<Pair<String, DrawableResource>> = listOf(
    "Home" to Res.drawable.ic_stop_label_home,
    "Work" to Res.drawable.ic_stop_label_work,
    "School" to Res.drawable.ic_stop_label_university,
    "Gym" to Res.drawable.ic_stop_label_gym,
    "Cafe" to Res.drawable.ic_stop_label_cafe,
    "Beach" to Res.drawable.ic_stop_label_beach,
    "Hospital" to Res.drawable.ic_stop_label_hospital,
    "Library" to Res.drawable.ic_stop_label_library,
    "Uni" to Res.drawable.ic_stop_label_university,
)

// Not shown as a suggestion chip (Uni covers that slot) but still resolves to the
// same icon when someone types the full word out.
private val stopLabelIconAliases: Map<String, DrawableResource> = mapOf(
    "university" to Res.drawable.ic_stop_label_university,
)

/** Returns the icon for [labelName] (case-insensitive), or null if no match. */
internal fun stopLabelIcon(labelName: String): DrawableResource? {
    val normalized = labelName.trim().lowercase()
    return stopLabelSuggestions.firstOrNull { (name, _) -> name.lowercase() == normalized }?.second
        ?: stopLabelIconAliases[normalized]
}

// Preset colours for the well-known label names — a "Beach" label gets a cool
// teal, not a random draw, so the common cases stay predictable and on-theme.
// Anything else gets a stable (hashed, not per-render random) pick from
// [fallbackLabelColorPalette] so a custom label always renders the same colour.
private val presetLabelColors: Map<String, Color> = mapOf(
    "home" to Color(0xFFEF7B45),
    "work" to Color(0xFF4C7EF3),
    "school" to Color(0xFF9C5CFC),
    "uni" to Color(0xFF9C5CFC),
    "university" to Color(0xFF9C5CFC),
    "gym" to Color(0xFFE0527A),
    "cafe" to Color(0xFFB07A4C),
    "beach" to Color(0xFF29B6C8),
    "hospital" to Color(0xFFEF5C6E),
    "library" to Color(0xFF4CAF7C),
)

private val fallbackLabelColorPalette: List<Color> = listOf(
    Color(0xFF4C7EF3),
    Color(0xFFE0527A),
    Color(0xFF29B6C8),
    Color(0xFF4CAF7C),
    Color(0xFFEF7B45),
    Color(0xFF9C5CFC),
    Color(0xFFB07A4C),
    Color(0xFFEF5C6E),
)

/** Deterministic colour for [labelName] — same name always resolves to the same colour. */
internal fun stopLabelColor(labelName: String): Color {
    val normalized = labelName.trim().lowercase()
    presetLabelColors[normalized]?.let { return it }
    val index = normalized.hashCode().mod(fallbackLabelColorPalette.size)
    return fallbackLabelColorPalette[index]
}
