package com.example.repsgrams

import android.content.Context
import androidx.room.Room
import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.data.datastore.PreferencesCycleSettingsRepository
import com.example.repsgrams.data.datastore.SessionProgressStore
import com.example.repsgrams.data.db.AppDatabase
import com.example.repsgrams.data.db.DatabaseInitializer
import com.example.repsgrams.data.repository.DefaultScheduleRepository
import com.example.repsgrams.data.repository.ScheduleRepository
import com.example.repsgrams.data.repository.DefaultSupplementRepository
import com.example.repsgrams.data.repository.SupplementRepository
import com.example.repsgrams.data.repository.WorkoutRepository
import com.example.repsgrams.data.repository.CalendarRepository
import com.example.repsgrams.data.repository.ProgressRepository
import com.example.repsgrams.reminder.ReminderScheduler
import com.example.repsgrams.reminder.WorkManagerReminderScheduler
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
    val backupManager: com.example.repsgrams.data.BackupManager
    val healthConnectManager: com.example.repsgrams.data.HealthConnectManager
    val databaseInitialization: Deferred<Unit>
}

class DefaultAppContainer(
    context: Context,
    override val clock: Clock = Clock.systemDefaultZone(),
) : AppContainer {
    private val appContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "reps-and-grams.db",
    ).addMigrations(
        AppDatabase.MIGRATION_1_2,
        AppDatabase.MIGRATION_2_3,
        AppDatabase.MIGRATION_3_4,
        AppDatabase.MIGRATION_4_5,
        AppDatabase.MIGRATION_5_6,
        AppDatabase.MIGRATION_6_7,
    ).build()

    override val cycleSettingsRepository: CycleSettingsRepository =
        PreferencesCycleSettingsRepository(appContext, clock)

    override val scheduleRepository: ScheduleRepository =
        DefaultScheduleRepository(database, cycleSettingsRepository, clock)

    override val reminderScheduler: ReminderScheduler =
        WorkManagerReminderScheduler(appContext, cycleSettingsRepository, clock)

    override val supplementRepository: SupplementRepository =
        DefaultSupplementRepository(database, clock)

    override val sessionProgressStore = SessionProgressStore(appContext)

    override val databaseInitialization: Deferred<Unit> = applicationScope.async {
        cycleSettingsRepository.ensureInitialized()
        DatabaseInitializer(database, clock).ensureSeeded()
    }

    override val calendarRepository = CalendarRepository(
        database, cycleSettingsRepository, clock, databaseInitialization,
    )

    override val workoutRepository = WorkoutRepository(
        database, clock, databaseInitialization, {},
    )

    override val progressRepository = com.example.repsgrams.data.repository.DefaultProgressRepository(database)
    override val backupManager = com.example.repsgrams.data.BackupManager(appContext, database)
    override val healthConnectManager = com.example.repsgrams.data.HealthConnectManager(appContext)
}
