package com.example.repsgrams.data.db

import org.junit.Assert.assertEquals
import org.junit.Test
import java.sql.DriverManager

/**
 * Pure-JVM migration test using xerial sqlite-jdbc.
 *
 * Simulates the v4→v5 schema migration (supplement_logs + supply_inventory →
 * supplements + supplement_intake_logs + supply_inventory) against a realistic
 * pre-existing database, and verifies the backfill row counts.
 */
class MigrationTest {

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
                    date TEXT NOT NULL PRIMARY KEY,
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
                    startDate TEXT NOT NULL
                )"""
            )

            // Pre-existing realistic data:
            //   Oct 1: both whey and creatine taken
            //   Oct 2: only creatine taken
            //   Oct 3: neither taken (should NOT produce rows)
            s.execute("INSERT INTO supplement_logs VALUES ('2023-10-01', 1, 25.0, 1, 5.0)")
            s.execute("INSERT INTO supplement_logs VALUES ('2023-10-02', 0, 0.0, 1, 5.0)")
            s.execute("INSERT INTO supplement_logs VALUES ('2023-10-03', 0, 0.0, 0, 0.0)")

            // Old supply rows
            s.execute("INSERT INTO supply_inventory (type, totalServings, servingsRemaining, startDate) VALUES ('WHEY', 90, 65.0, '2023-09-01')")
            s.execute("INSERT INTO supply_inventory (type, totalServings, servingsRemaining, startDate) VALUES ('CREATINE', 60, 44.0, '2023-09-01')")
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
                    date TEXT NOT NULL,
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
                    startDate TEXT NOT NULL,
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

        conn.rollback()
        conn.close()
    }
}
