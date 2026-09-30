package com.example.repsgrams.data

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class DatabaseGateDecision {
    START,
    EXPORT_THEN_CONTINUE,
    IMPORT_ONLY,
}

internal const val MESSAGE_EXPORTED = "Backup saved."
internal const val MESSAGE_EXPORT_FAILED = "Export failed."
internal const val MESSAGE_IMPORTED = "Backup imported."
internal const val MESSAGE_IMPORTED_DB_ONLY =
    "Backup imported. The workout log was replaced. Settings on this device were left unchanged."
internal const val MESSAGE_REFUSED_NEWER =
    "This backup is from a newer version of Gains and was not imported."
internal const val MESSAGE_IMPORT_FAILED = "Import failed."
internal const val MESSAGE_IMPORT_EMPTY = "That backup has nothing to import."
internal const val MESSAGE_IMPORT_UNREADABLE = "That backup could not be read and was not imported."

/** Missing, 0, or equal starts init. A positive lower version offers export. Newer is import-only. */
fun decideDatabaseGate(fileExists: Boolean, userVersion: Int, codeVersion: Int): DatabaseGateDecision {
    if (!fileExists || userVersion == 0 || userVersion == codeVersion) {
        return DatabaseGateDecision.START
    }
    if (userVersion > codeVersion) {
        return DatabaseGateDecision.IMPORT_ONLY
    }
    if (userVersion > 0) {
        return DatabaseGateDecision.EXPORT_THEN_CONTINUE
    }
    return DatabaseGateDecision.START
}

internal object BackupArchive {
    const val DATABASE_NAME = "reps-and-grams.db"
    const val DB_ENTRY = DATABASE_NAME
    const val WAL_ENTRY = "$DATABASE_NAME-wal"
    const val SHM_ENTRY = "$DATABASE_NAME-shm"
    const val CYCLE_ENTRY = "datastore/cycle_settings.preferences_pb"
    const val SESSION_ENTRY = "datastore/session_progress.preferences_pb"
    const val MANIFEST_ENTRY = "manifest.json"

    // Exact names only. Never turn a zip entry into a filesystem path.
    val ALLOWED_ENTRIES = setOf(
        DB_ENTRY,
        WAL_ENTRY,
        SHM_ENTRY,
        CYCLE_ENTRY,
        SESSION_ENTRY,
        MANIFEST_ENTRY,
    )

    private val FILE_ENTRIES = ALLOWED_ENTRIES - MANIFEST_ENTRY
    private const val MANIFEST_CAP_BYTES = 256 * 1024
    private const val MAX_ENTRY_BYTES = 512L * 1024L * 1024L
    private const val SQLITE_HEADER_BYTES = 100
    private val SQLITE_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    private val SCHEMA_VERSION_REGEX = Regex(""""schemaVersion"\s*:\s*(-?\d+)""")

