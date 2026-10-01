package com.example.repsgrams.data

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.units.Mass
import java.time.Instant
import java.time.ZoneId

class HealthConnectManager(private val context: Context) {
    suspend fun writeWorkoutSession(startTime: Instant, endTime: Instant, title: String) {
        try {
            val client = clientOrNull() ?: return
            val record = ExerciseSessionRecord(
                startTime = startTime,
                startZoneOffset = ZoneId.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneId.systemDefault().rules.getOffset(endTime),
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
                title = title,
            )
            client.insertRecords(listOf(record))
        } catch (error: Exception) {
            Log.e(LOG, "workout write failed: ${error.javaClass.simpleName}: ${error.message}")
        }
    }

    suspend fun writeBodyweight(time: Instant, weightKg: Float) {
        try {
            val client = clientOrNull() ?: return
            val record = WeightRecord(
                time = time,
                zoneOffset = ZoneId.systemDefault().rules.getOffset(time),
                weight = Mass.kilograms(weightKg.toDouble()),
            )
            client.insertRecords(listOf(record))
        } catch (error: Exception) {
            Log.e(LOG, "bodyweight write failed: ${error.javaClass.simpleName}: ${error.message}")
        }
    }

    private suspend fun clientOrNull(): HealthConnectClient? {
        if (HealthConnectClient.getSdkStatus(context, PROVIDER_PACKAGE) != HealthConnectClient.SDK_AVAILABLE) {
            return null
        }
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        if (!granted.containsAll(WRITE_PERMISSIONS)) return null
        return client
    }

    private companion object {
        const val LOG = "HealthConnect"
        const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
        val WRITE_PERMISSIONS = setOf(
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(WeightRecord::class),
        )
    }
}
