import re
with open("app/src/main/java/com/example/repsgrams/data/db/Entities.kt", "r") as f:
    text = f.read()

target_supp_logs = """@Entity(
    tableName = "supplement_logs",
    indices = [Index(value = ["date"], unique = true)],
)
data class SupplementLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val wheyTaken: Boolean = false,
    val wheyServings: Float = 0f,
    val creatineTaken: Boolean = false,
    val creatineGrams: Float = 0f,
)"""

replacement_supp_logs = """@Entity(tableName = "supplements")
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
)"""
text = text.replace(target_supp_logs, replacement_supp_logs)

target_inventory = """@Entity(
    tableName = "supply_inventory",
    indices = [Index(value = ["type"], unique = true)],
)
data class SupplyInventoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: SupplyType,
    val totalServings: Int,
    val servingsRemaining: Float,
    val startDate: LocalDate,
)"""
replacement_inventory = """@Entity(
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
)"""
text = text.replace(target_inventory, replacement_inventory)

with open("app/src/main/java/com/example/repsgrams/data/db/Entities.kt", "w") as f:
    f.write(text)
