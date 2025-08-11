package com.example.selvamoneymanager

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.selvamoneymanager.accounts.AccountRowItem
import com.example.selvamoneymanager.accounts.AddAccountActivity
import com.example.selvamoneymanager.db.Account
import com.example.selvamoneymanager.db.AccountAdapter
import com.example.selvamoneymanager.db.AccountDao
import com.example.selvamoneymanager.db.AppDatabase
import com.example.selvamoneymanager.more.MoreActivity
import com.example.selvamoneymanager.stats.StatsActivity
import com.example.selvamoneymanager.trans.TransactionsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import kotlin.math.abs

class HomeActivity : AppCompatActivity() {

    private lateinit var dao: AccountDao
    private lateinit var adapter: AccountAdapter
    private val displayItems = mutableListOf<AccountRowItem>()

    private lateinit var tvTotalAccounts: TextView
    private lateinit var tvTotalLiabilities: TextView
    private lateinit var tvNetWorth: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Toolbar
        findViewById<Toolbar>(R.id.toolbar)?.let {
            setSupportActionBar(it)
            supportActionBar?.title = "Money Manager"
        }

        // FAB: Add account
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddAccount)
            .setOnClickListener {
                startActivity(Intent(this, AddAccountActivity::class.java))
            }

        // Summary views
        tvTotalAccounts = findViewById(R.id.tvTotalAccounts)
        tvTotalLiabilities = findViewById(R.id.tvTotalLiabilities)
        tvNetWorth = findViewById(R.id.tvNetWorth)

        // DB
        dao = AppDatabase.getDatabase(this).accountDao()

        // Recycler setup
        recycler = findViewById(R.id.recyclerAccounts)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = AccountAdapter(
            items = displayItems,
            onClick = { /* open account details/edit if needed */ },
            onLongClick = { /* delete/options if needed */ }
        )
        recycler.adapter = adapter

        // Bottom Navigation
        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_accounts

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_accounts -> {
                    // Already here
                    true
                }
                R.id.nav_trans -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_more -> {
                    startActivity(Intent(this, MoreActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }

        bottomNav.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.nav_accounts) {
                recycler.smoothScrollToPosition(0)
            }
        }

        // Load initial data
        loadAndRender()
    }

    /** Loads accounts, updates summary totals, and rebuilds the grouped list */
    private fun loadAndRender() {
        lifecycleScope.launch {
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

    /** Converts raw accounts into a sectioned list: headers + account rows */
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
        bottomNav.selectedItemId = R.id.nav_accounts
        loadAndRender()
    }
}
