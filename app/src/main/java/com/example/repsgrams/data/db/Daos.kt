package com.example.repsgrams.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY name")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Insert suspend fun insert(exercise: ExerciseEntity): Long
    @Insert suspend fun insertAll(exercises: List<ExerciseEntity>): List<Long>
    @Update suspend fun update(exercise: ExerciseEntity)
    @Delete suspend fun delete(exercise: ExerciseEntity)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int
}

@Dao
interface WorkoutTemplateDao {
    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun getById(id: Long): WorkoutTemplateEntity?

    @Query("SELECT * FROM workout_templates ORDER BY dayLabel")
    fun observeAll(): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM workout_templates WHERE dayLabel = :dayLabel")
    suspend fun getByDayLabel(dayLabel: String): WorkoutTemplateEntity?

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE dayLabel = :dayLabel")
    fun observeTemplateGraph(dayLabel: String): Flow<WorkoutTemplateWithBlocks?>

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun getTemplateGraph(id: Long): WorkoutTemplateWithBlocks?

    @Insert suspend fun insert(template: WorkoutTemplateEntity): Long
    @Insert suspend fun insertAll(templates: List<WorkoutTemplateEntity>): List<Long>
    @Update suspend fun update(template: WorkoutTemplateEntity)
    @Delete suspend fun delete(template: WorkoutTemplateEntity)

    @Query("SELECT COUNT(*) FROM workout_templates")
    suspend fun count(): Int
}

@Dao
interface TemplateBlockDao {
    @Query("SELECT * FROM template_blocks WHERE templateId = :templateId ORDER BY orderIndex")
    fun observeForTemplate(templateId: Long): Flow<List<TemplateBlockEntity>>

    @Query("SELECT * FROM template_blocks WHERE templateId = :templateId ORDER BY orderIndex")
    suspend fun getForTemplate(templateId: Long): List<TemplateBlockEntity>

    @Insert suspend fun insert(block: TemplateBlockEntity): Long
    @Insert suspend fun insertAll(blocks: List<TemplateBlockEntity>): List<Long>
    @Update suspend fun update(block: TemplateBlockEntity)
    @Delete suspend fun delete(block: TemplateBlockEntity)

    @Query("SELECT COUNT(*) FROM template_blocks")
    suspend fun count(): Int
}

@Dao
interface TemplateBlockExerciseDao {
    @Query("SELECT * FROM template_block_exercises WHERE blockId = :blockId ORDER BY orderIndex")
    fun observeForBlock(blockId: Long): Flow<List<TemplateBlockExerciseEntity>>

    @Query("SELECT * FROM template_block_exercises WHERE blockId = :blockId ORDER BY orderIndex")
    suspend fun getForBlock(blockId: Long): List<TemplateBlockExerciseEntity>

    @Insert suspend fun insert(assignment: TemplateBlockExerciseEntity): Long
    @Insert suspend fun insertAll(assignments: List<TemplateBlockExerciseEntity>): List<Long>
    @Update suspend fun update(assignment: TemplateBlockExerciseEntity)
    @Delete suspend fun delete(assignment: TemplateBlockExerciseEntity)

