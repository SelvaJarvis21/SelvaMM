package com.example.selvamoneymanager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.selvamoneymanager.accounts.AccountRowItem
import com.example.selvamoneymanager.accounts.AccountsFragment
import com.example.selvamoneymanager.stats.StatsFragment
import com.example.selvamoneymanager.more.MoreFragment
import com.example.selvamoneymanager.db.Account
import com.example.selvamoneymanager.accounts.AccountAdapter
import com.example.selvamoneymanager.db.AccountDao
import com.example.selvamoneymanager.db.AppDatabase
import com.example.selvamoneymanager.trans.TransactionsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import kotlin.math.abs

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Money Manager"

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Load default fragment only once
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AccountsFragment())
                .setReorderingAllowed(true)
                .commit()
            bottomNav.selectedItemId = R.id.nav_accounts
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_accounts -> AccountsFragment()
                R.id.nav_trans    -> TransactionsFragment()
                R.id.nav_stats    -> StatsFragment()
                R.id.nav_more     -> {
                    Log.d("HomeActivity", "Loading MoreFragment")
                    MoreFragment()
                }
                else -> null
            }

            fragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, it)
                    .setReorderingAllowed(true) // safer transaction
                    .commit()
            }
            true
        }
    }
}
