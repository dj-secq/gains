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
            
            if (currentVersion < 2) {
                // Retrofit image asset names to existing exercises
                val allEx = database.exerciseDao().getAll()
                allEx.forEach { ex ->
                    val defaultEx = DEFAULT_EXERCISES.find { it.name == ex.name }
                    if (defaultEx?.imageAssetName != null && ex.imageAssetName == null) {
                        database.exerciseDao().update(ex.copy(imageAssetName = defaultEx.imageAssetName))
                    }
                }
            }

            val exerciseIds = database.exerciseDao().insertAll(DEFAULT_EXERCISES).mapIndexed { index, id ->
                DEFAULT_EXERCISES[index].name to id
            }.toMap()

            val templateDao = database.workoutTemplateDao()
            val blockDao = database.templateBlockDao()
            val assignmentDao = database.templateBlockExerciseDao()

            seedTemplate(
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
            seedTemplate(
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

            val today = LocalDate.now(clock)
            val wheyId = database.supplementDao().insert(
                com.example.repsgrams.data.db.SupplementEntity(
                    name = "Whey Protein",
                    doseAmount = 1f,
                    unit = "serving",
                    scheduleType = "workoutDayOnly",
                    containerSize = 65,
                    lowSupplyThreshold = 10,
                    colorToken = "green",
                    iconName = "water_drop"
                )
            )
            val creatineId = database.supplementDao().insert(
                com.example.repsgrams.data.db.SupplementEntity(
                    name = "Creatine",
                    doseAmount = 5f,
                    unit = "g",
                    scheduleType = "daily",
                    containerSize = 30,
                    lowSupplyThreshold = 5,
                    colorToken = "teal",
                    iconName = "science"
                )
            )
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
        val optional: Boolean = false,
        val exercises: List<SeedTarget>,
    )

    private companion object {
        const val SEED_VERSION_KEY = "default_program_seed_version"
        const val CURRENT_SEED_VERSION = 1

        val DEFAULT_EXERCISES = listOf(
            ExerciseEntity(name = "Jumping Jacks", tracksWeight = false),
            ExerciseEntity(name = "Arm Circles", imageAssetName = "ex_arm_circles", notes = "10 forward, 10 backward", tracksWeight = false),
            ExerciseEntity(name = "Band Pull-Aparts", imageAssetName = "ex_band_pull_aparts", tracksWeight = false),
            ExerciseEntity(name = "Bodyweight Squats", imageAssetName = "ex_bodyweight_squats", tracksWeight = false),
            ExerciseEntity(name = "Easy Push-Ups", imageAssetName = "ex_easy_push_ups", tracksWeight = false),
            ExerciseEntity(name = "Pull-Ups", imageAssetName = "ex_pull_ups", tracksWeight = false),
            ExerciseEntity(name = "DB Floor Press", imageAssetName = "ex_db_floor_press", tracksWeight = true),
            ExerciseEntity(name = "Single-Arm DB Row", imageAssetName = "ex_single_arm_db_row", tracksWeight = true),
            ExerciseEntity(name = "DB Overhead Press", imageAssetName = "ex_db_overhead_press", tracksWeight = true),
            ExerciseEntity(name = "DB Bicep Curl", imageAssetName = "ex_db_bicep_curl", tracksWeight = true),
            ExerciseEntity(name = "Bulgarian Split Squat", imageAssetName = "ex_bulgarian_split_squat", tracksWeight = false),
            ExerciseEntity(name = "Hanging Knee Raise", imageAssetName = "ex_hanging_knee_raise", tracksWeight = false),
            ExerciseEntity(name = "Chin-Ups", imageAssetName = "ex_chin_ups", tracksWeight = false),
            ExerciseEntity(name = "Push-Ups", imageAssetName = "ex_push_ups", tracksWeight = false),
            ExerciseEntity(name = "DB Romanian Deadlift", imageAssetName = "ex_db_romanian_deadlift", tracksWeight = true),
            ExerciseEntity(name = "Band Lateral Raise", imageAssetName = "ex_band_lateral_raise", tracksWeight = false),
            ExerciseEntity(name = "DB Overhead Triceps Extension", imageAssetName = "ex_db_overhead_triceps_extension", tracksWeight = true),
            ExerciseEntity(name = "Band Face Pull", imageAssetName = "ex_band_face_pull", tracksWeight = false),
            ExerciseEntity(name = "Hollow Body Hold", imageAssetName = "ex_hollow_body_hold", tracksWeight = false),
        )

        val warmUpTargets = listOf(
            SeedTarget("Jumping Jacks", 40, 60),
            SeedTarget("Arm Circles", 20),
            SeedTarget("Band Pull-Aparts", 15),
            SeedTarget("Bodyweight Squats", 10),
            SeedTarget("Easy Push-Ups", 5, 8),
        )

        val workoutABlocks = listOf(
            SeedBlock("Warm-Up", BlockKind.WARM_UP, 1, exercises = warmUpTargets),
            SeedBlock("Superset A", BlockKind.SUPERSET, 3, restSeconds = 90, exercises = listOf(
                SeedTarget("Pull-Ups", 4, 6),
                SeedTarget("DB Floor Press", 8, 12),
            )),
            SeedBlock("Superset B", BlockKind.SUPERSET, 3, restSeconds = 90, exercises = listOf(
                SeedTarget("Single-Arm DB Row", 8, 15, perSide = true),
                SeedTarget("DB Overhead Press", 8, 12),
            )),
            SeedBlock("Superset C", BlockKind.SUPERSET, 2, 3, 60, exercises = listOf(
                SeedTarget("DB Bicep Curl", 8, 15),
                SeedTarget("Bulgarian Split Squat", 8, 12, perSide = true),
            )),
            SeedBlock("Optional Core", BlockKind.STANDARD, 2, optional = true, exercises = listOf(
                SeedTarget("Hanging Knee Raise", 10, 15),
            )),
        )

        val workoutBBlocks = listOf(
            SeedBlock("Warm-Up", BlockKind.WARM_UP, 1, exercises = warmUpTargets),
            SeedBlock("Superset A", BlockKind.SUPERSET, 3, restSeconds = 90, exercises = listOf(
                SeedTarget("Chin-Ups", 4, 7),
                SeedTarget("Push-Ups", 8, 15),
            )),
            SeedBlock("Superset B", BlockKind.SUPERSET, 2, 3, 60, exercises = listOf(
                SeedTarget("DB Romanian Deadlift", 8, 15),
                SeedTarget("Band Lateral Raise", 12, 20),
            )),
            SeedBlock("Superset C", BlockKind.SUPERSET, 2, 3, 60, exercises = listOf(
                SeedTarget("DB Overhead Triceps Extension", 10, 15),
                SeedTarget("Band Face Pull", 15, 20),
            )),
            SeedBlock("Optional Core", BlockKind.STANDARD, 2, optional = true, exercises = listOf(
                SeedTarget("Hollow Body Hold", 20, 40, repType = RepType.SECONDS),
            )),
        )
    }
}
