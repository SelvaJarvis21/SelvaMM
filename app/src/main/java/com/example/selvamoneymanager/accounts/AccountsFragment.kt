package com.example.selvamoneymanager.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.selvamoneymanager.R
import com.example.selvamoneymanager.db.Account
import com.example.selvamoneymanager.db.AccountDao
import com.example.selvamoneymanager.db.AppDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import kotlin.math.abs

class AccountsFragment : Fragment() {

    private lateinit var dao: AccountDao
    private lateinit var adapter: AccountAdapter
    private val displayItems = mutableListOf<AccountRowItem>()

    private lateinit var tvTotalAccounts: TextView
    private lateinit var tvTotalLiabilities: TextView
    private lateinit var tvNetWorth: TextView
    private lateinit var recycler: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_accounts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // DAO
        dao = AppDatabase.getDatabase(requireContext()).accountDao()

        // Summary views
        tvTotalAccounts = view.findViewById(R.id.tvTotalAccounts)
        tvTotalLiabilities = view.findViewById(R.id.tvTotalLiabilities)
        tvNetWorth = view.findViewById(R.id.tvNetWorth)

        // Recycler setup
        recycler = view.findViewById(R.id.recyclerAccounts)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = AccountAdapter(
            displayItems,
            onEdit = { account -> showEditDialog(account) },
            onDelete = { account -> showDeleteDialog(account) },
            onClick = { account -> openAccountDetails(account) }
        )
        recycler.adapter = adapter

        // FAB: Add account
        view.findViewById<FloatingActionButton>(R.id.fabAddAccount)
            .setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AddAccountFragment())
                    .addToBackStack(null) // allows back button to return
                    .commit()
            }

        loadAndRender()
    }

    private fun loadAndRender() {
        viewLifecycleOwner.lifecycleScope.launch {
            val accounts = dao.getAllAccounts()

            // Totals
            val totalAssets = accounts.filter { it.amount >= 0 }.sumOf { it.amount }
            val totalLiabilitiesAbs = abs(accounts.filter { it.amount < 0 }.sumOf { it.amount })
            val netWorth = totalAssets - totalLiabilitiesAbs

            tvTotalAccounts.text = "₹%.2f".format(totalAssets)
            tvTotalLiabilities.text = "₹%.2f".format(totalLiabilitiesAbs)
            tvNetWorth.text = "₹%.2f".format(netWorth)

            // Grouped list
            displayItems.clear()
            displayItems.addAll(buildGroupedItems(accounts))
            adapter.notifyDataSetChanged()
        }
    }

    private fun buildGroupedItems(accounts: List<Account>): List<AccountRowItem> {
        val result = mutableListOf<AccountRowItem>()
        accounts.groupBy { it.group }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .forEach { (groupName, groupAccounts) ->
                result.add(AccountRowItem.SectionHeader(groupName))
                groupAccounts.sortedBy { it.name.lowercase() }.forEach { acc ->
                    result.add(AccountRowItem.AccountItem(acc))
                }
            }
        return result
    }

    private fun openAccountDetails(account: Account) {
        Toast.makeText(requireContext(), "Clicked ${account.name}", Toast.LENGTH_SHORT).show()
        // Later: navigate to Account details / transactions
    }

    private fun showEditDialog(account: Account) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_account, null)

        val etName = dialogView.findViewById<EditText>(R.id.etAccountName)
        val etBalance = dialogView.findViewById<EditText>(R.id.etAccountBalance)

        etName.setText(account.name)
        etBalance.setText(account.amount.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Account")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val updated = account.copy(
                    name = etName.text.toString(),
                    amount = etBalance.text.toString().toDoubleOrNull() ?: 0.0
                )
                viewLifecycleOwner.lifecycleScope.launch {
                    dao.update(updated)
                    loadAndRender()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(account: Account) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete ${account.name}?")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    dao.delete(account)
                    loadAndRender()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadAndRender()
    }
}
