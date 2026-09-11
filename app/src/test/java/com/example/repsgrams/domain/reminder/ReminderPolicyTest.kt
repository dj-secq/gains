package com.example.repsgrams.domain.reminder

import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPolicyTest {
    @Test fun workoutRequiresBothTogglesAWorkoutSlotAndNoStartedSession() {
        assertTrue(ReminderPolicy.shouldNotifyWorkout(true, true, "A", false))
        assertFalse(ReminderPolicy.shouldNotifyWorkout(false, true, "A", false))
        assertFalse(ReminderPolicy.shouldNotifyWorkout(true, false, "A", false))
        assertFalse(ReminderPolicy.shouldNotifyWorkout(true, true, null, false))
        assertFalse(ReminderPolicy.shouldNotifyWorkout(true, true, "B", true))
    }

    @Test fun supplementRemindersSkipTakenDoses() {
        assertTrue(ReminderPolicy.shouldNotifyCreatine(true, true, false))
        assertFalse(ReminderPolicy.shouldNotifyCreatine(true, true, true))
        assertTrue(ReminderPolicy.shouldNotifyWhey(true, true, false))
        assertFalse(ReminderPolicy.shouldNotifyWhey(false, true, false))
    }

    @Test fun nextDelayUsesTodayOrTomorrowAtLocalTime() {
        val zone = ZoneId.of("Asia/Manila")
        assertEquals(
            Duration.ofHours(2),
            ReminderTiming.delayUntilNext(
                ZonedDateTime.of(2026, 9, 11, 16, 0, 0, 0, zone), LocalTime.of(18, 0),
            ),
        )
        assertEquals(
            Duration.ofHours(22),
            ReminderTiming.delayUntilNext(
                ZonedDateTime.of(2026, 9, 11, 20, 0, 0, 0, zone), LocalTime.of(18, 0),
            ),
        )
    }
}
