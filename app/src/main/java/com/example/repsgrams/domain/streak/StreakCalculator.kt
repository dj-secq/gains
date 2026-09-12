package com.example.repsgrams.domain.streak

import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class StreakInfo(
    val currentWorkoutStreak: Int,
    val bestWorkoutStreak: Int,
    val currentStreak: Int = currentWorkoutStreak,
    val bestStreak: Int = bestWorkoutStreak
)

class StreakCalculator {
    fun calculate(
        today: LocalDate,
        sessions: List<WorkoutSessionEntity>,
        templates: List<WorkoutTemplateEntity>,
        graceDays: Int = 1
    ): StreakInfo {
        var currentWorkout = 0
        var bestWorkout = 0
        
        val completedSessions = sessions.filter { it.completed }.sortedBy { it.date }
        
        if (completedSessions.isNotEmpty()) {
            currentWorkout = 1
            bestWorkout = 1
            for (i in 1..completedSessions.lastIndex) {
                val prev = completedSessions[i-1]
                val curr = completedSessions[i]
                val prevTemplate = templates.find { it.id == prev.templateId }
                val restDaysAfter = prevTemplate?.restDaysAfter ?: 1
                
                val maxAllowedGap = restDaysAfter + 1 + graceDays
                val gap = ChronoUnit.DAYS.between(prev.date, curr.date).toInt()
                
                if (gap <= maxAllowedGap) {
                    currentWorkout++
                    if (currentWorkout > bestWorkout) bestWorkout = currentWorkout
                } else {
                    currentWorkout = 1
                }
            }
            
            // Check if broken right now
            val last = completedSessions.last()
            val lastTemplate = templates.find { it.id == last.templateId }
            val restDaysAfter = lastTemplate?.restDaysAfter ?: 1
            val maxAllowedGap = restDaysAfter + 1 + graceDays
            val gapToday = ChronoUnit.DAYS.between(last.date, today).toInt()
            if (gapToday > maxAllowedGap) {
                currentWorkout = 0
            }
        }
        return StreakInfo(currentWorkout, bestWorkout)
    }
}
