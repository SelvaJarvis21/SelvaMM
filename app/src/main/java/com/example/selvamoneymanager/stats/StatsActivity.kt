package com.example.selvamoneymanager.stats

import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import android.graphics.Typeface
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.selvamoneymanager.db.AppDatabase
import com.example.selvamoneymanager.R
import com.example.selvamoneymanager.trans.CategoryTotal
import com.example.selvamoneymanager.db.TransactionDao
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatsActivity : AppCompatActivity() {

    private lateinit var txnDao: TransactionDao

    // UI
    private lateinit var pieChart: PieChart
    private lateinit var tvPeriod: TextView
    private lateinit var chipIncome: Chip
    private lateinit var chipExpense: Chip
    private lateinit var tvTotalSummary: TextView
    private lateinit var rvCategoryTotals: RecyclerView
    private lateinit var catAdapter: CategoryTotalsAdapter
    private lateinit var ddTimeline: AutoCompleteTextView
    private lateinit var tvSelectedCategory: TextView
    private var lastRows: List<CategoryTotal> = emptyList()
    private var lastSelectedAmt: Double = 0.0




    // State
    private val cal = Calendar.getInstance()
    private var isYearly: Boolean = false // false = Monthly (default), true = Yearly

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        // DB
        txnDao = AppDatabase.getDatabase(this).transactionDao()


        // Views
        ddTimeline = findViewById(R.id.ddTimeline)
        pieChart = findViewById(R.id.pieChart)
        tvPeriod = findViewById(R.id.tvPeriod)
        chipIncome = findViewById(R.id.chipIncome)
        chipExpense = findViewById(R.id.chipExpense)
        tvTotalSummary = findViewById(R.id.tvTotalSummary)
        rvCategoryTotals = findViewById(R.id.rvCategoryTotals)
        rvCategoryTotals.layoutManager = LinearLayoutManager(this)
        catAdapter = CategoryTotalsAdapter()
        rvCategoryTotals.adapter = catAdapter
        tvSelectedCategory = findViewById(R.id.tvSelectedCategory)

        // pie highlighter - on click

        catAdapter = CategoryTotalsAdapter(onItemClick = { name ->
            // find the slice index and highlight it
            val data = pieChart.data ?: return@CategoryTotalsAdapter
            val entries = (data.dataSet as com.github.mikephil.charting.data.PieDataSet).values
            val idx = entries.indexOfFirst { (it as com.github.mikephil.charting.data.PieEntry).label == name }
            if (idx >= 0) {
                pieChart.highlightValue(idx.toFloat(), 0)
                // also update the selected text using same logic:
                val sum = lastRows.sumOf { kotlin.math.abs(it.total) }
                val amt = kotlin.math.abs(lastRows.find { (it.category ?: "Uncategorized") == name }?.total ?: 0.0)
                val pct = if (sum > 0) (amt / sum) * 100.0 else 0.0
                tvSelectedCategory.text = "%.1f%%  •  %s  •  ₹%.2f".format(java.util.Locale.getDefault(), pct, name, amt)
                catAdapter.setSelectedCategory(name)
            }
        })
        rvCategoryTotals.adapter = catAdapter
        // 3) SETUP CHART (legend off, selection listener, etc.)

        setupChart()

        // Timeline dropdown
        val modes = listOf("Monthly", "Yearly")
        ddTimeline.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, modes))
        ddTimeline.setText("Monthly", false)
        ddTimeline.setOnItemClickListener { _, _, position, _ ->
            isYearly = (position == 1) // 0=Monthly, 1=Yearly
            updateHeader()
            loadAndRender()
        }

        // Prev/Next
        findViewById<ImageButton>(R.id.btnPrev).setOnClickListener {
            if (isYearly) cal.add(Calendar.YEAR, -1) else cal.add(Calendar.MONTH, -1)
            updateHeader()
            loadAndRender()
        }
        findViewById<ImageButton>(R.id.btnNext).setOnClickListener {
            if (isYearly) cal.add(Calendar.YEAR, 1) else cal.add(Calendar.MONTH, 1)
            updateHeader()
            loadAndRender()
        }

        // Income / Expense toggle (mutually exclusive)
        chipIncome.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                chipExpense.isChecked = false
                loadAndRender()
            } else if (!chipExpense.isChecked) {
                chipExpense.isChecked = true
            }
        }
        chipExpense.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                chipIncome.isChecked = false
                loadAndRender()
            } else if (!chipIncome.isChecked) {
                chipIncome.isChecked = true
            }
        }

        // Initial state
        chipExpense.isChecked = true

        updateHeader()
        loadAndRender()
    }

    private fun updateHeader() {
        val fmt = if (isYearly)
            SimpleDateFormat("yyyy", Locale.getDefault())
        else
            SimpleDateFormat("MMM yyyy", Locale.getDefault())
        tvPeriod.text = fmt.format(cal.time)
    }

    private fun periodBounds(c: Calendar): Pair<Long, Long> {
        return if (isYearly) {
            val start = (c.clone() as Calendar).apply {
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = (c.clone() as Calendar).apply {
                set(Calendar.MONTH, Calendar.DECEMBER)
                set(Calendar.DAY_OF_MONTH, 31)
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            start to end
        } else {
            val start = (c.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = (c.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            start to end
        }
    }

    private fun setupChart() {
        pieChart.description.isEnabled = false
        pieChart.isRotationEnabled = false
        pieChart.setUsePercentValues(true)
        pieChart.setCenterTextTypeface(Typeface.DEFAULT_BOLD)
        pieChart.setNoDataText("No records")
        pieChart.centerText = ""
        pieChart.setEntryLabelTextSize(12f)

        val legend = pieChart.legend
        legend.isEnabled = false

        // 🔗 sync pie selection with list
        pieChart.setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
            override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                val pe = e as? com.github.mikephil.charting.data.PieEntry ?: return
                val cat = pe.label ?: "Uncategorized"
                val sum = lastRows.sumOf { kotlin.math.abs(it.total) }
                val amt = kotlin.math.abs(lastRows.find { (it.category ?: "Uncategorized") == cat }?.total ?: 0.0)
                val pct = if (sum > 0) (amt / sum) * 100.0 else 0.0
                showSelectedCategoryAnimated(cat, pct, amt)
                tvSelectedCategory.text = "%.1f%%  •  %s  •  ₹%.2f".format(java.util.Locale.getDefault(), pct, cat, amt)
                val center = "${if (chipIncome.isChecked) "Earned" else "Spent"}\n₹%.2f".format(amt)
                pieChart.crossfadeCenterText(center)

                // highlight in the list & scroll to it
                catAdapter.setSelectedCategory(cat)
                val pos = catAdapter.indexOfCategory(cat)
                if (pos >= 0) rvCategoryTotals.smoothScrollToPosition(pos)
            }
            override fun onNothingSelected() {
                tvSelectedCategory.crossfadeTo("")
                catAdapter.setSelectedCategory(null)
                lastSelectedAmt = 0.0
                val totals = lastRows.sumOf { kotlin.math.abs(it.total) }
                val center = "${if (chipIncome.isChecked) "Earned" else "Spent"}\n₹%.2f".format(totals)
                pieChart.crossfadeCenterText(center)
            }
        })
    }

    private fun loadAndRender() {
        lifecycleScope.launch {
            val (start, end) = periodBounds(cal)
            val type = if (chipIncome.isChecked) "INCOME" else "EXPENSE"
            val rows: List<CategoryTotal> = txnDao.getCategoryTotalsInRange(type, start, end)
            renderPie(rows, type)
        }
    }

    private fun renderPie(rows: List<CategoryTotal>, type: String) {
        lastRows = rows
        tvSelectedCategory.text = ""  // reset selection text
        if (rows.isEmpty()) {
            tvTotalSummary.text = "No records"
            pieChart.centerText = "No records"
            pieChart.clear()
            pieChart.invalidate()
            catAdapter.setData(emptyList())
            return
        }

        val total = rows.sumOf { it.total }
        val center = if (type == "INCOME") "Earned\n₹%.2f".format(total) else "Spent\n₹%.2f".format(total)
        tvTotalSummary.text = if (type == "INCOME") "Earned: ₹%.2f".format(total) else "Spent: ₹%.2f".format(total)

        val entries = rows.map { PieEntry(it.total.toFloat(), it.category ?: "Uncategorized") }
        val colors = entries.map { e -> colorForCategory(e.label ?: "Uncategorized") }

        val dataSet = PieDataSet(entries, "").apply {
            sliceSpace = 1f
            valueTextSize = 12f
            setDrawValues(true)
            valueTextColor = android.graphics.Color.WHITE
            this.colors = colors
        }
        val data = PieData(dataSet).apply {
            setValueFormatter(com.github.mikephil.charting.formatter.PercentFormatter(pieChart))
        }

        pieChart.data = data
        pieChart.crossfadeCenterText(center)
        pieChart.highlightValues(null)
        pieChart.invalidate()
        pieChart.animateY(600)

        // list under the chart
        catAdapter.setData(rows)
    }

    private val palette = listOf(
        "#F44336", "#FF9800", "#FFEB3B", "#4CAF50", "#00BCD4",
        "#2196F3", "#9C27B0", "#795548", "#607D8B", "#8BC34A",
        "#E91E63", "#3F51B5"
    ).map { android.graphics.Color.parseColor(it) }

    private fun colorForCategory(name: String): Int {
        val key = name.lowercase(Locale.getDefault())
        val idx = (key.hashCode() and 0x7fffffff) % palette.size
        return palette[idx]
    }


    private fun TextView.crossfadeTo(textNew: String, duration: Long = 180) {
        if (this.text == textNew) return
        animate().alpha(0f).setDuration(duration).withEndAction {
            text = textNew
            animate().alpha(1f).setDuration(duration).start()
        }.start()
    }

    private fun animateAmount(start: Double, end: Double, onUpdate: (String) -> Unit) {
        val va = ValueAnimator.ofFloat(start.toFloat(), end.toFloat())
        va.duration = 300
        va.interpolator = DecelerateInterpolator()
        va.addUpdateListener {
            val v = it.animatedValue as Float
            onUpdate("₹%.2f".format(v))
        }
        va.start()
    }

    private fun showSelectedCategoryAnimated(name: String, pct: Double, amt: Double) {
        // Left part (percent + name) crossfades
        val left = "%.1f%%  •  %s".format(java.util.Locale.getDefault(), pct, name)
        tvSelectedCategory.crossfadeTo(left)

        // Right part (amount) counts up. If you prefer a single TextView, just compose it all there.
        // Here we’ll keep it simple and animate the amount inside the same TextView:
        // Compose while animating so it feels like the number is changing.
        val start = lastSelectedAmt
        animateAmount(start, amt) { animatedAmount ->
            tvSelectedCategory.text = "$left  •  $animatedAmount"
        }
        lastSelectedAmt = amt
    }
    private fun PieChart.crossfadeCenterText(
        newText: String,
        fadeDuration: Long = 150L
    ) {
        // Avoid flicker if text is unchanged
        if (this.centerText == newText) return

        this.animate().alpha(0f).setDuration(fadeDuration).withEndAction {
            this.centerText = newText
            this.invalidate()
            this.animate().alpha(1f).setDuration(fadeDuration).start()
        }.start()
    }

}
