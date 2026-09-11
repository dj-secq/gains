import re

with open("app/src/main/java/com/example/repsgrams/AppContainer.kt", "r") as f:
    text = f.read()

imports = """import com.example.repsgrams.data.repository.BackupManager
"""
text = text.replace("import com.example.repsgrams.data.repository.WorkoutRepository", imports + "import com.example.repsgrams.data.repository.WorkoutRepository")

prop_old = """    val progressRepository: ProgressRepository
    val reminderScheduler: ReminderScheduler"""
prop_new = """    val progressRepository: ProgressRepository
    val reminderScheduler: ReminderScheduler
    val backupManager: BackupManager"""
text = text.replace(prop_old, prop_new)

init_old = """    override val progressRepository: ProgressRepository by lazy { ProgressRepository(database, clock) }
    override val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(context) }"""
init_new = """    override val progressRepository: ProgressRepository by lazy { ProgressRepository(database, clock) }
    override val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(context) }
    override val backupManager: BackupManager by lazy { BackupManager(context) }"""
text = text.replace(init_old, init_new)

with open("app/src/main/java/com/example/repsgrams/AppContainer.kt", "w") as f:
    f.write(text)

