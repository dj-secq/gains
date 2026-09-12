package com.example.repsgrams.data

import android.content.Context
import android.net.Uri
import com.example.repsgrams.data.db.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Intent

class BackupManager(private val context: Context, private val database: AppDatabase) {
    suspend fun exportDatabaseToZip(uri: Uri) {
        withContext(Dispatchers.IO) {
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            val dbFile = context.getDatabasePath("reps-and-grams.db")
            val walFile = context.getDatabasePath("reps-and-grams.db-wal")
            val shmFile = context.getDatabasePath("reps-and-grams.db-shm")

            context.contentResolver.openOutputStream(uri)?.use { os ->
                ZipOutputStream(os).use { zos ->
                    listOf(dbFile, walFile, shmFile).forEach { file ->
                        if (file.exists()) {
                            val entry = ZipEntry(file.name)
                            zos.putNextEntry(entry)
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
            }
        }
    }

    suspend fun importDatabaseFromZip(uri: Uri) {
        withContext(Dispatchers.IO) {
            val dbFile = context.getDatabasePath("reps-and-grams.db")
            val walFile = context.getDatabasePath("reps-and-grams.db-wal")
            val shmFile = context.getDatabasePath("reps-and-grams.db-shm")

            database.close()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val outFile = when (entry.name) {
                            "reps-and-grams.db" -> dbFile
                            "reps-and-grams.db-wal" -> walFile
                            "reps-and-grams.db-shm" -> shmFile
                            else -> null
                        }
                        outFile?.let {
                            FileOutputStream(it).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            // Restart app to pick up new DB
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }
    }
}
