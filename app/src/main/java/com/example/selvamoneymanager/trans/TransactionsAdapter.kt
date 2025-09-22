package com.example.selvamoneymanager.trans

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.selvamoneymanager.R
import java.text.SimpleDateFormat
import java.util.*

class TransactionsAdapter(private val items: List<TxnListItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val dfDay = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
    private val dfDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    companion object {
        private const val TYPE_SECTION = 0
        private const val TYPE_ROW = 1
        private const val TYPE_MONTH_TOTAL = 2
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is TxnListItem.Section -> TYPE_SECTION
        is TxnListItem.Row -> TYPE_ROW
        is TxnListItem.MonthTotal -> TYPE_MONTH_TOTAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SECTION -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_section_header, parent, false)
                SectionVH(v)
            }
            TYPE_MONTH_TOTAL -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_month_total, parent, false)
                MonthTotalVH(v)
            }
            else -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_transaction, parent, false)
                RowVH(v)
            }
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TxnListItem.Section -> (holder as SectionVH).bind(item.title)
            is TxnListItem.MonthTotal -> (holder as MonthTotalVH).bind(item)
            is TxnListItem.Row -> (holder as RowVH).bind(item.data)
        }
    }

    class SectionVH(v: View) : RecyclerView.ViewHolder(v) {
        private val tv = v.findViewById<TextView>(R.id.tvSectionTitle)
        fun bind(title: String) { tv.text = title }
    }

    class MonthTotalVH(v: View) : RecyclerView.ViewHolder(v) {
        private val monthLabel = v.findViewById<TextView>(R.id.tvMonthLabel)
        private val totalView = v.findViewById<TextView>(R.id.tvMonthTotal)

        fun bind(item: TxnListItem.MonthTotal) {
            monthLabel.text = item.monthLabel
            totalView.text = "₹%.2f".format(item.total)
            totalView.setTextColor(
                if (item.total >= 0) 0xFF00BCD4.toInt()
                else 0xFFF44336.toInt()
            )
        }
    }

    class RowVH(v: View) : RecyclerView.ViewHolder(v) {
        private val top = v.findViewById<TextView>(R.id.tvTop)
        private val desc = v.findViewById<TextView>(R.id.tvDesc)
        private val da = v.findViewById<TextView>(R.id.tvDateAmount)
        private val dfDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun bind(r: TransactionRow) {
            val date = dfDate.format(Date(r.dateMillis))
            when (r.type.uppercase(Locale.getDefault())) {
                "INCOME" -> {
                    // e.g., "Salary • HDFC"
                    top.text = "${r.category ?: "Income"} • ${r.accountName ?: ""}"
                    desc.text = r.description
                    da.text = "$date • ₹%.2f".format(r.amount)
                    da.setTextColor(0xFF00A65A.toInt()) // green-ish
                }
                "EXPENSE" -> {
                    // e.g., "Food • HDFC"
                    top.text = "${r.category ?: "Expense"} • ${r.accountName ?: ""}"
                    desc.text = r.description
                    da.text = "$date • ₹%.2f".format(r.amount)
                    da.setTextColor(0xFFF44336.toInt()) // red-ish
                }
                "TRANSFER" -> {
                    // e.g., "Transfer • HDFC → SBI"
                    val from = r.fromAccountName ?: "?"
                    val to = r.toAccountName ?: "?"
                    top.text = "Transfer • $from → $to"
                    desc.text = r.description
                    // show absolute amount for neutral transfer display
                    da.text = "$date • ₹%.2f".format(kotlin.math.abs(r.amount))
                    da.setTextColor(0xFF00BCD4.toInt()) // cyan/neutral
                }
                else -> {
                    // Fallback
                    top.text = (r.category ?: "Txn") + (if (!r.accountName.isNullOrBlank()) " • ${r.accountName}" else "")
                    desc.text = r.description
                    da.text = "$date • ₹%.2f".format(r.amount)
                    da.setTextColor(if (r.amount >= 0) 0xFF00BCD4.toInt() else 0xFFF44336.toInt())
                }
            }
        }
    }
}
