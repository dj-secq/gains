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
        SupplementEntity::class, SupplementIntakeLogEntity::class,
        BodyweightLogEntity::class,
        SupplyInventoryEntity::class,
        DatabaseMetadataEntity::class,
    ],
    version = 7,
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
    abstract fun supplementDao(): SupplementDao
    abstract fun supplementIntakeLogDao(): SupplementIntakeLogDao
    abstract fun bodyweightLogDao(): BodyweightLogDao
    abstract fun supplyInventoryDao(): SupplyInventoryDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun bodyMeasurementLogDao(): BodyMeasurementLogDao
    abstract fun achievementDao(): AchievementDao
    abstract fun databaseMetadataDao(): DatabaseMetadataDao

    companion object {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 6 introduced this column as nullable, leaving existing
                // programs with no rest when moving from one block to the next.
                // Preserve explicit values (including 0) and backfill only
                // non-final blocks that never received a value.
                db.execSQL(
                    """
                    UPDATE template_blocks
                    SET restSecondsAfterBlock = CASE
                        WHEN kind = 'WARM_UP' THEN 60
                        ELSE COALESCE(restSecondsBetweenRounds, 90)
                    END
                    WHERE restSecondsAfterBlock IS NULL
                      AND orderIndex < (
                          SELECT MAX(nextBlock.orderIndex)
                          FROM template_blocks AS nextBlock
                          WHERE nextBlock.templateId = template_blocks.templateId
                      )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE template_blocks ADD COLUMN restSecondsAfterBlock INTEGER")
            }
        }


        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create supplements table
                db.execSQL(
                    "CREATE TABLE `supplements` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`doseAmount` REAL NOT NULL, " +
                    "`unit` TEXT NOT NULL, " +
                    "`scheduleType` TEXT NOT NULL, " +
                    "`customDays` TEXT, " +
                    "`containerSize` INTEGER NOT NULL, " +
                    "`lowSupplyThreshold` INTEGER NOT NULL, " +
                    "`colorToken` TEXT NOT NULL, " +
                    "`iconName` TEXT NOT NULL, " +
                    "`isActive` INTEGER NOT NULL" +
                    ")"
                )

                // Create supplement_intake_logs table
                db.execSQL(
                    "CREATE TABLE `supplement_intake_logs` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`supplementId` INTEGER NOT NULL, " +
                    "`date` INTEGER NOT NULL, " +
                    "`taken` INTEGER NOT NULL, " +
                    "`actualAmount` REAL NOT NULL, " +
                    "FOREIGN KEY(`supplementId`) REFERENCES `supplements`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE" +
                    ")"
                )
                db.execSQL("CREATE INDEX `index_supplement_intake_logs_supplementId` ON `supplement_intake_logs` (`supplementId`)")
                db.execSQL("CREATE UNIQUE INDEX `index_supplement_intake_logs_date_supplementId` ON `supplement_intake_logs` (`date`, `supplementId`)")

                // Create new supply_inventory table
                db.execSQL(
                    "CREATE TABLE `supply_inventory_new` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`supplementId` INTEGER NOT NULL, " +
                    "`totalServings` INTEGER NOT NULL, " +
                    "`servingsRemaining` REAL NOT NULL, " +
                    "`startDate` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`supplementId`) REFERENCES `supplements`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE" +
                    ")"
                )
                db.execSQL("CREATE UNIQUE INDEX `index_supply_inventory_supplementId` ON `supply_inventory_new` (`supplementId`)")

                // Seed supplements
                db.execSQL(
                    "INSERT INTO supplements (id, name, doseAmount, unit, scheduleType, customDays, containerSize, lowSupplyThreshold, colorToken, iconName, isActive) " +
                    "VALUES (1, 'Promatrix Whey', 25.0, 'g', 'workoutDayOnly', NULL, 90, 10, 'wheyGreen', 'WaterDrop', 1)"
                )
                db.execSQL(
                    "INSERT INTO supplements (id, name, doseAmount, unit, scheduleType, customDays, containerSize, lowSupplyThreshold, colorToken, iconName, isActive) " +
                    "VALUES (2, 'Creatine Monohydrate', 5.0, 'g', 'daily', NULL, 60, 10, 'creatineTeal', 'Science', 1)"
                )

                // Migrate logs
                db.execSQL("INSERT INTO supplement_intake_logs (supplementId, date, taken, actualAmount) SELECT 1, date, wheyTaken, wheyServings FROM supplement_logs WHERE wheyTaken = 1 OR wheyServings > 0")
                db.execSQL("INSERT INTO supplement_intake_logs (supplementId, date, taken, actualAmount) SELECT 2, date, creatineTaken, creatineGrams FROM supplement_logs WHERE creatineTaken = 1 OR creatineGrams > 0")

                // Migrate inventory
                db.execSQL("INSERT INTO supply_inventory_new (id, supplementId, totalServings, servingsRemaining, startDate) SELECT id, 1, totalServings, servingsRemaining, startDate FROM supply_inventory WHERE type = 'WHEY'")
                db.execSQL("INSERT INTO supply_inventory_new (id, supplementId, totalServings, servingsRemaining, startDate) SELECT id, 2, totalServings, servingsRemaining, startDate FROM supply_inventory WHERE type = 'CREATINE'")

                // Drop old tables and rename new
                db.execSQL("DROP TABLE supplement_logs")
                db.execSQL("DROP TABLE supply_inventory")
                db.execSQL("ALTER TABLE supply_inventory_new RENAME TO supply_inventory")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_templates ADD COLUMN restDaysAfter INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE workout_templates ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_templates ADD COLUMN category TEXT NOT NULL DEFAULT 'Custom'")

                // Backfill existing templates with order matching their names if possible
                db.execSQL("UPDATE workout_templates SET orderIndex = 0, restDaysAfter = 1 WHERE dayLabel = 'A'")
                db.execSQL("UPDATE workout_templates SET orderIndex = 1, restDaysAfter = 2 WHERE dayLabel = 'B'")
            }
        }
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
