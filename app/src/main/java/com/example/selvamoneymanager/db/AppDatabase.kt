package com.example.selvamoneymanager.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Converters for CategoryType enum
class AppConverters {
    @TypeConverter fun fromType(t: CategoryType): String = t.name
    @TypeConverter fun toType(s: String): CategoryType = CategoryType.valueOf(s)
}

@Database(
    entities = [
        Account::class,
        Transaction::class,
        CategoryEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // Migration 6 -> 7: add categories table and link via categoryId
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1) Create categories
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories(
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        color INTEGER,
                        isDefault INTEGER NOT NULL
                    )
                """.trimIndent())

                // 2) Add categoryId column
                db.execSQL("ALTER TABLE transactions ADD COLUMN categoryId INTEGER")

                // 3) Move distinct old text categories into categories table
                db.execSQL("""
                    INSERT INTO categories (name, type, isDefault)
                    SELECT DISTINCT category, type, 0
                    FROM transactions
                    WHERE category IS NOT NULL
                """.trimIndent())

                // 4) Map new categoryId from inserted categories
                db.execSQL("""
                    UPDATE transactions
                    SET categoryId = (
                        SELECT id FROM categories c
                        WHERE c.name = transactions.category
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "selva_db"
                )
                    .addMigrations(MIGRATION_6_7)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