    @Query("SELECT COUNT(*) FROM template_block_exercises")
    suspend fun count(): Int
}

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions WHERE completed = 0 ORDER BY startTime DESC LIMIT 1")
    fun observeActive(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE completed = 0 ORDER BY startTime DESC LIMIT 1")
    suspend fun getActive(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun observeById(id: Long): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE date = :date ORDER BY startTime")
    fun observeForDate(date: LocalDate): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE date = :date ORDER BY startTime")
    suspend fun getForDate(date: LocalDate): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE date BETWEEN :start AND :end ORDER BY date, startTime")
    fun observeInRange(start: LocalDate, end: LocalDate): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE templateId = :templateId AND date < :beforeDate ORDER BY date DESC, startTime DESC LIMIT :limit")
    suspend fun getRecentForTemplate(templateId: Long, beforeDate: LocalDate, limit: Int): List<WorkoutSessionEntity>

    @Insert suspend fun insert(session: WorkoutSessionEntity): Long
    @Update suspend fun update(session: WorkoutSessionEntity)
    @Delete suspend fun delete(session: WorkoutSessionEntity)
}

data class ExerciseSetHistoryRow(
    val sessionId: Long,
    val date: LocalDate,
    val roundNumber: Int,
    val reps: Int?,
    val durationSeconds: Int?,
    val weightKg: Float?,
)

data class CalendarSetLogRow(
    val sessionId: Long,
    val exerciseName: String,
    val roundNumber: Int,
    val reps: Int?,
    val durationSeconds: Int?,
    val weightKg: Float?,
)

@Dao
interface SetLogDao {
    @Query(
        """
        SELECT set_logs.sessionId, exercises.name AS exerciseName, set_logs.roundNumber,
               set_logs.reps, set_logs.durationSeconds, set_logs.weightKg
        FROM set_logs
        INNER JOIN workout_sessions ON workout_sessions.id = set_logs.sessionId
        INNER JOIN exercises ON exercises.id = set_logs.exerciseId
        WHERE workout_sessions.date = :date
        ORDER BY workout_sessions.startTime, set_logs.loggedAt, set_logs.id
        """,
    )
    suspend fun getForDate(date: LocalDate): List<CalendarSetLogRow>

    @Query(
        """
        SELECT * FROM set_logs
        WHERE exerciseId = :exerciseId AND sessionId = (
            SELECT prior.id FROM workout_sessions AS prior
            INNER JOIN workout_sessions AS active_session ON active_session.id = :sessionId
            WHERE prior.templateId = active_session.templateId AND prior.date < active_session.date
              AND prior.completed = 1
              AND EXISTS (SELECT 1 FROM set_logs AS logs
                          WHERE logs.sessionId = prior.id AND logs.exerciseId = :exerciseId)
            ORDER BY prior.date DESC, prior.startTime DESC, prior.id DESC LIMIT 1
        )
        ORDER BY roundNumber, loggedAt, id
        """,
    )
    suspend fun getPreviousSessionRounds(exerciseId: Long, sessionId: Long): List<SetLogEntity>

    @Query(
        """
        SELECT set_logs.* FROM set_logs
        INNER JOIN workout_sessions ON workout_sessions.id = set_logs.sessionId
        WHERE set_logs.exerciseId = :exerciseId
          AND set_logs.sessionId != :sessionId
          AND workout_sessions.completed = 1
          AND workout_sessions.templateId = (
              SELECT templateId FROM workout_sessions WHERE id = :sessionId
          )
          AND workout_sessions.date < (
              SELECT date FROM workout_sessions WHERE id = :sessionId
          )
        ORDER BY workout_sessions.date DESC, workout_sessions.startTime DESC, set_logs.loggedAt DESC
        LIMIT 1
        """,
    )
    suspend fun getPreviousForExercise(exerciseId: Long, sessionId: Long): SetLogEntity?

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY loggedAt, id")
    fun observeForSession(sessionId: Long): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY loggedAt, id")
    suspend fun getForSession(sessionId: Long): List<SetLogEntity>

    @Query(
        """
        SELECT set_logs.sessionId, workout_sessions.date, set_logs.roundNumber,
               set_logs.reps, set_logs.durationSeconds, set_logs.weightKg
        FROM set_logs
        INNER JOIN workout_sessions ON workout_sessions.id = set_logs.sessionId
        WHERE set_logs.exerciseId = :exerciseId
        ORDER BY workout_sessions.date, set_logs.roundNumber
        """,
    )
    fun observeHistoryForExercise(exerciseId: Long): Flow<List<ExerciseSetHistoryRow>>

    @Insert suspend fun insert(log: SetLogEntity): Long
    @Update suspend fun update(log: SetLogEntity)
    @Delete suspend fun delete(log: SetLogEntity)

    @Query("SELECT COUNT(*) FROM set_logs")
    suspend fun count(): Int
}

@Dao
interface SupplementLogDao {
    @Query("SELECT * FROM supplement_logs WHERE date = :date")
    fun observeForDate(date: LocalDate): Flow<SupplementLogEntity?>

    @Query("SELECT * FROM supplement_logs WHERE date = :date")
    suspend fun getForDate(date: LocalDate): SupplementLogEntity?

    @Query("SELECT * FROM supplement_logs WHERE date BETWEEN :start AND :end ORDER BY date")
    fun observeInRange(start: LocalDate, end: LocalDate): Flow<List<SupplementLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: SupplementLogEntity): Long
}

@Dao
interface BodyweightLogDao {
    @Query("SELECT * FROM bodyweight_logs WHERE date = :date")
    suspend fun getForDate(date: LocalDate): BodyweightLogEntity?

    @Query("SELECT * FROM bodyweight_logs ORDER BY date DESC LIMIT 1")
    fun observeLatest(): Flow<BodyweightLogEntity?>

    @Query("SELECT * FROM bodyweight_logs WHERE date BETWEEN :start AND :end ORDER BY date")
    fun observeInRange(start: LocalDate, end: LocalDate): Flow<List<BodyweightLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: BodyweightLogEntity): Long
}

@Dao
interface SupplyInventoryDao {
    @Query("SELECT * FROM supply_inventory ORDER BY type")
    fun observeAll(): Flow<List<SupplyInventoryEntity>>

    @Query("SELECT * FROM supply_inventory WHERE type = :type")
    suspend fun get(type: SupplyType): SupplyInventoryEntity?

    @Query("SELECT * FROM supply_inventory ORDER BY type")
    suspend fun getAll(): List<SupplyInventoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(inventory: SupplyInventoryEntity): Long

    @Query("UPDATE supply_inventory SET servingsRemaining = MAX(0, servingsRemaining + :delta) WHERE type = :type")
    suspend fun adjustRemaining(type: SupplyType, delta: Float): Int
}


@Dao
interface PersonalRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PersonalRecordEntity): Long

    @Query("SELECT * FROM personal_records WHERE exerciseId = :exerciseId AND type = :type ORDER BY value DESC, achievedDate DESC LIMIT 1")
    suspend fun getLatestRecord(exerciseId: Long, type: String): PersonalRecordEntity?

    @Query("SELECT * FROM personal_records ORDER BY achievedDate DESC")
    fun observeAll(): Flow<List<PersonalRecordEntity>>
    
    @Query("SELECT * FROM personal_records WHERE type = :type ORDER BY achievedDate ASC")
    fun observeByType(type: String): Flow<List<PersonalRecordEntity>>
}

@Dao
interface BodyMeasurementLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: BodyMeasurementLogEntity): Long

    @Query("SELECT * FROM body_measurements WHERE type = :type ORDER BY date ASC")
    fun observeByType(type: String): Flow<List<BodyMeasurementLogEntity>>

    @Query("SELECT DISTINCT type FROM body_measurements ORDER BY type ASC")
    fun observeTypes(): Flow<List<String>>
}

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(achievement: AchievementEntity): Long

    @Query("SELECT * FROM achievements ORDER BY unlockedDate DESC")
    fun observeAll(): Flow<List<AchievementEntity>>
}

@Dao
interface DatabaseMetadataDao {
    @Query("SELECT intValue FROM database_metadata WHERE `key` = :key")
    suspend fun getInt(key: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(metadata: DatabaseMetadataEntity)
}
