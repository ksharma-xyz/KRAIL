package xyz.ksharma.krail.trip.planner.ui.intro.finale

// The two numbers of the smoothstep curve, 3 x squared minus 2 x cubed, which is slow at both ends.
private const val SMOOTHSTEP_QUADRATIC = 3f
private const val SMOOTHSTEP_CUBIC = 2f

// The share of the way from [from] to [to] that [time] is, held between 0 and 1.
internal fun shareOf(time: Long, from: Long, to: Long): Float =
    if (to <= from) 1f else ((time - from).toFloat() / (to - from)).coerceIn(0f, 1f)

// Slow at both ends, so a colour or a circle starts and arrives gently.
internal fun smoothstep(share: Float): Float {
    val x = share.coerceIn(0f, 1f)
    return x * x * (SMOOTHSTEP_QUADRATIC - SMOOTHSTEP_CUBIC * x)
}
