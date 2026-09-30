package com.example.repsgrams.service

import org.junit.Assert.assertEquals
import org.junit.Test

class RestCountdownTest {
    @Test
    fun zeroAtTheDeadline() {
        val deadline = 1_700_000_000_000L
        val shown = restSecondsUntil(deadline, deadline)
        assertEquals(0, shown.remaining)
        assertEquals(0, shown.overtime)
    }

    @Test
    fun doesNotAddOneSecond() {
        val now = 5_000L
        // The old display was ((end - now) / 1000) + 1, so 999ms left still showed 1.
        assertEquals(0, restSecondsUntil(now + 999L, now).remaining)
        assertEquals(1, restSecondsUntil(now + 1_000L, now).remaining)
        assertEquals(1, restSecondsUntil(now + 1_999L, now).remaining)
        assertEquals(0, restSecondsUntil(now + 999L, now).overtime)
    }

    @Test
    fun countsUpAfterTheDeadline() {
        val deadline = 10_000L
        assertEquals(0, restSecondsUntil(deadline, deadline + 999L).overtime)
        assertEquals(1, restSecondsUntil(deadline, deadline + 1_000L).overtime)
        assertEquals(2, restSecondsUntil(deadline, deadline + 2_500L).overtime)
        assertEquals(65, restSecondsUntil(deadline, deadline + 65_000L).overtime)
        assertEquals(0, restSecondsUntil(deadline, deadline + 2_500L).remaining)
    }
}
