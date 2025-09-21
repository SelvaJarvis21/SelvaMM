package com.example.selvamoneymanager.repo

import com.example.selvamoneymanager.db.CategoryDao
import com.example.selvamoneymanager.db.CategoryEntity
import com.example.selvamoneymanager.db.CategoryType

class CategoryRepo(private val dao: CategoryDao) {
    suspend fun list(type: CategoryType): List<CategoryEntity> = dao.getCategoriesByType(type)
    suspend fun add(name: String, type: CategoryType) =
        dao.insert(CategoryEntity(name = name.trim(), type = type))

    suspend fun update(entity: CategoryEntity) = dao.update(entity)
    suspend fun delete(entity: CategoryEntity) = dao.delete(entity)

    suspend fun ensureDefaults() {
        if (dao.count() == 0) {
            val defaults = listOf(
                "Salary" to CategoryType.INCOME,
                "Bonus" to CategoryType.INCOME,
                "Food" to CategoryType.EXPENSE,
                "Transport" to CategoryType.EXPENSE
            )
            defaults.forEach { (n, t) ->
                dao.insert(CategoryEntity(name = n, type = t, isDefault = true))
            }
        }
    }
}
