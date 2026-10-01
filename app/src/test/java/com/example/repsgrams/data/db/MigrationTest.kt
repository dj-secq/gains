package com.example.repsgrams.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Pure-JVM migration test using xerial sqlite-jdbc.
 *
 * Simulates the v4→v5 schema migration (supplement_logs + supply_inventory →
 * supplements + supplement_intake_logs + supply_inventory) against a realistic
 * pre-existing database, and verifies the backfill row counts.
 */
class MigrationTest {

    @Test
    fun `migrate6To7 backfills missing rest between blocks`() {
        Class.forName("org.sqlite.JDBC")
        val conn = DriverManager.getConnection("jdbc:sqlite::memory:")

        conn.createStatement().use { s ->
            s.execute(
                """CREATE TABLE template_blocks (
                    id INTEGER PRIMARY KEY,
                    templateId INTEGER NOT NULL,
                    label TEXT NOT NULL,
                    orderIndex INTEGER NOT NULL,
                    kind TEXT NOT NULL,
                    restSecondsBetweenRounds INTEGER,
                    restSecondsAfterBlock INTEGER
                )""",
            )
            s.execute("INSERT INTO template_blocks VALUES (1, 1, 'Warm-Up', 0, 'WARM_UP', NULL, NULL)")
            s.execute("INSERT INTO template_blocks VALUES (2, 1, 'Superset A', 1, 'SUPERSET', 90, NULL)")
            s.execute("INSERT INTO template_blocks VALUES (3, 1, 'Superset B', 2, 'SUPERSET', 60, 45)")
            s.execute("INSERT INTO template_blocks VALUES (4, 1, 'Optional Core', 3, 'STANDARD', NULL, NULL)")
            s.execute(
                """UPDATE template_blocks
                    SET restSecondsAfterBlock = CASE
                        WHEN kind = 'WARM_UP' THEN 60
                        ELSE COALESCE(restSecondsBetweenRounds, 90)
                    END
                    WHERE restSecondsAfterBlock IS NULL
                      AND orderIndex < (
                          SELECT MAX(nextBlock.orderIndex)
                          FROM template_blocks AS nextBlock
                          WHERE nextBlock.templateId = template_blocks.templateId
                      )""",
            )
        }

        val rests = conn.createStatement().use { s ->
            val result = s.executeQuery("SELECT restSecondsAfterBlock FROM template_blocks ORDER BY orderIndex")
            buildList<Int?> {
                while (result.next()) add(result.getInt(1).takeUnless { result.wasNull() })
            }
        }
        assertEquals(listOf(60, 90, 45, null), rests)
        conn.close()
    }

