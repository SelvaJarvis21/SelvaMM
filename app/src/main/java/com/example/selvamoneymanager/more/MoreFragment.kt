package com.example.selvamoneymanager.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.example.selvamoneymanager.ui.more.CategoryListFragment

class MoreFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MoreScreen(
                    onOpenIncome = {
                        parentFragmentManager.beginTransaction()
                            .replace(
                                com.example.selvamoneymanager.R.id.fragment_container,
                                CategoryListFragment.newInstance("INCOME")
                            )
                            .addToBackStack(null)
                            .commit()
                    },
                    onOpenExpense = {
                        parentFragmentManager.beginTransaction()
                            .replace(
                                com.example.selvamoneymanager.R.id.fragment_container,
                                CategoryListFragment.newInstance("EXPENSE")
                            )
                            .addToBackStack(null)
                            .commit()
                    }
                )
            }
        }
    }
}
