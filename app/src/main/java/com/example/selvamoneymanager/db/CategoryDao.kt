package com.example.selvamoneymanager.db

import androidx.room.*

@Dao
interface CategoryDao {

    // Get categories filtered by type (Income / Expense)
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    suspend fun getCategoriesByType(type: CategoryType): List<CategoryEntity>

    // Get all categories
    @Query("SELECT * FROM categories ORDER BY type, name ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    // Insert single category
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    // ✅ Insert multiple categories at once
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    // Update existing category
    @Update
    suspend fun update(category: CategoryEntity)

    // Delete category
    @Delete
    suspend fun delete(category: CategoryEntity)

    // Count all categories
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    // ✅ Insert default categories (called from AppDatabase onCreate)
    suspend fun insertDefaults() {
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
        insertAll(defaults)
    }
}
