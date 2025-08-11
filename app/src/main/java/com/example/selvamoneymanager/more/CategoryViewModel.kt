package com.example.selvamoneymanager.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.selvamoneymanager.db.CategoryEntity
import com.example.selvamoneymanager.db.CategoryType
import com.example.selvamoneymanager.repo.CategoryRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val repo: CategoryRepo
) : ViewModel() {

    private val _items = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val items: StateFlow<List<CategoryEntity>> = _items

    private var currentType: CategoryType = CategoryType.EXPENSE

    /** Caller (Composable/Activity) tells the VM which list to show */
    fun load(type: CategoryType) {
        currentType = type
        viewModelScope.launch {
            _items.value = repo.list(type)
        }
    }

    fun addCategory(name: String, type: CategoryType, onError: (String)->Unit, onDone:()->Unit) {
        viewModelScope.launch {
            try {
                val trimmed = name.trim()
                if (trimmed.isEmpty()) { onError("Name cannot be empty"); return@launch }
                repo.add(trimmed, type)
                _items.value = repo.list(type)
                onDone()
            } catch (_: Exception) {
                onError("Category already exists?")
            }
        }
    }

    fun editCategory(target: CategoryEntity, newName: String, onError: (String)->Unit, onDone:()->Unit) {
        viewModelScope.launch {
            try {
                if (target.isDefault) { onError("Default categories can’t be edited"); return@launch }
                val trimmed = newName.trim()
                if (trimmed.isEmpty()) { onError("Name cannot be empty"); return@launch }
                repo.update(target.copy(name = trimmed))
                _items.value = repo.list(currentType)
                onDone()
            } catch (_: Exception) {
                onError("Name already exists?")
            }
        }
    }

    fun deleteCategory(target: CategoryEntity, onError: (String)->Unit, onDone:()->Unit) {
        viewModelScope.launch {
            try {
                if (target.isDefault) { onError("Default categories can’t be deleted"); return@launch }
                repo.delete(target)
                _items.value = repo.list(currentType)
                onDone()
            } catch (_: Exception) {
                onError("Couldn’t delete category")
            }
        }
    }
}
