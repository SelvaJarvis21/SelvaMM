package com.example.selvamoneymanager.accounts

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.selvamoneymanager.db.AppDatabase
import com.example.selvamoneymanager.R
import com.example.selvamoneymanager.db.Account
import com.example.selvamoneymanager.db.AccountAdapter
import com.example.selvamoneymanager.db.AccountDao
import kotlinx.coroutines.launch

class AccountListActivity : AppCompatActivity() {

    private lateinit var accountAdapter: AccountAdapter
    private lateinit var db: AppDatabase
    private lateinit var dao: AccountDao

    private val accountList = mutableListOf<Account>() // used internally
    private val displayItems = mutableListOf<AccountRowItem>() // used by adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_list)

        db = AppDatabase.Companion.getDatabase(this)
        dao = db.accountDao()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerAccounts)
        val btnAdd = findViewById<Button>(R.id.btnAddAccount)

        accountAdapter = AccountAdapter(
            items = displayItems,
            onClick = { account -> openEditScreen(account) },
            onLongClick = { account -> deleteAccount(account) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = accountAdapter

        btnAdd.setOnClickListener {
            val intent = Intent(this, AddAccountActivity::class.java)
            startActivityForResult(intent, 100)
        }

        loadAccounts()
    }

    private fun buildGroupedItems(accounts: List<Account>): List<AccountRowItem> {
        val grouped = accounts.groupBy { it.group }
        val result = mutableListOf<AccountRowItem>()

        grouped.forEach { (groupName, groupAccounts) ->
            result.add(AccountRowItem.SectionHeader(groupName))
            groupAccounts.forEach {
                result.add(AccountRowItem.AccountItem(it))
            }
        }

        return result
    }

    private fun loadAccounts() {
        lifecycleScope.launch {
            val accounts = dao.getAllAccounts()
            accountList.clear()
            accountList.addAll(accounts)

            // Update display items for adapter
            displayItems.clear()
            displayItems.addAll(buildGroupedItems(accountList))
            accountAdapter.notifyDataSetChanged()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == 100 || requestCode == 200) && resultCode == RESULT_OK && data != null) {
            loadAccounts()
        }
    }

    private fun openEditScreen(account: Account) {
        val intent = Intent(this, AddAccountActivity::class.java).apply {
            putExtra("id", account.id)
            putExtra("group", account.group)
            putExtra("name", account.name)
            putExtra("amount", account.amount)
        }
        startActivityForResult(intent, 200)
    }

    private fun deleteAccount(account: Account) {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete ${account.name}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    dao.deleteAccount(account)
                    loadAccounts()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}