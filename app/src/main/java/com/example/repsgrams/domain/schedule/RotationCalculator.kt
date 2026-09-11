package com.example.repsgrams.domain.schedule

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class CycleSlot(
    val dayNumber: Int,
    val workoutDayLabel: String?,
) {
    init {
        require(dayNumber in 1..5) { "Cycle day must be between 1 and 5" }
        require(workoutDayLabel == null || workoutDayLabel == "A" || workoutDayLabel == "B")
    }

    val isWorkoutDay: Boolean get() = workoutDayLabel != null
}

object RotationCalculator {
    fun slotFor(cycleStartDate: LocalDate, targetDate: LocalDate): CycleSlot {
        val elapsedDays = ChronoUnit.DAYS.between(cycleStartDate, targetDate)
        return when (val index = Math.floorMod(elapsedDays, 5L).toInt()) {
            0 -> CycleSlot(dayNumber = 1, workoutDayLabel = "A")
            1 -> CycleSlot(dayNumber = 2, workoutDayLabel = null)
            2 -> CycleSlot(dayNumber = 3, workoutDayLabel = "B")
            3 -> CycleSlot(dayNumber = 4, workoutDayLabel = null)
            else -> CycleSlot(dayNumber = index + 1, workoutDayLabel = null)
        }
    }
}
