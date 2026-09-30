package com.example.repsgrams.domain.progress

import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.data.db.SupplyInventoryEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupplyStatusTest {
    private val today = LocalDate.of(2026, 9, 30)

    @Test
    fun `Supply is low only at or below the supplement threshold`() {
        val atThreshold = status(remaining = 10f, threshold = 10f)
        val below = status(remaining = 9.5f, threshold = 10f)
        val above = status(remaining = 10.1f, threshold = 10f)
        // 4 is under the function's default of 5, but this supplement's threshold is 2.
        val aboveOwnThreshold = status(remaining = 4f, threshold = 2f)

        assertTrue(atThreshold.isLow)
        assertTrue(below.isLow)
        assertFalse(above.isLow)
        assertFalse(aboveOwnThreshold.isLow)
        assertNull(atThreshold.estimatedRunOutDate)
        assertNull(below.estimatedRunOutDate)
        assertNull(above.estimatedRunOutDate)
        assertEquals(10f, atThreshold.servingsRemaining, 0f)
        assertEquals(9.5f, below.servingsRemaining, 0f)
        assertEquals(4f, aboveOwnThreshold.servingsRemaining, 0f)
    }

    private fun status(remaining: Float, threshold: Float) =
        ProgressStatsCalculator().calculateSupplyStatus(
            SupplyInventoryEntity(
                supplementId = 1,
                totalServings = 30,
                servingsRemaining = remaining,
                startDate = today,
            ),
            today,
            lowThreshold = threshold,
        )
}

class ProgressBodyweightConversionTest {
    @Test
    fun `Progress bodyweight stores kilograms and converts pounds at the boundary`() {
        assertEquals(2.2046226f, POUNDS_PER_KILOGRAM, 0f)
        assertEquals(80f, bodyweightToKilograms(80f, UnitSystem.KG), 0f)
        assertEquals(80f, bodyweightToDisplay(80f, UnitSystem.KG), 0f)
        val pounds = 176f
        val kilograms = bodyweightToKilograms(pounds, UnitSystem.LB)
        assertEquals(pounds / POUNDS_PER_KILOGRAM, kilograms, 0.0001f)
        assertEquals(pounds, bodyweightToDisplay(kilograms, UnitSystem.LB), 0.01f)
        assertEquals(1f, poundsToKilograms(POUNDS_PER_KILOGRAM), 0f)
        assertEquals(POUNDS_PER_KILOGRAM, kilogramsToPounds(1f), 0f)
    }
}
