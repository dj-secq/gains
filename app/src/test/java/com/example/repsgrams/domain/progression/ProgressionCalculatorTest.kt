package com.example.repsgrams.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressionCalculatorTest {
    @Test fun allRoundsMustReachTarget() {
        assertEquals(ProgressionSuggestion.INCREASE_WEIGHT,
            ProgressionCalculator.suggestionFor(12, true, listOf(PriorRound(12, 10f), PriorRound(14, 10f))))
        assertNull(ProgressionCalculator.suggestionFor(12, true,
            listOf(PriorRound(11, 10f), PriorRound(12, 10f))))
    }

    @Test fun missingAndTimedDataDoNotQualify() {
        assertNull(ProgressionCalculator.suggestionFor(12, true, emptyList()))
        assertNull(ProgressionCalculator.suggestionFor(12, true,
            listOf(PriorRound(12, 10f), PriorRound(null, null))))
    }

    @Test fun bodyweightAndUnweightedHistoryUseGenericAdvice() {
        assertEquals(ProgressionSuggestion.MORE_CHALLENGE,
            ProgressionCalculator.suggestionFor(12, false, listOf(PriorRound(12, null))))
        assertEquals(ProgressionSuggestion.MORE_CHALLENGE,
            ProgressionCalculator.suggestionFor(12, true, listOf(PriorRound(12, null))))
    }
}
