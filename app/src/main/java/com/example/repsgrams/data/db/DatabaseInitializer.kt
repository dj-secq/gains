package com.example.repsgrams.data.db

import androidx.room.withTransaction
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DatabaseInitializer(
    private val database: AppDatabase,
    private val clock: Clock,
) {
    private val mutex = Mutex()

    suspend fun ensureSeeded() = mutex.withLock {
        database.withTransaction {
            val metadataDao = database.databaseMetadataDao()
            val currentVersion = metadataDao.getInt(SEED_VERSION_KEY) ?: 0
            if (currentVersion >= CURRENT_SEED_VERSION) return@withTransaction

            val existingExercises = database.exerciseDao().getAll().associateBy { it.name }.toMutableMap()
            DEFAULT_EXERCISES.forEach { defaultExercise ->
                val existing = existingExercises[defaultExercise.name]
                if (existing == null) {
                    val id = database.exerciseDao().insert(defaultExercise)
                    existingExercises[defaultExercise.name] = defaultExercise.copy(id = id)
                } else {
                    var needsUpdate = false
                    var updated = existing
                    if (defaultExercise.imageAssetName != null && existing.imageAssetName == null) {
                        updated = updated.copy(imageAssetName = defaultExercise.imageAssetName)
                        needsUpdate = true
                    }
                    if (existing.muscleGroup == "Uncategorized" && defaultExercise.muscleGroup != "Uncategorized") {
                        updated = updated.copy(muscleGroup = defaultExercise.muscleGroup)
                        needsUpdate = true
                    }
                    if (needsUpdate) {
                        database.exerciseDao().update(updated)
                        existingExercises[defaultExercise.name] = updated
                    }
                }
            }
            val exerciseIds = existingExercises.mapValues { it.value.id }

            val templateDao = database.workoutTemplateDao()
            val blockDao = database.templateBlockDao()
            val assignmentDao = database.templateBlockExerciseDao()

            if (templateDao.getByDayLabel("A") == null) seedTemplate(
                template = WorkoutTemplateEntity(
                    name = "Workout A — Upper Body + Light Legs",
                    dayLabel = "A",
                    maxDurationMinutes = 40,
                ),
                blocks = workoutABlocks,
                exerciseIds = exerciseIds,
                templateDao = templateDao,
                blockDao = blockDao,
                assignmentDao = assignmentDao,
            )
            if (templateDao.getByDayLabel("B") == null) seedTemplate(
                template = WorkoutTemplateEntity(
                    name = "Workout B — Upper Body + Posterior Chain",
                    dayLabel = "B",
                    maxDurationMinutes = 40,
                ),
                blocks = workoutBBlocks,
                exerciseIds = exerciseIds,
                templateDao = templateDao,
                blockDao = blockDao,
                assignmentDao = assignmentDao,
            )

            val supplementDao = database.supplementDao()
            val existingSupplements = supplementDao.getAll()
            val wheyId = existingSupplements.firstOrNull { it.name.contains("whey", ignoreCase = true) }?.id
                ?: supplementDao.insert(com.example.repsgrams.data.db.SupplementEntity(
                    name = "Whey Protein",
                    doseAmount = 1f,
                    unit = "serving",
                    scheduleType = "workoutDayOnly",
                    containerSize = 65,
                    lowSupplyThreshold = 10,
                    colorToken = "green",
                    iconName = "water_drop"
                ))
            val creatineId = existingSupplements.firstOrNull { it.name.contains("creatine", ignoreCase = true) }?.id
                ?: supplementDao.insert(com.example.repsgrams.data.db.SupplementEntity(
                    name = "Creatine",
                    doseAmount = 5f,
                    unit = "g",
                    scheduleType = "daily",
                    containerSize = 30,
                    lowSupplyThreshold = 5,
                    colorToken = "teal",
                    iconName = "science"
                ))
            val inventoryDao = database.supplyInventoryDao()
            val today = LocalDate.now(clock)
            if (inventoryDao.get(wheyId) == null) {
                inventoryDao.insert(SupplyInventoryEntity(supplementId = wheyId, totalServings = 65, servingsRemaining = 65f, startDate = today))
            }
            if (inventoryDao.get(creatineId) == null) {
                inventoryDao.insert(SupplyInventoryEntity(supplementId = creatineId, totalServings = 30, servingsRemaining = 30f, startDate = today))
            }
            metadataDao.put(DatabaseMetadataEntity(SEED_VERSION_KEY, CURRENT_SEED_VERSION))
            }

    }

    private suspend fun seedTemplate(
        template: WorkoutTemplateEntity,
        blocks: List<SeedBlock>,
        exerciseIds: Map<String, Long>,
        templateDao: WorkoutTemplateDao,
        blockDao: TemplateBlockDao,
        assignmentDao: TemplateBlockExerciseDao,
    ) {
        val templateId = templateDao.insert(template)
        blocks.forEachIndexed { blockIndex, seedBlock ->
            val blockId = blockDao.insert(
                TemplateBlockEntity(
                    templateId = templateId,
                    label = seedBlock.label,
                    orderIndex = blockIndex,
                    kind = seedBlock.kind,
                    targetRoundsMin = seedBlock.roundsMin,
                    targetRoundsMax = seedBlock.roundsMax,
                    restSecondsBetweenRounds = seedBlock.restSeconds,
                    restSecondsAfterBlock = seedBlock.restSecondsAfterBlock,
                    isOptional = seedBlock.optional,
                ),
            )
            assignmentDao.insertAll(seedBlock.exercises.mapIndexed { exerciseIndex, target ->
                TemplateBlockExerciseEntity(
                    blockId = blockId,
                    exerciseId = checkNotNull(exerciseIds[target.name]) { "Missing seed exercise: ${target.name}" },
                    orderIndex = exerciseIndex,
                    targetValueLow = target.low,
                    targetValueHigh = target.high,
                    repType = target.repType,
                    perSide = target.perSide,
                )
            })
        }
    }

    private data class SeedTarget(
        val name: String,
        val low: Int,
        val high: Int = low,
        val perSide: Boolean = false,
        val repType: RepType = RepType.REPS,
    )

    private data class SeedBlock(
        val label: String,
        val kind: BlockKind,
        val roundsMin: Int,
        val roundsMax: Int = roundsMin,
        val restSeconds: Int? = null,
        val restSecondsAfterBlock: Int? = null,
        val optional: Boolean = false,
        val exercises: List<SeedTarget>,
    )

    private companion object {
        const val SEED_VERSION_KEY = "default_program_seed_version"
        const val CURRENT_SEED_VERSION = 4

        val DEFAULT_EXERCISES = listOf(
            ExerciseEntity(name = "Jumping Jacks", muscleGroup = "Full Body", tracksWeight = false),
            ExerciseEntity(name = "Arm Circles", muscleGroup = "Shoulders", imageAssetName = "ex_arm_circles", notes = "10 forward, 10 backward", tracksWeight = false),
            ExerciseEntity(name = "Band Pull-Aparts", muscleGroup = "Back", imageAssetName = "ex_band_pull_aparts", tracksWeight = false),
            ExerciseEntity(name = "Bodyweight Squats", muscleGroup = "Legs", imageAssetName = "ex_bodyweight_squats", tracksWeight = false),
            ExerciseEntity(name = "Easy Push-Ups", muscleGroup = "Chest", imageAssetName = "ex_easy_push_ups", tracksWeight = false),
            ExerciseEntity(name = "Pull-Ups", muscleGroup = "Back", imageAssetName = "ex_pull_ups", tracksWeight = false),
            ExerciseEntity(name = "DB Floor Press", muscleGroup = "Chest", imageAssetName = "ex_db_floor_press", tracksWeight = true),
            ExerciseEntity(name = "Single-Arm DB Row", muscleGroup = "Back", imageAssetName = "ex_single_arm_db_row", tracksWeight = true),
            ExerciseEntity(name = "DB Overhead Press", muscleGroup = "Shoulders", imageAssetName = "ex_db_overhead_press", tracksWeight = true),
            ExerciseEntity(name = "DB Bicep Curl", muscleGroup = "Arms", imageAssetName = "ex_db_bicep_curl", tracksWeight = true),
            ExerciseEntity(name = "Bulgarian Split Squat", muscleGroup = "Legs", imageAssetName = "ex_bulgarian_split_squat", tracksWeight = false),
            ExerciseEntity(name = "Hanging Knee Raise", muscleGroup = "Core", imageAssetName = "ex_hanging_knee_raise", tracksWeight = false),
            ExerciseEntity(name = "Chin-Ups", muscleGroup = "Back", imageAssetName = "ex_chin_ups", tracksWeight = false),
            ExerciseEntity(name = "Push-Ups", muscleGroup = "Chest", imageAssetName = "ex_push_ups", tracksWeight = false),
            ExerciseEntity(name = "DB Romanian Deadlift", muscleGroup = "Legs", imageAssetName = "ex_db_romanian_deadlift", tracksWeight = true),
            ExerciseEntity(name = "Band Lateral Raise", muscleGroup = "Shoulders", imageAssetName = "ex_band_lateral_raise", tracksWeight = false),
            ExerciseEntity(name = "DB Overhead Triceps Extension", muscleGroup = "Arms", imageAssetName = "ex_db_overhead_triceps_extension", tracksWeight = true),
            ExerciseEntity(name = "Band Face Pull", muscleGroup = "Back", imageAssetName = "ex_band_face_pull", tracksWeight = false),
            ExerciseEntity(name = "Hollow Body Hold", muscleGroup = "Core", imageAssetName = "ex_hollow_body_hold", tracksWeight = false),
        )

        val warmUpTargets = listOf(
            SeedTarget("Jumping Jacks", 40, 60),
            SeedTarget("Arm Circles", 20),
            SeedTarget("Band Pull-Aparts", 15),
            SeedTarget("Bodyweight Squats", 10),
            SeedTarget("Easy Push-Ups", 5, 8),
        )

        val workoutABlocks = listOf(
            SeedBlock("Warm-Up", BlockKind.WARM_UP, 1, restSecondsAfterBlock = 60, exercises = warmUpTargets),
            SeedBlock("Superset A", BlockKind.SUPERSET, 3, restSeconds = 90, restSecondsAfterBlock = 90, exercises = listOf(
                SeedTarget("Pull-Ups", 4, 6),
                SeedTarget("DB Floor Press", 8, 12),
            )),
            SeedBlock("Superset B", BlockKind.SUPERSET, 3, restSeconds = 90, restSecondsAfterBlock = 90, exercises = listOf(
                SeedTarget("Single-Arm DB Row", 8, 15, perSide = true),
                SeedTarget("DB Overhead Press", 8, 12),
            )),
            SeedBlock("Superset C", BlockKind.SUPERSET, 2, 3, 60, restSecondsAfterBlock = 60, exercises = listOf(
                SeedTarget("DB Bicep Curl", 8, 15),
                SeedTarget("Bulgarian Split Squat", 8, 12, perSide = true),
            )),
            SeedBlock("Optional Core", BlockKind.STANDARD, 2, optional = true, exercises = listOf(
                SeedTarget("Hanging Knee Raise", 10, 15),
            )),
        )

        val workoutBBlocks = listOf(
            SeedBlock("Warm-Up", BlockKind.WARM_UP, 1, restSecondsAfterBlock = 60, exercises = warmUpTargets),
            SeedBlock("Superset A", BlockKind.SUPERSET, 3, restSeconds = 90, restSecondsAfterBlock = 90, exercises = listOf(
                SeedTarget("Chin-Ups", 4, 7),
                SeedTarget("Push-Ups", 8, 15),
            )),
            SeedBlock("Superset B", BlockKind.SUPERSET, 2, 3, 60, restSecondsAfterBlock = 60, exercises = listOf(
                SeedTarget("DB Romanian Deadlift", 8, 15),
                SeedTarget("Band Lateral Raise", 12, 20),
            )),
            SeedBlock("Superset C", BlockKind.SUPERSET, 2, 3, 60, restSecondsAfterBlock = 60, exercises = listOf(
                SeedTarget("DB Overhead Triceps Extension", 10, 15),
                SeedTarget("Band Face Pull", 15, 20),
            )),
            SeedBlock("Optional Core", BlockKind.STANDARD, 2, optional = true, exercises = listOf(
                SeedTarget("Hollow Body Hold", 20, 40, repType = RepType.SECONDS),
            )),
        )
    }
}
