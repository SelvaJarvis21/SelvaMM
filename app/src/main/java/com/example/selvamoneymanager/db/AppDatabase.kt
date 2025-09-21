package com.example.selvamoneymanager.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

// Converters for enums
class AppConverters {
    // CategoryType
    @TypeConverter
    fun fromCategoryType(value: CategoryType): String = value.name
    @TypeConverter
    fun toCategoryType(value: String): CategoryType = CategoryType.valueOf(value)

    // TransactionType
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name
    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}

@Database(
    entities = [
        Account::class,
        Transaction::class,
        CategoryEntity::class
    ],
    version = 8, // bump since schema changed
    exportSchema = false
)
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "selva_db"
                )
                    // For dev: wipe and rebuild on version change
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
