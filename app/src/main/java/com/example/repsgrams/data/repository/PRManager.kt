package com.example.repsgrams.data.repository

import com.example.repsgrams.data.db.AppDatabase
import com.example.repsgrams.data.db.PersonalRecordEntity
import java.time.LocalDate

class PRManager(private val database: AppDatabase) {
    suspend fun checkAndSavePR(
        exerciseId: Long,
        reps: Int,
        weightKg: Float,
        setLogId: Long,
        date: LocalDate
    ): List<PersonalRecordEntity> {
        val newPRs = mutableListOf<PersonalRecordEntity>()
        val prDao = database.personalRecordDao()

        // Check Max Weight PR
        val currentMaxWeight = prDao.getLatestRecord(exerciseId, "maxWeight")
        if (currentMaxWeight == null || weightKg > currentMaxWeight.value) {
            val pr = PersonalRecordEntity(exerciseId = exerciseId, type = "maxWeight", value = weightKg, achievedDate = date, sourceSetLogId = setLogId)
            prDao.insert(pr)
            newPRs.add(pr)
        }

        // Check Max Reps at this exact weight PR
        val typeMaxReps = "maxReps_${weightKg}"
        val currentMaxReps = prDao.getLatestRecord(exerciseId, typeMaxReps)
        if (currentMaxReps == null || reps > currentMaxReps.value) {
            val pr = PersonalRecordEntity(exerciseId = exerciseId, type = typeMaxReps, value = reps.toFloat(), achievedDate = date, sourceSetLogId = setLogId)
            prDao.insert(pr)
            // We might not consider every rep record a major PR to celebrate, but we'll return it anyway.
            if (currentMaxReps != null) newPRs.add(pr) 
        }

        // Check Estimated 1RM PR
        val estimated1RM = weightKg * (1f + reps / 30f)
        val current1RM = prDao.getLatestRecord(exerciseId, "estimated1RM")
        if (current1RM == null || estimated1RM > current1RM.value) {
            val pr = PersonalRecordEntity(exerciseId = exerciseId, type = "estimated1RM", value = estimated1RM, achievedDate = date, sourceSetLogId = setLogId)
            prDao.insert(pr)
            newPRs.add(pr)
        }

        return newPRs
    }
}
