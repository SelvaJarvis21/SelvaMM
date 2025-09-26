package com.example.selvamoneymanager.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

// Converters for enums
class AppConverters {
    @TypeConverter
    fun fromCategoryType(value: CategoryType): String = value.name
    @TypeConverter
    fun toCategoryType(value: String): CategoryType = CategoryType.valueOf(value)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name
    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}

@Database(
    entities = [Transaction::class, Account::class, CategoryEntity::class],
    version = 4, // bumped
    exportSchema = true
)
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "selva_db"
                )
                    .fallbackToDestructiveMigration() // wipes old DB if mismatch
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)

                            // Insert default categories on first DB creation
                            Executors.newSingleThreadExecutor().execute {
                                val defaults = listOf(
                                    CategoryEntity(name = "Salary", type = CategoryType.INCOME),
                                    CategoryEntity(name = "Freelance/Side Hustle", type = CategoryType.INCOME),
                                    CategoryEntity(name = "Business Income", type = CategoryType.INCOME),
                                    CategoryEntity(name = "Investments", type = CategoryType.INCOME),
                                    CategoryEntity(name = "Gifts", type = CategoryType.INCOME),
                                    CategoryEntity(name = "Rental Income", type = CategoryType.INCOME),
                                    CategoryEntity(name = "Other Income", type = CategoryType.INCOME),
                                    CategoryEntity(name = "Food & Dining", type = CategoryType.EXPENSE),
                                    CategoryEntity(name = "Transport", type = CategoryType.EXPENSE),
                                    CategoryEntity(name = "Shopping", type = CategoryType.EXPENSE),
                                    CategoryEntity(name = "Bills & Utilities", type = CategoryType.EXPENSE),
                                    CategoryEntity(name = "Entertainment", type = CategoryType.EXPENSE),
                                    CategoryEntity(name = "Health & Fitness", type = CategoryType.EXPENSE),
                                    CategoryEntity(name = "Other Expenses", type = CategoryType.EXPENSE)
                                )

                                CoroutineScope(Dispatchers.IO).launch {
                                    getDatabase(context).categoryDao().insertAll(defaults)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
