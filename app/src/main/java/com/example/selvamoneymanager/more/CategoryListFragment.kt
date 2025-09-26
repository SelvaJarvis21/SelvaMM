package com.example.selvamoneymanager.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.selvamoneymanager.R
import com.example.selvamoneymanager.db.AppDatabase
import com.example.selvamoneymanager.db.CategoryDao
import com.example.selvamoneymanager.db.CategoryEntity
import com.example.selvamoneymanager.db.CategoryType
import com.example.selvamoneymanager.db.TransactionDao
import com.example.selvamoneymanager.more.CategoryAdapter
import kotlinx.coroutines.launch

class CategoryListFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: CategoryAdapter
    private lateinit var categoryDao: CategoryDao
    private lateinit var txnDao: TransactionDao
    private var categoryType: CategoryType = CategoryType.EXPENSE

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val typeStr = arguments?.getString(ARG_TYPE) ?: "EXPENSE"
        categoryType = if (typeStr == "INCOME") CategoryType.INCOME else CategoryType.EXPENSE

        val db = AppDatabase.getDatabase(requireContext())
        categoryDao = db.categoryDao()
        txnDao = db.transactionDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_category_list, container, false)

        recycler = view.findViewById(R.id.recyclerCategories)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = CategoryAdapter(
            emptyList(),
            onEdit = { editCategory(it) },
            onDelete = { deleteCategory(it) }
        )
        recycler.adapter = adapter

        loadCategories()

        return view
    }

    private fun loadCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            val cats = categoryDao.getCategoriesByType(categoryType)
            adapter.updateData(cats)
        }
    }

    private fun editCategory(cat: CategoryEntity) {
        val input = EditText(requireContext())
        input.setText(cat.name)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Category")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        categoryDao.update(cat.copy(name = newName))
                        loadCategories()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCategory(cat: CategoryEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            val count = txnDao.countByCategory(cat.id)
            if (count > 0) {
                Toast.makeText(requireContext(), "Category is in use, cannot delete", Toast.LENGTH_SHORT).show()
            } else {
                categoryDao.delete(cat)
                loadCategories()
            }
        }
    }
}
