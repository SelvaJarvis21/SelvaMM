package com.example.selvamoneymanager.stats

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.selvamoneymanager.R
import com.example.selvamoneymanager.trans.CategoryTotal
import java.util.Locale
import kotlin.math.abs

class CategoryTotalsAdapter(
    private val items: MutableList<CategoryTotal> = mutableListOf(),
    private var currencyPrefix: String = "₹",
    private val onItemClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<CategoryTotalsAdapter.VH>() {

    private var selectedCategory: String? = null
    private var sumAbs: Double = 0.0

    fun setData(newItems: List<CategoryTotal>) {
        items.clear()
        items.addAll(newItems)
        sumAbs = items.sumOf { abs(it.total) }
        // reset selection on data refresh so we don’t carry stale selection across periods
        selectedCategory = null
        notifyDataSetChanged()
    }

    fun setSelectedCategory(cat: String?) {
        selectedCategory = cat
        notifyDataSetChanged()
    }

    fun indexOfCategory(cat: String?): Int {
        val target = cat ?: "Uncategorized"
        return items.indexOfFirst { (it.category ?: "Uncategorized") == target }
    }

    fun setCurrencyPrefix(prefix: String) {
        currencyPrefix = prefix
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        val name = row.category ?: "Uncategorized"
        val amountAbs = abs(row.total)
        val pct = if (sumAbs > 0) (amountAbs / sumAbs) * 100.0 else 0.0

        // % first, then name, then amount
        holder.tvPct.text = String.format(Locale.getDefault(), "%.1f%%", pct)
        holder.tvName.text = name
        holder.tvAmount.text = String.format(Locale.getDefault(), "%s%.2f", currencyPrefix, amountAbs)

        val selected = (name == selectedCategory)
        holder.itemView.alpha = if (selected) 1.0f else 0.92f
        holder.tvName.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)

        holder.itemView.setOnClickListener { onItemClick?.invoke(name) }
    }

    override fun getItemCount(): Int = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvPct: TextView = v.findViewById(R.id.tvPct)
        val tvName: TextView = v.findViewById(R.id.tvCategoryName)
        val tvAmount: TextView = v.findViewById(R.id.tvCategoryAmount)
    }
}
