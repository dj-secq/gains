import re

with open("app/src/main/java/com/example/repsgrams/data/db/Entities.kt", "r") as f:
    text = f.read()

# ExerciseEntity
exercise_old = """@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String? = null,
    val tracksWeight: Boolean,
)"""
exercise_new = """@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String? = null,
    val tracksWeight: Boolean,
    val muscleGroup: String = "Uncategorized",
    val imageAssetName: String? = null,
    val isCustom: Boolean = false,
)"""
text = text.replace(exercise_old, exercise_new)

# WorkoutSessionEntity
session_old = """    val endTime: Instant? = null,
    val completed: Boolean = false,
    val durationSeconds: Int? = null,
)"""
session_new = """    val endTime: Instant? = null,
    val completed: Boolean = false,
    val durationSeconds: Int? = null,
    val notes: String? = null,
)"""
text = text.replace(session_old, session_new)

# SetLogEntity
setlog_old = """@Entity(
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
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
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
)"""
setlog_new = """@Entity(
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
)"""
text = text.replace(setlog_old, setlog_new)

with open("app/src/main/java/com/example/repsgrams/data/db/Entities.kt", "w") as f:
    f.write(text)