    @Test
    fun `migrate4To5 backfills supplement_intake_logs correctly`() {
        // Load the SQLite JDBC driver
        Class.forName("org.sqlite.JDBC")
        val conn = DriverManager.getConnection("jdbc:sqlite::memory:")
        conn.autoCommit = false

        // ----- Build a v4-compatible schema -----
        conn.createStatement().use { s ->
            // Old supplement_logs table (v4 schema)
            s.execute(
                """CREATE TABLE supplement_logs (
                    date INTEGER NOT NULL PRIMARY KEY,
                    wheyTaken INTEGER NOT NULL DEFAULT 0,
                    wheyServings REAL NOT NULL DEFAULT 0,
                    creatineTaken INTEGER NOT NULL DEFAULT 0,
                    creatineGrams REAL NOT NULL DEFAULT 0
                )"""
            )
            // Old supply_inventory table (v4)
            s.execute(
                """CREATE TABLE supply_inventory (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    type TEXT NOT NULL,
                    totalServings INTEGER NOT NULL,
                    servingsRemaining REAL NOT NULL,
                    startDate INTEGER NOT NULL
                )"""
            )

            // Pre-existing realistic data:
            //   Oct 1: both whey and creatine taken
            //   Oct 2: only creatine taken
            //   Oct 3: neither taken (should NOT produce rows)
            s.execute("INSERT INTO supplement_logs VALUES (19631, 1, 25.0, 1, 5.0)")
            s.execute("INSERT INTO supplement_logs VALUES (19632, 0, 0.0, 1, 5.0)")
            s.execute("INSERT INTO supplement_logs VALUES (19633, 0, 0.0, 0, 0.0)")

            // Old supply rows
            s.execute("INSERT INTO supply_inventory (type, totalServings, servingsRemaining, startDate) VALUES ('WHEY', 90, 65.0, 19571)")
            s.execute("INSERT INTO supply_inventory (type, totalServings, servingsRemaining, startDate) VALUES ('CREATINE', 60, 44.0, 19571)")
        }

        // ----- Run the v4→v5 migration SQL -----
        conn.createStatement().use { s ->
            // Create supplements table
            s.execute(
                """CREATE TABLE supplements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    doseAmount REAL NOT NULL,
                    unit TEXT NOT NULL,
                    scheduleType TEXT NOT NULL,
                    customDays TEXT,
                    containerSize INTEGER NOT NULL,
                    lowSupplyThreshold INTEGER NOT NULL,
                    colorToken TEXT NOT NULL,
                    iconName TEXT NOT NULL,
                    isActive INTEGER NOT NULL
                )"""
            )
            // Create supplement_intake_logs table
            s.execute(
                """CREATE TABLE supplement_intake_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    supplementId INTEGER NOT NULL,
                    date INTEGER NOT NULL,
                    taken INTEGER NOT NULL,
                    actualAmount REAL NOT NULL,
                    FOREIGN KEY(supplementId) REFERENCES supplements(id) ON DELETE CASCADE
                )"""
            )
            s.execute("CREATE UNIQUE INDEX idx_intake_date_supp ON supplement_intake_logs (date, supplementId)")

            // Create new supply_inventory_new
            s.execute(
                """CREATE TABLE supply_inventory_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    supplementId INTEGER NOT NULL,
                    totalServings INTEGER NOT NULL,
                    servingsRemaining REAL NOT NULL,
                    startDate INTEGER NOT NULL,
                    FOREIGN KEY(supplementId) REFERENCES supplements(id) ON DELETE CASCADE
                )"""
            )
            s.execute("CREATE UNIQUE INDEX idx_supply_supp ON supply_inventory_new (supplementId)")

            // Seed supplements
            s.execute("INSERT INTO supplements (id, name, doseAmount, unit, scheduleType, customDays, containerSize, lowSupplyThreshold, colorToken, iconName, isActive) VALUES (1, 'Promatrix Whey', 25.0, 'g', 'workoutDayOnly', NULL, 90, 10, 'wheyGreen', 'WaterDrop', 1)")
            s.execute("INSERT INTO supplements (id, name, doseAmount, unit, scheduleType, customDays, containerSize, lowSupplyThreshold, colorToken, iconName, isActive) VALUES (2, 'Creatine Monohydrate', 5.0, 'g', 'daily', NULL, 60, 10, 'creatineTeal', 'Science', 1)")

            // Migrate logs
            s.execute("INSERT INTO supplement_intake_logs (supplementId, date, taken, actualAmount) SELECT 1, date, wheyTaken, wheyServings FROM supplement_logs WHERE wheyTaken = 1 OR wheyServings > 0")
            s.execute("INSERT INTO supplement_intake_logs (supplementId, date, taken, actualAmount) SELECT 2, date, creatineTaken, creatineGrams FROM supplement_logs WHERE creatineTaken = 1 OR creatineGrams > 0")

            // Migrate inventory
            s.execute("INSERT INTO supply_inventory_new (id, supplementId, totalServings, servingsRemaining, startDate) SELECT id, 1, totalServings, servingsRemaining, startDate FROM supply_inventory WHERE type = 'WHEY'")
            s.execute("INSERT INTO supply_inventory_new (id, supplementId, totalServings, servingsRemaining, startDate) SELECT id, 2, totalServings, servingsRemaining, startDate FROM supply_inventory WHERE type = 'CREATINE'")

            // Drop old tables
            s.execute("DROP TABLE supplement_logs")
            s.execute("DROP TABLE supply_inventory")
            s.execute("ALTER TABLE supply_inventory_new RENAME TO supply_inventory")
        }

        // ----- Assertions -----
        // Whey: only Oct 1 was taken → 1 row
        val wheyCount = conn.createStatement().use { s ->
            val rs = s.executeQuery("SELECT COUNT(*) FROM supplement_intake_logs WHERE supplementId = 1")
            rs.getInt(1)
        }
        assertEquals("Whey backfill row count", 1, wheyCount)

        // Creatine: Oct 1 and Oct 2 were taken → 2 rows
        val creatineCount = conn.createStatement().use { s ->
            val rs = s.executeQuery("SELECT COUNT(*) FROM supplement_intake_logs WHERE supplementId = 2")
            rs.getInt(1)
        }
        assertEquals("Creatine backfill row count", 2, creatineCount)

        // Total intake rows = 3
        val totalCount = conn.createStatement().use { s ->
            val rs = s.executeQuery("SELECT COUNT(*) FROM supplement_intake_logs")
            rs.getInt(1)
        }
        assertEquals("Total intake backfill row count", 3, totalCount)

        // supply_inventory migrated: 2 rows (one whey, one creatine)
        val supplyCount = conn.createStatement().use { s ->
            val rs = s.executeQuery("SELECT COUNT(*) FROM supply_inventory")
            rs.getInt(1)
        }
        assertEquals("Supply inventory row count after migration", 2, supplyCount)

        // supplements table seeded: 2 rows
        val suppCount = conn.createStatement().use { s ->
            val rs = s.executeQuery("SELECT COUNT(*) FROM supplements")
            rs.getInt(1)
        }
        assertEquals("Supplements seed count", 2, suppCount)

        val intakeDateType = conn.createStatement().use { s ->
            val rs = s.executeQuery("SELECT type FROM pragma_table_info('supplement_intake_logs') WHERE name = 'date'")
            rs.getString(1)
        }
        val inventoryDateType = conn.createStatement().use { s ->
            val rs = s.executeQuery("SELECT type FROM pragma_table_info('supply_inventory') WHERE name = 'startDate'")
            rs.getString(1)
        }
        assertEquals("INTEGER", intakeDateType)
        assertEquals("INTEGER", inventoryDateType)

        val migratedDate = conn.createStatement().use { s ->
            val rs = s.executeQuery("SELECT date FROM supplement_intake_logs ORDER BY date LIMIT 1")
            rs.getLong(1)
        }
        assertEquals(19631L, migratedDate)

        conn.rollback()
        conn.close()
    }

