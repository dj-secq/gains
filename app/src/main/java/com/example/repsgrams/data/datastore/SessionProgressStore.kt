package com.example.repsgrams.data.datastore

import android.content.Context
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
            progress.restEndEpochMillis?.let { values[Keys.restEnd] = it } ?: values.remove(Keys.restEnd)
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

    suspend fun clearRestDeadlineIfMatch(expectedEndEpochMillis: Long) {
        context.sessionProgressDataStore.edit { values ->
            if (values[Keys.restEnd] == expectedEndEpochMillis) {
                values.remove(Keys.restEnd)
                values[Keys.restAlertFired] = false
                values.remove(Keys.restCaption)
            }
        }
    }

    suspend fun clear() = context.sessionProgressDataStore.edit { it.clear() }

    private companion object {
        val alertMutex = Mutex()
    }
}
