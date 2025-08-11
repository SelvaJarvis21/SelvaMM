package com.example.selvamoneymanager.db

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.selvamoneymanager.R
import com.example.selvamoneymanager.db.Account
import com.example.selvamoneymanager.accounts.AccountRowItem

class AccountAdapter(
    private val items: List<AccountRowItem>,
    private val onClick: (Account) -> Unit,
    private val onLongClick: (Account) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SECTION_HEADER = 0
        private const val TYPE_ACCOUNT_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AccountRowItem.SectionHeader -> TYPE_SECTION_HEADER
            is AccountRowItem.AccountItem -> TYPE_ACCOUNT_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_SECTION_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_section_header, parent, false)
            SectionHeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_account, parent, false)
            AccountViewHolder(view, onClick, onLongClick) // ✅ pass listeners
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AccountRowItem.SectionHeader -> (holder as SectionHeaderViewHolder).bind(item)
            is AccountRowItem.AccountItem -> (holder as AccountViewHolder).bind(item.account)
        }
    }

    class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSectionTitle = itemView.findViewById<TextView>(R.id.tvSectionTitle)
        fun bind(header: AccountRowItem.SectionHeader) {
            tvSectionTitle.text = header.title
        }
    }

    class AccountViewHolder(
        itemView: View,
        private val onClick: (Account) -> Unit,
        private val onLongClick: (Account) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvAccountName = itemView.findViewById<TextView>(R.id.tvAccountName)
        private val tvAmount = itemView.findViewById<TextView>(R.id.tvAmount)
        private val tvPayable = itemView.findViewById<TextView>(R.id.tvBalancePayable)
        private val tvOutstanding = itemView.findViewById<TextView>(R.id.tvOutstandingBalance)
        private val layoutCardDetails = itemView.findViewById<View>(R.id.layoutCardDetails)

        fun bind(account: Account) {
            tvAccountName.text = account.name
            tvAmount.text = "₹%.2f".format(account.amount)

            // Show/hide card details
            if (account.balancePayable != 0.0 || account.outstandingBalance != 0.0) {
                layoutCardDetails.visibility = View.VISIBLE
                tvPayable.text = "Balance Payable: ₹%.2f".format(account.balancePayable)
                tvOutstanding.text = "Outstanding: ₹%.2f".format(account.outstandingBalance)
            } else {
                layoutCardDetails.visibility = View.GONE
            }

            // Color code based on value
            tvAmount.setTextColor(
                if (account.amount >= 0) 0xFF00BCD4.toInt() else 0xFFF44336.toInt()
            )

            itemView.setOnClickListener { onClick(account) }
            itemView.setOnLongClickListener {
                onLongClick(account)
                true
            }
        }
    }

}