    @Test
    fun `migrate7To8 marks explicit rests and drops detached personal records`() {
        Class.forName("org.sqlite.JDBC")
        val conn = DriverManager.getConnection("jdbc:sqlite::memory:")
        conn.createStatement().use { statement ->
            statement.execute(
                """CREATE TABLE workout_sessions (
                    id INTEGER PRIMARY KEY,
                    templateId INTEGER,
                    date INTEGER NOT NULL,
                    notes TEXT
                )""",
            )
            statement.execute(
                """CREATE TABLE personal_records (
                    id INTEGER PRIMARY KEY,
                    sourceSetLogId INTEGER
                )""",
            )
            statement.execute("INSERT INTO workout_sessions (id, templateId, date, notes) VALUES (1, NULL, 1, 'Rest day')")
            statement.execute("INSERT INTO workout_sessions (id, templateId, date, notes) VALUES (2, 4, 2, 'Rest day')")
            statement.execute("INSERT INTO workout_sessions (id, templateId, date, notes) VALUES (3, NULL, 3, 'knee was sore')")
            statement.execute("INSERT INTO workout_sessions (id, templateId, date, notes) VALUES (4, 4, 4, NULL)")
            statement.execute("INSERT INTO personal_records (id, sourceSetLogId) VALUES (10, NULL)")
            statement.execute("INSERT INTO personal_records (id, sourceSetLogId) VALUES (11, 7)")
            SessionKindMigration.STATEMENTS.forEach(statement::execute)
            statement.execute("INSERT INTO workout_sessions (id, templateId, date, notes) VALUES (5, 4, 5, NULL)")
        }

        data class Row(val kind: String, val notes: String?)
        val rows = conn.createStatement().use { statement ->
            val result = statement.executeQuery(
                "SELECT id, sessionKind, notes FROM workout_sessions ORDER BY id",
            )
            buildMap {
                while (result.next()) {
                    put(
                        result.getInt(1),
                        Row(result.getString(2), result.getString(3).takeUnless { result.wasNull() }),
                    )
                }
            }
        }
        assertEquals(Row("REST", null), rows.getValue(1))
        assertEquals(Row("WORKOUT", "Rest day"), rows.getValue(2))
        assertEquals(Row("WORKOUT", "knee was sore"), rows.getValue(3))
        assertEquals(Row("WORKOUT", null), rows.getValue(4))
        assertEquals(Row("WORKOUT", null), rows.getValue(5))

        val recordIds = conn.createStatement().use { statement ->
            val result = statement.executeQuery("SELECT id FROM personal_records ORDER BY id")
            buildList {
                while (result.next()) add(result.getInt(1))
            }
        }
        assertEquals(listOf(11), recordIds)
        conn.close()
    }

