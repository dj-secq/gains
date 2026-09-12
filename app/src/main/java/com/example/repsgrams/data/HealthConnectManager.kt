package com.example.repsgrams.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.units.Mass
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class HealthConnectManager(private val context: Context) {
    private val client by lazy { 
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
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
                exerciseType = 80,
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
