package com.example.repsgrams.domain.progression

data class PriorRound(val value: Int?, val weightKg: Float?)

enum class ProgressionSuggestion { INCREASE_WEIGHT, MORE_CHALLENGE }

/** Advisory only: evaluate every logged round from one prior session. */
object ProgressionCalculator {
    fun suggestionFor(
        targetValueHigh: Int,
        tracksWeight: Boolean,
        rounds: List<PriorRound>,
    ): ProgressionSuggestion? {
        if (targetValueHigh <= 0 || rounds.isEmpty() ||
            rounds.any { it.value == null || it.value < targetValueHigh }
        ) return null
        return if (tracksWeight && rounds.any { it.weightKg != null }) {
            ProgressionSuggestion.INCREASE_WEIGHT
        } else ProgressionSuggestion.MORE_CHALLENGE
    }
}