    fun writeZip(
        output: OutputStream,
        files: List<Pair<String, File>>,
        schemaVersion: Int,
        appVersionName: String,
    ) {
        val included = files.mapNotNull { (name, file) ->
            if (name !in FILE_ENTRIES || !file.isFile) null else name to file
        }
        val manifest = manifestJson(
            schemaVersion = schemaVersion,
            appVersionName = appVersionName,
            files = included.map { it.first } + MANIFEST_ENTRY,
        )
        ZipOutputStream(LeaveOpenOutput(output)).use { zip ->
            for ((name, file) in included) {
                zip.putNextEntry(ZipEntry(name))
                file.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifest.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    fun importZip(
        input: InputStream,
        codeSchemaVersion: Int,
        destinations: Map<String, File>,
        stageDir: File,
        beforeOverwrite: () -> Unit = {},
    ): ImportOutcome {
        val staged = try {
            stageZip(input, stageDir)
        } catch (error: Exception) {
            deleteStage(stageDir)
            return ImportOutcome.Failed(
                message = MESSAGE_IMPORT_UNREADABLE,
                errorClass = error.javaClass.simpleName,
                errorDetail = error.message,
            )
        }
        when (val assessment = assess(staged, codeSchemaVersion)) {
            is Assessment.RefuseNewer -> {
                deleteStage(stageDir)
                return ImportOutcome.RefusedNewerSchema(assessment.schemaVersion, MESSAGE_REFUSED_NEWER)
            }
            is Assessment.Fail -> {
                deleteStage(stageDir)
                return ImportOutcome.Failed(assessment.message)
            }
            Assessment.Accept -> Unit
        }
        if (staged.files.keys.any { destinations[it] == null }) {
            deleteStage(stageDir)
            return ImportOutcome.Failed(MESSAGE_IMPORT_FAILED, errorDetail = "missing destination")
        }
        return try {
            beforeOverwrite()
            try {
                applyStaged(staged, destinations)
                val settingsIncluded = staged.settingsIncluded
                ImportOutcome.Imported(
                    settingsIncluded = settingsIncluded,
                    message = if (settingsIncluded) MESSAGE_IMPORTED else MESSAGE_IMPORTED_DB_ONLY,
                )
            } catch (error: Exception) {
                ImportOutcome.Failed(
                    message = MESSAGE_IMPORT_FAILED,
                    errorClass = error.javaClass.simpleName,
                    errorDetail = error.message,
                    databaseClosed = true,
                )
            }
        } catch (error: Exception) {
            ImportOutcome.Failed(
                message = MESSAGE_IMPORT_FAILED,
                errorClass = error.javaClass.simpleName,
                errorDetail = error.message,
                databaseClosed = true,
            )
        } finally {
            deleteStage(stageDir)
        }
    }

    private fun stageZip(input: InputStream, stageDir: File): StagedZip {
        stageDir.mkdirs()
        var sawManifest = false
        var malformedManifest = false
        var schemaVersion: Int? = null
        val staged = linkedMapOf<String, File>()
        ZipInputStream(LeaveOpenInput(input)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory && name in ALLOWED_ENTRIES && name !in staged) {
                    if (name == MANIFEST_ENTRY) {
                        sawManifest = true
                        val bytes = readCapped(zip, MANIFEST_CAP_BYTES)
                        if (bytes == null) {
                            malformedManifest = true
                        } else {
                            val parsed = parseSchemaVersion(bytes.toString(Charsets.UTF_8))
                            if (parsed == null) {
                                malformedManifest = true
                            } else {
                                schemaVersion = maxOf(schemaVersion ?: parsed, parsed)
                            }
                        }
                    } else {
                        val stageFile = File(stageDir, stageName(name))
                        // stageName is a fixed token, not the zip entry.
                        check(stageFile.parentFile == stageDir)
                        stageFile.outputStream().use { output -> copyCapped(zip, output, MAX_ENTRY_BYTES) }
                        staged[name] = stageFile
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val manifestState = when {
            malformedManifest -> ManifestState.MALFORMED
            sawManifest -> ManifestState.OK
            else -> ManifestState.ABSENT
        }
        return StagedZip(
            schemaVersion = if (malformedManifest) null else schemaVersion,
            manifestState = manifestState,
            files = staged,
        )
    }

    private fun assess(staged: StagedZip, codeSchemaVersion: Int): Assessment {
        when (staged.manifestState) {
            ManifestState.MALFORMED -> return Assessment.Fail(MESSAGE_IMPORT_UNREADABLE)
            ManifestState.OK -> {
                val schemaVersion = staged.schemaVersion ?: return Assessment.Fail(MESSAGE_IMPORT_UNREADABLE)
                if (schemaVersion > codeSchemaVersion) {
                    return Assessment.RefuseNewer(schemaVersion)
                }
            }
            ManifestState.ABSENT -> Unit
        }
        if (BackupArchive.DB_ENTRY !in staged.files && !staged.settingsIncluded) {
            return Assessment.Fail(MESSAGE_IMPORT_EMPTY)
        }
        // The manifest number can be lower than the database bytes.
        val fileVersion = staged.files[DB_ENTRY]?.let { sqliteUserVersion(it) }
        if (fileVersion != null && (fileVersion < 0 || fileVersion > codeSchemaVersion)) {
            return Assessment.RefuseNewer(fileVersion)
        }
        return Assessment.Accept
    }

    private fun applyStaged(staged: StagedZip, destinations: Map<String, File>) {
        val database = destinations[DB_ENTRY]
        if (DB_ENTRY in staged.files && database != null) {
            // A leftover -wal would replay onto the file we just restored.
            deleteSidecars(database)
        }
        val order = listOf(DB_ENTRY, WAL_ENTRY, SHM_ENTRY, CYCLE_ENTRY, SESSION_ENTRY)
        for (entry in order) {
            val stageFile = staged.files[entry] ?: continue
            val dest = destinations[entry] ?: continue
            moveFile(stageFile, dest)
        }
    }

    private fun deleteSidecars(database: File) {
        val parent = database.parentFile ?: return
        File(parent, database.name + "-wal").delete()
        File(parent, database.name + "-shm").delete()
        File(parent, database.name + "-journal").delete()
    }

    private fun moveFile(from: File, to: File) {
        val parent = to.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            error("could not create destination directory")
        }
        if (!from.renameTo(to)) {
            from.copyTo(to, overwrite = true)
            from.delete()
        }
    }

    private fun stageName(entry: String): String = when (entry) {
        DB_ENTRY -> "db"
        WAL_ENTRY -> "wal"
        SHM_ENTRY -> "shm"
        CYCLE_ENTRY -> "cycle"
        SESSION_ENTRY -> "session"
        else -> error("entry is not staged")
    }

    private fun parseSchemaVersion(manifest: String): Int? {
        val matches = SCHEMA_VERSION_REGEX.findAll(manifest).toList()
        if (matches.isEmpty()) return null
        var highest: Int? = null
        for (match in matches) {
            val value = match.groupValues[1].toIntOrNull() ?: return null
            highest = if (highest == null) value else maxOf(highest, value)
        }
        return highest
    }

    /** Null when the bytes are not a SQLite database. user_version is big-endian at offset 60. */
    private fun sqliteUserVersion(file: File): Int? {
        val header = ByteArray(SQLITE_HEADER_BYTES)
        file.inputStream().use { input ->
            var offset = 0
            while (offset < header.size) {
                val read = input.read(header, offset, header.size - offset)
                if (read < 0) return null
                offset += read
            }
        }
        if (!header.copyOf(SQLITE_MAGIC.size).contentEquals(SQLITE_MAGIC)) return null
        return ((header[60].toInt() and 0xff) shl 24) or
            ((header[61].toInt() and 0xff) shl 16) or
            ((header[62].toInt() and 0xff) shl 8) or
            (header[63].toInt() and 0xff)
    }

    private fun manifestJson(schemaVersion: Int, appVersionName: String, files: List<String>): String {
        val encodedFiles = files.joinToString(",") { jsonString(it) }
        return buildString {
            append("{\"schemaVersion\":")
            append(schemaVersion)
            append(",\"appVersionName\":")
            append(jsonString(appVersionName))
            append(",\"files\":[")
            append(encodedFiles)
            append("]}")
        }
    }

    private fun jsonString(value: String): String = buildString {
        append('"')
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(char)
            }
        }
        append('"')
    }

    private fun readCapped(input: InputStream, cap: Int): ByteArray? {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > cap) return null
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun copyCapped(input: InputStream, output: OutputStream, cap: Long) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > cap) error("backup entry too large")
            output.write(buffer, 0, read)
        }
    }

    private fun deleteStage(stageDir: File) {
        runCatching { stageDir.deleteRecursively() }
    }
}

