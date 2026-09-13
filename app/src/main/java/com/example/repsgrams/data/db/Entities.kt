package com.example.repsgrams.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String? = null,
    val tracksWeight: Boolean,
    val muscleGroup: String = "Uncategorized",
    val imageAssetName: String? = null,
    val isCustom: Boolean = false,
)

@Entity(
    tableName = "workout_templates",
    indices = [Index(value = ["dayLabel"], unique = true)],
)
data class WorkoutTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dayLabel: String,
    val maxDurationMinutes: Int,
    val restDaysAfter: Int = 1,
    val orderIndex: Int = 0,
    val category: String = "Custom",
)

@Entity(
    tableName = "template_blocks",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("templateId"), Index(value = ["templateId", "orderIndex"], unique = true)],
)
data class TemplateBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val label: String,
    val orderIndex: Int,
    val kind: BlockKind,
    val targetRoundsMin: Int,
    val targetRoundsMax: Int,
    val restSecondsBetweenRounds: Int? = null,
    val restSecondsAfterBlock: Int? = null,
    val isOptional: Boolean = false,
)

@Entity(
    tableName = "template_block_exercises",
    foreignKeys = [
        ForeignKey(
            entity = TemplateBlockEntity::class,
            parentColumns = ["id"],
            childColumns = ["blockId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("blockId"),
        Index("exerciseId"),
        Index(value = ["blockId", "orderIndex"], unique = true),
    ],
)
data class TemplateBlockExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val blockId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val targetValueLow: Int,
    val targetValueHigh: Int,
    val repType: RepType = RepType.REPS,
    val perSide: Boolean = false,
)

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("templateId"), Index("date")],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long?,
    val date: LocalDate,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val completed: Boolean = false,
    val durationSeconds: Int? = null,
    val notes: String? = null,
)

@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["substitutedFrom"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId"), Index("substitutedFrom")],
)
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val roundNumber: Int,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val weightKg: Float? = null,
    val loggedAt: Instant,
    val rpeTag: String? = null,
    val substitutedFrom: Long? = null,
)

@Entity(tableName = "supplements")
data class SupplementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val doseAmount: Float,
    val unit: String,
    val scheduleType: String,
    val customDays: String? = null,
    val containerSize: Int,
    val lowSupplyThreshold: Int,
    val colorToken: String,
    val iconName: String,
    val isActive: Boolean = true,
)

@Entity(
    tableName = "supplement_intake_logs",
    foreignKeys = [
        ForeignKey(
            entity = SupplementEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplementId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("supplementId"),
        Index(value = ["date", "supplementId"], unique = true)
    ],
)
data class SupplementIntakeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplementId: Long,
    val date: LocalDate,
    val taken: Boolean = false,
    val actualAmount: Float,
)

@Entity(
    tableName = "bodyweight_logs",
    indices = [Index(value = ["date"], unique = true)],
)
data class BodyweightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val weightKg: Float,
)

@Entity(
    tableName = "supply_inventory",
    foreignKeys = [
        ForeignKey(
            entity = SupplementEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplementId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["supplementId"], unique = true)],
)
data class SupplyInventoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplementId: Long,
    val totalServings: Int,
    val servingsRemaining: Float,
    val startDate: LocalDate,
)


@Entity(
    tableName = "personal_records",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SetLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceSetLogId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("exerciseId"), Index("sourceSetLogId")],
)
data class PersonalRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val type: String, // "maxWeight" / "maxReps" / "estimated1RM"
    val value: Float,
    val achievedDate: LocalDate,
    val sourceSetLogId: Long?,
)

@Entity(
    tableName = "body_measurements",
    indices = [Index(value = ["date", "type"], unique = true)],
)
data class BodyMeasurementLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val type: String, // "waist" / "chest" / "arms" / "thighs" / user-defined
    val valueCm: Float,
)

@Entity(
    tableName = "achievements",
    indices = [Index(value = ["key"], unique = true)],
)
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val unlockedDate: LocalDate,
)

@Entity(tableName = "database_metadata")
data class DatabaseMetadataEntity(
    @PrimaryKey val key: String,
    val intValue: Int,
)