    @Test
    fun `equipment map is the nineteen seed names and never barbell`() {
        assertEquals(19, ExerciseEquipment.BY_NAME.size)
        assertEquals(
            setOf(Equipment.DUMBBELL, Equipment.BAND, Equipment.BODYWEIGHT),
            ExerciseEquipment.BY_NAME.values.toSet(),
        )
        assertEquals(Equipment.DUMBBELL, ExerciseEquipment.BY_NAME.getValue("Single-Arm DB Row"))
        assertFalse(Schema9Migration.STATEMENTS.any { "FREESTYLE" in it || "BARBELL" in it })
    }

    @Test
    fun `migrate8To9 keeps the session set and template on the default program`() {
        val conn = openProgramFixture(schema8 = true)
        conn.createStatement().use { statement ->
            Schema9Migration.STATEMENTS.forEach(statement::execute)
        }
        assertProgramBackfill(conn)
        conn.close()
    }

    @Test
    fun `migrate7To9 keeps history after the session-kind migration`() {
        val conn = openProgramFixture(schema8 = false)
        conn.createStatement().use { statement ->
            SessionKindMigration.STATEMENTS.forEach(statement::execute)
            Schema9Migration.STATEMENTS.forEach(statement::execute)
        }
        assertProgramBackfill(conn)
        conn.close()
    }

