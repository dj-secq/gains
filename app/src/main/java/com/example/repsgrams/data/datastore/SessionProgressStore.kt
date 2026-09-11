package com.example.repsgrams.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionProgressDataStore by preferencesDataStore(name = "session_progress")

data class SessionProgress(
    val sessionId: Long,
    val blockIndex: Int,
    val exerciseIndex: Int,
    val roundNumber: Int,
    val restEndEpochMillis: Long?,
)

class SessionProgressStore(private val context: Context) {
    private object Keys {
        val sessionId = longPreferencesKey("session_id")
        val blockIndex = intPreferencesKey("block_index")
        val exerciseIndex = intPreferencesKey("exercise_index")
        val roundNumber = intPreferencesKey("round_number")
        val restEnd = longPreferencesKey("rest_end_epoch_millis")
    }

    val progress: Flow<SessionProgress?> = context.sessionProgressDataStore.data.map { values ->
        val sessionId = values[Keys.sessionId] ?: return@map null
        SessionProgress(
            sessionId,
            values[Keys.blockIndex] ?: 0,
            values[Keys.exerciseIndex] ?: 0,
            values[Keys.roundNumber] ?: 1,
            values[Keys.restEnd],
        )
    }

    suspend fun save(progress: SessionProgress) {
        context.sessionProgressDataStore.edit { values ->
            values[Keys.sessionId] = progress.sessionId
            values[Keys.blockIndex] = progress.blockIndex
            values[Keys.exerciseIndex] = progress.exerciseIndex
            values[Keys.roundNumber] = progress.roundNumber
            progress.restEndEpochMillis?.let { values[Keys.restEnd] = it } ?: values.remove(Keys.restEnd)
        }
    }

    suspend fun clear() = context.sessionProgressDataStore.edit { it.clear() }
}
