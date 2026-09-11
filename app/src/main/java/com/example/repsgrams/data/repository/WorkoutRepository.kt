package com.example.repsgrams.data.repository

import androidx.room.withTransaction
import com.example.repsgrams.data.db.AppDatabase
import com.example.repsgrams.data.db.ExerciseEntity
import com.example.repsgrams.data.db.SetLogEntity
import com.example.repsgrams.data.db.TemplateBlockEntity
import com.example.repsgrams.data.db.TemplateBlockExerciseEntity
import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import com.example.repsgrams.domain.session.WorkoutBlock
import com.example.repsgrams.domain.session.WorkoutExercise
import com.example.repsgrams.domain.session.WorkoutPlan
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val database: AppDatabase,
    private val clock: Clock,
    private val databaseReady: Deferred<Unit>,
    private val onSessionFinished: suspend (WorkoutSessionEntity) -> Unit = {},
) {
    fun observeActiveSession(): Flow<WorkoutSessionEntity?> = database.workoutSessionDao().observeActive()

    suspend fun startSession(dayLabel: String): Long {
        databaseReady.await()
        return database.withTransaction {
            database.workoutSessionDao().getActive()?.id ?: run {
                val template = requireNotNull(database.workoutTemplateDao().getByDayLabel(dayLabel)) {
                    "Workout $dayLabel is not available"
                }
                database.workoutSessionDao().insert(
                    WorkoutSessionEntity(
                        templateId = template.id,
                        date = LocalDate.now(clock),
                        startTime = Instant.now(clock),
                    ),
                )
            }
        }
    }

    suspend fun getSession(sessionId: Long): WorkoutSessionEntity? =
        database.workoutSessionDao().getById(sessionId)

    suspend fun loadPlan(templateId: Long): WorkoutPlan {
        databaseReady.await()
        val template = requireNotNull(database.workoutTemplateDao().getById(templateId))
        val blocks = database.templateBlockDao().getForTemplate(templateId).map { block ->
            WorkoutBlock(
                id = block.id,
                label = block.label,
                kind = block.kind,
                targetRoundsMin = block.targetRoundsMin,
                targetRoundsMax = block.targetRoundsMax,
                restSecondsBetweenRounds = block.restSecondsBetweenRounds,
                isOptional = block.isOptional,
                exercises = database.templateBlockExerciseDao().getForBlock(block.id).map { link ->
                    val exercise = requireNotNull(database.exerciseDao().getById(link.exerciseId))
                    WorkoutExercise(
                        id = exercise.id,
                        name = exercise.name,
                        notes = exercise.notes,
                        tracksWeight = exercise.tracksWeight,
                        targetValueLow = link.targetValueLow,
                        targetValueHigh = link.targetValueHigh,
                        repType = link.repType,
                        perSide = link.perSide,
                    )
                },
            )
        }
        return WorkoutPlan(template.id, template.name, template.dayLabel, template.maxDurationMinutes, blocks)
    }

    suspend fun previousSet(exerciseId: Long, sessionId: Long): SetLogEntity? =
        database.setLogDao().getPreviousForExercise(exerciseId, sessionId)

    suspend fun previousSessionRounds(exerciseId: Long, sessionId: Long): List<SetLogEntity> =
        database.setLogDao().getPreviousSessionRounds(exerciseId, sessionId)

    suspend fun logSet(
        sessionId: Long,
        exerciseId: Long,
        roundNumber: Int,
        reps: Int?,
        durationSeconds: Int?,
        weightKg: Float?,
        rpeTag: String? = null,
        substitutedFrom: Long? = null,
    ): List<com.example.repsgrams.data.db.PersonalRecordEntity> {
        val session = database.workoutSessionDao().getById(sessionId) ?: return emptyList()
        val setId = database.setLogDao().insert(
            SetLogEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                roundNumber = roundNumber,
                reps = reps,
                durationSeconds = durationSeconds,
                weightKg = weightKg,
                loggedAt = Instant.now(clock),
                rpeTag = rpeTag,
                substitutedFrom = substitutedFrom,
            ),
        )
        if (reps != null && weightKg != null) {
            val prManager = PRManager(database)
            return prManager.checkAndSavePR(exerciseId, reps, weightKg, setId, session.date)
        }
        return emptyList()
    }

    suspend fun finishSession(sessionId: Long, notes: String? = null): WorkoutSessionEntity {
        val session = requireNotNull(database.workoutSessionDao().getById(sessionId))
        if (session.completed) return session
        val end = Instant.now(clock)
        val duration = session.startTime?.let { start ->
            ((end.toEpochMilli() - start.toEpochMilli()).coerceAtLeast(0) / 1_000).toInt()
        }
        val finished = session.copy(endTime = end, completed = true, durationSeconds = duration, notes = notes)
        database.workoutSessionDao().update(finished)
        onSessionFinished(finished)
        return finished
    }

    suspend fun setCount(sessionId: Long): Int = database.setLogDao().getForSession(sessionId).size

    // Phase 8 Editor CRUD operations
    fun observeAllTemplates(): Flow<List<WorkoutTemplateEntity>> = database.workoutTemplateDao().observeAll()

    fun observeBlocksForTemplate(templateId: Long): Flow<List<TemplateBlockEntity>> = 
        database.templateBlockDao().observeForTemplate(templateId)
        
    fun observeExercisesForBlock(blockId: Long): Flow<List<TemplateBlockExerciseEntity>> =
        database.templateBlockExerciseDao().observeForBlock(blockId)

    suspend fun getTemplate(id: Long): WorkoutTemplateEntity? = database.workoutTemplateDao().getById(id)
    
    suspend fun getExercise(id: Long): ExerciseEntity? = database.exerciseDao().getById(id)

    suspend fun insertTemplate(template: WorkoutTemplateEntity) {
        database.workoutTemplateDao().insert(template)
    }

    suspend fun updateTemplate(template: WorkoutTemplateEntity) {
        database.workoutTemplateDao().update(template)
    }

    suspend fun deleteTemplate(template: WorkoutTemplateEntity) {
        database.workoutTemplateDao().delete(template)
    }

    suspend fun insertBlock(block: TemplateBlockEntity) {
        database.withTransaction {
            val existing = database.templateBlockDao().getForTemplate(block.templateId)
            val newOrder = existing.size
            database.templateBlockDao().insert(block.copy(orderIndex = newOrder))
        }
    }

    suspend fun updateBlock(block: TemplateBlockEntity) {
        database.templateBlockDao().update(block)
    }

    suspend fun deleteBlock(block: TemplateBlockEntity) {
        database.withTransaction {
            database.templateBlockDao().delete(block)
            val remaining = database.templateBlockDao().getForTemplate(block.templateId)
            remaining.forEachIndexed { idx, blk -> 
                if (blk.orderIndex != idx) database.templateBlockDao().update(blk.copy(orderIndex = idx))
            }
        }
    }

    suspend fun swapBlocks(blockId1: Long, blockId2: Long, templateId: Long) {
        database.withTransaction {
            val blocks = database.templateBlockDao().getForTemplate(templateId)
            val b1 = blocks.find { it.id == blockId1 } ?: return@withTransaction
            val b2 = blocks.find { it.id == blockId2 } ?: return@withTransaction
            // Temporary order index to avoid UNIQUE constraint violation during swap
            val maxOrder = blocks.size + 10
            database.templateBlockDao().update(b1.copy(orderIndex = maxOrder))
            database.templateBlockDao().update(b2.copy(orderIndex = b1.orderIndex))
            database.templateBlockDao().update(b1.copy(orderIndex = b2.orderIndex))
        }
    }

    suspend fun insertBlockExercise(exercise: TemplateBlockExerciseEntity) {
        database.withTransaction {
            val existing = database.templateBlockExerciseDao().getForBlock(exercise.blockId)
            val newOrder = existing.size
            database.templateBlockExerciseDao().insert(exercise.copy(orderIndex = newOrder))
        }
    }

    suspend fun updateBlockExercise(exercise: TemplateBlockExerciseEntity) {
        database.templateBlockExerciseDao().update(exercise)
    }

    suspend fun deleteBlockExercise(exercise: TemplateBlockExerciseEntity) {
        database.withTransaction {
            database.templateBlockExerciseDao().delete(exercise)
            val remaining = database.templateBlockExerciseDao().getForBlock(exercise.blockId)
            remaining.forEachIndexed { idx, ex ->
                if (ex.orderIndex != idx) database.templateBlockExerciseDao().update(ex.copy(orderIndex = idx))
            }
        }
    }

    suspend fun swapBlockExercises(exerciseId1: Long, exerciseId2: Long, blockId: Long) {
        database.withTransaction {
            val exercises = database.templateBlockExerciseDao().getForBlock(blockId)
            val e1 = exercises.find { it.id == exerciseId1 } ?: return@withTransaction
            val e2 = exercises.find { it.id == exerciseId2 } ?: return@withTransaction
            val maxOrder = exercises.size + 10
            database.templateBlockExerciseDao().update(e1.copy(orderIndex = maxOrder))
            database.templateBlockExerciseDao().update(e2.copy(orderIndex = e1.orderIndex))
            database.templateBlockExerciseDao().update(e1.copy(orderIndex = e2.orderIndex))
        }
    }

    fun observeAllExercises(): Flow<List<ExerciseEntity>> = database.exerciseDao().observeAll()

    suspend fun insertExercise(exercise: ExerciseEntity) {
        database.exerciseDao().insert(exercise)
    }

    suspend fun updateExercise(exercise: ExerciseEntity) {
        database.exerciseDao().update(exercise)
    }

    suspend fun deleteExercise(exercise: ExerciseEntity): Boolean {
        return try {
            database.exerciseDao().delete(exercise)
            true
        } catch (e: Exception) {
            // Likely restricted due to existing set logs
            false
        }
    }
}
