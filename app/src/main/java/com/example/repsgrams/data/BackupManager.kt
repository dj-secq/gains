package com.example.repsgrams.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.repsgrams.data.db.AppDatabase
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class BackupManager(
    private val context: Context,
    private val codeSchemaVersion: Int,
) {
    private val mutex = Mutex()

    /** Held for the file copy and for the first Room open so those cannot overlap. */
    internal val fileLock = Any()

    @Volatile
    private var roomDatabase: AppDatabase? = null

    @Volatile
    private var preOpenUserVersion: Int? = null

    internal fun attachDatabase(database: AppDatabase) {
        roomDatabase = database
    }

    internal fun notePreOpenUserVersion(version: Int) {
        preOpenUserVersion = version
    }

    suspend fun exportDatabaseToZip(uri: Uri): String = withContext(NonCancellable) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    synchronized(fileLock) { exportLocked(uri) }
                } catch (error: Exception) {
                    Log.e(TAG, "export failure: ${error.javaClass.simpleName}: ${error.message}")
                    MESSAGE_EXPORT_FAILED
                }
            }
        }
    }

    suspend fun importDatabaseFromZip(uri: Uri): String = withContext(NonCancellable) {
        mutex.withLock {
            val outcome = withContext(Dispatchers.IO) {
                try {
                    synchronized(fileLock) { importLocked(uri) }
                } catch (error: Exception) {
                    ImportOutcome.Failed(
                        message = MESSAGE_IMPORT_FAILED,
                        errorClass = error.javaClass.simpleName,
                        errorDetail = error.message,
                    )
                }
            }
            when (outcome) {
                is ImportOutcome.Imported -> {
                    Log.i(TAG, "import success settingsIncluded=${outcome.settingsIncluded}")
                    writeNotice(outcome.message)
                    restartApp()
                    outcome.message
                }
                is ImportOutcome.RefusedNewerSchema -> {
                    Log.w(
                        TAG,
                        "import refusal because the schema is newer: schemaVersion=${outcome.schemaVersion} code=$codeSchemaVersion",
                    )
                    outcome.message
                }
                is ImportOutcome.Failed -> {
                    Log.e(TAG, "import failure: ${outcome.errorClass}: ${outcome.errorDetail}")
                    if (outcome.databaseClosed) {
                        writeNotice(outcome.message)
                        restartApp()
                    }
                    outcome.message
                }
            }
        }
    }

    fun consumeImportNotice(): String? {
        val file = noticeFile()
        if (!file.isFile) return null
        val text = runCatching { file.readText() }.getOrNull()
        file.delete()
        return text?.trim()?.take(MAX_NOTICE_CHARS)?.takeIf { it.isNotEmpty() }
    }

    private fun exportLocked(uri: Uri): String {
        Log.i(TAG, "export start")
        val schemaVersion = currentSchemaVersion()
        val database = context.getDatabasePath(BackupArchive.DATABASE_NAME)
        if (!database.isFile) {
            Log.e(TAG, "export failure: database file missing")
            return MESSAGE_EXPORT_FAILED
        }
        val output = context.contentResolver.openOutputStream(uri)
        if (output == null) {
            Log.e(TAG, "export failure: could not open destination")
            return MESSAGE_EXPORT_FAILED
        }
        output.use { stream ->
            BackupArchive.writeZip(
                output = stream,
                files = exportFiles(),
                schemaVersion = schemaVersion,
                appVersionName = appVersionName(),
            )
        }
        Log.i(TAG, "export success")
        return MESSAGE_EXPORTED
    }

    private fun importLocked(uri: Uri): ImportOutcome {
        val input = context.contentResolver.openInputStream(uri)
            ?: return ImportOutcome.Failed(message = MESSAGE_IMPORT_FAILED, errorDetail = "no stream")
        return input.use { stream ->
            BackupArchive.importZip(
                input = stream,
                codeSchemaVersion = codeSchemaVersion,
                destinations = importDestinations(),
                stageDir = File(context.cacheDir, "backup-import-${System.nanoTime()}"),
                beforeOverwrite = { closeRoomDatabase() },
            )
        }
    }

    private fun currentSchemaVersion(): Int {
        val open = roomDatabase
        if (open != null) {
            return readSchemaVersion(open)
        }
        // Gate export copies the files. It does not open Room.
        return preOpenUserVersion ?: error("database version unknown")
    }

    private fun readSchemaVersion(database: AppDatabase): Int {
        val sqlite = database.openHelper.writableDatabase
        sqlite.query("PRAGMA wal_checkpoint(FULL)").close()
        val cursor = sqlite.query("PRAGMA user_version")
        try {
            check(cursor.moveToFirst()) { "user_version missing" }
            return cursor.getInt(0)
        } finally {
            cursor.close()
        }
    }

    private fun exportFiles(): List<Pair<String, File>> = listOf(
        BackupArchive.DB_ENTRY to context.getDatabasePath(BackupArchive.DATABASE_NAME),
        BackupArchive.WAL_ENTRY to context.getDatabasePath(BackupArchive.WAL_ENTRY),
        BackupArchive.SHM_ENTRY to context.getDatabasePath(BackupArchive.SHM_ENTRY),
        BackupArchive.CYCLE_ENTRY to File(context.filesDir, BackupArchive.CYCLE_ENTRY),
        BackupArchive.SESSION_ENTRY to File(context.filesDir, BackupArchive.SESSION_ENTRY),
    )

    private fun importDestinations(): Map<String, File> = mapOf(
        BackupArchive.DB_ENTRY to context.getDatabasePath(BackupArchive.DATABASE_NAME),
        BackupArchive.WAL_ENTRY to context.getDatabasePath(BackupArchive.WAL_ENTRY),
        BackupArchive.SHM_ENTRY to context.getDatabasePath(BackupArchive.SHM_ENTRY),
        BackupArchive.CYCLE_ENTRY to File(context.filesDir, BackupArchive.CYCLE_ENTRY),
        BackupArchive.SESSION_ENTRY to File(context.filesDir, BackupArchive.SESSION_ENTRY),
    )

    private fun closeRoomDatabase() {
        val open = roomDatabase ?: return
        roomDatabase = null
        open.close()
    }

    private fun appVersionName(): String {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return info.versionName ?: "unknown"
    }

    private fun noticeFile() = File(context.cacheDir, NOTICE_FILE)

    private fun writeNotice(message: String) {
        runCatching { noticeFile().writeText(message) }
            .onFailure { error ->
                Log.e(TAG, "import notice failed: ${error.javaClass.simpleName}: ${error.message}")
            }
    }

    private fun restartApp() {
        Log.i(TAG, "process about to exit")
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
        )
        if (intent != null) {
            context.startActivity(intent)
        }
        Runtime.getRuntime().exit(0)
    }

    private companion object {
        const val TAG = "Backup"
        const val NOTICE_FILE = "backup_import_notice.txt"
        const val MAX_NOTICE_CHARS = 500
    }
}
