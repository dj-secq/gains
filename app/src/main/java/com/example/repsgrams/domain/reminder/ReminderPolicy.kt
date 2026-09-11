package com.example.repsgrams.domain.reminder

import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

object ReminderPolicy {
    fun shouldNotifyWorkout(
        masterEnabled: Boolean,
        reminderEnabled: Boolean,
        workoutDayLabel: String?,
        sessionStarted: Boolean,
    ) = masterEnabled && reminderEnabled && workoutDayLabel != null && !sessionStarted

    fun shouldNotifyCreatine(masterEnabled: Boolean, reminderEnabled: Boolean, taken: Boolean) =
        masterEnabled && reminderEnabled && !taken

    fun shouldNotifyWhey(masterEnabled: Boolean, reminderEnabled: Boolean, taken: Boolean) =
        masterEnabled && reminderEnabled && !taken
}

object ReminderTiming {
    fun delayUntilNext(now: ZonedDateTime, time: LocalTime): Duration {
        var target = now.toLocalDate().atTime(time).atZone(now.zone)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target)
    }
}
