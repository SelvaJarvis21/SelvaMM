package com.example.selvamoneymanager.trans

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.selvamoneymanager.R
import com.example.selvamoneymanager.db.AppDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class TransactionsFragment : Fragment() {

    private lateinit var tvPeriodTitle: TextView
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var modeToggle: RadioGroup
    private lateinit var rbDay: RadioButton
    private lateinit var rbMonth: RadioButton
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: TransactionsAdapter

    // Track current period
    private var currentDate: LocalDate = LocalDate.now()
    private var currentMonth: YearMonth = YearMonth.now()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init views
        tvPeriodTitle = view.findViewById(R.id.tvPeriodTitle)
        btnPrev = view.findViewById(R.id.btnPrev)
        btnNext = view.findViewById(R.id.btnNext)
        modeToggle = view.findViewById(R.id.modeToggle)
        rbDay = view.findViewById(R.id.rbDay)
        rbMonth = view.findViewById(R.id.rbMonth)
        recycler = view.findViewById(R.id.recyclerTxns)

        // Setup RecyclerView + adapter once
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionsAdapter(emptyList()) { txnRow ->
            val bundle = Bundle().apply { putLong("txnId", txnRow.id.toLong()) }
            val fragment = AddTransactionFragment().apply { arguments = bundle }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment) // same ID as FAB
                .addToBackStack(null)
                .commit()
        }
        recycler.adapter = adapter

        // Default = Month mode
        rbMonth.isChecked = true
        updatePeriodTitle()
        loadTransactions()

        // Toggle Day/Month
        modeToggle.setOnCheckedChangeListener { _, _ ->
            updatePeriodTitle()
            loadTransactions()
        }

        // Prev/Next buttons
        btnPrev.setOnClickListener {
            if (rbDay.isChecked) currentDate = currentDate.minusDays(1)
            else currentMonth = currentMonth.minusMonths(1)
            updatePeriodTitle()
            loadTransactions()
        }

        btnNext.setOnClickListener {
            if (rbDay.isChecked) currentDate = currentDate.plusDays(1)
            else currentMonth = currentMonth.plusMonths(1)
            updatePeriodTitle()
            loadTransactions()
        }

        // FAB → AddTransactionFragment (new entry mode)
        view.findViewById<FloatingActionButton>(R.id.fabAddTxn).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddTransactionFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    private fun updatePeriodTitle() {
        val formatter = if (rbDay.isChecked) {
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
        } else {
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
        }

        tvPeriodTitle.text = if (rbDay.isChecked) {
            currentDate.format(formatter)
        } else {
            currentMonth.format(formatter)
        }
    }

    private fun loadTransactions() {
        val dao = AppDatabase.getDatabase(requireContext()).transactionDao()

        viewLifecycleOwner.lifecycleScope.launch {
            val rows: List<TransactionRow> = withContext(Dispatchers.IO) {
                val zone = ZoneId.systemDefault()
                val (start, end) = if (rbDay.isChecked) {
                    val start = currentDate.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = currentDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                    start to end
                } else {
                    val start = currentMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                    start to end
                }
                dao.getRowsInRange(start, end)
            }

            val items = if (rbDay.isChecked) buildDayItems(rows) else buildMonthItems(rows)
            adapter.updateItems(items)
        }
    }

    /** Build list with Section headers for each date */
    private fun buildDayItems(rows: List<TransactionRow>): List<TxnListItem> {
        val items = mutableListOf<TxnListItem>()
        val df = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

        rows.groupBy {
            Instant.ofEpochMilli(it.dateMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
            .toSortedMap(compareByDescending { it })
            .forEach { (date, txns) ->
                items.add(TxnListItem.Section(date.format(df)))
                txns.forEach { items.add(TxnListItem.Row(it)) }
            }

        return items
    }

    /** Build list with MonthTotal at the end */
    private fun buildMonthItems(rows: List<TransactionRow>): List<TxnListItem> {
        val items = mutableListOf<TxnListItem>()
        val df = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

        var total = 0.0

        rows.forEach { total += it.amount }

        items.add(
            TxnListItem.MonthTotal(
                currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                total
            )
        )


        rows.groupBy {
            Instant.ofEpochMilli(it.dateMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
            .toSortedMap(compareByDescending { it })
            .forEach { (date, txns) ->
                items.add(TxnListItem.Section(date.format(df)))
                txns.forEach {
                    items.add(TxnListItem.Row(it))
                    total += it.amount
                }
            }


        return items
    }

}
