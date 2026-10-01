package com.example.repsgrams.domain.progression

import com.example.repsgrams.data.db.RepType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionCalculatorTest {
    @Test fun weightedRepsAtTheTopIncreaseTheLoad() {
        val sets = listOf(PriorRound(12, 80f), PriorRound(12, 80f))
        assertEquals(
            ProgressionResult.INCREASE_LOAD,
            ProgressionCalculator.decide(true, sets, 12, 2, RepType.REPS, true),
        )
    }

    @Test fun aShortSetOrAShortSessionHolds() {
        assertEquals(
            ProgressionResult.HOLD,
            ProgressionCalculator.decide(true, listOf(PriorRound(11, 80f), PriorRound(12, 80f)), 12, 2, RepType.REPS, true),
        )
        assertEquals(
            ProgressionResult.HOLD,
            ProgressionCalculator.decide(true, listOf(PriorRound(12, 80f)), 12, 2, RepType.REPS, true),
        )
        assertEquals(
            ProgressionResult.HOLD,
            ProgressionCalculator.decide(false, listOf(PriorRound(12, 80f), PriorRound(12, 80f)), 12, 2, RepType.REPS, true),
        )
        assertEquals(
            ProgressionResult.HOLD,
            ProgressionCalculator.decide(true, listOf(PriorRound(null, 80f)), 12, 1, RepType.REPS, true),
        )
    }

    @Test fun secondsAndUnweightedAdvanceWithoutALoadChange() {
        val hit = listOf(PriorRound(40, 10f))
        assertEquals(
            ProgressionResult.ADVANCE_TARGET,
            ProgressionCalculator.decide(true, hit, 40, 1, RepType.SECONDS, false),
        )
        assertEquals(
            ProgressionResult.ADVANCE_TARGET,
            ProgressionCalculator.decide(true, hit, 40, 1, RepType.SECONDS, true),
        )
        assertEquals(
            ProgressionResult.ADVANCE_TARGET,
            ProgressionCalculator.decide(true, listOf(PriorRound(12, null)), 12, 1, RepType.REPS, false),
        )
    }

    @Test fun qualifySurvivesASessionThatDoesNotContainTheExercise() {
        val exercise = 7L
        val sessionA = 1L
        val maps = ProgressionCalculator.recordOnFinish(
            ProgressionMaps(), exercise, sessionA, qualified = true, skipped = false,
        )
        val first = ProgressionCalculator.consumeOnLoad(maps, exercise, loadingSessionId = 3L)
        assertEquals(sessionA, first.applySessionId)
        assertTrue(first.maps.qualified.isEmpty())
        val second = ProgressionCalculator.consumeOnLoad(first.maps, exercise, loadingSessionId = 4L)
        assertNull(second.applySessionId)
    }

    @Test fun skipStaysUntilThatExerciseIsLoaded() {
        val exercise = 7L
        val maps = ProgressionCalculator.recordOnFinish(
            ProgressionMaps(), exercise, 1L, qualified = false, skipped = true,
        )
        val loaded = ProgressionCalculator.consumeOnLoad(maps, exercise, loadingSessionId = 3L)
        assertNull(loaded.applySessionId)
        assertFalse(loaded.maps.skipped.containsKey(exercise))
    }

    @Test fun skipThisSessionSurvivesReentryAndBlocksTheBump() {
        val maps = ProgressionCalculator.recordOnFinish(
            ProgressionMaps(), 7L, 5L, qualified = true, skipped = true,
        )
        assertFalse(maps.qualified.containsKey(7L))
        assertEquals(5L, maps.skipped[7L])
        val again = ProgressionCalculator.consumeOnLoad(maps, 7L, loadingSessionId = 5L)
        assertEquals(5L, again.maps.skipped[7L])
        assertNull(again.applySessionId)
    }

    @Test fun aSkipFromAnEarlierSessionBlocksTheStoredBump() {
        val maps = ProgressionMaps(qualified = mapOf(7L to 1L), skipped = mapOf(7L to 2L))
        val loaded = ProgressionCalculator.consumeOnLoad(maps, 7L, loadingSessionId = 4L)
        assertNull(loaded.applySessionId)
        assertTrue(loaded.maps.qualified.isEmpty())
        assertTrue(loaded.maps.skipped.isEmpty())
    }

    @Test fun mapTokensDropGarbage() {
        assertEquals(mapOf(1L to 2L, 3L to 4L), parseProgressionMap("1=2,nope,3=4"))
        assertEquals("1=2,3=4", formatProgressionMap(mapOf(1L to 2L, 3L to 4L)))
        assertTrue(parseProgressionMap("").isEmpty())
        assertTrue(parseProgressionMap(" ").isEmpty())
    }

    @Test fun sentencesNameTheNextLoadOrSayHold() {
        assertEquals("Next time 85 kg, reps from 8", progressionSentence(ProgressionResult.INCREASE_LOAD, "85 kg", 8))
        assertEquals("Hold 82.5 kg", progressionSentence(ProgressionResult.HOLD, "82.5 kg", 8))
        assertEquals("Hold", progressionSentence(ProgressionResult.HOLD, null, 8))
        assertEquals("Hold", progressionSentence(ProgressionResult.HOLD, "  ", 8))
        assertEquals("More challenge, same load", progressionSentence(ProgressionResult.ADVANCE_TARGET, null, 40))
    }
}
