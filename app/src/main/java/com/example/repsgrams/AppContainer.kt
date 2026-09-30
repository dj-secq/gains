package com.example.repsgrams

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Room
import com.example.repsgrams.data.BackupArchive
import com.example.repsgrams.data.BackupManager
import com.example.repsgrams.data.DatabaseGateDecision
import com.example.repsgrams.data.decideDatabaseGate
import com.example.repsgrams.data.HealthConnectManager
import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.data.datastore.PreferencesCycleSettingsRepository
import com.example.repsgrams.data.datastore.SessionProgressStore
import com.example.repsgrams.data.db.APP_SCHEMA_VERSION
import com.example.repsgrams.data.db.AppDatabase
import com.example.repsgrams.data.db.DatabaseInitializer
import com.example.repsgrams.data.repository.CalendarRepository
import com.example.repsgrams.data.repository.DefaultProgressRepository
import com.example.repsgrams.data.repository.DefaultScheduleRepository
import com.example.repsgrams.data.repository.DefaultSupplementRepository
import com.example.repsgrams.data.repository.ProgressRepository
import com.example.repsgrams.data.repository.ScheduleRepository
import com.example.repsgrams.data.repository.SupplementRepository
import com.example.repsgrams.data.repository.WorkoutRepository
import com.example.repsgrams.reminder.ReminderScheduler
import com.example.repsgrams.reminder.WorkManagerReminderScheduler
import java.io.File
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

interface AppContainer {
    val database: AppDatabase
    val cycleSettingsRepository: CycleSettingsRepository
    val scheduleRepository: ScheduleRepository
    val supplementRepository: SupplementRepository
    val workoutRepository: WorkoutRepository
    val sessionProgressStore: SessionProgressStore
    val clock: Clock
    val reminderScheduler: ReminderScheduler
    val calendarRepository: CalendarRepository
    val progressRepository: ProgressRepository
    val backupManager: BackupManager
    val healthConnectManager: HealthConnectManager
    /** Null until [startDatabaseInitialization]. An unstarted gate must not await this. */
    val databaseInitialization: Deferred<Unit>?
    val databaseGate: DatabaseGateDecision
    val databaseUnreadable: Boolean
    fun startDatabaseInitialization()
}

class DefaultAppContainer(
    context: Context,
    override val clock: Clock = Clock.systemDefaultZone(),
) : AppContainer {
    private val appContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val cycleSettingsRepository: CycleSettingsRepository =
        PreferencesCycleSettingsRepository(appContext, clock)

    override val sessionProgressStore = SessionProgressStore(appContext)

    override val reminderScheduler: ReminderScheduler =
        WorkManagerReminderScheduler(appContext, cycleSettingsRepository, clock)

    override val healthConnectManager = HealthConnectManager(appContext)

    override val backupManager = BackupManager(appContext, APP_SCHEMA_VERSION)

    @Volatile
    override var databaseInitialization: Deferred<Unit>? = null
        private set

    override lateinit var database: AppDatabase
        private set

    override lateinit var scheduleRepository: ScheduleRepository
        private set

    override lateinit var supplementRepository: SupplementRepository
        private set

    override lateinit var workoutRepository: WorkoutRepository
        private set

    override lateinit var calendarRepository: CalendarRepository
        private set

    override lateinit var progressRepository: ProgressRepository
        private set

    override val databaseGate: DatabaseGateDecision
    override val databaseUnreadable: Boolean

    init {
        val inspected = inspectDatabase()
        databaseGate = inspected.gate
        databaseUnreadable = inspected.unreadable
        inspected.userVersion?.let { backupManager.notePreOpenUserVersion(it) }
        if (databaseGate == DatabaseGateDecision.START) {
            startDatabaseInitialization()
        }
    }

    @Synchronized
    override fun startDatabaseInitialization() {
        if (databaseInitialization != null) return
        if (databaseGate == DatabaseGateDecision.IMPORT_ONLY) {
            Log.w(BACKUP_LOG, "refusing to open Room")
            return
        }
        synchronized(backupManager.fileLock) {
            if (databaseInitialization != null) return
            Log.i(BACKUP_LOG, "starting database initialization")
            val db = Room.databaseBuilder(
                appContext,
                AppDatabase::class.java,
                BackupArchive.DATABASE_NAME,
            ).addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
            ).build()
            val deferred = applicationScope.async {
                cycleSettingsRepository.ensureInitialized()
                DatabaseInitializer(db, clock).ensureSeeded()
            }
            // Repositories exist only after init starts, so the gate cannot await Room.
            database = db
            backupManager.attachDatabase(db)
            scheduleRepository = DefaultScheduleRepository(db, cycleSettingsRepository, clock)
            supplementRepository = DefaultSupplementRepository(db, clock)
            workoutRepository = WorkoutRepository(db, clock, deferred, {})
            calendarRepository = CalendarRepository(db, cycleSettingsRepository, clock, deferred)
            progressRepository = DefaultProgressRepository(db)
            databaseInitialization = deferred
        }
    }

    private fun inspectDatabase(): InspectedDatabase {
        val file = appContext.getDatabasePath(BackupArchive.DATABASE_NAME)
        if (!file.exists()) {
            Log.i(BACKUP_LOG, "database file missing; starting initialization")
            return InspectedDatabase(
                gate = decideDatabaseGate(fileExists = false, userVersion = 0, codeVersion = APP_SCHEMA_VERSION),
                userVersion = null,
                unreadable = false,
            )
        }
        val userVersion = readUserVersion(file)
        if (userVersion == null) {
            Log.e(BACKUP_LOG, "user_version unreadable so Room is not built")
            return InspectedDatabase(
                gate = DatabaseGateDecision.IMPORT_ONLY,
                userVersion = null,
                unreadable = true,
            )
        }
        val gate = decideDatabaseGate(
            fileExists = true,
            userVersion = userVersion,
            codeVersion = APP_SCHEMA_VERSION,
        )
        when (gate) {
            DatabaseGateDecision.IMPORT_ONLY -> Log.w(
                BACKUP_LOG,
                "newer file than the code so Room is not built: user_version=$userVersion code=$APP_SCHEMA_VERSION",
            )
            DatabaseGateDecision.EXPORT_THEN_CONTINUE -> Log.i(
                BACKUP_LOG,
                "user_version $userVersion is lower than $APP_SCHEMA_VERSION; Room is not built",
            )
            DatabaseGateDecision.START -> Unit
        }
        return InspectedDatabase(gate = gate, userVersion = userVersion, unreadable = false)
    }

    private fun readUserVersion(file: File): Int? {
        var sqlite: SQLiteDatabase? = null
        return try {
            @Suppress("DEPRECATION")
            sqlite = SQLiteDatabase.openDatabase(
                file.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
            val version = sqlite.rawQuery("PRAGMA user_version", null).use { cursor ->
                if (!cursor.moveToFirst()) return null
                cursor.getInt(0)
            }
            Log.i(BACKUP_LOG, "user_version read before Room opens: $version")
            Log.i(MIGRATION_LOG, "user_version seen before open: $version")
            version
        } catch (error: Exception) {
            Log.e(BACKUP_LOG, "user_version read failed: ${error.javaClass.simpleName}: ${error.message}")
            null
        } finally {
            sqlite?.close()
        }
    }

    private data class InspectedDatabase(
        val gate: DatabaseGateDecision,
        val userVersion: Int?,
        val unreadable: Boolean,
    )

    private companion object {
        const val BACKUP_LOG = "Backup"
        const val MIGRATION_LOG = "Migration"
    }
}
