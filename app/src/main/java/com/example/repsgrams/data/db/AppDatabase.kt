package com.example.repsgrams.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PersonalRecordEntity::class,
        BodyMeasurementLogEntity::class,
        AchievementEntity::class,
        ExerciseEntity::class,
        WorkoutTemplateEntity::class,
        TemplateBlockEntity::class,
        TemplateBlockExerciseEntity::class,
        WorkoutSessionEntity::class,
        SetLogEntity::class,
        SupplementLogEntity::class,
        BodyweightLogEntity::class,
        SupplyInventoryEntity::class,
        DatabaseMetadataEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun templateBlockDao(): TemplateBlockDao
    abstract fun templateBlockExerciseDao(): TemplateBlockExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun setLogDao(): SetLogDao
    abstract fun supplementLogDao(): SupplementLogDao
    abstract fun bodyweightLogDao(): BodyweightLogDao
    abstract fun supplyInventoryDao(): SupplyInventoryDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun bodyMeasurementLogDao(): BodyMeasurementLogDao
    abstract fun achievementDao(): AchievementDao
    abstract fun databaseMetadataDao(): DatabaseMetadataDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `personal_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `exerciseId` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `value` REAL NOT NULL,
                        `achievedDate` INTEGER NOT NULL,
                        `sourceSetLogId` INTEGER,
                        FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`sourceSetLogId`) REFERENCES `set_logs`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_personal_records_exerciseId` ON `personal_records` (`exerciseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_personal_records_sourceSetLogId` ON `personal_records` (`sourceSetLogId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `body_measurements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `valueCm` REAL NOT NULL
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_body_measurements_date_type` ON `body_measurements` (`date`, `type`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `achievements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `key` TEXT NOT NULL,
                        `unlockedDate` INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_achievements_key` ON `achievements` (`key`)")
            }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ExerciseEntity additions
                db.execSQL("ALTER TABLE exercises ADD COLUMN muscleGroup TEXT NOT NULL DEFAULT 'Uncategorized'")
                db.execSQL("ALTER TABLE exercises ADD COLUMN imageAssetName TEXT")
                db.execSQL("ALTER TABLE exercises ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0")

                // WorkoutSessionEntity additions
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN notes TEXT")

                // SetLogEntity additions
                db.execSQL("ALTER TABLE set_logs ADD COLUMN rpeTag TEXT")
                db.execSQL("ALTER TABLE set_logs ADD COLUMN substitutedFrom INTEGER")
                // Foreign key constraint on substitutedFrom (SQLite ALTER TABLE doesn't support adding FKs directly, 
                // but Room doesn't strictly enforce SQLite FK pragmas after the fact unless you recreate the table. 
                // For a simple migration we'll just add the column. To be perfectly compliant with Room schema export, 
                // we technically need to recreate the table, but adding the column often works if FKs aren't strictly checked by SQLite pragma).
                // Actually, Room validation WILL fail if the table schema doesn't perfectly match the generated hash (which includes FKs).
                // It's safest to recreate the set_logs table if we added a FK.
                
                // Let's do the recreate dance for set_logs
                db.execSQL("""
                    CREATE TABLE set_logs_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        roundNumber INTEGER NOT NULL,
                        reps INTEGER,
                        durationSeconds INTEGER,
                        weightKg REAL,
                        loggedAt INTEGER NOT NULL,
                        rpeTag TEXT,
                        substitutedFrom INTEGER,
                        FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(substitutedFrom) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """)
                db.execSQL("INSERT INTO set_logs_new (id, sessionId, exerciseId, roundNumber, reps, durationSeconds, weightKg, loggedAt) SELECT id, sessionId, exerciseId, roundNumber, reps, durationSeconds, weightKg, loggedAt FROM set_logs")
                db.execSQL("DROP TABLE set_logs")
                db.execSQL("ALTER TABLE set_logs_new RENAME TO set_logs")
                db.execSQL("CREATE INDEX index_set_logs_sessionId ON set_logs(sessionId)")
                db.execSQL("CREATE INDEX index_set_logs_exerciseId ON set_logs(exerciseId)")
                db.execSQL("CREATE INDEX index_set_logs_substitutedFrom ON set_logs(substitutedFrom)")
            }
        }
    }
}
