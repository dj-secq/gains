package com.example.repsgrams.domain.streak

import com.example.repsgrams.data.db.SupplementLogEntity
import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.domain.schedule.RotationCalculator
import com.example.repsgrams.domain.schedule.CycleSlot
import java.time.LocalDate

data class StreakInfo(
    val currentStreak: Int,
    val bestStreak: Int,
)

class StreakCalculator {
    fun calculate(
        cycleStartDate: LocalDate,
        today: LocalDate,
        sessions: List<WorkoutSessionEntity>,
        supplements: List<SupplementLogEntity>,
    ): StreakInfo {
        var currentStreak = 0
        var bestStreak = 0
        
        val completedSessions = sessions.filter { it.completed }.map { it.date }.toSet()
        val takenCreatine = supplements.filter { it.creatineTaken }.map { it.date }.toSet()
        
        var date = cycleStartDate
        
        while (!date.isAfter(today)) {
            val isWorkoutDay = RotationCalculator.slotFor(cycleStartDate, date).isWorkoutDay
            val requirementMet = if (isWorkoutDay) {
                completedSessions.contains(date)
            } else {
                takenCreatine.contains(date)
            }
            
            if (requirementMet) {
                currentStreak++
                if (currentStreak > bestStreak) {
                    bestStreak = currentStreak
                }
            } else {
                if (date != today) {
                    currentStreak = 0
                }
            }
            date = date.plusDays(1)
        }
        
        return StreakInfo(currentStreak, bestStreak)
    }
}
