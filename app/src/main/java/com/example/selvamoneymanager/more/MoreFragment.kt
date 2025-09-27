package com.example.selvamoneymanager.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.example.selvamoneymanager.R
import com.example.selvamoneymanager.ui.more.CategoryListFragment
import com.example.selvamoneymanager.ui.theme.SelvaMoneyManagerTheme
import com.example.selvamoneymanager.util.backupDatabase
import com.example.selvamoneymanager.util.restoreDatabase

class MoreFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SelvaMoneyManagerTheme {
                    MoreScreen(
                        onOpenIncome = {
                            parentFragmentManager.beginTransaction()
                                .replace(
                                    R.id.fragment_container,
                                    CategoryListFragment.newInstance("INCOME")
                                )
                                .addToBackStack(null)
                                .commit()
                        },
                        onOpenExpense = {
                            parentFragmentManager.beginTransaction()
                                .replace(
                                    R.id.fragment_container,
                                    CategoryListFragment.newInstance("EXPENSE")
                                )
                                .addToBackStack(null)
                                .commit()
                        },
                        onBackup = {
                            val result = backupDatabase(requireContext())
                            Toast.makeText(
                                context,
                                if (result) "Backup successful" else "Backup failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onRestore = {
                            val result = restoreDatabase(requireContext())
                            Toast.makeText(
                                context,
                                if (result) "Restore successful" else "Restore failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}
