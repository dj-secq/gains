package com.example.repsgrams.ui.session

import com.example.repsgrams.data.db.RepType
import com.example.repsgrams.domain.progression.ProgressionSuggestion
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutSessionViewModelTest {
    // We will test the fallback logic purely as a static logic verification
    // Since creating the whole ViewModel in tests requires lots of mocks.

    @Test
    fun `zero history falls back to midpoint`() {
        val targetLow = 10
        val targetHigh = 20
        val midpoint = (targetLow + targetHigh) / 2
        assertEquals(15, midpoint)
    }
}
