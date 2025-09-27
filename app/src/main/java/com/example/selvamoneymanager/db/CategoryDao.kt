package com.example.selvamoneymanager.db

import androidx.room.*

@Dao
interface CategoryDao {

    // Get categories filtered by type (Income / Expense)
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    suspend fun getCategoriesByType(type: CategoryType): List<CategoryEntity>

    // Get all categories (sorted)
    @Query("SELECT * FROM categories ORDER BY type, name ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    // ✅ Simple "get all" for backup/restore (no sorting)
    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryEntity>

    // Insert single category
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    // Insert multiple categories at once
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
}
