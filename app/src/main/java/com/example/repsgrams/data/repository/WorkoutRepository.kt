package com.example.repsgrams.data.repository

import androidx.room.withTransaction
import com.example.repsgrams.data.db.AppDatabase
import com.example.repsgrams.data.db.ExerciseEntity
import com.example.repsgrams.data.db.SessionKind
import com.example.repsgrams.data.db.SessionRecordRow
import com.example.repsgrams.data.db.SetLogEntity
import com.example.repsgrams.data.db.SetType
import com.example.repsgrams.data.db.TemplateBlockEntity
import com.example.repsgrams.data.db.TemplateBlockExerciseEntity
import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import com.example.repsgrams.data.db.WorkoutTemplateWithBlocks
import com.example.repsgrams.domain.session.WorkoutBlock
import com.example.repsgrams.domain.session.WorkoutExercise
import com.example.repsgrams.domain.session.WorkoutPlan
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

internal fun isSameLoggedSet(
    row: SetLogEntity,
    exerciseId: Long,
    roundNumber: Int,
    setType: SetType,
): Boolean = row.exerciseId == exerciseId && row.roundNumber == roundNumber && row.setType == setType

class WorkoutRepository(
    private val database: AppDatabase,
    private val clock: Clock,
    private val databaseReady: Deferred<Unit>,
    private val onSessionFinished: suspend (WorkoutSessionEntity) -> Unit = {},
) {
    fun observeActiveSession(): Flow<WorkoutSessionEntity?> = database.workoutSessionDao().observeActive()

    fun observeSessionsForDate(date: LocalDate): Flow<List<WorkoutSessionEntity>> =
        database.workoutSessionDao().observeForDate(date)

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

    suspend fun logRestDay(date: LocalDate = LocalDate.now(clock)) {
        databaseReady.await()
        database.withTransaction {
            val alreadyLogged = database.workoutSessionDao().getForDate(date)
                .any { it.completed && it.sessionKind == SessionKind.REST }
            if (!alreadyLogged) {
                val now = Instant.now(clock)
                database.workoutSessionDao().insert(
                    WorkoutSessionEntity(
                        templateId = null,
                        date = date,
                        startTime = now,
                        endTime = now,
                        completed = true,
                        durationSeconds = 0,
                        notes = null,
                        sessionKind = SessionKind.REST,
                    ),
                )
            }
        }
    }

    suspend fun getSession(sessionId: Long): WorkoutSessionEntity? =
        database.workoutSessionDao().getById(sessionId)

    fun observePlanGraph(templateId: Long): Flow<WorkoutTemplateWithBlocks?> =
        database.workoutTemplateDao().observeGraphById(templateId)

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
                restSecondsAfterBlock = block.restSecondsAfterBlock,
                isOptional = block.isOptional,
                exercises = database.templateBlockExerciseDao().getForBlock(block.id).map { link ->
                    val exercise = requireNotNull(database.exerciseDao().getById(link.exerciseId))
                    WorkoutExercise(
                        id = exercise.id,
                        name = exercise.name,
                        imageAssetName = exercise.imageAssetName,
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
        return WorkoutPlan(template.id, template.name, template.dayLabel, template.maxDurationMinutes, template.category, blocks)
    }

    suspend fun getSessionSets(sessionId: Long): List<SetLogEntity> = database.setLogDao().getForSession(sessionId)

    suspend fun previousSet(exerciseId: Long, roundNumber: Int, sessionId: Long): SetLogEntity? =
        database.setLogDao().getPreviousForExercise(exerciseId, roundNumber, sessionId)

    /** Same template and round, then any template at this round, then any working round. */
    suspend fun previousColumn(exerciseId: Long, roundNumber: Int, sessionId: Long): SetLogEntity? =
        database.setLogDao().getPreviousInTemplate(exerciseId, roundNumber, sessionId)
            ?: previousSet(exerciseId, roundNumber, sessionId)
            ?: database.setLogDao().getPreviousWorkingAnyRound(exerciseId, sessionId)

    suspend fun recordsForSession(sessionId: Long): List<SessionRecordRow> =
        database.personalRecordDao().recordsForSession(sessionId)

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
        setType: SetType = SetType.WORKING,
    ): Long {
        database.workoutSessionDao().getById(sessionId) ?: return -1L
        val existing = database.setLogDao().getForSession(sessionId)
            .find { isSameLoggedSet(it, exerciseId, roundNumber, setType) }

        return if (existing != null) {
            database.setLogDao().update(existing.copy(
                reps = reps,
                durationSeconds = durationSeconds,
                weightKg = weightKg,
                rpeTag = rpeTag,
                substitutedFrom = substitutedFrom,
            ))
            existing.id
        } else {
            database.setLogDao().insert(
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
                    setType = setType,
                ),
            )
        }
    }

    /**
     * Writes personal records for a finished session.
     * [eligibleExerciseIds] is the working-block set; warm-up-only exercises stay out.
     */
    suspend fun recordSessionRecords(
        sessionId: Long,
        eligibleExerciseIds: Set<Long>,
    ): List<com.example.repsgrams.data.db.PersonalRecordEntity> {
        val session = database.workoutSessionDao().getById(sessionId) ?: return emptyList()
        val prManager = PRManager(database)
        val recorded = mutableListOf<com.example.repsgrams.data.db.PersonalRecordEntity>()
        for (set in database.setLogDao().getForSession(sessionId)) {
            if (set.exerciseId !in eligibleExerciseIds) continue
            val reps = set.reps ?: continue
            val weightKg = set.weightKg ?: continue
            recorded += prManager.checkAndSavePR(set.exerciseId, reps, weightKg, set.id, session.date)
        }
        return recorded
    }

    suspend fun wouldRecordPersonalRecord(exerciseId: Long, reps: Int, weightKg: Float): Boolean =
        PRManager(database).wouldRecord(exerciseId, reps, weightKg)

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
        val programId = template.programId.takeIf { it != 0L }
            ?: checkNotNull(database.programDao().getActive()) { "No active program" }.id
        val nextOrder = (database.workoutTemplateDao().getAll().maxOfOrNull { it.orderIndex } ?: -1) + 1
        database.workoutTemplateDao().insert(
            template.copy(orderIndex = nextOrder, programId = programId),
        )
    }

    suspend fun updateTemplate(template: WorkoutTemplateEntity) {
        database.workoutTemplateDao().update(template)
    }

    suspend fun deleteTemplate(template: WorkoutTemplateEntity) {
        database.workoutTemplateDao().delete(template)
    }

    suspend fun hasTemplateHistory(templateId: Long): Boolean = database.workoutSessionDao().hasHistory(templateId)

    suspend fun swapTemplates(id1: Long, id2: Long) {
        val dao = database.workoutTemplateDao()
        val t1 = dao.getById(id1) ?: return
        val t2 = dao.getById(id2) ?: return

        val order1 = t1.orderIndex
        val order2 = t2.orderIndex
        dao.update(t1.copy(orderIndex = -1))
        dao.update(t2.copy(orderIndex = order1))
        dao.update(t1.copy(orderIndex = order2))
    }

    suspend fun updateTemplateCategory(templateId: Long, category: String) {
        val dao = database.workoutTemplateDao()
        val t = dao.getById(templateId) ?: return
        dao.update(t.copy(category = category))
    }

    suspend fun updateTemplateRestDays(templateId: Long, restDays: Int) {
        val dao = database.workoutTemplateDao()
        val t = dao.getById(templateId) ?: return
        dao.update(t.copy(restDaysAfter = restDays))
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


    suspend fun deleteSession(sessionId: Long) {
        database.withTransaction {
            // Set delete is ON DELETE SET NULL, which would keep the record and drop its source.
            database.personalRecordDao().deleteForSession(sessionId)
            val session = database.workoutSessionDao().getById(sessionId) ?: return@withTransaction
            database.workoutSessionDao().delete(session)
        }
    }

}