    private fun openProgramFixture(schema8: Boolean): Connection {
        Class.forName("org.sqlite.JDBC")
        val conn = DriverManager.getConnection("jdbc:sqlite::memory:")
        conn.createStatement().use { statement ->
            statement.execute("PRAGMA foreign_keys = OFF")
            statement.execute(
                """CREATE TABLE exercises (
                    id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    notes TEXT,
                    tracksWeight INTEGER NOT NULL,
                    muscleGroup TEXT NOT NULL,
                    imageAssetName TEXT,
                    isCustom INTEGER NOT NULL
                )""",
            )
            statement.execute(
                """CREATE TABLE workout_templates (
                    id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    dayLabel TEXT NOT NULL,
                    maxDurationMinutes INTEGER NOT NULL,
                    restDaysAfter INTEGER NOT NULL,
                    orderIndex INTEGER NOT NULL,
                    category TEXT NOT NULL
                )""",
            )
            statement.execute(
                "CREATE UNIQUE INDEX index_workout_templates_dayLabel ON workout_templates(dayLabel)",
            )
            statement.execute(
                """CREATE TABLE template_blocks (
                    id INTEGER PRIMARY KEY,
                    templateId INTEGER NOT NULL,
                    label TEXT NOT NULL,
                    orderIndex INTEGER NOT NULL
                )""",
            )
            statement.execute(
                """CREATE TABLE template_block_exercises (
                    id INTEGER PRIMARY KEY,
                    blockId INTEGER NOT NULL,
                    exerciseId INTEGER NOT NULL,
                    orderIndex INTEGER NOT NULL,
                    targetValueLow INTEGER NOT NULL,
                    targetValueHigh INTEGER NOT NULL,
                    repType TEXT NOT NULL,
                    perSide INTEGER NOT NULL
                )""",
            )
            val sessionKind = if (schema8) ", sessionKind TEXT NOT NULL DEFAULT 'WORKOUT'" else ""
            statement.execute(
                """CREATE TABLE workout_sessions (
                    id INTEGER PRIMARY KEY,
                    templateId INTEGER,
                    date INTEGER NOT NULL,
                    notes TEXT
                    $sessionKind
                )""",
            )
            statement.execute(
                """CREATE TABLE personal_records (
                    id INTEGER PRIMARY KEY,
                    sourceSetLogId INTEGER
                )""",
            )
            statement.execute(
                """CREATE TABLE set_logs (
                    id INTEGER PRIMARY KEY,
                    sessionId INTEGER NOT NULL,
                    exerciseId INTEGER NOT NULL,
                    roundNumber INTEGER NOT NULL,
                    reps INTEGER,
                    durationSeconds INTEGER,
                    weightKg REAL,
                    loggedAt INTEGER NOT NULL,
                    rpeTag TEXT,
                    substitutedFrom INTEGER
                )""",
            )
            statement.execute("INSERT INTO exercises (id, name, tracksWeight, muscleGroup, isCustom) VALUES (1, 'Jumping Jacks', 0, 'Full Body', 0)")
            statement.execute("INSERT INTO exercises (id, name, tracksWeight, muscleGroup, isCustom) VALUES (2, 'Single-Arm DB Row', 1, 'Back', 0)")
            statement.execute("INSERT INTO exercises (id, name, tracksWeight, muscleGroup, isCustom) VALUES (3, 'My Custom Move', 1, 'Arms', 1)")
            statement.execute(
                "INSERT INTO workout_templates (id, name, dayLabel, maxDurationMinutes, restDaysAfter, orderIndex, category) " +
                    "VALUES (4, 'Workout A — Upper Body + Light Legs', 'A', 40, 1, 0, 'Custom')",
            )
            statement.execute("INSERT INTO template_blocks (id, templateId, label, orderIndex) VALUES (9, 4, 'Superset A', 1)")
            statement.execute(
                "INSERT INTO template_block_exercises (id, blockId, exerciseId, orderIndex, targetValueLow, targetValueHigh, repType, perSide) " +
                    "VALUES (15, 9, 2, 0, 8, 12, 'REPS', 0)",
            )
            if (schema8) {
                statement.execute("INSERT INTO workout_sessions (id, templateId, date, notes, sessionKind) VALUES (1, NULL, 100, NULL, 'REST')")
                statement.execute("INSERT INTO workout_sessions (id, templateId, date, notes, sessionKind) VALUES (2, 4, 150, 'Rest day', 'WORKOUT')")
                statement.execute("INSERT INTO workout_sessions (id, templateId, date, notes, sessionKind) VALUES (3, 4, 200, NULL, 'WORKOUT')")
            } else {
                statement.execute("INSERT INTO workout_sessions (id, templateId, date, notes) VALUES (1, NULL, 100, 'Rest day')")
                statement.execute("INSERT INTO workout_sessions (id, templateId, date, notes) VALUES (2, 4, 150, 'Rest day')")
                statement.execute("INSERT INTO workout_sessions (id, templateId, date, notes) VALUES (3, 4, 200, NULL)")
            }
            statement.execute(
                "INSERT INTO set_logs (id, sessionId, exerciseId, roundNumber, reps, weightKg, loggedAt, rpeTag) " +
                    "VALUES (8, 3, 2, 1, 8, 12.5, 1000, '7.5')",
            )
            statement.execute(
                "INSERT INTO set_logs (id, sessionId, exerciseId, roundNumber, reps, loggedAt, rpeTag) " +
                    "VALUES (9, 3, 1, 1, 40, 1001, 'Right')",
            )
            statement.execute(
                "INSERT INTO set_logs (id, sessionId, exerciseId, roundNumber, reps, loggedAt, rpeTag) " +
                    "VALUES (10, 3, 3, 1, 5, 1002, 'Easy')",
            )
            statement.execute(
                "INSERT INTO set_logs (id, sessionId, exerciseId, roundNumber, reps, weightKg, loggedAt, rpeTag) " +
                    "VALUES (11, 3, 2, 2, 6, 20.0, 1003, '10')",
            )
            statement.execute(
                "INSERT INTO set_logs (id, sessionId, exerciseId, roundNumber, reps, loggedAt, rpeTag) " +
                    "VALUES (12, 3, 3, 2, 4, 1004, '7.5.1')",
            )
        }
        return conn
    }

