package com.example.selvamoneymanager.more

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.selvamoneymanager.db.AppDatabase
import com.example.selvamoneymanager.db.CategoryType
import com.example.selvamoneymanager.repo.CategoryRepo

class CategoryListActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TYPE = "type" // "INCOME" or "EXPENSE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val typeStr = intent.getStringExtra(EXTRA_TYPE) ?: "EXPENSE"
        val type = if (typeStr == "INCOME") CategoryType.INCOME else CategoryType.EXPENSE

        setContent {
            // Simple VM provider without DI
            val vm: CategoryViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val dao = AppDatabase.getDatabase(applicationContext).categoryDao()
                    val repo = CategoryRepo(dao)
                    return CategoryViewModel(repo) as T
                }
            })

            CategoryListScreen(
                type = type,
                vm = vm
            )
        }
    }
}
