package com.example.selvamoneymanager.trans

sealed class TxnListItem {
    data class Section(val title: String) : TxnListItem()
    data class Row(val data: TransactionRow) : TxnListItem()
    data class MonthTotal(val monthLabel: String, val total: Double) : TxnListItem()
}
