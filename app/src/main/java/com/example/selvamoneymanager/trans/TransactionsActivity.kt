package com.example.selvamoneymanager.trans

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.selvamoneymanager.db.AppDatabase
import com.example.selvamoneymanager.R
import com.example.selvamoneymanager.db.TransactionDao
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class TransactionsActivity : AppCompatActivity() {

    private lateinit var txnDao: TransactionDao
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: TransactionsAdapter

    private lateinit var tvPeriodTitle: TextView
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var modeToggle: RadioGroup
    private lateinit var rbDay: RadioButton
    private lateinit var rbMonth: RadioButton

    private val items = mutableListOf<TxnListItem>()
    private val cal = Calendar.getInstance() // holds current period (month or year)

    private enum class Mode { DAY, MONTH }
    private var mode: Mode = Mode.DAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        txnDao = AppDatabase.getDatabase(this).transactionDao()

        tvPeriodTitle = findViewById(R.id.tvPeriodTitle)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        modeToggle = findViewById(R.id.modeToggle)
        rbDay = findViewById(R.id.rbDay)
        rbMonth = findViewById(R.id.rbMonth)

        recycler = findViewById(R.id.recyclerTxns)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = TransactionsAdapter(items)
        recycler.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAddTxn).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        // Toggle logic
        modeToggle.setOnCheckedChangeListener { _, checkedId ->
            mode = if (checkedId == R.id.rbMonth) Mode.MONTH else Mode.DAY
            updateTitle()
            loadData()
        }

        // Nav arrows
        btnPrev.setOnClickListener {
            if (mode == Mode.DAY) cal.add(Calendar.MONTH, -1) else cal.add(Calendar.YEAR, -1)
            updateTitle()
            loadData()
        }
        btnNext.setOnClickListener {
            if (mode == Mode.DAY) cal.add(Calendar.MONTH, 1) else cal.add(Calendar.YEAR, 1)
            updateTitle()
            loadData()
        }

        updateTitle()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData() // refresh after adding txn
    }

    private fun updateTitle() {
        if (mode == Mode.DAY) {
            val df = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            tvPeriodTitle.text = df.format(cal.time) // e.g., "Aug 2025"
        } else {
            tvPeriodTitle.text = cal.get(Calendar.YEAR).toString()
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            items.clear()

            if (mode == Mode.DAY) {
                val (start, end) = monthBounds(cal)
                val rows = txnDao.getRowsInRange(start, end)

                // Group by day (yyyy-MM-dd)
                val byDay = rows.groupBy { dayKey(it.dateMillis) }.toSortedMap(compareByDescending { it })
                val dayLabel = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())

                byDay.forEach { (_, list) ->
                    val header = dayLabel.format(Date(list.first().dateMillis))
                    items.add(TxnListItem.Section(header))
                    list.forEach { items.add(TxnListItem.Row(it)) }
                }
            } else {
                // Month view: show 12 months for the selected year
                val year = cal.get(Calendar.YEAR)
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

                for (m in 0..11) {
                    val c = Calendar.getInstance().apply {
                        clear()
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, m)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    val (start, end) = monthBounds(c)
                    val rows = txnDao.getRowsInRange(start, end)
                    if (rows.isNotEmpty()) {
                        val total = rows.sumOf { it.amount }
                        val label = "${monthFormat.format(c.time)} $year"
                        items.add(TxnListItem.MonthTotal(label, total))
                    }
                }
            }

            adapter.notifyDataSetChanged()
        }
    }

    private fun dayKey(millis: Long): String {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return df.format(Date(millis))
    }

    private fun monthBounds(c: Calendar): Pair<Long, Long> {
        val start = (c.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = (c.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        return start to end
    }
}
