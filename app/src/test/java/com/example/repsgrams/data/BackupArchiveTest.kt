package com.example.repsgrams.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupArchiveTest {

    @Test
    fun `missing zero or equal user version starts initialization`() {
        assertEquals(DatabaseGateDecision.START, decideDatabaseGate(fileExists = false, userVersion = 0, codeVersion = 7))
        assertEquals(DatabaseGateDecision.START, decideDatabaseGate(fileExists = false, userVersion = 8, codeVersion = 7))
        assertEquals(DatabaseGateDecision.START, decideDatabaseGate(fileExists = true, userVersion = 0, codeVersion = 7))
        assertEquals(DatabaseGateDecision.START, decideDatabaseGate(fileExists = true, userVersion = 7, codeVersion = 7))
    }

    @Test
    fun `lower user version offers export then continue`() {
        assertEquals(
            DatabaseGateDecision.EXPORT_THEN_CONTINUE,
            decideDatabaseGate(fileExists = true, userVersion = 1, codeVersion = 7),
        )
        assertEquals(
            DatabaseGateDecision.EXPORT_THEN_CONTINUE,
            decideDatabaseGate(fileExists = true, userVersion = 6, codeVersion = 7),
        )
    }

    @Test
    fun `newer user version is import only`() {
        assertEquals(
            DatabaseGateDecision.IMPORT_ONLY,
            decideDatabaseGate(fileExists = true, userVersion = 8, codeVersion = 7),
        )
    }

    @Test
    fun `import refuses a newer schemaVersion without overwriting`() {
        val root = Files.createTempDirectory("gains-backup-refuse").toFile()
        try {
            val database = File(root, "reps-and-grams.db")
            val settings = File(root, "cycle_settings.preferences_pb")
            database.writeText("original-db")
            settings.writeText("original-settings")
            val zip = File(root, "newer.zip")
            ZipOutputStream(FileOutputStream(zip)).use { output ->
                output.putNextEntry(ZipEntry("manifest.json"))
                output.write("""{"schemaVersion": 99, "appVersionName": "9", "files": ["reps-and-grams.db"]}""".toByteArray())
                output.closeEntry()
                output.putNextEntry(ZipEntry("reps-and-grams.db"))
                output.write("replaced-db".toByteArray())
                output.closeEntry()
                output.putNextEntry(ZipEntry("datastore/cycle_settings.preferences_pb"))
                output.write("replaced-settings".toByteArray())
                output.closeEntry()
                output.putNextEntry(ZipEntry("../pwn"))
                output.write("nope".toByteArray())
                output.closeEntry()
            }
            var overwriteStarted = false
            val outcome = zip.inputStream().use { input ->
                BackupArchive.importZip(
                    input = input,
                    codeSchemaVersion = 7,
                    destinations = mapOf(
                        BackupArchive.DB_ENTRY to database,
                        BackupArchive.CYCLE_ENTRY to settings,
                    ),
                    stageDir = File(root, "stage"),
                    beforeOverwrite = { overwriteStarted = true },
                )
            }
            assertTrue(outcome is ImportOutcome.RefusedNewerSchema)
            assertEquals(99, (outcome as ImportOutcome.RefusedNewerSchema).schemaVersion)
            assertEquals(MESSAGE_REFUSED_NEWER, outcome.message)
            assertFalse(overwriteStarted)
            assertEquals("original-db", database.readText())
            assertEquals("original-settings", settings.readText())
            assertFalse(File(root, "pwn").exists())
            assertFalse(File(root.parentFile, "pwn").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `zip without datastore entries overwrites the database only`() {
        val root = Files.createTempDirectory("gains-backup-legacy").toFile()
        try {
            val source = File(root, "source.db")
            source.writeText("new-db")
            val destDir = File(root, "dest").apply { mkdirs() }
            val database = File(destDir, "reps-and-grams.db")
            val settings = File(destDir, "cycle_settings.preferences_pb")
            database.writeText("old-db")
            settings.writeText("keep-settings")
            val zip = File(root, "legacy.zip")
            ZipOutputStream(FileOutputStream(zip)).use { output ->
                output.putNextEntry(ZipEntry(BackupArchive.DB_ENTRY))
                output.write(source.readBytes())
                output.closeEntry()
                output.putNextEntry(ZipEntry("../pwn"))
                output.write("nope".toByteArray())
                output.closeEntry()
            }
            var overwriteStarted = false
            val outcome = zip.inputStream().use { input ->
                BackupArchive.importZip(
                    input = input,
                    codeSchemaVersion = 7,
                    destinations = mapOf(
                        BackupArchive.DB_ENTRY to database,
                        BackupArchive.WAL_ENTRY to File(destDir, "reps-and-grams.db-wal"),
                        BackupArchive.SHM_ENTRY to File(destDir, "reps-and-grams.db-shm"),
                        BackupArchive.CYCLE_ENTRY to settings,
                        BackupArchive.SESSION_ENTRY to File(destDir, "session_progress.preferences_pb"),
                    ),
                    stageDir = File(root, "stage"),
                    beforeOverwrite = { overwriteStarted = true },
                )
            }
            assertTrue(outcome is ImportOutcome.Imported)
            assertFalse((outcome as ImportOutcome.Imported).settingsIncluded)
            assertEquals(MESSAGE_IMPORTED_DB_ONLY, outcome.message)
            assertTrue(overwriteStarted)
            assertEquals("new-db", database.readText())
            assertEquals("keep-settings", settings.readText())
            assertFalse(File(root, "pwn").exists())
            assertFalse(File(root.parentFile, "pwn").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `equal schema overwrites database sidecars and present datastore files`() {
        val root = Files.createTempDirectory("gains-backup-equal").toFile()
        try {
            val sourceDb = File(root, "source.db")
            val sourceWal = File(root, "source.wal")
            val sourceSettings = File(root, "source-settings")
            sourceDb.writeText("new-db")
            sourceWal.writeText("new-wal")
            sourceSettings.writeText("new-settings")
            val zip = File(root, "current.zip")
            FileOutputStream(zip).use { output ->
                BackupArchive.writeZip(
                    output = output,
                    files = listOf(
                        BackupArchive.DB_ENTRY to sourceDb,
                        BackupArchive.WAL_ENTRY to sourceWal,
                        BackupArchive.CYCLE_ENTRY to sourceSettings,
                    ),
                    schemaVersion = 7,
                    appVersionName = "1.0",
                )
            }
            val destDir = File(root, "dest").apply { mkdirs() }
            val database = File(destDir, "reps-and-grams.db")
            val wal = File(destDir, "reps-and-grams.db-wal")
            val shm = File(destDir, "reps-and-grams.db-shm")
            val settings = File(destDir, "cycle_settings.preferences_pb")
            database.writeText("old-db")
            wal.writeText("stale-wal")
            shm.writeText("stale-shm")
            settings.writeText("old-settings")
            val outcome = zip.inputStream().use { input ->
                BackupArchive.importZip(
                    input = input,
                    codeSchemaVersion = 7,
                    destinations = mapOf(
                        BackupArchive.DB_ENTRY to database,
                        BackupArchive.WAL_ENTRY to wal,
                        BackupArchive.SHM_ENTRY to shm,
                        BackupArchive.CYCLE_ENTRY to settings,
                        BackupArchive.SESSION_ENTRY to File(destDir, "session_progress.preferences_pb"),
                    ),
                    stageDir = File(root, "stage"),
                )
            }
            assertTrue(outcome is ImportOutcome.Imported)
            assertTrue((outcome as ImportOutcome.Imported).settingsIncluded)
            assertEquals(MESSAGE_IMPORTED, outcome.message)
            assertEquals("new-db", database.readText())
            assertEquals("new-wal", wal.readText())
            assertFalse(shm.exists())
            assertEquals("new-settings", settings.readText())
            assertFalse(File(destDir, "session_progress.preferences_pb").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `highest schemaVersion is the one that is refused`() {
        val root = Files.createTempDirectory("gains-backup-highest").toFile()
        try {
            val database = File(root, "reps-and-grams.db")
            database.writeText("original-db")
            val zip = File(root, "two-versions.zip")
            ZipOutputStream(FileOutputStream(zip)).use { output ->
                output.putNextEntry(ZipEntry("manifest.json"))
                output.write(
                    """{"schemaVersion": 1, "note": "schemaVersion": 99}""".toByteArray(),
                )
                output.closeEntry()
                output.putNextEntry(ZipEntry("reps-and-grams.db"))
                output.write("replaced-db".toByteArray())
                output.closeEntry()
            }
            var overwriteStarted = false
            val outcome = zip.inputStream().use { input ->
                BackupArchive.importZip(
                    input = input,
                    codeSchemaVersion = 7,
                    destinations = mapOf(BackupArchive.DB_ENTRY to database),
                    stageDir = File(root, "stage"),
                    beforeOverwrite = { overwriteStarted = true },
                )
            }
            assertTrue(outcome is ImportOutcome.RefusedNewerSchema)
            assertEquals(99, (outcome as ImportOutcome.RefusedNewerSchema).schemaVersion)
            assertFalse(overwriteStarted)
            assertEquals("original-db", database.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `sqlite user_version newer than the code is refused without a matching manifest`() {
        val root = Files.createTempDirectory("gains-backup-header").toFile()
        try {
            val database = File(root, "reps-and-grams.db")
            database.writeText("original-db")
            val zip = File(root, "header.zip")
            ZipOutputStream(FileOutputStream(zip)).use { output ->
                output.putNextEntry(ZipEntry("manifest.json"))
                output.write("""{"schemaVersion": 1, "appVersionName": "1.0", "files": ["reps-and-grams.db"]}""".toByteArray())
                output.closeEntry()
                output.putNextEntry(ZipEntry("reps-and-grams.db"))
                output.write(sqliteHeader(userVersion = 99))
                output.closeEntry()
            }
            var overwriteStarted = false
            val outcome = zip.inputStream().use { input ->
                BackupArchive.importZip(
                    input = input,
                    codeSchemaVersion = 7,
                    destinations = mapOf(BackupArchive.DB_ENTRY to database),
                    stageDir = File(root, "stage"),
                    beforeOverwrite = { overwriteStarted = true },
                )
            }
            assertTrue(outcome is ImportOutcome.RefusedNewerSchema)
            assertEquals(99, (outcome as ImportOutcome.RefusedNewerSchema).schemaVersion)
            assertEquals(MESSAGE_REFUSED_NEWER, outcome.message)
            assertFalse(overwriteStarted)
            assertEquals("original-db", database.readText())

            val legacy = File(root, "legacy-newer.zip")
            ZipOutputStream(FileOutputStream(legacy)).use { output ->
                output.putNextEntry(ZipEntry(BackupArchive.DB_ENTRY))
                output.write(sqliteHeader(userVersion = 8))
                output.closeEntry()
            }
            overwriteStarted = false
            val legacyOutcome = legacy.inputStream().use { input ->
                BackupArchive.importZip(
                    input = input,
                    codeSchemaVersion = 7,
                    destinations = mapOf(BackupArchive.DB_ENTRY to database),
                    stageDir = File(root, "stage-legacy"),
                    beforeOverwrite = { overwriteStarted = true },
                )
            }
            assertTrue(legacyOutcome is ImportOutcome.RefusedNewerSchema)
            assertFalse(overwriteStarted)
            assertEquals("original-db", database.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `malformed manifest does not overwrite`() {
        val root = Files.createTempDirectory("gains-backup-bad-manifest").toFile()
        try {
            val database = File(root, "reps-and-grams.db")
            database.writeText("original-db")
            val zip = File(root, "bad.zip")
            ZipOutputStream(FileOutputStream(zip)).use { output ->
                output.putNextEntry(ZipEntry("manifest.json"))
                output.write("""{"schemaVersion": "newer"}""".toByteArray())
                output.closeEntry()
                output.putNextEntry(ZipEntry("reps-and-grams.db"))
                output.write("replaced-db".toByteArray())
                output.closeEntry()
            }
            var overwriteStarted = false
            val outcome = zip.inputStream().use { input ->
                BackupArchive.importZip(
                    input = input,
                    codeSchemaVersion = 7,
                    destinations = mapOf(BackupArchive.DB_ENTRY to database),
                    stageDir = File(root, "stage"),
                    beforeOverwrite = { overwriteStarted = true },
                )
            }
            assertTrue(outcome is ImportOutcome.Failed)
            assertEquals(MESSAGE_IMPORT_UNREADABLE, outcome.message)
            assertFalse(overwriteStarted)
            assertEquals("original-db", database.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    private fun sqliteHeader(userVersion: Int): ByteArray {
        val header = ByteArray(100)
        "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII).copyInto(header)
        header[60] = (userVersion ushr 24).toByte()
        header[61] = (userVersion ushr 16).toByte()
        header[62] = (userVersion ushr 8).toByte()
        header[63] = userVersion.toByte()
        return header
    }
}
