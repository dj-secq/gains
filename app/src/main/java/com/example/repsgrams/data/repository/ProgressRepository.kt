package com.example.repsgrams.data.repository

import com.example.repsgrams.data.db.AppDatabase
import com.example.repsgrams.data.db.BodyweightLogEntity
import com.example.repsgrams.data.db.ExerciseEntity
import com.example.repsgrams.data.db.ExerciseSetHistoryRow
import com.example.repsgrams.data.db.SupplementIntakeLogEntity
import com.example.repsgrams.data.db.SupplyInventoryEntity
import com.example.repsgrams.data.db.SupplyType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import com.example.repsgrams.domain.streak.StreakInfo
import com.example.repsgrams.domain.streak.StreakCalculator

interface ProgressRepository {
    fun observeStreakInfo(today: LocalDate, graceDays: Int): Flow<StreakInfo>
    fun observeAllExercises(): Flow<List<ExerciseEntity>>
    fun observePersonalRecords(exerciseId: Long): Flow<List<com.example.repsgrams.data.db.PersonalRecordEntity>>
    fun observeBodyMeasurements(type: String): Flow<List<com.example.repsgrams.data.db.BodyMeasurementLogEntity>>
    suspend fun logBodyMeasurement(type: String, date: java.time.LocalDate, valueCm: Float)
    fun observeAchievements(): Flow<List<com.example.repsgrams.data.db.AchievementEntity>>
    fun observeExerciseHistory(exerciseId: Long): Flow<List<ExerciseSetHistoryRow>>
    fun observeLatestBodyweight(): Flow<BodyweightLogEntity?>
    fun observeBodyweightHistory(start: LocalDate, end: LocalDate): Flow<List<BodyweightLogEntity>>
    fun observeSupplementLogs(start: LocalDate, end: LocalDate): Flow<List<SupplementIntakeLogEntity>>
    fun observeSupplyInventory(): Flow<List<SupplyInventoryEntity>>
    
    suspend fun logBodyweight(date: LocalDate, weightKg: Float)
    suspend fun restockSupply(type: SupplyType, totalServings: Int, date: LocalDate)
}

class DefaultProgressRepository(
    private val database: AppDatabase,
) : ProgressRepository {
    private val streakCalculator = StreakCalculator()

    override fun observeStreakInfo(today: LocalDate, graceDays: Int): Flow<StreakInfo> {
        return kotlinx.coroutines.flow.combine(
            database.workoutSessionDao().observeAll(),
            database.supplementIntakeLogDao().observeAll(),
            database.workoutTemplateDao().observeAll()
        ) { sessions, supplements, templates ->
            streakCalculator.calculate(today, sessions, supplements, templates, graceDays)
        }
    }

    override fun observeAllExercises(): Flow<List<ExerciseEntity>> {
        return database.exerciseDao().observeAll()
    }

    override fun observePersonalRecords(exerciseId: Long): Flow<List<com.example.repsgrams.data.db.PersonalRecordEntity>> {
        return database.personalRecordDao().observeAll().map { list -> list.filter { it.exerciseId == exerciseId } }
    }

    override fun observeBodyMeasurements(type: String): Flow<List<com.example.repsgrams.data.db.BodyMeasurementLogEntity>> {
        return database.bodyMeasurementLogDao().observeByType(type)
    }

    override suspend fun logBodyMeasurement(type: String, date: java.time.LocalDate, valueCm: Float) {
        database.bodyMeasurementLogDao().insert(com.example.repsgrams.data.db.BodyMeasurementLogEntity(date = date, type = type, valueCm = valueCm))
    }

    override fun observeAchievements(): Flow<List<com.example.repsgrams.data.db.AchievementEntity>> {
        return database.achievementDao().observeAll()
    }

    override fun observeExerciseHistory(exerciseId: Long): Flow<List<ExerciseSetHistoryRow>> {
        return database.setLogDao().observeHistoryForExercise(exerciseId)
    }

    override fun observeLatestBodyweight(): Flow<BodyweightLogEntity?> {
        return database.bodyweightLogDao().observeLatest()
    }

    override fun observeBodyweightHistory(start: LocalDate, end: LocalDate): Flow<List<BodyweightLogEntity>> {
        return database.bodyweightLogDao().observeInRange(start, end)
    }

    override fun observeSupplementLogs(start: LocalDate, end: LocalDate): Flow<List<SupplementIntakeLogEntity>> {
        return database.supplementIntakeLogDao().observeInRange(start, end)
    }

    override fun observeSupplyInventory(): Flow<List<SupplyInventoryEntity>> {
        return database.supplyInventoryDao().observeAll()
    }

    override suspend fun logBodyweight(date: LocalDate, weightKg: Float) {
        database.bodyweightLogDao().upsert(BodyweightLogEntity(date = date, weightKg = weightKg))
    }

    override suspend fun restockSupply(type: SupplyType, totalServings: Int, date: LocalDate) {
        database.supplyInventoryDao().upsert(
            SupplyInventoryEntity(
                type = type,
                totalServings = totalServings,
                servingsRemaining = totalServings.toFloat(),
                startDate = date,
            )
        )
    }
}
