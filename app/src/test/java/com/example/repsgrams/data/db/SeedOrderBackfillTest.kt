package com.example.repsgrams.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeedOrderBackfillTest {
    @Test
    fun `untouched pair moves B to order 1 and two rest days`() {
        val updated = seedOrderBackfill(listOf(day("A", 0, 1, 1), day("B", 0, 1, 2)))
        assertEquals(2L, updated?.id)
        assertEquals(1, updated?.orderIndex)
        assertEquals(2, updated?.restDaysAfter)
    }

    @Test
    fun `untouched pair still claims order 1 when another template holds it`() {
        val updated = seedOrderBackfill(listOf(day("A", 0, 1, 1), day("B", 0, 1, 2), day("C", 1, 1, 3)))
        assertEquals(1, updated?.orderIndex)
        assertEquals(2, updated?.restDaysAfter)
    }

    @Test
    fun `changed B rest keeps the rest and takes the next free order`() {
        val updated = seedOrderBackfill(listOf(day("A", 0, 1, 1), day("B", 0, 3, 2)))
        assertEquals(1, updated?.orderIndex)
        assertEquals(3, updated?.restDaysAfter)
    }

    @Test
    fun `changed B rest stays put when the next order is taken`() {
        val updated = seedOrderBackfill(listOf(day("A", 0, 1, 1), day("B", 0, 3, 2), day("C", 1, 1, 3)))
        assertNull(updated)
    }

    @Test
    fun `edited A rest is left alone`() {
        assertNull(seedOrderBackfill(listOf(day("A", 0, 2, 1), day("B", 0, 1, 2))))
    }

    @Test
    fun `already ordered seed is left alone`() {
        assertNull(seedOrderBackfill(listOf(day("A", 0, 1, 1), day("B", 1, 2, 2))))
    }

    private fun day(label: String, order: Int, rest: Int, id: Long) = WorkoutTemplateEntity(
        id = id,
        name = label,
        dayLabel = label,
        maxDurationMinutes = 40,
        orderIndex = order,
        restDaysAfter = rest,
    )
}
