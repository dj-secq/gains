import re

with open("app/src/main/java/com/example/repsgrams/data/db/AppDatabase.kt", "r") as f:
    text = f.read()

# Add to @Database entities
entities_old = "    entities = ["
entities_new = "    entities = [\n        PersonalRecordEntity::class,\n        BodyMeasurementLogEntity::class,\n        AchievementEntity::class,"
text = text.replace(entities_old, entities_new, 1)

# Add version = 3
text = text.replace("version = 2", "version = 3")

# Add DAO methods
daos_old = "    abstract fun databaseMetadataDao(): DatabaseMetadataDao"
daos_new = """    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun bodyMeasurementLogDao(): BodyMeasurementLogDao
    abstract fun achievementDao(): AchievementDao
    abstract fun databaseMetadataDao(): DatabaseMetadataDao"""
text = text.replace(daos_old, daos_new)

# Add MIGRATION_2_3
mig_old = "    companion object {"
mig_new = """    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(\"\"\"
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
                \"\"\")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_personal_records_exerciseId` ON `personal_records` (`exerciseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_personal_records_sourceSetLogId` ON `personal_records` (`sourceSetLogId`)")

                db.execSQL(\"\"\"
                    CREATE TABLE IF NOT EXISTS `body_measurements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `valueCm` REAL NOT NULL
                    )
                \"\"\")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_body_measurements_date_type` ON `body_measurements` (`date`, `type`)")

                db.execSQL(\"\"\"
                    CREATE TABLE IF NOT EXISTS `achievements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `key` TEXT NOT NULL,
                        `unlockedDate` INTEGER NOT NULL
                    )
                \"\"\")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_achievements_key` ON `achievements` (`key`)")
            }
        }"""
text = text.replace(mig_old, mig_new)

add_mig_old = ".addMigrations(MIGRATION_1_2)"
add_mig_new = ".addMigrations(MIGRATION_1_2, MIGRATION_2_3)"
text = text.replace(add_mig_old, add_mig_new)

with open("app/src/main/java/com/example/repsgrams/data/db/AppDatabase.kt", "w") as f:
    f.write(text)
