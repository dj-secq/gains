package com.example.repsgrams.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.sessionProgressDataStore by preferencesDataStore(name = "session_progress")

data class SessionProgress(
    val sessionId: Long,
    val blockIndex: Int,
    val exerciseIndex: Int,
    val roundNumber: Int,
    val restEndEpochMillis: Long?,
    val notes: String = "",
    val restCaption: String = "",
    val restAlertFired: Boolean = false,
    /** Identifies one rest. ±15 keeps it; the next rest replaces it. */
    val restToken: Long = 0L,
    /** Extra working rounds for the current block. Not written to the template. */
    val extraRounds: Int = 0,
    /** `linkId:exerciseId` pairs for a this-session swap. */
    val substitutes: String = "",
    /** `exerciseId=sessionId` bumps already consumed in this session. */
    val appliedProgression: String = "",
    /** Exercise ids that show one warm-up row for this session. */
    val warmupExercises: String = "",
    /** Exercise ids appended to an empty workout, in order. */
    val freestyleExercises: String = "",
)

class SessionProgressStore(private val context: Context) {
    private object Keys {
        val sessionId = longPreferencesKey("session_id")
        val blockIndex = intPreferencesKey("block_index")
        val exerciseIndex = intPreferencesKey("exercise_index")
        val roundNumber = intPreferencesKey("round_number")
        val restEnd = longPreferencesKey("rest_end_epoch_millis")
        val notes = stringPreferencesKey("notes")
        val restCaption = stringPreferencesKey("rest_caption")
        val restAlertFired = booleanPreferencesKey("rest_alert_fired")
        val restToken = longPreferencesKey("rest_token")
        val extraRounds = intPreferencesKey("extra_rounds")
        val substitutes = stringPreferencesKey("substitutes")
        val appliedProgression = stringPreferencesKey("applied_progression")
        val warmupExercises = stringPreferencesKey("warmup_exercises")
        val freestyleExercises = stringPreferencesKey("freestyle_exercises")
    }

    val progress: Flow<SessionProgress?> = context.sessionProgressDataStore.data.map { values ->
        val sessionId = values[Keys.sessionId] ?: return@map null
        SessionProgress(
            sessionId = sessionId,
            blockIndex = values[Keys.blockIndex] ?: 0,
            exerciseIndex = values[Keys.exerciseIndex] ?: 0,
            roundNumber = values[Keys.roundNumber] ?: 1,
            restEndEpochMillis = values[Keys.restEnd],
            notes = values[Keys.notes] ?: "",
            restCaption = values[Keys.restCaption] ?: "",
            restAlertFired = values[Keys.restAlertFired] ?: false,
            restToken = values[Keys.restToken] ?: 0L,
            extraRounds = values[Keys.extraRounds] ?: 0,
            substitutes = values[Keys.substitutes] ?: "",
            appliedProgression = values[Keys.appliedProgression] ?: "",
            warmupExercises = values[Keys.warmupExercises] ?: "",
            freestyleExercises = values[Keys.freestyleExercises] ?: "",
        )
    }

    suspend fun save(progress: SessionProgress) {
        context.sessionProgressDataStore.edit { values ->
            values[Keys.sessionId] = progress.sessionId
            values[Keys.blockIndex] = progress.blockIndex
            values[Keys.exerciseIndex] = progress.exerciseIndex
            values[Keys.roundNumber] = progress.roundNumber
            values[Keys.notes] = progress.notes
            values[Keys.restCaption] = progress.restCaption
            values[Keys.restAlertFired] = progress.restAlertFired
            values[Keys.extraRounds] = progress.extraRounds
            values[Keys.substitutes] = progress.substitutes
            values[Keys.appliedProgression] = progress.appliedProgression
            values[Keys.warmupExercises] = progress.warmupExercises
            values[Keys.freestyleExercises] = progress.freestyleExercises
            val end = progress.restEndEpochMillis
            if (end != null) {
                values[Keys.restEnd] = end
                if (progress.restToken != 0L) values[Keys.restToken] = progress.restToken
            } else {
                values.remove(Keys.restEnd)
                values.remove(Keys.restToken)
            }
        }
    }

    suspend fun saveNotes(sessionId: Long, notes: String) {
        context.sessionProgressDataStore.edit { values ->
            val stored = values[Keys.sessionId]
            if (stored != null && stored != sessionId) return@edit
            if (stored == null) values[Keys.sessionId] = sessionId
            values[Keys.notes] = notes
        }
    }

    suspend fun adjustRestDeadline(deltaMs: Long, nowEpochMillis: Long): Long? {
        val after = context.sessionProgressDataStore.edit { values ->
            val current = values[Keys.restEnd] ?: return@edit
            val updated = current + deltaMs
            values[Keys.restEnd] = updated
            if (updated > nowEpochMillis) values[Keys.restAlertFired] = false
        }
        return after[Keys.restEnd]
    }

    /**
     * Sets the alert flag if this deadline has not already rung.
     * Callers play sound only when this returns true, so the in-process loop and the alarm cannot double-ring.
     */
    suspend fun claimRestAlert(): Boolean = alertMutex.withLock {
        var won = false
        context.sessionProgressDataStore.edit { values ->
            val already = values[Keys.restAlertFired] == true
            if (values[Keys.restEnd] == null || already) {
                won = false
            } else {
                values[Keys.restAlertFired] = true
                won = true
            }
        }
        won
    }

    /** @return true when this call removed [expectedEndEpochMillis]. A newer deadline is left alone. */
    suspend fun clearRestDeadlineIfMatch(expectedEndEpochMillis: Long): Boolean {
        var cleared = false
        context.sessionProgressDataStore.edit { values ->
            if (values[Keys.restEnd] == expectedEndEpochMillis) {
                clearRestFields(values)
                cleared = true
            }
        }
        return cleared
    }

    /**
     * Clears the deadline only when [expectedToken] is still the stored rest.
     * An adjusted ±15 keeps the token, so Skip still matches. The next rest does not.
     */
    suspend fun clearRestForToken(expectedToken: Long): Boolean {
        if (expectedToken == 0L) return false
        var cleared = false
        context.sessionProgressDataStore.edit { values ->
            if (values[Keys.restToken] == expectedToken) {
                clearRestFields(values)
                cleared = true
            }
        }
        return cleared
    }

    private fun clearRestFields(values: MutablePreferences) {
        values.remove(Keys.restEnd)
        values[Keys.restAlertFired] = false
        values.remove(Keys.restCaption)
        values.remove(Keys.restToken)
    }

    suspend fun clear() = context.sessionProgressDataStore.edit { it.clear() }

    private companion object {
        val alertMutex = Mutex()
    }
}
