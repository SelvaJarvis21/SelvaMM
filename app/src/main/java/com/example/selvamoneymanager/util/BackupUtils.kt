package com.example.selvamoneymanager.util

import android.content.Context
import android.os.Environment
import com.example.selvamoneymanager.db.AppDatabase
import com.example.selvamoneymanager.db.Account
import com.example.selvamoneymanager.db.CategoryEntity
import com.example.selvamoneymanager.db.Transaction
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Data wrapper for backup
data class BackupData(
    val accounts: List<Account>,
    val categories: List<CategoryEntity>,
    val transactions: List<Transaction>
)

object BackupUtil {

    // EXPORT → create backup file in Downloads
    suspend fun exportBackup(context: Context): File? = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)

            val accounts = db.accountDao().getAll()
            val categories = db.categoryDao().getAll()
            val transactions = db.transactionDao().getAll()

            val backupData = BackupData(accounts, categories, transactions)
            val json = Gson().toJson(backupData)

            // 📂 Save to Downloads folder
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val fileName = "selva_backup_${System.currentTimeMillis()}.mmbak"
            val backupFile = File(downloadsDir, fileName)
            backupFile.writeText(json)

            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // IMPORT → restore data into DB
    suspend fun importBackup(context: Context, file: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val json = file.readText()
            val backupData = Gson().fromJson(json, BackupData::class.java)

            val db = AppDatabase.getDatabase(context)
            db.clearAllTables()

            db.accountDao().insertAll(backupData.accounts)
            db.categoryDao().insertAll(backupData.categories)
            db.transactionDao().insertAll(backupData.transactions)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
