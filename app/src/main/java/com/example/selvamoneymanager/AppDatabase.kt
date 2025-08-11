package com.example.selvamoneymanager


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// ✅ import entities & DAOs from their new packages
import com.example.selvamoneymanager.accounts.Account
import com.example.selvamoneymanager.accounts.AccountDao
import com.example.selvamoneymanager.trans.Transaction
import com.example.selvamoneymanager.trans.TransactionDao


@Database(
    entities = [Account::class, Transaction::class],
    version = 6, // bump if you changed schema recently
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "selva_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
