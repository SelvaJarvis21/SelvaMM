package com.example.selvamoneymanager.accounts

sealed class AccountRowItem {
    data class SectionHeader(val title: String) : AccountRowItem()
    data class AccountItem(val account: Account) : AccountRowItem()
}
