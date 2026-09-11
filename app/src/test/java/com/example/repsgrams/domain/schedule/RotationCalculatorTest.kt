package com.example.repsgrams.domain.schedule

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RotationCalculatorTest {
    private val start = LocalDate.of(2026, 1, 1)

    @Test
    fun `returns every slot in the five day rotation`() {
        val expected = listOf(
            CycleSlot(1, "A"),
            CycleSlot(2, null),
            CycleSlot(3, "B"),
            CycleSlot(4, null),
            CycleSlot(5, null),
        )

        expected.forEachIndexed { offset, slot ->
            assertEquals(slot, RotationCalculator.slotFor(start, start.plusDays(offset.toLong())))
        }
    }

    @Test
    fun `wraps across multiple cycles`() {
        assertEquals(CycleSlot(1, "A"), RotationCalculator.slotFor(start, start.plusDays(10)))
        assertEquals(CycleSlot(3, "B"), RotationCalculator.slotFor(start, start.plusDays(17)))
    }

    @Test
    fun `uses floor modulo for dates before the cycle start`() {
        assertEquals(CycleSlot(5, null), RotationCalculator.slotFor(start, start.minusDays(1)))
        assertEquals(CycleSlot(1, "A"), RotationCalculator.slotFor(start, start.minusDays(5)))
    }

    @Test
    fun `calendar boundaries count elapsed dates correctly`() {
        val leapStart = LocalDate.of(2024, 2, 28)
        assertEquals(CycleSlot(2, null), RotationCalculator.slotFor(leapStart, LocalDate.of(2024, 2, 29)))
        assertEquals(CycleSlot(3, "B"), RotationCalculator.slotFor(leapStart, LocalDate.of(2024, 3, 1)))

        val yearStart = LocalDate.of(2025, 12, 31)
        assertEquals(CycleSlot(2, null), RotationCalculator.slotFor(yearStart, LocalDate.of(2026, 1, 1)))
    }
}
