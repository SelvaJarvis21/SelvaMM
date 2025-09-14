package com.example.selvamoneymanager.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.selvamoneymanager.db.AppDatabase
import com.example.selvamoneymanager.db.CategoryType
import com.example.selvamoneymanager.repo.CategoryRepo
import com.example.selvamoneymanager.viewmodel.CategoryViewModel
import com.example.selvamoneymanager.more.CategoryListScreen


class CategoryListFragment : Fragment() {

    companion object {
        private const val ARG_TYPE = "type"
        fun newInstance(type: String): CategoryListFragment {
            return CategoryListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val typeStr = arguments?.getString(ARG_TYPE) ?: "EXPENSE"
        val type = if (typeStr == "INCOME") CategoryType.INCOME else CategoryType.EXPENSE

        return ComposeView(requireContext()).apply {
            setContent {
                val vm: CategoryViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        val dao = AppDatabase.getDatabase(requireContext()).categoryDao()
                        val repo = CategoryRepo(dao)
                        return CategoryViewModel(repo) as T
                    }
                })

                CategoryListScreen(type = type, vm = vm)
            }
        }
    }
}
