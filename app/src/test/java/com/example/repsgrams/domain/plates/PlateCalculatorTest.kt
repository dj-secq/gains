package com.example.repsgrams.domain.plates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateCalculatorTest {
    private val kilograms = listOf(25f, 20f, 15f, 10f, 5f, 2.5f, 1.25f)

    @Test fun sixtyIsOneTwentyPlate() {
        val result = calculatePlates(60f, 20f, kilograms)
        assertEquals(listOf(20f), result.perSide)
        assertNull(result.makeable)
        assertEquals("20", plateCaption(result, "kg"))
    }

    @Test fun oneHundredIsTwentyFiveAndFifteen() {
        val result = calculatePlates(100f, 20f, kilograms)
        assertEquals(listOf(25f, 15f), result.perSide)
        assertNull(result.makeable)
    }

    @Test fun sixtyTwoAndAHalfUsesTheSmallestPlate() {
        val result = calculatePlates(62.5f, 20f, kilograms)
        assertEquals(listOf(20f, 1.25f), result.perSide)
        assertNull(result.makeable)
        assertEquals("20+1.25", formatPlateStack(result.perSide))
    }

    @Test fun sixtyOneRoundsDownAndNamesTheGap() {
        val result = calculatePlates(61f, 20f, kilograms)
        assertEquals(listOf(20f), result.perSide)
        assertEquals(60f, result.makeable ?: 0f, 0.001f)
        assertEquals(1f, result.difference ?: 0f, 0.001f)
        assertEquals("20 · 60 kg", plateCaption(result, "kg"))
    }

    @Test fun aTargetUnderTheBarDoesNotInventPlates() {
        val result = calculatePlates(15f, 20f, kilograms)
        assertTrue(result.underBar)
        assertTrue(result.perSide.isEmpty())
        assertEquals("Under the bar", plateCaption(result, "kg"))
    }

    @Test fun pairsOfTheSamePlateAreUnlimited() {
        val result = calculatePlates(120f, 20f, listOf(25f))
        assertEquals(listOf(25f, 25f), result.perSide)
        assertNull(result.makeable)
    }

    @Test fun threeTwoAndAHalfPlatesDoNotLeaveAFloatRemainder() {
        val result = calculatePlates(35f, 20f, listOf(2.5f))
        assertEquals(listOf(2.5f, 2.5f, 2.5f), result.perSide)
        assertNull(result.makeable)
    }
}
