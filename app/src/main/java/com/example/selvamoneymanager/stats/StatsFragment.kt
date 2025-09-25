package com.example.selvamoneymanager.stats

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.selvamoneymanager.R
import com.example.selvamoneymanager.db.AppDatabase
import com.example.selvamoneymanager.db.TransactionDao
import com.example.selvamoneymanager.db.TransactionType
import com.example.selvamoneymanager.trans.CategoryTotal
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class StatsFragment : Fragment() {

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

    // State
    private val cal = Calendar.getInstance()
    private var isYearly: Boolean = false
    private var lastRows: List<CategoryTotal> = emptyList()
    private var lastSelectedAmt: Double = 0.0

    /** Derived period string for child fragments */
    private val selectedTimePeriod: String
        get() = if (isYearly) "yearly" else "monthly"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_stats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txnDao = AppDatabase.getDatabase(requireContext()).transactionDao()

        ddTimeline = view.findViewById(R.id.ddTimeline)
        pieChart = view.findViewById(R.id.pieChart)
        tvPeriod = view.findViewById(R.id.tvPeriod)
        chipIncome = view.findViewById(R.id.chipIncome)
        chipExpense = view.findViewById(R.id.chipExpense)
        tvTotalSummary = view.findViewById(R.id.tvTotalSummary)
        rvCategoryTotals = view.findViewById(R.id.rvCategoryTotals)
        tvSelectedCategory = view.findViewById(R.id.tvSelectedCategory)

        rvCategoryTotals.layoutManager = LinearLayoutManager(requireContext())
        catAdapter = CategoryTotalsAdapter(onItemClick = { categoryName ->
            val catId = lastRows.find { it.category == categoryName }?.categoryId ?: return@CategoryTotalsAdapter
            openCategoryTransactions(catId, categoryName)
        })
        rvCategoryTotals.adapter = catAdapter

        setupChart()

        // Timeline dropdown
        val modes = listOf("Monthly", "Yearly")
        ddTimeline.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, modes))
        ddTimeline.setText("Monthly", false)
        ddTimeline.setOnItemClickListener { _, _, position, _ ->
            isYearly = (position == 1)
            updateHeader()
            loadAndRender()
        }

        // Prev/Next buttons
        view.findViewById<ImageButton>(R.id.btnPrev).setOnClickListener {
            if (isYearly) cal.add(Calendar.YEAR, -1) else cal.add(Calendar.MONTH, -1)
            updateHeader()
            loadAndRender()
        }
        view.findViewById<ImageButton>(R.id.btnNext).setOnClickListener {
            if (isYearly) cal.add(Calendar.YEAR, 1) else cal.add(Calendar.MONTH, 1)
            updateHeader()
            loadAndRender()
        }

        // Income / Expense toggle
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
                set(Calendar.MONTH, Calendar.JANUARY); set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = (c.clone() as Calendar).apply {
                set(Calendar.MONTH, Calendar.DECEMBER); set(Calendar.DAY_OF_MONTH, 31)
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
        pieChart.legend.isEnabled = false

        pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val pe = e as? PieEntry ?: return
                val cat = pe.label ?: "Uncategorized"
                val sum = lastRows.sumOf { abs(it.total) }
                val amt = abs(lastRows.find { (it.category ?: "Uncategorized") == cat }?.total ?: 0.0)
                val pct = if (sum > 0) (amt / sum) * 100.0 else 0.0
                showSelectedCategoryAnimated(cat, pct, amt)
                tvSelectedCategory.text = "%.1f%%  •  %s  •  ₹%.2f".format(Locale.getDefault(), pct, cat, amt)
                val center = "${if (chipIncome.isChecked) "Earned" else "Spent"}\n₹%.2f".format(amt)
                pieChart.crossfadeCenterText(center)

                catAdapter.setSelectedCategory(cat)
                val pos = catAdapter.indexOfCategory(cat)
                if (pos >= 0) rvCategoryTotals.smoothScrollToPosition(pos)

                // ✅ Open transaction list for this category
                val catId = lastRows.find { (it.category ?: "Uncategorized") == cat }?.categoryId
                if (catId != null) {
                    openCategoryTransactions(catId, cat)
                }
            }

            override fun onNothingSelected() {
                tvSelectedCategory.crossfadeTo("")
                catAdapter.setSelectedCategory(null)
                lastSelectedAmt = 0.0
                val totals = lastRows.sumOf { abs(it.total) }
                val center = "${if (chipIncome.isChecked) "Earned" else "Spent"}\n₹%.2f".format(totals)
                pieChart.crossfadeCenterText(center)
            }
        })
    }

    private fun openCategoryTransactions(categoryId: Int, categoryName: String) {
        val fragment = CategoryTransactionsFragment.newInstance(
            categoryId,
            categoryName,
            selectedTimePeriod
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun loadAndRender() {
        viewLifecycleOwner.lifecycleScope.launch {
            val (start, end) = periodBounds(cal)
            val type = if (chipIncome.isChecked) TransactionType.INCOME else TransactionType.EXPENSE
            val rows: List<CategoryTotal> = txnDao.getCategoryTotalsInRange(type, start, end)
            renderPie(rows, type)
        }
    }

    private fun renderPie(rows: List<CategoryTotal>, type: TransactionType) {
        lastRows = rows
        tvSelectedCategory.text = ""

        if (rows.isEmpty()) {
            tvTotalSummary.text = "No records"
            pieChart.centerText = "No records"
            pieChart.clear()
            pieChart.invalidate()
            catAdapter.setData(emptyList())
            return
        }

        val total = rows.sumOf { it.total }
        val center = if (type == TransactionType.INCOME) "Earned\n₹%.2f".format(total)
        else "Spent\n₹%.2f".format(total)
        tvTotalSummary.text = if (type == TransactionType.INCOME) "Earned: ₹%.2f".format(total)
        else "Spent: ₹%.2f".format(total)

        val entries = rows.map { PieEntry(it.total.toFloat(), it.category ?: "Uncategorized") }
        val colors = entries.map { e -> colorForCategory(e.label ?: "Uncategorized") }

        val dataSet = PieDataSet(entries, "").apply {
            sliceSpace = 1f
            valueTextSize = 12f
            setDrawValues(true)
            valueTextColor = Color.WHITE
            this.colors = colors
        }
        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(pieChart))
        }

        pieChart.data = data
        pieChart.crossfadeCenterText(center)
        pieChart.highlightValues(null)
        pieChart.invalidate()
        pieChart.animateY(600)

        catAdapter.setData(rows)
    }

    private val palette = listOf(
        "#F44336", "#FF9800", "#FFEB3B", "#4CAF50", "#00BCD4",
        "#2196F3", "#9C27B0", "#795548", "#607D8B", "#8BC34A",
        "#E91E63", "#3F51B5"
    ).map { Color.parseColor(it) }

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
        val left = "%.1f%%  •  %s".format(Locale.getDefault(), pct, name)
        tvSelectedCategory.crossfadeTo(left)

        val start = lastSelectedAmt
        animateAmount(start, amt) { animatedAmount ->
            tvSelectedCategory.text = "$left  •  $animatedAmount"
        }
        lastSelectedAmt = amt
    }

    private fun PieChart.crossfadeCenterText(newText: String, fadeDuration: Long = 150L) {
        if (this.centerText == newText) return
        this.animate().alpha(0f).setDuration(fadeDuration).withEndAction {
            this.centerText = newText
            this.invalidate()
            this.animate().alpha(1f).setDuration(fadeDuration).start()
        }.start()
    }

    private fun highlightCategory(name: String) {
        val data = pieChart.data ?: return
        val entries = (data.dataSet as PieDataSet).values
        val idx = entries.indexOfFirst { (it as PieEntry).label == name }
        if (idx >= 0) {
            pieChart.highlightValue(idx.toFloat(), 0)
            val sum = lastRows.sumOf { abs(it.total) }
            val amt = abs(lastRows.find { (it.category ?: "Uncategorized") == name }?.total ?: 0.0)
            val pct = if (sum > 0) (amt / sum) * 100.0 else 0.0
            tvSelectedCategory.text = "%.1f%%  •  %s  •  ₹%.2f".format(Locale.getDefault(), pct, name, amt)
            catAdapter.setSelectedCategory(name)
        }
    }
}
