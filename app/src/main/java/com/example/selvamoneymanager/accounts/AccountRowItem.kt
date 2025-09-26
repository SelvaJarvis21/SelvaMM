package com.example.selvamoneymanager.accounts

import com.example.selvamoneymanager.db.Account

sealed class AccountRowItem {
    data class SectionHeader(val title: String) : AccountRowItem()
    data class AccountItem(
        val account: Account,
        val currentBalance: Double
    ) : AccountRowItem()
}
