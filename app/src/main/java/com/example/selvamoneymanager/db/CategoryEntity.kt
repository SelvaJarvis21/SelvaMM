package com.example.selvamoneymanager.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "type"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,                  // Int to match Transaction.categoryId
    val name: String,
    val type: CategoryType,           // INCOME or EXPENSE
    val color: Long? = null,          // Optional: ARGB
    val isDefault: Boolean = false    // Seed vs user-created
)

enum class CategoryType { INCOME, EXPENSE }
