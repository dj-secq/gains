package com.example.repsgrams.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.Mass
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class HealthConnectManager(private val context: Context) {
    private val client by lazy { 
        if (HealthConnectClient.isProviderAvailable(context)) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    suspend fun writeWorkoutSession(startTime: Instant, endTime: Instant, title: String) {
        val hcClient = client ?: return
        try {
            val record = ExerciseSessionRecord(
                startTime = startTime,
                startZoneOffset = ZoneId.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneId.systemDefault().rules.getOffset(endTime),
                exerciseType = androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_WORKOUT,
                title = title
            )
            hcClient.insertRecords(listOf(record))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun writeBodyweight(time: Instant, weightKg: Float) {
        val hcClient = client ?: return
        try {
            val record = WeightRecord(
                time = time,
                zoneOffset = ZoneId.systemDefault().rules.getOffset(time),
                weight = Mass.kilograms(weightKg.toDouble())
            )
            hcClient.insertRecords(listOf(record))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
