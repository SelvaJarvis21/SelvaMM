package com.example.selvamoneymanager.accounts

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import com.example.selvamoneymanager.accounts.AddAccountFragment

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
        dao = AppDatabase.Companion.getDatabase(requireContext()).accountDao()

        // Summary views
        tvTotalAccounts = view.findViewById(R.id.tvTotalAccounts)
        tvTotalLiabilities = view.findViewById(R.id.tvTotalLiabilities)
        tvNetWorth = view.findViewById(R.id.tvNetWorth)

        // Recycler setup
        recycler = view.findViewById(R.id.recyclerAccounts)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = AccountAdapter(
            items = displayItems,
            onClick = { /* TODO: handle account tap */ },
            onLongClick = { /* TODO: handle account long-press */ }
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

    override fun onResume() {
        super.onResume()
        loadAndRender()
    }
}