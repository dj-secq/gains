package com.example.repsgrams.data.repository

import com.example.repsgrams.data.db.SetLogEntity
import com.example.repsgrams.data.db.SetType
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetLogIdentityTest {
    private fun row(setType: SetType) = SetLogEntity(
        sessionId = 1,
        exerciseId = 5,
        roundNumber = 1,
        loggedAt = Instant.EPOCH,
        setType = setType,
    )

    @Test
    fun `warm-up round does not match the working round`() {
        val working = row(SetType.WORKING)
        assertTrue(isSameLoggedSet(working, exerciseId = 5, roundNumber = 1, setType = SetType.WORKING))
        assertFalse(isSameLoggedSet(working, exerciseId = 5, roundNumber = 1, setType = SetType.WARMUP))
        assertFalse(isSameLoggedSet(working, exerciseId = 5, roundNumber = 2, setType = SetType.WORKING))
        assertFalse(isSameLoggedSet(working, exerciseId = 6, roundNumber = 1, setType = SetType.WORKING))
    }
}
