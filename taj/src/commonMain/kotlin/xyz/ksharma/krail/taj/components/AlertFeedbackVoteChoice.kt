package xyz.ksharma.krail.taj.components

/**
 * A vote a rider casts on a piece of AI-generated content (currently only the alert
 * summary). Generic on purpose — this is the vote's meaning, not the surface it appeared
 * on; the caller attaches surface context (e.g. alert id) when logging it.
 */
enum class AlertFeedbackVoteChoice { UP, DOWN }