// Zip streams close their delegate. The caller still owns the content stream.
private class LeaveOpenOutput(private val delegate: OutputStream) : OutputStream() {
    override fun write(byte: Int) = delegate.write(byte)
    override fun write(buffer: ByteArray, offset: Int, length: Int) = delegate.write(buffer, offset, length)
    override fun flush() = delegate.flush()
    override fun close() = flush()
}

private class LeaveOpenInput(private val delegate: InputStream) : InputStream() {
    override fun read(): Int = delegate.read()
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = delegate.read(buffer, offset, length)
    override fun close() = Unit
}

private data class StagedZip(
    val schemaVersion: Int?,
    val manifestState: ManifestState,
    val files: Map<String, File>,
) {
    val settingsIncluded: Boolean
        get() = BackupArchive.CYCLE_ENTRY in files || BackupArchive.SESSION_ENTRY in files
}

private enum class ManifestState { ABSENT, OK, MALFORMED }

private sealed class Assessment {
    data object Accept : Assessment()
    data class RefuseNewer(val schemaVersion: Int) : Assessment()
    data class Fail(val message: String) : Assessment()
}

internal sealed class ImportOutcome {
    abstract val message: String

    data class Imported(
        val settingsIncluded: Boolean,
        override val message: String,
    ) : ImportOutcome()

    data class RefusedNewerSchema(
        val schemaVersion: Int,
        override val message: String,
    ) : ImportOutcome()

    data class Failed(
        override val message: String,
        val errorClass: String? = null,
        val errorDetail: String? = null,
        val databaseClosed: Boolean = false,
    ) : ImportOutcome()
}
