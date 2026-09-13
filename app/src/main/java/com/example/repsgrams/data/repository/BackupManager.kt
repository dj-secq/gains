package com.example.repsgrams.data.repository

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(private val context: Context) {
    private val dbName = "repsgrams_db"

    fun exportToZip(outStream: FileOutputStream) {
        val dbFile = context.getDatabasePath(dbName)
        val walFile = File(dbFile.parent, "$dbName-wal")
        val shmFile = File(dbFile.parent, "$dbName-shm")

        ZipOutputStream(outStream).use { zos ->
            val files = listOf(dbFile, walFile, shmFile).filter { it.exists() }
            for (file in files) {
                zos.putNextEntry(ZipEntry(file.name))
                FileInputStream(file).use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    fun importFromZip(inStream: FileInputStream): Boolean {
        return try {
            val dbFile = context.getDatabasePath(dbName)
            val dbDir = dbFile.parentFile ?: return false

            // Overwrite existing files
            ZipInputStream(inStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val target = File(dbDir, entry.name)
                    if (target.name.startsWith(dbName)) {
                        FileOutputStream(target).use { zis.copyTo(it) }
                    }
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
