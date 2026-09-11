import re

with open("app/src/main/java/com/example/repsgrams/data/db/Entities.kt", "r") as f:
    text = f.read()

new_entities = """
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
"""

text = text.replace("@Entity(tableName = \"database_metadata\")", new_entities + "\n@Entity(tableName = \"database_metadata\")")

with open("app/src/main/java/com/example/repsgrams/data/db/Entities.kt", "w") as f:
    f.write(text)

