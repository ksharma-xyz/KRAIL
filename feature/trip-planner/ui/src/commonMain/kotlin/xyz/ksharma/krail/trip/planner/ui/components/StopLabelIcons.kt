package xyz.ksharma.krail.trip.planner.ui.components

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
 * in the suggestion chip row.
 */
internal val stopLabelSuggestions: List<Pair<String, DrawableResource>> = listOf(
    "Home" to Res.drawable.ic_stop_label_home,
    "Work" to Res.drawable.ic_stop_label_work,
    "Gym" to Res.drawable.ic_stop_label_gym,
    "Cafe" to Res.drawable.ic_stop_label_cafe,
    "Beach" to Res.drawable.ic_stop_label_beach,
    "Hospital" to Res.drawable.ic_stop_label_hospital,
    "Library" to Res.drawable.ic_stop_label_library,
    "University" to Res.drawable.ic_stop_label_university,
)

/** Returns the icon for [labelName] (case-insensitive), or null if no match. */
internal fun stopLabelIcon(labelName: String): DrawableResource? {
    val normalized = labelName.trim().lowercase()
    return stopLabelSuggestions.firstOrNull { (name, _) -> name.lowercase() == normalized }?.second
}
