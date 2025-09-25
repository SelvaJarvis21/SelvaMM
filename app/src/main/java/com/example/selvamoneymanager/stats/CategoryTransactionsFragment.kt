package com.example.selvamoneymanager.stats

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.selvamoneymanager.R
import com.example.selvamoneymanager.db.AppDatabase
import com.example.selvamoneymanager.db.TransactionDao
import com.example.selvamoneymanager.db.Transaction
import com.example.selvamoneymanager.trans.TransactionRow
import com.example.selvamoneymanager.trans.TransactionsAdapter
import com.example.selvamoneymanager.trans.TxnListItem
import kotlinx.coroutines.launch
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId

class CategoryTransactionsFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: TransactionsAdapter
    private lateinit var txnDao: TransactionDao
    private var categoryId: Int = 0
    private var categoryName: String? = null
    private var timePeriod: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryId = it.getInt(ARG_CATEGORY_ID)
            categoryName = it.getString(ARG_CATEGORY_NAME)
            timePeriod = it.getString(ARG_TIME_PERIOD)
        }
        txnDao = AppDatabase.getDatabase(requireContext()).transactionDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_category_transactions, container, false)
        recycler = view.findViewById(R.id.recyclerTransactions)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = TransactionsAdapter(emptyList()) { txn ->
            // Handle click here (for now, maybe just log or Toast)
            Toast.makeText(requireContext(), "Clicked: ${txn.description}", Toast.LENGTH_SHORT).show()
        }
        recycler.adapter = adapter

        loadTransactions()
        return view
    }

    private fun loadTransactions() {
        viewLifecycleOwner.lifecycleScope.launch {
            val (start, end) = getDateRange(timePeriod ?: "monthly")
            val txns: List<Transaction> = txnDao.getTransactionsByCategory(categoryId, start, end)

            val items: List<TxnListItem> = txns.map { txn: Transaction ->
                TxnListItem.Row(
                    TransactionRow(
                        id = txn.id,
                        type = txn.type.name,   // ✅ Convert enum to String
                        dateMillis = txn.dateMillis,
                        amount = txn.amount,
                        category = categoryName, // ✅ show category name passed from StatsFragment
                        description = txn.description,
                        accountId = txn.accountId,
                        accountName = null,       // can enrich via AccountDao if needed
                        fromAccountId = txn.fromAccountId,
                        fromAccountName = null,
                        toAccountId = txn.toAccountId,
                        toAccountName = null
                    )
                )
            }

            adapter.updateItems(items)
        }
    }

    private fun getDateRange(period: String): Pair<Long, Long> {
        return if (period == "yearly") {
            val year = Year.now()
            val start = year.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val end = year.atMonth(12).atEndOfMonth()
                .atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            Pair(start, end)
        } else {
            val ym = YearMonth.now()
            val start = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val end = ym.atEndOfMonth().atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            Pair(start, end)
        }
    }

    companion object {
        private const val ARG_CATEGORY_ID = "categoryId"
        private const val ARG_CATEGORY_NAME = "categoryName"
        private const val ARG_TIME_PERIOD = "timePeriod"

        fun newInstance(id: Int, name: String, period: String) =
            CategoryTransactionsFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_CATEGORY_ID, id)
                    putString(ARG_CATEGORY_NAME, name)
                    putString(ARG_TIME_PERIOD, period)
                }
            }
    }
}
