import re

# 1. Fix AppContainer
with open("app/src/main/java/com/example/repsgrams/AppContainer.kt", "r") as f:
    text = f.read()

if "backupManager" not in text:
    text = text.replace("import com.example.repsgrams.data.repository.WorkoutRepository", "import com.example.repsgrams.data.repository.BackupManager\nimport com.example.repsgrams.data.repository.WorkoutRepository")
    
    prop_old = """    val progressRepository: ProgressRepository
    val reminderScheduler: ReminderScheduler
}"""
    prop_new = """    val progressRepository: ProgressRepository
    val reminderScheduler: ReminderScheduler
    val backupManager: BackupManager
}"""
    text = text.replace(prop_old, prop_new)

    init_old = """    override val progressRepository: ProgressRepository by lazy { ProgressRepository(database, clock) }
    override val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(context) }
}"""
    init_new = """    override val progressRepository: ProgressRepository by lazy { ProgressRepository(database, clock) }
    override val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(context) }
    override val backupManager: BackupManager by lazy { BackupManager(context) }
}"""
    text = text.replace(init_old, init_new)

with open("app/src/main/java/com/example/repsgrams/AppContainer.kt", "w") as f:
    f.write(text)

# 2. Fix ProgressRepository
with open("app/src/main/java/com/example/repsgrams/data/repository/ProgressRepository.kt", "r") as f:
    text = f.read()

if "logBodyMeasurement" not in text:
    method = """
    suspend fun logBodyMeasurement(date: LocalDate, type: String, valueCm: Float) {
        database.bodyMeasurementLogDao().insert(
            com.example.repsgrams.data.db.BodyMeasurementLogEntity(
                date = date,
                type = type,
                valueCm = valueCm
            )
        )
    }

    fun observeBodyMeasurementsByType(type: String): Flow<List<com.example.repsgrams.data.db.BodyMeasurementLogEntity>> {
        return database.bodyMeasurementLogDao().observeByType(type)
    }

    suspend fun logBodyweight(date: LocalDate, weightKg: Float) {"""
    text = text.replace("    suspend fun logBodyweight(date: LocalDate, weightKg: Float) {", method)

with open("app/src/main/java/com/example/repsgrams/data/repository/ProgressRepository.kt", "w") as f:
    f.write(text)

# 3. Fix RepsGramsApp missing rememberCoroutineScope
with open("app/src/main/java/com/example/repsgrams/ui/navigation/RepsGramsApp.kt", "r") as f:
    text = f.read()
if "import androidx.compose.runtime.rememberCoroutineScope" not in text:
    text = text.replace("import androidx.compose.ui.Modifier", "import androidx.compose.runtime.rememberCoroutineScope\nimport androidx.compose.ui.Modifier")
with open("app/src/main/java/com/example/repsgrams/ui/navigation/RepsGramsApp.kt", "w") as f:
    f.write(text)

# 4. Fix ProgressViewModel UserStats instantiation missing measurements
with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "r") as f:
    text = f.read()
if "UserStats(streak, bw, supps, supplies, settings)" in text:
    text = text.replace("UserStats(streak, bw, supps, supplies, settings)", "UserStats(streak, bw, supps, supplies, settings, emptyMap())")
with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "w") as f:
    f.write(text)