    private fun assertProgramBackfill(conn: Connection) {
        val program = conn.createStatement().use { statement ->
            val result = statement.executeQuery("SELECT id, name, active, scheduleMode FROM programs")
            assertTrue(result.next())
            val row = listOf(result.getInt(1), result.getString(2), result.getInt(3), result.getString(4))
            assertFalse(result.next())
            row
        }
        assertEquals(listOf(1, "Program", 1, "ROTATION"), program)

        val template = conn.createStatement().use { statement ->
            val result = statement.executeQuery(
                "SELECT id, name, dayLabel, programId, weekday, restDaysAfter, orderIndex FROM workout_templates WHERE id = 4",
            )
            assertTrue(result.next())
            val weekday = result.getInt(5).takeUnless { result.wasNull() }
            listOf(result.getInt(1), result.getString(2), result.getString(3), result.getInt(4), weekday, result.getInt(6), result.getInt(7))
        }
        assertEquals(
            listOf(4, "Workout A — Upper Body + Light Legs", "A", 1, null, 1, 0),
            template,
        )

        val sessions = conn.createStatement().use { statement ->
            val result = statement.executeQuery(
                "SELECT id, templateId, sessionKind, notes FROM workout_sessions ORDER BY id",
            )
            buildMap {
                while (result.next()) {
                    val templateId = result.getInt(2).takeUnless { result.wasNull() }
                    val notes = result.getString(4).takeUnless { result.wasNull() }
                    put(result.getInt(1), Triple(templateId, result.getString(3), notes))
                }
            }
        }
        assertEquals(Triple(null, "REST", null), sessions.getValue(1))
        assertEquals(Triple(4, "WORKOUT", "Rest day"), sessions.getValue(2))
        assertEquals(Triple(4, "WORKOUT", null), sessions.getValue(3))
        assertNull(sessions.values.find { it.second == "FREESTYLE" })

        val sets = conn.createStatement().use { statement ->
            val result = statement.executeQuery(
                "SELECT id, reps, weightKg, rpeTag, setType, rpe FROM set_logs ORDER BY id",
            )
            buildMap {
                while (result.next()) {
                    val weight = result.getDouble(3).takeUnless { result.wasNull() }
                    val rpe = result.getDouble(6).takeUnless { result.wasNull() }
                    put(
                        result.getInt(1),
                        listOf(result.getInt(2), weight, result.getString(4), result.getString(5), rpe),
                    )
                }
            }
        }
        assertEquals(listOf(8, 12.5, "7.5", "WORKING", 7.5), sets.getValue(8))
        assertEquals(listOf(40, null, "Right", "WORKING", null), sets.getValue(9))
        assertEquals(listOf(5, null, "Easy", "WORKING", null), sets.getValue(10))
        assertEquals(listOf(6, 20.0, "10", "WORKING", 10.0), sets.getValue(11))
        assertEquals(listOf(4, null, "7.5.1", "WORKING", null), sets.getValue(12))

        val equipment = conn.createStatement().use { statement ->
            val result = statement.executeQuery("SELECT name, equipment FROM exercises ORDER BY id")
            buildMap {
                while (result.next()) put(result.getString(1), result.getString(2))
            }
        }
        assertEquals("BODYWEIGHT", equipment.getValue("Jumping Jacks"))
        assertEquals("DUMBBELL", equipment.getValue("Single-Arm DB Row"))
        assertEquals("OTHER", equipment.getValue("My Custom Move"))
        assertFalse(equipment.containsValue("BARBELL"))

        val block = conn.createStatement().use { statement ->
            val result = statement.executeQuery(
                "SELECT templateId, restSecondsAfter, progressionIncrementKg " +
                    "FROM template_blocks b JOIN template_block_exercises e ON e.blockId = b.id WHERE b.id = 9",
            )
            assertTrue(result.next())
            val rest = result.getInt(2).takeUnless { result.wasNull() }
            val increment = result.getDouble(3).takeUnless { result.wasNull() }
            listOf(result.getInt(1), rest, increment)
        }
        assertEquals(listOf(4, null, null), block)

        val oldIndex = conn.createStatement().use { statement ->
            val result = statement.executeQuery(
                "SELECT COUNT(*) FROM sqlite_master WHERE name = 'index_workout_templates_dayLabel'",
            )
            result.next()
            result.getInt(1)
        }
        assertEquals(0, oldIndex)

        assertThrows(SQLException::class.java) {
            conn.createStatement().execute(
                "INSERT INTO workout_templates (name, dayLabel, maxDurationMinutes, restDaysAfter, orderIndex, category, programId, weekday) " +
                    "VALUES ('Duplicate', 'A', 30, 1, 3, 'Custom', 1, NULL)",
            )
        }
        conn.createStatement().execute(
            "INSERT INTO programs (id, name, active, scheduleMode) VALUES (2, 'Other', 0, 'WEEKLY')",
        )
        conn.createStatement().execute(
            "INSERT INTO workout_templates (name, dayLabel, maxDurationMinutes, restDaysAfter, orderIndex, category, programId, weekday) " +
                "VALUES ('Other A', 'A', 30, 1, 4, 'Custom', 2, NULL)",
        )
        val sharedLabels = conn.createStatement().use { statement ->
            val result = statement.executeQuery("SELECT COUNT(*) FROM workout_templates WHERE dayLabel = 'A'")
            result.next()
            result.getInt(1)
        }
        assertEquals(2, sharedLabels)
    }
}
