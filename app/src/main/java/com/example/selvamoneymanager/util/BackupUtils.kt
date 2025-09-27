package com.example.selvamoneymanager.util

import android.content.Context
import java.io.File

fun backupDatabase(context: Context): Boolean {
    return try {
        val dbName = "AppDatabase.db" // ⚠️ replace with your actual DB name
        val dbPath = context.getDatabasePath(dbName)

        val backupDir = File(context.getExternalFilesDir(null), "backup")
        if (!backupDir.exists()) backupDir.mkdirs()

        val backupFile = File(backupDir, dbName)
        dbPath.copyTo(backupFile, overwrite = true)

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun restoreDatabase(context: Context): Boolean {
    return try {
        val dbName = "AppDatabase.db" // ⚠️ replace with your actual DB name
        val dbPath = context.getDatabasePath(dbName)

        val backupFile = File(context.getExternalFilesDir(null), "backup/$dbName")
        if (backupFile.exists()) {
            backupFile.copyTo(dbPath, overwrite = true)
            true
        } else {
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
