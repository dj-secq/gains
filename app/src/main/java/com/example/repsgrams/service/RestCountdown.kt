package com.example.repsgrams.service

/**
 * Whole seconds until [endEpochMillis].
 * Zero on the deadline second. After that, [RestSeconds.overtime] counts up.
 * There is no +1, so the timer can actually show 0.
 */
fun restSecondsUntil(endEpochMillis: Long, nowEpochMillis: Long): RestSeconds {
    val signed = ((endEpochMillis - nowEpochMillis) / 1_000L).toInt()
    return if (signed >= 0) {
        RestSeconds(remaining = signed, overtime = 0)
    } else {
        RestSeconds(remaining = 0, overtime = -signed)
    }
}

data class RestSeconds(
    val remaining: Int,
    val overtime: Int,
)
