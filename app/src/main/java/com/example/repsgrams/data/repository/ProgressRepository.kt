package com.example.repsgrams.data.repository

import com.example.repsgrams.data.db.AppDatabase
import com.example.repsgrams.data.db.BodyweightLogEntity
import com.example.repsgrams.data.db.ExerciseEntity
import com.example.repsgrams.data.db.ExerciseSetHistoryRow
import com.example.repsgrams.data.db.SupplementLogEntity
import com.example.repsgrams.data.db.SupplyInventoryEntity
import com.example.repsgrams.data.db.SupplyType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

import com.example.repsgrams.domain.streak.StreakInfo
import com.example.repsgrams.domain.streak.StreakCalculator

interface ProgressRepository {
    fun observeStreakInfo(cycleStartDate: LocalDate, today: LocalDate): Flow<StreakInfo>
    fun observeAllExercises(): Flow<List<ExerciseEntity>>
    fun observeExerciseHistory(exerciseId: Long): Flow<List<ExerciseSetHistoryRow>>
    fun observeLatestBodyweight(): Flow<BodyweightLogEntity?>
    fun observeBodyweightHistory(start: LocalDate, end: LocalDate): Flow<List<BodyweightLogEntity>>
    fun observeSupplementLogs(start: LocalDate, end: LocalDate): Flow<List<SupplementLogEntity>>
    fun observeSupplyInventory(): Flow<List<SupplyInventoryEntity>>
    
    suspend fun logBodyweight(date: LocalDate, weightKg: Float)
    suspend fun restockSupply(type: SupplyType, totalServings: Int, date: LocalDate)
}

class DefaultProgressRepository(
    private val database: AppDatabase,
) : ProgressRepository {
    private val streakCalculator = StreakCalculator()

    override fun observeStreakInfo(cycleStartDate: LocalDate, today: LocalDate): Flow<StreakInfo> {
        return kotlinx.coroutines.flow.combine(
            database.workoutSessionDao().observeInRange(cycleStartDate, today),
            database.supplementLogDao().observeInRange(cycleStartDate, today)
        ) { sessions, supplements ->
            streakCalculator.calculate(cycleStartDate, today, sessions, supplements)
        }
    }

    override fun observeAllExercises(): Flow<List<ExerciseEntity>> {
        return database.exerciseDao().observeAll()
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

    override fun observeSupplementLogs(start: LocalDate, end: LocalDate): Flow<List<SupplementLogEntity>> {
        return database.supplementLogDao().observeInRange(start, end)
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
