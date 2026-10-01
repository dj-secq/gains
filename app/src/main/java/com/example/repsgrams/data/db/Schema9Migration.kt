package com.example.repsgrams.data.db

/**
 * Statements Room runs for schema 8 → 9. [MigrationTest] executes this same list.
 * A fresh install creates the columns from the entities and does not run these
 * statements. This list does not write a freestyle session.
 */
object Schema9Migration {
    private const val NUMERIC_RPE = """
        rpeTag GLOB '[0-9]'
        OR rpeTag GLOB '[0-9][0-9]'
        OR rpeTag GLOB '[0-9][0-9][0-9]'
        OR rpeTag GLOB '[0-9].[0-9]'
        OR rpeTag GLOB '[0-9].[0-9][0-9]'
        OR rpeTag GLOB '[0-9][0-9].[0-9]'
        OR rpeTag GLOB '[0-9][0-9].[0-9][0-9]'
        OR rpeTag GLOB '[0-9][0-9][0-9].[0-9]'
        OR rpeTag GLOB '[0-9][0-9][0-9].[0-9][0-9]'
    """

    val STATEMENTS: List<String> = buildList {
        add(
            "CREATE TABLE IF NOT EXISTS `programs` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`active` INTEGER NOT NULL, " +
                "`scheduleMode` TEXT NOT NULL)",
        )
        add(
            "INSERT INTO `programs` (`id`, `name`, `active`, `scheduleMode`) " +
                "VALUES (1, 'Program', 1, 'ROTATION')",
        )
        add(
            "CREATE TABLE IF NOT EXISTS `workout_templates_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`dayLabel` TEXT NOT NULL, " +
                "`maxDurationMinutes` INTEGER NOT NULL, " +
                "`restDaysAfter` INTEGER NOT NULL, " +
                "`orderIndex` INTEGER NOT NULL, " +
                "`category` TEXT NOT NULL, " +
                "`programId` INTEGER NOT NULL, " +
                "`weekday` INTEGER, " +
                "FOREIGN KEY(`programId`) REFERENCES `programs`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        add(
            "INSERT INTO `workout_templates_new` (" +
                "`id`, `name`, `dayLabel`, `maxDurationMinutes`, `restDaysAfter`, " +
                "`orderIndex`, `category`, `programId`, `weekday`) " +
                "SELECT `id`, `name`, `dayLabel`, `maxDurationMinutes`, `restDaysAfter`, " +
                "`orderIndex`, `category`, 1, NULL FROM `workout_templates`",
        )
        add("DROP TABLE `workout_templates`")
        add("ALTER TABLE `workout_templates_new` RENAME TO `workout_templates`")
        add(
            "CREATE INDEX IF NOT EXISTS `index_workout_templates_programId` " +
                "ON `workout_templates` (`programId`)",
        )
        add(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_workout_templates_programId_dayLabel` " +
                "ON `workout_templates` (`programId`, `dayLabel`)",
        )
        add("ALTER TABLE template_block_exercises ADD COLUMN restSecondsAfter INTEGER")
        add("ALTER TABLE template_block_exercises ADD COLUMN progressionIncrementKg REAL")
        add("ALTER TABLE set_logs ADD COLUMN setType TEXT NOT NULL DEFAULT 'WORKING'")
        add("ALTER TABLE set_logs ADD COLUMN rpe REAL")
        add("ALTER TABLE set_logs ADD COLUMN notes TEXT")
        add(
            "UPDATE set_logs SET rpe = CAST(rpeTag AS REAL) WHERE ${NUMERIC_RPE.trimIndent()}",
        )
        add("ALTER TABLE exercises ADD COLUMN equipment TEXT NOT NULL DEFAULT 'OTHER'")
        addAll(equipmentUpdates())
    }

    private fun equipmentUpdates(): List<String> =
        Equipment.entries.mapNotNull { equipment ->
            val names = ExerciseEquipment.BY_NAME.filterValues { it == equipment }.keys.sorted()
            if (names.isEmpty()) return@mapNotNull null
            val quoted = names.joinToString(", ") { "'${it.replace("'", "''")}'" }
            "UPDATE exercises SET equipment = '${equipment.name}' WHERE name IN ($quoted)"
        }
}

/** The nineteen seeded exercise names. A name missing from this map stays [Equipment.OTHER]. */
object ExerciseEquipment {
    val BY_NAME: Map<String, Equipment> = mapOf(
        "DB Floor Press" to Equipment.DUMBBELL,
        "Single-Arm DB Row" to Equipment.DUMBBELL,
        "DB Overhead Press" to Equipment.DUMBBELL,
        "DB Bicep Curl" to Equipment.DUMBBELL,
        "DB Romanian Deadlift" to Equipment.DUMBBELL,
        "DB Overhead Triceps Extension" to Equipment.DUMBBELL,
        "Band Pull-Aparts" to Equipment.BAND,
        "Band Lateral Raise" to Equipment.BAND,
        "Band Face Pull" to Equipment.BAND,
        "Jumping Jacks" to Equipment.BODYWEIGHT,
        "Arm Circles" to Equipment.BODYWEIGHT,
        "Bodyweight Squats" to Equipment.BODYWEIGHT,
        "Easy Push-Ups" to Equipment.BODYWEIGHT,
        "Pull-Ups" to Equipment.BODYWEIGHT,
        "Bulgarian Split Squat" to Equipment.BODYWEIGHT,
        "Hanging Knee Raise" to Equipment.BODYWEIGHT,
        "Chin-Ups" to Equipment.BODYWEIGHT,
        "Push-Ups" to Equipment.BODYWEIGHT,
        "Hollow Body Hold" to Equipment.BODYWEIGHT,
    )
}
