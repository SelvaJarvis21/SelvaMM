package com.example.selvamoneymanager.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    version = 4,
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

        /**
         * Migration 3 -> 4
         * If schema didn’t actually change between v3 and v4, this can be a no-op.
         * Add ALTER TABLE / CREATE INDEX statements here if needed.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Example placeholders if you ever add columns/indexes:
                // db.execSQL("""ALTER TABLE `Transaction` ADD COLUMN `note` TEXT""")
                // db.execSQL("""CREATE INDEX IF NOT EXISTS idx_txn_date ON `Transaction`(`date`)""")
                // No-op for now.
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "selva_db"
                )
                    // IMPORTANT: use explicit migrations (no destructive fallback)
                    .addMigrations(MIGRATION_3_4)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed defaults on first DB creation (fresh installs only)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { dbInstance ->
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
                                    dbInstance.categoryDao().insertAll(defaults)
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
