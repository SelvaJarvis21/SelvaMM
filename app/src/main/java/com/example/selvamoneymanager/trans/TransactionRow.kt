package com.example.selvamoneymanager.trans

data class TransactionRow(
    val id: Int,
    val type: String,
    val dateMillis: Long,
    val amount: Double,
    val category: String?,
    val description: String,
    val accountId: Int?,
    val accountName: String?,        // for income/expense
    val fromAccountId: Int?,         // for transfer
    val fromAccountName: String?,    // for transfer
    val toAccountId: Int?,           // for transfer
    val toAccountName: String?       // for transfer
)