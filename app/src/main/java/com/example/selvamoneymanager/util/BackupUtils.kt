package com.example.selvamoneymanager.util

import android.content.Context
import android.util.Log
import java.io.File

fun backupDatabase(context: Context): Boolean {
    return try {
        val dbName = "selva_db"
        val dbPath = context.getDatabasePath(dbName) // ✅ actual database in /databases

        if (!dbPath.exists()) {
            Log.e("BackupUtils", "Database not found at ${dbPath.absolutePath}")
            return false
        }

        val backupDir = File(context.getExternalFilesDir(null), "backup")
        if (!backupDir.exists()) backupDir.mkdirs()

        val backupFile = File(backupDir, dbName)
        dbPath.copyTo(backupFile, overwrite = true)

        Log.i("BackupUtils", "Backup saved at ${backupFile.absolutePath}, size=${backupFile.length()}")
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun restoreDatabase(context: Context): Boolean {
    return try {
        val dbName = "selva_db"
        val dbPath = context.getDatabasePath(dbName) // ✅ actual database path

        val backupFile = File(context.getExternalFilesDir(null), "backup/$dbName")
        if (!backupFile.exists()) {
            Log.e("BackupUtils", "Backup not found at ${backupFile.absolutePath}")
            return false
        }

        backupFile.copyTo(dbPath, overwrite = true)

        Log.i("BackupUtils", "Database restored from ${backupFile.absolutePath}, size=${backupFile.length()}")
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
